import http from 'node:http';
import {timingSafeEqual} from 'node:crypto';

function equal(a,b){const x=Buffer.from(String(a||'')),y=Buffer.from(String(b));return x.length===y.length&&timingSafeEqual(x,y);}
const appRoutes=[
  ['GET',/^\/api\/(identity|sessions|environment|detail|settings|ai\/status|ai\/voice|commands\/[A-Za-z0-9_-]+)$/],
  ['PUT',/^\/api\/(settings|sessions\/[A-Za-z0-9_-]{1,100})$/],
  ['POST',/^\/api\/(commands|ai\/chat(?:\/stream)?)$/],
];
const deviceRoutes=[['POST',/^\/api\/(device\/(status|environment)|ai\/voice)$/]];

/** Internet ingress only. LAN credentials never authenticate here; device tokens cannot read a diary. */
export async function startRemoteGateway({port=8081,upstreamPort=8080,upstreamKey,appKey,deviceKey}){
  if(!appKey||!deviceKey||appKey.length<32||deviceKey.length<32||appKey===deviceKey||appKey===upstreamKey||deviceKey===upstreamKey)
    throw new Error('Remote access requires independent, randomly generated app and device keys.');
  const windows=new Map();
  const allow=(bucket,limit)=>{
    const now=Date.now();let w=windows.get(bucket);
    if(!w||now-w.time>=60000){w={time:now,count:0};windows.set(bucket,w);}
    if(windows.size>2048)for(const [key,v] of windows)if(now-v.time>=60000)windows.delete(key);
    return ++w.count<=limit;
  };
  const server=http.createServer(async(req,res)=>{
    res.setHeader('Cache-Control','no-store');
    res.setHeader('X-Content-Type-Options','nosniff');
    const send=(status,error)=>{res.writeHead(status,{'Content-Type':'application/json; charset=utf-8'});res.end(JSON.stringify({error}));};
    if(req.url==='/health'&&req.method==='GET'){
      res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify({ok:true,service:'Sleep Remote Gateway'}));return;
    }
    if(!req.url?.startsWith('/api/')||req.url.includes('%')||req.url.includes('\\')||req.url.includes('..')){send(404,'Không tìm thấy đường dẫn');return;}
    const role=equal(req.headers['x-api-key'],appKey)?'app':equal(req.headers['x-api-key'],deviceKey)?'device':null;
    if(!role){
      // Bound memory even when an attacker varies a supplied forwarding header.
      const ip=String(req.headers['cf-connecting-ip']||req.socket.remoteAddress).slice(0,64);
      const bucket=windows.size<2000?'invalid:'+ip:'invalid:overflow';
      send(allow(bucket,30)?401:429,'Khóa kết nối không hợp lệ');return;
    }
    const path=req.url.split('?')[0];
    const routes=role==='app'?appRoutes:deviceRoutes;
    if(!routes.some(([method,pattern])=>req.method===method&&pattern.test(path))){send(403,'Khóa này không có quyền thực hiện thao tác');return;}
    if(!allow(role,role==='app'?360:120)){res.setHeader('Retry-After','60');send(429,'Vui lòng thử lại sau');return;}
    const limit=path==='/api/ai/voice'&&req.method==='POST'?256044:65536;
    if(Number(req.headers['content-length'])>limit){send(413,'Yêu cầu quá lớn');return;}
    const chunks=[];let length=0;
    try{
      for await(const chunk of req){length+=chunk.length;if(length>limit){send(413,'Yêu cầu quá lớn');return;}chunks.push(chunk);}
    }catch{if(!res.destroyed)send(400,'Yêu cầu chưa hoàn tất');return;}
    const headers={'X-API-Key':upstreamKey,'Content-Type':path==='/api/ai/voice'?'audio/wav':'application/json','Content-Length':length,'Accept':'application/x-ndjson, application/json'};
    if(path==='/api/ai/voice'&&req.headers['x-request-id'])headers['X-Request-ID']=String(req.headers['x-request-id']).slice(0,100);
    const upstream=http.request({host:'127.0.0.1',port:upstreamPort,path:req.url,method:req.method,headers},reply=>{
      res.writeHead(reply.statusCode,{'Content-Type':reply.headers['content-type']||'application/json','Cache-Control':'no-store, no-transform','X-Accel-Buffering':'no'});
      res.flushHeaders();reply.pipe(res);
      reply.on('error',()=>res.destroy());
    });
    upstream.setTimeout(160000,()=>upstream.destroy(new Error('timeout')));
    upstream.on('error',()=>{if(!res.headersSent&&!res.destroyed)send(502,'Máy chủ đang khởi động hoặc chưa phản hồi');else res.destroy();});
    res.on('close',()=>{if(!res.writableEnded)upstream.destroy();});
    upstream.end(Buffer.concat(chunks));
  });
  server.headersTimeout=10000;server.requestTimeout=20000;server.maxHeadersCount=40;
  await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(port,'127.0.0.1',resolve);});
  return {port:server.address().port,close:()=>new Promise(resolve=>{server.close(resolve);server.closeIdleConnections();})};
}
