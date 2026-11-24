import { DatabaseSync } from 'node:sqlite';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
export function openDatabase(path) {
  if(path !== ':memory:') mkdirSync(dirname(path), {recursive:true});
  const db = new DatabaseSync(path);
  db.exec(`PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON;
    CREATE TABLE IF NOT EXISTS sessions(id TEXT PRIMARY KEY, start INTEGER NOT NULL, end INTEGER,
      quality INTEGER NOT NULL DEFAULT 0, notes TEXT NOT NULL DEFAULT '', interrupted INTEGER DEFAULT 0,
      deviceId TEXT NOT NULL, version INTEGER NOT NULL, deleted INTEGER NOT NULL DEFAULT 0, updatedAt INTEGER NOT NULL);
    CREATE UNIQUE INDEX IF NOT EXISTS one_active ON sessions((1)) WHERE end IS NULL AND deleted=0;
    CREATE TABLE IF NOT EXISTS telemetry(id TEXT PRIMARY KEY, deviceId TEXT NOT NULL, timestamp INTEGER NOT NULL,
      temperature REAL, humidity REAL, light REAL, source TEXT NOT NULL);
    CREATE INDEX IF NOT EXISTS telemetry_time ON telemetry(deviceId,timestamp);
    CREATE TABLE IF NOT EXISTS events(id TEXT PRIMARY KEY, deviceId TEXT NOT NULL, timestamp INTEGER NOT NULL,
      kind TEXT NOT NULL, value TEXT NOT NULL, source TEXT NOT NULL);
    CREATE INDEX IF NOT EXISTS events_time ON events(deviceId,timestamp);
    CREATE TABLE IF NOT EXISTS kv(key TEXT PRIMARY KEY, value TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS commands(id TEXT PRIMARY KEY, createdAt INTEGER, expiresAt INTEGER,
      brightness INTEGER, state TEXT, detail TEXT);
  `);
  const columns=new Set(db.prepare('PRAGMA table_info(telemetry)').all().map(c=>c.name));
  for(const name of ['lightAdc','lightMillivolts']){
    if(!columns.has(name))db.exec('ALTER TABLE telemetry ADD COLUMN '+name+' INTEGER');
  }
  return db;
}
