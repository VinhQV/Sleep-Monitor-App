package com.example.sleepmonitor.data
import android.content.*
import android.database.sqlite.*
import org.json.JSONObject

/** Two independent datasets prevent demo samples from ever syncing to real devices. */
class LocalStore(context:Context):SQLiteOpenHelper(context,"sleep-monitor.db",null,1) {
    override fun onCreate(db:SQLiteDatabase){
        db.execSQL("CREATE TABLE sessions(mode TEXT,id TEXT,json TEXT,dirty INTEGER,PRIMARY KEY(mode,id))")
        db.execSQL("CREATE TABLE cache(key TEXT PRIMARY KEY,value TEXT)")
    }
    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int) { error("Database migration required") }
    fun sessions(demo:Boolean):List<SleepSession>{
        val rows=mutableListOf<SleepSession>();readableDatabase.rawQuery("SELECT json,dirty FROM sessions WHERE mode=?",arrayOf(if(demo)"demo" else "live")).use { c->while(c.moveToNext())rows+=JSONObject(c.getString(0)).session().copy(dirty=c.getInt(1)==1) };return rows.sortedByDescending{it.start}
    }
    fun put(demo:Boolean,s:SleepSession){writableDatabase.insertWithOnConflict("sessions",null,ContentValues().apply{put("mode",if(demo)"demo" else "live");put("id",s.id);put("json",s.json().toString());put("dirty",if(s.dirty)1 else 0)},SQLiteDatabase.CONFLICT_REPLACE)}
    fun cache(key:String):String?=readableDatabase.rawQuery("SELECT value FROM cache WHERE key=?",arrayOf(key)).use{if(it.moveToFirst())it.getString(0)else null}
    fun cache(key:String,value:String){writableDatabase.insertWithOnConflict("cache",null,ContentValues().apply{put("key",key);put("value",value)},SQLiteDatabase.CONFLICT_REPLACE)}
    fun resetLive(){writableDatabase.delete("sessions","mode=?",arrayOf("live"))}
    fun connection():Connection=cache("connection")?.let { val j=JSONObject(it);Connection(j.getString("url"),j.getString("key"),j.getBoolean("demo")) }?:Connection(demo=false)
    fun connection(c:Connection){cache("connection",JSONObject().put("url",c.url).put("key",c.key).put("demo",c.demo).toString())}
    fun preserveChatConnection(old:Connection,next:Connection){
        val prefix="chat-"+connectionFingerprint(old);val target="chat-"+connectionFingerprint(next)
        if(prefix==target)return
        val values=mutableListOf<Pair<String,String>>()
        readableDatabase.rawQuery("SELECT key,value FROM cache WHERE key LIKE ?",arrayOf("$prefix%" )).use{c->while(c.moveToNext())values+=c.getString(0) to c.getString(1)}
        values.forEach{(key,value)->val destination=target+key.removePrefix(prefix);if(cache(destination)==null)cache(destination,value)}
    }
}
