import { config } from './config.js';
import { startSystem } from './system.js';
const system=await startSystem(config);
console.log(`Sleep Monitor API http://${config.host}:${system.httpPort} · MQTT ${system.mqttPort}`);
console.log('Local development service. For ESP32 LAN access set HOST=0.0.0.0 on your private network.');
for(const signal of ['SIGINT','SIGTERM'])process.on(signal,async()=>{await system.close();process.exit(0);});
