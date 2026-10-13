package common;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Saves the system state before a test, creates and tracks the test's own data,
 * then deletes that data and checks that the saved state is back.
 */
public class TestFixture {

    private final Deque<String> createdObjects = new ArrayDeque<>();
    private SystemState savedState;

    public void saveState() {
        savedState = SystemState.capture();
    }

    public String create(String resource, String body) {
        ApiResponse response = ApiClient.post("/" + resource, body);
        assertEquals(201, response.status(), "could not create test data in " + resource + ": " + response.body());
        String id = response.json().path("id").asText();
        recordCreated(resource, id);
        return id;
    }

    /** Registers an object the test created some other way, so it is deleted afterwards. */
    public void recordCreated(String resource, String id) {
        createdObjects.push(resource + "/" + id);
    }

    public void link(String relationshipPath, String targetId) {
        ApiResponse response = ApiClient.post(relationshipPath, JsonBodies.idReference(targetId));
        assertEquals(201, response.status(), "could not create link " + relationshipPath + ": " + response.body());
    }

    public void restoreState() {
        createdObjects.forEach(path -> ApiClient.delete("/" + path));
        createdObjects.clear();
        if (savedState != null) {
            assertEquals(Set.of(), savedState.changedObjects(SystemState.capture()),
                    "the test did not restore the system state");
        }
    }
}
