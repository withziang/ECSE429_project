package runner;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

public class RandomizedTestRunner {

    public static void main(String[] args) {
        long seed = 32L;

        System.out.println("Randomized JUnit 5 Test Suite");
        System.out.println("Random-order seed: " + seed);
        System.out.println("Starting tests...\n");

        LauncherDiscoveryRequest request =
            LauncherDiscoveryRequestBuilder.request()
                .selectors(
                    selectPackage("todos"),
                    selectPackage("projects"),
                    selectPackage("categories")
                )
                .configurationParameter(
                    "junit.jupiter.execution.order.random.seed",
                    String.valueOf(seed)
                )
                .configurationParameter(
                    "junit.jupiter.testmethod.order.default",
                    "org.junit.jupiter.api.MethodOrderer$Random"
                )
                .configurationParameter(
                    "junit.jupiter.testclass.order.default",
                    "org.junit.jupiter.api.ClassOrderer$Random"
                )
                .build();

        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));

        if (listener.getSummary().getTotalFailureCount() > 0) {
            System.out.println("\nSome tests failed.");
        }
    }
}