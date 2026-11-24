import test from 'node:test';
import assert from 'node:assert/strict';
import {createAssistant} from '../src/assistant.js';
import {readGeminiStream} from '../src/assistant-gemini.js';

const config={provider:'gemini',model:'test-model',geminiApiKey:'fixture-only'};
const question={cloudConversation:'gemini-v1',messages:[{role:'user',content:'Chào bạn'}]};

test('Gemini receives product guidance and only validated style and optional topic',async()=>{
  const prompts=[];
  const ai=createAssistant(config,async(url,options)=>{
    const payload=JSON.parse(options.body);
    prompts.push(payload.systemInstruction.parts[0].text);
    assert.ok(!options.body.includes('PRIVATE'));
    return Response.json({candidates:[{content:{parts:[{text:'OK'}]},finishReason:'STOP'}]});
  });
  await ai.reply({...question,notes:'PRIVATE'});
  await ai.reply({...question,replyStyle:'steps',coachTopic:'ROOM',privateTip:'PRIVATE'});
  assert.match(prompts[0],/Ghi ngủ/);
  assert.match(prompts[0],/ngoài phạm vi/);
  assert.doesNotMatch(prompts[0],/người dùng muốn ưu tiên/);
  assert.match(prompts[1],/ba bước có số thứ tự/);
  assert.match(prompts[1],/người dùng muốn ưu tiên sự thoải mái trong phòng/);
  for(const invalid of [{replyStyle:'PRIVATE'},{coachTopic:'PRIVATE'}]){
    await assert.rejects(ai.reply({...question,...invalid}),{status:400});
  }
  assert.equal(prompts.length,2);
});

test('cloud requires the compatible conversation marker before any provider request',async()=>{
  let calls=0;
  const assistant=createAssistant(config,async()=>{calls++;throw Error('unexpected request');});
  await assert.rejects(assistant.reply({messages:question.messages}),{status:400});
  assert.equal(calls,0);
});

test('Gemini personalizes with whitelisted diary data and does not receive notes or credentials',async()=>{
  let sent;
  const assistant=createAssistant(config,async(url,options)=>{
    sent=JSON.parse(options.body);
    return Response.json({candidates:[{content:{parts:[{text:'Bạn đã ghi 7 giờ ngủ.'}]},finishReason:'STOP'}]});
  });
  const result=await assistant.reply({...question,context:{source:'live',days:[{date:'2026-11-22',minutes:420,quality:4,notes:'PRIVATE NOTE'}],key:'PRIVATE KEY'}});
  assert.equal(result.contextUsed,true);
  assert.match(sent.systemInstruction.parts[0].text,/"minutes":420/);
  assert.ok(!JSON.stringify(sent).includes('PRIVATE'));
  assert.equal(assistant.status().contextAllowed,true);
});

test('cloud detects a newly configured key, forwards only chat, and streams visible UTF-8 text',async()=>{
  let key='',sent=0;
  const assistant=createAssistant({...config,get geminiApiKey(){return key;}},async(url,options)=>{
    assert.ok(url.startsWith('https://generativelanguage.googleapis.com/v1beta/models/test-model'));
    assert.equal(options.headers['x-goog-api-key'],key);
    if(!options.method)return Response.json({name:'models/test-model'});
    const payload=JSON.parse(options.body);
    assert.deepEqual(payload.contents,[{role:'user',parts:[{text:'Chào bạn'}]}]);
    assert.ok(!options.body.includes(key));assert.ok(url.endsWith('streamGenerateContent?alt=sse'));
    const frame=(parts,reason)=>'data: '+JSON.stringify({candidates:[{content:{parts},finishReason:reason}]})+'\r\n\r\n';
    const data=Buffer.from(frame([{text:'reasoning',thought:true},{text:'Chào '}])+frame([{text:'bạn'}],'STOP'));sent++;
    return new Response(new ReadableStream({start(control){for(const byte of data)control.enqueue(Uint8Array.of(byte));control.close();}}));
  });
  assert.equal((await assistant.checkStatus()).ready,false);
  key='fixture-secret';
  assert.equal((await assistant.checkStatus()).ready,true);
  const chunks=[];const answer=await assistant.reply(question,undefined,x=>chunks.push(x));
  assert.equal(answer.text,'Chào bạn');assert.deepEqual(chunks,['Chào ','bạn']);
  assert.equal(answer.contextUsed,false);assert.equal(sent,1);
});

test('cloud never treats an interrupted or blocked stream as complete and reports free tier quota',async()=>{
  for(const data of [{candidates:[{content:{parts:[{text:'Chưa xong'}]}}]},{promptFeedback:{blockReason:'SAFETY'}},{candidates:[{finishReason:'MAX_TOKENS'}]}]){
    await assert.rejects(readGeminiStream(new Response('data: '+JSON.stringify(data)+'\n\n').body,()=>{}),{status:502});
  }
  const ai=createAssistant(config,async()=>new Response('sensitive upstream detail',{status:429}));
  await assert.rejects(ai.reply(question),e=>e.status===429&&!e.message.includes('sensitive'));
});
