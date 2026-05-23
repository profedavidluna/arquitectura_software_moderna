// =============================================================================
// Database Connection Pool - Product Service
// =============================================================================
// Uses the 'pg' library's Pool for connection pooling.
// Connection pooling is essential in SOA for efficient resource usage
// when handling many concurrent requests.
// =============================================================================

import { Pool } from 'pg';
import { config } from '../../config';

/**
 * PostgreSQL connection pool.
 * Pool manages multiple connections and reuses them efficiently.
 */
export const pool = new Pool({
  host: config.db.host,
  port: config.db.port,
  user: config.db.user,
  password: config.db.password,
  database: config.db.database,
  max: 20, // Maximum number of connections in the pool
  idleTimeoutMillis: 30000, // Close idle connections after 30s
  connectionTimeoutMillis: 2000, // Fail fast if can't connect in 2s
});

/**
 * Test database connectivity.
 * Called during service startup to verify the database is reachable.
 */
export async function testConnection(): Promise<void> {
  const client = await pool.connect();
  try {
    await client.query('SELECT 1');
    console.log('[Database] Connection established successfully');
  } finally {
    client.release();
  }
}
