// Explicit opt-in simulation: all packets carry source=simulator.
import mqtt from 'mqtt';import { randomUUID } from 'node:crypto';import { config } from './config.js';
const c=mqtt.connect(process.env.MQTT_URL||'mqtt://127.0.0.1:'+config.mqttPort,{username:config.deviceId,password:config.devicePassword,
  will:{topic:'sleep/'+config.deviceId+'/status',payload:JSON.stringify({online:false,source:'simulator'}),qos:1}});
const root='sleep/'+config.deviceId+'/';let brightness=0,phase=0;
const send=(topic,p)=>c.publish(root+topic,JSON.stringify(p),{qos:1});
function status(){send('status',{online:true,lampOnline:true,brightness,timeSynced:true,queue:0,source:'simulator'});}
c.on('connect',()=>{console.log('SIMULATOR connected: no physical sensors');c.subscribe([root+'command',root+'config']);tick();});
c.on('message',(topic,data)=>{const p=JSON.parse(data);if(topic===root+'command'&&p.expiresAt>Date.now()){
  brightness=p.brightness;send('command-ack',{id:p.id,ok:true,detail:'Simulated lamp'});send('event',{id:randomUUID(),timestamp:Date.now(),kind:'light_change',value:String(brightness),source:'simulator'});status();
}});
function tick(){if(!c.connected)return;phase+=0.15;status();send('telemetry',{id:randomUUID(),timestamp:Date.now(),temperature:26+Math.sin(phase),humidity:60+Math.round(Math.sin(phase)*3),light:Math.round(25+brightness*2),source:'simulator'});}
const timer=setInterval(tick,5000);process.on('SIGINT',()=>{clearInterval(timer);send('status',{online:false,source:'simulator'});c.end();});
