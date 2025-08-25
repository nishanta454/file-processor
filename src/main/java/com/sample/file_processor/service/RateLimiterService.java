package com.sample.file_processor.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimiterService {

    // Cache to store buckets per IP
    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @Value("${upload.api.rate-limit.minutes:10}")
    private int limitDurationMinutes;

    public boolean tryConsume(String key) {
        Bucket bucket = cache.get(key, k -> createNewBucket());
        return bucket.tryConsume(1);
    }

    private Bucket createNewBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                    .capacity(1)  // Allow 1 request
                    .refillGreedy(1, Duration.ofMinutes(limitDurationMinutes))  // Refill 1 token every X minutes
                )
                .build();
    }
}
