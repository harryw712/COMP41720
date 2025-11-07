package com.example.serverservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@RestController
@RequestMapping("/api")
public class HelloController {
    @Value("${app.flaky.failureRate:0.3}") private double failureRate;
    @Value("${app.flaky.baseDelayMs:120}") private int baseDelayMs;
    private final Random random = new Random();

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hello(@RequestParam(defaultValue = "world") String name) {
        return ResponseEntity.ok("hello " + name);
    }

    @GetMapping("/unreliable")
    public ResponseEntity<String> unreliable(
            @RequestParam(required = false) Double rate,
            @RequestParam(required = false) Integer delay) throws InterruptedException {

        double r = rate != null ? rate : failureRate;
        int d = delay != null ? delay : baseDelayMs;

        // 模拟随机延迟 + 失败
        Thread.sleep(d + random.nextInt(80));
        if (random.nextDouble() < r) {
            return ResponseEntity.status(500).body("backend error");
        }
        return ResponseEntity.ok("ok");
    }
}