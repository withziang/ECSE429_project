package todos;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import common.ApiClient;
import common.ApiResponse;
import common.ServiceRunningCheck;
import common.SystemState;
import common.TestFixture;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.NodeList;

/** /todos: list, filter, HEAD and create. */
@ExtendWith(ServiceRunningCheck.class)
class TodosTest {

    private static final String GROCERIES =
            "{\"title\":\"Buy groceries\",\"doneStatus\":false,\"description\":\"milk, eggs, bread\"}";

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
    void getTodosReturnsSeededTodosAsJson() {
        ApiResponse response = ApiClient.get("/todos");

        assertEquals(200, response.status());
        assertEquals(Optional.of(ApiClient.JSON), response.header("Content-Type"));
        assertTrue(titlesOf(response.json().get("todos")).contains("scan paperwork"));
    }

    @Test
    void getTodosAsXmlReturnsTodoElements() {
        String id = fixture.create("todos", GROCERIES);

        ApiResponse response = ApiClient.send("GET", "/todos", null, ApiClient.JSON, ApiClient.XML);

        assertEquals(200, response.status());
        assertEquals(Optional.of(ApiClient.XML), response.header("Content-Type"));
        assertTrue(textsOf(response.xml().getElementsByTagName("id")).contains(id));
        assertTrue(textsOf(response.xml().getElementsByTagName("title")).contains("Buy groceries"));
    }

    @Test
    void filterByDoneStatusReturnsOnlyMatchingTodos() {
        String doneId = fixture.create("todos", "{\"title\":\"Pay rent\",\"doneStatus\":true}");
        String openId = fixture.create("todos", "{\"title\":\"Call dentist\",\"doneStatus\":false}");

        ApiResponse response = ApiClient.get("/todos?doneStatus=true");

        assertEquals(200, response.status());
        assertTrue(response.idsIn("todos").contains(doneId));
        assertFalse(response.idsIn("todos").contains(openId));
        response.json().get("todos").forEach(todo -> assertEquals("true", todo.get("doneStatus").asText()));
    }

    @Test
    void filterByTitleMatchesTheExactTitleOnly() {
        String id = fixture.create("todos", "{\"title\":\"Renew passport\"}");

        ApiResponse exactTitle = ApiClient.get("/todos?title=Renew%20passport");
        ApiResponse partialTitle = ApiClient.get("/todos?title=Renew");

        assertEquals(List.of(id), exactTitle.idsIn("todos"));
        assertFalse(partialTitle.idsIn("todos").contains(id));
    }

    @Test
    void headTodosReturnsHeadersWithoutBody() {
        ApiResponse response = ApiClient.head("/todos");

        assertEquals(200, response.status());
        assertEquals(Optional.of(ApiClient.JSON), response.header("Content-Type"));
        assertTrue(response.body().isEmpty());
    }

    @Test
    void postTodoCreatesTodoWithTheGivenFields() {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post("/todos", GROCERIES);
        String id = response.json().path("id").asText();
        fixture.recordCreated("todos", id);

        assertEquals(201, response.status());
        assertEquals(Optional.of("todos/" + id), response.header("Location"));
        JsonNode stored = ApiClient.get("/todos/" + id).firstIn("todos");
        assertEquals("Buy groceries", stored.get("title").asText());
        assertEquals("milk, eggs, bread", stored.get("description").asText());
        assertEquals("false", stored.get("doneStatus").asText());
        assertEquals(Set.of("todos/" + id), before.changedObjects(SystemState.capture()));
    }

    @Test
    void postTodoFromXmlCreatesTodo() {
        String xml = "<todo><title>Book flight</title><doneStatus>false</doneStatus>"
                + "<description>Montreal to Toronto</description></todo>";

        ApiResponse response = ApiClient.send("POST", "/todos", xml, ApiClient.XML, ApiClient.JSON);
        String id = response.json().path("id").asText();
        fixture.recordCreated("todos", id);

        assertEquals(201, response.status());
        JsonNode stored = ApiClient.get("/todos/" + id).firstIn("todos");
        assertEquals("Book flight", stored.get("title").asText());
        assertEquals("Montreal to Toronto", stored.get("description").asText());
    }

    @Test
    void postTodoWithoutTitleIsRejected() {
        assertCreationRejected("{\"description\":\"no title here\"}", "title : field is mandatory");
    }

    @Test
    void postTodoWithStringDoneStatusIsRejected() {
        assertCreationRejected("{\"title\":\"Pay rent\",\"doneStatus\":\"true\"}", "doneStatus should be BOOLEAN");
    }

    @Test
    void postTodoWithIdIsRejected() {
        assertCreationRejected("{\"id\":\"50\",\"title\":\"With id\"}", "Not allowed to create with id");
    }

    @Test
    void failedCreateStillUsesUpAnId() {
        int idBefore = Integer.parseInt(fixture.create("todos", "{\"title\":\"Water plants\"}"));
        ApiClient.post("/todos", "{\"title\":\"\"}");

        int idAfter = Integer.parseInt(fixture.create("todos", "{\"title\":\"Renew passport\"}"));

        assertEquals(idBefore + 2, idAfter);
    }

    private void assertCreationRejected(String body, String expectedError) {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post("/todos", body);

        assertEquals(400, response.status());
        assertTrue(response.body().contains(expectedError), response.body());
        assertEquals(Set.of(), before.changedObjects(SystemState.capture()));
    }

    private static List<String> titlesOf(JsonNode todos) {
        List<String> titles = new ArrayList<>();
        todos.forEach(todo -> titles.add(todo.get("title").asText()));
        return titles;
    }

    private static List<String> textsOf(NodeList elements) {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < elements.getLength(); i++) {
            texts.add(elements.item(i).getTextContent());
        }
        return texts;
    }
}
