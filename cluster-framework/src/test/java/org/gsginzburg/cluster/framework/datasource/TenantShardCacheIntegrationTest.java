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

package org.gsginzburg.cluster.framework.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.gsginzburg.cluster.framework.AbstractClusterIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class TenantShardCacheIntegrationTest extends AbstractClusterIntegrationTest {

    @Autowired TenantShardCache tenantShardCache;

    private final List<String> schemasToCleanup = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String schema : schemasToCleanup) {
            dropSchemaIfExists(schema);
            tenantShardCache.removeTenant(schema);
        }
        schemasToCleanup.clear();
        // Force refresh so residual schemas from other tests don't bleed into this one
        tenantShardCache.refresh();
    }

    @Test
    void refresh_discoversUuidSchemaWithChangelogTable() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        schemasToCleanup.add(tenantId);
        createMinimalTenantSchema(tenantId);

        tenantShardCache.refresh();

        Optional<ShardInfo> info = tenantShardCache.getShardForTenant(tenantId);
        assertThat(info).as("Cache should discover tenant %s after refresh", tenantId).isPresent();
        assertThat(info.get().getShardId()).isEqualTo("shard-1");
        assertThat(info.get().getSchemaName()).isEqualTo(tenantId);
    }

    @Test
    void refresh_ignoresSchemaWithoutChangelogTable() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        schemasToCleanup.add(tenantId);

        // Create a UUID-named schema but WITHOUT a databasechangelog table
        try (Connection conn = DriverManager.getConnection(SHARD_JDBC_URL, SHARD_USER, SHARD_PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS \"" + tenantId + "\"");
        }

        tenantShardCache.refresh();

        assertThat(tenantShardCache.getShardForTenant(tenantId)).isEmpty();
    }

    @Test
    void refresh_ignoresNonUuidSchemas() throws Exception {
        String schemaName = "not-a-uuid";
        schemasToCleanup.add(schemaName);

        try (Connection conn = DriverManager.getConnection(SHARD_JDBC_URL, SHARD_USER, SHARD_PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");
            stmt.execute("CREATE TABLE \"" + schemaName + "\".databasechangelog (id VARCHAR(255))");
        }

        tenantShardCache.refresh();

        assertThat(tenantShardCache.getShardForTenant(schemaName)).isEmpty();
    }

    @Test
    void registerTenant_addsTenantToCache() {
        String tenantId = UUID.randomUUID().toString();
        schemasToCleanup.add(tenantId);

        ShardInfo info = ShardInfo.builder()
                .shardId("shard-1")
                .jdbcUrl(SHARD_JDBC_URL)
                .schemaName(tenantId)
                .build();

        tenantShardCache.registerTenant(tenantId, info);

        assertThat(tenantShardCache.getShardForTenant(tenantId)).isPresent();
        assertThat(tenantShardCache.isTenantActive(tenantId)).isTrue();
    }

    @Test
    void getShardForTenant_returnsCorrectShardInfo() {
        String tenantId = UUID.randomUUID().toString();
        schemasToCleanup.add(tenantId);

        ShardInfo registered = ShardInfo.builder()
                .shardId("shard-1")
                .jdbcUrl(SHARD_JDBC_URL)
                .schemaName(tenantId)
                .build();
        tenantShardCache.registerTenant(tenantId, registered);

        ShardInfo fetched = tenantShardCache.getShardForTenant(tenantId).orElseThrow();
        assertThat(fetched.getShardId()).isEqualTo("shard-1");
        assertThat(fetched.getJdbcUrl()).isEqualTo(SHARD_JDBC_URL);
        assertThat(fetched.getSchemaName()).isEqualTo(tenantId);
    }

    @Test
    void removeTenant_removesFromCacheAndStatus() {
        String tenantId = UUID.randomUUID().toString();
        schemasToCleanup.add(tenantId);
        tenantShardCache.registerTenant(tenantId, ShardInfo.builder()
                .shardId("shard-1").jdbcUrl(SHARD_JDBC_URL).schemaName(tenantId).build());

        tenantShardCache.removeTenant(tenantId);

        assertThat(tenantShardCache.getShardForTenant(tenantId)).isEmpty();
        assertThat(tenantShardCache.isTenantActive(tenantId)).isFalse();
    }

    @Test
    void setTenantStatus_updatesActiveFlag() {
        String tenantId = UUID.randomUUID().toString();
        schemasToCleanup.add(tenantId);
        tenantShardCache.registerTenant(tenantId, ShardInfo.builder()
                .shardId("shard-1").jdbcUrl(SHARD_JDBC_URL).schemaName(tenantId).build());

        assertThat(tenantShardCache.isTenantActive(tenantId)).isTrue();

        tenantShardCache.setTenantStatus(tenantId, false);
        assertThat(tenantShardCache.isTenantActive(tenantId)).isFalse();

        tenantShardCache.setTenantStatus(tenantId, true);
        assertThat(tenantShardCache.isTenantActive(tenantId)).isTrue();
    }

    @Test
    void getAllTenants_returnsUnmodifiableSnapshot() {
        String t1 = UUID.randomUUID().toString();
        String t2 = UUID.randomUUID().toString();
        schemasToCleanup.add(t1);
        schemasToCleanup.add(t2);

        tenantShardCache.registerTenant(t1, ShardInfo.builder()
                .shardId("shard-1").jdbcUrl(SHARD_JDBC_URL).schemaName(t1).build());
        tenantShardCache.registerTenant(t2, ShardInfo.builder()
                .shardId("shard-1").jdbcUrl(SHARD_JDBC_URL).schemaName(t2).build());

        Map<String, ShardInfo> all = tenantShardCache.getAllTenants();
        assertThat(all).containsKeys(t1, t2);
    }

    // --- helpers ---

    /** Creates a UUID-named schema with a minimal databasechangelog table, as scanShard expects. */
    private void createMinimalTenantSchema(String tenantId) throws Exception {
        try (Connection conn = DriverManager.getConnection(SHARD_JDBC_URL, SHARD_USER, SHARD_PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS \"" + tenantId + "\"");
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS \"" + tenantId + "\".databasechangelog (" +
                "  id VARCHAR(255) NOT NULL," +
                "  author VARCHAR(255) NOT NULL," +
                "  filename VARCHAR(255) NOT NULL," +
                "  dateexecuted TIMESTAMPTZ NOT NULL DEFAULT now()," +
                "  orderexecuted INT NOT NULL," +
                "  exectype VARCHAR(10) NOT NULL," +
                "  md5sum VARCHAR(35)," +
                "  description VARCHAR(255)," +
                "  comments VARCHAR(255)," +
                "  tag VARCHAR(255)," +
                "  liquibase VARCHAR(20)," +
                "  contexts VARCHAR(255)," +
                "  labels VARCHAR(255)," +
                "  deployment_id VARCHAR(10)" +
                ")");
        }
    }
}
