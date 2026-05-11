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

package org.gsginzburg.cluster.framework.liquibase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import org.gsginzburg.cluster.framework.autoconfigure.ClusterProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaManager {

    private final ClusterProperties clusterProperties;

    private static final String INSTANCE_CHANGELOG = "db/changelog/changelog-instance.xml";

    public void createTenantSchema(String tenantId, String shardId) throws Exception {
        ClusterProperties.ShardConfig shardConfig = clusterProperties.getShards().get(shardId);
        if (shardConfig == null) throw new IllegalArgumentException("Shard not found: " + shardId);

        try (Connection conn = DriverManager.getConnection(
                shardConfig.getJdbcUrl(), shardConfig.getUsername(), shardConfig.getPassword())) {

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE SCHEMA IF NOT EXISTS \"" + tenantId + "\"");
                stmt.execute("SET search_path TO \"" + tenantId + "\"");
            }

            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
            database.setDefaultSchemaName(tenantId);

            try (var liquibase = new Liquibase(INSTANCE_CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
                liquibase.update();
            }
        }
        log.info("Created tenant schema {} on shard {}", tenantId, shardId);
    }

    public void dropTenantSchema(String tenantId, String shardId) throws Exception {
        ClusterProperties.ShardConfig shardConfig = clusterProperties.getShards().get(shardId);
        if (shardConfig == null) throw new IllegalArgumentException("Shard not found: " + shardId);

        try (Connection conn = DriverManager.getConnection(
                shardConfig.getJdbcUrl(), shardConfig.getUsername(), shardConfig.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER SCHEMA \"" + tenantId + "\" RENAME TO \"archived-" + tenantId + "\"");
        }
        log.info("Archived tenant schema {} on shard {}", tenantId, shardId);
    }
}
