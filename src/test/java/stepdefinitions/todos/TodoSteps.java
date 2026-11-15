package stepdefinitions.todos;

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

    @Given("the todo {string} is a task of the project {string}")
    public void theTodoIsATaskOfTheProject(String todo, String project) {
        String path = "/projects/" + context.idOf("projects", project) + "/tasks";
        ApiResponse response = ApiClient.post(path, JsonBodies.idReference(context.idOf("todos", todo)));
        assertEquals(HttpStatus.CREATED, response.status(), "could not add \"" + todo + "\" to \"" + project + "\"");
    }

    @Given("the todo {string} has been deleted")
    public void theTodoHasBeenDeleted(String title) {
        iDeleteTheTodo(title);
        assertEquals(HttpStatus.OK, context.lastResponse().status(), "could not delete todo \"" + title + "\"");
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

    @When("I list the todos with done status {string}")
    public void iListTheTodosWithDoneStatus(String doneStatus) {
        context.setLastResponse(ApiClient.get("/todos?doneStatus=" + doneStatus));
    }

    @When("I look up the todo {string} in {format}")
    public void iLookUpTheTodoIn(String title, String format) {
        String path = "/todos/" + context.idOf("todos", title);
        context.setLastResponse(ApiClient.send("GET", path, null, null, format));
    }

    @When("I look up the todo with id {string}")
    public void iLookUpTheTodoWithId(String id) {
        context.setLastResponse(ApiClient.get("/todos/" + id));
    }

    @When("I delete the todo {string}")
    public void iDeleteTheTodo(String title) {
        context.setLastResponse(ApiClient.delete("/todos/" + context.idOf("todos", title)));
    }

    @Then("the list includes the todo {string}")
    public void theListIncludesTheTodo(String title) {
        assertTrue(listedIds().contains(context.idOf("todos", title)), "\"" + title + "\" is not in the list");
    }

    @Then("the list does not include the todo {string}")
    public void theListDoesNotIncludeTheTodo(String title) {
        assertFalse(listedIds().contains(context.idOf("todos", title)), "\"" + title + "\" is in the list");
    }

    @Then("the {format} response shows the todo {string}")
    public void theResponseShowsTheTodo(String format, String title) {
        ApiResponse response = context.lastResponse();
        String shownTitle = format.equals(ApiClient.XML)
                ? response.xml().getElementsByTagName("title").item(0).getTextContent()
                : response.firstIn("todos").path("title").asText();
        assertEquals(title, shownTitle);
    }

    @Then("the todo {string} no longer exists")
    public void theTodoNoLongerExists(String title) {
        assertEquals(HttpStatus.NOT_FOUND, ApiClient.get("/todos/" + context.idOf("todos", title)).status());
    }

    @Then("the project {string} has no tasks")
    public void theProjectHasNoTasks(String project) {
        ApiResponse response = ApiClient.get("/projects/" + context.idOf("projects", project) + "/tasks");
        assertEquals(List.of(), response.idsIn("todos"));
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

    private List<String> listedIds() {
        return context.lastResponse().idsIn("todos");
    }

    private List<String> categoryTitles(String todo) {
        ApiResponse response = ApiClient.get("/todos/" + context.idOf("todos", todo) + "/categories");
        assertEquals(HttpStatus.OK, response.status(), "could not read the categories of \"" + todo + "\"");
        List<String> titles = new ArrayList<>();
        response.json().path("categories").forEach(category -> titles.add(category.path("title").asText()));
        return titles;
    }
}
