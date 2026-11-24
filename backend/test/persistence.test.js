import test from 'node:test';import assert from 'node:assert/strict';
import {mkdtemp,rm} from 'node:fs/promises';import {tmpdir} from 'node:os';import {join} from 'node:path';
import {openDatabase} from '../src/database.js';import {createStore} from '../src/domain.js';
test('disk restart, offline button replay, deletion tombstones and idempotent retry',async()=>{
 const dir=await mkdtemp(join(tmpdir(),'sleep-test-'));const path=join(dir,'test.sqlite');let db;
 try{
  db=openDatabase(path);let store=createStore(db,'bedroom-01');const now=Date.now();
  const future={id:'newer-app-session',start:now-3600000,end:null,quality:0,notes:'',interrupted:false,version:0};store.saveSession(future);
  store.ingest('event',{id:'offline-start',timestamp:now-12*3600000,kind:'sleep_start',source:'device'});
  store.ingest('event',{id:'offline-end',timestamp:now-5*3600000,kind:'sleep_end',source:'device'});
  assert.equal(store.active().id,future.id);
  const replayed=store.sessions().find(s=>s.id==='device_offline-start');assert.equal(replayed.end,now-5*3600000);
  const removed=store.saveSession({...replayed,deleted:true});assert.equal(removed.version,2);
  db.close();db=openDatabase(path);store=createStore(db,'bedroom-01');
  assert.equal(store.sessions().find(s=>s.id===removed.id).deleted,1);
  assert.equal(store.saveSession({...replayed,deleted:true}).version,2);
  store.ingest('event',{id:'offline-start',timestamp:now-12*3600000,kind:'sleep_start',source:'device'});
  assert.equal(store.sessions().length,2);
 }finally{db?.close();await rm(dir,{recursive:true,force:true});}
});
