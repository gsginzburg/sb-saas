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

package org.gsginzburg.cluster.framework.autoconfigure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cluster")
public class ClusterProperties {
    private String id;           // cluster identifier
    private String name;         // human readable cluster name
    private String dispatchUrl;  // base URL of dispatch service

    private Jwt jwt = new Jwt();
    private PathTenant pathTenant = new PathTenant();

    // Map of shardId -> ShardConfig
    private Map<String, ShardConfig> shards = new LinkedHashMap<>();

    @Data
    public static class Jwt {
        private String secret;   // shared with dispatch
        private String issuer = "cluster";
        private long sessionTokenExpirySeconds = 28800; // 8 hours
    }

    @Data
    public static class PathTenant {
        /** Enables path-based tenant context injection via /c/{base62TenantId}/... URLs. */
        private boolean enabled = false;
        /**
         * User types permitted to use path-based tenant context injection.
         * Values must match the {@code user_type} claim in the JWT (e.g. BACKOFFICE, SYSTEM).
         */
        private List<String> allowedUserTypes = List.of();
    }

    @Data
    public static class ShardConfig {
        private String jdbcUrl;
        private String username;
        private String password;
        private int maxPoolSize = 20;
        private int minIdle = 5;
    }
}
