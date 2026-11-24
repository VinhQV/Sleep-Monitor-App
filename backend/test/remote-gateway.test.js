import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import {startRemoteGateway} from '../src/remote-gateway.js';
const appKey='a'.repeat(43),deviceKey='d'.repeat(43),upstreamKey='local-fixture';
test('remote gateway authenticates, separates device permissions and streams without buffering',async()=>{
  const seen=[];
  const upstream=http.createServer(async(req,res)=>{
    let body='';for await(const chunk of req)body+=chunk;
    seen.push({key:req.headers['x-api-key'],url:req.url,body});
    if(req.url.endsWith('/stream')){
      res.writeHead(200,{'Content-Type':'application/x-ndjson'});res.write('{"type":"delta","text":"one"}\n');
      setTimeout(()=>res.end('{"type":"done","text":"one"}\n'),200);
    }else{res.writeHead(200,{'Content-Type':'application/json'});res.end('{"ok":true}');}
  });
  await new Promise(r=>upstream.listen(0,'127.0.0.1',r));
  const gate=await startRemoteGateway({port:0,upstreamPort:upstream.address().port,upstreamKey,appKey,deviceKey});
  const base='http://127.0.0.1:'+gate.port;
  const request=(path,key,method='GET',body)=>fetch(base+path,{method,headers:{'X-API-Key':key,'Content-Type':'application/json'},body});
  try{
    assert.equal((await request('/api/sessions',upstreamKey)).status,401);
    assert.equal((await request('/api/sessions',deviceKey)).status,403);
    assert.equal((await request('/api/ai/chat',deviceKey,'POST','{}')).status,403);
    assert.equal((await request('/api/device/status',appKey,'POST','{}')).status,403);
    assert.equal((await request('/api/device/status',deviceKey,'POST','{"enabled":true}')).status,200);
    assert.equal((await request('/api/sessions',appKey)).status,200);
    assert.equal((await request('/api/device/environment',deviceKey,'POST','x'.repeat(65537))).status,413);
    const stream=await request('/api/ai/chat/stream',appKey,'POST','{}');
    const reader=stream.body.getReader();const first=new TextDecoder().decode((await reader.read()).value);
    assert.match(first,/delta/);assert.doesNotMatch(first,/done/);
    let rest='';for(;;){const v=await reader.read();if(v.done)break;rest+=new TextDecoder().decode(v.value);}
    assert.match(rest,/done/);
    assert.equal(seen.length,3);assert.ok(seen.every(x=>x.key===upstreamKey));
  }finally{await gate.close();await new Promise(r=>upstream.close(r));}
});
test('refuses weak or shared remote credentials before opening a port',async()=>{
  await assert.rejects(startRemoteGateway({appKey:'demo',deviceKey,upstreamKey}));
  await assert.rejects(startRemoteGateway({appKey,deviceKey:appKey,upstreamKey}));
});
