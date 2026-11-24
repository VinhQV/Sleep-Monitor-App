import test from 'node:test';
import assert from 'node:assert/strict';
import express from 'express';
import {randomUUID} from 'node:crypto';
import {validateWav,transcribeVoice,registerVoice} from '../src/voice.js';

function wav(){
  const b=Buffer.alloc(32044);
  b.write('RIFF');b.writeUInt32LE(b.length-8,4);b.write('WAVEfmt ',8);
  b.writeUInt32LE(16,16);b.writeUInt16LE(1,20);b.writeUInt16LE(1,22);
  b.writeUInt32LE(16000,24);b.writeUInt32LE(32000,28);
  b.writeUInt16LE(2,32);b.writeUInt16LE(16,34);
  b.write('data',36);b.writeUInt32LE(b.length-44,40);
  return b;
}
const ai={provider:'gemini',model:'fixture',geminiApiKey:'fixture'};
test('voice validates WAV structure and rejects invalid duration and format',()=>{
  validateWav(wav());
  for(const b of [Buffer.alloc(12),Buffer.alloc(256045),Buffer.from(wav())]){
    if(b.length===32044)b.writeUInt32LE(8000,24);
    assert.throws(()=>validateWav(b),{status:400});
  }
});
test('transcription sends only audio to Gemini and handles silence and quota',async()=>{
  const text=await transcribeVoice(wav(),ai,undefined,async(url,options)=>{
    const p=JSON.parse(options.body);
    assert.equal(p.contents[0].parts[0].inlineData.mimeType,'audio/wav');
    assert.deepEqual(Buffer.from(p.contents[0].parts[0].inlineData.data,'base64'),wav());
    return Response.json({candidates:[{finishReason:'STOP',content:{parts:[{text:'{"transcript":"Chào bạn"}'}]}}]});
  });
  assert.equal(text,'Chào bạn');
  await assert.rejects(transcribeVoice(wav(),ai,undefined,async()=>new Response('',{status:429})),{status:429});
  await assert.rejects(transcribeVoice(wav(),ai,undefined,async()=>Response.json({
    candidates:[{finishReason:'STOP',content:{parts:[{text:'{"transcript":""}'}]}}]})),{status:422});
});
test('device voice saves complete answers, deduplicates retries and exposes inbox without audio',async()=>{
  const app=express(),data=new Map();let calls=0;
  registerVoice(app,{cfg:{ai},store:{get:k=>data.get(k),set:(k,v)=>data.set(k,v)},
    assistant:{reply:async body=>{
      calls++;assert.equal(body.cloudConversation,'gemini-v1');
      assert.equal(body.messages[0].content,'Chào bạn');
      return {text:'Chào bạn, mình có thể giúp gì về giấc ngủ?'};
    }},
    fetcher:async()=>Response.json({candidates:[{finishReason:'STOP',content:{parts:[{text:'{"transcript":"Chào bạn"}'}]}}]})});
  app.use((e,req,res,next)=>res.status(e.status||500).json({error:e.message}));
  const server=await new Promise(r=>{const s=app.listen(0,'127.0.0.1',()=>r(s));});
  const url='http://127.0.0.1:'+server.address().port+'/api/ai/voice';
  try{
    const headers={'Content-Type':'audio/wav','X-Request-ID':randomUUID()};
    const first=await fetch(url,{method:'POST',headers,body:wav()});
    assert.equal(first.status,200);
    const item=await first.json();
    const duplicate=await fetch(url,{method:'POST',headers,body:wav()});
    assert.deepEqual(await duplicate.json(),item);assert.equal(calls,1);
    assert.deepEqual((await (await fetch(url)).json()).items,[item]);
    assert.equal(JSON.stringify(item).includes('base64'),false);
    const invalid=await fetch(url,{method:'POST',headers,body:Buffer.alloc(10)});
    assert.equal(invalid.status,400);
  }finally{await new Promise(r=>server.close(r));}
});
