# ECSE 429 Project

Testing the Thingifier REST API todo list manager (v1.5.5).

## Layout
- `Bug reports/` — Bugs discovered during testing.
- `Exploratory testing session notes/` — Session notes taken by the team.
- `Exploratory testing screenshots/` — Screenshots captured during exploratory testing sessions.
- `src/` — Application under test (`runTodoManagerRestAPI-1.5.5.jar` and `run.sh`).
- `src/test/java/` — Test suite.
    - `common/` — Shared API client, response helpers, and the check that fails tests when the service is not running.
    - `todos/` — Todo endpoint tests.
    - `categories/` — Category endpoint tests.
    - `runner/` — `RandomizedTestRunner`, which runs the whole suite in random order.
- `pom.xml` — Maven build file; declares every dependency.

## Requirements

- Java 17 or later.
- An IDE that imports Maven projects (IntelliJ IDEA, VS Code with the Java extensions, Eclipse), or Maven 3.
- Dependencies are resolved from `pom.xml`; nothing has to be configured by hand:
    - JUnit Jupiter 5.13.1
    - JUnit Platform Launcher 1.13.1 (used by `RandomizedTestRunner`)
    - Jackson Databind 2.18.2 (JSON parsing in assertions)

## Running the application

From the repository root:
```
java -jar src/runTodoManagerRestAPI-1.5.5.jar
```
or `./src/run.sh`.

Serves on http://localhost:4567

Documentation:
- General documentation: `http://localhost:4567/docs`
- Swagger documentation: `http://localhost:4567/docs/swagger`

Keep the server running while executing the tests. If it is not running, every test fails with
"Todo Manager is not running on http://localhost:4567".

Stop the server with Ctrl+C or by sending `GET /shutdown`.

## Running the Tests

1. Start the REST API using the instructions above.
2. Open the project as a Maven project in your IDE.
3. Run the `main` method of `src/test/java/runner/RandomizedTestRunner.java`.

The runner draws a new random seed on every run, shuffles both the test classes and the test methods
inside them, prints each test as it finishes, and ends with a summary and the details of any failures.
To repeat an earlier order, pass its seed as the first program argument (for example `32`).

Single test classes can also be run from the IDE's test view, or the whole suite with `mvn test`
(default order).

## Test Organization

The suite is organized by API resource, one test class per endpoint group, with shared code in `common/`.
Every test creates its own data, records the state it starts from, deletes what it created afterwards,
and checks that the state was restored, so tests can run in any order.

Tests cover:
- Documented behaviour, checked through response content and not only status codes.
- Exact return codes.
- Side effects: other objects must be unchanged after each operation.
- JSON and XML payloads.
- Invalid operations and nonexistent resources.
- Malformed JSON and XML payloads.
- Undocumented behaviour found during exploratory testing.

Where the API does not behave as documented or expected there are two tests: one named
`[Expected, fails]` that asserts the expected behaviour and fails on purpose, and one named `[Actual]`
that asserts what the API really does. These intentional failures are listed in the bug reports.
