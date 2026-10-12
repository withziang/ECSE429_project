package categories;

import common.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ServiceRunningCheck.class)
public class CategoriesTest {
    // <--------------------- Shared helpers --------------------->
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

        System.out.println("[POST] Creating a test category...");

        ApiResponse response = ApiClient.post("/categories", json);

        System.out.println("[POST] Status: " + response.status());
        System.out.println("[POST] Response: " + response.body());

        assertTrue(response.status() == 200 || response.status() == 201,
            "Category creation failed: " + response.body());

        String id = extractId(response.body());
        System.out.println("[POST] Created category with ID: " + id);

        return id;
    }

    private void deleteCategory(String id) {
        System.out.println("[DELETE] Cleaning up category ID: " + id);

        ApiResponse response = ApiClient.delete("/categories/" + id);

        System.out.println("[DELETE] Status: " + response.status());
    }


    // <--------------------- /categories tests --------------------->
    @Test
    void getCategoryReturnsSuccess() {
        System.out.println("\n========== TEST: GET /categories ==========");

        ApiResponse response = ApiClient.get("/categories");

        System.out.println("[GET] Status: " + response.status());
        System.out.println("[GET] Response: " + response.body());

        assertEquals(200, response.status());
        assertNotNull(response.body());

        System.out.println("[PASS] Category collection was retrieved.");
    }

    @Test
    void getCategoryReturnsAllExistingCategories() {
        System.out.println("\n========== TEST: GET /categories preserves existing categories ==========");

        String createdId = null;

        try {
            ApiResponse initial = ApiClient.get("/categories");

            System.out.println("[GET] Initial status: " + initial.status());
            System.out.println("[GET] Initial categories: " + initial.body());

            assertEquals(200, initial.status());

            createdId = createCategory();

            ApiResponse result = ApiClient.get("/categories");

            System.out.println("[GET] Status after creation: " + result.status());
            System.out.println("[GET] Categories after creation: " + result.body());

            assertEquals(200, result.status());

            assertTrue(containsId(result.body(), createdId),
                "New category should appear in GET /categories");

            System.out.println("[CHECK] New category ID " + createdId + " is present.");

            Matcher oldIds = Pattern
                .compile("\"id\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(initial.body());

            while (oldIds.find()) {
                String oldId = oldIds.group(1);

                System.out.println("[CHECK] Verifying existing category ID: " + oldId);

                assertTrue(containsId(result.body(), oldId),
                    "Existing category ID " + oldId + " should remain present");

                System.out.println("[CHECK] Existing category ID " + oldId + " remains present.");
            }

            System.out.println("[PASS] Existing categories were preserved.");
        } finally {
            if (createdId != null) {
                deleteCategory(createdId);
            }
        }
    }

    @Test
    void postCategoryCreatesCategory() {
        System.out.println("\n========== TEST: POST /categories ==========");

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

            System.out.println("[POST] Sending category: " + json);

            ApiResponse response = ApiClient.post("/categories", json);

            System.out.println("[POST] Status: " + response.status());
            System.out.println("[POST] Response: " + response.body());

            assertTrue(response.status() == 200 || response.status() == 201,
                "POST should create a category: " + response.body());

            assertTrue(response.body().contains(title),
                "Response should contain the category title");
            assertTrue(response.body().contains(description),
                "Response should contain the category description");

            createdId = extractId(response.body());

            System.out.println("[PASS] Created category ID " + createdId
                + " with title '" + title + "'.");
        } finally {
            if (createdId != null) {
                deleteCategory(createdId);
            }
        }
    }

    @Test
    void headCategoryReturnsHeaders() {
        System.out.println("\n========== TEST: HEAD /categories ==========");

        ApiResponse response = ApiClient.head("/categories");

        System.out.println("[HEAD] Status: " + response.status());
        System.out.println("[HEAD] Response body length: " + response.body().length());

        assertEquals(200, response.status());
        assertTrue(response.body().isEmpty(),
            "HEAD response should not contain a body");

        System.out.println("[PASS] HEAD returned 200 with an empty body.");
    }


    // <--------------------- /categories/:id tests --------------------->
    @Test
    void getCategoryByIdReturnsCorrectCategory() {
        System.out.println("\n========== TEST: GET /categories/:id ==========");

        String createdId = createCategory();

        try {
            ApiResponse response = ApiClient.get("/categories/" + createdId);

            System.out.println("[GET] Requested category ID: " + createdId);
            System.out.println("[GET] Status: " + response.status());
            System.out.println("[GET] Response: " + response.body());

            assertEquals(200, response.status());
            assertTrue(containsId(response.body(), createdId),
                "Response should contain the category ID");
            assertTrue(response.body().contains("JUnit Test Category"),
                "Response should contain the expected category title");

            System.out.println("[PASS] Retrieved the expected category.");
        } finally {
            deleteCategory(createdId);
        }
    }

    @Test
    void headCategoryByIdReturnsSuccess() {
        System.out.println("\n========== TEST: HEAD /categories/:id ==========");

        String createdId = createCategory();

        try {
            ApiResponse response = ApiClient.head("/categories/" + createdId);

            System.out.println("[HEAD] Requested category ID: " + createdId);
            System.out.println("[HEAD] Status: " + response.status());
            System.out.println("[HEAD] Response body length: " + response.body().length());

            assertEquals(200, response.status());
            assertTrue(response.body().isEmpty(),
                "HEAD response should not contain a body");

            System.out.println("[PASS] HEAD returned 200 with an empty body.");
        } finally {
            deleteCategory(createdId);
        }
    }

    @Test
    void putCategoryUpdatesCategory() {
        System.out.println("\n========== TEST: PUT /categories/:id ==========");

        String createdId = createCategory();

        try {
            String json = """
                {
                    "title": "Updated by PUT",
                    "description": "Updated description"
                }
                """;

            System.out.println("[PUT] Updating category ID: " + createdId);
            System.out.println("[PUT] Request body: " + json);

            ApiResponse response = ApiClient.put("/categories/" + createdId, json);

            System.out.println("[PUT] Status: " + response.status());
            System.out.println("[PUT] Response: " + response.body());

            assertTrue(
                response.status() == 200 || response.status() == 204,
                "PUT update should succeed: " + response.body()
            );

            ApiResponse check = ApiClient.get("/categories/" + createdId);

            System.out.println("[GET] Verifying updated category...");
            System.out.println("[GET] Status: " + check.status());
            System.out.println("[GET] Updated category: " + check.body());

            assertEquals(200, check.status());
            assertTrue(check.body().contains("Updated by PUT"));
            assertTrue(check.body().contains("Updated description"));

            System.out.println("[PASS] Category was updated and changes persisted.");
        } finally {
            deleteCategory(createdId);
        }
    }

    @Test
    void deleteCategoryByIdReturnsSuccess() {
        System.out.println("\n========== TEST: DELETE /categories/:id ==========");

        String createdId = createCategory();

        System.out.println("[DELETE] Deleting category ID: " + createdId);

        ApiResponse deleteResponse = ApiClient.delete("/categories/" + createdId);

        System.out.println("[DELETE] Status: " + deleteResponse.status());
        System.out.println("[DELETE] Response: " + deleteResponse.body());

        assertTrue(
            deleteResponse.status() == 200 || deleteResponse.status() == 204,
            "DELETE should succeed: " + deleteResponse.body()
        );

        System.out.println("[GET] Verifying category no longer exists...");

        ApiResponse getResponse = ApiClient.get("/categories/" + createdId);

        System.out.println("[GET] Status after deletion: " + getResponse.status());

        assertEquals(404, getResponse.status(),
            "GET after DELETE should return 404");

        System.out.println("[PASS] Category was deleted and GET returned 404.");
    }
}