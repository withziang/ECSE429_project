package categories;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class CategoriesTest {
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


    // <--------------------- /categories tests --------------------->
    @Test
    void getCategoryReturnsSuccess() throws Exception {
        HttpResponse<String> response = send("GET", "/categories", null);

        assertEquals(200, response.statusCode());
        assertNotNull(response.body());
    }

    @Test
    void getCategoryReturnsAllExistingCategories() throws Exception {
        String createdId = null;

        try {
            HttpResponse<String> initial =
                send("GET", "/categories", null);

            assertEquals(200, initial.statusCode());

            createdId = createCategory();

            HttpResponse<String> result =
                send("GET", "/categories", null);

            assertEquals(200, result.statusCode());
            assertTrue(containsId(result.body(), createdId),
                "New category should appear in GET /categories");

            Matcher oldIds = Pattern.compile(
                "\"id\"\\s*:\\s*\"([^\"]+)\""
            ).matcher(initial.body());

            while (oldIds.find()) {
                String oldId = oldIds.group(1);

                assertTrue(containsId(result.body(), oldId),
                    "Existing category ID " + oldId
                        + " should remain present");
            }
        } finally {
            if (createdId != null) {
                deleteCategory(createdId);
            }
        }
    }

    @Test
    void postCategoryCreatesCategory() throws Exception {
        String title = "JUnit POST Test";
        String description = "Temporary test category";
        String createdId = null;

        try {
            String json = """
                    {
                        "title": "JUnit POST Test",
                        "description": "Temporary test category"
                    }
                    """;

            HttpResponse<String> response = send("POST", "/categories", json);

            assertTrue(response.statusCode() == 200
                    || response.statusCode() == 201,
                "POST should create a category: " + response.body());

            assertTrue(response.body().contains(title),
                "Response should contain the category title");
            assertTrue(response.body().contains(description),
                "Response should contain the category description");

            createdId = extractId(response.body());
        } finally {
            if (createdId != null) {
                deleteCategory(createdId);
            }
        }
    }

    @Test
    void headCategoryReturnsHeaders() throws Exception {
        HttpResponse<String> response = send("HEAD", "/categories", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().isEmpty(),
            "HEAD response should not contain a body");
    }


    // <--------------------- /categories/:id tests --------------------->
    @Test
    void getCategoryByIdReturnsCorrectCategory() throws Exception {
        String createdId = createCategory();

        try {
            HttpResponse<String> response = send("GET", "/categories/" + createdId, null);

            assertEquals(200, response.statusCode());
            assertTrue(containsId(response.body(), createdId),
                "Response should contain the category ID");
            assertTrue(response.body().contains("JUnit Test Category"),
                "Response should contain the expected category title");
        } finally {
            deleteCategory(createdId);
        }
    }

    @Test
    void headCategoryByIdReturnsSuccess() throws Exception {
        String createdId = createCategory();

        try {
            HttpResponse<String> response = send("HEAD", "/categories/" + createdId, null);

            assertEquals(200, response.statusCode());
            assertTrue(response.body().isEmpty(),
                "HEAD response should not contain a body");
        } finally {
            deleteCategory(createdId);
        }
    }

    @Test
    void putCategoryUpdatesCategory() throws Exception {
        String createdId = createCategory();

        try {
            String json = """
                    {
                        "title": "Updated by PUT",
                        "description": "Updated description"
                    }
                    """;

            HttpResponse<String> response = send("PUT", "/categories/" + createdId, json);

            assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                "PUT update should succeed: " + response.body());

            HttpResponse<String> check = send("GET", "/categories/" + createdId, null);

            assertEquals(200, check.statusCode());
            assertTrue(check.body().contains("Updated by PUT"));
            assertTrue(check.body().contains("Updated description"));
        } finally {
            deleteCategory(createdId);
        }
    }

    @Test
    void deleteCategoryByIdReturnsSuccess() throws Exception {
        String createdId = createCategory();

        HttpResponse<String> deleteResponse = send("DELETE", "/categories/" + createdId, null);

        assertTrue(deleteResponse.statusCode() >= 200 && deleteResponse.statusCode() < 300,
            "DELETE should succeed: " + deleteResponse.body());

        HttpResponse<String> getResponse = send("GET", "/categories/" + createdId, null);

        assertEquals(404, getResponse.statusCode(),
            "GET after DELETE should return 404");
    }
}
