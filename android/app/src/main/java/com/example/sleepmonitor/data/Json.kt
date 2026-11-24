package com.example.sleepmonitor.data
import org.json.*

fun SleepSession.json()=JSONObject().put("id",id).put("start",start).put("end",end?:JSONObject.NULL).put("quality",quality).put("notes",notes).put("interrupted",interrupted).put("version",version).put("deleted",deleted)
fun JSONObject.bool(key:String)=optBoolean(key,false)||optInt(key,0)==1
fun JSONObject.session()=SleepSession(getString("id"),getLong("start"),if(isNull("end"))null else getLong("end"),optInt("quality"),optString("notes"),bool("interrupted"),optInt("version"),bool("deleted"))
fun JSONObject.sample()=SensorSample(getString("id"),getLong("timestamp"),nullableNumber("temperature"),nullableNumber("humidity"),nullableNumber("light"),optString("source","device"),nullableNumber("lightAdc"),nullableNumber("lightMillivolts"))
fun JSONObject.nullableNumber(key:String):Double?=if(isNull(key)||!has(key))null else getDouble(key)
fun JSONObject.event()=SleepEvent(getString("id"),getLong("timestamp"),getString("kind"),optString("value"),optString("source","device"))
fun <T> JSONArray.mapObjects(block:(JSONObject)->T):List<T> = (0 until length()).map{block(getJSONObject(it))}
fun JSONObject.environment():Environment {val d=optJSONObject("status")?:JSONObject();return Environment(optJSONObject("current")?.sample(),(optJSONArray("samples")?:JSONArray()).mapObjects{it.sample()},(optJSONArray("events")?:JSONArray()).mapObjects{it.event()},DeviceStatus(d.bool("online"),d.optLong("lastSeen"),d.bool("lampOnline"),d.optInt("brightness"),d.optString("source","device"),d.bool("timeSynced"),d.optInt("queue"),d.bool("sensorOnly")))}
fun Settings.json()=JSONObject().put("temperatureMin",temperatureMin).put("temperatureMax",temperatureMax).put("humidityMin",humidityMin).put("humidityMax",humidityMax).put("lightMax",lightMax).put("alarmEnabled",alarmEnabled).put("alarmMinute",alarmMinute).put("fadeMinutes",fadeMinutes).put("autoLight",autoLight).put("manualBrightness",manualBrightness).put("timezoneOffset",timezoneOffset)
fun JSONObject.settings()=Settings(getDouble("temperatureMin"),getDouble("temperatureMax"),getDouble("humidityMin"),getDouble("humidityMax"),getDouble("lightMax"),bool("alarmEnabled"),getInt("alarmMinute"),getInt("fadeMinutes"),bool("autoLight"),getInt("manualBrightness"),optInt("timezoneOffset",420))
