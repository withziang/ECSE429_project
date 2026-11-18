package runner;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.launcher.EngineFilter.includeEngines;

import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

public class RandomizedStoryRunner {

    public static void main(String[] args) {
        long seed = RandomizedTestRunner.seedFrom(args);
        RandomizedTestRunner.run("Randomized Cucumber Story Test Suite", seed, LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("features"))
                .filters(includeEngines("cucumber"))
                .configurationParameter("cucumber.glue", "stepdefinitions")
                .configurationParameter("cucumber.publish.quiet", "true")
                .configurationParameter("cucumber.execution.order", "random")
                .configurationParameter("cucumber.execution.order.random.seed", String.valueOf(seed))
                .build());
    }
}
