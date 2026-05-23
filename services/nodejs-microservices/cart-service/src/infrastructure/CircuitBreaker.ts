/**
 * Circuit Breaker Pattern Implementation
 * 
 * Prevents cascading failures in distributed systems by detecting failures
 * and stopping requests to a failing service temporarily.
 * 
 * States:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Service is failing, requests are rejected immediately
 * - HALF_OPEN: Testing if service has recovered (allows one request)
 * 
 * Transitions:
 * - CLOSED → OPEN: When failure count exceeds threshold
 * - OPEN → HALF_OPEN: After timeout period expires
 * - HALF_OPEN → CLOSED: If test request succeeds
 * - HALF_OPEN → OPEN: If test request fails
 * 
 * This is essential in microservices to prevent a single failing service
 * from bringing down the entire system (fault isolation).
 */
export type CircuitState = 'CLOSED' | 'OPEN' | 'HALF_OPEN';

export class CircuitBreaker {
  private state: CircuitState = 'CLOSED';
  private failureCount = 0;
  private successCount = 0;
  private lastFailureTime = 0;
  private readonly threshold: number;
  private readonly timeout: number;
  private readonly successThreshold: number;

  constructor(
    private readonly name: string,
    options?: { threshold?: number; timeout?: number; successThreshold?: number }
  ) {
    this.threshold = options?.threshold || 5;
    this.timeout = options?.timeout || 30000; // 30 seconds
    this.successThreshold = options?.successThreshold || 2;
  }

  /**
   * Execute a function through the circuit breaker.
   * If the circuit is open, the function won't be called.
   */
  async execute<T>(fn: () => Promise<T>): Promise<T> {
    if (this.state === 'OPEN') {
      // Check if timeout has elapsed - transition to HALF_OPEN
      if (Date.now() - this.lastFailureTime > this.timeout) {
        console.log(`[CircuitBreaker:${this.name}] Transitioning to HALF_OPEN`);
        this.state = 'HALF_OPEN';
      } else {
        throw new Error(`Circuit breaker [${this.name}] is OPEN - service unavailable`);
      }
    }

    try {
      const result = await fn();
      this.onSuccess();
      return result;
    } catch (error) {
      this.onFailure();
      throw error;
    }
  }

  private onSuccess(): void {
    if (this.state === 'HALF_OPEN') {
      this.successCount++;
      if (this.successCount >= this.successThreshold) {
        console.log(`[CircuitBreaker:${this.name}] Transitioning to CLOSED`);
        this.state = 'CLOSED';
        this.failureCount = 0;
        this.successCount = 0;
      }
    } else {
      this.failureCount = 0; // Reset on success in CLOSED state
    }
  }

  private onFailure(): void {
    this.failureCount++;
    this.lastFailureTime = Date.now();

    if (this.state === 'HALF_OPEN') {
      console.log(`[CircuitBreaker:${this.name}] HALF_OPEN failed, transitioning to OPEN`);
      this.state = 'OPEN';
      this.successCount = 0;
    } else if (this.failureCount >= this.threshold) {
      console.log(`[CircuitBreaker:${this.name}] Threshold reached, transitioning to OPEN`);
      this.state = 'OPEN';
    }
  }

  getState(): CircuitState {
    return this.state;
  }

  getFailureCount(): number {
    return this.failureCount;
  }
}
