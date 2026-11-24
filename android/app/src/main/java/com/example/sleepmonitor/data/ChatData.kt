package com.example.sleepmonitor.data

import com.example.sleepmonitor.viewmodel.AppState
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference

data class ChatMessage(val id:String, val role:String, val text:String, val error:String?=null)
data class AssistantStatus(val ready:Boolean=false, val provider:String="", val message:String="Đang kiểm tra kết nối AI…", val checking:Boolean=true)

/** Personalization sends a seven-day summary and a timestamped room sample, never diary notes. */
fun assistantContext(state:AppState):JSONObject {
    val cutoff=LocalDate.now().minusDays(6)
    val sessions=state.sessions.filter{!it.deleted && it.end!=null && !it.date.isBefore(cutoff) && !it.date.isAfter(LocalDate.now())}
    val days=JSONArray()
    sessions.groupBy{it.date}.toSortedMap().forEach{(day,rows)->
        val qualities=rows.filter{it.quality>0}.map{it.quality}
        days.put(JSONObject().put("date",day.toString()).put("minutes",rows.sumOf{it.durationMinutes}.coerceAtMost(2880))
            .put("quality",if(qualities.isEmpty())0.0 else qualities.average()))
    }
    return JSONObject().put("source",if(state.connection.demo)"demo" else "live").put("days",days).apply{
        state.environment.current?.let{s->
            put("environment",JSONObject().put("timestamp",s.timestamp)
                .put("source",if(state.connection.demo)"demo" else s.source.takeIf{it in listOf("device","simulator")}?:"unknown")
                .put("temperature",s.temperature).put("humidity",s.humidity).put("light",s.light)
                .put("lightAdc",s.lightAdc).put("lightMillivolts",s.lightMillivolts)
                .put("online",state.connection.demo||(state.serverReachable&&state.environment.status.online)))
        }
    }
}

class AssistantApi(private val connection:Connection) {
    private val active=AtomicReference<HttpURLConnection?>()
    fun cancel(){active.getAndSet(null)?.disconnect()}
    suspend fun stream(body:JSONObject,onDelta:suspend (String)->Unit):JSONObject {
        val http=URL(connection.url.trimEnd('/')+"/api/ai/chat/stream").openConnection() as HttpURLConnection
        active.set(http)
        try{
            http.instanceFollowRedirects=false
            http.connectTimeout=8000;http.readTimeout=150000
            http.requestMethod="POST";http.doOutput=true
            http.setRequestProperty("X-API-Key",connection.key)
            http.setRequestProperty("Accept","application/x-ndjson")
            http.setRequestProperty("Content-Type","application/json; charset=utf-8")
            http.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
            val status=http.responseCode
            if(status !in 200..299){
                val raw=http.errorStream?.bufferedReader(Charsets.UTF_8)?.use{it.readText()}.orEmpty()
                val error=runCatching{JSONObject(raw).optString("error")}.getOrDefault("")
                throw ApiException(status,error.ifBlank{"Máy chủ chưa phản hồi thành công"})
            }
            return http.inputStream.bufferedReader(Charsets.UTF_8).use{readChatStream(it,onDelta)}
        }finally{active.compareAndSet(http,null);http.disconnect()}
    }
    fun request(path:String,body:JSONObject?=null):JSONObject {
        val http=URL(connection.url.trimEnd('/')+"/api/ai/"+path).openConnection() as HttpURLConnection
        active.set(http)
        try {
            http.instanceFollowRedirects=false
            http.connectTimeout=8000;http.readTimeout=if(body==null)15000 else 150000
            http.setRequestProperty("X-API-Key",connection.key)
            if(body!=null){http.requestMethod="POST";http.doOutput=true;http.setRequestProperty("Content-Type","application/json; charset=utf-8");http.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}}
            val status=http.responseCode
            val raw=(if(status in 200..299)http.inputStream else http.errorStream)?.bufferedReader(Charsets.UTF_8)?.use{it.readText()}.orEmpty()
            val json=runCatching{JSONObject(raw)}.getOrElse{JSONObject()}
            if(status !in 200..299)throw ApiException(status,json.optString("error","Máy chủ chưa phản hồi thành công"))
            return json
        }finally{active.compareAndSet(http,null);http.disconnect()}
    }
}
