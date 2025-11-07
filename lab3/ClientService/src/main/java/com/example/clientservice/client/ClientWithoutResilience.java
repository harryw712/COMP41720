package com.example.clientservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ClientWithoutResilience {
    private static final Logger log = LoggerFactory.getLogger(ClientWithoutResilience.class);
    private final BackendClient backend;
    public ClientWithoutResilience(BackendClient backend) { this.backend = backend; }

    public void runOnce(int n) {
        long t0 = System.nanoTime();
        long[] ok = {0}, fail = {0};
        Flux.range(0, n)
                .flatMap(i -> backend.callUnreliable()
                        .doOnNext(v -> ok[0]++)
                        .onErrorResume(e -> { fail[0]++; return Flux.empty(); }))
                .blockLast();
        long ms = (System.nanoTime() - t0)/1_000_000;
        log.info("[BASELINE] total={} ok={} fail={} time={}ms", n, ok[0], fail[0], ms);
    }
}