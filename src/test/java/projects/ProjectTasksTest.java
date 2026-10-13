package projects;

import static common.HttpStatus.CREATED;
import static common.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import common.ApiClient;
import common.ApiResponse;
import common.JsonBodies;
import common.SystemState;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class ProjectTasksTest extends ProjectsTestSupport {

    @Test
    void getReturnsNoTasksForNewProject() {
        String projectId = createProject();

        ApiResponse response = ApiClient.get(tasksPath(projectId));

        assertEquals(OK, response.status());
        assertEquals(0, response.json().get("todos").size());
    }

    @Test
    void getReturnsTheTasksOfTheProjectAsJson() {
        String projectId = createProject();
        String todoId = createTodo();
        link(tasksPath(projectId), todoId);

        ApiResponse response = ApiClient.get(tasksPath(projectId));

        assertEquals(OK, response.status());
        assertEquals(Optional.of(ApiClient.JSON), response.header("Content-Type"));
        JsonNode tasks = response.json().get("todos");
        assertEquals(Set.of(todoId), ids(tasks));
        assertEquals(TODO_TITLE, tasks.get(0).get("title").asText());
    }

    @Test
    void getReturnsTheTasksOfTheProjectAsXml() {
        String projectId = createProject();
        String todoId = createTodo();
        link(tasksPath(projectId), todoId);

        ApiResponse response = ApiClient.send("GET", tasksPath(projectId), null, null, ApiClient.XML);

        assertEquals(OK, response.status());
        assertEquals(Optional.of(ApiClient.XML), response.header("Content-Type"));
        Document tasks = response.xml();
        assertEquals(1, tasks.getElementsByTagName("todo").getLength());
        assertEquals(TODO_TITLE, tasks.getElementsByTagName("title").item(0).getTextContent());
    }

    @Test
    void headReturnsHeadersWithoutBody() {
        String projectId = createProject();

        ApiResponse response = ApiClient.head(tasksPath(projectId));

        assertEquals(OK, response.status());
        assertEquals(Optional.of(ApiClient.JSON), response.header("Content-Type"));
        assertTrue(response.body().isEmpty());
    }

    @Test
    void postLinksAnExistingTodoInBothDirections() {
        String projectId = createProject();
        String todoId = createTodo();
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(tasksPath(projectId), JsonBodies.idReference(todoId));

        assertEquals(CREATED, response.status());
        assertEquals(Set.of(todoId), ids(project(projectId).get("tasks")));
        assertEquals(Set.of(projectId), ids(todo(todoId).get("tasksof")));
        assertEquals(TODO_TITLE, todo(todoId).get("title").asText());
        assertOnlyChangedSince(before, "projects/" + projectId, "todos/" + todoId);
    }

    @Test
    void postingTheSameTodoTwiceKeepsOneLink() {
        String projectId = createProject();
        String todoId = createTodo();
        link(tasksPath(projectId), todoId);

        ApiResponse response = ApiClient.post(tasksPath(projectId), JsonBodies.idReference(todoId));

        assertEquals(CREATED, response.status());
        assertEquals(1, ApiClient.get(tasksPath(projectId)).json().get("todos").size());
    }

    @Test
    void deleteRemovesTheLinkAndKeepsTheTodo() {
        String projectId = createProject();
        String todoId = createTodo();
        SystemState beforeLink = SystemState.capture();
        link(tasksPath(projectId), todoId);

        ApiResponse response = ApiClient.delete(tasksPath(projectId) + "/" + todoId);

        assertEquals(OK, response.status());
        assertEquals(0, ApiClient.get(tasksPath(projectId)).json().get("todos").size());
        assertEquals(TODO_TITLE, todo(todoId).get("title").asText());
        assertNothingChangedSince(beforeLink);
    }

    @Test
    void deleteLeavesTheOtherTasksLinked() {
        String projectId = createProject();
        String removedTodoId = createTodo();
        String keptTodoId = createTodo();
        link(tasksPath(projectId), removedTodoId);
        link(tasksPath(projectId), keptTodoId);

        ApiResponse response = ApiClient.delete(tasksPath(projectId) + "/" + removedTodoId);

        assertEquals(OK, response.status());
        assertEquals(Set.of(keptTodoId), ids(ApiClient.get(tasksPath(projectId)).json().get("todos")));
    }
}
