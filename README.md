# TicketVerse — Enterprise High-Concurrency Ticket Booking System 🎟️

![Production Ready](https://img.shields.io/badge/System-Production_Ready-brightgreen)
![Spring Boot](https://img.shields.io/badge/Backend-Spring_Boot_3.3-green)
![Angular](https://img.shields.io/badge/Frontend-Angular_18-red)
![Redis](https://img.shields.io/badge/Cache-Redis_7_Redisson-dc382d)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL_16-blue)
![OpenAPI](https://img.shields.io/badge/Docs-Swagger_OpenAPI_3-85ea2d)

A distributed, high-concurrency ticket reservation engine built to handle massive flash-sale traffic spikes, preventing double-bookings with **Redisson Distributed Locks**, **Redis Sorted Set Virtual Waiting Rooms**, **Distributed Background Hold Release Sweeper**, **Idempotent Payments**, and **Cryptographically Verified ZXing QR E-Tickets**.

---

## 🏗️ System Architecture & Key Capabilities

```
+-----------------------------------------------------------------------------------+
|                                 ANGULAR 18 SPA                                    |
|   (Interactive 2D Seat Map, Live Waiting Room, Executive Telemetry Dashboard)     |
+----------------------------------------+------------------------------------------+
                                         | REST & STOMP WebSockets
                                         v
+-----------------------------------------------------------------------------------+
|                              SPRING BOOT 3.3 BACKEND                              |
|   - Bucket4j Token Bucket Rate Limiting Filter                                    |
|   - Redisson Multi-Lock Concurrency Guard (lock:seat:{id})                        |
|   - Redisson Lock-Guarded Background Hold Sweeper (lock:sweeper:seat-holds)        |
|   - Redis TTL Temporary Seat Hold Engine (5-min auto release + WS Push)           |
|   - Virtual Waiting Room & Tokenized Traffic Queue Engine                         |
|   - Idempotent Payment Intent Processor                                           |
|   - Async ZXing QR Code E-Ticket Engine                                           |
|   - Springdoc OpenAPI 3.0 & Swagger UI Integration                                |
+-------------------+------------------------------------------+--------------------+
                    |                                          |
                    v                                          v
+---------------------------------------+  +----------------------------------------+
|           REDIS 7 CLUSTER             |  |         POSTGRESQL 16 DATABASE         |
| - Sorted Sets (Virtual Waiting Room)  |  | - Row-Level Pessimistic Locks (FOR UPDATE)
| - Distributed Multi-Locks & TTL Holds |  | - Transactional Booking Ledger & Tickets|
+---------------------------------------+  +----------------------------------------+
```

---

## 📖 OpenAPI 3.0 Documentation & Swagger UI

Interactive Swagger UI documentation is exposed at:
- **Swagger UI Console**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI 3.0 JSON Spec**: `http://localhost:8080/v3/api-docs`

Postman Collection manifest available at `docs/postman_collection.json`.

---

## 📊 Endpoints & API Reference

### 🎫 Virtual Waiting Room Queue (`/api/v1/queue`)
- `POST /api/v1/queue/join?eventId={id}` — Enters the virtual waiting room for high-demand drops.
- `GET /api/v1/queue/status?eventId={id}&queueToken={token}` — Fetches live queue position and admission pass.
- `POST /api/v1/queue/admin/drain?eventId={id}&batchSize=10` — (Admin) Admits batch of waiting users.

### 📲 Digital E-Tickets & Verification (`/api/v1/bookings`)
- `GET /api/v1/bookings/{id}/ticket-pdf` — Downloads digital pass HTML/PDF with embedded scannable QR code.
- `GET /api/v1/bookings/verify/{ticketCode}` — Validates QR code ticket hash for venue entry scanner.

### 📈 Executive Business Telemetry (`/api/v1/analytics`)
- `GET /api/v1/analytics/dashboard` — Calculates real-time system revenue, occupancy rates, and lock conversion.

---

## 🛠️ Local Development & Quickstart

```bash
# 1. Start Infrastructure Services (PostgreSQL & Redis)
docker-compose up -d

# 2. Build and Run Spring Boot Backend
cd backend
./mvnw clean spring-boot:run

# 3. Build and Run Angular 18 Frontend
cd ../frontend
npm install
npm start
```

---

## 🧪 Concurrency & Unit Test Suite

Run unit and integration test suites:

```bash
# Execute Backend JUnit 5 Test Suite (SeatHold, Booking, Payment, HighConcurrency)
cd backend
./mvnw test

# Run Multi-Threaded Python Concurrency Stress Test (Zero double bookings check)
python load-test/concurrency_load_test.py
```
