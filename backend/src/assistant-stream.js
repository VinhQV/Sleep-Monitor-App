import { Problem } from './domain.js';

// Decode bytes incrementally: a Vietnamese character can cross network chunks.
export async function readOllamaStream(body, onDelta) {
  if (!body) throw new Problem(502, 'AI chưa gửi câu trả lời.');
  const decoder = new TextDecoder('utf-8', { fatal: true });
  let buffer = '', answer = '', finished = false;
  const consume = line => {
    if (!line.trim()) return;
    let frame;
    try { frame = JSON.parse(line); } catch { throw new Problem(502, 'Phản hồi AI không hợp lệ. Hãy thử lại.'); }
    if (frame.error) throw new Problem(502, 'AI gặp lỗi khi đang trả lời. Hãy thử lại.');
    if (frame.done_reason === 'length') throw new Problem(502, 'Câu trả lời chưa hoàn tất. Hãy thử câu hỏi ngắn hơn.');
    const delta = frame.message?.content;
    if (typeof delta === 'string' && delta) {
      if (answer.length + delta.length > 16000) throw new Problem(502, 'Câu trả lời quá dài. Hãy thử câu hỏi ngắn hơn.');
      answer += delta;
      onDelta(delta);
    }
    // Thinking tokens and metadata never become user-visible answer text.
    if (frame.done === true) finished = true;
  };
  for await (const chunk of body) {
    buffer += decoder.decode(chunk, { stream: true });
    let newline;
    while ((newline = buffer.indexOf('\n')) !== -1) {
      consume(buffer.slice(0, newline));
      buffer = buffer.slice(newline + 1);
      if (finished) return answer;
    }
    if (buffer.length > 65536) throw new Problem(502, 'Phản hồi AI không hợp lệ.');
  }
  buffer += decoder.decode();
  consume(buffer);
  if (!finished) throw new Problem(502, 'Kết nối bị ngắt trước khi AI trả lời xong. Hãy thử lại.');
  return answer;
}
