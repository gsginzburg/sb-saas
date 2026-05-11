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
import java.sql.SQLException;
import java.sql.Statement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Routes JDBC connections to the correct shard and injects
 * {@code SET search_path TO "<tenantId>"} on every acquired connection.
 *
 * <p>Shard selection is resolved at connection time by looking up the current
 * tenant ID in {@link TenantShardCache} — shard ID is never stored in the JWT
 * or in {@link TenantContext}. Schema name equals the tenant UUID because each
 * tenant owns exactly one UUID-named PostgreSQL schema.
 *
 * <p>Setting {@code search_path} on every logical connection acquisition means
 * HikariCP connection reuse never leaks one tenant's schema into another
 * tenant's transaction.
 */
@Slf4j
@RequiredArgsConstructor
public class ClusterRoutingDataSource extends AbstractRoutingDataSource {

    private final TenantShardCache tenantShardCache;

    @Override
    protected Object determineCurrentLookupKey() {
        TenantContext ctx = TenantContextHolder.get();
        if (ctx == null || ctx.getTenantId() == null) return null;
        return tenantShardCache.getShardForTenant(ctx.getTenantId())
                .map(ShardInfo::getShardId)
                .orElse(null);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();
        applyTenantSchema(conn);
        return conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = super.getConnection(username, password);
        applyTenantSchema(conn);
        return conn;
    }

    private void applyTenantSchema(Connection conn) throws SQLException {
        TenantContext ctx = TenantContextHolder.get();
        if (ctx == null || ctx.getTenantId() == null || ctx.getTenantId().isBlank()) return;
        // Schema name == tenant UUID. Double-quote because UUIDs contain hyphens.
        String sql = "SET search_path TO \"" + ctx.getTenantId() + "\"";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
        log.debug("search_path set: schema={}", ctx.getTenantId());
    }
}
