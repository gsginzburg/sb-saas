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

package org.gsginzburg.shared.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Canonical representation of the claims carried in every platform JWT.
 * Fields not present in a given token type will be null.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class JwtClaims {

    /** Subject – user UUID. */
    private String sub;

    /** Issuer – "dispatch" for BACKOFFICE/TENANT_EXCHANGE tokens, cluster ID for CLUSTER_SESSION. */
    private String iss;

    /** Token purpose / type. */
    private TokenType type;

    /** Tenant UUID – present on TENANT_EXCHANGE and CLUSTER_SESSION tokens. */
    private String tenantId;

    /** Cluster URL – present on TENANT_EXCHANGE tokens only. */
    private String clusterUrl;

    /** Cluster ID – present on CLUSTER_SESSION tokens. */
    private String clusterId;

    /** User email address. */
    private String email;

    /** Authority roles granted to this user. */
    private List<String> roles;

    /** User type (e.g. BACKOFFICE, TENANT, SYSTEM) – carried from the originating token. */
    private String userType;

    /** Token issuance timestamp. */
    private Instant issuedAt;

    /** Token expiry timestamp. */
    private Instant expiresAt;
}
