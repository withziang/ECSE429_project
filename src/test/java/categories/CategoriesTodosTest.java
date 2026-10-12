package categories;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class CategoriesTodosTest {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final String BASE_URL = "http://localhost:4567";

    // <--------------------- Shared helpers --------------------->
    private HttpResponse<String> send(String method, String path, String json) throws Exception {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + path))
            .header("Accept", "application/json");

        if (json != null) {
            builder.header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(json));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return CLIENT.send(
            builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String extractId(String body) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"?([^\",}\\s]+)\"?").matcher(body);

        assertTrue(matcher.find(), "Could not extract ID from: " + body);
        return matcher.group(1);
    }

    private boolean containsId(String body, String id) {
        return Pattern.compile("\"id\"\\s*:\\s*\"?" + Pattern.quote(id) + "\"?").matcher(body).find();
    }

    private String createCategory() throws Exception {
        String json = """
                {
                    "title": "JUnit Test Category",
                    "description": "Temporary test category"
                }
                """;

        HttpResponse<String> response = send("POST", "/categories", json);

        assertTrue(response.statusCode() == 200 || response.statusCode() == 201,
            "Category creation failed: " + response.body());

        return extractId(response.body());
    }

    private void deleteCategory(String id) throws Exception {
        send("DELETE", "/categories/" + id, null);
    }

    public String createTodo() throws  Exception {
        String json = """
            {
                "title": "JUnit Test Todo",
                "doneStatus": false,
                "description": "Temporary test todo"
            }
            """;

        HttpResponse<String> response = send("POST", "/todos", json);

        assertTrue(
            response.statusCode() == 200 || response.statusCode() == 201,
            "Todo creation failed: " + response.body()
        );

        return extractId(response.body());
    }

    private void deleteTodo(String id) throws Exception {
        send("DELETE", "/todos/" + id, null);
    }

    private void linkTodoToCategory(String categoryId, String todoId) throws Exception {
        String json = """
            {
                "id": "%s"
            }
            """.formatted(todoId);

        HttpResponse<String> response = send(
            "POST",
            "/categories/" + categoryId + "/todos",
            json
        );

        assertTrue(
            response.statusCode() == 200 || response.statusCode() == 201,
            "Relationship creation failed: " + response.body()
        );
    }


    // <--------------------- /categories/:id/todos tests --------------------->
    @Test
    void getCategoryTodosReturnsSuccessWhenEmpty() throws Exception {
        String categoryId = createCategory();

        try {
            HttpResponse<String> response = send(
                "GET",
                "/categories/" + categoryId + "/todos",
                null
            );

            assertEquals(200, response.statusCode(), response.body());
        } finally {
            deleteCategory(categoryId);
        }
    }

    @Test
    void headCategoryTodosReturnsHeaders() throws Exception {
        String categoryId = createCategory();

        try {
            HttpResponse<String> response = send(
                "HEAD",
                "/todos",
                null
            );

            assertEquals(200, response.statusCode());
        } finally {
            deleteCategory(categoryId);
        }
    }

    @Test
    void postCategoryTodosCreatesRelationship() throws Exception {
        String categoryId = createCategory();
        String todoId = createTodo();

        try {
            linkTodoToCategory(categoryId, todoId);

            HttpResponse<String> response = send(
                "GET",
                "/categories/" + categoryId + "/todos",
                null
            );

            assertEquals(200, response.statusCode(), response.body());
            assertTrue(
                containsId(response.body(), todoId),
                "Todo was not found in the category's todos: "
                    + response.body()
            );
        } finally {
            deleteCategory(categoryId);
            deleteTodo(todoId);
        }
    }


    // <------------------- /categories/:id/todos/:id tests ------------------->
    @Test
    void deleteCategoryTodoRelationshipRemovesLink() throws Exception {
        String categoryId = createCategory();
        String todoId = createTodo();

        try {
            linkTodoToCategory(categoryId, todoId);

            HttpResponse<String> deleteResponse = send(
                "DELETE",
                "/categories/" + categoryId + "/todos/" + todoId,
                null
            );

            assertTrue(
                deleteResponse.statusCode() == 200 || deleteResponse.statusCode() == 204,
                "Relationship deletion failed: " + deleteResponse.body()
            );

            HttpResponse<String> getResponse = send(
                "GET",
                "/categories/" + categoryId + "/todos",
                null
            );

            assertEquals(200, getResponse.statusCode(), getResponse.body());
            assertFalse(
                containsId(getResponse.body(), todoId),
                "Todo relationship still exists: " + getResponse.body()
            );
        } finally {
            deleteCategory(categoryId);
            deleteTodo(todoId);
        }
    }
}
