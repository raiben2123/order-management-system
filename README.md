# Order Management System

Event-driven microservices platform with a distributed saga between order and inventory services, deployed on Kubernetes with full observability.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=spring&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-7.5-231F20?logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-316192?logo=postgresql&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?logo=kubernetes&logoColor=white)
![Helm](https://img.shields.io/badge/Helm-0F1689?logo=helm&logoColor=white)

---

## What this project is

A backend platform that handles the lifecycle of customer orders: validating stock, reserving it across services, confirming or rolling back the order, and exposing everything through metrics and dashboards. The interesting part is not the domain itself but **how the services coordinate**: there is no central transaction. Order and inventory services exchange Kafka events and compensate each other on failure (a choreographed saga).

The project was built as a deliberate exercise in distributed-systems patterns — circuit breakers, retries, idempotent reservations, autoscaling, and observability — rather than as a CRUD demo.

---

## Architecture

```
                ┌───────────────┐
                │  API Gateway  │   (Spring Cloud Gateway)
                └───────┬───────┘
                        │
        ┌───────────────┴────────────────┐
        ▼                                ▼
┌───────────────┐               ┌────────────────────┐
│ order-service │               │ inventory-service  │
│  (Spring 3)   │               │    (Spring 3)      │
└───┬───────┬───┘               └────┬───────────┬───┘
    │       │                        │           │
    │       │  REST (sync, with      │           │
    │       │  circuit breaker)      │           │
    │       └──────────────────────► │           │
    │                                            │
    │       Kafka topics                         │
    │   ┌─────────────────────┐                  │
    └──►│   order-events      │─────────────────►│
        └─────────────────────┘                  │
        ┌─────────────────────┐                  │
        │  inventory-events   │◄─────────────────┘
        └─────────────────────┘
                ▲
                └── consumed by order-service to confirm/cancel

    PostgreSQL (per-service schema)   ·   Redis cache   ·   Prometheus + Grafana
```

### The saga in one paragraph

A `POST /api/orders` creates an order in `PENDING` state and publishes an `OrderCreatedEvent` to Kafka. The inventory service consumes it, reserves stock for every item, and publishes an `InventoryReservedEvent` with the outcome. If any item fails to reserve, the inventory service **compensates** by cancelling its own previous reservations within the same order before publishing the failure. The order service consumes the result event and transitions the order to `CONFIRMED` or `CANCELLED`. No two-phase commit, no distributed transaction — just events and idempotent state.

---

## Tech stack

| Layer | Technologies |
|---|---|
| **Language & framework** | Java 21, Spring Boot 3.2 |
| **Persistence** | PostgreSQL 15, Spring Data JPA, Hibernate |
| **Messaging** | Apache Kafka 7.5 (Confluent) + Zookeeper |
| **Cache** | Redis 7 |
| **Resilience** | Resilience4j (circuit breaker + retry) |
| **Observability** | Spring Boot Actuator, Micrometer, Prometheus, Grafana |
| **Build** | Maven, JaCoCo (coverage) |
| **Containers** | Docker, Docker Compose |
| **Orchestration** | Kubernetes (tested on Minikube), Helm chart, Horizontal Pod Autoscaler |
| **CI/CD** | GitHub Actions |

---

## Services

### `order-service` (port 8081)

REST API for orders. Owns the `orders` and `order_items` tables. Publishes to `order-events`, consumes `inventory-events`.

**Endpoints**

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/orders` | Create an order. Pre-checks inventory via REST (with circuit breaker) before persisting. |
| `GET` | `/api/orders` | List all orders. |
| `GET` | `/api/orders/{orderNumber}` | Get a specific order. |
| `PATCH` | `/api/orders/{orderNumber}/status` | Update order status. |

**Resilience configuration** (in `application.properties`)

- Circuit breaker `inventoryService`: 50% failure rate threshold, sliding window of 10 calls, 10s open state.
- Retry `inventoryService`: 3 attempts, 1s wait between retries.

### `inventory-service` (port 8082)

REST API for products and stock. Owns the `products` and `inventory_reservations` tables. Consumes `order-events`, publishes `inventory-events`.

**Endpoints**

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/products` | Create a product with initial stock. |
| `GET` | `/api/products` | List all products. |
| `GET` | `/api/products/{productId}` | Get a specific product. |
| `PATCH` | `/api/products/{productId}/stock` | Adjust stock manually. |

**Reservation model**

Stock has two columns: `stock_quantity` (physical) and `reserved_quantity` (logical hold). A reservation moves stock from available to reserved without removing it from the warehouse until confirmation. This makes cancellations safe and idempotent.

### `api-gateway` (Spring Cloud Gateway)

Single entry point for both services. Currently configured at the application level; routing rules will be added together with authentication when the next services are introduced.

---

## Running the project

### With Docker Compose (single command)

```bash
docker-compose up -d
```

Brings up everything: PostgreSQL, Redis, Kafka + Zookeeper, Prometheus, Grafana, and both services.

| Service | URL |
|---|---|
| order-service | http://localhost:8081 |
| inventory-service | http://localhost:8082 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin / admin) |

### On Kubernetes

Manifests live under `k8s/`, organized by responsibility (`02-configmaps`, `03-secrets`, `04-storage`, `05-databases`, `06-messaging`, `07-services`, etc.).

```bash
kubectl apply -f k8s/01-namespace
kubectl apply -f k8s/02-configmaps
kubectl apply -f k8s/03-secrets
kubectl apply -f k8s/04-storage
kubectl apply -f k8s/05-databases
kubectl apply -f k8s/06-messaging
kubectl apply -f k8s/07-services
```

A real run on Minikube currently looks like this:

```
NAME                                  READY   STATUS    RESTARTS   AGE
inventory-service-79dc9dc6bd-5g7ls    1/1     Running   0          42m
inventory-service-79dc9dc6bd-h8z9j    1/1     Running   0          42m
order-service-647575f5b8-f8d52        1/1     Running   0          42m
order-service-647575f5b8-w4vtr        1/1     Running   0          42m
kafka-6848cb68b-9dcgn                 1/1     Running   0          42m
postgres-0                            1/1     Running   0          46m
redis-7c8f85d6bf-6xf8x                1/1     Running   0          46m
zookeeper-86b4db56c5-2svrr            1/1     Running   0          42m

NAME                                          TARGETS       MIN   MAX   REPLICAS
hpa/inventory-service                         cpu: 2%/50%   2     10    2
hpa/order-service                             cpu: 0%/50%   2     10    2
```

Both services run with **2 replicas by default and an HPA configured to scale up to 10 pods** at 50% CPU.

### With Helm

A packaged chart is available in `helm-chart/`:

```bash
helm install oms ./helm-chart
```

---

## Try the saga end-to-end

```bash
# 1. Create a product with 100 units of stock
curl -X POST http://localhost:8082/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Mechanical Keyboard","description":"75% layout","price":120.00,"stockQuantity":100}'

# Response: { "productId": "PROD-A1B2C3D4", ... }

# 2. Create an order for 3 units of that product
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId":"CUST-001",
    "items":[{"productId":"PROD-A1B2C3D4","quantity":3,"unitPrice":120.00}]
  }'

# Response: { "orderNumber": "ORD-XXXXXXXX", "status": "PENDING", ... }

# 3. After ~1 second, query the order again
curl http://localhost:8081/api/orders/ORD-XXXXXXXX
# status: "CONFIRMED"   (the saga has completed)
```

To see the **compensation flow**, request more units than available — the order will end up in `CANCELLED` and the reservation log will show the rollback.

---

## Observability

- Both services expose Prometheus metrics at `/actuator/prometheus`.
- Resilience4j health indicators are exposed too, so you can see circuit-breaker state from `/actuator/health`.
- Grafana is preconfigured to talk to Prometheus; dashboards live in `infrastructure/`.

---

## CI/CD

Two GitHub Actions workflows live in `.github/workflows/`:

- **`ci.yml`** — Builds and tests both services on every push to `main` and `develop`, runs the test suite, and uploads JAR artifacts.
- **`docker-build.yml`** — Builds and tags Docker images for each service.

JaCoCo is wired into Maven so coverage reports are generated as part of the build.

---

## Roadmap

This project intentionally focuses on **doing the saga between order and inventory well** rather than spawning many shallow services. Two next steps are planned:

- **`payment-service`** — Will consume `inventory-events` with success outcome, simulate a payment provider, and publish a `payment-events` topic. Order will only transition to `CONFIRMED` once payment succeeds.
- **`notification-service`** — Will subscribe to terminal events (`CONFIRMED`, `CANCELLED`) and emit notifications.

When those land, the api-gateway will get full routing rules and the saga will become four hops instead of two.

---

## Repository layout

```
.
├── api-gateway/                 # Spring Cloud Gateway entry point
├── order-service/               # Order microservice (Spring Boot 3, Java 21)
├── inventory-service/           # Inventory microservice with reservation model
├── k8s/                         # Plain Kubernetes manifests, ordered by concern
├── helm-chart/                  # Packaged Helm chart for one-command deploys
├── infraestructure/             # Prometheus / Grafana config (legacy spelling)
├── infrastructure/              # Prometheus / Grafana config (current)
├── .github/workflows/           # CI and Docker build pipelines
├── docker-compose.yml           # Local full-stack setup
└── init.sql                     # Initial DB bootstrapping
```

---

## Why this exists

Built as a portfolio project to learn and demonstrate **production patterns for distributed systems** — choreographed sagas, idempotent compensations, resilient inter-service calls, autoscaling, and observability — using the Spring Boot ecosystem on Kubernetes.

Feedback and questions are welcome. If you spot something that could be done in a more idiomatic way, open an issue.