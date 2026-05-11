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

package org.gsginzburg.shared.client;

import org.gsginzburg.shared.dto.ClusterInfo;
import org.gsginzburg.shared.dto.ClusterTenantInfo;
import org.gsginzburg.shared.dto.ClusterUserInfo;

/**
 * Contract for cluster-to-dispatch communication.
 *
 * Implementations (e.g. a Feign client or RestClient adapter) are provided
 * by the cluster-framework module and auto-configured for each cluster
 * application via the Spring Boot starter.
 */
public interface DispatchClient {

    /**
     * Retrieve tenant details from dispatch by tenant UUID.
     *
     * @param tenantId UUID of the tenant
     * @return tenant info required to bootstrap a tenant context on the cluster
     * @throws org.gsginzburg.shared.exception.TenantNotFoundException  if tenantId is unknown
     * @throws org.gsginzburg.shared.exception.TenantInactiveException  if tenant is not active
     */
    ClusterTenantInfo getTenantInfo(String tenantId);

    /**
     * Retrieve user details from dispatch by user UUID.
     *
     * @param userId UUID of the user
     * @return user info including roles
     */
    ClusterUserInfo getUserInfo(String userId);

    /**
     * Retrieve cluster metadata from dispatch by cluster UUID.
     *
     * @param clusterId UUID of the cluster
     * @return cluster descriptor
     */
    ClusterInfo getClusterInfo(String clusterId);
}
