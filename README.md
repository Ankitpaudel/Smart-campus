# Smart Campus – Sensor & Room Management API

A JAX-RS RESTful API for the University of Westminster **5COSC022W Client-Server Architectures** coursework.

---

## API Overview

The Smart Campus API manages two primary domain resources:

| Resource | Base Path |
|---|---|
| Rooms | `/api/v1/rooms` |
| Sensors | `/api/v1/sensors` |
| Sensor Readings (sub-resource) | `/api/v1/sensors/{sensorId}/readings` |

The API is built with **JAX-RS (Jersey 2.x)** hosted on a **Grizzly2 embedded HTTP server**. All data is persisted in memory using `ConcurrentHashMap` and `ArrayList`. No database is used.

---

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 11 |
| JAX-RS Implementation | Jersey 2.39.1 |
| Embedded Server | Grizzly2 HTTP Server |
| JSON Serialisation | Jackson 2.15 (via Jersey media module) |
| Build Tool | Maven 3 |

---

## How to Build & Run

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Build
```bash
git clone <https://github.com/Ankitpaudel/Smart-campus.git>
cd smart-campus-api
mvn clean package
```

This produces a fat JAR at `target/smart-campus-api-1.0.0.jar`.

### Run
```bash
java -jar target/smart-campus-api-1.0.0.jar
```

The server starts at: **http://localhost:8080/api/v1**

---

## Sample curl Commands

### 1. Discover the API
```bash
curl -s http://localhost:8080/api/v1 | jq .
```

### 2. Create a Room
```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LAB-202","name":"AI Research Lab","capacity":25}' | jq .
```

### 3. Register a Sensor (valid roomId)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":412.0,"roomId":"LAB-202"}' | jq .
```

### 4. Filter Sensors by type
```bash
curl -s "http://localhost:8080/api/v1/sensors?type=CO2" | jq .
```

### 5. Post a Sensor Reading (updates parent sensor's currentValue)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":430.5}' | jq .
```

### 6. Attempt to delete a Room with sensors (triggers HTTP 409)
```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 | jq .
```

### 7. Register a Sensor with a non-existent roomId (triggers HTTP 422)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":0,"roomId":"GHOST-999"}' | jq .
```

### 8. Post a reading to a MAINTENANCE sensor (triggers HTTP 403)
```bash
# First set sensor status to MAINTENANCE (or create one):
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"OCC-002","type":"Occupancy","status":"MAINTENANCE","currentValue":0,"roomId":"LAB-101"}' | jq .

curl -s -X POST http://localhost:8080/api/v1/sensors/OCC-002/readings \
  -H "Content-Type: application/json" \
  -d '{"value":15}' | jq .
```

---

## Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── Main.java                          # Entry point – starts Grizzly server
    ├── SmartCampusApplication.java        # JAX-RS @ApplicationPath("/api/v1")
    ├── DataStore.java                     # Singleton in-memory data store
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   ├── SensorReading.java
    │   └── ApiError.java
    ├── resource/
    │   ├── DiscoveryResource.java         # GET /api/v1
    │   ├── RoomResource.java              # /api/v1/rooms
    │   ├── SensorResource.java            # /api/v1/sensors
    │   └── SensorReadingResource.java     # sub-resource: /readings
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   ├── SensorUnavailableException.java
    │   └── ExceptionMappers.java          # All @Provider mappers + global safety net
    └── filter/
        └── ApiLoggingFilter.java          # ContainerRequest + ContainerResponse filter
```

---

## Report – Answers to Coursework Questions

### Part 1.1 – JAX-RS Resource Lifecycle
### Question 1.1: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.
### Answer:

By default, JAX-RS creates a **new instance of each Resource class for every incoming HTTP request** (request-scoped lifecycle). This is the specification-mandated default, and Jersey adheres to it. The implication is that no instance-level fields on a resource class are shared between requests — each request starts with a freshly constructed object.

