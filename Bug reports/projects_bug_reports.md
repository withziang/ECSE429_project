# Bug Reports: Projects API

Each bug has a failing test in `ProjectsDocumentedBehaviorTest` and a passing test in `ProjectsActualBehaviorTest`. The curl commands are in `Exploratory testing scripts/Projects/projects_session.sh`, section "Behavior that differs from the documentation".

## PROJ-001

**Summary**: PUT /projects/:id resets omitted fields and removes task and category links.

**Description**:
The documentation describes POST and PUT `/projects/:id` with the same sentence: amend a project "with a body containing the fields to amend". POST behaves that way. PUT replaces the whole project instead. Every field missing from the body goes back to its default, and the project loses all of its `tasks` and `categories` links, even when the body contains every field.

**Potential impact**:
A client that renames a project with PUT silently loses its description, its active and completed flags, and its links to todos and categories. The todos also lose their `tasksof` link, so they no longer belong to any project.

**Steps to reproduce**:
1. Create a project with a title, a description and `"active": true`.
2. Link an existing todo with POST `/projects/:id/tasks`.
3. Send PUT `/projects/:id` with the body `{"title": "Renamed by PUT"}`.
4. Send GET `/projects/:id`.

**Expected result**: The title changes. The description, the active flag and the task link are kept.

**Actual result**: The description is empty, active is false and the project has no tasks.

**Tests**: `putAmendsOnlyTheSuppliedFields`, `putKeepsTheTasksOfTheProject` (expected), `putResetsOmittedFieldsToTheirDefaults`, `putRemovesTheTasksOfTheProject` (actual).

## PROJ-002

**Summary**: Relationship GET on a nonexistent project returns 200 with other projects' data.

**Description**:
GET and HEAD on `/projects/:id/tasks` and `/projects/:id/categories` do not check that the project exists. For an id that matches no project the API answers 200 and returns the tasks, or categories, linked to every project in the system. A todo that belongs to two projects appears twice.

**Potential impact**:
A client that asks for the tasks of a deleted or mistyped project receives a plausible list that belongs to other projects, with no error to signal the mistake. Decisions made from that list are based on wrong data.

**Steps to reproduce**:
1. Start the application with its default data, where project 1 has todos 1 and 2.
2. Send GET `/projects/0/tasks`.

**Expected result**: 404 with an error message, as GET `/projects/0` returns.

**Actual result**: 200 with todos 1 and 2 in the body.

**Tests**: `getTasksOfMissingProjectReturnsNotFound`, `getCategoriesOfMissingProjectReturnsNotFound` (expected), `getTasksOfMissingProjectReturnsTheTasksOfOtherProjects`, `getCategoriesOfMissingProjectReturnsTheCategoriesOfOtherProjects` (actual).

## PROJ-003

**Summary**: Deleting a task link from a nonexistent project returns a NullPointerException.

**Description**:
DELETE `/projects/:id/tasks/:id` with a project id that does not exist returns 400 and the body `{"errorMessages":["java.lang.NullPointerException"]}` whenever the todo is a task of some other project. The same happens on `/projects/:id/categories/:id`. When the todo has no project the API returns the correct 404.

**Potential impact**:
The response exposes an unhandled internal exception and reports a client error code for a resource that is simply missing. Clients cannot tell a missing project from a malformed request, and the unhandled path is a sign of missing validation in the delete handler.

**Steps to reproduce**:
1. Start the application with its default data, where todo 1 is a task of project 1.
2. Send DELETE `/projects/0/tasks/1`.

**Expected result**: 404 with the message "Could not find any instances with projects/0/tasks/1".

**Actual result**: 400 with the message "java.lang.NullPointerException". No data changes.

**Tests**: `deleteTaskOfMissingProjectReturnsNotFound` (expected), `deleteTaskOfMissingProjectReportsANullPointerException` (actual).

## PROJ-004

**Summary**: The documented example JSON and XML project inputs are rejected with 400.

**Description**:
The documentation at `/docs` gives an "Example JSON Input to API calls" for a project in which `completed` and `active` are strings (`"false"`, `"true"`), and an "Example XML Input" that contains `<id>null</id>`. Sending either example to POST `/projects` fails. The API only accepts real JSON booleans and rejects any body that carries an id.

**Potential impact**:
A developer who copies the documented example cannot create a project and has to guess the accepted format. The API also returns booleans as strings, so a client cannot send back what it just received.

**Steps to reproduce**:
1. Send POST `/projects` with `{"title":"a qui officia deseru","completed":"false","active":"true","description":"t in voluptate velit"}`.
2. Send POST `/projects` with Content-Type `application/xml` and the documented XML example, including `<id>null</id>`.

**Expected result**: 201 for both requests.

**Actual result**: 400 "Failed Validation: completed should be BOOLEAN, active should be BOOLEAN" for the JSON example and 400 "Invalid Creation: Failed Validation: Not allowed to create with id" for the XML example.

**Tests**: `postAcceptsTheDocumentedJsonExample`, `postAcceptsTheDocumentedXmlExample` (expected), `postRejectsTheDocumentedJsonExample`, `postRejectsTheDocumentedXmlExample` (actual).

## PROJ-005

**Summary**: An existing todo or category cannot be linked to a project with an XML body.

**Description**:
The documentation says to set Content-Type to `application/xml` to send XML. POST `/projects/:id/tasks` with `<todo><id>3</id></todo>` returns 404 "Could not find thing matching value for id" although todo 3 exists. The JSON body `{"id":"3"}` succeeds. A JSON body with a numeric id, `{"id":3}`, fails the same way, which suggests the id is compared as a string and the XML value is parsed as a number. `/projects/:id/categories` has the same defect.

**Potential impact**:
Clients that only speak XML cannot attach existing todos or categories to a project. The 404 message points at a missing todo, which sends the developer looking for the wrong problem.

**Steps to reproduce**:
1. Create a todo with POST `/todos` and note its id, for example 3.
2. Send POST `/projects/1/tasks` with Content-Type `application/xml` and the body `<todo><id>3</id></todo>`.
3. Send GET `/projects/1/tasks`.

**Expected result**: 201, and todo 3 is listed under the tasks of project 1.

**Actual result**: 404 "Could not find thing matching value for id". No link is created.

**Tests**: `postLinksAnExistingTodoFromAnXmlBody` (expected), `postDoesNotFindAnExistingTodoFromAnXmlBody` (actual).
