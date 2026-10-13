package categories;

import common.ApiClient;
import common.ApiResponse;
import common.ServiceRunningCheck;
import common.TestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * POST and PUT /categories/:id are both documented as amending a category,
 * but PUT clears the fields that are not sent (bug CAT-001).
 */
@ExtendWith(ServiceRunningCheck.class)
class CategoriesPostPutTest {

    private static final String ORIGINAL_CATEGORY = """
            {
                "title": "Original Category",
                "description": "Original description"
            }
            """;
    private static final String TITLE_ONLY = """
            {
                "title": "Updated Category"
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

    @Test
    @DisplayName("[Expected, fails] POST and PUT with only a title both keep the description")
    void postAndPutShouldBothPreserveOmittedFields() {
        String id = fixture.create("categories", ORIGINAL_CATEGORY);

        ApiResponse postResponse = ApiClient.post("/categories/" + id, TITLE_ONLY);
        ApiResponse putResponse = ApiClient.put("/categories/" + id, TITLE_ONLY);

        assertEquals(200, postResponse.status());
        assertEquals("Original description", postResponse.json().path("description").asText(),
                "POST should preserve the omitted description");
        assertEquals(200, putResponse.status());
        assertEquals("Original description", putResponse.json().path("description").asText(),
                "PUT should preserve the omitted description");
    }

    @Test
    @DisplayName("[Actual] POST with only a title keeps the description, PUT clears it")
    void postAndPutHandleOmittedFieldsDifferently() {
        String id = fixture.create("categories", ORIGINAL_CATEGORY);

        ApiResponse postResponse = ApiClient.post("/categories/" + id, TITLE_ONLY);
        ApiResponse putResponse = ApiClient.put("/categories/" + id, TITLE_ONLY);

        assertEquals(200, postResponse.status());
        assertEquals("Original description", postResponse.json().path("description").asText());
        assertEquals(200, putResponse.status());
        assertEquals("", putResponse.json().path("description").asText());
        assertEquals("", ApiClient.get("/categories/" + id).firstIn("categories").path("description").asText());
    }
}
