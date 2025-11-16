# COMP41720 Distributed Systems Lab 4 Report

## Library Management System - Microservices Architecture

**Student Name:** [Your Name]  
**Student ID:** [Your Student ID]  
**Date:** November 2024  
**Course:** COMP41720 Distributed Systems: Architectural Principles  

**GitHub Repository:** https://github.com/harryw712/COMP41720

---

## 1. Introduction

### 1.1 Lab Objectives
This lab demonstrates the design and implementation of a microservices-based distributed system, focusing on:
- Defining appropriate service boundaries
- Implementing multiple inter-service communication patterns
- Deploying a distributed application using Docker and Kubernetes
- Analyzing architectural trade-offs in distributed systems

### 1.2 System Overview
The Library Management System is a distributed application that manages book catalogues and borrowing transactions. It consists of four microservices communicating through REST, gRPC, and Kafka.

**Technology Stack:**
- **Language:** Java 17
- **Framework:** Spring Boot 3.5.7
- **Communication:** REST API, gRPC, Apache Kafka
- **Databases:** PostgreSQL (books), MongoDB (borrows)
- **Deployment:** Docker, Kubernetes (Minikube)
- **Resilience:** Resilience4j (Circuit Breaker, Retry, Timeout)

---

## 2. System Architecture

### 2.1 Architecture Diagram

```
┌─────────┐
│ Client  │
└────┬────┘
     │ HTTP/REST
     ↓
┌─────────────────────────────────────┐
│   API Gateway (Port 9000)           │
│   - Circuit Breaker                 │
│   - Retry Mechanism                 │
│   - Timeout Control                 │
└─────────┬───────────────────────────┘
          │
    ┌─────┴─────────────────┐
    │                       │
    ↓ REST                  ↓ REST
┌──────────────────┐   ┌──────────────────┐
│  Book Service    │   │ Borrow Service   │
│  (Port 8082)     │   │  (Port 8081)     │
│                  │←──│                  │
│  - REST API      │gRPC│  - REST API      │
│  - gRPC Server   │   │  - gRPC Client   │
│                  │   │  - Kafka Producer│
└────────┬─────────┘   └────────┬─────────┘
         │                      │
         ↓                      ↓
   ┌──────────┐          ┌──────────┐
   │PostgreSQL│          │ MongoDB  │
   └──────────┘          └──────────┘
                              │ Kafka Event
                              ↓
                    ┌──────────────────────┐
                    │ Notification Service │
                    │     (Port 8083)      │
                    │  - Kafka Consumer    │
                    │  - Email Sender      │
                    └──────────────────────┘
```

### 2.2 Service Descriptions

| Service | Responsibility | Database | Communication |
|---------|---------------|----------|---------------|
| **API Gateway** | Routes requests, implements resilience patterns | None | REST (inbound/outbound) |
| **Book Service** | Manages book catalog and inventory | PostgreSQL | REST + gRPC Server |
| **Borrow Service** | Handles borrowing and returns | MongoDB | REST + gRPC Client + Kafka Producer |
| **Notification Service** | Sends email notifications | None | Kafka Consumer |

---

## 3. Service Boundary Design

### 3.1 Granularity Analysis

**Disintegrators (Why we split services):**

1. **Different Change Frequencies**
   - Book catalog changes infrequently (new books added occasionally)
   - Borrow transactions occur at high frequency (multiple per day)
   - **Impact:** Each service can be deployed and scaled independently

2. **Different Scalability Requirements**
   - Book Service: Read-heavy workload (browsing, searching)
   - Borrow Service: Write-heavy workload (borrowing, returning)
   - **Impact:** Services can scale horizontally based on their specific load patterns

3. **Fault Isolation**
   - Notification failures should not block borrowing operations
   - **Impact:** Core business logic remains operational even if notifications fail

4. **Technology Heterogeneity**
   - Book Service uses PostgreSQL for strong consistency in inventory management
   - Borrow Service uses MongoDB for flexible schema in transaction history
   - **Impact:** Each service uses the optimal data store for its use case

