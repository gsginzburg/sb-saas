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

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.gsginzburg.cluster.framework.autoconfigure.ClusterProperties;
import org.gsginzburg.cluster.framework.datasource.ShardInfo;
import org.gsginzburg.cluster.framework.datasource.TenantShardCache;
import org.gsginzburg.cluster.framework.liquibase.TenantSchemaManager;
import org.gsginzburg.shared.exception.TenantNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantMigrationService {

    private final TenantShardCache tenantShardCache;
    private final PgDumpService pgDumpService;
    private final ClusterProperties clusterProperties;
    private final TenantSchemaManager schemaManager;

    public void createTenant(String tenantId, String shardId) throws Exception {
        schemaManager.createTenantSchema(tenantId, shardId);
        ClusterProperties.ShardConfig shardConfig = clusterProperties.getShards().get(shardId);
        tenantShardCache.registerTenant(tenantId, ShardInfo.builder()
                .shardId(shardId)
                .jdbcUrl(shardConfig.getJdbcUrl())
                .schemaName(tenantId)
                .build());
    }

    public void deleteTenant(String tenantId) throws Exception {
        ShardInfo shardInfo = tenantShardCache.getShardForTenant(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        tenantShardCache.setTenantStatus(tenantId, false);
        schemaManager.dropTenantSchema(tenantId, shardInfo.getShardId());
        tenantShardCache.removeTenant(tenantId);
    }

    @Async
    public void moveTenant(String tenantId, String targetShardId) {
        log.info("Starting tenant migration: {} -> shard {}", tenantId, targetShardId);
        Path dumpFile = Path.of(System.getProperty("java.io.tmpdir"), tenantId + ".dump");
        try {
            ShardInfo sourceInfo = tenantShardCache.getShardForTenant(tenantId)
                    .orElseThrow(() -> new TenantNotFoundException(tenantId));
            ClusterProperties.ShardConfig sourceConfig = clusterProperties.getShards().get(sourceInfo.getShardId());
            ClusterProperties.ShardConfig targetConfig = clusterProperties.getShards().get(targetShardId);

            if (targetConfig == null) throw new IllegalArgumentException("Target shard not found: " + targetShardId);

            // 1. Mark inactive
            tenantShardCache.setTenantStatus(tenantId, false);

            // 2. Dump
            pgDumpService.dumpSchema(sourceConfig.getJdbcUrl(), sourceConfig.getUsername(),
                    sourceConfig.getPassword(), tenantId, dumpFile);

            // 3. Restore
            pgDumpService.restoreSchema(targetConfig.getJdbcUrl(), targetConfig.getUsername(),
                    targetConfig.getPassword(), dumpFile);

            // 4. Archive old schema
            try (Connection conn = DriverManager.getConnection(
                    sourceConfig.getJdbcUrl(), sourceConfig.getUsername(), sourceConfig.getPassword());
                 Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER SCHEMA \"" + tenantId + "\" RENAME TO \"archived-" + tenantId + "\"");
            }

            // 5. Update cache
            tenantShardCache.registerTenant(tenantId, ShardInfo.builder()
                    .shardId(targetShardId)
                    .jdbcUrl(targetConfig.getJdbcUrl())
                    .schemaName(tenantId)
                    .build());

            // 6. Re-activate
            tenantShardCache.setTenantStatus(tenantId, true);
            log.info("Tenant migration completed: {} -> shard {}", tenantId, targetShardId);

        } catch (Exception e) {
            log.error("Tenant migration failed for {}: {}", tenantId, e.getMessage(), e);
            tenantShardCache.setTenantStatus(tenantId, true); // re-activate on failure
        } finally {
            try { Files.deleteIfExists(dumpFile); } catch (Exception ignored) {}
        }
    }
}
