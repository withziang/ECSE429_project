package stepdefinitions.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.ApiClient;
import common.ApiResponse;
import common.HttpStatus;
import common.JsonBodies;
import common.ServiceRunningCheck;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

/** Steps that every feature can use, whatever resource it is about. */
public class CommonSteps {

    private final ScenarioContext context;

    public CommonSteps(ScenarioContext context) {
        this.context = context;
    }

    /** Lets steps say "todo", "project" or "category"; the value is the API path, e.g. "categories". */
    @ParameterType("todo|project|category")
    public String resource(String name) {
        return name.equals("category") ? "categories" : name + "s";
    }

    @Given("the Todo Manager is running")
    public void theTodoManagerIsRunning() {
        ServiceRunningCheck.assertRunning();
    }

    @Given("a {resource} titled {string} exists")
    public void aResourceTitledExists(String resource, String title) {
        ApiResponse response = ApiClient.post("/" + resource, JsonBodies.object("title", title));
        assertEquals(HttpStatus.CREATED, response.status(), "could not create " + resource + ": " + response.body());
        context.remember(resource, title, response.json().path("id").asText());
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        ApiResponse response = context.lastResponse();
        assertEquals(status, response.status(), "unexpected status, body: " + response.body());
    }

    @Then("the error message contains {string}")
    public void theErrorMessageContains(String message) {
        String errors = context.lastResponse().json().path("errorMessages").toString();
        assertTrue(errors.contains(message), "error messages " + errors + " do not contain \"" + message + "\"");
    }

    @Then("no {resource} has the description {string}")
    public void noResourceHasTheDescription(String resource, String description) {
        ApiClient.get("/" + resource).json().path(resource).forEach(object ->
                assertFalse(object.path("description").asText().equals(description),
                        resource + " " + object.path("id").asText() + " has the description \"" + description + "\""));
    }
}
