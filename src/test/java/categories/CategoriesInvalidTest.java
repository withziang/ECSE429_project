package categories;

import common.ApiClient;
import common.ApiResponse;
import common.ServiceRunningCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ServiceRunningCheck.class)
public class CategoriesInvalidTest {
    @Test
    void malformedJsonCategoryIsRejected() {
        System.out.println("\n========== TEST: POST /categories - Malformed JSON ==========");
        System.out.println("[POST] Sending malformed JSON...");

        ApiResponse response = ApiClient.post(
            "/categories",
            "{\"title\":"
        );

        System.out.println("[POST] Status: " + response.status());
        System.out.println("[POST] Response: " + response.body());

        assertFalse(response.status() >= 200 && response.status() < 300,
            "Malformed JSON should not be accepted");

        System.out.println("[PASS] Malformed JSON was rejected.");
    }

    @Test
    void malformedXmlCategoryIsRejected() {
        System.out.println("\n========== TEST: POST /categories - Malformed XML ==========");
        System.out.println("[POST] Sending malformed XML...");

        ApiResponse response = ApiClient.send(
            "POST",
            "/categories",
            "<category><title>Broken</category>",
            ApiClient.XML,
            ApiClient.JSON
        );

        System.out.println("[POST] Status: " + response.status());
        System.out.println("[POST] Response: " + response.body());

        assertFalse(response.status() >= 200 && response.status() < 300,
            "Malformed XML should not be accepted");

        System.out.println("[PASS] Malformed XML was rejected.");
    }

    @Test
    void nonNumericCategoryIdIsRejected() {
        System.out.println("\n========== TEST: GET /categories/abc ==========");
        System.out.println("[GET] Requesting category with non-numeric ID...");

        ApiResponse response = ApiClient.get("/categories/abc");

        System.out.println("[GET] Status: " + response.status());
        System.out.println("[GET] Response: " + response.body());

        assertFalse(response.status() >= 200 && response.status() < 300,
            "A non-numeric category ID should not return success");

        System.out.println("[PASS] Non-numeric category ID did not return success.");
    }

    @Test
    void nonexistentCategoryReturnsNotFound() {
        System.out.println("\n========== TEST: GET /categories/99999 ==========");
        System.out.println("[GET] Requesting a nonexistent category...");

        ApiResponse response = ApiClient.get("/categories/99999");

        System.out.println("[GET] Status: " + response.status());
        System.out.println("[GET] Response: " + response.body());

        assertEquals(404, response.status(),
            "A nonexistent category should return 404");

        System.out.println("[PASS] Nonexistent category returned 404.");
    }

    @Test
    void deletingCategoryTwiceDoesNotSucceedTwice() {
        System.out.println("\n========== TEST: DELETE /categories/:id twice ==========");

        String json = """
            {
                "title": "Double Delete Test",
                "description": "Temporary category"
            }
            """;

        ApiResponse created = ApiClient.post("/categories", json);

        System.out.println("[POST] Creation status: " + created.status());
        System.out.println("[POST] Response: " + created.body());

        assertTrue(created.status() >= 200 && created.status() < 300,
            "Category creation failed: " + created.body());

        String id = created.json().path("id").asText();
        assertFalse(id.isBlank(), "Created category has no ID");

        ApiResponse firstDelete = ApiClient.delete("/categories/" + id);

        System.out.println("[DELETE] First deletion status: " + firstDelete.status());
        System.out.println("[DELETE] First response: " + firstDelete.body());

        assertTrue(firstDelete.status() >= 200 && firstDelete.status() < 300,
            "First deletion failed");

        ApiResponse secondDelete = ApiClient.delete("/categories/" + id);

        System.out.println("[DELETE] Second deletion status: " + secondDelete.status());
        System.out.println("[DELETE] Second response: " + secondDelete.body());

        assertFalse(secondDelete.status() >= 200 && secondDelete.status() < 300,
            "Deleting the same category twice unexpectedly succeeded");

        System.out.println("[PASS] Second deletion did not return success.");
    }

    @Test
    void missingCategoryTitleIsRejected() {
        System.out.println("\n========== TEST: POST /categories - Missing title ==========");

        String json = """
            {
                "description": "Category without a title"
            }
            """;

        System.out.println("[POST] Creating category without a title...");

        ApiResponse response = ApiClient.post("/categories", json);

        System.out.println("[POST] Status: " + response.status());
        System.out.println("[POST] Response: " + response.body());

        assertFalse(response.status() >= 200 && response.status() < 300,
            "A category without a mandatory title should be rejected");

        System.out.println("[PASS] Missing title was rejected.");
    }

    @Test
    void emptyCategoryTitleIsRejected() {
        System.out.println("\n========== TEST: POST /categories - Empty title ==========");

        String json = """
            {
                "title": "",
                "description": "Category with an empty title"
            }
            """;

        System.out.println("[POST] Creating category with an empty title...");

        ApiResponse response = ApiClient.post("/categories", json);

        System.out.println("[POST] Status: " + response.status());
        System.out.println("[POST] Response: " + response.body());

        assertFalse(response.status() >= 200 && response.status() < 300,
            "A category with an empty title should be rejected");

        System.out.println("[PASS] Empty title was rejected.");
    }
}