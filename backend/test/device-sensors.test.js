import test from 'node:test';
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {mkdtempSync,rmSync} from 'node:fs';
import {join} from 'node:path';
import {tmpdir} from 'node:os';
import {startSystem} from '../src/system.js';
import {config} from '../src/config.js';
import {openDatabase} from '../src/database.js';

test('existing lux history survives ADC schema migration and reopening',()=>{
  const dir=mkdtempSync(join(tmpdir(),'sleep-adc-'));
  const path=join(dir,'sleep.sqlite');
  try{
    const old=new DatabaseSync(path);
    old.exec("CREATE TABLE telemetry(id TEXT PRIMARY KEY,deviceId TEXT,timestamp INTEGER,temperature REAL,humidity REAL,light REAL,source TEXT); INSERT INTO telemetry VALUES('old','bedroom-01',1700000000000,25,60,12,'device')");
    old.close();
    for(let i=0;i<2;i++){
      const db=openDatabase(path);
      const row=db.prepare('SELECT * FROM telemetry').get();
      assert.equal(row.light,12);
      assert.equal(row.lightAdc,null);
      assert.equal(row.lightMillivolts,null);
      db.close();
    }
  }finally{rmSync(dir,{recursive:true,force:true});}
});

test('switch off, ADC ingest, stale heartbeat and reconnect preserve correct connection state',async()=>{
  const sys=await startSystem({...config,host:'127.0.0.1',port:0,mqttPort:0,database:':memory:'});
  const base='http://127.0.0.1:'+sys.httpPort+'/api';
  const api=(path,body)=>fetch(base+path,{method:body?'POST':'GET',
    headers:{'X-API-Key':config.apiKey,'Content-Type':'application/json'},
    body:body?JSON.stringify(body):undefined});
  const sample={temperature:26,humidity:55,lightAdc:1234,lightMillivolts:994};
  try{
    assert.equal((await fetch(base+'/device/status',{method:'POST',headers:{'Content-Type':'application/json'},body:'{"enabled":false}'})).status,401);
    assert.equal((await api('/device/status',{enabled:true})).status,200);
    assert.equal((await api('/device/environment',sample)).status,200);
    let env=await (await api('/environment')).json();
    assert.equal(env.current.lightAdc,1234);
    assert.equal(env.current.lightMillivolts,994);
    assert.equal(env.current.light,null);
    assert.equal(env.status.online,true);
    const firstSeen=env.status.lastSeen;
    sys.store.set('status',{...env.status,lastSeen:firstSeen-20000});
    assert.equal((await (await api('/environment')).json()).status.online,false);
    assert.equal((await api('/device/environment',sample)).status,200);
    env=await (await api('/environment')).json();
    assert.equal(env.status.online,true);
    assert.ok(env.status.lastSeen>=firstSeen);
    assert.equal((await api('/device/environment',{...sample,lightAdc:4096})).status,400);
    assert.equal((await api('/device/status',{enabled:false})).status,200);
    assert.equal((await (await api('/environment')).json()).status.online,false);
    assert.equal((await api('/device/environment',sample)).status,409);
    assert.equal((await api('/device/status',{enabled:true})).status,200);
    assert.equal((await api('/device/environment',sample)).status,200);
    assert.equal((await (await api('/environment')).json()).status.online,true);
  }finally{await sys.close();}
});
