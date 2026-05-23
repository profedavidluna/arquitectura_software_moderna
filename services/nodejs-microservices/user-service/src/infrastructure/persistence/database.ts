/**
 * Database Connection Pool
 * 
 * Uses pg Pool for connection pooling, which is essential in microservices
 * to efficiently manage database connections under high concurrency.
 * Each service has its own database (Database-per-Service pattern).
 */
import { Pool } from 'pg';
import { config } from '../../config';

export const pool = new Pool({
  host: config.database.host,
  port: config.database.port,
  database: config.database.name,
  user: config.database.user,
  password: config.database.password,
  max: 20, // Maximum connections in pool
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});

pool.on('error', (err) => {
  console.error('[Database] Unexpected error on idle client:', err);
});

export async function initializeDatabase(): Promise<void> {
  try {
    const client = await pool.connect();
    console.log(`[Database] Connected to ${config.database.name}`);
    client.release();
  } catch (error) {
    console.error('[Database] Failed to connect:', error);
    throw error;
  }
}
