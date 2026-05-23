/**
 * Redis Cache Client
 * 
 * Implements the Cache-Aside pattern:
 * 1. Check cache first
 * 2. If miss, load from database
 * 3. Store in cache for future requests
 * 
 * This dramatically reduces database load for frequently accessed products.
 * TTL (Time-To-Live) ensures eventual consistency with the database.
 */
import Redis from 'ioredis';
import { config } from '../../config';

export class RedisClient {
  private client: Redis;
  private connected = false;

  constructor() {
    this.client = new Redis({
      host: config.redis.host,
      port: config.redis.port,
      retryStrategy: (times) => {
        if (times > 3) return null; // Stop retrying after 3 attempts
        return Math.min(times * 200, 2000);
      },
    });

    this.client.on('connect', () => {
      this.connected = true;
      console.log('[Redis] Connected successfully');
    });

    this.client.on('error', (err) => {
      console.error('[Redis] Connection error:', err.message);
      this.connected = false;
    });
  }

  /**
   * Get cached value. Returns null on cache miss or Redis unavailability.
   * The service should gracefully degrade when cache is unavailable.
   */
  async get<T>(key: string): Promise<T | null> {
    if (!this.connected) return null;
    try {
      const value = await this.client.get(key);
      return value ? JSON.parse(value) : null;
    } catch (error) {
      console.warn('[Redis] Get error:', error);
      return null;
    }
  }

  /**
   * Set cached value with TTL (default 5 minutes).
   * Fire-and-forget - cache failures shouldn't break the main flow.
   */
  async set(key: string, value: any, ttlSeconds = 300): Promise<void> {
    if (!this.connected) return;
    try {
      await this.client.setex(key, ttlSeconds, JSON.stringify(value));
    } catch (error) {
      console.warn('[Redis] Set error:', error);
    }
  }

  /**
   * Invalidate cache entry when data changes.
   * Essential for maintaining consistency after writes.
   */
  async delete(key: string): Promise<void> {
    if (!this.connected) return;
    try {
      await this.client.del(key);
    } catch (error) {
      console.warn('[Redis] Delete error:', error);
    }
  }

  async disconnect(): Promise<void> {
    await this.client.quit();
  }
}
