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

package org.gsginzburg.dispatch.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.gsginzburg.dispatch.domain.dto.AssignTenantUserRequest;
import org.gsginzburg.dispatch.domain.dto.CreateTenantRequest;
import org.gsginzburg.dispatch.domain.model.Cluster;
import org.gsginzburg.dispatch.domain.model.Tenant;
import org.gsginzburg.dispatch.domain.model.TenantStatus;
import org.gsginzburg.dispatch.domain.model.TenantUser;
import org.gsginzburg.dispatch.domain.model.TenantUserId;
import org.gsginzburg.dispatch.domain.model.UserStatus;
import org.gsginzburg.dispatch.domain.repository.ClusterRepository;
import org.gsginzburg.dispatch.domain.repository.TenantRepository;
import org.gsginzburg.dispatch.domain.repository.TenantUserRepository;
import org.gsginzburg.shared.exception.TenantNotFoundException;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ClusterRepository clusterRepository;
    private final TenantUserRepository tenantUserRepository;

    @Transactional(readOnly = true)
    public Page<Tenant> getTenants(int page, int size) {
        return tenantRepository.findAll(PageRequest.of(page, size, Sort.by("name")));
    }

    @Transactional(readOnly = true)
    public Tenant getTenant(UUID id) {
        return findTenant(id);
    }

    @Transactional
    public Tenant createTenant(CreateTenantRequest request) {
        Cluster cluster = clusterRepository.findById(request.getClusterId())
                .orElseThrow(() -> new RuntimeException("Cluster not found: " + request.getClusterId()));
        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .cluster(cluster)
                .build();
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant updateTenantStatus(UUID id, TenantStatus status) {
        Tenant tenant = findTenant(id);
        tenant.setStatus(status);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant assignCluster(UUID id, UUID clusterId) {
        Tenant tenant = findTenant(id);
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new RuntimeException("Cluster not found: " + clusterId));
        tenant.setCluster(cluster);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(UUID id) {
        Tenant tenant = findTenant(id);
        tenant.setStatus(TenantStatus.ARCHIVED);
        tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantUser> getTenantUsers(UUID tenantId) {
        return tenantUserRepository.findByTenantId(tenantId);
    }

    @Transactional
    public void assignUser(UUID tenantId, AssignTenantUserRequest request) {
        TenantUser tu = TenantUser.builder()
                .tenantId(tenantId)
                .userId(request.getUserId())
                .role(request.getRole())
                .build();
        tenantUserRepository.save(tu);
    }

    @Transactional
    public void removeUser(UUID tenantId, UUID userId) {
        TenantUser tu = tenantUserRepository.findById(new TenantUserId(tenantId, userId))
                .orElseThrow(() -> new RuntimeException("User not in tenant"));
        tu.setStatus(UserStatus.INACTIVE);
        tenantUserRepository.save(tu);
    }

    private Tenant findTenant(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id.toString()));
    }
}
