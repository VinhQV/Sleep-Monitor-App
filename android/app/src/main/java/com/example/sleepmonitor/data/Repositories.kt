package com.example.sleepmonitor.data
import java.time.*
import java.util.UUID
import kotlin.math.sin
import org.json.*

interface SleepRepository {
    fun getSleepSessions():List<SleepSession>
    fun saveSleepSession(s:SleepSession)
    fun synchronize()
}
class LocalSleepRepository(private val local:LocalStore,private val connection:Connection):SleepRepository {
    override fun getSleepSessions()=local.sessions(connection.demo)
    override fun saveSleepSession(s:SleepSession)=local.put(connection.demo,s.copy(dirty=!connection.demo))
    override fun synchronize(){
        if(connection.demo)return
        val api=Api(connection)
        for(s in getSleepSessions().filter{it.dirty})local.put(false,JSONObject(api.request("/sessions/${s.id}","PUT",s.json())).session())
        for(s in JSONArray(api.request("/sessions")).mapObjects{it.session()})local.put(false,s)
    }
}
interface IoTRepository {
    fun getCurrentEnvironment():Environment
    fun getSensorHistory(start:Long,end:Long):Environment
}
class RemoteIoTRepository(private val api:Api):IoTRepository {
    override fun getCurrentEnvironment()=JSONObject(api.request("/environment")).environment()
    override fun getSensorHistory(start:Long,end:Long)=JSONObject(api.request("/detail?start=$start&end=$end")).environment()
}
class MockIoTRepository:IoTRepository {
    override fun getCurrentEnvironment():Environment {
        val now=System.currentTimeMillis();val rows=(0..47).map{sample(now-(47-it)*1800000L)}
        return Environment(rows.last(),rows,emptyList(),DeviceStatus(true,now,true,20,"demo",true))
    }
    override fun getSensorHistory(start:Long,end:Long):Environment {
        val rows=(0..24).map{sample(start+(end-start)*it/24)}
        return Environment(rows.last(),rows,listOf(SleepEvent("demo-event",start+(end-start)/2,"light_change","Giảm sáng theo lịch ngủ","demo")))
    }
    private fun sample(t:Long)=SensorSample("demo-$t",t,25.8+sin(t/1800000.0)*0.7,61+sin(t/3000000.0)*3,40+sin(t/2400000.0)*15,"demo")
}
fun seedDemo(local:LocalStore){
    // Remove only the original placeholder note, never text the user has edited or added.
    if(local.cache("sampleCopyV21")==null){
        local.sessions(true).filter{it.id in (0..6).map{i->"demo-$i"} && it.notes=="Dữ liệu mẫu để khám phá ứng dụng"}
            .forEach{local.put(true,it.copy(notes=""))}
        local.cache("sampleCopyV21","true")
    }
    if(local.cache("demoSeeded")!=null)return
    val durations=listOf(452,405,485,432,390,495,445)
    for(i in durations.indices){val end=LocalDate.now().minusDays((i+1).toLong()).atTime(6,42).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        local.put(true,SleepSession("demo-$i",end-durations[i]*60000L,end,listOf(4,3,5,4,2,5,4)[i],"",i==1))}
    local.cache("demoSeeded","true")
}
