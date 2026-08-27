# Eureka Service Discovery Server (`eureka-server`)

The `eureka-server` microservice acts as the centralized **Service Registry & Phonebook Directory** for the Connecting Dots ecosystem. It enables dynamic service discovery so microservices can communicate without hardcoding IP addresses or domain names.

---

## 1. Core Service Overview

- **Port:** `8761`
- **Container Name:** `eureka-server`
- **Dashboard URL:** `http://localhost:8761`
- **Primary Role:** Registry server where `gateway-service`, `core-service`, and `ai-service` register their network locations upon startup.

---

## 2. Tech Stack & Dependencies

- **Language & JDK:** Java 25
- **Framework:** Spring Boot 4.0.7
- **Cloud Extension:** Spring Cloud 2025.1.2 (Netflix Eureka Server)
- **Build Tool:** Maven (`pom.xml`)

### Key Dependency
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

---

## 3. Configuration & Working Logic

### `application.yml`

```yaml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false  # Server does not register with itself
    fetch-registry: false        # Server does not need to fetch registry from another node
  server:
    wait-time-in-ms-when-sync-empty: 0
```

### How Service Discovery Works Step-by-Step

1. **Bootstrapping**: `eureka-server` starts up on port `8761` and initializes the registry dashboard.
2. **Registration**: When `core-service` or `ai-service` boots, its Eureka Client sends an HTTP `POST` to `http://eureka-server:8761/eureka/apps/{SERVICE_NAME}` registering its IP and port.
3. **Heartbeat Monitoring**: Registered microservices send ping heartbeats every 30 seconds to maintain an active lease.
4. **Dynamic Routing**: When a client request hits `gateway-service`, the gateway queries Eureka to dynamically resolve `lb://core-service` or `lb://ai-service` to an active container address.

---

## 4. Key Annotations & Concepts

- **`@EnableEurekaServer`**: Placed on `EurekaServerApplication.java`. Tells Spring Boot to configure and start Netflix Eureka Server endpoints and UI dashboard.
- **Heartbeat & Lease Expiration**: If a service fails to send a heartbeat within the eviction threshold (default 90 seconds), Eureka removes it from the registry.
- **Self-Preservation Mode**: A safety mechanism where Eureka stops evicting instances if a network partition causes sudden loss of heartbeats across multiple services.

---

## 5. Key Engineering Challenges & Architectural Decisions

Service discovery design decisions, client-side load balancing, and failure modes are documented in the master guide:
👉 **[ARCHITECTURE_DECISIONS_AND_SOLUTIONS.md](../../ARCHITECTURE_DECISIONS_AND_SOLUTIONS.md)**
