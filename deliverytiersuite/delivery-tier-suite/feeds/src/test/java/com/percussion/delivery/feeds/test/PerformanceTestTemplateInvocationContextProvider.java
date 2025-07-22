// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.junit.jupiter.api.extension.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * Custom test invocation context provider for performance tests.
 * Provides different performance test scenarios.
 */
public class PerformanceTestTemplateInvocationContextProvider
        implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext context) {
        return Stream.of(
            // Light load scenario
            createContext("Light Load", 1, 100, 500),
            // Medium load scenario
            createContext("Medium Load", 5, 200, 1000),
            // Heavy load scenario
            createContext("Heavy Load", 10, 500, 2000)
        );
    }

    private TestTemplateInvocationContext createContext(
            String displayName,
            int concurrentUsers,
            int operations,
            long maxResponseTime) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return displayName;
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return List.of(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(ParameterContext parameterContext,
                            ExtensionContext extensionContext) {
                        return parameterContext.getParameter().getType()
                            .equals(PerformanceScenario.class);
                    }

                    @Override
                    public Object resolveParameter(ParameterContext parameterContext,
                            ExtensionContext extensionContext) {
                        return new PerformanceScenario(
                            displayName,
                            concurrentUsers,
                            operations,
                            maxResponseTime
                        );
                    }
                });
            }
        };
    }

    /**
     * Represents a performance test scenario.
     */
    public record PerformanceScenario(
        String name,
        int concurrentUsers,
        int operations,
        long maxResponseTimeMs
    ) {}
}
