package stepdefinitions.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import common.ApiClient;
import common.ApiResponse;
import common.HttpStatus;
import common.JsonBodies;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import stepdefinitions.common.ScenarioContext;

public class ProjectSteps {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final ScenarioContext context;

    public ProjectSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("the task {string} has been removed from the project {string}")
    public void theTaskHasBeenRemovedFromTheProject(String todo, String project) {
        iRemoveTheTaskFromTheProject(todo, project);
        assertEquals(HttpStatus.OK, context.lastResponse().status(),
                "could not remove \"" + todo + "\" from \"" + project + "\"");
    }

    @When("I create a project titled {string} with description {string}, completed {string} and active {string}")
    public void iCreateAProjectWithAllFields(String title, String description, String completed, String active) {
        ObjectNode body = JSON_MAPPER.createObjectNode()
                .put("title", title)
                .put("description", description)
                .put("completed", Boolean.parseBoolean(completed))
                .put("active", Boolean.parseBoolean(active));
        createProject(body.toString(), ApiClient.JSON);
    }

    @When("I create a project from the {format} body {string}")
    public void iCreateAProjectFromTheBody(String format, String body) {
        createProject(body, format);
    }

    @When("I create a project titled {string} with {word} set to {string}")
    public void iCreateAProjectWithFlagSetTo(String title, String flag, String value) {
        createProject(JsonBodies.object("title", title, flag, value), ApiClient.JSON);
    }

    @When("I add the todo {string} to the project {string}")
    public void iAddTheTodoToTheProject(String todo, String project) {
        iAddTheTodoWithIdToTheProject(context.idOf("todos", todo), project);
    }

    @When("I add the todo with id {string} to the project {string}")
    public void iAddTheTodoWithIdToTheProject(String todoId, String project) {
        context.setLastResponse(ApiClient.post(tasksPath(project), JsonBodies.idReference(todoId)));
    }

    @When("I add a new task titled {string} to the project {string}")
    public void iAddANewTaskTitledToTheProject(String todo, String project) {
        ApiResponse response = ApiClient.post(tasksPath(project), JsonBodies.object("title", todo));
        context.setLastResponse(response);
        if (response.status() == HttpStatus.CREATED) {
            context.remember("todos", todo, response.json().path("id").asText());
        }
    }

    @When("I mark the project {string} as completed")
    public void iMarkTheProjectAsCompleted(String project) {
        iMarkTheProjectWithIdAsCompleted(context.idOf("projects", project));
    }

    @When("I mark the project with id {string} as completed")
    public void iMarkTheProjectWithIdAsCompleted(String id) {
        context.setLastResponse(ApiClient.post("/projects/" + id, "{\"completed\":true}"));
    }

    @When("I set the project {string} to completed {string} and active {string}")
    public void iSetTheProjectToCompletedAndActive(String project, String completed, String active) {
        ObjectNode body = JSON_MAPPER.createObjectNode()
                .put("completed", Boolean.parseBoolean(completed))
                .put("active", Boolean.parseBoolean(active));
        context.setLastResponse(ApiClient.post("/projects/" + context.idOf("projects", project), body.toString()));
    }

    @When("I view the tasks of the project {string} in {format}")
    public void iViewTheTasksOfTheProjectIn(String project, String format) {
        context.setLastResponse(ApiClient.send("GET", tasksPath(project), null, null, format));
    }

    @When("I view the tasks of the project with id {string}")
    public void iViewTheTasksOfTheProjectWithId(String id) {
        context.setLastResponse(ApiClient.get("/projects/" + id + "/tasks"));
    }

    @When("I remove the task {string} from the project {string}")
    public void iRemoveTheTaskFromTheProject(String todo, String project) {
        String path = tasksPath(project) + "/" + context.idOf("todos", todo);
        context.setLastResponse(ApiClient.delete(path));
    }

    @Then("the project {string} has description {string}")
    public void theProjectHasDescription(String project, String description) {
        assertEquals(description, project(context.idOf("projects", project)).path("description").asText());
    }

    @Then("the project {string} has completed {string} and active {string}")
    public void theProjectHasCompletedAndActive(String project, String completed, String active) {
        assertFlags(project(context.idOf("projects", project)), completed, active);
    }

