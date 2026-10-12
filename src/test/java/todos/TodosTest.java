package todos;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import common.ApiClient;
import common.ApiResponse;
import common.ServiceRunningCheck;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ServiceRunningCheck.class)
class TodosTest {

    @Test
    void getTodosReturnsSeededTodosAsJson() {
        ApiResponse response = ApiClient.get("/todos");

        assertEquals(200, response.status());
        assertEquals(Optional.of(ApiClient.JSON), response.header("Content-Type"));
        assertTrue(titlesOf(response.json().get("todos")).contains("scan paperwork"));
    }

    private static List<String> titlesOf(JsonNode todos) {
        List<String> titles = new ArrayList<>();
        todos.forEach(todo -> titles.add(todo.get("title").asText()));
        return titles;
    }
}
