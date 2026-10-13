package categories;

import com.fasterxml.jackson.databind.JsonNode;
import common.ApiClient;
import common.ApiResponse;
import common.ServiceRunningCheck;
import common.SystemState;
import common.TestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** /categories and /categories/:id. */
@ExtendWith(ServiceRunningCheck.class)
class CategoriesTest {

    private static final String TEST_CATEGORY = """
            {
                "title": "JUnit Test Category",
                "description": "Temporary test category"
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

    // <--------------------- /categories tests --------------------->
    @Test
    void getCategoryReturnsSuccess() {
        ApiResponse response = ApiClient.get("/categories");

        assertEquals(200, response.status());
        assertEquals(Optional.of(ApiClient.JSON), response.header("Content-Type"));
        assertTrue(response.json().path("categories").isArray(), response.body());
    }

    @Test
    void getCategoryReturnsAllExistingCategories() {
        List<String> existingIds = ApiClient.get("/categories").idsIn("categories");
        String createdId = fixture.create("categories", TEST_CATEGORY);

        List<String> idsAfterCreation = ApiClient.get("/categories").idsIn("categories");

        assertTrue(idsAfterCreation.contains(createdId), "New category should appear in GET /categories");
        assertTrue(idsAfterCreation.containsAll(existingIds), "Existing categories should remain present");
    }

    @Test
    void postCategoryCreatesCategory() {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post("/categories", """
                {
                    "title": "JUnit POST Test",
                    "description": "Temporary test category"
                }
                """);
        String createdId = response.json().path("id").asText();
        fixture.recordCreated("categories", createdId);

        assertEquals(201, response.status());
        assertEquals(Optional.of("categories/" + createdId), response.header("Location"));
        JsonNode stored = ApiClient.get("/categories/" + createdId).firstIn("categories");
        assertEquals("JUnit POST Test", stored.path("title").asText());
        assertEquals("Temporary test category", stored.path("description").asText());
        assertEquals(Set.of("categories/" + createdId), before.changedObjects(SystemState.capture()));
    }

    @Test
    void headCategoryReturnsHeaders() {
        ApiResponse response = ApiClient.head("/categories");

        assertEquals(200, response.status());
        assertTrue(response.body().isEmpty(), "HEAD response should not contain a body");
    }

    // <--------------------- /categories/:id tests --------------------->
    @Test
    void getCategoryByIdReturnsCorrectCategory() {
        String createdId = fixture.create("categories", TEST_CATEGORY);

        ApiResponse response = ApiClient.get("/categories/" + createdId);

        assertEquals(200, response.status());
        JsonNode category = response.firstIn("categories");
        assertEquals(createdId, category.path("id").asText());
        assertEquals("JUnit Test Category", category.path("title").asText());
        assertEquals("Temporary test category", category.path("description").asText());
    }

    @Test
    void getCategoryByIdAsXmlReturnsCategoryElement() {
        String createdId = fixture.create("categories", TEST_CATEGORY);

        ApiResponse response = ApiClient.send("GET", "/categories/" + createdId, null, ApiClient.JSON, ApiClient.XML);

        assertEquals(200, response.status());
        assertEquals(Optional.of(ApiClient.XML), response.header("Content-Type"));
        assertEquals("JUnit Test Category",
                response.xml().getElementsByTagName("title").item(0).getTextContent());
    }

    @Test
    void headCategoryByIdReturnsSuccess() {
        String createdId = fixture.create("categories", TEST_CATEGORY);

        ApiResponse response = ApiClient.head("/categories/" + createdId);

        assertEquals(200, response.status());
        assertTrue(response.body().isEmpty(), "HEAD response should not contain a body");
    }

    @Test
    void putCategoryUpdatesCategory() {
        String createdId = fixture.create("categories", TEST_CATEGORY);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.put("/categories/" + createdId, """
                {
                    "title": "Updated by PUT",
                    "description": "Updated description"
                }
                """);

        assertEquals(200, response.status());
        JsonNode stored = ApiClient.get("/categories/" + createdId).firstIn("categories");
        assertEquals("Updated by PUT", stored.path("title").asText());
        assertEquals("Updated description", stored.path("description").asText());
        assertEquals(Set.of("categories/" + createdId), before.changedObjects(SystemState.capture()));
    }

    @Test
    void deleteCategoryByIdReturnsSuccess() {
        String createdId = fixture.create("categories", TEST_CATEGORY);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.delete("/categories/" + createdId);

        assertEquals(200, response.status());
        assertEquals(404, ApiClient.get("/categories/" + createdId).status(), "GET after DELETE should return 404");
        assertEquals(Set.of("categories/" + createdId), before.changedObjects(SystemState.capture()));
    }
}
