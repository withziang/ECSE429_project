package projects;

import static common.HttpStatus.BAD_REQUEST;
import static common.HttpStatus.NOT_FOUND;
import static common.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.ApiClient;
import common.ApiResponse;
import common.SystemState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectsActualBehaviorTest extends ProjectsTestSupport {

    private static final String TITLE_ONLY = """
            {"title": "Bathroom renovation"}
            """;

    @Test
    @DisplayName("[Actual] PUT with only a title resets the other fields")
    void putResetsOmittedFieldsToTheirDefaults() {
        String id = createProject();

        ApiResponse response = ApiClient.put(path(id), TITLE_ONLY);

        assertEquals(OK, response.status());
        assertEquals("", project(id).get("description").asText());
        assertEquals("false", project(id).get("active").asText());
    }

    @Test
    @DisplayName("[Actual] PUT with only a title removes the tasks of the project")
    void putRemovesTheTasksOfTheProject() {
        String projectId = createProject();
        String todoId = createTodo();
        link(tasksPath(projectId), todoId);

        ApiResponse response = ApiClient.put(path(projectId), TITLE_ONLY);

        assertEquals(OK, response.status());
        assertTrue(ids(project(projectId).path("tasks")).isEmpty());
        assertTrue(ids(todo(todoId).path("tasksof")).isEmpty());
    }

    @Test
    @DisplayName("[Actual] tasks of a missing project return 200 with other projects' tasks")
    void getTasksOfMissingProjectReturnsTheTasksOfOtherProjects() {
        String projectId = createProject();
        String todoId = createTodo();
        link(tasksPath(projectId), todoId);

        ApiResponse response = ApiClient.get(tasksPath(MISSING_ID));

        assertEquals(OK, response.status());
        assertTrue(ids(response.json().get("todos")).contains(todoId));
    }

    @Test
    @DisplayName("[Actual] categories of a missing project return 200 with other projects' categories")
    void getCategoriesOfMissingProjectReturnsTheCategoriesOfOtherProjects() {
        String projectId = createProject();
        String categoryId = createCategory();
        link(categoriesPath(projectId), categoryId);

        ApiResponse response = ApiClient.get(categoriesPath(MISSING_ID));

        assertEquals(OK, response.status());
        assertTrue(ids(response.json().get("categories")).contains(categoryId));
    }

    @Test
    @DisplayName("[Actual] deleting a task of a missing project returns 400 NullPointerException")
    void deleteTaskOfMissingProjectReportsANullPointerException() {
        String todoId = createTodo();
        link(tasksPath(createProject()), todoId);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.delete(tasksPath(MISSING_ID) + "/" + todoId);

        assertEquals(BAD_REQUEST, response.status());
        assertTrue(describesNullPointer(firstErrorMessage(response)), firstErrorMessage(response));
        assertNothingChangedSince(before);
    }

    @Test
    @DisplayName("[Actual] linking with an XML body returns 404 and links nothing")
    void postDoesNotFindAnExistingTodoFromAnXmlBody() {
        String projectId = createProject();
        String todoId = createTodo();
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.send("POST", tasksPath(projectId), xmlTodoReference(todoId),
                ApiClient.XML, ApiClient.JSON);

        assertEquals(NOT_FOUND, response.status());
        assertEquals("Could not find thing matching value for id", firstErrorMessage(response));
        assertNothingChangedSince(before);
    }

    @Test
    @DisplayName("[Actual] the documented JSON example is rejected with 400")
    void postRejectsTheDocumentedJsonExample() {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(PROJECTS, DOCUMENTED_JSON_EXAMPLE);

        assertEquals(BAD_REQUEST, response.status());
        assertEquals("Failed Validation: completed should be BOOLEAN, active should be BOOLEAN",
                firstErrorMessage(response));
        assertNothingChangedSince(before);
    }

    @Test
    @DisplayName("[Actual] the documented XML example is rejected with 400")
    void postRejectsTheDocumentedXmlExample() {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.send("POST", PROJECTS, DOCUMENTED_XML_EXAMPLE, ApiClient.XML, ApiClient.JSON);

        assertEquals(BAD_REQUEST, response.status());
        assertTrue(firstErrorMessage(response).contains("Not allowed to create with id"));
        assertNothingChangedSince(before);
    }

    private static boolean describesNullPointer(String errorMessage) {
        return errorMessage.contains("NullPointerException") || errorMessage.contains("is null");
    }
}
