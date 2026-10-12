package categories;

import common.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ServiceRunningCheck.class)
public class CategoriesTodosTest {
    // <--------------------- Resource-specific helpers --------------------->
    private String extractId(String body) {
        Matcher matcher = Pattern
            .compile("\"id\"\\s*:\\s*\"?([^\",}\\s]+)\"?")
            .matcher(body);

        assertTrue(matcher.find(), "Could not extract ID from: " + body);
        return matcher.group(1);
    }

    private boolean containsId(String body, String id) {
        return Pattern
            .compile("\"id\"\\s*:\\s*\"?" + Pattern.quote(id) + "\"?")
            .matcher(body)
            .find();
    }

    private String createCategory() {
        String json = """
            {
                "title": "JUnit Test Category",
                "description": "Temporary test category"
            }
            """;

        System.out.println("[POST] Creating test category...");

        ApiResponse response = ApiClient.post("/categories", json);

        System.out.println("[POST] Status: " + response.status());
        System.out.println("[POST] Response: " + response.body());

        assertTrue(response.status() == 200 || response.status() == 201,
            "Category creation failed: " + response.body());

        String id = extractId(response.body());
        System.out.println("[POST] Created category ID: " + id);

        return id;
    }

    private void deleteCategory(String id) {
        System.out.println("[CLEANUP] Deleting category ID: " + id);

        ApiResponse response = ApiClient.delete("/categories/" + id);

        System.out.println("[CLEANUP] Category DELETE status: " + response.status());
    }

    public String createTodo() {
        String json = """
            {
                "title": "JUnit Test Todo",
                "doneStatus": false,
                "description": "Temporary test todo"
            }
            """;

        System.out.println("[POST] Creating test todo...");

        ApiResponse response = ApiClient.post("/todos", json);

        System.out.println("[POST] Status: " + response.status());
        System.out.println("[POST] Response: " + response.body());

        assertTrue(
            response.status() == 200 || response.status() == 201,
            "Todo creation failed: " + response.body()
        );

        String id = extractId(response.body());
        System.out.println("[POST] Created todo ID: " + id);

        return id;
    }

    private void deleteTodo(String id) {
        System.out.println("[CLEANUP] Deleting todo ID: " + id);

        ApiResponse response = ApiClient.delete("/todos/" + id);

        System.out.println("[CLEANUP] Todo DELETE status: " + response.status());
    }

    private void linkTodoToCategory(String categoryId, String todoId) {
        String json = """
            {
                "id": "%s"
            }
            """.formatted(todoId);

        System.out.println("[POST] Linking todo " + todoId
            + " to category " + categoryId);
        System.out.println("[POST] Relationship request body: " + json);

        ApiResponse response = ApiClient.post(
            "/categories/" + categoryId + "/todos",
            json
        );

        System.out.println("[POST] Relationship status: " + response.status());
        System.out.println("[POST] Relationship response: " + response.body());

        assertTrue(
            response.status() == 200 || response.status() == 201,
            "Relationship creation failed: " + response.body()
        );

        System.out.println("[PASS] Todo was linked to the category.");
    }


    // <--------------------- /categories/:id/todos tests --------------------->
    @Test
    void getCategoryTodosReturnsSuccessWhenEmpty() {
        System.out.println("\n========== TEST: GET /categories/:id/todos (empty) ==========");

        String categoryId = createCategory();

        try {
            System.out.println("[GET] Retrieving todos for category " + categoryId);

            ApiResponse response =
                ApiClient.get("/categories/" + categoryId + "/todos");

            System.out.println("[GET] Status: " + response.status());
            System.out.println("[GET] Response: " + response.body());

            assertEquals(200, response.status(), response.body());

            System.out.println("[PASS] GET category todos returned 200.");
        } finally {
            deleteCategory(categoryId);
        }
    }

    @Test
    void headCategoryTodosReturnsHeaders() {
        System.out.println("\n========== TEST: HEAD /categories/:id/todos ==========");

        String categoryId = createCategory();

        try {
            System.out.println("[HEAD] Checking todos endpoint for category " + categoryId);

            ApiResponse response =
                ApiClient.head("/categories/" + categoryId + "/todos");

            System.out.println("[HEAD] Status: " + response.status());
            System.out.println("[HEAD] Response body length: " + response.body().length());

            assertEquals(200, response.status());

            System.out.println("[PASS] HEAD category todos returned 200.");
        } finally {
            deleteCategory(categoryId);
        }
    }

    @Test
    void postCategoryTodosCreatesRelationship() {
        System.out.println("\n========== TEST: POST /categories/:id/todos ==========");

        String categoryId = createCategory();
        String todoId = createTodo();

        try {
            linkTodoToCategory(categoryId, todoId);

            System.out.println("[GET] Verifying todo " + todoId
                + " appears under category " + categoryId);

            ApiResponse response =
                ApiClient.get("/categories/" + categoryId + "/todos");

            System.out.println("[GET] Status: " + response.status());
            System.out.println("[GET] Response: " + response.body());

            assertEquals(200, response.status(), response.body());

            assertTrue(
                containsId(response.body(), todoId),
                "Todo was not found in the category's todos: "
                    + response.body()
            );

            System.out.println("[PASS] Todo " + todoId
                + " appears in category " + categoryId + "'s todos.");
        } finally {
            deleteCategory(categoryId);
            deleteTodo(todoId);
        }
    }


    // <------------------- /categories/:id/todos/:id tests ------------------->
    @Test
    void deleteCategoryTodoRelationshipRemovesLink() {
        System.out.println("\n========== TEST: DELETE /categories/:id/todos/:id ==========");

        String categoryId = createCategory();
        String todoId = createTodo();

        try {
            linkTodoToCategory(categoryId, todoId);

            System.out.println("[DELETE] Removing relationship between category "
                + categoryId + " and todo " + todoId);

            ApiResponse deleteResponse = ApiClient.delete(
                "/categories/" + categoryId + "/todos/" + todoId
            );

            System.out.println("[DELETE] Status: " + deleteResponse.status());
            System.out.println("[DELETE] Response: " + deleteResponse.body());

            assertTrue(
                deleteResponse.status() == 200
                    || deleteResponse.status() == 204,
                "Relationship deletion failed: " + deleteResponse.body()
            );

            System.out.println("[GET] Verifying the relationship no longer exists...");

            ApiResponse getResponse =
                ApiClient.get("/categories/" + categoryId + "/todos");

            System.out.println("[GET] Status: " + getResponse.status());
            System.out.println("[GET] Response: " + getResponse.body());

            assertEquals(200, getResponse.status(), getResponse.body());

            assertFalse(
                containsId(getResponse.body(), todoId),
                "Todo relationship still exists: " + getResponse.body()
            );

            System.out.println("[PASS] Todo relationship was removed successfully.");
        } finally {
            deleteCategory(categoryId);
            deleteTodo(todoId);
        }
    }
}