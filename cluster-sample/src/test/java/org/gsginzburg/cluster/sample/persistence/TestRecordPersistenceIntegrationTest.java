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

package org.gsginzburg.cluster.sample.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.gsginzburg.cluster.framework.datasource.TenantContext;
import org.gsginzburg.cluster.framework.datasource.TenantContextHolder;
import org.gsginzburg.cluster.framework.datasource.TenantShardCache;
import org.gsginzburg.cluster.framework.management.TenantMigrationService;
import org.gsginzburg.cluster.sample.client.DispatchClientImpl;
import org.gsginzburg.cluster.sample.domain.dto.CreateTestRecordRequest;
import org.gsginzburg.cluster.sample.domain.dto.TestRecordDto;
import org.gsginzburg.cluster.sample.domain.model.TestRecord;
import org.gsginzburg.cluster.sample.domain.repository.TestRecordRepository;
import org.gsginzburg.cluster.sample.service.TestRecordService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestRecordPersistenceIntegrationTest.DbProvisioner.class)
class TestRecordPersistenceIntegrationTest {

    // ── DB lifecycle ──────────────────────────────────────────────────────────

    public static class DbProvisioner implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        private static final AtomicBoolean done = new AtomicBoolean(false);

        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            if (done.compareAndSet(false, true)) {
                runPsql("postgres", "CREATE USER \"fast-saas-spring-boot-test\" WITH PASSWORD 'fast-saas-spring-boot-test'");
                runPsql("postgres", "CREATE DATABASE \"fast-saas-spring-boot-test\" OWNER \"fast-saas-spring-boot-test\"");
                runPsql("postgres", "GRANT ALL PRIVILEGES ON DATABASE \"fast-saas-spring-boot-test\" TO \"fast-saas-spring-boot-test\"");
                Runtime.getRuntime().addShutdownHook(new Thread(DbProvisioner::teardown, "sample-db-teardown"));
            }
        }

        private static void teardown() {
            runPsql("postgres", "DROP DATABASE IF EXISTS \"fast-saas-spring-boot-test\" WITH (FORCE)");
            runPsql("postgres", "DROP USER IF EXISTS \"fast-saas-spring-boot-test\"");
        }

        private static void runPsql(String database, String sql) {
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
                System.err.println("[DbProvisioner] Failed: " + e.getMessage());
            }
        }
    }

    // ── Test constants ────────────────────────────────────────────────────────

    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/fast-saas-spring-boot-test";
    private static final String DB_USER  = "fast-saas-spring-boot-test";
    private static final String DB_PASS  = "fast-saas-spring-boot-test";

    // ── Injected beans ────────────────────────────────────────────────────────

    @MockitoBean SecurityFilterChain   securityFilterChain;
    @MockitoBean DispatchClientImpl    dispatchClientImpl;

    @Autowired TenantMigrationService tenantMigrationService;
    @Autowired TenantShardCache       tenantShardCache;
    @Autowired TestRecordRepository   testRecordRepository;
    @Autowired TestRecordService      testRecordService;

    // ── Per-test tenant ───────────────────────────────────────────────────────

    private String tenantId;

    @BeforeEach
    void createTenantSchemaAndSetContext() throws Exception {
        tenantId = UUID.randomUUID().toString();
        // Runs Liquibase changelog-instance.xml inside the new UUID schema
        tenantMigrationService.createTenant(tenantId, "shard-1");
        // Tell the routing datasource which schema to use for this thread
        TenantContextHolder.set(TenantContext.builder().tenantId(tenantId).build());
    }

    @AfterEach
    void dropTenantSchemaAndClearContext() {
        TenantContextHolder.clear();
        dropSchemaIfExists(tenantId);
        dropSchemaIfExists("archived-" + tenantId);
        tenantShardCache.removeTenant(tenantId);
    }

    // ── CRUD tests via TestRecordRepository ───────────────────────────────────

    @Test
    void save_persistsRecordAndCanBeLoadedById() {
        TestRecord saved = testRecordRepository.save(record("Alpha", "first record", 1));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        TestRecord loaded = testRecordRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getName()).isEqualTo("Alpha");
        assertThat(loaded.getDescription()).isEqualTo("first record");
        assertThat(loaded.getValue()).isEqualTo(1);
        assertThat(loaded.getCreatedAt()).isNotNull();
    }

    @Test
    void save_allFieldsWrittenToCorrectTenantSchemaInPostgres() throws Exception {
        TestRecord saved = testRecordRepository.save(record("BetaRecord", "db-verify", 99));

        // Verify the row is physically in the tenant's UUID-named schema
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT name, description, value FROM \"" + tenantId + "\".test WHERE id = '" + saved.getId() + "'");
            assertThat(rs.next()).as("Row must exist in schema %s", tenantId).isTrue();
            assertThat(rs.getString("name")).isEqualTo("BetaRecord");
            assertThat(rs.getString("description")).isEqualTo("db-verify");
            assertThat(rs.getInt("value")).isEqualTo(99);
        }
    }

    @Test
    void save_withNullOptionalFields_persistsSuccessfully() {
        TestRecord saved = testRecordRepository.save(record("NullFields", null, null));

        TestRecord loaded = testRecordRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getDescription()).isNull();
        assertThat(loaded.getValue()).isNull();
    }

    @Test
    void findAll_returnsAllSavedRecords() {
        testRecordRepository.save(record("R1", null, 10));
        testRecordRepository.save(record("R2", null, 20));
        testRecordRepository.save(record("R3", null, 30));

        List<TestRecord> all = testRecordRepository.findAll();

        assertThat(all).hasSize(3);
        assertThat(all).extracting(TestRecord::getName)
                .containsExactlyInAnyOrder("R1", "R2", "R3");
    }

    @Test
    void save_update_modifiesExistingRecordInDb() throws Exception {
        TestRecord saved = testRecordRepository.save(record("Original", "before", 1));

        saved.setName("Updated");
        saved.setDescription("after");
        saved.setValue(2);
        testRecordRepository.save(saved);

        TestRecord reloaded = testRecordRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Updated");
        assertThat(reloaded.getDescription()).isEqualTo("after");
        assertThat(reloaded.getValue()).isEqualTo(2);

        // Confirm via JDBC only one row, with updated values
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM \"" + tenantId + "\".test WHERE id = '" + saved.getId() + "'");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("name")).isEqualTo("Updated");
            assertThat(rs.next()).isFalse();  // only one row
        }
    }

    @Test
    void deleteById_removesRecordFromDb() throws Exception {
        TestRecord saved = testRecordRepository.save(record("ToDelete", null, 0));
        UUID id = saved.getId();

        testRecordRepository.deleteById(id);

        assertThat(testRecordRepository.findById(id)).isEmpty();

        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM \"" + tenantId + "\".test WHERE id = '" + id + "'");
            rs.next();
            assertThat(rs.getInt(1)).isZero();
        }
    }

    @Test
    void deleteAll_removesAllRecordsFromDb() throws Exception {
        testRecordRepository.save(record("D1", null, 1));
        testRecordRepository.save(record("D2", null, 2));

        testRecordRepository.deleteAll();

        assertThat(testRecordRepository.findAll()).isEmpty();

        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM \"" + tenantId + "\".test");
            rs.next();
            assertThat(rs.getInt(1)).isZero();
        }
    }

    @Test
    void findById_nonExistentId_returnsEmpty() {
        Optional<TestRecord> result = testRecordRepository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    void count_reflectsNumberOfSavedRecords() {
        assertThat(testRecordRepository.count()).isZero();

        testRecordRepository.save(record("C1", null, 1));
        testRecordRepository.save(record("C2", null, 2));

        assertThat(testRecordRepository.count()).isEqualTo(2);
    }

    @Test
    void data_isIsolatedToTenantSchema() throws Exception {
        testRecordRepository.save(record("Isolated", null, 7));

        // Row must NOT appear in any other schema (sanity check via information_schema)
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT table_schema FROM information_schema.tables " +
                    "WHERE table_name = 'test' AND table_schema = '" + tenantId + "'");
            assertThat(rs.next()).as("test table must be in schema %s", tenantId).isTrue();
        }
    }

    // ── CRUD tests via TestRecordService ─────────────────────────────────────

    @Test
    void service_create_persistsViaServiceAndVerifiableByRepository() {
        CreateTestRecordRequest req = new CreateTestRecordRequest();
        req.setName("ServiceRecord");
        req.setDescription("via service");
        req.setValue(55);

        TestRecordDto dto = testRecordService.create(req);

        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getName()).isEqualTo("ServiceRecord");
        assertThat(dto.getDescription()).isEqualTo("via service");
        assertThat(dto.getValue()).isEqualTo(55);

        // Verify via repository (separate transaction → real DB read, @CreationTimestamp populated)
        TestRecord stored = testRecordRepository.findById(UUID.fromString(dto.getId())).orElseThrow();
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getName()).isEqualTo("ServiceRecord");
    }

    @Test
    void service_getAll_returnsAllPersistedRecords() {
        testRecordService.create(req("SA", "a", 1));
        testRecordService.create(req("SB", "b", 2));

        List<TestRecordDto> all = testRecordService.getAll();

        assertThat(all).hasSize(2);
        assertThat(all).extracting(TestRecordDto::getName)
                .containsExactlyInAnyOrder("SA", "SB");
    }

    @Test
    void service_delete_removesRecordFromDb() throws Exception {
        TestRecordDto created = testRecordService.create(req("ToGo", null, 0));
        UUID id = UUID.fromString(created.getId());

        testRecordService.delete(id);

        assertThat(testRecordRepository.findById(id)).isEmpty();

        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM \"" + tenantId + "\".test WHERE id = '" + id + "'");
            rs.next();
            assertThat(rs.getInt(1)).isZero();
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static TestRecord record(String name, String description, Integer value) {
        return TestRecord.builder()
                .name(name)
                .description(description)
                .value(value)
                .build();
    }

    private static CreateTestRecordRequest req(String name, String description, Integer value) {
        CreateTestRecordRequest r = new CreateTestRecordRequest();
        r.setName(name);
        r.setDescription(description);
        r.setValue(value);
        return r;
    }

    private void dropSchemaIfExists(String schemaName) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
        } catch (Exception e) {
            System.err.println("[Test cleanup] Failed to drop schema " + schemaName + ": " + e.getMessage());
        }
    }
}
