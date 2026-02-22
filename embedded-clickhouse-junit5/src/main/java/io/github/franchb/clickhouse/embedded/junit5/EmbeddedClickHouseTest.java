package io.github.franchb.clickhouse.embedded.junit5;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that starts an embedded ClickHouse server for the test class.
 * <p>
 * Usage:
 * <pre>
 * &#64;EmbeddedClickHouseTest
 * class MyTest {
 *     &#64;Test
 *     void test(EmbeddedClickHouse server) {
 *         String jdbcUrl = server.jdbcUrl();
 *         // ...
 *     }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ClickHouseExtension.class)
public @interface EmbeddedClickHouseTest {
}
