import test from 'node:test';import assert from 'node:assert/strict';import mqtt from 'mqtt';
import {startSystem} from '../src/system.js';import {config} from '../src/config.js';
test('HTTP + real MQTT ingest, deduplication, session persistence, stale data and command acknowledgement',async()=>{
 const s=await startSystem({...config,host:'127.0.0.1',port:0,mqttPort:0,database:':memory:'});
 const c=mqtt.connect('mqtt://127.0.0.1:'+s.mqttPort,{username:config.deviceId,password:config.devicePassword,reconnectPeriod:0});
 try {
  await new Promise((r,j)=>{c.once('connect',r);c.once('error',j);});
  const base='http://127.0.0.1:'+s.httpPort;
  const api=(path,method='GET',body)=>fetch(base+'/api'+path,{method,headers:{'X-API-Key':config.apiKey,'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});
  assert.equal((await fetch(base+'/api/sessions')).status,401);
  const now=Date.now(),session={id:'test',start:now-8*3600000,end:now-28*60000,quality:4,notes:'Slept well',interrupted:false,version:0};
  let res=await api('/sessions/test','PUT',session);assert.equal(res.status,200);assert.equal((await res.json()).version,1);
  assert.equal((await api('/sessions/test','PUT',session)).status,200);
  assert.equal((await api('/sessions/test','PUT',{...session,notes:'Stale edit'})).status,409);
  assert.equal((await api('/sessions/bad','PUT',{...session,id:'bad',end:session.start})).status,400);
  const send=(topic,p)=>new Promise((r,j)=>c.publish('sleep/bedroom-01/'+topic,JSON.stringify(p),{qos:1},e=>e?j(e):r()));
  const p={id:'sample',timestamp:now-60000,temperature:25.5,humidity:61,light:30,source:'device'};
  await send('telemetry',p);await send('telemetry',p);await send('telemetry',{...p,id:'old',timestamp:now-120000,temperature:20});
  await send('status',{online:true,lampOnline:true,timeSynced:true,brightness:0});
  // Broker PUBACK follows publish hooks asynchronously: wait for storage, bounded.
  for(let n=0;n<30 && s.store.db.prepare('SELECT count(*) n FROM telemetry').get().n<2;n++)await new Promise(r=>setTimeout(r,30));
  const env=await (await api('/environment')).json();assert.equal(env.samples.length,2);assert.equal(env.current.temperature,25.5);
  assert.equal(env.status.online,true);
  await c.subscribeAsync('sleep/bedroom-01/command');
  c.on('message',(topic,data)=>{if(topic.endsWith('/command')){const cmd=JSON.parse(data);void send('command-ack',{id:cmd.id,ok:true});}});
  const cmd=await (await api('/commands','POST',{brightness:40})).json();
  let state;for(let n=0;n<30;n++){state=await(await api('/commands/'+cmd.id)).json();if(state.state==='applied')break;await new Promise(r=>setTimeout(r,30));}assert.equal(state.state,'applied');
  const start={id:'button_start',timestamp:now-30000,kind:'sleep_start',source:'device'};
  await send('event',start);await send('event',start);await send('event',{id:'button_end',timestamp:now,kind:'sleep_end',source:'device'});
  for(let n=0;n<30 && s.store.active();n++)await new Promise(r=>setTimeout(r,30));
  const rows=await(await api('/sessions')).json();assert.equal(rows.filter(x=>x.id==='device_button_start').length,1);
  assert.equal(rows.find(x=>x.id==='device_button_start').end,now);
  const settings=await(await api('/settings')).json();assert.equal((await api('/settings','PUT',{...settings,humidityMax:-1})).status,400);
 }finally{await c.endAsync();await s.close();}
});
