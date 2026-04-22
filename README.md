# Smart Campus REST API

A RESTful API built with Java, JAX-RS (Jersey), and Grizzly HTTP server
for managing rooms and sensors on a smart campus.

---

## Prerequisites

- Java 11 or higher
- Apache Maven 3.6+
- Apache NetBeans IDE

---

## How to Build and Run

### Using NetBeans
1. Open NetBeans
2. File → Open Project → select the SmartCampusAPI folder
3. Right-click project → Build with Dependencies
4. Right-click project → Run
5. The server starts at http://localhost:8080/api/v1
6. Press Enter in the Output window to stop the server

### Using Command Line
```bash
cd SmartCampusAPI
mvn clean compile
mvn exec:java
```

The server will start at: `http://localhost:8080/api/v1`

---

## API Endpoints

| Method | URL | Description | Success Code |
|--------|-----|-------------|-------------|
| GET | /api/v1 | API discovery with HATEOAS links | 200 OK |
| GET | /api/v1/rooms | List all rooms | 200 OK |
| POST | /api/v1/rooms | Create a new room | 201 Created |
| GET | /api/v1/rooms/{roomId} | Get a specific room | 200 OK |
| DELETE | /api/v1/rooms/{roomId} | Delete a room (blocked if has sensors) | 204 No Content |
| GET | /api/v1/sensors | List all sensors | 200 OK |
| GET | /api/v1/sensors?type=CO2 | Filter sensors by type | 200 OK |
| POST | /api/v1/sensors | Create a new sensor (validates roomId) | 201 Created |
| GET | /api/v1/sensors/{sensorId} | Get a specific sensor | 200 OK |
| GET | /api/v1/sensors/{sensorId}/readings | Get reading history for a sensor | 200 OK |
| POST | /api/v1/sensors/{sensorId}/readings | Add a reading (updates currentValue) | 201 Created |
| PUT | /api/v1/rooms/{roomId} | Update a room | 200 OK |
| PUT | /api/v1/sensors/{sensorId} | Update a sensor (e.g., change status) | 200 OK |
| GET | /api/v1/rooms?page=1&size=10 | List rooms with pagination | 200 OK |
| GET | /api/v1/webhooks | List all webhooks | 200 OK |
| POST | /api/v1/webhooks | Register a webhook | 201 Created |
| GET | /api/v1/webhooks/{webhookId} | Get a specific webhook | 200 OK |
| DELETE | /api/v1/webhooks/{webhookId} | Delete a webhook | 204 No Content |

### Error Responses

| Scenario | HTTP Status | Exception |
|----------|------------|-----------|
| Room/Sensor not found | 404 Not Found | ResourceNotFoundException |
| Delete room that has sensors | 409 Conflict | RoomNotEmptyException |
| Create sensor with non-existent roomId | 422 Unprocessable Entity | LinkedResourceNotFoundException |
| Add reading to MAINTENANCE sensor | 403 Forbidden | SensorUnavailableException |
| Any unexpected server error | 500 Internal Server Error | CatchAllExceptionMapper |

---

## Sample curl Commands

### 1. Discover the API
```bash
curl -X GET http://localhost:8080/api/v1
```

### 2. Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"Lecture Hall A\", \"capacity\": 200}"
```

### 3. List All Rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 4. Get a Specific Room
```bash
curl -X GET http://localhost:8080/api/v1/rooms/room-1
```

### 5. Create a Sensor (linked to room-1)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"type\": \"CO2\", \"status\": \"ACTIVE\", \"roomId\": \"room-1\"}"
```

### 6. List All Sensors
```bash
curl -X GET http://localhost:8080/api/v1/sensors
```

### 7. Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 8. Add a Sensor Reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/sensor-1/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\": 412.5}"
```

### 9. Get All Readings for a Sensor
```bash
curl -X GET http://localhost:8080/api/v1/sensors/sensor-1/readings
```

### 10. Try Deleting a Room That Has Sensors (Error 409)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/room-1
```

