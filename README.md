# ECSE 429 Project

Testing the Thingifier REST API todo list manager (v1.5.5).

## Layout
- `Exploratory testing screenshots/` — Screenshots captured during exploratory testing sessions.
- `src/` — Application under test, including the runnable JAR and `run.sh`.
- `test/` — Test suite.
    - `common/` — Shared API client, response utilities, and service checks.
    - `todos/` — Todo endpoint tests.
    - `projects/` — Project endpoint tests.
    - `categories/` — Category endpoint tests.

## Requirements

- Java 8 or later.
- IntelliJ IDEA or another Java IDE.
- JUnit Jupiter 5.13.1:
    - `junit-jupiter`
    - `junit-jupiter-api`
    - `junit-jupiter-params`
    - `junit-jupiter-engine`
- JUnit Platform 1.13.1:
    - `junit-platform-launcher`
    - `junit-platform-engine`
    - `junit-platform-commons`

## Running the application

Start the REST API:

```
./src/run.sh
```

Serves on http://localhost:4567

Documentation:
- General documentation: `http://localhost:4567/docs`
- Swagger documentation: `http://localhost:4567/docs/swagger`

Keep the server running while executing the tests.

Stop the server with Ctrl+C or by sending `GET /shutdown`.

## Running the Tests

1. Start the REST API using the instructions above.
2. Open the project in IntelliJ IDEA.
3. Ensure the JUnit 5 and JUnit Platform dependencies are configured.
4. Run the test suite using `RandomizedTestRunner.java`.

The runner executes tests in randomized order and prints the random seed and test summary. Reusing the same seed can help reproduce the execution order, provided the tests and environment remain unchanged.

## Test Organization

The test suite is organized by API resource, with shared utilities in `common/`. Tests clean up their own test data to support independent execution in randomized order.

Tests cover:
- Documented requests and expected response codes. 
- Invalid operations and nonexistent resources.
- Malformed JSON and XML payloads.