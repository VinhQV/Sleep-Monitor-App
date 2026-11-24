import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { readOllamaStream } from '../src/assistant-stream.js';
import { startSystem } from '../src/system.js';
import { config } from '../src/config.js';

const frame=(text,done=false)=>JSON.stringify({message:{content:text},done})+'\n';
const bytes=async function*(raw){for(const byte of Buffer.from(raw))yield Uint8Array.of(byte);};

test('stream decoder preserves Vietnamese across byte boundaries and never exposes thinking',async()=>{
  const deltas=[];
  const raw=JSON.stringify({message:{thinking:'PRIVATE REASONING'},done:false})+'\n'+frame('Giấc ngủ ')+frame('êm dịu.',true).trimEnd();
  const text=await readOllamaStream(bytes(raw),d=>deltas.push(d));
  assert.equal(text,'Giấc ngủ êm dịu.');assert.equal(deltas.join(''),text);
});

test('stream decoder rejects truncated, malformed, error and length-limited replies',async()=>{
  for(const raw of [frame('partial'),frame('partial')+'invalid\n',frame('partial')+'{"error":"PRIVATE PROVIDER ERROR"}\n',frame('partial')+'{"done":true,"done_reason":"length"}\n']){
    await assert.rejects(readOllamaStream(bytes(raw),()=>{}),e=>e.status===502&&!e.message.includes('PRIVATE'));
  }
});

test('HTTP streaming delivers first text before completion, reports interrupted replies and cancels upstream', {timeout:15000}, async()=>{
  let calls=0, completed=false;
  const release=Promise.withResolvers(), upstreamClosed=Promise.withResolvers();
  const provider=http.createServer(async(req,res)=>{
    calls++;let raw='';for await(const chunk of req)raw+=chunk;
    const body=JSON.parse(raw);assert.equal(body.stream,true);assert.equal(body.think,false);
    assert.ok(!JSON.stringify(body).includes('PRIVATE NOTE'));
    const mode=body.messages.at(-1).content;
    res.setHeader('Content-Type','application/x-ndjson');res.write(frame('Giấc ngủ '));
    if(mode==='cancel'){res.on('close',()=>upstreamClosed.resolve());return;}
    if(mode==='truncate'){res.end();return;}
    await release.promise;
    completed=true;res.end(frame('êm dịu.',true));
  });
  await new Promise(r=>provider.listen(0,'127.0.0.1',r));
  const system=await startSystem({...config,port:0,mqttPort:0,database:':memory:',ai:{provider:'ollama',model:'fixture',ollamaUrl:'http://127.0.0.1:'+provider.address().port}});
  const url='http://127.0.0.1:'+system.httpPort+'/api/ai/chat/stream';
  const headers={'X-API-Key':config.apiKey,'Content-Type':'application/json'};
  const body=mode=>JSON.stringify({messages:[{role:'user',content:mode}],context:{source:'demo',days:[],notes:'PRIVATE NOTE'}});
  try{
    assert.equal((await fetch(url,{method:'POST',headers:{'Content-Type':'application/json'},body:body('normal')})).status,401);
    assert.equal(calls,0);
    const response=await fetch(url,{method:'POST',headers,body:body('normal'),signal:AbortSignal.timeout(8000)});
    assert.match(response.headers.get('content-type'),/application\/x-ndjson/);
    const reader=response.body.getReader();const first=await reader.read();
    assert.equal(completed,false);assert.equal(JSON.parse(Buffer.from(first.value).toString().trim()).text,'Giấc ngủ ');
    release.resolve();let rest='';while(true){const next=await reader.read();if(next.done)break;rest+=Buffer.from(next.value).toString();}
    const done=rest.trim().split('\n').map(JSON.parse).at(-1);
    assert.equal(done.type,'done');assert.equal(done.text,'Giấc ngủ êm dịu.');assert.equal(done.contextUsed,true);

    const truncated=await fetch(url,{method:'POST',headers,body:body('truncate')});
    const frames=(await truncated.text()).trim().split('\n').map(JSON.parse);
    assert.equal(frames[0].type,'delta');assert.equal(frames.at(-1).type,'error');assert.equal(frames.at(-1).status,502);

    const controller=new AbortController();
    const canceled=await fetch(url,{method:'POST',headers,body:body('cancel'),signal:controller.signal});
    const cancelReader=canceled.body.getReader();await cancelReader.read();controller.abort();await cancelReader.cancel().catch(()=>{});
    await Promise.race([upstreamClosed.promise,new Promise((_,reject)=>{const timer=setTimeout(()=>reject(new Error('upstream did not close')),3000);timer.unref();})]);
  }finally{release.resolve();await system.close();provider.closeAllConnections();await new Promise(r=>provider.close(r));}
});
