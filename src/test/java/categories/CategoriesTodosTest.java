package categories;

import common.ApiClient;
import common.ApiResponse;
import common.JsonBodies;
import common.ServiceRunningCheck;
import common.SystemState;
import common.TestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** /categories/:id/todos and /categories/:id/todos/:id. */
@ExtendWith(ServiceRunningCheck.class)
class CategoriesTodosTest {

    private static final String TEST_CATEGORY = """
            {
                "title": "JUnit Test Category",
                "description": "Temporary test category"
            }
            """;
    private static final String TEST_TODO = """
            {
                "title": "JUnit Test Todo",
                "doneStatus": false,
                "description": "Temporary test todo"
            }
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

    // <--------------------- /categories/:id/todos tests --------------------->
    @Test
    void getCategoryTodosReturnsSuccessWhenEmpty() {
        String categoryId = fixture.create("categories", TEST_CATEGORY);

        ApiResponse response = ApiClient.get(todosOf(categoryId));

        assertEquals(200, response.status(), response.body());
        assertEquals(List.of(), response.idsIn("todos"));
    }

    @Test
    void headCategoryTodosReturnsHeaders() {
        String categoryId = fixture.create("categories", TEST_CATEGORY);

        ApiResponse response = ApiClient.head(todosOf(categoryId));

        assertEquals(200, response.status());
        assertTrue(response.body().isEmpty(), "HEAD response should not contain a body");
    }

    @Test
    void postCategoryTodosCreatesRelationship() {
        String categoryId = fixture.create("categories", TEST_CATEGORY);
        String todoId = fixture.create("todos", TEST_TODO);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(todosOf(categoryId), JsonBodies.idReference(todoId));

        assertEquals(201, response.status(), response.body());
        assertEquals(List.of(todoId), ApiClient.get(todosOf(categoryId)).idsIn("todos"));
        assertEquals(Set.of("categories/" + categoryId), before.changedObjects(SystemState.capture()));
    }

    // The docs list category => todo and todo => category as separate relationships.
    @Test
    void linkFromCategorySideIsNotListedOnTheTodo() {
        String categoryId = fixture.create("categories", TEST_CATEGORY);
        String todoId = fixture.create("todos", TEST_TODO);

        fixture.link(todosOf(categoryId), todoId);

        assertEquals(List.of(), ApiClient.get("/todos/" + todoId + "/categories").idsIn("categories"));
    }

    // <------------------- /categories/:id/todos/:id tests ------------------->
    @Test
    void deleteCategoryTodoRelationshipRemovesLink() {
        String categoryId = fixture.create("categories", TEST_CATEGORY);
        String todoId = fixture.create("todos", TEST_TODO);
        fixture.link(todosOf(categoryId), todoId);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.delete(todosOf(categoryId) + "/" + todoId);

        assertEquals(200, response.status(), response.body());
        assertEquals(List.of(), ApiClient.get(todosOf(categoryId)).idsIn("todos"));
        assertEquals(Set.of("categories/" + categoryId), before.changedObjects(SystemState.capture()));
    }

    @Test
    void deleteCategoryTodoRelationshipTwiceReturnsNotFound() {
        String categoryId = fixture.create("categories", TEST_CATEGORY);
        String todoId = fixture.create("todos", TEST_TODO);
        fixture.link(todosOf(categoryId), todoId);
        ApiClient.delete(todosOf(categoryId) + "/" + todoId);

        ApiResponse secondDelete = ApiClient.delete(todosOf(categoryId) + "/" + todoId);

        assertEquals(404, secondDelete.status());
    }

    private static String todosOf(String categoryId) {
        return "/categories/" + categoryId + "/todos";
    }
}
