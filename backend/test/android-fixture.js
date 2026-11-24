// Explicit local integration fixture. NOT an AI model and never started by npm start.
import http from 'node:http';
import {startSystem} from '../src/system.js';
import {config} from '../src/config.js';
const provider=http.createServer(async(req,res)=>{
  if(req.method==='GET'&&req.url==='/api/tags'){res.setHeader('Content-Type','application/json');res.end(JSON.stringify({models:[{name:'integration-fixture:latest'}]}));return;}
  let raw='';for await(const chunk of req)raw+=chunk;
  const body=JSON.parse(raw);const prompt=body.messages.at(-1).content;
  if(prompt.includes('TEST_ERROR')){res.writeHead(429);res.end('{}');return;}
  const text='[KIỂM THỬ KẾT NỐI — không phải câu trả lời AI] Máy chủ đã nhận câu hỏi. '+(body.messages[0].content.includes('"days"')?'Có tóm tắt được cho phép.':'Không có dữ liệu nhật ký.');
  if(body.stream){res.setHeader('Content-Type','application/x-ndjson');res.write(JSON.stringify({message:{content:text.slice(0,30)},done:false})+'\n');}
  const timer=setTimeout(()=>{if(!res.destroyed){
    if(body.stream)res.end(JSON.stringify({message:{content:text.slice(30)},done:true})+'\n');
    else{res.setHeader('Content-Type','application/json');res.end(JSON.stringify({message:{content:text}}));}
  }},prompt.includes('TEST_CANCEL')?30000:300);
  res.on('close',()=>clearTimeout(timer));
});
await new Promise(r=>provider.listen(0,'127.0.0.1',r));
const system=await startSystem({...config,database:':memory:',ai:{provider:'ollama',model:'integration-fixture',ollamaUrl:'http://127.0.0.1:'+provider.address().port}});
console.log('TEST FIXTURE ONLY: API '+system.httpPort+', MQTT '+system.mqttPort+'. No real AI model.');
const close=async()=>{await system.close();provider.close();process.exit(0);};
process.on('SIGINT',close);process.on('SIGTERM',close);
