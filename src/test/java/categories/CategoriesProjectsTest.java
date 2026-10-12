package categories;

import common.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ServiceRunningCheck.class)
public class CategoriesProjectsTest {
    // --------------------- Resource-specific helpers ---------------------
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

        assertTrue(
            response.status() == 200 || response.status() == 201,
            "Category creation failed: " + response.body()
        );

        String id = extractId(response.body());
        System.out.println("[POST] Created category ID: " + id);

        return id;
    }

    private void deleteCategory(String id) {
        System.out.println("[CLEANUP] Deleting category ID: " + id);

        ApiResponse response = ApiClient.delete("/categories/" + id);

        System.out.println("[CLEANUP] Category DELETE status: " + response.status());
    }

    private String createProject() {
        String json = """
            {
                "title": "JUnit Test Project",
                "completed": false,
                "active": true,
                "description": "Temporary test project"
            }
            """;

        System.out.println("[POST] Creating test project...");

        ApiResponse response = ApiClient.post("/projects", json);

        System.out.println("[POST] Status: " + response.status());
        System.out.println("[POST] Response: " + response.body());

        assertTrue(
            response.status() == 200 || response.status() == 201,
            "Project creation failed: " + response.body()
        );

        String id = extractId(response.body());
        System.out.println("[POST] Created project ID: " + id);

        return id;
    }

    private void deleteProject(String id) {
        System.out.println("[CLEANUP] Deleting project ID: " + id);

        ApiResponse response = ApiClient.delete("/projects/" + id);

        System.out.println("[CLEANUP] Project DELETE status: " + response.status());
    }

    private void linkProjectToCategory(String categoryId, String projectId) {
        String json = """
            {
                "id": "%s"
            }
            """.formatted(projectId);

        System.out.println("[POST] Linking project " + projectId
            + " to category " + categoryId);
        System.out.println("[POST] Relationship request body: " + json);

        ApiResponse response = ApiClient.post(
            "/categories/" + categoryId + "/projects",
            json
        );

        System.out.println("[POST] Relationship status: " + response.status());
        System.out.println("[POST] Relationship response: " + response.body());

        assertTrue(
            response.status() == 200 || response.status() == 201,
            "Linking project failed: " + response.body()
        );

        System.out.println("[PASS] Project was linked to the category.");
    }


    // --------------------- /categories/:id/projects tests ---------------------
    @Test
    void getCategoryProjectsReturnsSuccess() {
        System.out.println("\n========== TEST: GET /categories/:id/projects ==========");

        String categoryId = createCategory();

        try {
            System.out.println("[GET] Retrieving projects for category " + categoryId);

            ApiResponse response =
                ApiClient.get("/categories/" + categoryId + "/projects");

            System.out.println("[GET] Status: " + response.status());
            System.out.println("[GET] Response: " + response.body());

            assertEquals(200, response.status());
            assertNotNull(response.body());

            System.out.println("[PASS] Category projects endpoint returned a response.");
        } finally {
            deleteCategory(categoryId);
        }
    }

    @Test
    void headCategoryProjectsReturnsHeaders() {
        System.out.println("\n========== TEST: HEAD /categories/:id/projects ==========");

        String categoryId = createCategory();

        try {
            System.out.println("[HEAD] Checking projects endpoint for category " + categoryId);

            ApiResponse response =
                ApiClient.head("/categories/" + categoryId + "/projects");

            System.out.println("[HEAD] Status: " + response.status());
            System.out.println("[HEAD] Response body length: " + response.body().length());

            assertEquals(200, response.status());
            assertTrue(
                response.body().isEmpty(),
                "HEAD response should not contain a body"
            );

            System.out.println("[PASS] HEAD returned 200 with an empty body.");
        } finally {
            deleteCategory(categoryId);
        }
    }

    // The API's example output represents "completed" and "active" as strings,
    // but POST /projects requires actual JSON booleans (without quotation marks).
    @Test
    void postCategoryProjectsCreatesRelationship() {
        System.out.println("\n========== TEST: POST /categories/:id/projects ==========");

        String categoryId = createCategory();
        String projectId = null;

        try {
            projectId = createProject();
            linkProjectToCategory(categoryId, projectId);

            System.out.println("[GET] Verifying project " + projectId
                + " appears under category " + categoryId);

            ApiResponse response =
                ApiClient.get("/categories/" + categoryId + "/projects");

            System.out.println("[GET] Status: " + response.status());
            System.out.println("[GET] Response: " + response.body());

            assertEquals(200, response.status());
            assertTrue(
                containsId(response.body(), projectId),
                "Project should appear in the category's projects"
            );

            System.out.println("[PASS] Project " + projectId
                + " appears in category " + categoryId + "'s projects.");
        } finally {
            deleteCategory(categoryId);

            if (projectId != null) {
                deleteProject(projectId);
            }
        }
    }


    // ------------------ /categories/:id/projects/:id tests ------------------
    @Test
    void deleteCategoryProjectRelationshipRemovesLink() {
        System.out.println("\n========== TEST: DELETE /categories/:id/projects/:id ==========");

        String categoryId = createCategory();
        String projectId = null;

        try {
            projectId = createProject();
            linkProjectToCategory(categoryId, projectId);

            System.out.println("[DELETE] Removing relationship between category "
                + categoryId + " and project " + projectId);

            ApiResponse deleteResponse = ApiClient.delete(
                "/categories/" + categoryId + "/projects/" + projectId
            );

            System.out.println("[DELETE] Status: " + deleteResponse.status());
            System.out.println("[DELETE] Response: " + deleteResponse.body());

            assertTrue(
                deleteResponse.status() == 200 || deleteResponse.status() == 204,
                "Relationship deletion failed: " + deleteResponse.body()
            );

            System.out.println("[GET] Verifying the relationship no longer exists...");

            ApiResponse getResponse =
                ApiClient.get("/categories/" + categoryId + "/projects");

            System.out.println("[GET] Status: " + getResponse.status());
            System.out.println("[GET] Response: " + getResponse.body());

            assertEquals(200, getResponse.status());
            assertFalse(
                containsId(getResponse.body(), projectId),
                "Project relationship should no longer exist"
            );

            System.out.println("[PASS] Relationship was removed successfully.");
        } finally {
            deleteCategory(categoryId);

            if (projectId != null) {
                deleteProject(projectId);
            }
        }
    }
}