### 11. Try Creating a Sensor with Non-Existent Room (Error 422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"type\": \"Temperature\", \"roomId\": \"room-999\"}"
```

### 12. Delete a Room Successfully (room with no sensors)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/room-2
```

### 13. Update a Room
```bash
curl -X PUT http://localhost:8080/api/v1/rooms/room-1 \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"Updated Lecture Hall\", \"capacity\": 250}"
```

### 14. Update Sensor Status to MAINTENANCE
```bash
curl -X PUT http://localhost:8080/api/v1/sensors/sensor-1 \
  -H "Content-Type: application/json" \
  -d "{\"status\": \"MAINTENANCE\"}"
```

### 15. Get Rooms with Pagination
```bash
curl -X GET "http://localhost:8080/api/v1/rooms?page=1&size=2"
```

### 16. Create Room with Missing Name (400 Error)
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d "{\"capacity\": 30}"
```

### 17. Create Sensor with Invalid Status (400 Error)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"type\": \"CO2\", \"status\": \"INVALID\", \"roomId\": \"room-1\"}"
```

---

## Design Decisions

### RESTful Design (Nouns, not Verbs)
All endpoints use noun-based resource URLs following REST conventions:
- `/rooms` (not `/createRoom` or `/getRooms`)
- `/sensors` (not `/registerSensor`)
- `/sensors/{id}/readings` (not `/addReading`)

HTTP methods (GET, POST, DELETE) define the action, not the URL.

### ACID Compliance
Operations that modify multiple data structures (e.g., creating a
sensor updates both the sensor repository AND the room's sensorIds
list) are wrapped in try-catch blocks with rollback logic. If the
second operation fails, the first is undone to maintain data
consistency.

### Thread Safety
- `ConcurrentHashMap` for all data storage (no race conditions on reads/writes)
- `AtomicInteger` for ID generation (no duplicate IDs under concurrent access)
- `Collections.synchronizedList()` for reading lists (safe concurrent appends)
- No primitive data types in models — `Integer`, `Double` wrapper classes used
  to allow null values and avoid autoboxing issues in concurrent contexts

### Memory Management
- Server shutdown uses `try-finally` to guarantee `server.shutdownNow()` is called
- `CatchAllExceptionMapper` prevents uncaught exceptions from leaking resources
- No open streams or connections left dangling

### Scalability Considerations
- Stateless resource classes (per-request lifecycle) mean the API
  can be horizontally scaled behind a load balancer
- In-memory storage could be replaced with a database (e.g., PostgreSQL)
  for production use without changing the resource layer
- Query parameter filtering reduces unnecessary data transfer
- HATEOAS links enable clients to navigate efficiently without
  hardcoded URLs

---

# Report Answers

---

## Part 1 Questions

### **Q1: Does JAX-RS create a new resource class instance per request or use a singleton? How does this affect in-memory data management?**

By default, JAX-RS resource classes follow a **request-scoped lifecycle**, meaning a brand new instance of the resource class is created for every incoming HTTP request and destroyed once the response is sent. This means any data stored in instance variables of a resource class is lost between requests.

JAX-RS also supports a singleton lifecycle by annotating a resource class with `@Singleton` (`jakarta.inject.Singleton`). A singleton resource class is instantiated once when the application starts and the same instance is reused for every request. While this would allow data to persist in instance variables, it introduces concurrency risks because multiple threads (requests) access the same object simultaneously.

In this project, the resource classes remain **request-scoped (default)** to avoid shared mutable state inside the resource layer. Instead, data persistence is handled by dedicated repository classes (`RoomRepository`, `SensorRepository`, `SensorReadingRepository`) that implement the Singleton design pattern using a static `getInstance()` method. This cleanly separates request handling from data storage responsibilities.

To synchronise in-memory data and prevent race conditions, the following strategies are used:

1. **ConcurrentHashMap**  
   Used instead of `HashMap` for all data storage. `ConcurrentHashMap` is thread-safe and allows concurrent reads without locking while segmenting writes to minimise contention.

