package categories;

import common.ApiClient;
import common.ApiResponse;
import common.ServiceRunningCheck;
import common.SystemState;
import common.TestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Invalid requests to /categories and /categories/:id. */
@ExtendWith(ServiceRunningCheck.class)
class CategoriesInvalidTest {

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
    void malformedJsonCategoryIsRejected() {
        assertCreationRejected(ApiClient.JSON, "{\"title\":");
    }

    @Test
    void malformedXmlCategoryIsRejected() {
        assertCreationRejected(ApiClient.XML, "<category><title>Broken</category>");
    }

    @Test
    void missingCategoryTitleIsRejected() {
        ApiResponse response = assertCreationRejected(ApiClient.JSON, """
                {
                    "description": "Category without a title"
                }
                """);

        assertTrue(response.body().contains("title : field is mandatory"), response.body());
    }

    @Test
    void emptyCategoryTitleIsRejected() {
        ApiResponse response = assertCreationRejected(ApiClient.JSON, """
                {
                    "title": "",
                    "description": "Category with an empty title"
                }
                """);

        assertTrue(response.body().contains("title : can not be empty"), response.body());
    }

    @Test
    void nonNumericCategoryIdIsRejected() {
        ApiResponse response = ApiClient.get("/categories/abc");

        assertEquals(404, response.status(), "A non-numeric category ID should not be found");
    }

    @Test
    void nonexistentCategoryReturnsNotFound() {
        ApiResponse response = ApiClient.get("/categories/99999");

        assertEquals(404, response.status(), "A nonexistent category should return 404");
        assertTrue(response.body().contains("Could not find an instance with categories/99999"), response.body());
    }

    @Test
    void deletingCategoryTwiceDoesNotSucceedTwice() {
        String id = fixture.create("categories", """
                {
                    "title": "Double Delete Test",
                    "description": "Temporary category"
                }
                """);

        ApiResponse firstDelete = ApiClient.delete("/categories/" + id);
        ApiResponse secondDelete = ApiClient.delete("/categories/" + id);

        assertEquals(200, firstDelete.status());
        assertEquals(404, secondDelete.status(), "Deleting the same category twice should return 404");
    }

    private ApiResponse assertCreationRejected(String contentType, String body) {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.send("POST", "/categories", body, contentType, ApiClient.JSON);

        assertEquals(400, response.status(), response.body());
        assertEquals(Set.of(), before.changedObjects(SystemState.capture()), "A rejected request should change nothing");
        return response;
    }
}
