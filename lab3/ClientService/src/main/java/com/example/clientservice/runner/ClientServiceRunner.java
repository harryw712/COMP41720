package com.example.clientservice.runner;

import com.example.clientservice.client.ClientResilience;
import com.example.clientservice.client.ClientWithoutResilience;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ClientServiceRunner implements CommandLineRunner {
    private final ClientWithoutResilience baseline;
    private final ClientResilience resilient;

    public ClientServiceRunner(ClientWithoutResilience b, ClientResilience r) {
        this.baseline = b; this.resilient = r;
    }

    @Override
    public void run(String... args) {
        int N = 200; // 每轮请求数，可按需调整
        baseline.runOnce(N);
        resilient.runOnce(N);
    }
}