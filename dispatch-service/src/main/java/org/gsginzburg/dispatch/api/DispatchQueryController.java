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

package org.gsginzburg.dispatch.api;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.gsginzburg.dispatch.domain.model.Tenant;
import org.gsginzburg.dispatch.domain.repository.AppUserRepository;
import org.gsginzburg.dispatch.domain.repository.ClusterRepository;
import org.gsginzburg.dispatch.domain.repository.TenantRepository;
import org.gsginzburg.shared.dto.ApiResponse;
import org.gsginzburg.shared.dto.ClusterInfo;
import org.gsginzburg.shared.dto.ClusterTenantInfo;
import org.gsginzburg.shared.dto.ClusterUserInfo;

/**
 * Read-only API consumed by cluster services to query dispatch data.
 * Authenticated via the same JWT mechanism (BACKOFFICE or cluster-internal service token).
 */
@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchQueryController {

    private final TenantRepository tenantRepository;
    private final AppUserRepository userRepository;
    private final ClusterRepository clusterRepository;

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<ApiResponse<ClusterTenantInfo>> getTenantInfo(@PathVariable UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        ClusterTenantInfo info = ClusterTenantInfo.builder()
                .id(tenant.getId().toString())
                .name(tenant.getName())
                .status(tenant.getStatus().name())
                .clusterId(tenant.getCluster().getId().toString())
                .clusterName(tenant.getCluster().getName())
                .clusterUrl(tenant.getCluster().getUrl())
                .build();
        return ResponseEntity.ok(ApiResponse.ok(info));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<ClusterUserInfo>> getUserInfo(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(u -> ClusterUserInfo.builder()
                        .id(u.getId().toString())
                        .email(u.getEmail())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .userType(u.getUserType().name())
                        .build())
                .map(info -> ResponseEntity.ok(ApiResponse.ok(info)))
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    @GetMapping("/cluster/{clusterId}")
    public ResponseEntity<ApiResponse<ClusterInfo>> getClusterInfo(@PathVariable UUID clusterId) {
        return clusterRepository.findById(clusterId)
                .map(c -> ClusterInfo.builder()
                        .id(c.getId().toString())
                        .name(c.getName())
                        .url(c.getUrl())
                        .status(c.getStatus().name())
                        .build())
                .map(info -> ResponseEntity.ok(ApiResponse.ok(info)))
                .orElseThrow(() -> new RuntimeException("Cluster not found: " + clusterId));
    }
}
