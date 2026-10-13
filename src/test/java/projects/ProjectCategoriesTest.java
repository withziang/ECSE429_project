package projects;

import static common.HttpStatus.CREATED;
import static common.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import common.ApiClient;
import common.ApiResponse;
import common.JsonBodies;
import common.SystemState;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class ProjectCategoriesTest extends ProjectsTestSupport {

    @Test
    void getReturnsNoCategoriesForNewProject() {
        String projectId = createProject();

        ApiResponse response = ApiClient.get(categoriesPath(projectId));

        assertEquals(OK, response.status());
        assertEquals(0, response.json().get("categories").size());
    }

    @Test
    void getReturnsTheCategoriesOfTheProjectAsJson() {
        String projectId = createProject();
        String categoryId = createCategory();
        link(categoriesPath(projectId), categoryId);

        ApiResponse response = ApiClient.get(categoriesPath(projectId));

        assertEquals(OK, response.status());
        assertEquals(Optional.of(ApiClient.JSON), response.header("Content-Type"));
        JsonNode categories = response.json().get("categories");
        assertEquals(Set.of(categoryId), ids(categories));
        assertEquals(CATEGORY_TITLE, categories.get(0).get("title").asText());
    }

    @Test
    void getReturnsTheCategoriesOfTheProjectAsXml() {
        String projectId = createProject();
        String categoryId = createCategory();
        link(categoriesPath(projectId), categoryId);

        ApiResponse response = ApiClient.send("GET", categoriesPath(projectId), null, null, ApiClient.XML);

        assertEquals(OK, response.status());
        assertEquals(Optional.of(ApiClient.XML), response.header("Content-Type"));
        Document categories = response.xml();
        assertEquals(1, categories.getElementsByTagName("category").getLength());
        assertEquals(CATEGORY_TITLE, categories.getElementsByTagName("title").item(0).getTextContent());
    }

    @Test
    void headReturnsHeadersWithoutBody() {
        String projectId = createProject();

        ApiResponse response = ApiClient.head(categoriesPath(projectId));

        assertEquals(OK, response.status());
        assertEquals(Optional.of(ApiClient.JSON), response.header("Content-Type"));
        assertTrue(response.body().isEmpty());
    }

    @Test
    void postLinksAnExistingCategoryAndChangesOnlyTheProject() {
        String projectId = createProject();
        String categoryId = createCategory();
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post(categoriesPath(projectId), JsonBodies.idReference(categoryId));

        assertEquals(CREATED, response.status());
        assertEquals(Set.of(categoryId), ids(project(projectId).get("categories")));
        assertEquals(CATEGORY_TITLE, category(categoryId).get("title").asText());
        assertOnlyChangedSince(before, "projects/" + projectId);
    }

    @Test
    void postingTheSameCategoryTwiceKeepsOneLink() {
        String projectId = createProject();
        String categoryId = createCategory();
        link(categoriesPath(projectId), categoryId);

        ApiResponse response = ApiClient.post(categoriesPath(projectId), JsonBodies.idReference(categoryId));

        assertEquals(CREATED, response.status());
        assertEquals(1, ApiClient.get(categoriesPath(projectId)).json().get("categories").size());
    }

    @Test
    void deleteRemovesTheLinkAndKeepsTheCategory() {
        String projectId = createProject();
        String categoryId = createCategory();
        SystemState beforeLink = SystemState.capture();
        link(categoriesPath(projectId), categoryId);

        ApiResponse response = ApiClient.delete(categoriesPath(projectId) + "/" + categoryId);

        assertEquals(OK, response.status());
        assertEquals(0, ApiClient.get(categoriesPath(projectId)).json().get("categories").size());
        assertEquals(CATEGORY_TITLE, category(categoryId).get("title").asText());
        assertNothingChangedSince(beforeLink);
    }

    @Test
    void deleteLeavesTheOtherCategoriesLinked() {
        String projectId = createProject();
        String removedCategoryId = createCategory();
        String keptCategoryId = createCategory();
        link(categoriesPath(projectId), removedCategoryId);
        link(categoriesPath(projectId), keptCategoryId);

        ApiResponse response = ApiClient.delete(categoriesPath(projectId) + "/" + removedCategoryId);

        assertEquals(OK, response.status());
        assertEquals(Set.of(keptCategoryId),
                ids(ApiClient.get(categoriesPath(projectId)).json().get("categories")));
    }
}
