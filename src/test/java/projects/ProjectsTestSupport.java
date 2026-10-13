package projects;

import static common.HttpStatus.CREATED;
import static common.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import common.ApiClient;
import common.ApiResponse;
import common.ServiceRunningCheck;
import common.SystemState;
import common.TestFixture;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ServiceRunningCheck.class)
abstract class ProjectsTestSupport {

    static final String PROJECTS = "/projects";
    static final String MISSING_ID = "0";
    static final String TITLE = "Kitchen renovation";
    static final String DESCRIPTION = "Replace cabinets and countertop";
    static final String TODO_TITLE = "Order cabinets";
    static final String CATEGORY_TITLE = "Renovation";

    static final String DOCUMENTED_JSON_EXAMPLE = """
            {
              "title": "a qui officia deseru",
              "completed": "false",
              "active": "true",
              "description": "t in voluptate velit"
            }
            """;
    static final String DOCUMENTED_XML_EXAMPLE = """
            <project>
              <active>true</active>
              <description>t in voluptate velit</description>
              <id>null</id>
              <completed>false</completed>
              <title>a qui officia deseru</title>
            </project>
            """;

    private final TestFixture fixture = new TestFixture();

    @BeforeEach
    void saveState() {
        fixture.saveState();
    }

    @AfterEach
    void restoreState() {
        fixture.restoreState();
    }

    String createProject() {
        return fixture.create("projects", """
                {"title": "%s", "description": "%s", "completed": false, "active": true}
                """.formatted(TITLE, DESCRIPTION));
    }

    String createTodo() {
        return fixture.create("todos", """
                {"title": "%s"}
                """.formatted(TODO_TITLE));
    }

    String createCategory() {
        return fixture.create("categories", """
                {"title": "%s"}
                """.formatted(CATEGORY_TITLE));
    }

    void link(String relationshipPath, String id) {
        fixture.link(relationshipPath, id);
    }

    String idOfCreated(String resource, ApiResponse response) {
        assertEquals(CREATED, response.status(), response.body());
        String id = response.json().get("id").asText();
        fixture.recordCreated(resource, id);
        return id;
    }

    String idOfCreatedFromXml(String resource, ApiResponse response) {
        assertEquals(CREATED, response.status(), response.body());
        String id = response.xml().getElementsByTagName("id").item(0).getTextContent();
        fixture.recordCreated(resource, id);
        return id;
    }

    static JsonNode project(String id) {
        return find("projects", id);
    }

    static JsonNode todo(String id) {
        return find("todos", id);
    }

    static JsonNode category(String id) {
        return find("categories", id);
    }

    static String path(String id) {
        return PROJECTS + "/" + id;
    }

    static String tasksPath(String projectId) {
        return path(projectId) + "/tasks";
    }

    static String categoriesPath(String projectId) {
        return path(projectId) + "/categories";
    }

    static String xmlTodoReference(String id) {
        return "<todo><id>%s</id></todo>".formatted(id);
    }

    static Set<String> ids(JsonNode entities) {
        Set<String> ids = new TreeSet<>();
        entities.forEach(entity -> ids.add(entity.get("id").asText()));
        return ids;
    }

    static String firstErrorMessage(ApiResponse response) {
        return response.json().get("errorMessages").get(0).asText();
    }

    static void assertNothingChangedSince(SystemState before) {
        assertOnlyChangedSince(before);
    }

    static void assertOnlyChangedSince(SystemState before, String... changedObjects) {
        assertEquals(Set.of(changedObjects), before.changedObjects(SystemState.capture()));
    }

    private static JsonNode find(String resource, String id) {
        ApiResponse response = ApiClient.get("/" + resource + "/" + id);
        assertEquals(OK, response.status(), response.body());
        return response.firstIn(resource);
    }
}
