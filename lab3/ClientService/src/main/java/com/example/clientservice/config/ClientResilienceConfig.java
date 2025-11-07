package com.example.clientservice.config;

import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.retry.*;
import io.github.resilience4j.core.IntervalFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class ClientResilienceConfig {
    @Bean
    public CircuitBreaker circuitBreaker() {
        CircuitBreakerConfig cfg = CircuitBreakerConfig.custom()
                .failureRateThreshold(50f)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .waitDurationInOpenState(Duration.ofSeconds(4))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        return CircuitBreakerRegistry.of(cfg).circuitBreaker("backend");
    }

    @Bean
    public Retry retry() {
        // 指数退避 + 抖动
        IntervalFunction backoff = attempt -> {
            long base = (long) (100 * Math.pow(2, attempt-1));
            long jitter = ThreadLocalRandom.current().nextLong(0, 75);
            return base + jitter;
        };
        RetryConfig cfg = RetryConfig.custom()
                .maxAttempts(4)
                .intervalFunction(backoff)
                .retryExceptions(RuntimeException.class)
                .build();
        return RetryRegistry.of(cfg).retry("backend");
    }
}