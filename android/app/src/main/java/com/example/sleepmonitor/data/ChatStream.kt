package com.example.sleepmonitor.data

import org.json.JSONObject
import java.io.BufferedReader

/** A partial reply is display-only until the server explicitly confirms completion. */
internal suspend fun readChatStream(reader:BufferedReader,onDelta:suspend (String)->Unit):JSONObject {
    var received=0
    while(true){
        val line=reader.readLine()?:throw ApiException(502,"Kết nối bị ngắt trước khi AI trả lời xong. Hãy thử lại.")
        if(line.isBlank())continue
        if(line.length>100000)throw ApiException(502,"Phản hồi AI không hợp lệ.")
        val frame=runCatching{JSONObject(line)}.getOrElse{throw ApiException(502,"Phản hồi AI không hợp lệ. Hãy thử lại.")}
        when(frame.optString("type")){
            "delta"->{
                val text=frame.getString("text")
                received+=text.length
                if(received>16000)throw ApiException(502,"Câu trả lời quá dài. Hãy thử câu hỏi ngắn hơn.")
                if(text.isNotEmpty())onDelta(text)
            }
            "done"->{
                if(frame.optString("id").isBlank()||frame.optString("text").isBlank()||frame.optString("text").length>16000)
                    throw ApiException(502,"AI chưa tạo được câu trả lời. Hãy thử lại.")
                return frame
            }
            "error"->throw ApiException(frame.optInt("status",502),frame.optString("error","AI gặp lỗi khi đang trả lời. Hãy thử lại."))
            else->throw ApiException(502,"Phản hồi AI không hợp lệ. Hãy thử lại.")
        }
    }
}
