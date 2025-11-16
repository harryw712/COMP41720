# Library Management System - Microservices Architecture

**COMP41720 Distributed Systems Lab 4**

A microservices-based library management system demonstrating REST, gRPC, and Kafka communication patterns.

---

## System Architecture

```
Client
   |
   v HTTP/REST
API Gateway (Circuit Breaker + Retry + Timeout) - Port 9000
   |
   +---> Book Service (Book Management) - Port 8082
   |     |
   |     +---> PostgreSQL
   |
   +---> Borrow Service (Borrow Management) - Port 8081
   |     |
   |     +---> MongoDB
   |     +---> Book Service (gRPC)
   |     +---> Kafka (Async Events)
   |
   +---> Notification Service (Email Notifications) - Port 8083
         |
         +---> Kafka Consumer
```

For full documentation, see [REPORT.md](REPORT.md)

Code repository: https://github.com/harryw712/COMP41720

**COMP41720 Distributed Systems Lab 4**  
University College Dublin
