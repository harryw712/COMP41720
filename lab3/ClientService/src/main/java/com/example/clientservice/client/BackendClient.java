package com.example.clientservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class BackendClient {
    private final WebClient webClient;

    public BackendClient(@Value("${app.backend.baseUrl}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public Mono<String> callUnreliable() {
        return webClient.get().uri("/api/unreliable")
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, r -> Mono.error(new RuntimeException("5xx error")))
                .bodyToMono(String.class);
    }

    public Mono<String> ping() {
        return webClient.get().uri("/api/ping").retrieve().bodyToMono(String.class);
    }
}