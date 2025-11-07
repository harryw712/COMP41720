# COMP41720 – Lab 3: Resilience & Observability in a Kubernetes-Deployed Microservice

**Author:** Hairui Wang (23226138)  
**Repository (code & manifests):** [https://github.com/harryw712/COMP41720](https://github.com/harryw712/COMP41720)

---

## 1. Introduction

This lab investigates how application-level resilience patterns interact with platform-level healing to keep a distributed system responsive under faults. We implement a two-service micro-architecture—**ClientService → ServerService**—deploy it on **Kubernetes**, and validate three mechanisms:
1. **Circuit Breaker** to avoid cascading failures
2. **Retry with Exponential Backoff + Jitter** for transient faults
3. **Chaos Experiments** to verify behavior under pod termination and slow/failing endpoints

Our goal is to reason about **availability**, **latency**, and **fault tolerance trade-offs**, and to document repeatable experiments with concrete evidence (logs, metrics, timelines).

---

## 2. System Overview

### 2.1 Architecture

```
┌───────────────────┐         HTTP           ┌───────────────────┐
│   ClientService   │ ─────────────────────▶ │   ServerService   │
│ (Resilience4j:    │ ◀───────────────────── │  /api/ping        │
│  CB + Retry)      │        responses       │  /api/unreliable  │
└───────────────────┘                         └───────────────────┘
          ▲                                              ▲
          │                                              │
          └────────────── Kubernetes (Deployments, Services) ───────┘
```

- **ServerService** exposes:
    - `/api/ping` – sanity check
    - `/api/unreliable?rate=&delay=` – controllable failure/latency to simulate real incidents
- **ClientService** uses WebClient + Resilience4j (CB, Retry) to compare baseline vs. resilient behavior.

### 2.2 Tech Stack
- Language/Runtime: Java 17, Spring Boot 3.x, Maven
- Resilience: Resilience4j (circuit breaker, retry, backoff, jitter)
- Platform: Docker, Kubernetes (Minikube or Docker Desktop)
- Chaos tool: Chaos Toolkit (k8s extension)

---

## 3. Build, Containerization & Deployment

### 3.1 Local Run (no containers)

```bash
# Terminal A: Server
cd ServerService
mvn spring-boot:run

# Terminal B: Client (points to localhost:8081)
cd ClientService
mvn -Dspring-boot.run.arguments="--app.backend.baseUrl=http://localhost:8081" spring-boot:run
```

Expected quick checks:
```bash
curl http://localhost:8081/api/ping       # -> "pong"
curl http://localhost:8081/api/unreliable # -> 200 or 500 with variable delay
```

### 3.2 Docker Images
```bash
# Build images
cd ServerService && mvn -DskipTests package && docker build -t local/serverservice:latest .
cd ../ClientService && mvn -DskipTests package && docker build -t local/clientservice:latest .
```
*(If using a remote registry, tag & push accordingly.)*

### 3.3 Kubernetes Manifests
```bash
# Apply server (2 replicas) & client (1 replica)
kubectl apply -f k8s/server.yaml
kubectl apply -f k8s/client.yaml

kubectl get pods -o wide
kubectl get svc
```

Override backend URL via env in `client.yaml`:
```
APP_BACKEND_BASEURL = http://server-svc:8081
```

---

## 4. Resilience Configuration (ClientService)

- **Circuit Breaker**
    - Sliding window: 20 calls (count-based)
    - Failure rate threshold: 50%
    - Open wait: 4s
    - Half-open permitted calls: 3

- **Retry**
    - Max attempts: 4 (initial + 3 retries)
    - Exponential Backoff: 100ms, 200ms, 400ms
    - Jitter: ±75ms randomization per attempt to prevent synchronized retry storms

*(Exact values are coded in `ClientResilienceConfig` and can be tweaked per experiment.)*

---

## 5. Experiments

Each experiment includes setup, commands, observations (logs/metrics), and interpretation.  
Replace placeholders with your real numbers, timestamps, and screenshots.

### 5.1 Baseline (No Resilience)

**Setup:** Use `ClientWithoutResilience`.  
N = 200 requests to `/api/unreliable?rate=0.35&delay=120` in a tight loop.

**Command / Trigger:** Start ClientService; baseline runner executes automatically (see logs).

**Collect:**
- Client counters: ok, fail, total elapsed (ms)
- Sample server logs (failures & delays)
- Any spikes in latency perceived by the client

**Expected Observation:**
- Success rate ~ XX%, failures propagate directly
- Long tails when server delays increase
- Threads blocked during slow responses

**Interpretation:** Baseline reveals raw backend propagation to users; no guardrails.

---

### 5.2 With Resilience (CB + Retry)

**Setup:** Enable `ClientResilience` runner with same N and server parameters.

**Collect:**
- Client totals + CB transitions (CLOSED → OPEN → HALF_OPEN → CLOSED)
- Retry counts, intervals (confirm backoff + jitter)
- Compare success rate & total time vs baseline

**Expected Effects:**
- Higher perceived availability (fast-fail under OPEN)
- Fewer long waits due to timeouts and gating
- Slightly higher total time, but fewer user-visible failures

| Metric | Baseline | Resilience |
|---------|-----------|------------|
| Success rate (%) | XX.X | YY.Y |
| Avg latency (ms) | XXX | YYY |
| 95th percentile latency (ms) | XXX | YYY |
| Client errors | NN | MM |
| CB open events | – | K |
| Avg retries per failed call | – | R.R |

**Interpretation:** Resilience improves availability with bounded overhead—acceptable for idempotent operations.

---

### 5.3 Circuit Breaker Dynamics

**Goal:** Trigger CB OPEN by increasing failure probability.

**Setup:** Call `/api/unreliable?rate=0.8&delay=120` for many requests.

**Capture:**
- CB metrics timeline
- Failure rate %, transitions
- Fast-fail logs, HALF_OPEN recovery

**Expected Behavior:**
- ≥50% failures in window(20) → CB → OPEN
- After 4s → HALF_OPEN; 3 trial calls decide CLOSED/OPEN
- CB limits cascading effects and stabilizes system.

---

### 5.4 Retry with Exponential Backoff + Jitter

**Goal:** Validate jitter’s smoothing effect.

**Setup:** Failure rate ~0.5, transient failures `rate=0.5&delay=250`.

**Capture:**
- Timestamps of retries (≈100ms, 200ms, 400ms ± jitter)
- Server logs showing staggered retry arrivals

**Expected:** Jitter spreads retries across time, preventing retry storms.  
**Trade-off:** Slightly higher latency for improved success rate.

---

### 5.5 Chaos Experiment: Kill a Server Pod

**Tool:** Chaos Toolkit (k8s)  
**Manifest:** `k8s/chaos-toolkit.yaml` (terminates one server pod & verifies recovery)

**Procedure:**
```bash
# watch pods
kubectl get pods -w

# run chaos
kubectl exec -it deployment/chaos-toolkit -- chaos run /tmp/experiments/experiment.json
```

**Evidence:**
- Pod timeline (Terminating → Creating → Running)
- Client logs (retries, CB state)
- Time from kill → pod ready (~ your value)

**Expected:**
- With resilience: minimal disruption, auto recovery
- Without resilience: visible user errors until retry

**Interpretation:** App-level resilience + Kubernetes healing = high responsiveness.

---

## 6. Analysis & Discussion

### 6.1 Availability, Consistency, and Latency
- Availability ↑ via CB fast-fail + retries; latency ↑ on failures
- Temporary consistency trade-off keeps UI responsive
- Acceptable for idempotent reads.

### 6.2 When to Prefer Which Pattern
- **Retry:** transient faults, idempotent ops
- **Circuit Breaker:** persistent faults or degradation
- **Combo:** retries inside CB (CLOSED/HALF_OPEN), fast-fail when OPEN.

### 6.3 What the Chaos Experiment Proves
- Platform restarts pods, but app patterns define user experience
- Timeouts + bounded retries + CB avoid exhaustion and allow healing.

---

## 7. Limitations & Future Work
- Single client pod; no load generator or autoscaling yet
- No Prometheus/Grafana metrics stack (logs only)
- Future: add rate limiting, bulkheads, endpoint timeouts, and metrics export.

---

## 8. Conclusion

Resilience patterns significantly improve perceived availability under failures with modest overhead.  
Experiments show that:
- Circuit Breaker **protects**,
- Retry **recovers**, and
- Chaos **validates** self-healing with Kubernetes.

---

## 9. Reproducibility (Quick Commands)

```bash
# Build images
cd ServerService && mvn -DskipTests package && docker build -t local/serverservice:latest .
cd ../ClientService && mvn -DskipTests package && docker build -t local/clientservice:latest .

# Deploy
kubectl apply -f k8s/server.yaml
kubectl apply -f k8s/client.yaml

# Verify
kubectl get pods -o wide
kubectl logs deploy/client-deploy -f

# Chaos
kubectl apply -f k8s/chaos-toolkit.yaml
kubectl get pods -w
kubectl exec -it deployment/chaos-toolkit -- chaos run /tmp/experiments/experiment.json
```

---

## 10. Appendix
- Key endpoints: `/api/ping`, `/api/unreliable?rate=&delay=`
- Default ports: Server 8081, Client 8080
- Resilience parameters: see `ClientResilienceConfig.java`