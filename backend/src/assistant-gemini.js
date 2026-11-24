import { Problem } from './domain.js';

export const GEMINI_BASE='https://generativelanguage.googleapis.com/v1beta/models/';

export function geminiFailure(status){
  if(status===429)return new Problem(429,'Gemini đã hết hạn mức hoặc đang giới hạn lượt dùng. Hãy đợi rồi thử lại; app không tự chuyển sang gói trả phí.');
  if(status===401||status===403)return new Problem(503,'Khóa Gemini chưa hợp lệ hoặc chưa có quyền truy cập. Kiểm tra cấu hình cloud trên máy tính.');
  if(status===404)return new Problem(503,'Model Gemini chưa khả dụng cho tài khoản này. Kiểm tra model trong cấu hình cloud.');
  return new Problem(502,'Gemini chưa phản hồi thành công. Hãy thử lại sau.');
}

function visibleText(frame){
  if(frame.error)throw geminiFailure(Number(frame.error.code)||502);
  if(frame.promptFeedback?.blockReason)throw new Problem(502,'Gemini không thể trả lời yêu cầu này. Hãy diễn đạt lại câu hỏi.');
  const candidate=frame.candidates?.[0];
  if(candidate?.finishReason&&candidate.finishReason!=='STOP'){
    throw new Problem(502,candidate.finishReason==='MAX_TOKENS'?'Câu trả lời chưa hoàn tất. Hãy thử câu hỏi ngắn hơn.':'Gemini không thể hoàn tất câu trả lời. Hãy diễn đạt lại câu hỏi.');
  }
  return {text:(candidate?.content?.parts||[]).filter(p=>!p.thought&&typeof p.text==='string').map(p=>p.text).join(''),done:candidate?.finishReason==='STOP'};
}

export async function readGeminiStream(body,onDelta){
  if(!body)throw new Problem(502,'Gemini chưa gửi câu trả lời.');
  const decoder=new TextDecoder('utf-8',{fatal:true});
  let buffer='',data=[],answer='',finished=false;
  const event=()=>{
    if(!data.length)return;
    let frame;
    try{frame=JSON.parse(data.join('\n'));}catch{throw new Problem(502,'Phản hồi Gemini không hợp lệ.');}
    data=[];
    const chunk=visibleText(frame);
    if(answer.length+chunk.text.length>16000)throw new Problem(502,'Câu trả lời quá dài. Hãy thử câu hỏi ngắn hơn.');
    if(chunk.text){answer+=chunk.text;onDelta(chunk.text);}
    finished ||= chunk.done;
  };
  const line=value=>{
    value=value.replace(/\r$/,'');
    if(value===''){event();return;}
    if(value.startsWith('data:'))data.push(value.slice(5).replace(/^ /,''));
    if(data.reduce((n,s)=>n+s.length,0)>100000)throw new Problem(502,'Phản hồi Gemini không hợp lệ.');
  };
  for await(const bytes of body){
    buffer+=decoder.decode(bytes,{stream:true});let index;
    while((index=buffer.indexOf('\n'))>=0){line(buffer.slice(0,index));buffer=buffer.slice(index+1);}
    if(buffer.length>100000)throw new Problem(502,'Phản hồi Gemini không hợp lệ.');
  }
  buffer+=decoder.decode();if(buffer)line(buffer);event();
  if(!finished)throw new Problem(502,'Kết nối bị ngắt trước khi Gemini trả lời xong. Hãy thử lại.');
  return answer;
}

export async function replyGemini(config,messages,instructions,signal,onDelta,fetcher){
  const url=GEMINI_BASE+encodeURIComponent(config.model)+(onDelta?':streamGenerateContent?alt=sse':':generateContent');
  const response=await fetcher(url,{
    method:'POST',headers:{'Content-Type':'application/json','x-goog-api-key':config.geminiApiKey},signal,
    body:JSON.stringify({systemInstruction:{parts:[{text:instructions}]},contents:messages.map(m=>({role:m.role==='assistant'?'model':'user',parts:[{text:m.content}]})),generationConfig:{maxOutputTokens:1024}})
  });
  if(!response.ok)throw geminiFailure(response.status);
  if(onDelta)return readGeminiStream(response.body,onDelta);
  const result=visibleText(await response.json());
  if(!result.done)throw new Problem(502,'Câu trả lời Gemini chưa hoàn tất. Hãy thử lại.');
  return result.text;
}
