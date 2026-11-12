package stepdefinitions.todos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import common.ApiClient;
import common.ApiResponse;
import common.HttpStatus;
import common.JsonBodies;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.ArrayList;
import java.util.List;
import stepdefinitions.common.ScenarioContext;

public class TodoSteps {

    private static final String DONE = "{\"doneStatus\":true}";

    private final ScenarioContext context;

    public TodoSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("the todo {string} is marked as done")
    public void theTodoIsMarkedAsDone(String title) {
        iMarkTheTodoAsDone(title);
        assertEquals(HttpStatus.OK, context.lastResponse().status(), "could not mark todo \"" + title + "\" as done");
    }

    @When("I create a todo with title {string}")
    public void iCreateATodoWithTitle(String title) {
        createTodo(JsonBodies.object("title", title));
    }

    @When("I create a todo with title {string} and description {string}")
    public void iCreateATodoWithTitleAndDescription(String title, String description) {
        createTodo(JsonBodies.object("title", title, "description", description));
    }

    @When("I mark the todo {string} as done")
    public void iMarkTheTodoAsDone(String title) {
        iMarkTheTodoWithIdAsDone(context.idOf("todos", title));
    }

    @When("I mark the todo with id {string} as done")
    public void iMarkTheTodoWithIdAsDone(String id) {
        context.setLastResponse(ApiClient.post("/todos/" + id, DONE));
    }

    @When("I add the todo {string} to the category {string}")
    public void iAddTheTodoToTheCategory(String todo, String category) {
        iAddTheTodoToTheCategoryWithId(todo, context.idOf("categories", category));
    }

    @When("I add the todo {string} to the category with id {string}")
    public void iAddTheTodoToTheCategoryWithId(String todo, String categoryId) {
        addToCategory(todo, JsonBodies.idReference(categoryId));
    }

    @When("I add the todo {string} to a new category titled {string}")
    public void iAddTheTodoToANewCategoryTitled(String todo, String category) {
        addToCategory(todo, JsonBodies.object("title", category));
    }

    @Then("the todo {string} has description {string}")
    public void theTodoHasDescription(String title, String description) {
        assertEquals(description, todo(title).path("description").asText());
    }

    @Then("the todo {string} is done")
    public void theTodoIsDone(String title) {
        assertEquals("true", todo(title).path("doneStatus").asText());
    }

    @Then("the todo {string} is not done")
    public void theTodoIsNotDone(String title) {
        assertEquals("false", todo(title).path("doneStatus").asText());
    }

    @Then("the todo {string} is in the category {string}")
    public void theTodoIsInTheCategory(String todo, String category) {
        List<String> categories = categoryTitles(todo);
        assertTrue(categories.contains(category), "todo \"" + todo + "\" is only in the categories " + categories);
    }

    @Then("the todo {string} has no categories")
    public void theTodoHasNoCategories(String todo) {
        assertEquals(List.of(), categoryTitles(todo));
    }

    private void createTodo(String body) {
        ApiResponse response = ApiClient.post("/todos", body);
        context.setLastResponse(response);
        if (response.status() == HttpStatus.CREATED) {
            JsonNode todo = response.json();
            context.remember("todos", todo.path("title").asText(), todo.path("id").asText());
        }
    }

    private void addToCategory(String todo, String body) {
        context.setLastResponse(ApiClient.post("/todos/" + context.idOf("todos", todo) + "/categories", body));
    }

    private JsonNode todo(String title) {
        ApiResponse response = ApiClient.get("/todos/" + context.idOf("todos", title));
        assertEquals(HttpStatus.OK, response.status(), "could not read todo \"" + title + "\": " + response.body());
        return response.firstIn("todos");
    }

    private List<String> categoryTitles(String todo) {
        ApiResponse response = ApiClient.get("/todos/" + context.idOf("todos", todo) + "/categories");
        assertEquals(HttpStatus.OK, response.status(), "could not read the categories of \"" + todo + "\"");
        List<String> titles = new ArrayList<>();
        response.json().path("categories").forEach(category -> titles.add(category.path("title").asText()));
        return titles;
    }
}
