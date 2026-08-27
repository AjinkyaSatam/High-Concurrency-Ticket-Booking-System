# TicketVerse — Enterprise High-Concurrency Ticket Booking System 🎟️

![Production Ready](https://img.shields.io/badge/System-Production_Ready-brightgreen)
![Spring Boot](https://img.shields.io/badge/Backend-Spring_Boot_3.3-green)
![Angular](https://img.shields.io/badge/Frontend-Angular_18-red)
![Redis](https://img.shields.io/badge/Cache-Redis_7_Redisson-dc382d)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL_16-blue)

A distributed, high-concurrency ticket reservation engine built to handle massive flash-sale traffic spikes, preventing double-bookings with **Redisson Distributed Locks**, **Redis Sorted Set Virtual Waiting Rooms**, **Idempotent Payments**, and **Cryptographically Verified ZXing QR E-Tickets**.

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
|   - Redis TTL Temporary Seat Hold Engine (5-min auto release)                     |
|   - Virtual Waiting Room & Tokenized Traffic Queue Engine                         |
|   - Idempotent Payment Intent Processor                                           |
|   - Async ZXing QR Code E-Ticket Engine                                           |
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

## ⚡ Complete 12-Phase System Roadmap

| Phase | Module Name | Architectural Description | Status |
| :---: | :--- | :--- | :---: |
| **Phase 1** | **Project Setup & Architecture** | Spring Boot 3.3, PostgreSQL 16, Redis 7, Docker Compose foundation. | ✅ DONE |
| **Phase 2** | **JWT Auth & User Roles** | Spring Security 6 stateless JWT authentication and role-based access. | ✅ DONE |
| **Phase 3** | **Venue & Event Management** | Venue seating configurations and live event publishing controllers. | ✅ DONE |
| **Phase 4** | **Seat Map Grid Generation** | 2D interactive venue seat grid generation (VIP, Premium, Regular). | ✅ DONE |
| **Phase 5** | **Basic Booking Engine** | Transactional ticket reservation workflow and user booking history. | ✅ DONE |
| **Phase 6** | **High Concurrency & Locks** | Redisson distributed multi-locking on seat IDs + DB pessimistic locks. | ✅ DONE |
| **Phase 7** | **Temporary Seat Holds** | Redis TTL seat hold reservations with auto-release countdown. | ✅ DONE |
| **Phase 8** | **Idempotent Payment Gateway** | Idempotency keys to guarantee zero duplicate payment charges. | ✅ DONE |
| **Phase 9** | **Real-time WebSockets** | STOMP WebSocket broadcast of live seat map state changes. | ✅ DONE |
| **Phase 10**| **Rate Limiting & Readiness** | Bucket4j API rate limiting filter and Spring Actuator health probes. | ✅ DONE |
| **Phase 11**| **Virtual Waiting Room Queue** | Redis `zset` tokenized traffic queue engine & live queue UI. | ✅ DONE |
| **Phase 12**| **E-Tickets, Analytics & Suite**| ZXing QR E-Ticket generator, Admin Executive Dashboard & Load Suite. | ✅ DONE |

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

## 🧪 Concurrency Load Test Suite

Run the Python multi-threaded load test script to verify zero double bookings under 50+ concurrent requests:

```bash
python load-test/concurrency_load_test.py
```
