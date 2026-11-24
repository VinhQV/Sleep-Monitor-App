#include "DurableQueue.h"
#include <LittleFS.h>
#include <ArduinoJson.h>
#include <algorithm>
bool DurableQueue::begin(){
  // Only format an uninitialized filesystem. Mount failure with existing data is not erased automatically.
  ready=LittleFS.begin(false);if(!ready){Serial.println("LittleFS mount failed. Upload filesystem image once (pio run -e gateway -t uploadfs).");return false;}
  File root=LittleFS.open("/");File f=root.openNextFile();
  while(f){String p=f.name();if(!p.startsWith("/"))p="/"+p;if(p.endsWith(".json")){files.push_back(p);next=max(next,(uint32_t)strtoul(p.c_str()+1,nullptr,10)+1);}f=root.openNextFile();}
  std::sort(files.begin(),files.end());return true;
}
bool DurableQueue::push(const String& topic,const String& payload,const String& id,bool critical){
  if(!ready||files.size()>=(critical?512:500))return false;
  char path[32];snprintf(path,sizeof(path),"/%010u.json",next++);String tmp=String(path)+".tmp";
  StaticJsonDocument<1280> d;d["topic"]=topic;d["payload"]=payload;d["id"]=id;
  File f=LittleFS.open(tmp,"w");if(!f)return false;size_t expected=measureJson(d),written=serializeJson(d,f);f.flush();f.close();
  if(written!=expected||!LittleFS.rename(tmp,path)){LittleFS.remove(tmp);return false;}
  files.push_back(path);return true;
}
bool DurableQueue::front(QueueItem& i){
  while(!files.empty()){
    i.path=files.front();File f=LittleFS.open(i.path);StaticJsonDocument<1280>d;auto e=deserializeJson(d,f);f.close();
    if(!e){i.id=d["id"].as<String>();i.topic=d["topic"].as<String>();i.payload=d["payload"].as<String>();return true;}
    // Quarantine corrupt data for diagnosis, do not loop forever on it.
    LittleFS.rename(i.path,i.path+".bad");files.pop_front();Serial.println("Queue item corrupt; quarantined.");
  }return false;
}
void DurableQueue::ack(const String& id){QueueItem i;if(front(i)&&i.id==id){if(LittleFS.remove(i.path))files.pop_front();}}
