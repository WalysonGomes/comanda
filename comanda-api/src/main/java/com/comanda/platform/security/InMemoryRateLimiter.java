package com.comanda.platform.security;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Hand-rolled token-bucket, keyed by an arbitrary string (design.md Decision 2): one instance in
 * the VPS, so an in-memory map is sufficient (PRD Regra 3 — no Redis/distributed store before
 * there's more than one node). State resets on restart and isn't shared across nodes; both are
 * accepted MVP limitations, documented in design.md's Risks section.
 */
public class InMemoryRateLimiter {

    private final long capacity;
    private final double refillTokensPerMillisecond;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(long capacity, long windowMillis) {
        this.capacity = capacity;
        this.refillTokensPerMillisecond = (double) capacity / windowMillis;
    }

    public boolean tryConsume(String key) {
        return buckets.computeIfAbsent(key, k -> new Bucket(capacity)).tryConsume(refillTokensPerMillisecond, capacity);
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefillMillis;

        Bucket(long capacity) {
            this.tokens = capacity;
            this.lastRefillMillis = System.currentTimeMillis();
        }

        synchronized boolean tryConsume(double refillTokensPerMillisecond, long capacity) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillMillis;
            if (elapsed > 0) {
                tokens = Math.min(capacity, tokens + elapsed * refillTokensPerMillisecond);
                lastRefillMillis = now;
            }
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
