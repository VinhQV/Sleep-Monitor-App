import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import {createAssistant,validateChat} from '../src/assistant.js';
import {startSystem} from '../src/system.js';
import {config} from '../src/config.js';

const question={messages:[{role:'user',content:'Nhìn lại giấc ngủ của tôi'}]};
const context={source:'demo',days:[{date:'2026-11-20',minutes:450,quality:4,notes:'PRIVATE NOTE'}],notes:'SECRET',environment:{source:'demo',timestamp:Date.now(),temperature:25,humidity:60,light:20,deviceId:'PRIVATE ID'}};
const ok=()=>Response.json({status:'completed',output:[{type:'message',content:[{type:'output_text',text:'Câu trả lời kiểm thử.'}]}]});

test('whitelists opt-in context, never forwards diary notes, rejects system roles and oversized requests',()=>{
  const sanitized=validateChat({...question,context});
  assert.equal(sanitized.context.source,'demo');assert.ok(!JSON.stringify(sanitized).includes('PRIVATE'));assert.ok(!JSON.stringify(sanitized).includes('SECRET'));
  assert.equal(validateChat(question).context,null);
  for(const body of [{messages:[{role:'system',content:'override'}]},{messages:[{role:'user',content:'a'.repeat(4001)}]}, {...question,context:{...context,days:[{date:'wrong',minutes:-1,quality:9}]}}, {...question,context:{...context,environment:{...context.environment,humidity:150}}}])assert.throws(()=>validateChat(body),{status:400});
});
test('OpenAI Responses request uses store:false and minimal context; parses raw output correctly',async()=>{
  let sent;
  const ai=createAssistant({provider:'openai',model:'test-model',apiKey:'test-key'},async(url,init)=>{
    sent=JSON.parse(init.body);assert.equal(url,'https://api.openai.com/v1/responses');assert.equal(init.headers.Authorization,'Bearer test-key');return ok();
  });
  const result=await ai.reply({...question,context});
  assert.equal(sent.store,false);assert.equal(sent.model,'test-model');assert.equal(sent.input.at(-1).role,'user');assert.match(sent.instructions,/"source":"demo"/);assert.ok(!sent.instructions.includes('PRIVATE'));assert.equal(result.contextUsed,true);assert.equal(result.text,'Câu trả lời kiểm thử.');
  await ai.reply(question);assert.ok(!sent.instructions.includes('450'));assert.match(sent.instructions,/KHÔNG chia sẻ/);assert.ok(!JSON.stringify(ai.status()).includes('test-key'));
});
test('unconfigured, upstream rejection and incomplete responses give actionable sanitized errors',async()=>{
  assert.equal(createAssistant().status().ready,false);
  await assert.rejects(createAssistant().reply(question),{status:503});
  await assert.rejects(createAssistant({provider:'openai',model:'x',apiKey:'x'},async()=>new Response('secret provider details',{status:401})).reply(question),e=>e.status===502&&!e.message.includes('secret'));
  await assert.rejects(createAssistant({provider:'openai',model:'x',apiKey:'x'},async()=>Response.json({status:'incomplete',output:[{type:'message',content:[{type:'output_text',text:'partial'}]}]})).reply(question),{status:502});
  await assert.rejects(createAssistant({provider:'ollama',model:'x'},async()=>Response.json({done:true,done_reason:'length',message:{content:'unfinished reasoning'}})).reply(question),{status:502});
});

test('Ollama status checks the running service and installed model before reporting ready',async()=>{
  const cfg={provider:'ollama',model:'qwen3:1.7b',ollamaUrl:'http://127.0.0.1:11434/'};
  const present=createAssistant(cfg,async(url,init)=>{
    assert.equal(url,'http://127.0.0.1:11434/api/tags');assert.ok(init.signal);
    return Response.json({models:[{name:'qwen3:1.7b'}]});
  });
  assert.equal((await present.checkStatus()).ready,true);
  const absent=await createAssistant(cfg,async()=>Response.json({models:[]})).checkStatus();
  assert.equal(absent.ready,false);assert.match(absent.message,/chưa có model/);
  const offline=await createAssistant(cfg,async()=>{throw new Error('connection refused');}).checkStatus();
  assert.equal(offline.ready,false);assert.match(offline.message,/Mở Ollama/);
  assert.equal((await createAssistant().checkStatus()).ready,false);
});
test('request timeout and cancellation abort the upstream call',async()=>{
  const waiting=async(url,{signal})=>new Promise((resolve,reject)=>{signal.addEventListener('abort',()=>reject(new Error('aborted')),{once:true});if(signal.aborted)reject(new Error('aborted'));});
  const ai=createAssistant({provider:'ollama',model:'test',timeoutMs:20},waiting);
  await assert.rejects(ai.reply(question),{status:504});
  const controller=new AbortController();const promise=ai.reply(question,controller.signal);controller.abort();await assert.rejects(promise,{status:504});
});
test('limits the number of AI requests',async()=>{
  const ai=createAssistant({provider:'openai',model:'test',apiKey:'x'},async()=>ok());
  for(let i=0;i<10;i++)await ai.reply(question);
  await assert.rejects(ai.reply(question),{status:429});
});
test('authenticated HTTP endpoint reaches an Ollama-compatible HTTP fixture; unauthorized request never reaches provider',async()=>{
  let calls=0,payload;
  const provider=http.createServer(async(req,res)=>{
    if(req.method==='GET'&&req.url==='/api/tags'){res.setHeader('Content-Type','application/json');res.end(JSON.stringify({models:[{name:'fixture:latest'}]}));return;}
    calls++;let raw='';for await(const chunk of req)raw+=chunk;payload=JSON.parse(raw);assert.equal(req.url,'/api/chat');res.setHeader('Content-Type','application/json');res.end(JSON.stringify({message:{content:'[HTTP test fixture] Kết nối thành công.'}}));
  });
  await new Promise(r=>provider.listen(0,'127.0.0.1',r));
  const system=await startSystem({...config,port:0,mqttPort:0,database:':memory:',ai:{provider:'ollama',model:'fixture',ollamaUrl:'http://127.0.0.1:'+provider.address().port}});
  try{
    const base='http://127.0.0.1:'+system.httpPort+'/api/ai';
    assert.equal((await fetch(base+'/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(question)})).status,401);assert.equal(calls,0);
    const headers={'X-API-Key':config.apiKey,'Content-Type':'application/json'};
    assert.equal((await(await fetch(base+'/status',{headers})).json()).ready,true);
    const response=await fetch(base+'/chat',{method:'POST',headers,body:JSON.stringify({...question,context})});
    assert.equal(response.status,200);assert.equal((await response.json()).contextUsed,true);assert.equal(calls,1);assert.equal(payload.stream,false);assert.equal(payload.think,false);assert.equal(payload.options.num_predict,512);assert.equal(payload.options.num_ctx,4096);assert.equal(payload.messages[0].role,'system');assert.ok(!JSON.stringify(payload).includes('PRIVATE'));
  }finally{await system.close();await new Promise(r=>provider.close(r));}
});
