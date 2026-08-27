# API Gateway Microservice (`gateway-service`)

The `gateway-service` microservice is the single **Edge Entry Point** for all incoming external HTTP requests from the Next.js frontend or mobile clients. Built on non-blocking reactive Spring Cloud Gateway, it handles request routing, load balancing, CORS enforcement, and Redis rate limiting.

---

## 1. Core Service Overview

- **Port:** `8080` (Primary Public Gateway Entry)
- **Container Name:** `gateway-service`
- **Primary Role:** Edge router, security shield, load balancer, and traffic rate limiter.

---

## 2. Tech Stack & Dependencies

- **Language & JDK:** Java 25
- **Framework:** Spring Boot 4.0.7 (Spring WebFlux / Project Reactor)
- **Gateway Engine:** Spring Cloud Gateway 2025.1.2
- **Caching & Rate Limiting:** Spring Data Reactive Redis (`redis:7-alpine`)
- **Discovery Client:** Spring Cloud Netflix Eureka Client

---

## 3. Inputs, Outputs & Route Mappings

| Client Request Path | Routing Target | Load Balancer Scheme | Service Responsible |
| :--- | :--- | :--- | :--- |
| `POST /api/v1/core/auth/**` | `http://core-service:8081` | `lb://core-service` | Authentication (Login/Register) |
| `GET /api/v1/core/problem-statements/**` | `http://core-service:8081` | `lb://core-service` | Problem Statement Discovery |
| `POST /api/v1/core/applications/**` | `http://core-service:8081` | `lb://core-service` | Contributor Project Applications |
| `POST /api/v1/ai/webhook` | `http://ai-service:8082` | `lb://ai-service` | Gemini AI Webhook Processing |

---

## 4. Working Logic Explained Simply

1. **Client Request Entry**: All incoming traffic hits `gateway-service` on port `8080`.
2. **CORS Inspection**: Global CORS policies defined in `application.yml` validate origin, headers, and HTTP methods (`GET`, `POST`, `PUT`, `DELETE`).
3. **Redis Rate Limiting**: Requests pass through a reactive Redis Token-Bucket filter:
   - **Replenish Rate:** 10 requests per second.
   - **Burst Capacity:** 20 requests.
   - **Key Resolver:** Client IP address (`#{@ipKeyResolver}`).
4. **Dynamic Load Balancing**: The gateway queries `eureka-server` to resolve `lb://core-service` or `lb://ai-service` into an active container IP and forwards the request without blocking threads.

---

## 5. Key Annotations, Classes & Configuration

### Rate Limiter Configuration (`GatewayConfig.java`)

```java
@Configuration
public class GatewayConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress() != null ?
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "anonymous"
        );
    }
}
```

### Route & Redis Configuration (`application.yml`)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: core-service-route
          uri: lb://core-service
          predicates:
            - Path=/api/v1/core/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@ipKeyResolver}"
```

---

## 6. Key Engineering Challenges & Architectural Decisions

Edge gateway reactive design, Redis token-bucket rate limiting, and CORS security architecture are documented in the master guide:
👉 **[ARCHITECTURE_DECISIONS_AND_SOLUTIONS.md](../../ARCHITECTURE_DECISIONS_AND_SOLUTIONS.md)**
