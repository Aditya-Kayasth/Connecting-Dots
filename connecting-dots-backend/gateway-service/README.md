# Gateway Service (`gateway-service`)

## Role & Responsibilities
The secure perimeter entry point (`port 8080`) built on reactive Spring WebFlux. It performs dynamic load-balanced routing (`lb://`), JWT token verification, and Redis-backed token-bucket rate limiting (`RequestRateLimiter`).

## Tech Stack
* Spring Boot 4.0.7 / Spring Cloud Gateway (Reactive WebFlux)
* Spring Cloud 2025.1.2
* Spring Data Redis Reactive
* Spring Cloud Netflix Eureka Client
* Java 25

## Port & Routing Configuration
* **Default Port:** `8080`
* **Core Service Routes:** `/api/v1/core/**` -> `lb://core-service`
* **AI Service Routes:** `/api/v1/ai/**` -> `lb://ai-service`

## Key Features & Filters
* **Token-Bucket Rate Limiting:** Backed by Upstash Redis, restricting abusive traffic to 10 requests/second with a burst allowance of 20 via `#{@ipKeyResolver}`.
* **Dynamic Service Discovery:** Queries `eureka-server` dynamically to resolve `lb://` addresses into live container IPs.

## Key Configuration (`application.yml`)
```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 10
            redis-rate-limiter.burstCapacity: 20
            key-resolver: "#{@ipKeyResolver}"
```
