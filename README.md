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

## Report Answers

### Part 1 Questions

**Q1: Does JAX-RS create a new resource class instance per request or use a singleton? How does this affect in-memory data management?**

By default, JAX-RS creates a new instance of each resource class for every incoming request. 
This is called the "per-request" lifecycle. This means that if we stored data in instance variables 
of the resource class, it would be lost after each request completes because the object gets destroyed. 
To solve this, we use the Singleton design pattern for our repository classes 
(RoomRepository, SensorRepository, SensorReadingRepository). Each repository has a single static instance 
that is shared across all requests, so data stored in the ConcurrentHashMap inside each repository persists 
for the lifetime of the application. Without this approach, our in-memory data would disappear after every single API call.

**Q2: Why are hypermedia links (HATEOAS) considered good REST design?**

HATEOAS (Hypermedia As The Engine Of Application State) is considered good REST design because it makes the 
API self-discoverable. Instead of clients needing to hardcode every URL, they can start at the root endpoint 
and follow the links in each response to find related resources — similar to how users browse a website by 
clicking links. This reduces coupling between client and server because if URLs change, clients just follow 
the updated links rather than breaking. It also serves as built-in documentation, telling the client exactly 
what actions are available for each resource at any given time.

---

### Part 2 Questions

**Q3: What are the trade-offs of returning IDs only versus full objects when listing rooms?**

Returning IDs only produces smaller response payloads and is faster to serialise and transmit, which is beneficial 
when there are hundreds or thousands of rooms. However, the client must then make a separate GET request for each 
room to see its details, creating the "N+1 problem" where listing 100 rooms requires 101 total requests. Returning 
full objects gives the client all data in a single request, but increases the payload size and may include data the 
client does not need. A common middle ground is returning summary representations (key fields like id and name) with 
hypermedia links to the full resource, which is the approach used in this API.

**Q4: Is your DELETE idempotent? What happens if the same DELETE is sent twice?**

The first DELETE request for a room that exists (and has no sensors) successfully removes it and returns 204 No Content.
 The second identical DELETE request will return 404 Not Found because the room no longer exists in the repository. 
Strictly speaking, this means the HTTP response code differs between the two calls, which some consider not perfectly 
idempotent. However, the server-side state is identical after both calls — the room is gone — which satisfies the REST 
definition of idempotency (the state of the server does not change with repeated identical requests). Returning 404 on 
the second call is more informative than silently returning 204 for something that does not exist.

---

### Part 3 Questions

**Q5: What happens if a client sends XML instead of JSON to a @Consumes(APPLICATION_JSON) endpoint?**

The server will reject the request with a 415 Unsupported Media Type status code before the resource method is even invoked. 
JAX-RS automatically inspects the Content-Type header of the incoming request and compares it against the media types 
declared in the @Consumes annotation. If the client sends Content-Type: application/xml but the endpoint only accepts 
application/json, Jersey returns 415 immediately. The resource method code never runs, which means no processing or 
validation is wasted on a request the server cannot understand.

**Q6: Why are query parameters (?type=CO2) better than path segments (/type/CO2) for filtering?**

Query parameters are better for filtering because they are optional by nature — the base URL /api/v1/sensors works perfectly
 without any query parameter and returns all sensors. Path segments like /sensors/type/CO2 imply a hierarchical resource 
structure and suggest that "type" is a sub-resource of sensors, which is semantically incorrect. Query parameters also make 
it trivial to combine multiple filters (e.g., ?type=CO2&status=ACTIVE) without creating complicated nested URL patterns. 
They follow standard URI conventions that developers expect, and HTTP caches can handle them appropriately.

---

### Part 4 Questions

**Q7: What are the benefits of the Sub-Resource Locator pattern versus putting everything in one big class?**

The Sub-Resource Locator pattern provides several benefits. First, it enforces separation of concerns by keeping 
reading-specific logic in SensorReadingSubResource rather than bloating the SensorResource class with additional methods. 
Second, the code structure mirrors the URL hierarchy (sensors/{id}/readings), making the codebase intuitive to navigate. 
Third, the parent resource method acts as a validation gateway — it checks the sensor exists and retrieves it before 
delegating to the sub-resource, avoiding duplicated validation logic. Fourth, each class has a single focused responsibility, 
making unit testing simpler. Finally, it makes the codebase more maintainable as the API grows, because new sub-resources 
can be added without modifying existing classes.

---

### Part 5 Questions

**Q8: Why is 422 more appropriate than 404 for a missing reference inside valid JSON?**

404 Not Found means the URL endpoint itself could not be found, which is misleading in this scenario because the endpoint 
/api/v1/sensors exists and works correctly. The real problem is that the JSON body contains a roomId that references a 
room which does not exist in the system. 422 Unprocessable Entity correctly communicates that the request was syntactically 
valid (the JSON is well-formed and the endpoint is correct) but the server cannot process it because the semantic content is 
invalid — a referenced resource does not exist. This distinction helps clients understand that they need to fix the data in 
their request body, not the URL they are calling.

**Q9: From a security perspective, why is exposing Java stack traces to users dangerous?**

Stack traces reveal internal implementation details such as fully qualified class names, method names, line numbers, file 
paths, library names and version numbers, and the overall technology stack. An attacker can use this information to 
identify known vulnerabilities in specific library versions (e.g., a known exploit in a particular Jackson version), 
understand the application architecture to craft targeted attacks, discover potential injection or file path traversal 
entry points, and map internal package structures. This is why the CatchAllExceptionMapper in this API returns a generic 
"An unexpected error occurred" message to the client while logging the actual stack trace on the server console where 
only developers can see it.
