package todos;

import static org.junit.jupiter.api.Assertions.*;

import common.ApiClient;
import common.ApiResponse;
import common.JsonBodies;
import common.ServiceRunningCheck;
import common.SystemState;
import common.TestFixture;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** /todos/:id/tasksof and /todos/:id/tasksof/:id: the todo-to-project relationship. */
@ExtendWith(ServiceRunningCheck.class)
class TodoTasksofTest {

    private static final String MISSING_ID = "999999";
    private static final String EXISTING_PROJECT_ID = "1";
    private static final String EXPENSE_REPORT =
            "{\"title\":\"Submit expense report\",\"doneStatus\":false,\"description\":\"September receipts\"}";

    private final TestFixture fixture = new TestFixture();

    @BeforeEach
    void saveState() {
        fixture.saveState();
    }

    @AfterEach
    void restoreState() {
        fixture.restoreState();
    }

    @Test
    void getTasksofReturnsTheLinkedProjects() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        fixture.link(tasksofOf(todoId), EXISTING_PROJECT_ID);

        ApiResponse response = ApiClient.get(tasksofOf(todoId));

        assertEquals(200, response.status());
        assertEquals(List.of(EXISTING_PROJECT_ID), response.idsIn("projects"));
    }

    @Test
    void headTasksofReturnsHeadersWithoutBody() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);

        ApiResponse response = ApiClient.head(tasksofOf(todoId));

        assertEquals(200, response.status());
        assertTrue(response.body().isEmpty());
    }

    @Test
    void postTasksofLinksTheTodoAndTheProjectBothWays() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(tasksofOf(todoId), JsonBodies.idReference(EXISTING_PROJECT_ID));

        assertEquals(201, response.status());
        assertEquals(List.of(EXISTING_PROJECT_ID), ApiClient.get(tasksofOf(todoId)).idsIn("projects"));
        assertTrue(ApiClient.get("/projects/" + EXISTING_PROJECT_ID + "/tasks").idsIn("todos").contains(todoId));
        assertEquals(Set.of("todos/" + todoId, "projects/" + EXISTING_PROJECT_ID),
                before.changedObjects(SystemState.capture()));
    }

    @Test
    void postTasksofToMissingProjectReturnsNotFound() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(tasksofOf(todoId), JsonBodies.idReference(MISSING_ID));

        assertEquals(404, response.status());
        assertEquals(Set.of(), before.changedObjects(SystemState.capture()));
    }

    @Test
    void deleteTasksofRemovesTheLinkFromBothSides() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        fixture.link(tasksofOf(todoId), EXISTING_PROJECT_ID);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.delete(tasksofOf(todoId) + "/" + EXISTING_PROJECT_ID);

        assertEquals(200, response.status());
        assertEquals(List.of(), ApiClient.get(tasksofOf(todoId)).idsIn("projects"));
        assertFalse(ApiClient.get("/projects/" + EXISTING_PROJECT_ID + "/tasks").idsIn("todos").contains(todoId));
        assertEquals(Set.of("todos/" + todoId, "projects/" + EXISTING_PROJECT_ID),
                before.changedObjects(SystemState.capture()));
    }

    @Test
    @DisplayName("[Expected, fails] projects of a missing todo return 404")
    void tasksofOfMissingTodoShouldReturnNotFound() {
        ApiResponse response = ApiClient.get(tasksofOf(MISSING_ID));

        assertEquals(404, response.status());
    }

    @Test
    @DisplayName("[Actual] projects of a missing todo return 200 with other todos' projects")
    void tasksofOfMissingTodoReturnsOtherTodosProjects() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        fixture.link(tasksofOf(todoId), EXISTING_PROJECT_ID);

        ApiResponse response = ApiClient.get(tasksofOf(MISSING_ID));

        assertEquals(200, response.status());
        assertTrue(response.idsIn("projects").contains(EXISTING_PROJECT_ID));
    }

    private static String tasksofOf(String todoId) {
        return "/todos/" + todoId + "/tasksof";
    }
}
