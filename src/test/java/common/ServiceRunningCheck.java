package common;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.UncheckedIOException;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ServiceRunningCheck implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        try {
            ApiClient.get("/todos");
        } catch (UncheckedIOException e) {
            fail("Todo Manager is not running on " + ApiClient.BASE_URL + "; start it with src/run.sh", e);
        }
    }
}
