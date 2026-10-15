# TravelAgg – Travel Booking Aggregator
### Java Spring Boot · Industrial Practice Project

A full-stack travel booking aggregator that lets users search, compare, and book **flights, trains, and buses** across India using a unified interface — with transparent fare breakdowns, add-ons, promo codes, and booking management.

---

## Tech Stack

| Layer        | Technology                                |
|--------------|-------------------------------------------|
| Frontend     | HTML5 · Vanilla CSS · Vanilla JavaScript  |
| Backend      | Java 17 · Spring Boot 3.2                 |
| REST APIs    | Spring Web (MVC)                          |
| Persistence  | Spring Data JPA · Hibernate               |
| Database     | MySQL 8+                                  |
| Build        | Maven 3.9+                               |

---

## Architecture

```
Controller → Service → Repository → Entity → MySQL
     ↕
  Static (HTML / CSS / JS)
```

```
com.travelaggregator/
├── TravelBookingAggregatorApplication.java  ← Entry point
├── config/
│   └── DataSeeder.java                      ← Auto-seeds demo data
├── controller/
│   ├── JourneyController.java               ← GET/POST /api/journeys/*
│   ├── BookingController.java               ← POST/PUT  /api/bookings/*
│   └── FrontendController.java              ← Serves index.html
├── service/
│   ├── JourneyService.java                  ← Search, sort, filter logic
│   └── BookingService.java                  ← Fare calc, booking CRUD
├── repository/
│   ├── JourneyRepository.java               ← JPQL search query
│   └── BookingRepository.java               ← PNR/email lookup
├── entity/
│   ├── Journey.java                         ← journeys table
│   ├── Booking.java                         ← bookings table
│   ├── TransportMode.java  (FLIGHT|TRAIN|BUS)
│   └── BookingStatus.java  (CONFIRMED|CANCELLED|PENDING)
├── dto/
│   ├── JourneySearchRequest.java
│   ├── JourneyResponse.java
│   ├── BookingRequest.java (validated)
│   ├── BookingResponse.java
│   ├── FareCalculationResponse.java
│   └── ApiResponse.java    ← Consistent JSON envelope
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    └── SeatUnavailableException.java
```

---

## Setup & Run

### Prerequisites
- Java 17+  (`java -version`)
- Maven 3.9+ (`mvn -version`)
- MySQL 8+   (`mysql --version`)

### Step 1 – Create the Database

```sql
CREATE DATABASE travel_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Step 2 – Configure Database Credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/travel_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD_HERE
```

### Step 3 – Build & Run

```bash
# From the project root (where pom.xml lives)
mvn clean package -DskipTests
java -jar target/travel-booking-aggregator-1.0.0.jar

# OR run directly via Maven
mvn spring-boot:run
```

### Step 4 – Open the App

Visit: **http://localhost:8080**

The `DataSeeder` automatically inserts 31 demo journeys (flights, trains, buses) on first startup if the table is empty.

---

## REST API Reference

### Journey Endpoints

| Method | URL                         | Description                     |
|--------|-----------------------------|---------------------------------|
| POST   | `/api/journeys/search`      | Search journeys by route & date |
| GET    | `/api/journeys/{id}`        | Get journey by ID               |
| GET    | `/api/journeys/origins`     | List distinct origin cities     |
| GET    | `/api/journeys/destinations`| List distinct destination cities|

**Search Request Body:**
```json
{
  "origin": "Delhi",
  "destination": "Mumbai",
  "travelDate": "2024-12-20",
  "passengers": 2,
  "transportMode": "FLIGHT",
  "sortBy": "PRICE",
  "minFare": 0,
  "maxFare": 10000
}
```

### Booking Endpoints

| Method | URL                            | Description                     |
|--------|--------------------------------|---------------------------------|
| POST   | `/api/bookings`                | Create a booking (recalcs fare) |
| GET    | `/api/bookings/pnr/{pnr}`      | Get booking by PNR              |
| GET    | `/api/bookings/email/{email}`  | Get all bookings by email       |
| PUT    | `/api/bookings/{id}/cancel`    | Cancel a booking                |
| POST   | `/api/bookings/calculate-fare` | Preview fare (no persistence)   |

---

## Fare Calculation Logic

The fare is **always recalculated server-side** before saving (client-submitted fares are never trusted).

```
Per seat:
  Base Fare (from DB)
  + Service Fee (FLIGHT: 5%, TRAIN: 3%, BUS: 4%)
  + GST         (FLIGHT: 5%, TRAIN: 0%, BUS: 5%)
  × passengers
  + Add-ons     (MEAL ₹350, INSURANCE ₹199, PRIORITY ₹149,
                  EXTRA_BAGGAGE ₹500, SEAT_SELECTION ₹99)  × passengers
  − Discount    (PROMO codes applied on gross total)
  = Total Payable
```

**Demo Promo Codes:** `TRAVEL10` (10%), `FIRST20` (20%), `SAVE5` (5%), `STUDENT15` (15%)

---

## Demo Search Queries

| From      | To        | Date       | Mode   |
|-----------|-----------|------------|--------|
| Delhi     | Mumbai    | 2024-12-20 | FLIGHT |
| Delhi     | Mumbai    | 2024-12-20 | TRAIN  |
| Mumbai    | Goa       | 2024-12-20 | BUS    |
| Delhi     | Bangalore | 2024-12-20 | (All)  |
| Bangalore | Chennai   | 2024-12-20 | BUS    |
| Mumbai    | Pune      | 2024-12-20 | BUS    |

---

## Features

- **Unified Search** – Flights, trains, buses in one query
- **Smart Sorting** – Price, Duration, Departure
- **Sidebar Filters** – Mode + Price Range (client-side, instant)
- **Live Fare Calculator** – Debounced, updates on every add-on/pax change
- **Add-ons** – Meal, Insurance, Priority Boarding, Extra Baggage, Seat Selection
- **Promo Codes** – Applied to gross total, validated server-side
- **Booking Persistence** – Full entity saved in MySQL with PNR
- **My Bookings** – Lookup by PNR or email
- **Seat Management** – Seats decremented on booking, restored on cancellation
- **Responsive UI** – Works on mobile, tablet, desktop
- **Bean Validation** – `@Valid` on all request bodies, field-level errors returned
- **Global Error Handler** – Consistent `{ success, message, data, timestamp }` envelope

---

## Project generated for Industrial Practice (IP) — Java Spring Boot
