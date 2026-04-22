
# Smart Campus REST API

A RESTful API built with Java, JAX-RS (Jersey), and Grizzly HTTP server
for managing rooms and sensors on a smart campus.

## Video Demo

[![Video demonstration](https://img.youtube.com/vi/aBJncOUL3ww/0.jpg)](https://www.youtube.com/watch?v=aBJncOUL3ww)

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

---

### Part 1 Questions

**Q1: Does JAX-RS create a new resource class instance per request or use a singleton? How does this affect in-memory data management?**

By default, JAX-RS resource classes use a request scoped lifecycle. This means that for every incoming HTTP request, the runtime creates a new instance of the resource class and destroys it once the response is sent. As a result, any data stored in instance variables inside the resource class does not persist between requests.

If application state were stored directly inside resource classes, it would disappear after each request. To avoid this, persistent in memory data is handled using repository classes such as RoomRepository, SensorRepository, and SensorReadingRepository. These repositories implement the Singleton design pattern using a static getInstance method so that one shared instance exists for the lifetime of the application.

Because these repositories are shared across multiple requests and therefore multiple threads, thread safety becomes important. ConcurrentHashMap is used instead of HashMap to ensure safe concurrent access. AtomicInteger is used for ID generation to guarantee uniqueness even when multiple creation requests occur at the same time. Lists that may be modified concurrently are wrapped using Collections.synchronizedList to prevent concurrent modification issues. 

This design keeps resource classes stateless while ensuring that shared data structures are safe under concurrent access. It also clearly separates request handling from data persistence responsibilities.

---

**Q2: Why are hypermedia links (HATEOAS) considered good REST design?**

HATEOAS, which stands for Hypermedia As The Engine Of Application State, is a core REST constraint and represents the highest level in the Richardson Maturity Model. It promotes the idea that clients should discover available actions dynamically through links provided in responses rather than hardcoding endpoint URLs.

One important benefit is self discoverability. A client can begin at the root endpoint and navigate the API by following links included in each response. This removes the need to memorise or manually construct URL paths.

Another benefit is reduced coupling between client and server. Because clients follow links provided by the server, the server can reorganise its URI structure without breaking existing clients. As long as the links in responses remain accurate, the client does not need to change.

Hypermedia also communicates what actions are currently valid for a resource. For example, a delete link may only be present if deletion is allowed. In this sense, the API behaves like a state machine driven by server provided transitions.

Compared to static documentation, hypermedia responses are always consistent with the actual running system. Static documentation can become outdated, but links generated by the application always reflect the current state and available operations.

---

### Part 2 Questions

**Q3: What are the trade offs of returning IDs only versus full objects when listing rooms?**

Returning only resource identifiers results in smaller payload sizes and faster transmission. This can be beneficial when dealing with large datasets or bandwidth constrained clients. The server also performs less serialisation work in this case.

However, if only IDs are returned, the client must issue additional requests to retrieve detailed information for each resource. For example, listing one hundred rooms would require one request for the list and one hundred additional requests for individual room details. This is commonly referred to as the N plus one problem. It increases latency and server load due to repeated round trips.

Returning full resource representations avoids this issue because all relevant information is delivered in a single response. The trade off is increased payload size and the possibility of over fetching data that the client may not need.

In this API, a balanced approach is used. The list endpoint returns reasonably complete representations including important fields and hypermedia links. Pagination is supported to keep response sizes manageable when the dataset grows.

---

**Q4: Is your DELETE idempotent? What happens if the same DELETE is sent twice?**

According to the HTTP specification, an operation is idempotent if performing the same request multiple times results in the same server state as performing it once. The DELETE method is defined as idempotent in terms of server state.

When a room that exists and has no sensors is deleted for the first time, it is removed from the repository and the server returns 204 No Content. If the same DELETE request is sent again, the room no longer exists and a 404 Not Found response is returned.

Although the response codes differ, the server state remains unchanged after the first successful deletion. The room is removed and remains removed regardless of how many additional DELETE requests are made. Therefore, the implementation satisfies the definition of idempotency because repeated identical requests do not produce additional changes in server state.

Returning 404 on subsequent calls is appropriate because it accurately reflects that the resource no longer exists, rather than pretending the deletion occurred again.

---

### Part 3 Questions

**Q5: What happens if a client sends XML instead of JSON to a @Consumes(APPLICATION_JSON) endpoint?**

If a client sends a request with Content Type set to application xml to an endpoint annotated with @Consumes(MediaType.APPLICATION_JSON), the JAX RS runtime will reject the request with a 415 Unsupported Media Type status code. The resource method will not be executed.

Before invoking the resource method, Jersey compares the Content Type header of the request against the media types declared in the @Consumes annotation. If there is no match, the framework does not proceed further.

Additionally, Jersey attempts to locate a suitable MessageBodyReader to convert the incoming data into the expected Java object. Since only a JSON provider is registered in this project, there is no mechanism to parse XML input. As a result, the framework rejects the request before any business logic runs.

This behaviour ensures that incorrect input formats are blocked at the framework level, preventing unnecessary processing and maintaining strict content negotiation rules.

---

**Q6: Why are query parameters better than path segments for filtering?**

Path parameters are used to identify specific resources within a hierarchy. For example, /api/v1/sensors/sensor1 refers to a single unique sensor resource.

Query parameters, on the other hand, are used to modify or filter the representation of a collection. For example, /api/v1/sensors?type=CO2 still refers to the sensors collection, but applies a filtering constraint.

Using query parameters for filtering is generally preferable for several reasons. They are optional by design, so the same endpoint can return all sensors when no filter is provided. They also allow multiple filters to be combined easily without creating complex routing structures. 

If filtering were implemented using path segments, many route combinations would need to be defined to support different filter orders. Furthermore, path segments imply a resource hierarchy, which is not accurate when the value represents a filter criterion rather than a resource.

Query parameters align with common REST conventions and make the intended behaviour of the endpoint clearer.

---

### Part 4 Questions

**Q7: What are the benefits of the Sub Resource Locator pattern versus putting everything in one large class?**

The Sub Resource Locator pattern allows a parent resource to delegate handling of a nested path to a separate class. In this project, the SensorResource class defines a method for the path sensors/{id}/readings, which returns an instance of SensorReadingSubResource. The actual HTTP methods for handling readings are defined in that dedicated class.

This approach improves separation of concerns. SensorResource focuses only on sensor level operations, while SensorReadingSubResource handles reading related logic. It prevents the resource class from becoming excessively large and difficult to maintain.

The parent resource method can also perform validation before delegation. For example, it verifies that the sensor exists before returning the sub resource. This avoids duplicating validation logic in multiple methods.

As the API grows, additional nested resources such as alerts or calibration records can be implemented as separate classes. This keeps the code modular, easier to navigate, and simpler to test.

---

### Part 5 Questions

**Q8: Why is 422 more appropriate than 404 for a missing reference inside valid JSON?**

A 404 Not Found response indicates that the target URL does not exist. In contrast, 422 Unprocessable Entity indicates that the request was syntactically valid but could not be processed due to semantic errors in the request body.

When a client sends a POST request to create a sensor with a roomId that does not exist, the endpoint itself is valid and reachable. The JSON is correctly formatted. The issue is that the referenced room does not exist in the system.

Returning 404 in this situation would incorrectly suggest that the endpoint is wrong. Returning 422 more accurately communicates that the problem lies in the content of the request rather than the URI. This distinction helps clients handle errors correctly and preserves clear REST semantics.

---

**Q9: From a security perspective, why is exposing Java stack traces to users dangerous?**

Exposing stack traces to API consumers reveals internal implementation details such as class names, package structures, method names, and line numbers. It can also expose information about frameworks and libraries being used.

An attacker could use this information to identify known vulnerabilities in specific libraries, understand how requests are processed internally, and craft targeted attacks based on the structure of the application.

For this reason, the API uses a catch all exception mapper that returns a generic 500 Internal Server Error response. Detailed stack traces are logged internally for developers but are not exposed to external users. This reduces the amount of information available to potential attackers and follows secure design principles.
