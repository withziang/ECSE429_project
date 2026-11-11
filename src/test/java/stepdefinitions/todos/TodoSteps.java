package stepdefinitions.todos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import common.ApiClient;
import common.ApiResponse;
import common.HttpStatus;
import common.JsonBodies;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import stepdefinitions.common.ScenarioContext;

public class TodoSteps {

    private final ScenarioContext context;

    public TodoSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("I create a todo with title {string}")
    public void iCreateATodoWithTitle(String title) {
        createTodo(JsonBodies.object("title", title));
    }

    @When("I create a todo with title {string} and description {string}")
    public void iCreateATodoWithTitleAndDescription(String title, String description) {
        createTodo(JsonBodies.object("title", title, "description", description));
    }

    @Then("the todo {string} has description {string}")
    public void theTodoHasDescription(String title, String description) {
        assertEquals(description, todo(title).path("description").asText());
    }

    @Then("the todo {string} is not done")
    public void theTodoIsNotDone(String title) {
        assertEquals("false", todo(title).path("doneStatus").asText());
    }

    private void createTodo(String body) {
        ApiResponse response = ApiClient.post("/todos", body);
        context.setLastResponse(response);
        if (response.status() == HttpStatus.CREATED) {
            JsonNode todo = response.json();
            context.remember("todos", todo.path("title").asText(), todo.path("id").asText());
        }
    }

    private JsonNode todo(String title) {
        ApiResponse response = ApiClient.get("/todos/" + context.idOf("todos", title));
        assertEquals(HttpStatus.OK, response.status(), "could not read todo \"" + title + "\": " + response.body());
        return response.firstIn("todos");
    }
}