    @Then("the created project has title {string}, completed {string} and active {string}")
    public void theCreatedProjectHas(String title, String completed, String active) {
        JsonNode project = project(context.lastResponse().json().path("id").asText());
        assertEquals(title, project.path("title").asText());
        assertFlags(project, completed, active);
    }

    @Then("no project is titled {string}")
    public void noProjectIsTitled(String title) {
        ApiClient.get("/projects").json().path("projects").forEach(project ->
                assertFalse(project.path("title").asText().equals(title),
                        "project " + project.path("id").asText() + " is titled \"" + title + "\""));
    }

    @Then("the project {string} has the task {string}")
    public void theProjectHasTheTask(String project, String todo) {
        List<String> tasks = taskIds(project);
        assertTrue(tasks.contains(context.idOf("todos", todo)), "\"" + project + "\" only has the tasks " + tasks);
    }

    @Then("the project {string} does not have the task {string}")
    public void theProjectDoesNotHaveTheTask(String project, String todo) {
        assertFalse(taskIds(project).contains(context.idOf("todos", todo)),
                "\"" + todo + "\" is still a task of \"" + project + "\"");
    }

    @Then("the todo {string} belongs to the project {string}")
    public void theTodoBelongsToTheProject(String todo, String project) {
        ApiResponse response = ApiClient.get("/todos/" + context.idOf("todos", todo));
        List<String> projects = new ArrayList<>();
        response.firstIn("todos").path("tasksof").forEach(link -> projects.add(link.path("id").asText()));
        assertTrue(projects.contains(context.idOf("projects", project)),
                "\"" + todo + "\" only belongs to the projects " + projects);
    }

    @Then("the todo {string} still exists")
    public void theTodoStillExists(String todo) {
        assertEquals(HttpStatus.OK, ApiClient.get("/todos/" + context.idOf("todos", todo)).status());
    }

    @Then("the XML task list includes the todo {string}")
    public void theXmlTaskListIncludesTheTodo(String todo) {
        List<String> ids = xmlTodoIds();
        assertTrue(ids.contains(context.idOf("todos", todo)), "\"" + todo + "\" is not in the XML list " + ids);
    }

    @Then("the XML task list does not include the todo {string}")
    public void theXmlTaskListDoesNotIncludeTheTodo(String todo) {
        assertFalse(xmlTodoIds().contains(context.idOf("todos", todo)), "\"" + todo + "\" is in the XML list");
    }

    private void createProject(String body, String contentType) {
        ApiResponse response = ApiClient.send("POST", "/projects", body, contentType, ApiClient.JSON);
        context.setLastResponse(response);
        if (response.status() == HttpStatus.CREATED) {
            JsonNode project = response.json();
            context.remember("projects", project.path("title").asText(), project.path("id").asText());
        }
    }

    private String tasksPath(String project) {
        return "/projects/" + context.idOf("projects", project) + "/tasks";
    }

    private JsonNode project(String id) {
        ApiResponse response = ApiClient.get("/projects/" + id);
        assertEquals(HttpStatus.OK, response.status(), "could not read project " + id + ": " + response.body());
        return response.firstIn("projects");
    }

    private static void assertFlags(JsonNode project, String completed, String active) {
        assertEquals(completed, project.path("completed").asText(), "completed");
        assertEquals(active, project.path("active").asText(), "active");
    }

    private List<String> taskIds(String project) {
        ApiResponse response = ApiClient.get(tasksPath(project));
        assertEquals(HttpStatus.OK, response.status(), "could not read the tasks of \"" + project + "\"");
        return response.idsIn("todos");
    }

    private List<String> xmlTodoIds() {
        List<String> ids = new ArrayList<>();
        NodeList todos = context.lastResponse().xml().getElementsByTagName("todo");
        for (int i = 0; i < todos.getLength(); i++) {
            NodeList fields = todos.item(i).getChildNodes();
            for (int j = 0; j < fields.getLength(); j++) {
                Node field = fields.item(j);
                if (field instanceof Element && field.getNodeName().equals("id")) {
                    ids.add(field.getTextContent());
                }
            }
        }
        return ids;
    }
}
