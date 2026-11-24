import { readFileSync } from 'node:fs';
const geminiKeyFile = new URL('../data/gemini.key', import.meta.url);
// The setup wizard can install a key while the gateway remains online.
function readGeminiKey() {
  try { return readFileSync(geminiKeyFile,'utf8').trim() || process.env.GEMINI_API_KEY || ''; }
  catch (error) { if (error.code === 'ENOENT') return process.env.GEMINI_API_KEY || ''; throw error; }
}
// Local classroom deployment. Override every demo credential before sharing a LAN.
export const config = {
  host: process.env.HOST || '127.0.0.1',
  port: Number(process.env.PORT || 8080), mqttPort: Number(process.env.MQTT_PORT || 1883),
  apiKey: process.env.API_KEY || 'sleep-local-demo',
  deviceId: 'bedroom-01', devicePassword: process.env.DEVICE_PASSWORD || 'sleep-device-demo',
  database: process.env.DATABASE || 'data/sleep.sqlite',
  ai: { provider:process.env.AI_PROVIDER || 'gemini', model:process.env.AI_MODEL || (['gemini',undefined].includes(process.env.AI_PROVIDER)?'gemini-3.5-flash-lite':''),
    apiKey:process.env.OPENAI_API_KEY || '', get geminiApiKey(){return readGeminiKey();}, ollamaUrl:process.env.OLLAMA_URL || 'http://127.0.0.1:11434' },
};
export const defaultSettings = {
  temperatureMin:18, temperatureMax:27, humidityMin:40, humidityMax:70,
  lightMax:150, alarmEnabled:false, alarmMinute:390, fadeMinutes:15,
  autoLight:true, manualBrightness:30, timezoneOffset:420,
};
