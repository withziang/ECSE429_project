package projects;

import static common.HttpStatus.CREATED;
import static common.HttpStatus.NOT_FOUND;
import static common.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import common.ApiClient;
import common.ApiResponse;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectsDocumentedBehaviorTest extends ProjectsTestSupport {

    private static final String TITLE_ONLY = """
            {"title": "Bathroom renovation"}
            """;

    @Test
    @DisplayName("[Expected, fails] PUT with only a title keeps the other fields")
    void putAmendsOnlyTheSuppliedFields() {
        String id = createProject();

        ApiResponse response = ApiClient.put(path(id), TITLE_ONLY);

        assertEquals(OK, response.status());
        assertEquals(DESCRIPTION, project(id).get("description").asText());
        assertEquals("true", project(id).get("active").asText());
    }

    @Test
    @DisplayName("[Expected, fails] PUT with only a title keeps the tasks of the project")
    void putKeepsTheTasksOfTheProject() {
        String projectId = createProject();
        String todoId = createTodo();
        link(tasksPath(projectId), todoId);

        ApiResponse response = ApiClient.put(path(projectId), TITLE_ONLY);

        assertEquals(OK, response.status());
        assertEquals(Set.of(todoId), ids(project(projectId).path("tasks")));
    }

    @Test
    @DisplayName("[Expected, fails] tasks of a missing project return 404")
    void getTasksOfMissingProjectReturnsNotFound() {
        assertEquals(NOT_FOUND, ApiClient.get(tasksPath(MISSING_ID)).status());
    }

    @Test
    @DisplayName("[Expected, fails] categories of a missing project return 404")
    void getCategoriesOfMissingProjectReturnsNotFound() {
        assertEquals(NOT_FOUND, ApiClient.get(categoriesPath(MISSING_ID)).status());
    }

    @Test
    @DisplayName("[Expected, fails] deleting a task of a missing project returns 404")
    void deleteTaskOfMissingProjectReturnsNotFound() {
        String todoId = createTodo();
        link(tasksPath(createProject()), todoId);

        ApiResponse response = ApiClient.delete(tasksPath(MISSING_ID) + "/" + todoId);

        assertEquals(NOT_FOUND, response.status());
    }

    @Test
    @DisplayName("[Expected, fails] linking with an XML body links the todo")
    void postLinksAnExistingTodoFromAnXmlBody() {
        String projectId = createProject();
        String todoId = createTodo();

        ApiResponse response = ApiClient.send("POST", tasksPath(projectId), xmlTodoReference(todoId),
                ApiClient.XML, ApiClient.JSON);

        assertEquals(CREATED, response.status());
        assertEquals(Set.of(todoId), ids(project(projectId).path("tasks")));
    }

    @Test
    @DisplayName("[Expected, fails] the documented JSON example creates a project")
    void postAcceptsTheDocumentedJsonExample() {
        ApiResponse response = ApiClient.post(PROJECTS, DOCUMENTED_JSON_EXAMPLE);

        idOfCreated("projects", response);
    }

    @Test
    @DisplayName("[Expected, fails] the documented XML example creates a project")
    void postAcceptsTheDocumentedXmlExample() {
        ApiResponse response = ApiClient.send("POST", PROJECTS, DOCUMENTED_XML_EXAMPLE, ApiClient.XML, ApiClient.JSON);

        idOfCreated("projects", response);
    }
}
