package categories;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class CategoriesProjectsTest {
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

    private String createProject() throws Exception {
        String json = """
                {
                    "title": "JUnit Test Project",
                    "completed": false,
                    "active": true,
                    "description": "Temporary test project"
                }
                """;

        HttpResponse<String> response = send("POST", "/projects", json);

        assertTrue(response.statusCode() == 200 || response.statusCode() == 201,
            "Project creation failed: " + response.body());

        return extractId(response.body());
    }

    private void deleteProject(String id) throws Exception {
        send("DELETE", "/projects/" + id, null);
    }

    private void linkProjectToCategory(String categoryId, String projectId) throws Exception {
        String json = """
        {
            "id": "%s"
        }
        """.formatted(projectId);

        HttpResponse<String> response = send(
            "POST",
            "/categories/" + categoryId + "/projects",
            json
        );

        assertTrue(response.statusCode() == 200 || response.statusCode() == 201,
            "Linking project failed: " + response.body());
    }

    // <--------------------- /categories/:id/projects tests --------------------->
    @Test
    void getCategoryProjectsReturnsSuccess() throws Exception {
        String categoryId = createCategory();

        try {
            HttpResponse<String> response =
                send("GET",
                    "/categories/" + categoryId + "/projects",
                    null);

            assertEquals(200, response.statusCode());
            assertNotNull(response.body());
        } finally {
            deleteCategory(categoryId);
        }
    }

    @Test
    void headCategoryProjectsReturnsHeaders() throws Exception {
        String categoryId = createCategory();

        try {
            HttpResponse<String> response =
                send("HEAD",
                    "/categories/" + categoryId + "/projects",
                    null);

            assertEquals(200, response.statusCode());
            assertTrue(response.body().isEmpty(),
                "HEAD response should not contain a body");
        } finally {
            deleteCategory(categoryId);
        }
    }

    // The API's example output represents "completed" and "active" fields as strings,
    // but POST /projects requires actual JSON booleans.
    // Sending "true"/"false" as strings causes a validation error.
    @Test
    void postCategoryProjectsCreatesRelationship() throws Exception {
        String categoryId = createCategory();
        String projectId = null;

        try {
            projectId = createProject();
            linkProjectToCategory(categoryId, projectId);

            HttpResponse<String> response =
                send("GET",
                    "/categories/" + categoryId + "/projects",
                    null);

            assertEquals(200, response.statusCode());
            assertTrue(containsId(response.body(), projectId),
                "Project should appear in the category's projects");
        } finally {
            deleteCategory(categoryId);

            if (projectId != null) {
                deleteProject(projectId);
            }
        }
    }


    // <------------------ /categories/:id/projects/:id tests --------------------->
    @Test
    void deleteCategoryProjectRelationshipRemovesLink() throws Exception {
        String categoryId = createCategory();
        String projectId = null;

        try {
            projectId = createProject();
            linkProjectToCategory(categoryId, projectId);

            HttpResponse<String> deleteResponse =
                send("DELETE",
                    "/categories/" + categoryId
                        + "/projects/" + projectId,
                    null);

            assertTrue(deleteResponse.statusCode() >= 200
                    && deleteResponse.statusCode() < 300,
                "Relationship deletion failed: "
                    + deleteResponse.body());

            HttpResponse<String> getResponse =
                send("GET",
                    "/categories/" + categoryId + "/projects",
                    null);

            assertEquals(200, getResponse.statusCode());
            assertFalse(containsId(getResponse.body(), projectId),
                "Project relationship should no longer exist");
        } finally {
            deleteCategory(categoryId);

            if (projectId != null) {
                deleteProject(projectId);
            }
        }
    }
}
