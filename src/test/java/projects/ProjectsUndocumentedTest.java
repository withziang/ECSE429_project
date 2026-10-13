package projects;

import static common.HttpStatus.METHOD_NOT_ALLOWED;
import static common.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import common.ApiClient;
import common.ApiResponse;
import common.SystemState;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProjectsUndocumentedTest extends ProjectsTestSupport {

    @Test
    void optionsListsTheMethodsOfTheCollection() {
        ApiResponse response = ApiClient.send("OPTIONS", PROJECTS, null, null, null);

        assertEquals(OK, response.status());
        assertEquals(Optional.of("OPTIONS, GET, HEAD, POST"), response.header("Allow"));
    }

    @Test
    void optionsListsTheMethodsOfAProject() {
        String id = createProject();

        ApiResponse response = ApiClient.send("OPTIONS", path(id), null, null, null);

        assertEquals(OK, response.status());
        assertEquals(Optional.of("OPTIONS, GET, HEAD, POST, PUT, DELETE"), response.header("Allow"));
    }

    @Test
    void collectionRejectsPutAndDelete() {
        SystemState before = SystemState.capture();

        assertEquals(METHOD_NOT_ALLOWED, ApiClient.put(PROJECTS, "{}").status());
        assertEquals(METHOD_NOT_ALLOWED, ApiClient.delete(PROJECTS).status());
        assertNothingChangedSince(before);
    }

    @Test
    void projectRejectsPatch() {
        String id = createProject();
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.send("PATCH", path(id), "{}", ApiClient.JSON, ApiClient.JSON);

        assertEquals(METHOD_NOT_ALLOWED, response.status());
        assertNothingChangedSince(before);
    }

    @Test
    void postingATodoWithoutIdCreatesItAsATaskOfTheProject() {
        String projectId = createProject();

        ApiResponse response = ApiClient.post(tasksPath(projectId), """
                {"title": "%s"}
                """.formatted(TODO_TITLE));

        String todoId = idOfCreated("todos", response);
        assertEquals(TODO_TITLE, todo(todoId).get("title").asText());
        assertEquals(Set.of(todoId), ids(project(projectId).get("tasks")));
    }

    @Test
    void postingACategoryWithoutIdCreatesItUnderTheProject() {
        String projectId = createProject();

        ApiResponse response = ApiClient.post(categoriesPath(projectId), """
                {"title": "%s"}
                """.formatted(CATEGORY_TITLE));

        String categoryId = idOfCreated("categories", response);
        assertEquals(CATEGORY_TITLE, category(categoryId).get("title").asText());
        assertEquals(Set.of(categoryId), ids(project(projectId).get("categories")));
    }

    @Test
    void postingAProjectWithTasksLinksThem() {
        String todoId = createTodo();

        ApiResponse response = ApiClient.post(PROJECTS, """
                {"title": "Kitchen renovation", "tasks": [{"id": "%s"}]}
                """.formatted(todoId));

        String projectId = idOfCreated("projects", response);
        assertEquals(Set.of(todoId), ids(project(projectId).get("tasks")));
        assertEquals(Set.of(projectId), ids(todo(todoId).get("tasksof")));
    }
}
