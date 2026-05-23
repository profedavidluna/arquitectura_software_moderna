import Redis from 'ioredis';
import { config } from '../../config';

export class RedisClient {
  private client: Redis;
  private connected = false;

  constructor() {
    this.client = new Redis({
      host: config.redis.host,
      port: config.redis.port,
      retryStrategy: (times) => times > 3 ? null : Math.min(times * 200, 2000),
    });

    this.client.on('connect', () => { this.connected = true; console.log('[Redis] Connected'); });
    this.client.on('error', (err) => { this.connected = false; });
  }

  async get<T>(key: string): Promise<T | null> {
    if (!this.connected) return null;
    try {
      const value = await this.client.get(key);
      return value ? JSON.parse(value) : null;
    } catch { return null; }
  }

  async set(key: string, value: any, ttlSeconds = 600): Promise<void> {
    if (!this.connected) return;
    try { await this.client.setex(key, ttlSeconds, JSON.stringify(value)); } catch {}
  }

  async delete(key: string): Promise<void> {
    if (!this.connected) return;
    try { await this.client.del(key); } catch {}
  }

  async disconnect(): Promise<void> { await this.client.quit(); }
}
