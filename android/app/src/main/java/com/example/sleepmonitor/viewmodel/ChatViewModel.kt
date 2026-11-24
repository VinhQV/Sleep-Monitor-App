package com.example.sleepmonitor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepmonitor.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

data class ChatState(val messages:List<ChatMessage> = emptyList(),val sending:Boolean=false,val partialAnswer:String="",val shareContext:Boolean=false,
    val status:AssistantStatus=AssistantStatus())

class ChatViewModel(application:Application):AndroidViewModel(application) {
    private val local=LocalStore(application)
    private val _focus=MutableStateFlow(CoachFocus.SCHEDULE)
    private val _style=MutableStateFlow(ReplyStyle.BRIEF)
    private val _useTopic=MutableStateFlow(false)
    val useTopic=_useTopic.asStateFlow()
    val focus=_focus.asStateFlow()
    val style=_style.asStateFlow()
    private val _state=MutableStateFlow(ChatState())
    val state:StateFlow<ChatState> = _state.asStateFlow()
    private var connection:Connection?=null
    private var cacheKey=""
    private var connectionCacheKey=""
    private var generation=0
    private var request:Job?=null
    private var statusRequest:Job?=null
    private var api:AssistantApi?=null

    suspend fun receiveVoice() {
        val c=connection?:return
        val key=cacheKey
        val token=generation
        if(key.isEmpty()||_state.value.sending||_state.value.status.provider!="gemini")return
        try {
            val result=withContext(Dispatchers.IO){AssistantApi(c).request("voice")}
            if(connection!=c||cacheKey!=key||generation!=token||_state.value.sending)return
            val seenKey=key+"-voice-seen"
            val seen=runCatching{JSONArray(local.cache(seenKey)?:"[]")}.getOrDefault(JSONArray())
            val ids=(0 until seen.length()).map{seen.getString(it)}.toMutableSet()
            val items=result.optJSONArray("items")?:return
            val incoming=mutableListOf<ChatMessage>()
            for(i in 0 until items.length()) {
                val item=items.getJSONObject(i)
                val id=item.getString("id")
                if(ids.add(id)) {
                    incoming+=ChatMessage("voice-"+id,"user",item.getString("transcript"))
                    incoming+=ChatMessage(item.getString("answerId"),"assistant",item.getString("text"))
                }
            }
            if(incoming.isNotEmpty()){
                _state.update{it.copy(messages=(it.messages+incoming).takeLast(40))}
                persist()
                local.cache(seenKey,JSONArray(ids.toList().takeLast(100)).toString())
            }
        }catch(e:CancellationException){throw e}catch(_:Exception){
            // Text chat remains available while the device inbox is unreachable.
        }
    }

