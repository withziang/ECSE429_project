package stepdefinitions.categories;

import common.ApiClient;
import common.ApiResponse;
import common.HttpStatus;
import common.JsonBodies;

import stepdefinitions.common.ScenarioContext;

import com.fasterxml.jackson.databind.JsonNode;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class CategorySteps {

    private final ScenarioContext context;

    public CategorySteps(ScenarioContext context) {
        this.context = context;
    }


    // <------------------------- Create a category -------------------------->
    @When("I create a category with title {string}")
    public void createCategoryWithTitle(String title) {
        ApiResponse response = ApiClient.post(
            "/categories",
            JsonBodies.object("title", title)
        );

        context.setLastResponse(response);
        rememberCreatedCategory(response, title);
    }

    @When("I create a category with title {string} and description {string}")
    public void createCategoryWithTitleAndDescription(
        String title, String description) {

        ApiResponse response = ApiClient.post(
            "/categories",
            JsonBodies.object(
                "title", title,
                "description", description
            )
        );

        context.setLastResponse(response);
        rememberCreatedCategory(response, title);
    }

    @Then("the category {string} exists")
    public void categoryExists(String title) {
        assertTrue(categoryWithTitleExists(title));
    }

    @Then("no category exists with title {string}")
    public void categoryDoesNotExist(String title) {
        assertFalse(categoryWithTitleExists(title));
    }

    @When("I create a category with invalid title {string}")
    public void createCategoryWithInvalidTitle(String invalidTitle) {

        String title = switch (invalidTitle) {
            case "EMPTY" -> "";
            case "WHITESPACE" -> "   ";
            default -> throw new IllegalArgumentException(
                "Unknown invalid title: " + invalidTitle
            );
        };

        ApiResponse response = ApiClient.post(
            "/categories",
            JsonBodies.object("title", title)
        );

        context.setLastResponse(response);
    }

    @Then("no category exists with invalid title {string}")
    public void noCategoryExistsWithInvalidTitle(String invalidTitle) {

        String title = switch (invalidTitle) {
            case "EMPTY" -> "";
            case "WHITESPACE" -> "   ";
            default -> throw new IllegalArgumentException(
                "Unknown invalid title: " + invalidTitle
            );
        };

        assertFalse(categoryWithTitleExists(title));
    }


    // <------------------------- Rename a category -------------------------------->
    @When("I rename category {string} to {string} using {string}")
    public void renameCategory(
        String oldTitle, String newTitle, String method) {

        String id = context.idOf("categories", oldTitle);

        ApiResponse response = updateCategory(method, id, newTitle);

        context.setLastResponse(response);

        if (response.status() == HttpStatus.OK) {
            context.remember("categories", newTitle, id);
        }
    }

    @When("I rename category with id {string} to {string} using {string}")
    public void renameMissingCategory(
        String id, String title, String method) {

        context.setLastResponse(updateCategory(method, id, title));
    }


    // <-------------------- Assign category to a project ----------------------->
    @When("I assign project {string} to category {string}")
    public void assignProject(String project, String category) {

        String categoryId = context.idOf("categories", category);
        String projectId = context.idOf("projects", project);

        ApiResponse response = ApiClient.post(
            "/categories/" + categoryId + "/projects",
            JsonBodies.idReference(projectId)
        );

        context.setLastResponse(response);
    }

    @When("I assign project {string} to category {string} twice")
    public void assignProjectTwice(String project, String category) {

        String categoryId = context.idOf("categories", category);
        String projectId = context.idOf("projects", project);

        ApiClient.post(
            "/categories/" + categoryId + "/projects",
            JsonBodies.idReference(projectId)
        );

        ApiResponse response = ApiClient.post(
            "/categories/" + categoryId + "/projects",
            JsonBodies.idReference(projectId)
        );

        context.setLastResponse(response);
    }

    @When("I assign project with id {string} to category {string}")
    public void assignMissingProject(String projectId, String category) {

        String categoryId = context.idOf("categories", category);

        ApiResponse response = ApiClient.post(
            "/categories/" + categoryId + "/projects",
            JsonBodies.idReference(projectId)
        );

        context.setLastResponse(response);
    }

    @Then("category {string} contains project {string} exactly once")
    public void categoryContainsProject(String category, String project) {

        String categoryId = context.idOf("categories", category);
        String projectId = context.idOf("projects", project);

        ApiResponse response = ApiClient.get(
            "/categories/" + categoryId + "/projects"
        );

        assertEquals(HttpStatus.OK, response.status());

        long count = response.json().findValues("id").stream()
            .filter(node -> node.asText().equals(projectId))
            .count();

        assertEquals(1, count);
    }

    @Then("the response status indicates success")
    public void responseStatusIndicatesSuccess() {

        int status = context.lastResponse().status();

        assertTrue(
            status == 200 || status == 201 || status == 204,
            "Expected successful response but received: " + status
        );
    }


    // <-------------------- List category todos -------------------->
    @Given("todo {string} is assigned to category {string}")
    public void assignTodoToCategory(String todo, String category) {

        String categoryId = context.idOf("categories", category);
        String todoId = context.idOf("todos", todo);

        ApiResponse response = ApiClient.post(
            "/categories/" + categoryId + "/todos",
            JsonBodies.idReference(todoId)
        );

        assertTrue(
            response.status() == 200 || response.status() == 201,
            "Could not assign todo to category. Status: " + response.status()
        );
    }

    @When("I list todos for category {string}")
    public void listCategoryTodos(String category) {

        String categoryId = context.idOf("categories", category);

        context.setLastResponse(
            ApiClient.get("/categories/" + categoryId + "/todos")
        );
    }

    @When("I list todos for category with id {string}")
    public void listTodosForMissingCategory(String id) {

        context.setLastResponse(
            ApiClient.get("/categories/" + id + "/todos")
        );
    }

    @Then("category {string} contains todo {string}")
    public void categoryContainsTodo(String category, String todo) {

        String todoId = context.idOf("todos", todo);

        long count = context.lastResponse().json()
            .findValues("id")
            .stream()
            .filter(node -> node.asText().equals(todoId))
            .count();

        assertEquals(1, count);
    }

    @Then("category {string} has no todos")
    public void categoryHasNoTodos(String category) {

        assertEquals(
            0,
            context.lastResponse().json().path("todos").size()
        );
    }

    @Then("the response contains the todo {string} of another category")
    public void responseContainsTodoOfAnotherCategory(String todo) {

        assertTrue(
            context.lastResponse().idsIn("todos").contains(context.idOf("todos", todo))
        );
    }


    // <-------------------- Delete a category -------------------->
    @When("I delete category {string}")
    public void deleteCategory(String category) {

        String id = context.idOf("categories", category);

        context.setLastResponse(
            ApiClient.delete("/categories/" + id)
        );
    }

    @When("I delete category {string} twice")
    public void deleteCategoryTwice(String category) {

        String id = context.idOf("categories", category);

        ApiClient.delete("/categories/" + id);

        context.setLastResponse(
            ApiClient.delete("/categories/" + id)
        );
    }


    // <---------------------------- Helpers -------------------------------->
    private void rememberCreatedCategory(ApiResponse response, String title) {

        if (response.status() == HttpStatus.CREATED) {
            String id = response.json().findPath("id").asText();

            assertFalse(id.isEmpty(), "Category ID was not returned");

            context.remember("categories", title, id);
        }
    }

    private ApiResponse updateCategory(
        String method, String id, String title) {

        String path = "/categories/" + id;
        String body = JsonBodies.object("title", title);

        if (method.equalsIgnoreCase("POST")) {
            return ApiClient.post(path, body);
        }

        if (method.equalsIgnoreCase("PUT")) {
            return ApiClient.put(path, body);
        }

        throw new IllegalArgumentException("Unsupported method: " + method);
    }

    private boolean categoryWithTitleExists(String title) {
        ApiResponse response = ApiClient.get("/categories");

        assertEquals(HttpStatus.OK, response.status());

        JsonNode json = response.json();

        return json.findValues("title").stream()
            .anyMatch(node -> node.asText().equals(title));
    }
}