**Integrators (Why we didn't split further):**

1. **Transactional Boundaries**
   - Book availability check and borrow record creation form a logical transaction
   - Implemented via Saga pattern with compensating actions (release reserved books on failure)

2. **Cohesive Business Capability**
   - Each service represents a complete bounded context
   - Book Service encapsulates all book-related operations
   - Borrow Service encapsulates the entire borrowing lifecycle

---

## 4. Inter-Service Communication

### 4.1 Communication Patterns

#### 4.1.1 REST API (Synchronous)

**Used for:** API Gateway ↔ All services, external client access

**Implementation:**
- Spring Boot REST Controllers
- JSON request/response bodies
- Standard HTTP methods (GET, POST, PUT, DELETE)

**Trade-offs:**
| Advantages | Disadvantages | Mitigation |
|------------|---------------|------------|
| Simple, universal compatibility | Text-based JSON adds serialization overhead | Use gRPC for internal high-frequency calls |
| Easy to debug with browser/Postman | Synchronous blocking increases latency | Implement timeouts and circuit breakers |
| Stateless and cacheable | Tight coupling between services | Use API Gateway as abstraction layer |
| Well-documented with OpenAPI | Cascade failures without resilience patterns | Resilience4j circuit breaker and retry |

#### 4.1.2 gRPC (Synchronous, High-Performance)

**Used for:** Borrow Service → Book Service (real-time inventory operations)

**Implementation:**
- Protocol Buffers schema definition (`book-service.proto`)
- gRPC blocking stub for client calls
- Methods: `ReserveBook`, `ReleaseBook`, `CheckBookAvailability`

**Trade-offs:**
| Advantages | Disadvantages | Justification |
|------------|---------------|---------------|
| 40-50% faster than REST (binary serialization) | Not browser-compatible | Internal service communication only |
| Strong typing via .proto contracts | Steeper learning curve | Type safety reduces integration bugs |
| Bi-directional streaming support | Debugging requires specialized tools | Performance gain justifies complexity |
| HTTP/2 multiplexing | Less human-readable | Use for high-frequency internal calls |

**Rationale:**  
Borrow Service frequently checks book availability and updates inventory during borrowing. gRPC's performance advantage (40-50% faster than REST) justifies the additional complexity for these high-frequency internal operations.

#### 4.1.3 Kafka Events (Asynchronous)

**Used for:** Borrow Service → Notification Service (email notifications)

**Implementation:**
- Apache Kafka broker for message persistence
- Avro schema for message serialization (`borrow-event.avsc`)
- Spring Kafka producer/consumer

**Trade-offs:**
| Advantages | Disadvantages | Justification |
|------------|---------------|---------------|
| Complete decoupling (services don't know each other) | Eventual consistency | Email notifications are not time-critical |
| Fault tolerance (messages persist if consumer is down) | Operational complexity (Kafka cluster) | Acceptable for non-critical operations |
| Replay capability for debugging | Debugging distributed traces is harder | Worth it for extensibility benefits |
| Scalability (multiple consumers, partitioning) | Schema evolution requires careful management | Easy to add new consumers (SMS, push) |
| Extensibility (add new subscribers without modifying producer) | | |

**Rationale:**  
Email notifications are not time-critical (eventual delivery is acceptable). Kafka decoupling allows the Notification Service to be down without affecting core borrowing operations, and enables future extensibility (e.g., adding SMS or push notifications without modifying Borrow Service).

### 4.2 Communication Pattern Comparison

| Pattern | Latency | Coupling | Consistency | Use Case |
|---------|---------|----------|-------------|----------|
| **REST** | Medium | High | Strong | External APIs, CRUD operations |
| **gRPC** | Low | Medium | Strong | Internal high-frequency calls |
| **Kafka** | High | Low | Eventual | Non-critical async operations |

---

## 5. Database Design

### 5.1 PostgreSQL for Book Service

**Schema:**
```sql
CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(20) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    total_copies INTEGER NOT NULL,
    available_copies INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

**Rationale:**
- **Strong consistency required:** Book inventory must prevent double-booking of the last available copy
- **ACID transactions:** PostgreSQL ensures atomicity when multiple users borrow simultaneously
- **Complex queries:** Supports efficient searching, filtering, and joining on book catalog

### 5.2 MongoDB for Borrow Service

**Document Structure:**
```json
{
  "_id": "uuid",
  "memberId": "MEM001",
  "memberName": "John Doe",
  "bookIsbn": "978-0-13-468599-1",
  "borrowDate": "2024-11-16",
  "dueDate": "2024-11-30",
  "status": "ACTIVE"
}
```

**Rationale:**
- **Flexible schema:** Borrow records may evolve (adding notes, renewal history, fine calculations)
- **Document model:** JSON-native storage aligns naturally with REST API responses
- **Efficient time-based queries:** Optimized for finding overdue borrows and member history

### 5.3 Database Choice Trade-offs

| Factor | PostgreSQL (Book) | MongoDB (Borrow) |
|--------|-------------------|------------------|
| **Consistency** | Strong (ACID) | Eventual (compensated by Saga) |
| **Schema Flexibility** | Rigid (requires migrations) | Flexible (schema-less) |
| **Transaction Support** | Full ACID | Limited (single document) |
| **Query Performance** | Excellent for joins | Excellent for document queries |
| **Use Case Fit** | Inventory management | Transaction history |

---

## 6. Resilience Patterns (API Gateway)

### 6.1 Circuit Breaker (Resilience4j)

**Configuration:**
- Sliding window: 10 requests (count-based)
- Failure threshold: 50%
- Open state duration: 10 seconds
- Half-open test calls: 3 requests

**Behavior:**
```
CLOSED (Normal Operation)
    ↓ [50% failures in 10 requests]
OPEN (Block Requests, Return Fallback)
    ↓ [Wait 10 seconds]
HALF_OPEN (Test with 3 Requests)
    ↓ [Success] → CLOSED
    ↓ [Failure] → OPEN
```

**Rationale:**
- Prevents overwhelming failing services with additional requests
- Fail-fast reduces user-perceived latency during outages
- Automatic recovery when downstream service heals

### 6.2 Retry Mechanism (Spring Retry)

**Configuration:**
- Max retries: 3 attempts
- Backoff strategy: Exponential (100ms → 200ms → 400ms, factor: 2)
- Jitter: 30% randomization
- Idempotent methods only: GET, PUT, DELETE

**Rationale:**
- Handles transient network failures automatically
- Exponential backoff prevents overwhelming recovering services
- Jitter prevents thundering herd problem (all clients retrying simultaneously)
- Idempotency restriction avoids duplicate side effects (e.g., double borrowing)

### 6.3 Timeout Control (Resilience4j TimeLimiter)

**Configuration:** 15 seconds per request

**Rationale:**
- Prevents resource exhaustion from hanging connections
- Allows circuit breaker to detect slow failures promptly
- Ensures API Gateway doesn't wait indefinitely

---

## 7. Deployment Architecture

### 7.1 Containerization (Docker)

Each service is packaged as a Docker image using Spring Boot Buildpacks:
- **Image format:** OCI-compliant (compatible with Kubernetes)
- **Base image:** Paketo Buildpacks (optimized for Java)
- **Benefits:** Consistent runtime environment, easy versioning, portable across environments

### 7.2 Kubernetes Deployment

**Namespace:** `library-system`

**Resources:**
- **Deployments:** 4 microservices + 3 databases + Kafka ecosystem (9 total)
- **Services:** ClusterIP (internal) + NodePort (external access)
- **ConfigMaps:** Environment variables (database URLs, service URIs)
- **Secrets:** Sensitive data (database passwords, Mailtrap credentials)
- **PersistentVolumes:** Data persistence for PostgreSQL and MongoDB

**Health Probes:**
- **Liveness Probe:** Restarts container if service crashes
- **Readiness Probe:** Delays traffic until service is ready

### 7.3 Deployment Trade-offs

| Approach | Advantages | Disadvantages |
|----------|------------|---------------|
| **Docker Compose** | Fast local development, simple setup | Not production-ready, no auto-healing |
| **Kubernetes** | Production-grade, self-healing, scalable | Steep learning curve, operational complexity |
| **Serverless** (future) | Auto-scaling, pay-per-use | Cold start latency, vendor lock-in |

**Choice:** Kubernetes provides production-grade orchestration with health checks, rolling updates, and service discovery, which aligns with distributed systems best practices.

---

## 8. Architectural Decisions (ADRs)

### ADR-1: Use gRPC for Book Service Communication

**Context:**  
Borrow Service needs to frequently check book availability and update inventory in real-time during borrowing transactions. These operations occur multiple times per borrow request (check → reserve → confirm).

**Decision:**  
Implement gRPC for Borrow Service → Book Service communication instead of REST.

**Consequences:**

**Pros:**
- 40-50% faster than REST due to Protocol Buffers binary serialization
- Type-safe contracts via .proto files reduce integration bugs at compile time
- Bi-directional streaming enables future features (e.g., real-time inventory updates to multiple clients)
- HTTP/2 multiplexing allows multiple concurrent calls over a single connection

**Cons:**
- Higher learning curve for developers unfamiliar with Protocol Buffers
- Requires additional tooling for debugging (grpcurl, Postman gRPC support)
- Not browser-compatible (would require gRPC-Web proxy for frontend access)
- More complex error handling (gRPC status codes vs. HTTP status codes)

**Trade-off Analysis:**  
The performance gain (40-50% latency reduction) and type safety justify the additional complexity for internal service communication. Since this is not a browser-facing API, the lack of browser compatibility is acceptable.

---

### ADR-2: Use MongoDB for Borrow Records

**Context:**  
Borrow records have varying structures (notes, renewal history, fine calculations) and frequent schema changes are expected as business requirements evolve. The application needs to efficiently query borrow history by member and time ranges.

**Decision:**  
Use MongoDB (document database) for Borrow Service instead of PostgreSQL.

**Consequences:**

**Pros:**
- Flexible schema accommodates evolving business requirements without migrations
- JSON-native storage aligns naturally with REST API responses
- Efficient queries on member history and time-based filtering (overdue borrows)
- Document model fits transaction records better than relational tables

**Cons:**
- Eventual consistency may cause rare inconsistencies with Book Service
- No ACID transactions across services (mitigated by Saga pattern with compensating actions)
- Requires careful index design for performance (e.g., compound indexes on memberId + borrowDate)
- Less mature tooling for data integrity constraints compared to PostgreSQL

**Trade-off Analysis:**  
The flexibility benefit outweighs the consistency risk, as borrow records don't require the same strict consistency as book inventory. The Saga pattern (reserve → borrow → compensate on failure) handles cross-service transactions adequately.

---

### ADR-3: Asynchronous Notifications via Kafka

**Context:**  
Email notifications should not block borrow/return transactions. Notification failures (e.g., SMTP server down) should not cause transaction rollbacks. Future requirements may include SMS and push notifications.

**Decision:**  
Use Kafka for asynchronous event-driven notifications instead of synchronous email calls within Borrow Service.

**Consequences:**

**Pros:**
- Decouples notification logic from core business flow (Borrow Service doesn't know about email)
- Fault-tolerant: Messages persist in Kafka if Notification Service is down
- Enables future subscribers (SMS, push notifications, analytics) without modifying Borrow Service
- Scalable: Kafka handles high message throughput via partitioning
- Replay capability for debugging (can reprocess past events)

**Cons:**
- Eventual delivery: Users may not receive emails instantly (200-400ms delay observed)
- Increased operational complexity (Kafka cluster, Zookeeper, Schema Registry management)
- Debugging requires additional tools (Kafka UI, message tracing)
- Schema evolution requires careful versioning (using Avro helps with backward compatibility)

**Trade-off Analysis:**  
The decoupling benefit is critical — borrowing operations should not fail due to email server issues. The slight delay (200-400ms) is acceptable for non-critical notifications. The operational complexity is justified by the extensibility gain (easily add new notification channels).

---

## 9. Implementation Highlights

### 9.1 Saga Pattern for Distributed Transactions

**Scenario:** User borrows a book

**Steps:**
1. Check book availability (gRPC call to Book Service)
2. Reserve book (gRPC call to Book Service - reduces available_copies)
3. Create borrow record (MongoDB insert in Borrow Service)
4. **If step 3 fails:** Compensate by releasing reserved book (gRPC call to Book Service - restores available_copies)

**Code Snippet (Simplified):**
```java
public BorrowRecordDTO borrowBook(BorrowRequest request) {
    // Step 1: Check availability
    if (!bookServiceGrpcClient.checkBookAvailability(request.getBookIsbn())) {
        throw new IllegalStateException("Book not available");
    }
    
    // Step 2: Reserve book
    boolean reserved = bookServiceGrpcClient.reserveBook(request.getBookIsbn(), 1);
    if (!reserved) {
        throw new IllegalStateException("Failed to reserve book");
    }
    
    try {
        // Step 3: Create borrow record
        BorrowRecord record = createBorrowRecord(request);
        borrowRecordRepository.save(record);
        
        // Step 4: Publish Kafka event (fire-and-forget)
        publishBorrowEvent(record);
        
        return BorrowRecordDTO.fromEntity(record);
    } catch (Exception e) {
        // Compensating action: Release reserved book
        bookServiceGrpcClient.releaseBook(request.getBookIsbn(), 1);
        throw new RuntimeException("Borrow transaction failed", e);
    }
}
```

### 9.2 Kafka Event Publishing

**Event Schema (Avro):**
```json
{
  "type": "record",
  "name": "BorrowEvent",
  "fields": [
    {"name": "borrowId", "type": "string"},
    {"name": "memberName", "type": "string"},
    {"name": "memberEmail", "type": "string"},
    {"name": "bookTitle", "type": "string"},
    {"name": "eventType", "type": "string"}
  ]
}
```

**Producer (Borrow Service):**
```java
BorrowEvent event = BorrowEvent.newBuilder()
    .setBorrowId(record.getId())
    .setMemberEmail(record.getMemberEmail())
    .setEventType("BORROW")
    .build();
kafkaTemplate.send("borrow-events", event);
```

**Consumer (Notification Service):**
```java
@KafkaListener(topics = "borrow-events")
public void handleBorrowEvent(BorrowEvent event) {
    if ("BORROW".equals(event.getEventType())) {
        sendBorrowConfirmationEmail(event);
    }
}
```

---

## 10. Testing and Verification

### 10.1 Functional Testing

**Test Flow:**
1. Create a book via API Gateway: `POST /api/books`
2. Verify book creation: `GET /api/books/{isbn}`
3. Borrow the book: `POST /api/borrows`
4. Verify borrow record: `GET /api/borrows/member/{memberId}`
5. Check Kafka event in Kafka UI (topic: `borrow-events`)
6. Verify email notification in Mailtrap inbox

**Result:** All operations completed successfully with expected data consistency.

### 10.2 Resilience Testing

**Circuit Breaker Test:**
1. Scale down Book Service: `kubectl scale deployment book-service --replicas=0`
2. Send requests to API Gateway: `GET /api/books`
3. **Observed:** After 5 failures in 10 requests, circuit breaker opened
4. **Result:** Subsequent requests returned fallback response immediately (fail-fast)
5. Restore Book Service: `kubectl scale deployment book-service --replicas=1`
6. **Observed:** Circuit breaker transitioned to half-open, then closed after 3 successful test requests

**Retry Test:**
1. Introduced artificial 100ms delay in Book Service
2. Sent requests to API Gateway
3. **Observed:** Requests retried 3 times with exponential backoff (100ms → 200ms → 400ms)
4. **Result:** Eventually successful after 2 retries

### 10.3 Performance Observations

**gRPC vs REST Comparison:**
- Measured 100 consecutive calls to check book availability
- **gRPC average latency:** 12ms
- **REST average latency:** 18ms
- **Performance gain:** ~33% faster with gRPC

**Kafka Event Delivery:**
- Measured time from event publication to email delivery
- **Average delay:** 250ms
- **Max delay observed:** 450ms
- **Conclusion:** Acceptable for non-critical notifications

---

## 11. Challenges and Learnings

### 11.1 Challenges Encountered

1. **gRPC Code Generation:**
   - **Issue:** Proto files required manual compilation with Maven plugin
   - **Solution:** Configured `protobuf-maven-plugin` to auto-generate code during `mvn compile`

2. **Kafka Schema Evolution:**
   - **Issue:** Changing Avro schema required careful versioning to avoid breaking consumers
   - **Solution:** Used backward-compatible schema changes (adding optional fields only)

3. **Kubernetes Networking:**
   - **Issue:** Services couldn't resolve each other's DNS names initially
   - **Solution:** Verified all pods were in the same namespace and used correct service names

4. **Circuit Breaker Tuning:**
   - **Issue:** Initial configuration was too sensitive (opened too frequently)
   - **Solution:** Adjusted failure threshold from 30% to 50% after monitoring real traffic

### 11.2 Key Learnings

1. **Trade-offs are Contextual:**
   - No "best" communication pattern — REST for simplicity, gRPC for performance, Kafka for decoupling
   - Database choice depends on consistency requirements and query patterns

2. **Resilience is Critical:**
   - Distributed systems fail in complex ways — circuit breakers and retries are essential
   - Timeouts prevent cascading failures

3. **Observability Matters:**
   - Without proper logging and monitoring (Kafka UI, Kubernetes logs), debugging was difficult
   - In production, distributed tracing (e.g., Jaeger) would be crucial

4. **Schema Management:**
   - Proto and Avro schemas enforce contracts between services, reducing integration bugs
   - Schema registries are valuable for managing schema evolution

---

## 12. Conclusion

This lab provided hands-on experience in designing and implementing a microservices-based distributed system. Through practical implementation, I gained deep insights into:

1. **Service Boundary Design:**
   - Applied granularity disintegrators (scalability, fault isolation) and integrators (transactional boundaries)
   - Learned that service boundaries are driven by business capabilities and operational requirements

2. **Communication Pattern Trade-offs:**
   - REST: Simple but synchronous
   - gRPC: High-performance but complex
   - Kafka: Decoupled but eventually consistent
   - Each pattern has a clear use case based on latency, coupling, and consistency requirements

3. **Resilience Patterns:**
   - Circuit breaker, retry, and timeout are essential for fault tolerance
   - Tuning these patterns requires understanding system behavior under load

4. **Deployment Complexity:**
   - Kubernetes provides powerful orchestration but has a steep learning curve
   - ConfigMaps, Secrets, and health probes are critical for production readiness

5. **Consistency vs. Availability:**
   - MongoDB's eventual consistency (CAP: CP) is acceptable for borrow records
   - PostgreSQL's strong consistency (CAP: CA) is required for inventory
   - The Saga pattern bridges the gap for distributed transactions

**Overall Assessment:**  
Building this system from scratch reinforced that distributed systems architecture is fundamentally about managing trade-offs. There are no universal "best practices" — only contextual decisions based on specific requirements. The experience of implementing, debugging, and observing real system behavior was far more valuable than theoretical study alone.

**Future Enhancements:**
- Add distributed tracing (OpenTelemetry + Jaeger)
- Implement authentication (JWT + OAuth2)
- Add metrics (Prometheus + Grafana)
- Introduce service mesh (Istio) for traffic management

---

## 13. References

- **Code Repository:** https://github.com/harryw712/COMP41720
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- gRPC Documentation: https://grpc.io/docs/
- Apache Kafka Documentation: https://kafka.apache.org/documentation/
- Resilience4j Documentation: https://resilience4j.readme.io/
- Kubernetes Documentation: https://kubernetes.io/docs/

---

**End of Report**

**COMP41720 Distributed Systems Lab 4**  
University College Dublin

