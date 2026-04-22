# Smart Campus Sensor Management API

A production-grade RESTful API built with **JAX-RS (Jersey 2.41)** and deployed on **Apache Tomcat** for managing campus IoT infrastructure — rooms, sensor devices, and live measurement data. All data is held entirely in-memory using thread-safe `ConcurrentHashMap` structures; no database is required.

---

## Table of Contents

- [API Design Overview](#api-design-overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [How to Build and Run](#how-to-build-and-run)
- [API Endpoints](#api-endpoints)
- [Sample curl Commands](#sample-curl-commands)
- [Error Handling Strategy](#error-handling-strategy)
- [Conceptual Report — Answers to Coursework Questions](#conceptual-report--answers-to-coursework-questions)

---

## API Design Overview

The Smart Campus Sensor Management API follows RESTful architectural principles with a versioned entry point at `/api/v1`. Three core resource types are managed:

1. **Rooms** (`/api/v1/rooms`) — Physical campus locations that host IoT devices.
2. **Sensors** (`/api/v1/sensors`) — IoT hardware units installed inside rooms.
3. **Readings** (`/api/v1/sensors/{sensorId}/readings`) — Time-series measurement events (sub-resource of Sensors).

Key design decisions:
- **HATEOAS Discovery Endpoint** at `GET /api/v1` provides navigable resource links so clients can explore the API without consulting external documentation.
- **Sub-Resource Locator Pattern** delegates reading history management to a dedicated `SensorReadingResource` class, keeping the codebase modular.
- **Four Custom Exception Mappers** ensure every error path returns a structured JSON body — the API never exposes raw Java stack traces.
- **JAX-RS Logging Filter** provides uniform request/response observability across all endpoints without cluttering resource classes.
- **Business Rule Enforcement** — rooms with active sensors cannot be deleted; sensors in MAINTENANCE or OFFLINE state cannot accept new readings.

---

## Technology Stack

| Component         | Technology                        |
|-------------------|-----------------------------------|
| Language          | Java 17                           |
| API Framework     | JAX-RS 2.1 (Jersey 2.41)         |
| Servlet Container | Apache Tomcat (WAR deployment)    |
| JSON Processing   | Jackson (via jersey-media-json-jackson) |
| Build Tool        | Apache Maven 3.6+                 |
| Data Storage      | `ConcurrentHashMap` (in-memory)   |

---

## Project Structure

```
src/main/java/com/smartcampus/
├── SmartCampusApplication.java                  # @ApplicationPath("/api/v1") (JAX-RS Application)
├── SmartCampusResourceConfig.java               # Jersey ResourceConfig (Tomcat servlet bootstrap)
├── model/
│   ├── SensorRoom.java                          # Campus room entity (POJO)
│   ├── Sensor.java                              # Sensor device entity (POJO)
│   └── SensorReading.java                       # Measurement event entity (POJO)
├── repository/
│   └── DataStore.java                           # Thread-safe singleton in-memory registry
├── resource/
│   ├── DiscoveryResource.java                   # GET /api/v1  (HATEOAS discovery)
│   ├── SensorRoomResource.java                  # /api/v1/rooms  (CRUD)
│   ├── SensorResource.java                      # /api/v1/sensors  (CRUD + filtering)
│   └── SensorReadingResource.java               # Sub-resource: /sensors/{id}/readings
├── exception/
│   ├── ErrorResponse.java                       # Standard JSON error envelope
│   ├── RoomNotEmptyException.java               # 409 Conflict trigger
│   ├── LinkedResourceNotFoundException.java     # 422 Unprocessable Entity trigger
│   └── SensorUnavailableException.java          # 403 Forbidden trigger
├── mapper/
│   ├── RoomNotEmptyExceptionMapper.java         # 409 → JSON
│   ├── LinkedResourceNotFoundExceptionMapper.java # 422 → JSON
│   ├── SensorUnavailableExceptionMapper.java    # 403 → JSON
│   └── GenericExceptionMapper.java              # 500 catch-all safety net
└── filter/
    └── LoggingFilter.java                       # Request + response logging

src/main/webapp/
└── WEB-INF/
    └── web.xml                                  # Tomcat deployment descriptor (Jersey servlet mapping)
```

---

## How to Build and Run

### Prerequisites

- **Java 17** or later — verify with `java -version`
- **Apache Maven 3.6+** — verify with `mvn -version`
- **Apache Tomcat 9.x** (recommended for `javax.*` APIs)

### Step 1 — Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-sensor-api.git
cd smart-campus-sensor-api
```

### Step 2 — Build the WAR (Tomcat)

```bash
mvn clean package
```

This produces `target/smart-campus-sensor-api.war`.

### Step 3 — Deploy to Tomcat

Copy `target/smart-campus-sensor-api.war` into Tomcat’s `webapps/` folder, then start Tomcat.

### Step 4 — Verify the Server is Running

```bash
curl http://localhost:8080/smart-campus-sensor-api/api/v1
```

You should receive a JSON discovery document.

---

## API Endpoints

### Discovery

| Method | Path       | Description                        |
|--------|------------|------------------------------------|
| GET    | /api/v1    | API metadata and resource link map |

### Rooms — `/api/v1/rooms`

| Method | Path               | Description                                   | Success Code |
|--------|--------------------|-----------------------------------------------|-------------|
| GET    | /rooms             | List all campus rooms                         | 200          |
| POST   | /rooms             | Create a new room                             | 201          |
| GET    | /rooms/{roomId}    | Fetch a specific room by ID                   | 200          |
| PUT    | /rooms/{roomId}    | Update room details                           | 200          |
| DELETE | /rooms/{roomId}    | Delete room (blocked if sensors present)      | 200 / 409   |

### Sensors — `/api/v1/sensors`

| Method | Path                    | Description                                      | Success Code |
|--------|-------------------------|--------------------------------------------------|-------------|
| GET    | /sensors                | List all sensors (optional `?type=` filter)      | 200          |
| POST   | /sensors                | Register a new sensor (validates roomId)         | 201          |
| GET    | /sensors/{sensorId}     | Fetch a specific sensor by ID                    | 200          |
| PUT    | /sensors/{sensorId}     | Update sensor properties                         | 200          |
| DELETE | /sensors/{sensorId}     | Remove sensor and its reading history            | 200          |

### Readings — `/api/v1/sensors/{sensorId}/readings`

| Method | Path                              | Description                                         | Success Code |
|--------|-----------------------------------|-----------------------------------------------------|-------------|
| GET    | /sensors/{sensorId}/readings      | Retrieve full measurement history for a sensor      | 200          |
| POST   | /sensors/{sensorId}/readings      | Record a new measurement (blocked if MAINTENANCE)   | 201          |
| GET    | /sensors/{sensorId}/readings/{id} | Fetch a specific reading by ID                      | 200          |

---

## Sample curl Commands

> Replace `{roomId}` and `{sensorId}` with actual UUIDs returned by the API.

**1. Discover the API and list available resources:**
```bash
curl -s http://localhost:8080/smart-campus-sensor-api/api/v1 | python3 -m json.tool
```

**2. Create a new campus room and capture its ID:**
```bash
curl -s -X POST http://localhost:8080/smart-campus-sensor-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name":"Robotics Lab","location":"Block D, Level 2","floor":2,"capacity":25}' \
  | python3 -m json.tool
```

**3. Register a temperature sensor in the newly created room:**
```bash
curl -s -X POST http://localhost:8080/smart-campus-sensor-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"name":"Lab Temp Probe D2-01","type":"Temperature","roomId":"PASTE_ROOM_ID_HERE"}' \
  | python3 -m json.tool
```

**4. Filter sensors by type (e.g. CO2 monitors only):**
```bash
curl -s "http://localhost:8080/smart-campus-sensor-api/api/v1/sensors?type=CO2" | python3 -m json.tool
```

**5. Post a new measurement reading to a sensor:**
```bash
curl -s -X POST http://localhost:8080/smart-campus-sensor-api/api/v1/sensors/PASTE_SENSOR_ID_HERE/readings \
  -H "Content-Type: application/json" \
  -d '{"value":22.7,"unit":"degC"}' \
  | python3 -m json.tool
```

**6. Retrieve the full reading history for a sensor:**
```bash
curl -s http://localhost:8080/smart-campus-sensor-api/api/v1/sensors/PASTE_SENSOR_ID_HERE/readings \
  | python3 -m json.tool
```

**7. Attempt to delete a room that still has sensors (expect 409 Conflict):**
```bash
curl -s -X DELETE http://localhost:8080/smart-campus-sensor-api/api/v1/rooms/PASTE_ROOM_ID_HERE \
  | python3 -m json.tool
```

**8. Attempt to post a reading to a MAINTENANCE sensor (expect 403 Forbidden):**
```bash
# The seeded data includes one sensor in MAINTENANCE state — fetch its ID first:
curl -s "http://localhost:8080/smart-campus-sensor-api/api/v1/sensors?type=Temperature" | python3 -m json.tool
# Then post a reading to the MAINTENANCE sensor ID:
curl -s -X POST http://localhost:8080/smart-campus-sensor-api/api/v1/sensors/MAINTENANCE_SENSOR_ID/readings \
  -H "Content-Type: application/json" \
  -d '{"value":55.0,"unit":"degC"}' \
  | python3 -m json.tool
```

---

## Error Handling Strategy

All errors are returned as a consistent JSON envelope:

```json
{
  "httpStatus": 409,
  "errorCode":  "Conflict",
  "detail":     "Room 'Robotics Lab' cannot be deleted because 1 sensor(s) are still assigned.",
  "occurredAt": "2026-04-20T10:15:30.123Z"
}
```

| Scenario                                     | HTTP Status              | Exception Class                       |
|----------------------------------------------|--------------------------|---------------------------------------|
| Room deleted while sensors are assigned      | 409 Conflict             | `RoomNotEmptyException`              |
| Sensor created with non-existent roomId      | 422 Unprocessable Entity | `LinkedResourceNotFoundException`    |
| Reading posted to MAINTENANCE/OFFLINE sensor | 403 Forbidden            | `SensorUnavailableException`         |
| Any unexpected runtime error                 | 500 Internal Server Error| `GenericExceptionMapper<Throwable>`  |
| Resource not found                           | 404 Not Found            | Inline `ErrorResponse` in resource   |

---

## Conceptual Report — Answers to Coursework Questions

---

### Part 1.1 — JAX-RS Resource Class Lifecycle

By default, JAX-RS operates with a **per-request (prototype) lifecycle**: the runtime instantiates a brand-new resource class object for every incoming HTTP request and discards it once the response has been sent. This means no state is preserved in instance fields across requests.

This design decision has a direct impact on in-memory data management. Because each request receives a freshly created resource object, any data stored as a plain instance field would be lost the moment the request completes. To share data across requests, a **singleton** must be used — this is why `DataStore.getInstance()` is called in every resource constructor rather than storing data locally.

Thread-safety must be explicitly addressed at the singleton level. Since multiple requests can arrive concurrently, plain `HashMap` or `ArrayList` structures would be vulnerable to race conditions — two threads could simultaneously read-modify-write the same collection and produce corrupted state. This project uses `ConcurrentHashMap` for all top-level maps (rooms, sensors) and `Collections.synchronizedList` for per-sensor reading lists, ensuring that concurrent access is handled safely without data loss.

---

### Part 1.2 — HATEOAS and Hypermedia-Driven APIs

HATEOAS (Hypermedia as the Engine of Application State) is the principle that API responses should embed navigable links pointing to related or next-step resources, rather than requiring clients to have prior knowledge of URL patterns from static documentation.

A client that discovers `GET /api/v1` and receives a response containing `"sensors": {"href": "/api/v1/sensors"}` can immediately navigate to the sensor collection without having been told that URL in advance. This makes the API **self-documenting at runtime** — changes to URL structures are propagated through link updates rather than requiring clients to be re-deployed with hardcoded paths. For developers integrating with the API, this dramatically reduces onboarding time and eliminates the risk of using stale documentation. Automated agents and service meshes can also explore and adapt to the API structure dynamically without human intervention.

---

### Part 2.1 — Returning IDs vs Full Objects in List Responses

Returning only IDs in a list response minimises payload size, which is beneficial when clients only need to display a count or perform a lookup. However, it forces each client to issue a follow-up `GET /{id}` request per item to retrieve usable data — a classic **N+1 request problem** that multiplies network round-trips and increases latency proportionally with the list size.

Returning full objects in the list response trades a larger initial payload for **zero additional requests** — the client can render the entire room list, including names, capacities, and sensor counts, from a single response. For typical campus management dashboards where the full room detail is always needed, this is the superior choice. Network bandwidth cost is acceptable given that the dataset (campus rooms) is bounded and room objects are small.

---

### Part 2.2 — Idempotency of DELETE

The DELETE operation in this implementation is **idempotent** by the standard REST definition, which states that repeating the same request must produce the same server state — not necessarily the same response code.

On the **first** `DELETE /api/v1/rooms/{roomId}` call, the room is found and removed; the server returns HTTP 200 with a confirmation body. On any **subsequent** call with the same roomId, the room no longer exists in the registry, so the server returns HTTP 404. The server state does not change between the second, third, or any further identical call — the resource remains absent. This satisfies idempotency: no matter how many times the request is repeated, the outcome (room absent from registry) is identical.

---

### Part 3.1 — Technical Consequences of @Consumes Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation declares the set of media types that the POST method is prepared to process. When a client sends a request with a different `Content-Type` header — for example `text/plain` or `application/xml` — JAX-RS evaluates the annotation during request routing **before** the method is invoked. Finding no match, the runtime automatically returns **HTTP 415 Unsupported Media Type**, and the method body is never executed. This fail-fast behaviour protects the application from receiving unparseable input and eliminates the need for manual content-type validation inside the method.

---

### Part 3.2 — Query Parameters vs Path Segments for Filtering

Using `@QueryParam("type")` for filtering (e.g. `GET /api/v1/sensors?type=CO2`) is superior to embedding the filter in the path (e.g. `GET /api/v1/sensors/type/CO2`) for several reasons:

- **Optionality**: query parameters are inherently optional — omitting the parameter returns the full collection, preserving backward compatibility.
- **Path semantics**: path segments should identify discrete resources or resource hierarchies. Embedding a filter criterion in the path creates a URL that appears to name a specific resource that does not actually exist as a persistent entity.
- **Composability**: multiple filters can be combined naturally (`?type=CO2&status=ACTIVE`) without restructuring the URL.
- **REST convention**: filtering, sorting, and pagination of collections are universally represented as query parameters across established API design standards.

---

### Part 4.1 — Sub-Resource Locator Pattern and Complexity Management

The Sub-Resource Locator pattern delegates responsibility for a nested URL path to a separate, purpose-built class. In this project, any request arriving at `/api/v1/sensors/{sensorId}/readings` is intercepted by a locator method in `SensorResource` that returns a new `SensorReadingResource` instance. JAX-RS then routes the remainder of the path within that returned object.

This approach manages complexity in large APIs by enforcing **single responsibility**: `SensorResource` handles sensor-level CRUD while `SensorReadingResource` exclusively manages reading history. Each class grows independently, can be tested in isolation, and remains readable. The alternative — defining every nested path directly inside a single large controller class — produces a monolith that is difficult to navigate, test, and maintain as the API grows. The pattern also allows the sub-resource to carry contextual state (here, the `ownerSensorId`) without polluting the parent resource.

---

### Part 5.2 — HTTP 422 vs HTTP 404 for Missing Linked Resources

When a client POSTs a new sensor with a `roomId` that does not exist, returning HTTP 404 would be semantically misleading — 404 means the **requested URL** was not found, implying the endpoint `/api/v1/sensors` does not exist, which is false.

HTTP **422 Unprocessable Entity** is the correct choice because the request URL resolved correctly, the JSON payload was syntactically valid, and the server fully understood the client's intent. The problem is a **semantic validation failure**: a field inside the payload references an entity that cannot be resolved within the system. 422 precisely communicates this distinction — the server can read the request but cannot act on it due to a logical inconsistency in the submitted data.

---

### Part 5.4 — Cybersecurity Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers creates several concrete attack vectors:

1. **Internal path disclosure** — file system paths visible in traces reveal the server's directory layout, which can guide targeted file-inclusion or path traversal attacks.
2. **Technology fingerprinting** — fully-qualified class names and package structures reveal the exact frameworks and library versions in use (e.g. `org.glassfish.jersey 2.41`), allowing an attacker to search for published CVEs against those specific versions.
3. **Architecture mapping** — the call stack exposes the application's internal class hierarchy and control flow, helping attackers identify unprotected code branches or potential injection entry points.
4. **Data leakage** — traces from database or network code may inadvertently include connection strings, hostnames, or credentials embedded in exception messages.

The `GenericExceptionMapper` in this project prevents all of the above by logging the full exception server-side only, while returning a single generic sentence to the client that reveals nothing about internal implementation details.

---

### Part 5.5 — JAX-RS Filters vs Inline Logging

Using a JAX-RS filter for logging is superior to inserting `Logger.info()` calls inside every resource method for the following reasons:

- **Universal coverage**: the filter applies automatically to every endpoint — present and future — without requiring any modification to resource classes.
- **Single point of change**: updating the log format, switching logging frameworks, or toggling verbosity requires editing one class rather than dozens.
- **Separation of concerns**: resource methods remain focused on domain logic; cross-cutting infrastructure concerns such as logging, authentication, and metrics are handled at the filter layer.
- **Consistency**: because the same code path handles all requests, the log format is guaranteed to be uniform across the API rather than varying by developer convention.
- **Reduced error risk**: there is no possibility of accidentally omitting logging from a newly added endpoint, which is a common oversight when logging is managed manually.

---

*Report prepared as part of 5COSC022W Client-Server Architectures Coursework — University of Westminster, 2025/26.*
