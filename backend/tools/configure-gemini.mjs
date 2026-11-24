import { mkdir, rename, writeFile, rm } from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import { config } from '../src/config.js';
import { createAssistant } from '../src/assistant.js';

// Read from the masked prompt via stdin. Never put the key in argv, logs or the APK.
async function configure() {
  const input=[];
  for await(const bytes of process.stdin) input.push(bytes);
  const key=Buffer.concat(input).toString('utf8').trim();
  if(!key || /\s/.test(key)) throw new Error('Please paste a single API key.');
  if(config.ai.provider!=='gemini') throw new Error('Set AI_PROVIDER=gemini and AI_MODEL=gemini-3.5-flash-lite in backend/.env, then restart the gateway.');
  const assistant=createAssistant({...config.ai,geminiApiKey:key});
  const status=await assistant.checkStatus();
  if(!status.ready) throw new Error(status.message);
  // Exercise the same streaming path as Android with a synthetic question before saving.
  const body={cloudConversation:'gemini-v1',messages:[{role:'user',content:'Chào bạn. Trả lời một câu ngắn bằng tiếng Việt.'}]};
  await assistant.reply(body,undefined,()=>{});
  const dir=new URL('../data/',import.meta.url);
  const temporary=new URL('.gemini-'+randomUUID()+'.tmp',dir);
  await mkdir(dir,{recursive:true});
  try {
    await writeFile(temporary,key,{mode:0o600,flag:'wx'});
    await rename(temporary,new URL('gemini.key',dir));
  } finally { await rm(temporary,{force:true}); }
  console.log('Gemini returned a complete streamed answer. Key saved for this gateway.');
}
configure().catch(error=>{ console.error(error.message); process.exitCode=1; });
