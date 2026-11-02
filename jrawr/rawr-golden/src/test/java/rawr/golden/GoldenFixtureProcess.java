package rawr.golden;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.io.PrintWriter;

/**
 * One fresh JVM for one golden/optimizer/relative-stats fixture. The Gradle
 * {@code goldenAllParallel} task launches this class per fixture so the fixtures can run
 * concurrently without sharing the model's static/carry-over state.
 */
public final class GoldenFixtureProcess {
    private GoldenFixtureProcess() {
    }

    public static void main(String[] args) {
        if (args.length != 1 || args[0].isBlank()) {
            System.err.println("Usage: GoldenFixtureProcess <junit-tag>");
            System.exit(2);
        }
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage("rawr.golden"))
                .filters(TagFilter.includeTags(args[0]))
                .build();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        PrintWriter out = new PrintWriter(System.out, true);
        listener.getSummary().printTo(out);
        if (listener.getSummary().getTotalFailureCount() > 0) {
            listener.getSummary().printFailuresTo(out);
            out.flush();
            System.exit(1);
        }
        if (listener.getSummary().getTestsStartedCount() == 0) {
            System.err.println("No tests matched tag " + args[0] + ".");
            System.exit(2);
        }
        out.flush();
    }
}
