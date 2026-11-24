#include "RadioLink.h"
#include "DeviceConfig.h"
#include <esp_now.h>
#include <WiFi.h>
static uint8_t peer[6];static QueueHandle_t inbox;
static void received(const uint8_t* mac,const uint8_t* data,int len){
  if(memcmp(mac,peer,6)||len!=sizeof(LampPacket))return;
  LampPacket p;memcpy(&p,data,sizeof(p));if(p.magic!=0x534C5039||p.version!=1||p.brightness>100)return;
  xQueueSend(inbox,&p,0); // Wi-Fi callback: no flash, network, PWM or JSON work here.
}
bool beginRadio(){
  inbox=xQueueCreate(12,sizeof(LampPacket));uint8_t key[16];if(!inbox||!parseMac(deviceConfig.peerMac,peer)||!parseKey(deviceConfig.linkKey,key)||esp_now_init()!=ESP_OK)return false;
  esp_now_set_pmk(key);esp_now_peer_info_t info={};memcpy(info.peer_addr,peer,6);memcpy(info.lmk,key,16);info.channel=0;info.ifidx=WIFI_IF_STA;info.encrypt=true;
  if(esp_now_add_peer(&info)!=ESP_OK)return false;
  esp_now_register_recv_cb(received);return true;
}
bool sendRadio(const LampPacket& p){return esp_now_send(peer,(const uint8_t*)&p,sizeof(p))==ESP_OK;}
bool receiveRadio(LampPacket& p){return inbox&&xQueueReceive(inbox,&p,0)==pdTRUE;}