2. **AtomicInteger for ID Generation**  
   Guarantees unique IDs even when multiple requests arrive simultaneously, without requiring explicit synchronization.

3. **Collections.synchronizedList()**  
   Wraps reading lists to prevent `ConcurrentModificationException` during concurrent updates.

4. **Synchronized getInstance() Methods**  
   Ensures only one repository instance is created even if multiple threads access it during application startup.

These combined techniques ensure that the in-memory data store is thread-safe while keeping resource classes stateless and request-scoped.

---

### **Q2: Why are hypermedia links (HATEOAS) considered good REST design?**

HATEOAS (Hypermedia As The Engine Of Application State) is a core REST constraint and represents **Level 3** of the Richardson Maturity Model.

It is considered good REST design for several reasons:

#### 1. Self-Discoverability  
A client can start at the root endpoint (`/api/v1`) and navigate the API by following links embedded in responses — similar to browsing a website through hyperlinks.

#### 2. Reduced Client-Server Coupling  
Clients do not hardcode URLs. If the server reorganises endpoints or changes URI structures, clients continue functioning by following updated links dynamically.

#### 3. State-Driven Interaction  
Hypermedia communicates which actions are available at a given moment. For example, a room might include a `"delete"` link only if deletion is allowed. This effectively turns the API into a state machine driven by server-provided transitions.

#### 4. Live Documentation  
Static documentation (Swagger, PDFs, wiki pages) can become outdated. Hypermedia links are generated dynamically from the running application — meaning the documentation *is* the API response and is always accurate.

In this API, responses include:
- `rel` (relationship)
- `href` (URL)
- `method` (HTTP verb)

This allows clients to navigate and interact with the system programmatically without external documentation.

---

## Part 2 Questions

### **Q3: What are the trade-offs of returning IDs only versus full objects when listing rooms?**

There are trade-offs involving bandwidth, performance, and usability.

### ID-Only Approach

Example response:
```json
["room-1", "room-2", "room-3"]
```

**Advantages:**
- Extremely small payload  
- Fast serialization  
- Reduced bandwidth usage  
- Good for IoT or mobile environments  

**Disadvantages:**
- Creates the "N+1 problem"  
- Listing 100 rooms requires 101 requests  
- Increased latency and server load  

---

### Full-Object Approach

Example response:
```json
{
  "id": "room-1",
  "name": "Lecture Hall",
  "capacity": 200,
  "sensorIds": []
}
```

**Advantages:**
- Eliminates N+1 problem  
- Client gets all required data in one request  

**Disadvantages:**
- Larger payload (possible over-fetching)  
- More server serialization work  

---

### Middle-Ground Strategy (Used in This API)

This API returns summary representations including:
- `id`
- `name`
- `capacity`
- `sensorIds`
- HATEOAS links

Pagination (`?page=1&size=20`) further controls payload size and scalability.

---

### **Q4: Is your DELETE idempotent? What happens if the same DELETE is sent twice?**

According to RFC 7231 (Section 4.2.2), an operation is idempotent if repeated identical requests result in the same server state.

Consider `DELETE /api/v1/rooms/room-2`:

**Before deletion:**
```
{room-1, room-2}
```

**First DELETE:**
- Room exists  
- No sensors assigned  
- Removed from repository  
- Response: `204 No Content`

State becomes:
```
{room-1}
```

**Second DELETE:**
- Room not found  
- `ResourceNotFoundException` thrown  
- Response: `404 Not Found`

State remains:
```
{room-1}
```

Although the response code changes from 204 to 404, the **server state remains unchanged** after the first deletion. Therefore, the operation satisfies REST’s definition of idempotency.

Returning 404 is more informative than returning 204 repeatedly, as it correctly indicates that the resource no longer exists.

---

## Part 3 Questions

### **Q5: What happens if a client sends XML instead of JSON to a @Consumes(APPLICATION_JSON) endpoint?**

If a client sends:

```
Content-Type: application/xml
```

