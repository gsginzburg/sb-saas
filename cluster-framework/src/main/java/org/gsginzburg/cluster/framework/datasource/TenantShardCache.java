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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.gsginzburg.cluster.framework.autoconfigure.ClusterProperties;
import org.gsginzburg.shared.util.UuidUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantShardCache {

    private final ClusterProperties clusterProperties;

    // tenantId -> ShardInfo
    private volatile Map<String, ShardInfo> tenantShardMap = new ConcurrentHashMap<>();
    // tenantId -> true=active, false=inactive
    private volatile Map<String, Boolean> tenantStatusMap = new ConcurrentHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @PostConstruct
    public void initialize() {
        refresh();
    }

    @Scheduled(fixedDelay = 600_000, initialDelay = 600_000) // every 10 minutes
    public void scheduledRefresh() {
        refresh();
    }

    public void refresh() {
        log.info("Refreshing tenant-shard cache from {} configured shards", clusterProperties.getShards().size());
        Map<String, ShardInfo> newMap = new HashMap<>();

        for (Map.Entry<String, ClusterProperties.ShardConfig> entry : clusterProperties.getShards().entrySet()) {
            String shardId = entry.getKey();
            ClusterProperties.ShardConfig shardConfig = entry.getValue();
            try {
                scanShard(shardId, shardConfig, newMap);
            } catch (Exception e) {
                log.error("Failed to scan shard {}: {}", shardId, e.getMessage(), e);
            }
        }

        lock.writeLock().lock();
        try {
            tenantShardMap = new ConcurrentHashMap<>(newMap);
            // Initialize status for any new tenants (default active)
            for (String tenantId : newMap.keySet()) {
                tenantStatusMap.putIfAbsent(tenantId, true);
            }
        } finally {
            lock.writeLock().unlock();
        }
        log.info("Tenant-shard cache refreshed: {} tenants across {} shards", newMap.size(), clusterProperties.getShards().size());
    }

    private void scanShard(String shardId, ClusterProperties.ShardConfig shardConfig,
                           Map<String, ShardInfo> accumulator) throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                shardConfig.getJdbcUrl(), shardConfig.getUsername(), shardConfig.getPassword())) {

            // Collect all schemas that have a databasechangelog table and are UUID-named
            List<String> uuidSchemas = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT table_schema FROM information_schema.tables " +
                    "WHERE table_name = 'databasechangelog'");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String schemaName = rs.getString(1);
                    if (UuidUtils.isUuid(schemaName)) {
                        uuidSchemas.add(schemaName);
                    }
                }
            }

            for (String schemaName : uuidSchemas) {
                try {
                    UUID.fromString(schemaName);
                    ShardInfo info = ShardInfo.builder()
                            .shardId(shardId)
                            .jdbcUrl(shardConfig.getJdbcUrl())
                            .schemaName(schemaName)
                            .build();

                    accumulator.put(schemaName, info);
                } catch(IllegalArgumentException e) {
                    // Ignore non-UUID schema names
                }
            }
        }
    }

    public Optional<ShardInfo> getShardForTenant(String tenantId) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(tenantShardMap.get(tenantId));
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isTenantActive(String tenantId) {
        lock.readLock().lock();
        try {
            return tenantStatusMap.getOrDefault(tenantId, false);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTenantStatus(String tenantId, boolean active) {
        lock.writeLock().lock();
        try {
            tenantStatusMap.put(tenantId, active);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void registerTenant(String tenantId, ShardInfo shardInfo) {
        lock.writeLock().lock();
        try {
            tenantShardMap.put(tenantId, shardInfo);
            tenantStatusMap.put(tenantId, true);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeTenant(String tenantId) {
        lock.writeLock().lock();
        try {
            tenantShardMap.remove(tenantId);
            tenantStatusMap.remove(tenantId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<String, ShardInfo> getAllTenants() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableMap(new HashMap<>(tenantShardMap));
        } finally {
            lock.readLock().unlock();
        }
    }
}
