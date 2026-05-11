/*
 * Copyright 2026 Gary Ginzburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsginzburg.cluster.framework;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        classes = TestClusterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(initializers = AbstractClusterIntegrationTest.DbProvisioner.class)
public abstract class AbstractClusterIntegrationTest {

    @MockitoBean SecurityFilterChain securityFilterChain;

    protected static final String SHARD_JDBC_URL = "jdbc:postgresql://localhost:5432/fast-saas-spring-boot-test";
    protected static final String SHARD_USER     = "fast-saas-spring-boot-test";
    protected static final String SHARD_PASS     = "fast-saas-spring-boot-test";

    public static class DbProvisioner implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        private static final AtomicBoolean done = new AtomicBoolean(false);

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            if (done.compareAndSet(false, true)) {
                runPsql("postgres", "CREATE USER \"fast-saas-spring-boot-test\" WITH PASSWORD 'fast-saas-spring-boot-test'");
                runPsql("postgres", "CREATE DATABASE \"fast-saas-spring-boot-test\" OWNER \"fast-saas-spring-boot-test\"");
                runPsql("postgres", "GRANT ALL PRIVILEGES ON DATABASE \"fast-saas-spring-boot-test\" TO \"fast-saas-spring-boot-test\"");
                Runtime.getRuntime().addShutdownHook(new Thread(DbProvisioner::teardown, "cluster-db-teardown"));
            }
        }

        private static void teardown() {
            runPsql("postgres", "DROP DATABASE IF EXISTS \"fast-saas-spring-boot-test\" WITH (FORCE)");
            runPsql("postgres", "DROP USER IF EXISTS \"fast-saas-spring-boot-test\"");
        }

        static void runPsql(String database, String sql) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "sudo", "-n", "-u", "postgres", "psql", "-d", database, "-c", sql);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String output = new String(p.getInputStream().readAllBytes());
                int exit = p.waitFor();
                if (exit != 0) {
                    System.err.println("[DbProvisioner] psql exit=" + exit + ": " + output.trim());
                }
            } catch (Exception e) {
                System.err.println("[DbProvisioner] Failed to run: " + sql + " — " + e.getMessage());
            }
        }
    }

    /** Drop a schema by name (ignores errors, e.g. schema doesn't exist). */
    protected void dropSchemaIfExists(String schemaName) {
        try (Connection conn = DriverManager.getConnection(SHARD_JDBC_URL, SHARD_USER, SHARD_PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
        } catch (Exception e) {
            System.err.println("[Test cleanup] Failed to drop schema " + schemaName + ": " + e.getMessage());
        }
    }

    /** Drop all UUID-format schemas and archived-* schemas in the test DB. */
    protected void dropAllTestSchemas() {
        String sql = "SELECT schema_name FROM information_schema.schemata " +
                     "WHERE schema_name ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' " +
                     "   OR schema_name LIKE 'archived-%'";
        try (Connection conn = DriverManager.getConnection(SHARD_JDBC_URL, SHARD_USER, SHARD_PASS);
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString(1);
                try (Statement drop = conn.createStatement()) {
                    drop.execute("DROP SCHEMA IF EXISTS \"" + name + "\" CASCADE");
                }
            }
        } catch (Exception e) {
            System.err.println("[Test cleanup] Failed to drop test schemas: " + e.getMessage());
        }
    }
}
