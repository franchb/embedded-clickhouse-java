package io.github.franchb.clickhouse.embedded.junit5;

import io.github.franchb.clickhouse.embedded.Config;
import io.github.franchb.clickhouse.embedded.EmbeddedClickHouse;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension that starts an embedded ClickHouse server before all tests
 * in a class and stops it after all tests.
 * <p>
 * Can be used directly:
 * <pre>
 * &#64;ExtendWith(ClickHouseExtension.class)
 * class MyTest { ... }
 * </pre>
 * <p>
 * Or via the convenience annotation:
 * <pre>
 * &#64;EmbeddedClickHouseTest
 * class MyTest { ... }
 * </pre>
 * <p>
 * The extension resolves {@link EmbeddedClickHouse} parameters in test methods.
 */
public class ClickHouseExtension
        implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(ClickHouseExtension.class);

    private static final String SERVER_KEY = "server";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        EmbeddedClickHouse server = EmbeddedClickHouse.create(Config.defaultConfig());
        server.start();
        context.getStore(NAMESPACE).put(SERVER_KEY, server);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        EmbeddedClickHouse server = context.getStore(NAMESPACE)
                .remove(SERVER_KEY, EmbeddedClickHouse.class);
        if (server != null) {
            server.stop();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == EmbeddedClickHouse.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return extensionContext.getStore(NAMESPACE).get(SERVER_KEY, EmbeddedClickHouse.class);
    }
}