This design matters greatly for in-memory data management. If we stored our `HashMap<String, Room>` as an instance field on `RoomResource`, each request would see a brand-new empty map and data would be lost immediately. To share state across requests, we use a **dedicated singleton `DataStore` class** whose fields are `static` and are backed by `ConcurrentHashMap`. `ConcurrentHashMap` is thread-safe for individual operations (put, get, remove). For compound check-then-act operations (e.g., "if room doesn't exist, create it"), we use `synchronized` blocks to guarantee atomicity and prevent race conditions under concurrent load.

### Part 1.2 – HATEOAS (Hypermedia As The Engine Of Application State)
### Question 1.2: Why is the provision of Hypermedia (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?
### Answer:

HATEOAS is a constraint of REST that requires responses to include links describing what actions a client can take next, rather than relying on hard-coded URLs. The Discovery endpoint at `GET /api/v1` exemplifies this: it returns a `links` map pointing clients to `/api/v1/rooms` and `/api/v1/sensors`. This means:

1. **Clients remain decoupled from URL structure.** If the API changes a path in a future version, only the server-side link must be updated; client code that follows the provided links automatically adapts.
2. **Self-documenting.** A new developer can start at `GET /api/v1` and navigate the entire API without reading external documentation.
3. **Discoverability at runtime.** Machine clients can dynamically explore available actions instead of hard-coding every endpoint.

Compared to static API documentation (Swagger/OpenAPI), HATEOAS keeps navigation data in the live response, so it is always synchronised with the actual server state.

---

### Part 2.1 – ID-only vs Full Object in List Responses
### Question 2.1: When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client-side processing.
### Answer:

Returning **only IDs** (e.g., `["LIB-301", "LAB-101"]`) minimises payload size and is appropriate when the client only needs to know what exists, or will immediately fetch individual resources anyway. However, it forces multiple round-trips (one list call plus N detail calls), increasing latency.

Returning **full objects** (the approach taken here) means the client has everything it needs in a single request — room name, capacity, sensor references — enabling richer UI rendering without further calls. The trade-off is a larger response body. For a campus-scale system with hundreds of rooms this is still negligible on a LAN, but for high-frequency mobile clients one might implement pagination or a `?fields=id,name` sparse-fieldset query parameter.

### Part 2.2 – Idempotency of DELETE
### Question 2.2: Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.
### Answer:

Yes, `DELETE /{roomId}` is **idempotent** in this implementation, which is correct per the HTTP specification. Idempotency means that making the same request N times produces the same server state as making it once.

- **First call**: Room exists → deleted → HTTP 204 No Content.
- **Subsequent calls**: Room no longer exists → HTTP 404 Not Found.

The server state after each call is identical (the room is absent), so the operation is idempotent. The response code *may* differ (204 vs 404), but the HTTP spec permits this; idempotency refers to server state, not response codes. This is important for distributed systems where a client may retry a DELETE after a network timeout without knowing if the first request succeeded.

---

### Part 3.1 – @Consumes and Content-Type Mismatches
### Question 3.1: We explicitly use the @Consumes(MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?
### Answer:

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that the POST endpoint only accepts requests with `Content-Type: application/json`. If a client sends `Content-Type: text/plain` or `Content-Type: application/xml`, JAX-RS will immediately return **HTTP 415 Unsupported Media Type** before the resource method is even invoked — no custom code required. The JAX-RS runtime inspects the `Content-Type` request header during the matching phase and rejects mismatched requests with 415. This is both a security measure (preventing unexpected data formats) and a contract enforcement mechanism, ensuring the entity body is always parseable by Jackson.

### Part 3.2 – @QueryParam vs Path Segment for Filtering
### Question 3.2: You implemented this filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g.,/api/v1/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?
### Answer:

A path segment (`/api/v1/sensors/type/CO2`) implies the segment *identifies a unique resource*. There is no single resource called `type/CO2` — it is a filter criterion applied to a collection. Using `@QueryParam` correctly signals that the parameter *modifies* the collection request without changing its identity. Concrete reasons:

