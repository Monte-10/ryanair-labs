# Interconnecting Flights API

This API, developed in Spring Boot, retrieves direct and interconnecting flights with a maximum of one layover. It utilizes Ryanair's API to fetch available routes within a given date range.

## Table of Contents
- [Requirements](#requirements)
- [Installation](#installation)
- [Compilation](#compilation)
- [Running the Application](#running-the-application)
- [API Usage](#api-usage)
- [Swagger Documentation](#swagger-documentation)
- [Testing](#testing)
- [Building and Running the JAR](#building-and-running-the-jar)
- [Architecture and Design](#architecture-and-design)
- [Notes](#notes)

## Requirements
Before starting, make sure you have the following installed:
- Java 17 - Required for Spring Boot 3.2.4
- Maven 3.8+ - For dependency management and compilation
- Git (Optional) - To clone the repository

Check your installed versions:
```bash
java -version
mvn -version
git --version
```

## Installation
Clone the repository:
```bash
git clone https://github.com/Monte-10/ryanair-labs.git
cd interconnecting-flights
```

## Compilation
To compile the project and ensure everything is working correctly, run:
```bash
mvn clean install
```
This will:
1. Download the necessary dependencies
2. Compile the source code
3. Run unit tests
4. Generate an executable JAR file

If everything works fine, you should see "BUILD SUCCESS".

## Running the Application
Once compiled, start the API with:
```bash
mvn spring-boot:run
```
This will launch the server at `http://localhost:8080/`.

Alternatively, you can run the generated JAR file:
```bash
java -jar target/interconnecting-flights-0.0.1-SNAPSHOT.jar
```

## API Usage
You can query available flights using cURL or a web browser.

### Example Request:
```bash
curl "http://localhost:8080/interconnections?departure=DUB&arrival=WRO&departureDateTime=2025-06-10T07:00:00Z&arrivalDateTime=2025-06-10T21:00:00Z"
```
Response:
```json
[
  {
    "stops": 1,
    "legs": [
      {
        "departureAirport": "DUB",
        "arrivalAirport": "BGY",
        "departureDateTime": "2025-06-10T11:50:00",
        "arrivalDateTime": "2025-06-10T15:20:00"
      },
      {
        "departureAirport": "BGY",
        "arrivalAirport": "WRO",
        "departureDateTime": "2025-06-10T19:20:00",
        "arrivalDateTime": "2025-06-10T21:00:00"
      }
    ]
  },
  {
    "stops": 1,
    "legs": [
      {
        "departureAirport": "DUB",
        "arrivalAirport": "BVA",
        "departureDateTime": "2025-06-10T07:20:00",
        "arrivalDateTime": "2025-06-10T09:55:00"
      },
      {
        "departureAirport": "BVA",
        "arrivalAirport": "WRO",
        "departureDateTime": "2025-06-10T15:35:00",
        "arrivalDateTime": "2025-06-10T17:30:00"
      }
    ]
  },
...
```

### Parameters:
| Parameter | Description | Example |
|-----------|------------|---------|
| `departure` | IATA code of the departure airport | `DUB` |
| `arrival` | IATA code of the arrival airport | `WRO` |
| `departureDateTime` | Minimum departure date & time (ISO 8601) | `2025-06-10T07:00:00Z` |
| `arrivalDateTime` | Maximum arrival date & time (ISO 8601) | `2025-06-10T21:00:00Z` |

## Swagger Documentation
This API includes an interactive Swagger UI.

Access it at:  
`http://localhost:8080/swagger-ui/index.html`

Alternatively, fetch the JSON documentation:
```bash
curl "http://localhost:8080/v3/api-docs"
```

## Testing
The project includes unit and integration tests. To run them:
```bash
mvn test
```
For detailed logs:
```bash
mvn test -X
```

## Building and Running the JAR
To create the executable JAR file, run:
```bash
mvn clean package
```
This will generate:
```
target/interconnecting-flights-0.0.1-SNAPSHOT.jar
```

You can execute the JAR file with:
```bash
java -jar target/interconnecting-flights-0.0.1-SNAPSHOT.jar
```
To change the default port, specify it:
```bash
java -jar target/interconnecting-flights-0.0.1-SNAPSHOT.jar --server.port=9090
```
To stop the server, press `CTRL + C`.

## Architecture and Design

### Why Hexagonal Architecture?

This project follows **Hexagonal Architecture (Ports and Adapters)** to create a **decoupled and maintainable** software design. The key benefits include:

- **Separation of concerns**: Business logic is independent of external APIs.  
- **Testability**: The core logic can be tested without relying on external services.  
- **Scalability**: Adding new airline APIs or modifying existing components does not impact the system.  
- **Flexibility**: External dependencies can be easily swapped or replaced.  

### Project Structure

1. **Domain Layer**  
   - Core business logic and models.  
   - **Example:** `FlightLeg.java` (flight segment model).  

2. **Application Layer**  
   - Implements use cases.  
   - **Example:** `FlightService.java` (orchestrates flight searches).  

3. **Infrastructure Layer**  
   - Handles communication with external services.  
   - **Example:** `RoutesClient.java` (fetches routes from Ryanair API).  

4. **API Layer**  
   - Exposes REST endpoints.  
   - **Example:** `InterconnectionsController.java` (manages HTTP requests).  

### SOLID Principles in Action

- **SRP**: `FlightService.java` only handles flight search logic.  
- **OCP**: New airline APIs can be integrated without modifying `FlightService.java`.  
- **LSP**: API clients are interchangeable due to interface-based design.  
- **ISP**: `RoutesClient.java` and `SchedulesClient.java` focus on separate concerns.  
- **DIP**: High-level modules depend on abstractions rather than concrete implementations.  

### Why OpenAPI?

- Provides standardized API documentation.  
- Enables automatic client SDK generation.  
- Offers an interactive UI for testing (Swagger).  
- Ensures contract validation between API and clients.  

### Potential Improvements

- Modularizing into separate Maven modules for better maintainability.  
- Separating the project into multiple Maven modules:
    - boot → Application entry point (Spring Boot setup).
    - domain → Contains only business logic and models.
    - application → Implements use cases.
    - infrastructure → Handles external API communication.
    - api-rest → Exposes REST controllers.
    
    `Pros`:

    - Improves maintainability and scalability.
    - Makes it easier to replace or extend individual components.
    - Improves testing (e.g., testing domain logic without infrastructure dependencies).
    
    `Cons`:

    - Increases project complexity for small-scale applications.

- Adding caching mechanisms to improve performance.  

## Notes
- Built with Spring Boot 3.2.4.  
- External Ryanair API must be operational for real flight routes.  
- WireMock is used for integration tests.  