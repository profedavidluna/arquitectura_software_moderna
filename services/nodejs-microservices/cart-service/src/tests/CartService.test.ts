/**
 * Cart Service Tests - includes Circuit Breaker testing
 */
import { CircuitBreaker } from '../infrastructure/CircuitBreaker';

describe('CircuitBreaker', () => {
  let breaker: CircuitBreaker;

  beforeEach(() => {
    breaker = new CircuitBreaker('test-service', { threshold: 3, timeout: 1000, successThreshold: 2 });
  });

  it('should start in CLOSED state', () => {
    expect(breaker.getState()).toBe('CLOSED');
  });

  it('should allow requests in CLOSED state', async () => {
    const result = await breaker.execute(async () => 'success');
    expect(result).toBe('success');
    expect(breaker.getState()).toBe('CLOSED');
  });

  it('should transition to OPEN after threshold failures', async () => {
    const failingFn = async () => { throw new Error('Service down'); };

    for (let i = 0; i < 3; i++) {
      await expect(breaker.execute(failingFn)).rejects.toThrow('Service down');
    }

    expect(breaker.getState()).toBe('OPEN');
  });

  it('should reject requests immediately when OPEN', async () => {
    // Force open state
    const failingFn = async () => { throw new Error('fail'); };
    for (let i = 0; i < 3; i++) {
      try { await breaker.execute(failingFn); } catch {}
    }

    await expect(breaker.execute(async () => 'test')).rejects.toThrow('Circuit breaker [test-service] is OPEN');
  });

  it('should transition to HALF_OPEN after timeout', async () => {
    // Create breaker with very short timeout for testing
    const fastBreaker = new CircuitBreaker('fast-test', { threshold: 1, timeout: 50, successThreshold: 1 });

    await expect(fastBreaker.execute(async () => { throw new Error('fail'); })).rejects.toThrow();
    expect(fastBreaker.getState()).toBe('OPEN');

    // Wait for timeout
    await new Promise(resolve => setTimeout(resolve, 100));

    // Next call should transition to HALF_OPEN and succeed
    const result = await fastBreaker.execute(async () => 'recovered');
    expect(result).toBe('recovered');
    expect(fastBreaker.getState()).toBe('CLOSED');
  });

  it('should reset failure count on success in CLOSED state', async () => {
    const failingFn = async () => { throw new Error('fail'); };

    // 2 failures (below threshold of 3)
    try { await breaker.execute(failingFn); } catch {}
    try { await breaker.execute(failingFn); } catch {}

    // Success resets counter
    await breaker.execute(async () => 'success');
    expect(breaker.getFailureCount()).toBe(0);
    expect(breaker.getState()).toBe('CLOSED');
  });
});
