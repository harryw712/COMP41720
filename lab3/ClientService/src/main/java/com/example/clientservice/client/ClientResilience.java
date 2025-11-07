package com.example.clientservice.client;

import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.retry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ClientResilience {
    private static final Logger log = LoggerFactory.getLogger(ClientResilience.class);
    private final BackendClient backend;
    private final CircuitBreaker cb;
    private final Retry retry;

    public ClientResilience(BackendClient backend, CircuitBreaker circuitBreaker, Retry retry) {
        this.backend = backend; this.cb = circuitBreaker; this.retry = retry;
    }

    public void runOnce(int n) {
        long t0 = System.nanoTime();
        long[] ok = {0}, fail = {0};
        Flux.range(0, n).flatMap(i -> decorate(backend.callUnreliable())
                .doOnNext(v -> ok[0]++)
                .onErrorResume(e -> { fail[0]++; return Mono.empty(); })
        ).blockLast();
        long ms = (System.nanoTime() - t0)/1_000_000;
        CircuitBreaker.Metrics m = cb.getMetrics();
        log.info("[RESILIENT] total={} ok={} fail={} time={}ms cb[state={}, failRate={}%, bufferedCalls={}]",
                n, ok[0], fail[0], ms, cb.getState(), m.getFailureRate(), m.getNumberOfBufferedCalls());
    }

    private <T> Mono<T> decorate(Mono<T> mono) {
        return mono.transformDeferred(CircuitBreakerOperator.of(cb))
                .transformDeferred(RetryOperator.of(retry));
    }
}