package projects;

import static common.HttpStatus.NOT_FOUND;
import static common.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import common.ApiClient;
import common.ApiResponse;
import common.SystemState;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class ProjectByIdTest extends ProjectsTestSupport {

    private static final String NEW_TITLE = "Bathroom renovation";
    private static final String NEW_DESCRIPTION = "Retile the shower";

    @Test
    void getReturnsRequestedProjectAsJson() {
        String id = createProject();

        ApiResponse response = ApiClient.get(path(id));

        assertEquals(OK, response.status());
        assertEquals(Optional.of(ApiClient.JSON), response.header("Content-Type"));
        JsonNode projects = response.json().get("projects");
        assertEquals(1, projects.size());
        assertEquals(id, projects.get(0).get("id").asText());
        assertEquals(TITLE, projects.get(0).get("title").asText());
        assertEquals(DESCRIPTION, projects.get(0).get("description").asText());
    }

    @Test
    void getReturnsRequestedProjectAsXml() {
        String id = createProject();

        ApiResponse response = ApiClient.send("GET", path(id), null, null, ApiClient.XML);

        assertEquals(OK, response.status());
        assertEquals(Optional.of(ApiClient.XML), response.header("Content-Type"));
        Document project = response.xml();
        assertEquals(1, project.getElementsByTagName("project").getLength());
        assertEquals(id, project.getElementsByTagName("id").item(0).getTextContent());
        assertEquals(TITLE, project.getElementsByTagName("title").item(0).getTextContent());
    }

    @Test
    void headReturnsHeadersWithoutBody() {
        String id = createProject();

        ApiResponse response = ApiClient.head(path(id));

        assertEquals(OK, response.status());
        assertEquals(Optional.of(ApiClient.JSON), response.header("Content-Type"));
        assertTrue(response.body().isEmpty());
    }

    @Test
    void postAmendsOnlyTheSuppliedFields() {
        String id = createProject();
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(path(id), """
                {"description": "%s"}
                """.formatted(NEW_DESCRIPTION));

        assertEquals(OK, response.status());
        JsonNode amended = project(id);
        assertEquals(NEW_DESCRIPTION, amended.get("description").asText());
        assertEquals(TITLE, amended.get("title").asText());
        assertEquals("true", amended.get("active").asText());
        assertOnlyChangedSince(before, "projects/" + id);
    }

    @Test
    void postAmendsProjectFromXml() {
        String id = createProject();

        ApiResponse response = ApiClient.send("POST", path(id), """
                <project><title>%s</title></project>
                """.formatted(NEW_TITLE), ApiClient.XML, ApiClient.XML);

        assertEquals(OK, response.status());
        assertEquals(NEW_TITLE, response.xml().getElementsByTagName("title").item(0).getTextContent());
        assertEquals(NEW_TITLE, project(id).get("title").asText());
    }

    @Test
    void putReplacesEveryField() {
        String id = createProject();
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.put(path(id), """
                {"title": "%s", "description": "%s", "completed": true, "active": false}
                """.formatted(NEW_TITLE, NEW_DESCRIPTION));

        assertEquals(OK, response.status());
        JsonNode replaced = project(id);
        assertEquals(NEW_TITLE, replaced.get("title").asText());
        assertEquals(NEW_DESCRIPTION, replaced.get("description").asText());
        assertEquals("true", replaced.get("completed").asText());
        assertEquals("false", replaced.get("active").asText());
        assertOnlyChangedSince(before, "projects/" + id);
    }

    @Test
    void deleteRemovesOnlyTheRequestedProject() {
        SystemState before = SystemState.capture();
        String id = createProject();

        ApiResponse response = ApiClient.delete(path(id));

        assertEquals(OK, response.status());
        assertEquals(NOT_FOUND, ApiClient.get(path(id)).status());
        assertFalse(ids(ApiClient.get(PROJECTS).json().get("projects")).contains(id));
        assertNothingChangedSince(before);
    }

    @Test
    void deleteKeepsTheTasksOfTheProject() {
        String projectId = createProject();
        String todoId = createTodo();
        link(tasksPath(projectId), todoId);

        ApiResponse response = ApiClient.delete(path(projectId));

        assertEquals(OK, response.status());
        assertFalse(ids(todo(todoId).path("tasksof")).contains(projectId));
    }
}
