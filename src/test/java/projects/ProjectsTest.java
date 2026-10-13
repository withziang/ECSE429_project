package projects;

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

class ProjectsTest extends ProjectsTestSupport {

    @Test
    void getReturnsEveryProjectAsJson() {
        String firstId = createProject();
        String secondId = createProject();

        ApiResponse response = ApiClient.get(PROJECTS);

        assertEquals(OK, response.status());
        assertEquals(Optional.of(ApiClient.JSON), response.header("Content-Type"));
        assertTrue(ids(response.json().get("projects")).contains(firstId));
        assertTrue(ids(response.json().get("projects")).contains(secondId));
    }

    @Test
    void getReturnsProjectsAsXml() {
        createProject();

        ApiResponse response = ApiClient.send("GET", PROJECTS, null, null, ApiClient.XML);

        assertEquals(OK, response.status());
        assertEquals(Optional.of(ApiClient.XML), response.header("Content-Type"));
        Document projects = response.xml();
        assertEquals("projects", projects.getDocumentElement().getTagName());
        assertTrue(projects.getDocumentElement().getTextContent().contains(TITLE));
    }

    @Test
    void getFiltersProjectsByQueryParameters() {
        String activeId = createProject();
        String inactiveId = createInactiveProject();

        ApiResponse response = ApiClient.get(PROJECTS + "?active=true");

        assertEquals(OK, response.status());
        JsonNode activeProjects = response.json().get("projects");
        assertTrue(ids(activeProjects).contains(activeId));
        assertFalse(ids(activeProjects).contains(inactiveId));
        activeProjects.forEach(project -> assertEquals("true", project.get("active").asText()));
    }

    @Test
    void headReturnsHeadersWithoutBody() {
        ApiResponse response = ApiClient.head(PROJECTS);

        assertEquals(OK, response.status());
        assertEquals(Optional.of(ApiClient.JSON), response.header("Content-Type"));
        assertTrue(response.body().isEmpty());
    }

    @Test
    void postCreatesProjectFromJson() {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(PROJECTS, """
                {"title": "%s", "description": "%s", "completed": true, "active": true}
                """.formatted(TITLE, DESCRIPTION));

        String id = idOfCreated("projects", response);
        JsonNode created = project(id);
        assertEquals(TITLE, created.get("title").asText());
        assertEquals(DESCRIPTION, created.get("description").asText());
        assertEquals("true", created.get("completed").asText());
        assertEquals("true", created.get("active").asText());
        assertOnlyChangedSince(before, "projects/" + id);
    }

    @Test
    void postCreatesProjectFromXml() {
        ApiResponse response = ApiClient.send("POST", PROJECTS, """
                <project>
                  <title>%s</title>
                  <description>%s</description>
                  <active>true</active>
                </project>
                """.formatted(TITLE, DESCRIPTION), ApiClient.XML, ApiClient.XML);

        String id = idOfCreatedFromXml("projects", response);
        assertEquals(Optional.of(ApiClient.XML), response.header("Content-Type"));
        assertEquals(TITLE, project(id).get("title").asText());
        assertEquals("true", project(id).get("active").asText());
    }

    @Test
    void postWithoutFieldsCreatesProjectWithDefaults() {
        ApiResponse response = ApiClient.post(PROJECTS, "{}");

        JsonNode created = project(idOfCreated("projects", response));
        assertEquals("", created.get("title").asText());
        assertEquals("", created.get("description").asText());
        assertEquals("false", created.get("completed").asText());
        assertEquals("false", created.get("active").asText());
    }

    private String createInactiveProject() {
        ApiResponse response = ApiClient.post(PROJECTS, """
                {"title": "Archived move", "active": false}
                """);
        return idOfCreated("projects", response);
    }
}
