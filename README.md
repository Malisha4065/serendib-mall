# Serendib Mall - Microservices E-Commerce Platform

A modern, event-driven e-commerce platform built with microservices architecture, implementing CQRS, Event Sourcing, and the Saga pattern.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Client Applications                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BFF (Backend for Frontend)                           │
│                    GraphQL API Gateway - Port 8080                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │ gRPC
              ┌────────────────────────┼────────────────────────┐
              ▼                        ▼                        ▼
┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│  Product Services    │  │  Inventory Service   │  │    Order Service     │
│  (CQRS Pattern)      │  │      Port 8085       │  │      Port 8084       │
│                      │  │      gRPC: 9091      │  │      gRPC: 9094      │
│  Command: 8083/9093  │  └──────────────────────┘  └──────────────────────┘
│  Query:   8086/9090  │             │                        │
└──────────────────────┘             │                        │
         │                           │                        │
         │ CDC                       │ CDC                    │ CDC
         ▼                           ▼                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Debezium Connect (Change Data Capture)                    │
│                              Port 8087                                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Redpanda (Kafka-compatible Message Broker)                │
│                         Port 9092 (external), 29092 (internal)               │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
              ┌────────────────────────┼────────────────────────┐
              ▼                        ▼                        ▼
┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│  Product Query Svc   │  │  Inventory Service   │  │  Payment Service     │
│  (Event Consumer)    │  │  (Event Consumer)    │  │     Port 8089        │
└──────────────────────┘  └──────────────────────┘  └──────────────────────┘
```

## Technology Stack

| Component | Technology |
|-----------|------------|
| **Language** | Java 21, Spring Boot 3.x |
| **API Gateway** | GraphQL (Spring for GraphQL) |
| **Service Communication** | gRPC |
| **Message Broker** | Redpanda (Kafka-compatible) |
| **CDC** | Debezium |
| **Databases** | PostgreSQL 16 (write), Elasticsearch 8.11 (read) |
| **Caching** | Redis 7 |
| **Tracing** | Zipkin |
| **Containerization** | Docker Compose |

## Microservices

### 1. BFF (Backend for Frontend)
- **Port**: 8080
- **Purpose**: GraphQL API gateway that aggregates data from multiple microservices
- **Communication**: gRPC clients to downstream services

**GraphQL Operations:**
```graphql
# Queries
product(id: ID!): ProductDetails

# Mutations
createProduct(input: CreateProductInput!): ProductDetails
createOrder(productId: ID!, quantity: Int!): Order
```

### 2. Product Command Service
- **HTTP Port**: 8083
- **gRPC Port**: 9093
- **Database**: PostgreSQL (`product_db`)
- **Purpose**: Handles write operations for products (CQRS - Command side)
- **Features**: 
  - Creates products
  - Publishes events via Outbox pattern
  - Flyway database migrations

### 3. Product Query Service
- **HTTP Port**: 8086
- **gRPC Port**: 9090
- **Database**: Elasticsearch
- **Purpose**: Handles read operations for products (CQRS - Query side)
- **Features**:
  - Consumes product events from Kafka
  - Full-text search capabilities
  - Optimized for read performance

### 4. Inventory Service
- **HTTP Port**: 8085
- **gRPC Port**: 9091
- **Database**: PostgreSQL (`inventory_db`)
- **Cache**: Redis
- **Purpose**: Manages product inventory/stock levels
- **Features**:
  - Stock reservation (Saga participant)
  - Redis caching for fast lookups
  - Outbox pattern for reliable messaging

### 5. Order Service
- **HTTP Port**: 8084
- **gRPC Port**: 9094
- **Database**: PostgreSQL (`order_db`)
- **Purpose**: Manages order lifecycle
- **Features**:
  - Order creation and status management
  - Saga orchestrator for order fulfillment
  - Outbox pattern for reliable messaging

### 6. Payment Service
- **HTTP Port**: 8089
- **Database**: PostgreSQL (`payment_db`)
- **Purpose**: Handles payment processing
- **Features**:
  - Payment processing (Saga participant)
  - Outbox pattern for reliable messaging

## Infrastructure Components

### PostgreSQL
- **Port**: 5432
- **Databases**: `product_db`, `inventory_db`, `order_db`, `payment_db`
- **Configuration**: WAL level set to `logical` for Debezium CDC

### Redis
- **Port**: 6379
- **Purpose**: Caching layer for inventory service

### Elasticsearch
- **Port**: 9200
- **Purpose**: Read-optimized storage for product search

### Redpanda
- **External Port**: 9092
- **Internal Port**: 29092
- **Console**: 8088
- **Purpose**: Kafka-compatible message broker for event streaming

### Debezium Connect
- **Port**: 8087
- **Purpose**: Change Data Capture from PostgreSQL outbox tables
- **Connectors**:
  - `product-events-connector`
  - `order-events-connector`
  - `inventory-outbox-connector`
  - `payment-outbox-connector`

### Zipkin
- **Port**: 9411
- **Purpose**: Distributed tracing across all microservices

## Key Patterns Implemented

### CQRS (Command Query Responsibility Segregation)
The Product domain is split into separate services:
- **Command Service**: Writes to PostgreSQL
- **Query Service**: Reads from Elasticsearch
- Events synchronize data between the two

### Event Sourcing
State changes are captured as events and published through Kafka/Redpanda.

### Outbox Pattern
All services use outbox tables for reliable event publishing:
- Events are written to an outbox table within the same transaction
- Debezium captures changes and publishes to Kafka
- Guarantees at-least-once delivery

### Saga Pattern
Order fulfillment uses choreography-based saga:
1. Order Service creates order
2. Inventory Service reserves stock
3. Payment Service processes payment
4. Services publish success/failure events

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 21 (for local development)
- Maven (for local development)

### Running the Stack

```bash
# Start all services
docker compose up -d --build

