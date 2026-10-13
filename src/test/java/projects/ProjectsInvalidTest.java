package projects;

import static common.HttpStatus.BAD_REQUEST;
import static common.HttpStatus.NOT_ACCEPTABLE;
import static common.HttpStatus.NOT_FOUND;
import static common.HttpStatus.OK;
import static common.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.ApiClient;
import common.ApiResponse;
import common.JsonBodies;
import common.SystemState;
import org.junit.jupiter.api.Test;

class ProjectsInvalidTest extends ProjectsTestSupport {

    private static final String MALFORMED_XML = "<project><title>Kitchen renovation</project>";
    private static final String MALFORMED_JSON = "{\"title\": \"Kitchen renovation\"";

    @Test
    void postRejectsMalformedXml() {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.send("POST", PROJECTS, MALFORMED_XML, ApiClient.XML, ApiClient.JSON);

        assertEquals(BAD_REQUEST, response.status());
        assertTrue(firstErrorMessage(response).contains("Mismatched title and project"));
        assertNothingChangedSince(before);
    }

    @Test
    void postRejectsMalformedJson() {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(PROJECTS, MALFORMED_JSON);

        assertEquals(BAD_REQUEST, response.status());
        assertTrue(firstErrorMessage(response).contains("End of input"));
        assertNothingChangedSince(before);
    }

    @Test
    void amendRejectsMalformedXmlAndKeepsTheProject() {
        String id = createProject();
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.send("PUT", path(id), MALFORMED_XML, ApiClient.XML, ApiClient.JSON);

        assertEquals(BAD_REQUEST, response.status());
        assertNothingChangedSince(before);
    }

    @Test
    void postRejectsAnIdInTheBody() {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(PROJECTS, """
                {"id": "500", "title": "Kitchen renovation"}
                """);

        assertEquals(BAD_REQUEST, response.status());
        assertTrue(firstErrorMessage(response).contains("Not allowed to create with id"));
        assertNothingChangedSince(before);
    }

    @Test
    void postRejectsAnUnknownField() {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(PROJECTS, """
                {"title": "Kitchen renovation", "deadline": "Friday"}
                """);

        assertEquals(BAD_REQUEST, response.status());
        assertEquals("Could not find field: deadline", firstErrorMessage(response));
        assertNothingChangedSince(before);
    }

    @Test
    void amendRejectsANonBooleanStatusAndKeepsTheProject() {
        String id = createProject();
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(path(id), """
                {"active": "sometimes"}
                """);

        assertEquals(BAD_REQUEST, response.status());
        assertEquals("Failed Validation: active should be BOOLEAN", firstErrorMessage(response));
        assertNothingChangedSince(before);
    }

    @Test
    void postRejectsAnUnsupportedContentType() {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.send("POST", PROJECTS, "title=Kitchen",
                "application/x-www-form-urlencoded", ApiClient.JSON);

        assertEquals(UNSUPPORTED_MEDIA_TYPE, response.status());
        assertNothingChangedSince(before);
    }

    @Test
    void getRejectsAnUnsupportedAcceptType() {
        ApiResponse response = ApiClient.send("GET", PROJECTS, null, null, "text/plain");

        assertEquals(NOT_ACCEPTABLE, response.status());
        assertEquals("Unrecognised Accept Type", firstErrorMessage(response));
    }

    @Test
    void getOfMissingProjectReturnsNotFound() {
        ApiResponse response = ApiClient.get(path(MISSING_ID));

        assertEquals(NOT_FOUND, response.status());
        assertEquals("Could not find an instance with projects/" + MISSING_ID, firstErrorMessage(response));
    }

    @Test
    void headOfMissingProjectReturnsNotFound() {
        assertEquals(NOT_FOUND, ApiClient.head(path(MISSING_ID)).status());
    }

    @Test
    void amendOfMissingProjectReturnsNotFoundAndCreatesNothing() {
        SystemState before = SystemState.capture();
        String amendment = """
                {"title": "Kitchen renovation"}
                """;

        assertEquals(NOT_FOUND, ApiClient.post(path(MISSING_ID), amendment).status());
        assertEquals(NOT_FOUND, ApiClient.put(path(MISSING_ID), amendment).status());
        assertNothingChangedSince(before);
    }

    @Test
    void deletingAProjectTwiceFailsTheSecondTime() {
        String id = createProject();
        assertEquals(OK, ApiClient.delete(path(id)).status());

        ApiResponse secondDelete = ApiClient.delete(path(id));

        assertEquals(NOT_FOUND, secondDelete.status());
        assertEquals("Could not find any instances with projects/" + id, firstErrorMessage(secondDelete));
    }

    @Test
    void linkingAMissingTodoReturnsNotFoundAndKeepsTheProject() {
        String projectId = createProject();
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(tasksPath(projectId), JsonBodies.idReference(MISSING_ID));

        assertEquals(NOT_FOUND, response.status());
        assertNothingChangedSince(before);
    }

    @Test
    void linkingAMissingCategoryReturnsNotFoundAndKeepsTheProject() {
        String projectId = createProject();
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(categoriesPath(projectId), JsonBodies.idReference(MISSING_ID));

        assertEquals(NOT_FOUND, response.status());
        assertNothingChangedSince(before);
    }

    @Test
    void linkingToAMissingProjectReturnsNotFound() {
        String todoId = createTodo();
        String categoryId = createCategory();
        SystemState before = SystemState.capture();

        assertEquals(NOT_FOUND, ApiClient.post(tasksPath(MISSING_ID), JsonBodies.idReference(todoId)).status());
        assertEquals(NOT_FOUND, ApiClient.post(categoriesPath(MISSING_ID), JsonBodies.idReference(categoryId)).status());
        assertNothingChangedSince(before);
    }

    @Test
    void deletingATaskLinkTwiceFailsTheSecondTime() {
        String projectId = createProject();
        String todoId = createTodo();
        String taskPath = tasksPath(projectId) + "/" + todoId;
        link(tasksPath(projectId), todoId);
        assertEquals(OK, ApiClient.delete(taskPath).status());

        assertEquals(NOT_FOUND, ApiClient.delete(taskPath).status());
    }

    @Test
    void deletingACategoryLinkThatWasNeverMadeReturnsNotFound() {
        String projectId = createProject();
        String categoryId = createCategory();
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.delete(categoriesPath(projectId) + "/" + categoryId);

        assertEquals(NOT_FOUND, response.status());
        assertNothingChangedSince(before);
    }
}
