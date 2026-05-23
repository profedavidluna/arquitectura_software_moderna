import { Pool } from 'pg';
import { config } from '../../config';

export const pool = new Pool({
  host: config.database.host,
  port: config.database.port,
  database: config.database.name,
  user: config.database.user,
  password: config.database.password,
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});

pool.on('error', (err) => console.error('[Database] Error:', err));

export async function initializeDatabase(): Promise<void> {
  const client = await pool.connect();
  console.log(`[Database] Connected to ${config.database.name}`);
  client.release();
}
