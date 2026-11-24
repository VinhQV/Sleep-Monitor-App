package com.example.sleepmonitor.data
import java.net.HttpURLConnection
import java.net.URI
import org.json.*

class ApiException(val status:Int,message:String):Exception(message)
class Api(private val connection:Connection){
    fun request(path:String,method:String="GET",body:JSONObject?=null):String {
        val uri=URI(connection.url.trimEnd('/')+"/api"+path)
        require(uri.scheme in listOf("http","https") && uri.host!=null){"Nhập địa chỉ HTTPS từ xa hoặc HTTP trong mạng nội bộ"}
        val c=uri.toURL().openConnection() as HttpURLConnection
        try {
            c.instanceFollowRedirects=false
            c.requestMethod=method;c.connectTimeout=8000;c.readTimeout=12000;c.setRequestProperty("X-API-Key",connection.key)
            if(body!=null){c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}}
            val status=c.responseCode;val data=(if(status in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}?:""
            if(status !in 200..299)throw ApiException(status,runCatching{JSONObject(data).getString("error")}.getOrDefault("HTTP $status"))
            return data
        }finally{c.disconnect()}
    }
}