- **Multiple filters are composable**: `?type=CO2&status=ACTIVE` reads naturally; path-based filters would require ugly nested paths like `/sensors/type/CO2/status/ACTIVE`.
- **Optional parameters**: a query string can be absent entirely; path segments cannot be easily optional without defining multiple `@Path` variants.
- **Caching semantics**: `/sensors` and `/sensors?type=CO2` are both representations of the sensors collection; caches can relate them. A path-based design creates entirely separate resource identities.
- **REST conventions**: filtering, sorting, and pagination parameters belong in the query string by universal REST convention (also aligned with how search engines and browser forms work).

---

### Part 4.1 – Sub-Resource Locator Pattern
### Question 4.1: Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path in one massive controller class?
### Answer:

The sub-resource locator (`@Path("/{sensorId}/readings")` returning a `SensorReadingResource` instance) delegates all reading-related logic to a separate class. Benefits:

1. **Single Responsibility Principle**: `SensorResource` handles sensor CRUD; `SensorReadingResource` handles reading history. Each class has one reason to change.
2. **Reduced class size**: In a large API, monolithic resource classes with dozens of methods become impossible to navigate. Delegation keeps every class focused and testable in isolation.
3. **Contextual injection**: The locator passes `sensorId` into the sub-resource constructor, so `SensorReadingResource` always operates in the correct sensor context without needing to re-parse path params.
4. **Reusability**: The sub-resource class can theoretically be reused from multiple parent locators if a similar pattern emerges elsewhere in the API.

---

### Part 5.2 – HTTP 422 vs HTTP 404 for Missing References
### Question 5.2: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?
### Answer:

When a client POSTs a new Sensor with a `roomId` that does not exist, the request itself is **syntactically valid JSON** — the problem is *semantic*, not structural. A 404 (Not Found) is semantically wrong here: 404 means the *requested URL* was not found, not that a field inside the payload references a missing entity.

HTTP 422 (Unprocessable Entity) was specifically defined (RFC 4918, WebDAV) for cases where the server understands the content type and the syntax is correct, but the **semantic instructions cannot be followed** — exactly this situation. Returning 422 communicates to the client: "your JSON was parsed successfully, but the data inside it is logically inconsistent with the current server state." This enables clients to distinguish network/routing errors (404) from data integrity errors (422) and handle them differently in their error-handling logic.

---

### Part 5.4 – Cybersecurity Risks of Exposing Stack Traces
### Question 5.4: From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?
### Answer:
Exposing raw Java stack traces to external consumers creates several attack surfaces:

1. **Package and class name disclosure**: Reveals the internal package structure (e.g., `com.smartcampus.resource.SensorResource`), helping an attacker map the codebase and craft targeted exploits.
2. **Framework and library version fingerprinting**: Stack traces often include third-party library names and versions (e.g., Jersey, Grizzly, Jackson). An attacker can cross-reference these against known CVE databases to find unpatched vulnerabilities.
3. **File path and server environment disclosure**: Stack traces may reveal absolute file system paths (e.g., `/home/deploy/app.jar`), OS details, or container paths that narrow down the attack surface.
4. **Business logic exposure**: The sequence of method calls in a trace reveals internal control flow, making it easier to craft inputs that trigger specific code paths or bypass validation.

The Global `ExceptionMapper<Throwable>` prevents all of this by logging the full trace **server-side** (visible to developers) while returning only a generic `500 Internal Server Error` message to the client.

---

### Part 5.5 – JAX-RS Filters vs Manual Logging
### Question 5.5: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?
### Answer:

Inserting `Logger.info()` calls inside every resource method has several drawbacks:

1. **Repetition and human error**: Developers must remember to add logging to every new endpoint. One forgotten call creates a blind spot in observability.
2. **Violation of DRY and SRP**: Logging is a cross-cutting concern that has nothing to do with business logic. Mixing the two makes resource classes harder to read and test.
3. **Inconsistency**: Different developers may log different fields or use different formats, making log aggregation and search fragile.

A JAX-RS `ContainerRequestFilter` / `ContainerResponseFilter` pair runs automatically for **every** request and response, guaranteed by the framework. This means:
- Complete coverage with zero effort per endpoint.
- Logging format is defined in one place and is consistent across the API.
- Resource methods contain only business logic, improving readability and testability.
- Filters can be activated/deactivated globally (e.g., with `@NameBinding`) without touching resource classes.
