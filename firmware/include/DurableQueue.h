#pragma once
#include <Arduino.h>
#include <deque>
struct QueueItem {String path,id,topic,payload;};
class DurableQueue {
  std::deque<String> files;uint32_t next=1;bool ready=false;
public:
  bool begin();
  bool push(const String& topic,const String& payload,const String& id,bool critical=false);
  bool front(QueueItem& item);
  void ack(const String& id);
  size_t size() const{return files.size();}
  bool healthy() const{return ready;}
};
