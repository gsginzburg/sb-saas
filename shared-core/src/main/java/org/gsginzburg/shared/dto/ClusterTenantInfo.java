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

package org.gsginzburg.shared.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Tenant details returned by dispatch in response to a cluster-side lookup.
 * Carries enough context for the cluster to establish a tenant session without
 * an additional round-trip.
 */
@Data
@Builder
public class ClusterTenantInfo {

    /** Tenant UUID. */
    private String id;

    /** Human-readable tenant display name. */
    private String name;

    /** Tenant lifecycle status (e.g. ACTIVE, SUSPENDED, INACTIVE). */
    private String status;

    /** UUID of the cluster this tenant is assigned to. */
    private String clusterId;

    /** Display name of the assigned cluster. */
    private String clusterName;

    /** Base URL of the assigned cluster. */
    private String clusterUrl;
}
