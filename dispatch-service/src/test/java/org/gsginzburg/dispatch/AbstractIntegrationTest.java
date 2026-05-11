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

package org.gsginzburg.dispatch;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(initializers = AbstractIntegrationTest.DbProvisioner.class)
public abstract class AbstractIntegrationTest {

    public static class DbProvisioner implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        private static final AtomicBoolean done = new AtomicBoolean(false);

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            if (done.compareAndSet(false, true)) {
                runPsql("postgres", "CREATE USER \"fast-saas-spring-boot-test\" WITH PASSWORD 'fast-saas-spring-boot-test'");
                runPsql("postgres", "CREATE DATABASE \"fast-saas-spring-boot-test\" OWNER \"fast-saas-spring-boot-test\"");
                runPsql("postgres", "GRANT ALL PRIVILEGES ON DATABASE \"fast-saas-spring-boot-test\" TO \"fast-saas-spring-boot-test\"");
                runDispatchMigrations();
                Runtime.getRuntime().addShutdownHook(new Thread(DbProvisioner::teardown, "db-teardown"));
            }
        }

        private static void runDispatchMigrations() {
            try (Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/fast-saas-spring-boot-test", "fast-saas-spring-boot-test", "fast-saas-spring-boot-test")) {
                conn.createStatement().execute("CREATE SCHEMA IF NOT EXISTS dispatch");
                Database database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(conn));
                database.setDefaultSchemaName("dispatch");
                database.setLiquibaseSchemaName("dispatch");
                try (Liquibase liquibase = new Liquibase(
                        "db/changelog/dispatch-changelog-master.xml",
                        new ClassLoaderResourceAccessor(), database)) {
                    liquibase.update("test");
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to run dispatch Liquibase migrations", e);
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
}