    fun bind(c:Connection){
        if(connection==c){refreshStatus();return}
        cancel();generation++;statusRequest?.cancel();connection=c
        connectionCacheKey="chat-"+connectionFingerprint(c)
        val savedFocus=local.cache("$connectionCacheKey-focus")
        val savedStyle=local.cache("$connectionCacheKey-style")
        _focus.value=CoachFocus.entries.find{it.name==savedFocus}?:CoachFocus.SCHEDULE
        _style.value=ReplyStyle.entries.find{it.wire==savedStyle}?:ReplyStyle.BRIEF
        _useTopic.value=local.cache("$connectionCacheKey-use-topic")=="true"
        cacheKey=""
        _state.value=ChatState()
        refreshStatus()
    }
    fun chooseFocus(value:CoachFocus){
        if(connectionCacheKey.isEmpty())return
        local.cache("$connectionCacheKey-focus",value.name)
        _focus.value=value
    }
    fun chooseStyle(value:ReplyStyle){
        if(connectionCacheKey.isEmpty())return
        local.cache("$connectionCacheKey-style",value.wire)
        _style.value=value
    }
    fun useTopicInChat(enabled:Boolean){
        if(connectionCacheKey.isEmpty())return
        local.cache("$connectionCacheKey-use-topic",enabled.toString())
        _useTopic.value=enabled
    }
    private fun loadConversation(key:String,status:AssistantStatus){
        cacheKey=key
        val saved=runCatching{JSONObject(local.cache(cacheKey)?:"{}")}.getOrDefault(JSONObject())
        val messages=mutableListOf<ChatMessage>()
        saved.optJSONArray("messages")?.let{array->for(i in 0 until array.length()){
            val m=array.getJSONObject(i);messages+=ChatMessage(m.getString("id"),m.getString("role"),m.getString("text"),m.optString("error").takeIf{it.isNotEmpty()})
        }}
        _state.value=ChatState(messages=messages,shareContext=local.cache("ai-personalization")!="false",status=status)
    }
    fun refreshStatus(){
        val c=connection?:return
        statusRequest?.cancel()
        _state.update{it.copy(status=it.status.copy(checking=true))}
        statusRequest=viewModelScope.launch{
            val status=try{
                val j=withContext(Dispatchers.IO){AssistantApi(c).request("status")}
                AssistantStatus(j.optBoolean("ready"),j.optString("provider"),j.optString("message",if(j.optBoolean("ready"))"Sẵn sàng trò chuyện" else "AI chưa được kết nối trên máy chủ"),false)
            }catch(e:CancellationException){throw e}catch(e:Exception){AssistantStatus(message=if(e is ApiException)e.message?:"Lỗi kết nối" else "Chưa kết nối được máy chủ AI",checking=false)}
            if(connection==c){
                val key=when(status.provider){
                    "gemini"->connectionCacheKey+"-gemini-v1"
                    "ollama","openai"->connectionCacheKey
                    else->cacheKey
                }
                if(key.isNotEmpty()&&key!=cacheKey){cancel();loadConversation(key,status)}
                else _state.update{it.copy(status=status)}
            }
        }
    }
    private fun persist(){
        if(cacheKey.isEmpty())return
        val array=JSONArray();_state.value.messages.takeLast(40).forEach{m->array.put(JSONObject().put("id",m.id).put("role",m.role).put("text",m.text).put("error",m.error?:""))}
        local.cache(cacheKey,JSONObject().put("shareContext",_state.value.shareContext).put("messages",array).toString())
    }
    fun clear(){cancel();_state.update{it.copy(messages=emptyList())};persist()}
    fun share(enabled:Boolean){clear();_state.update{it.copy(shareContext=enabled)};persist()}
    fun send(text:String,app:AppState,retry:Boolean=false){
        val c=connection?:return
        if(_state.value.sending||!_state.value.status.ready||text.isBlank()||text.length>2000)return
        val token=generation
        val previous=if(retry)_state.value.messages.dropLast(1)else _state.value.messages
        val message=ChatMessage(UUID.randomUUID().toString(),"user",text.trim())
        _state.update{it.copy(messages=(previous+message).takeLast(40),sending=true,partialAnswer="")}
        // A persisted pending question becomes retryable if Android kills the process.
        _state.update{it.copy(messages=it.messages.dropLast(1)+message.copy(error="Câu hỏi chưa hoàn tất. Bạn có thể thử lại."))};persist()
        _state.update{it.copy(messages=it.messages.dropLast(1)+message)}
        val history=previous.filter{it.error==null}.takeLast(11).map{it.copy(text=it.text.take(4000))}.toMutableList()
        while(history.sumOf{it.text.length}+message.text.length>16000)history.removeAt(0)
        val body=JSONObject().put("messages",JSONArray().apply{(history+message).forEach{put(JSONObject().put("role",it.role).put("content",it.text))}})
        body.put("replyStyle",_style.value.wire)
        if(_useTopic.value)body.put("coachTopic",_focus.value.name)
        if(_state.value.status.provider=="gemini")body.put("cloudConversation","gemini-v1")
        if(local.cache("ai-personalization")!="false")body.put("context",assistantContext(app))
        val client=AssistantApi(c);api=client
        request=viewModelScope.launch{
            try{
                val answer=withContext(Dispatchers.IO){client.stream(body){delta->
                    withContext(Dispatchers.Main.immediate){if(token==generation)_state.update{it.copy(partialAnswer=it.partialAnswer+delta)}}
                }}
                if(token==generation)_state.update{it.copy(messages=(it.messages+ChatMessage(answer.getString("id"),"assistant",answer.getString("text"))).takeLast(40),sending=false,partialAnswer="")}
            }catch(e:CancellationException){throw e}catch(e:Exception){
                if(token==generation)_state.update{it.copy(messages=it.messages.dropLast(1)+message.copy(error=if(e is ApiException)e.message else "Không nhận được câu trả lời. Kiểm tra mạng rồi thử lại."),sending=false)}
            }finally{if(token==generation){api=null;_state.update{it.copy(sending=false,partialAnswer="")};persist()}}
        }
    }
    fun cancel(){
        generation++
        api?.cancel();request?.cancel();api=null
        if(_state.value.sending)_state.update{it.copy(sending=false,partialAnswer="",messages=it.messages.mapIndexed{i,m->if(i==it.messages.lastIndex)m.copy(error="Đã dừng trả lời. Bạn có thể thử lại.")else m})}
        persist()
    }
    override fun onCleared(){generation++;api?.cancel();local.close();super.onCleared()}
}