to an endpoint annotated with:

```java
@Consumes(MediaType.APPLICATION_JSON)
```

JAX-RS returns:

```
HTTP 415 Unsupported Media Type
```

#### Internal Processing Steps:

1. **Content-Type Matching**  
   Jersey checks if the request Content-Type matches `@Consumes`.

2. **MessageBodyReader Lookup**  
   Jersey attempts to find a provider to deserialize the body into a Java object. If only Jackson JSON provider is registered, XML cannot be processed.

3. **Request Rejected Before Business Logic**  
   The resource method is never invoked.

This ensures:
- Framework-level input validation  
- No wasted processing  
- Stronger security posture  

---

### **Q6: Why are query parameters (?type=CO2) better than path segments (/type/CO2) for filtering?**

Path parameters identify a specific resource:

```
/api/v1/sensors/sensor-1
```

Query parameters modify how a collection is represented:

```
/api/v1/sensors?type=CO2
```

#### Reasons Query Parameters Are Superior:

1. **Optionality**  
   The base endpoint works with or without filters.

2. **Avoid Combinatorial Explosion**  
   Path-based filtering requires many route combinations. Query parameters handle all combinations cleanly:
   ```
   ?type=CO2&status=ACTIVE
   ```

3. **Semantic Correctness**  
   `CO2` is not a resource — it is a filter value.

4. **HTTP Caching Compatibility**  
   Query parameters form part of the cache key.

5. **Industry Convention**  
   Used consistently in major APIs (Google, GitHub, Twitter).

---

## Part 4 Questions

### **Q7: What are the benefits of the Sub-Resource Locator pattern versus putting everything in one big class?**

The Sub-Resource Locator pattern allows a parent resource to delegate nested paths to specialised classes.

Example:
```
/api/v1/sensors/{sensorId}/readings
```

The parent `SensorResource` validates the sensor and returns a `SensorReadingSubResource`.

#### Benefits:

1. **Separation of Concerns**  
   Prevents a “God class” with excessive responsibilities.

2. **Single Responsibility Principle**  
   Each class handles one logical feature set.

3. **Delegation & Validation**  
   Parent validates once, child focuses on its specific logic.

4. **Scalability**  
   Supports growth:
   ```
   /sensors/{id}/readings
   /sensors/{id}/alerts
   /sensors/{id}/calibrations
   ```

5. **Testability**  
   Sub-resources can be unit tested independently.

---

## Part 5 Questions

### **Q8: Why is 422 more appropriate than 404 for a missing reference inside valid JSON?**

According to:

- **404 Not Found (RFC 7231)** → Target URL does not exist.  
- **422 Unprocessable Entity (RFC 4918)** → Request syntax is correct, but semantic content is invalid.  

Example:

```
POST /api/v1/sensors
{
  "type": "CO2",
  "roomId": "room-999"
}
```

The endpoint exists.  
The JSON is valid.  
But `room-999` does not exist.

Returning 404 is misleading because the URL is correct.

Returning 422 correctly communicates:
- The request was syntactically valid.  
- The semantic reference inside the payload is invalid.  

This improves:
- Client-side error handling  
- Monitoring accuracy  
- REST semantic clarity  

---

### **Q9: From a security perspective, why is exposing Java stack traces to users dangerous?**

Exposing stack traces is classified by OWASP as **Security Misconfiguration (A05:2021)**.

A stack trace reveals:
- Internal package structure  
- Class and method names  
- Line numbers  
- Library frameworks (Jersey, Jackson)  
- Potential vulnerable versions  
- Control flow paths  

Attackers can:
- Search CVE databases for known exploits  
- Target specific vulnerable components  
- Craft payloads to exploit logic flaws  
- Identify potential deserialization vulnerabilities  

Instead of exposing stack traces, the API returns:

```json
{
  "statusCode": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please try again later."
}
```

Stack traces are logged internally for developers.

This follows:
- Principle of Least Privilege  
- Defense-in-Depth  
- Secure-by-Design principles  
