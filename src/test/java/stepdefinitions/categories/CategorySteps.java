package stepdefinitions.categories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import common.ApiClient;
import common.ApiResponse;
import common.HttpStatus;
import common.JsonBodies;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Collections;
import java.util.List;
import stepdefinitions.common.ScenarioContext;

public class CategorySteps {

    private final ScenarioContext context;

    public CategorySteps(ScenarioContext context) {
        this.context = context;
    }

    // Create a category

    @When("I create a category with title {string}")
    public void iCreateACategoryWithTitle(String title) {
        createCategory(title, JsonBodies.object("title", title));
    }

    @When("I create a category with title {string} and description {string}")
    public void iCreateACategoryWithTitleAndDescription(String title, String description) {
        createCategory(title, JsonBodies.object("title", title, "description", description));
    }

    @When("I create a category with invalid title {string}")
    public void iCreateACategoryWithInvalidTitle(String invalidTitle) {
        context.setLastResponse(ApiClient.post("/categories", JsonBodies.object("title", titleFor(invalidTitle))));
    }

    @Then("the category {string} exists")
    public void theCategoryExists(String title) {
        assertTrue(categoryWithTitleExists(title), "no category is titled \"" + title + "\"");
    }

    @Then("no category exists with title {string}")
    public void noCategoryExistsWithTitle(String title) {
        assertFalse(categoryWithTitleExists(title), "a category is titled \"" + title + "\"");
    }

    @Then("no category exists with invalid title {string}")
    public void noCategoryExistsWithInvalidTitle(String invalidTitle) {
        noCategoryExistsWithTitle(titleFor(invalidTitle));
    }

    // Rename a category

    @When("I rename category {string} to {string} using {string}")
    public void iRenameCategory(String oldTitle, String newTitle, String method) {
        String id = context.idOf("categories", oldTitle);
        iRenameCategoryWithId(id, newTitle, method);
        if (context.lastResponse().status() == HttpStatus.OK) {
            context.remember("categories", newTitle, id);
        }
    }

    @When("I rename category with id {string} to {string} using {string}")
    public void iRenameCategoryWithId(String id, String title, String method) {
        String path = "/categories/" + id;
        String body = JsonBodies.object("title", title);
        context.setLastResponse(switch (method) {
            case "POST" -> ApiClient.post(path, body);
            case "PUT" -> ApiClient.put(path, body);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        });
    }

    // Assign a category to a project

    @When("I assign project {string} to category {string}")
    public void iAssignProjectToCategory(String project, String category) {
        iAssignProjectWithIdToCategory(context.idOf("projects", project), category);
    }

    @When("I assign project {string} to category {string} twice")
    public void iAssignProjectToCategoryTwice(String project, String category) {
        iAssignProjectToCategory(project, category);
        iAssignProjectToCategory(project, category);
    }

    @When("I assign project with id {string} to category {string}")
    public void iAssignProjectWithIdToCategory(String projectId, String category) {
        String path = "/categories/" + context.idOf("categories", category) + "/projects";
        context.setLastResponse(ApiClient.post(path, JsonBodies.idReference(projectId)));
    }

    @Then("category {string} contains project {string} exactly once")
    public void categoryContainsProjectExactlyOnce(String category, String project) {
        ApiResponse response = ApiClient.get("/categories/" + context.idOf("categories", category) + "/projects");
        assertEquals(HttpStatus.OK, response.status(), "could not read the projects of \"" + category + "\"");
        assertEquals(1, Collections.frequency(response.idsIn("projects"), context.idOf("projects", project)));
    }

    @Then("the response status indicates success")
    public void theResponseStatusIndicatesSuccess() {
        int status = context.lastResponse().status();
        assertTrue(status / 100 == 2, "expected a successful response but received " + status);
    }

    // List a category's todos

    @Given("todo {string} is assigned to category {string}")
    public void todoIsAssignedToCategory(String todo, String category) {
        String path = "/categories/" + context.idOf("categories", category) + "/todos";
        ApiResponse response = ApiClient.post(path, JsonBodies.idReference(context.idOf("todos", todo)));
        assertEquals(HttpStatus.CREATED, response.status(), "could not add \"" + todo + "\" to \"" + category + "\"");
    }

    @When("I list todos for category {string}")
    public void iListTodosForCategory(String category) {
        iListTodosForCategoryWithId(context.idOf("categories", category));
    }

    @When("I list todos for category with id {string}")
    public void iListTodosForCategoryWithId(String id) {
        context.setLastResponse(ApiClient.get("/categories/" + id + "/todos"));
    }

    @Then("category {string} contains todo {string}")
    public void categoryContainsTodo(String category, String todo) {
        assertEquals(1, Collections.frequency(listedTodoIds(), context.idOf("todos", todo)),
                "\"" + todo + "\" is not listed once in \"" + category + "\"");
    }

    @Then("category {string} has no todos")
    public void categoryHasNoTodos(String category) {
        assertTrue(listedTodoIds().isEmpty(), "\"" + category + "\" lists the todos " + listedTodoIds());
    }

    @Then("the response contains the todo {string} of another category")
    public void theResponseContainsTheTodoOfAnotherCategory(String todo) {
        assertTrue(listedTodoIds().contains(context.idOf("todos", todo)), "\"" + todo + "\" is not in the list");
    }

    // Delete a category

    @When("I delete category {string}")
    public void iDeleteCategory(String category) {
        context.setLastResponse(ApiClient.delete("/categories/" + context.idOf("categories", category)));
    }

    @When("I delete category {string} twice")
    public void iDeleteCategoryTwice(String category) {
        iDeleteCategory(category);
        iDeleteCategory(category);
    }

    private void createCategory(String title, String body) {
        ApiResponse response = ApiClient.post("/categories", body);
        context.setLastResponse(response);
        if (response.status() == HttpStatus.CREATED) {
            context.remember("categories", title, response.json().path("id").asText());
        }
    }

    /** The feature files name the invalid titles, since an empty or blank value cannot be read in a table. */
    private static String titleFor(String invalidTitle) {
        return switch (invalidTitle) {
            case "EMPTY" -> "";
            case "WHITESPACE" -> "   ";
            default -> throw new IllegalArgumentException("Unknown invalid title: " + invalidTitle);
        };
    }

    private boolean categoryWithTitleExists(String title) {
        ApiResponse response = ApiClient.get("/categories");
        assertEquals(HttpStatus.OK, response.status(), "could not read the categories");
        for (JsonNode category : response.json().path("categories")) {
            if (category.path("title").asText().equals(title)) {
                return true;
            }
        }
        return false;
    }

    private List<String> listedTodoIds() {
        return context.lastResponse().idsIn("todos");
    }
}
