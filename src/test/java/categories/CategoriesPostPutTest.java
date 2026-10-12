package categories;

import common.ApiClient;
import common.ApiResponse;
import common.ServiceRunningCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ServiceRunningCheck.class)
public class CategoriesPostPutTest {

    @Test
    void postAndPutHandleOmittedFieldsDifferently() {
        System.out.println(
            "\n========== TEST: POST vs PUT /categories/:id ==========");

        String originalJson = """
                {
                    "title": "Original Category",
                    "description": "Original description"
                }
                """;

        String updateJson = """
                {
                    "title": "Updated Category"
                }
                """;

        System.out.println("[POST] Creating test category...");

        ApiResponse created = ApiClient.post("/categories", originalJson);

        System.out.println("[POST] Creation status: " + created.status());
        System.out.println("[POST] Response: " + created.body());

        assertTrue(created.status() >= 200 && created.status() < 300, "Category creation failed: " + created.body());

        String id = created.json().path("id").asText();
        assertFalse(id.isBlank(), "Created category has no ID");

        try {
            // Test POST update
            System.out.println("[POST] Updating title only...");

            ApiResponse postResponse =
                ApiClient.post("/categories/" + id, updateJson);

            System.out.println("[POST] Status: " + postResponse.status());
            System.out.println("[POST] Response: " + postResponse.body());

            assertTrue(postResponse.status() >= 200 && postResponse.status() < 300, "POST update failed");

            String descriptionAfterPost = postResponse.json().path("description").asText();
            System.out.println("[POST] Description in response: " + descriptionAfterPost);
            assertEquals("Original description", descriptionAfterPost, "POST response should preserve the omitted description");

            // Reset category before testing PUT
            System.out.println("[PUT] Resetting category...");

            ApiResponse reset = ApiClient.put("/categories/" + id, originalJson);

            System.out.println("[PUT] Reset status: " + reset.status());
            System.out.println("[PUT] Reset response: " + reset.body());

            assertTrue(reset.status() >= 200 && reset.status() < 300, "Could not reset category");

            // Test PUT update
            System.out.println("[PUT] Updating title only...");

            ApiResponse putResponse = ApiClient.put("/categories/" + id, updateJson);

            System.out.println("[PUT] Status: " + putResponse.status());
            System.out.println("[PUT] Response: " + putResponse.body());

            assertTrue(putResponse.status() >= 200 && putResponse.status() < 300, "PUT update failed");

            String descriptionAfterPut = putResponse.json().path("description").asText();
            System.out.println("[PUT] Description in response: " + descriptionAfterPut);
            assertNotEquals(descriptionAfterPost, descriptionAfterPut, "Expected POST and PUT to handle omitted fields differently");

            System.out.println("[PASS] POST and PUT handle omitted descriptions differently.");

        } finally {
            System.out.println("[CLEANUP] Deleting test category...");
            ApiResponse deleted = ApiClient.delete("/categories/" + id);
            System.out.println("[CLEANUP] Status: " + deleted.status());
        }
    }
}