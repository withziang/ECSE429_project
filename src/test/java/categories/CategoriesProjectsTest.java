package categories;

import common.ApiClient;
import common.ApiResponse;
import common.JsonBodies;
import common.ServiceRunningCheck;
import common.SystemState;
import common.TestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** /categories/:id/projects and /categories/:id/projects/:id. */
@ExtendWith(ServiceRunningCheck.class)
class CategoriesProjectsTest {

    private static final String TEST_CATEGORY = """
            {
                "title": "JUnit Test Category",
                "description": "Temporary test category"
            }
            """;
    // The API's example output represents "completed" and "active" as strings,
    // but POST /projects requires actual JSON booleans (without quotation marks).
    private static final String TEST_PROJECT = """
            {
                "title": "JUnit Test Project",
                "completed": false,
                "active": true,
                "description": "Temporary test project"
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

    // --------------------- /categories/:id/projects tests ---------------------
    @Test
    void getCategoryProjectsReturnsSuccess() {
        String categoryId = fixture.create("categories", TEST_CATEGORY);

        ApiResponse response = ApiClient.get(projectsOf(categoryId));

        assertEquals(200, response.status());
        assertEquals(List.of(), response.idsIn("projects"));
    }

    @Test
    void headCategoryProjectsReturnsHeaders() {
        String categoryId = fixture.create("categories", TEST_CATEGORY);

        ApiResponse response = ApiClient.head(projectsOf(categoryId));

        assertEquals(200, response.status());
        assertTrue(response.body().isEmpty(), "HEAD response should not contain a body");
    }

    @Test
    void postCategoryProjectsCreatesRelationship() {
        String categoryId = fixture.create("categories", TEST_CATEGORY);
        String projectId = fixture.create("projects", TEST_PROJECT);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(projectsOf(categoryId), JsonBodies.idReference(projectId));

        assertEquals(201, response.status(), response.body());
        assertEquals(List.of(projectId), ApiClient.get(projectsOf(categoryId)).idsIn("projects"));
        assertEquals(Set.of("categories/" + categoryId), before.changedObjects(SystemState.capture()),
                "Linking should not modify the project");
    }

    // ------------------ /categories/:id/projects/:id tests ------------------
    @Test
    void deleteCategoryProjectRelationshipRemovesLink() {
        String categoryId = fixture.create("categories", TEST_CATEGORY);
        String projectId = fixture.create("projects", TEST_PROJECT);
        fixture.link(projectsOf(categoryId), projectId);
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.delete(projectsOf(categoryId) + "/" + projectId);

        assertEquals(200, response.status(), response.body());
        assertEquals(List.of(), ApiClient.get(projectsOf(categoryId)).idsIn("projects"));
        assertEquals(Set.of("categories/" + categoryId), before.changedObjects(SystemState.capture()));
    }

    @Test
    void deleteCategoryProjectRelationshipTwiceReturnsNotFound() {
        String categoryId = fixture.create("categories", TEST_CATEGORY);
        String projectId = fixture.create("projects", TEST_PROJECT);
        fixture.link(projectsOf(categoryId), projectId);
        ApiClient.delete(projectsOf(categoryId) + "/" + projectId);

        ApiResponse secondDelete = ApiClient.delete(projectsOf(categoryId) + "/" + projectId);

        assertEquals(404, secondDelete.status());
    }

    private static String projectsOf(String categoryId) {
        return "/categories/" + categoryId + "/projects";
    }
}
