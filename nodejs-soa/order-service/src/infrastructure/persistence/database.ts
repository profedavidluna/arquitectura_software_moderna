// =============================================================================
// Database Connection Pool - Order Service
// =============================================================================

import { Pool } from 'pg';
import { config } from '../../config';

export const pool = new Pool({
  host: config.db.host,
  port: config.db.port,
  user: config.db.user,
  password: config.db.password,
  database: config.db.database,
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});

export async function testConnection(): Promise<void> {
  const client = await pool.connect();
  try {
    await client.query('SELECT 1');
    console.log('[Database] Connection established successfully');
  } finally {
    client.release();
  }
}
