# TradeBridge Africa

Multi-service trade platform. Java 24 + Spring Boot 3.4 + PostgreSQL + Kafka + Redis + MinIO.

## Modules

| Path | Type | Port | Database |
|------|------|------|----------|
| `libs/common-kernel` | library | — | — |
| `libs/security-starter` | library | — | — |
| `libs/messaging-starter` | library | — | — |
| `libs/observability-starter` | library | — | — |
| `libs/validation-starter` | library | — | — |
| `services/api-gateway` | service | 8080 | — |
| `services/identity-service` | service | 8081 | `tb_identity_db` |
| `services/country-service` | service | 8082 | `tb_country_db` |
| `services/notification-service` | service | 8083 | `tb_notification_db` |
| `services/customer-service` | service | 8084 | `tb_customer_db` |

## Prerequisites

- JDK 24 (see `.sdkmanrc` — run `sdk env install`)
- Docker & Docker Compose
- Maven 3.9+ (or use `./mvnw`)

## Local infrastructure

```bash
docker compose up -d

mvn -pl services/identity-service -am clean compile -DskipTests
