# Eureka Server (`eureka-server`)

## Role & Responsibilities
The Service Discovery registry for the Connecting Dots microservices cluster. It operates as a centralized telephone directory where backend instances register themselves upon startup and discover peer locations dynamically.

## Tech Stack
* Spring Boot 4.0.7
* Spring Cloud Netflix Eureka Server (`@EnableEurekaServer`)
* Spring Cloud 2025.1.2
* Java 25

## Port & Network Configuration
* **Default Port:** `8761`
* **Dashboard URL:** `http://localhost:8761/`

## Key Configuration (`application.yml`)
```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    enable-self-preservation: false # Disabled for local/staging container agility
```
