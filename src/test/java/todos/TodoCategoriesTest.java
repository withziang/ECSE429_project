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

/** /todos/:id/categories and /todos/:id/categories/:id: the todo-to-category relationship. */
@ExtendWith(ServiceRunningCheck.class)
class TodoCategoriesTest {

    private static final String MISSING_ID = "999999";
    private static final String EXISTING_CATEGORY_ID = "2";
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
    void getCategoriesReturnsTheLinkedCategories() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        fixture.link(categoriesOf(todoId), EXISTING_CATEGORY_ID);

        ApiResponse response = ApiClient.get(categoriesOf(todoId));

        assertEquals(200, response.status());
        assertEquals(List.of(EXISTING_CATEGORY_ID), response.idsIn("categories"));
    }

    @Test
    void headCategoriesReturnsHeadersWithoutBody() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);

        ApiResponse response = ApiClient.head(categoriesOf(todoId));

        assertEquals(200, response.status());
        assertTrue(response.body().isEmpty());
    }

    // The docs list todo => category and category => todo as separate relationships,
    // so linking from the todo side changes only the todo.
    @Test
    void postCategoriesLinksTheTodoToTheCategory() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(categoriesOf(todoId), JsonBodies.idReference(EXISTING_CATEGORY_ID));

        assertEquals(201, response.status());
        assertEquals(List.of(EXISTING_CATEGORY_ID), ApiClient.get(categoriesOf(todoId)).idsIn("categories"));
        assertEquals(Set.of("todos/" + todoId), before.changedObjects(SystemState.capture()));
    }

    @Test
    void postCategoriesFromMissingTodoReturnsNotFound() {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(categoriesOf(MISSING_ID), JsonBodies.idReference(EXISTING_CATEGORY_ID));

        assertEquals(404, response.status());
        assertEquals(Set.of(), before.changedObjects(SystemState.capture()));
    }

    @Test
    void deleteCategoriesRemovesTheLink() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        fixture.link(categoriesOf(todoId), EXISTING_CATEGORY_ID);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.delete(categoriesOf(todoId) + "/" + EXISTING_CATEGORY_ID);

        assertEquals(200, response.status());
        assertEquals(List.of(), ApiClient.get(categoriesOf(todoId)).idsIn("categories"));
        assertEquals(Set.of("todos/" + todoId), before.changedObjects(SystemState.capture()));
    }

    @Test
    void deletingTheSameLinkTwiceReturnsNotFound() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        fixture.link(categoriesOf(todoId), EXISTING_CATEGORY_ID);
        ApiClient.delete(categoriesOf(todoId) + "/" + EXISTING_CATEGORY_ID);

        ApiResponse secondDelete = ApiClient.delete(categoriesOf(todoId) + "/" + EXISTING_CATEGORY_ID);

        assertEquals(404, secondDelete.status());
    }

    @Test
    @DisplayName("[Expected, fails] categories of a missing todo return 404")
    void categoriesOfMissingTodoShouldReturnNotFound() {
        ApiResponse response = ApiClient.get(categoriesOf(MISSING_ID));

        assertEquals(404, response.status());
    }

    @Test
    @DisplayName("[Actual] categories of a missing todo return 200 with other todos' categories")
    void categoriesOfMissingTodoReturnsOtherTodosCategories() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        fixture.link(categoriesOf(todoId), EXISTING_CATEGORY_ID);

        ApiResponse response = ApiClient.get(categoriesOf(MISSING_ID));

        assertEquals(200, response.status());
        assertTrue(response.idsIn("categories").contains(EXISTING_CATEGORY_ID));
    }

    @Test
    @DisplayName("[Expected, fails] HEAD on the categories of a missing todo returns 404")
    void headCategoriesOfMissingTodoShouldReturnNotFound() {
        ApiResponse response = ApiClient.head(categoriesOf(MISSING_ID));

        assertEquals(404, response.status());
    }

    @Test
    @DisplayName("[Actual] HEAD on the categories of a missing todo returns 200")
    void headCategoriesOfMissingTodoReturnsOk() {
        ApiResponse response = ApiClient.head(categoriesOf(MISSING_ID));

        assertEquals(200, response.status());
    }

    @Test
    @DisplayName("[Expected, fails] linking with an XML body links the category")
    void linkWithXmlBodyShouldLinkTheCategory() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);

        ApiResponse response = ApiClient.send("POST", categoriesOf(todoId), xmlReference(EXISTING_CATEGORY_ID),
                ApiClient.XML, ApiClient.JSON);

        assertEquals(201, response.status());
        assertEquals(List.of(EXISTING_CATEGORY_ID), ApiClient.get(categoriesOf(todoId)).idsIn("categories"));
    }

    @Test
    @DisplayName("[Actual] linking with an XML body returns 404 and links nothing")
    void linkWithXmlBodyReturnsNotFound() {
        String todoId = fixture.create("todos", EXPENSE_REPORT);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.send("POST", categoriesOf(todoId), xmlReference(EXISTING_CATEGORY_ID),
                ApiClient.XML, ApiClient.JSON);

        assertEquals(404, response.status());
        assertTrue(response.body().contains("Could not find thing matching value for id"), response.body());
        assertEquals(Set.of(), before.changedObjects(SystemState.capture()));
    }

    private static String categoriesOf(String todoId) {
        return "/todos/" + todoId + "/categories";
    }

    private static String xmlReference(String categoryId) {
        return "<category><id>" + categoryId + "</id></category>";
    }
}