# View logs
docker compose logs -f

# Stop all services
docker compose down

# Reset everything (including data)
docker compose down -v
```

### Service URLs

| Service | URL |
|---------|-----|
| GraphQL Playground | http://localhost:8080/graphiql |
| Redpanda Console | http://localhost:8088 |
| Zipkin UI | http://localhost:9411 |
| Debezium Connect | http://localhost:8087 |
| Elasticsearch | http://localhost:9200 |

### Register Debezium Connectors

After starting the stack, register the CDC connectors:

```bash
# Product events connector
./infra/debezium/register-connector.sh

# Order events connector
./infra/debezium/register-order-connector.sh

# Inventory outbox connector
./infra/debezium/register-inventory-outbox-connector.sh

# Payment outbox connector
./infra/debezium/register-payment-outbox-connector.sh
```

## Example GraphQL Operations

### Create a Product
```graphql
mutation {
  createProduct(input: {
    name: "Laptop"
    description: "High-performance laptop"
    price: 999.99
    currency: "USD"
    category: "Electronics"
  }) {
    id
    name
    price
  }
}
```

### Query a Product
```graphql
query {
  product(id: "product-001") {
    id
    name
    description
    price
    stockLevel
  }
}
```

### Create an Order
```graphql
mutation {
  createOrder(productId: "product-001", quantity: 2) {
    id
    status
    productId
  }
}
```

## Project Structure

```
serendib-mall/
├── docker-compose.yml          # Docker orchestration
├── proto/                      # Shared gRPC protocol definitions
│   ├── product/
│   ├── inventory/
│   ├── order/
│   └── payment/
├── infra/
│   ├── postgres/
│   │   └── init.sql           # Database initialization
│   └── debezium/
│       └── *.sh               # Connector registration scripts
├── serendibmall-bff/          # GraphQL BFF service
├── product-command-service/   # Product write service (CQRS)
├── product-query-service/     # Product read service (CQRS)
├── inventory-service/         # Inventory management
├── order-service/             # Order management
└── payment-service/           # Payment processing
```

## Troubleshooting

### Services exit immediately after startup
If databases don't exist, services will fail. Ensure PostgreSQL volume is fresh:
```bash
docker compose down -v
docker compose up -d --build
```

### Debezium connection timeout
Debezium may fail if Redpanda isn't fully ready. The compose file includes `restart: on-failure:5` to handle this automatically.

### Check service health
```bash
docker compose ps
docker compose logs <service-name>
```

### Verify Kafka topics
Access Redpanda Console at http://localhost:8088 to view topics and messages.

## License

This project is for educational and demonstration purposes.
