package todos;

import static org.junit.jupiter.api.Assertions.*;

import common.ApiClient;
import common.ApiResponse;
import common.ServiceRunningCheck;
import common.SystemState;
import common.TestFixture;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** POST /todos with a malformed JSON payload. */
@ExtendWith(ServiceRunningCheck.class)
class TodosMalformedJsonTest {

    private static final String UNTERMINATED_STRING = "{\"title\":\"Broken";

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
    void malformedJsonIsRejectedWithoutCreatingATodo() {
        SystemState before = SystemState.capture();

        ApiResponse response = ApiClient.post("/todos", UNTERMINATED_STRING);

        assertEquals(400, response.status());
        assertTrue(response.json().has("errorMessages"), response.body());
        assertEquals(Set.of(), before.changedObjects(SystemState.capture()));
    }

    @Test
    @DisplayName("[Expected, fails] the error for malformed JSON is labelled as JSON")
    void malformedJsonErrorShouldBeLabelledAsJson() {
        ApiResponse response = ApiClient.post("/todos", UNTERMINATED_STRING);

        assertEquals(Optional.of(ApiClient.JSON), response.header("Content-Type"));
    }

    @Test
    @DisplayName("[Actual] the error for malformed JSON is labelled text/html")
    void malformedJsonErrorIsLabelledAsHtml() {
        ApiResponse response = ApiClient.post("/todos", UNTERMINATED_STRING);

        assertEquals(Optional.of("text/html;charset=utf-8"), response.header("Content-Type"));
    }
}
