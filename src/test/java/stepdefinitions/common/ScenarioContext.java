package stepdefinitions.common;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import common.ApiResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Data shared by the step definitions of one scenario: the last response and the ids of the objects
 * the scenario created, looked up by title. A new instance is created for every scenario.
 */
public class ScenarioContext {

    private final Map<String, String> idsByTitle = new HashMap<>();
    private ApiResponse lastResponse;

    public void remember(String resource, String title, String id) {
        idsByTitle.put(resource + "/" + title, id);
    }

    public String idOf(String resource, String title) {
        String id = idsByTitle.get(resource + "/" + title);
        assertNotNull(id, "no " + resource + " titled \"" + title + "\" was created in this scenario");
        return id;
    }

    public ApiResponse lastResponse() {
        assertNotNull(lastResponse, "no request was sent in this scenario");
        return lastResponse;
    }

    public void setLastResponse(ApiResponse response) {
        lastResponse = response;
    }
}
