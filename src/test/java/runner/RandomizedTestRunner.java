package runner;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;
import java.util.Random;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

/**
 * Runs all test classes and test methods in a pseudo-random order.
 * A new seed is drawn on every run; pass a seed as the first argument to repeat an earlier order.
 */
public class RandomizedTestRunner {

    private static final int MAX_STACK_TRACE_LINES = 10;

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : new Random().nextLong();

        System.out.println("Randomized JUnit 5 Test Suite");
        System.out.println("Random-order seed: " + seed);
        System.out.println("Starting tests...\n");

        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        Launcher launcher = LauncherFactory.create();
        launcher.execute(requestWithRandomOrder(seed), summaryListener, new ExecutionOrderPrinter());

        printSummary(summaryListener.getSummary(), seed);
    }

    private static LauncherDiscoveryRequest requestWithRandomOrder(long seed) {
        return LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        selectPackage("todos"),
                        selectPackage("projects"),
                        selectPackage("categories")
                )
                .configurationParameter("junit.jupiter.execution.order.random.seed", String.valueOf(seed))
                .configurationParameter("junit.jupiter.testmethod.order.default",
                        "org.junit.jupiter.api.MethodOrderer$Random")
                .configurationParameter("junit.jupiter.testclass.order.default",
                        "org.junit.jupiter.api.ClassOrderer$Random")
                .build();
    }

    private static void printSummary(TestExecutionSummary summary, long seed) {
        PrintWriter out = new PrintWriter(System.out);
        summary.printTo(out);
        summary.printFailuresTo(out, MAX_STACK_TRACE_LINES);
        out.println("Random-order seed: " + seed + " (pass it as the first argument to repeat this order)");
        out.flush();
    }

    /** Prints each test as it finishes, so the execution order is visible. */
    private static final class ExecutionOrderPrinter implements TestExecutionListener {

        private TestPlan testPlan;
        private int position;

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            this.testPlan = testPlan;
        }

        @Override
        public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
            if (identifier.isTest()) {
                position++;
                String testClass = testPlan.getParent(identifier).map(TestIdentifier::getDisplayName).orElse("");
                System.out.printf("%3d. [%s] %s > %s%n",
                        position, result.getStatus(), testClass, identifier.getDisplayName());
            }
        }
    }
}
