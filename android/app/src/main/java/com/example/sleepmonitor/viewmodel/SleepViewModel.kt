package com.example.sleepmonitor.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.sleepmonitor.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.*
import org.json.JSONObject
import java.util.UUID
import com.example.sleepmonitor.reminders.SleepReminders

data class AppState(val sessions:List<SleepSession> = emptyList(),val environment:Environment=Environment(),val settings:Settings=Settings(),
    val connection:Connection=Connection(demo=true),val notice:String="",val syncing:Boolean=false,val ratingId:String?=null,val motionEnabled:Boolean=true,
    val detail:Environment=Environment(),val detailId:String?=null,val serverReachable:Boolean=false,val command:String="",val savedId:String?=null,val now:Long=System.currentTimeMillis(),
    val appearanceMode:AppearanceMode=AppearanceMode.AUTOMATIC,val aiPersonalization:Boolean=true)
class SleepViewModel(application:Application):AndroidViewModel(application){
    private val local=LocalStore(application)
    private val mutex=Mutex()
    private val _state=MutableStateFlow(AppState(connection=local.connection(),motionEnabled=local.cache("motion")!="false",appearanceMode=AppearanceMode.fromStored(local.cache("appearance")),aiPersonalization=local.cache("ai-personalization")!="false"))
    val state:StateFlow<AppState> = _state.asStateFlow()
    private fun repo()=LocalSleepRepository(local,_state.value.connection)
    private fun iot():IoTRepository=if(_state.value.connection.demo)MockIoTRepository()else RemoteIoTRepository(Api(_state.value.connection))
    init{
        seedDemo(local)
        _state.update{it.copy(sessions=repo().getSleepSessions(),settings=local.cache("settings")?.let{s->runCatching{JSONObject(s).settings()}.getOrNull()}?:Settings(),
            environment=if(_state.value.connection.demo)Environment() else local.cache("env")?.let{s->runCatching{JSONObject(s).environment()}.getOrNull()}?:Environment())}
        viewModelScope.launch {while(isActive){refresh();delay(5000)}}
    }
    private fun work(block:suspend()->Unit){viewModelScope.launch(Dispatchers.IO){mutex.withLock{try{block();SleepReminders.schedule(getApplication())}catch(e:Exception){_state.update{it.copy(notice=e.message?:"Có lỗi xảy ra",syncing=false)}}}}}
    private suspend fun refresh(){withContext(Dispatchers.IO){mutex.withLock{
        _state.update{it.copy(syncing=true,now=System.currentTimeMillis())}
        try{
            repo().synchronize()
            val env=iot().getCurrentEnvironment()
            var settings=_state.value.settings
            if(!_state.value.connection.demo){val api=Api(_state.value.connection);settings=JSONObject(api.request("/settings")).settings();local.cache("settings",settings.json().toString());local.cache("env",api.request("/environment"))}
            _state.update{it.copy(sessions=repo().getSleepSessions(),environment=env,settings=settings,serverReachable=true,syncing=false)}
        }catch(e:Exception){
            _state.update{it.copy(sessions=repo().getSleepSessions(),serverReachable=false,syncing=false,environment=it.environment.copy(status=it.environment.status.copy(online=false)),notice=if(e is ApiException)e.message?:"Lỗi đồng bộ" else it.notice)}
        }
    }}}
    fun consumeSaved(){_state.update{it.copy(savedId=null)}}
    fun motion(enabled:Boolean){local.cache("motion",enabled.toString());_state.update{it.copy(motionEnabled=enabled)}}
    fun appearance(mode:AppearanceMode){local.cache("appearance",mode.name);_state.update{it.copy(appearanceMode=mode)}}
    fun personalizeAi(enabled:Boolean){local.cache("ai-personalization",enabled.toString());_state.update{it.copy(aiPersonalization=enabled)}}
    fun openRating(id:String){if(_state.value.sessions.any{it.id==id&&it.end!=null&&!it.deleted})_state.update{it.copy(ratingId=id)}}
    fun dismissNotice(){_state.update{it.copy(notice="")}}
    fun startSleep()=work{
        check(repo().getSleepSessions().none{it.end==null&&!it.deleted}){"Đã có phiên đang ngủ"}
        val s=SleepSession(UUID.randomUUID().toString(),System.currentTimeMillis());repo().saveSleepSession(s)
        _state.update{it.copy(sessions=repo().getSleepSessions(),notice="")}
    }
    fun wakeUp()=work{
        val s=repo().getSleepSessions().firstOrNull{it.end==null&&!it.deleted}?:return@work
        repo().saveSleepSession(s.copy(end=maxOf(System.currentTimeMillis(),s.start+1000)))
        _state.update{it.copy(sessions=repo().getSleepSessions(),ratingId=s.id)}
    }
    fun closeRating(){_state.update{it.copy(ratingId=null)}}
    fun rate(id:String,quality:Int,notes:String,interrupted:Boolean)=work{
        val s=repo().getSleepSessions().first{it.id==id};repo().saveSleepSession(s.copy(quality=quality,notes=notes.take(2000),interrupted=interrupted))
        _state.update{it.copy(sessions=repo().getSleepSessions(),ratingId=null,notice="Đã lưu đánh giá")}
    }
    fun saveManual(id:String?,start:Long,end:Long,quality:Int,notes:String,interrupted:Boolean)=work{
        require(end>start && end-start<=48*3600000 && end<=System.currentTimeMillis()+60000){"Kiểm tra ngày/giờ: phiên phải dài hơn 0, không quá 48 giờ và không ở tương lai"}
        val old=repo().getSleepSessions().find{it.id==id}
        val s=SleepSession(old?.id?:UUID.randomUUID().toString(),start,end,quality,notes.take(2000),interrupted,old?.version?:0)
        repo().saveSleepSession(s);_state.update{it.copy(sessions=repo().getSleepSessions(),notice="Đã lưu phiên ngủ",savedId=s.id)}
    }
    fun deleteSession(id:String)=work{
        val s=repo().getSleepSessions().first{it.id==id};repo().saveSleepSession(s.copy(deleted=true));_state.update{it.copy(sessions=repo().getSleepSessions(),notice="Đã xóa phiên ngủ")}
    }
    fun connect(connection:Connection)=work{
        require(connection.url.startsWith("http://")||connection.url.startsWith("https://")){"Địa chỉ phải bắt đầu http:// hoặc https://"}
        if(!connection.demo){
            val api=Api(connection);api.request("/settings")
            val nextId=JSONObject(api.request("/identity")).getString("serverId")
            require(nextId.isNotBlank()){"Máy chủ chưa xác nhận kết nối"}
            val old=_state.value.connection;val oldIdKey="server-identity-"+connectionFingerprint(old)
            val oldId=local.cache(oldIdKey)?:if(!old.demo)runCatching{JSONObject(Api(old).request("/identity")).getString("serverId")}.getOrNull()else null
            if(oldId==nextId)local.preserveChatConnection(old,connection)
            local.cache("server-identity-"+connectionFingerprint(connection),nextId)
        }
        local.connection(connection);_state.update{it.copy(connection=connection,environment=Environment(),detail=Environment(),notice="Đã lưu kết nối",serverReachable=false)}
        _state.update{it.copy(sessions=repo().getSleepSessions())}
    }
    fun saveSettings(settings:Settings)=work{
        require(settings.temperatureMin<settings.temperatureMax && settings.humidityMin<settings.humidityMax && settings.humidityMin>=0 && settings.humidityMax<=100 && settings.lightMax>=0){"Ngưỡng môi trường không hợp lệ"}
        if(!_state.value.connection.demo)Api(_state.value.connection).request("/settings","PUT",settings.json())
        local.cache("settings",settings.json().toString());_state.update{it.copy(settings=settings,notice="Đã lưu cấu hình")}
    }
    fun detail(id:String)=work{
        _state.update{it.copy(detail=Environment(),detailId=id)}
        val s=repo().getSleepSessions().first{it.id==id};val end=s.end?:System.currentTimeMillis()
        val cacheKey="detail-${_state.value.connection.demo}-$id"
        if(_state.value.connection.demo)_state.update{it.copy(detail=iot().getSensorHistory(s.start,end))}
        else try{val raw=Api(_state.value.connection).request("/detail?start=${s.start}&end=$end");local.cache(cacheKey,raw);_state.update{it.copy(detail=JSONObject(raw).environment())}}
        catch(e:Exception){val cached=local.cache(cacheKey);_state.update{it.copy(detail=cached?.let{r->JSONObject(r).environment()}?:Environment(),notice="Không kết nối máy chủ; đang xem dữ liệu đã lưu nếu có")}}
    }
    fun lamp(brightness:Int)=work{
        if(_state.value.connection.demo){_state.update{it.copy(command="Độ sáng đã chọn: $brightness%")};return@work}
        val api=Api(_state.value.connection);val id=JSONObject(api.request("/commands","POST",JSONObject().put("brightness",brightness))).getString("id")
        _state.update{it.copy(command="Đang chờ đèn xác nhận…")}
        var finished=false
        repeat(17){if(!finished){delay(1000);val cmd=JSONObject(api.request("/commands/$id"));when(cmd.getString("state")){
            "applied"->{_state.update{it.copy(command="Đèn đã xác nhận $brightness%")};finished=true}
            "failed","expired"->{_state.update{it.copy(command="Đèn chưa thực hiện lệnh. Kiểm tra kết nối ESP-NOW.")};finished=true}
        }}}
        if(!finished)_state.update{it.copy(command="Hết thời gian chờ đèn")}
    }
    fun reloadServer()=work{
        check(!_state.value.connection.demo){"Chỉ áp dụng chế độ thiết bị thật"}
        val raw=Api(_state.value.connection).request("/sessions")
        local.resetLive();org.json.JSONArray(raw).mapObjects{it.session()}.forEach{local.put(false,it)}
        _state.update{it.copy(sessions=repo().getSleepSessions(),notice="Đã tải lại bản máy chủ")}
    }
}
