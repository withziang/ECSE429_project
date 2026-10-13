package todos;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import common.ApiClient;
import common.ApiResponse;
import common.ServiceRunningCheck;
import common.SystemState;
import common.TestFixture;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** /todos/:id: read, HEAD, amend with POST and PUT, delete. */
@ExtendWith(ServiceRunningCheck.class)
class TodoByIdTest {

    private static final String MISSING_ID = "999999";
    private static final String EXISTING_PROJECT_ID = "1";
    private static final String EXISTING_CATEGORY_ID = "2";
    private static final String DONE_GROCERIES =
            "{\"title\":\"Buy groceries\",\"doneStatus\":true,\"description\":\"milk, eggs, bread\"}";
    private static final String TITLE_ONLY = "{\"title\":\"Buy groceries today\"}";

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
    void getTodoReturnsThatTodo() {
        String id = fixture.create("todos", DONE_GROCERIES);

        ApiResponse response = ApiClient.get("/todos/" + id);

        assertEquals(200, response.status());
        JsonNode todo = response.firstIn("todos");
        assertEquals(id, todo.get("id").asText());
        assertEquals("Buy groceries", todo.get("title").asText());
        assertEquals("milk, eggs, bread", todo.get("description").asText());
        assertEquals("true", todo.get("doneStatus").asText());
    }

    @Test
    void getTodoAsXmlReturnsTodoElement() {
        String id = fixture.create("todos", DONE_GROCERIES);

        ApiResponse response = ApiClient.send("GET", "/todos/" + id, null, ApiClient.JSON, ApiClient.XML);

        assertEquals(200, response.status());
        assertEquals(Optional.of(ApiClient.XML), response.header("Content-Type"));
        assertEquals("Buy groceries", response.xml().getElementsByTagName("title").item(0).getTextContent());
    }

    @Test
    void getMissingTodoReturnsNotFound() {
        ApiResponse response = ApiClient.get("/todos/" + MISSING_ID);

        assertEquals(404, response.status());
        assertTrue(response.body().contains("Could not find an instance with todos/" + MISSING_ID));
    }

    @Test
    void headTodoReturnsHeadersWithoutBody() {
        String id = fixture.create("todos", DONE_GROCERIES);

        ApiResponse response = ApiClient.head("/todos/" + id);

        assertEquals(200, response.status());
        assertTrue(response.body().isEmpty());
    }

    @Test
    void postAmendsOnlyTheFieldsSent() {
        String id = fixture.create("todos", DONE_GROCERIES);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post("/todos/" + id, "{\"description\":\"milk, eggs, bread, coffee\"}");

        assertEquals(200, response.status());
        JsonNode todo = ApiClient.get("/todos/" + id).firstIn("todos");
        assertEquals("Buy groceries", todo.get("title").asText());
        assertEquals("milk, eggs, bread, coffee", todo.get("description").asText());
        assertEquals("true", todo.get("doneStatus").asText());
        assertEquals(Set.of("todos/" + id), before.changedObjects(SystemState.capture()));
    }

    @Test
    void postCanMarkTodoAsNotDone() {
        String id = fixture.create("todos", DONE_GROCERIES);

        ApiResponse response = ApiClient.post("/todos/" + id, "{\"doneStatus\":false}");

        assertEquals(200, response.status());
        assertEquals("false", response.json().get("doneStatus").asText());
        assertEquals("false", ApiClient.get("/todos/" + id).firstIn("todos").get("doneStatus").asText());
    }

    @Test
    void postToMissingTodoReturnsNotFound() {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post("/todos/" + MISSING_ID, "{\"title\":\"ghost\"}");

        assertEquals(404, response.status());
        assertEquals(Set.of(), before.changedObjects(SystemState.capture()));
    }

    @Test
    @DisplayName("[Expected, fails] PUT with only a title keeps the other fields")
    void putWithTitleOnlyShouldKeepOtherFields() {
        String id = fixture.create("todos", DONE_GROCERIES);

        ApiResponse response = ApiClient.put("/todos/" + id, TITLE_ONLY);

        assertEquals(200, response.status());
        JsonNode todo = ApiClient.get("/todos/" + id).firstIn("todos");
        assertEquals("Buy groceries today", todo.get("title").asText());
        assertEquals("milk, eggs, bread", todo.get("description").asText());
        assertEquals("true", todo.get("doneStatus").asText());
    }

    @Test
    @DisplayName("[Actual] PUT with only a title resets the other fields")
    void putWithTitleOnlyResetsOtherFields() {
        String id = fixture.create("todos", DONE_GROCERIES);

        ApiResponse response = ApiClient.put("/todos/" + id, TITLE_ONLY);

        assertEquals(200, response.status());
        JsonNode todo = ApiClient.get("/todos/" + id).firstIn("todos");
        assertEquals("Buy groceries today", todo.get("title").asText());
        assertEquals("", todo.get("description").asText());
        assertEquals("false", todo.get("doneStatus").asText());
    }

    @Test
    @DisplayName("[Expected, fails] PUT with only a title keeps the todo's links")
    void putWithTitleOnlyShouldKeepLinks() {
        String id = linkedTodo();

        ApiClient.put("/todos/" + id, TITLE_ONLY);

        assertEquals(List.of(EXISTING_CATEGORY_ID), ApiClient.get("/todos/" + id + "/categories").idsIn("categories"));
        assertEquals(List.of(EXISTING_PROJECT_ID), ApiClient.get("/todos/" + id + "/tasksof").idsIn("projects"));
    }

    @Test
    @DisplayName("[Actual] PUT with only a title removes the todo's links")
    void putWithTitleOnlyRemovesLinks() {
        String id = linkedTodo();

        ApiResponse response = ApiClient.put("/todos/" + id, TITLE_ONLY);

        assertEquals(200, response.status());
        assertEquals(List.of(), ApiClient.get("/todos/" + id + "/categories").idsIn("categories"));
        assertEquals(List.of(), ApiClient.get("/todos/" + id + "/tasksof").idsIn("projects"));
    }

    @Test
    void deleteRemovesTheTodo() {
        String id = fixture.create("todos", DONE_GROCERIES);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.delete("/todos/" + id);

        assertEquals(200, response.status());
        assertEquals(404, ApiClient.get("/todos/" + id).status());
        assertEquals(Set.of("todos/" + id), before.changedObjects(SystemState.capture()));
    }

    @Test
    void deletingTheSameTodoTwiceReturnsNotFound() {
        String id = fixture.create("todos", DONE_GROCERIES);
        ApiClient.delete("/todos/" + id);

        ApiResponse secondDelete = ApiClient.delete("/todos/" + id);

        assertEquals(404, secondDelete.status());
        assertTrue(secondDelete.body().contains("Could not find any instances with todos/" + id));
    }

    private String linkedTodo() {
        String id = fixture.create("todos", DONE_GROCERIES);
        fixture.link("/todos/" + id + "/categories", EXISTING_CATEGORY_ID);
        fixture.link("/todos/" + id + "/tasksof", EXISTING_PROJECT_ID);
        return id;
    }
}
