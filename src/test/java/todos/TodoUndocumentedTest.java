package todos;

import static org.junit.jupiter.api.Assertions.*;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Undocumented behaviour of the todos endpoints found during exploratory testing. */
@ExtendWith(ServiceRunningCheck.class)
class TodoUndocumentedTest {

    private static final String EXISTING_CATEGORY_ID = "2";
    private static final String EXPENSE_REPORT = "{\"title\":\"Submit expense report\"}";

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
    void optionsOnTodosListsTheAllowedMethods() {
        ApiResponse response = options("/todos");

        assertEquals(200, response.status());
        assertEquals(Optional.of("OPTIONS, GET, HEAD, POST"), response.header("Allow"));
    }

    @Test
    void optionsOnOneTodoListsTheAllowedMethods() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);

        ApiResponse response = options("/todos/" + todoId);

        assertEquals(200, response.status());
        assertEquals(Optional.of("OPTIONS, GET, HEAD, POST, PUT, DELETE"), response.header("Allow"));
    }

    @Test
    void optionsOnACategoryLinkAllowsOnlyDelete() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        fixture.link("/todos/" + todoId + "/categories", EXISTING_CATEGORY_ID);

        ApiResponse response = options("/todos/" + todoId + "/categories/" + EXISTING_CATEGORY_ID);

        assertEquals(200, response.status());
        assertEquals(Optional.of("OPTIONS, DELETE"), response.header("Allow"));
    }

    @Test
    void patchOnOneTodoIsNotAllowed() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.send("PATCH", "/todos/" + todoId, "{\"title\":\"patched\"}",
                ApiClient.JSON, ApiClient.JSON);

        assertEquals(405, response.status());
        assertEquals(Set.of(), before.changedObjects(SystemState.capture()));
    }

    @Test
    void linkingACategoryByTitleCreatesANewCategory() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post("/todos/" + todoId + "/categories", "{\"title\":\"Errands\"}");
        String categoryId = response.json().path("id").asText();
        fixture.recordCreated("categories", categoryId);

        assertEquals(201, response.status());
        assertEquals(Optional.of("categories/" + categoryId), response.header("Location"));
        assertEquals("Errands", ApiClient.get("/categories/" + categoryId).firstIn("categories").get("title").asText());
        assertEquals(List.of(categoryId), ApiClient.get("/todos/" + todoId + "/categories").idsIn("categories"));
        assertEquals(Set.of("todos/" + todoId, "categories/" + categoryId), before.changedObjects(SystemState.capture()));
    }

    private static ApiResponse options(String path) {
        return ApiClient.send("OPTIONS", path, null, ApiClient.JSON, ApiClient.JSON);
    }
}
