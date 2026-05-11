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

package org.gsginzburg.cluster.framework.management;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.gsginzburg.cluster.framework.AbstractClusterIntegrationTest;
import org.gsginzburg.cluster.framework.datasource.ShardInfo;
import org.gsginzburg.cluster.framework.datasource.TenantShardCache;

import static org.assertj.core.api.Assertions.assertThat;

class TenantMigrationServiceIntegrationTest extends AbstractClusterIntegrationTest {

    @Autowired TenantMigrationService tenantMigrationService;
    @Autowired TenantShardCache tenantShardCache;

    private final List<String> createdTenants = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String tenantId : createdTenants) {
            dropSchemaIfExists(tenantId);
            dropSchemaIfExists("archived-" + tenantId);
            tenantShardCache.removeTenant(tenantId);
        }
        createdTenants.clear();
    }

    @Test
    void createTenant_schemaExistsInPostgres() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        createdTenants.add(tenantId);

        tenantMigrationService.createTenant(tenantId, "shard-1");

        try (Connection conn = DriverManager.getConnection(SHARD_JDBC_URL, SHARD_USER, SHARD_PASS)) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '" + tenantId + "'");
            assertThat(rs.next()).as("Schema %s must exist", tenantId).isTrue();
        }
    }

    @Test
    void createTenant_liquibaseMigrationsRun() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        createdTenants.add(tenantId);

        tenantMigrationService.createTenant(tenantId, "shard-1");

        try (Connection conn = DriverManager.getConnection(SHARD_JDBC_URL, SHARD_USER, SHARD_PASS)) {
            // databasechangelog written by Liquibase
            ResultSet changelog = conn.createStatement().executeQuery(
                    "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = '" + tenantId + "' AND table_name = 'databasechangelog'");
            assertThat(changelog.next()).as("databasechangelog must exist in schema %s", tenantId).isTrue();

            // 'test' table created by changelog-instance.xml
            ResultSet testTable = conn.createStatement().executeQuery(
                    "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = '" + tenantId + "' AND table_name = 'test'");
            assertThat(testTable.next()).as("'test' table must exist in schema %s", tenantId).isTrue();
        }
    }

    @Test
    void createTenant_registersTenantInShardCache() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        createdTenants.add(tenantId);

        tenantMigrationService.createTenant(tenantId, "shard-1");

        Optional<ShardInfo> info = tenantShardCache.getShardForTenant(tenantId);
        assertThat(info).isPresent();
        assertThat(info.get().getShardId()).isEqualTo("shard-1");
        assertThat(info.get().getSchemaName()).isEqualTo(tenantId);
        assertThat(info.get().getJdbcUrl()).contains("fast-saas-spring-boot-test");
    }

    @Test
    void createTenant_newTenantIsActiveByDefault() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        createdTenants.add(tenantId);

        tenantMigrationService.createTenant(tenantId, "shard-1");

        assertThat(tenantShardCache.isTenantActive(tenantId)).isTrue();
    }

    @Test
    void deleteTenant_archivesSchemaInPostgres() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        createdTenants.add(tenantId);
        tenantMigrationService.createTenant(tenantId, "shard-1");

        tenantMigrationService.deleteTenant(tenantId);

        try (Connection conn = DriverManager.getConnection(SHARD_JDBC_URL, SHARD_USER, SHARD_PASS)) {
            // Original schema must be gone
            ResultSet original = conn.createStatement().executeQuery(
                    "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '" + tenantId + "'");
            assertThat(original.next()).as("Original schema must no longer exist").isFalse();

            // Archived schema must be present
            String archivedName = "archived-" + tenantId;
            ResultSet archived = conn.createStatement().executeQuery(
                    "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '" + archivedName + "'");
            assertThat(archived.next()).as("Archived schema %s must exist", archivedName).isTrue();
        }
    }

    @Test
    void deleteTenant_removesTenantFromShardCache() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        createdTenants.add(tenantId);
        tenantMigrationService.createTenant(tenantId, "shard-1");

        tenantMigrationService.deleteTenant(tenantId);

        assertThat(tenantShardCache.getShardForTenant(tenantId)).isEmpty();
        assertThat(tenantShardCache.isTenantActive(tenantId)).isFalse();
    }
}
