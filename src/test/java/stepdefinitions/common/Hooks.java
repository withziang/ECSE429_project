package stepdefinitions.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import common.ApiClient;
import common.ServiceRunningCheck;
import common.SystemState;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import java.util.Set;

/**
 * Runs around every scenario: fails it if the service is not running, saves the system state,
 * then deletes everything the scenario created and checks that the saved state is back.
 */
public class Hooks {

    private SystemState initialState;

    @Before(order = 0)
    public void checkServiceIsRunning() {
        ServiceRunningCheck.assertRunning();
    }

    @Before(order = 1)
    public void saveInitialState() {
        initialState = SystemState.capture();
    }

    @After
    public void restoreInitialState() {
        if (initialState == null) {
            return; // the service was not running, so nothing was saved or changed
        }
        initialState.addedObjects(SystemState.capture()).forEach(path -> ApiClient.delete("/" + path));
        assertEquals(Set.of(), initialState.changedObjects(SystemState.capture()),
                "the scenario did not restore the system state");
    }
}
