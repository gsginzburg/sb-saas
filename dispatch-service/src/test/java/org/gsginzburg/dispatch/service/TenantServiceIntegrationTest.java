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

import org.gsginzburg.dispatch.AbstractIntegrationTest;
import org.gsginzburg.dispatch.domain.dto.AssignTenantUserRequest;
import org.gsginzburg.dispatch.domain.dto.CreateClusterRequest;
import org.gsginzburg.dispatch.domain.dto.CreateTenantRequest;
import org.gsginzburg.dispatch.domain.model.AppUser;
import org.gsginzburg.dispatch.domain.model.Cluster;
import org.gsginzburg.dispatch.domain.model.Tenant;
import org.gsginzburg.dispatch.domain.model.TenantStatus;
import org.gsginzburg.dispatch.domain.model.TenantUser;
import org.gsginzburg.dispatch.domain.model.TenantUserId;
import org.gsginzburg.dispatch.domain.model.UserStatus;
import org.gsginzburg.dispatch.domain.repository.AppUserRepository;
import org.gsginzburg.dispatch.domain.repository.TenantRepository;
import org.gsginzburg.dispatch.domain.repository.TenantUserRepository;
import org.gsginzburg.shared.dto.PageDto;
import org.gsginzburg.shared.security.UserRole;
import org.gsginzburg.shared.security.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired TenantService tenantService;
    @Autowired ClusterService clusterService;
    @Autowired TenantRepository tenantRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired TenantUserRepository tenantUserRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private UUID clusterId;

    @BeforeEach
    void setupCluster() {
        CreateClusterRequest req = new CreateClusterRequest();
        req.setName("Test Cluster " + UUID.randomUUID());
        req.setUrl("https://test.cluster.internal");
        Cluster cluster = clusterService.createCluster(req);
        clusterId = cluster.getId();
    }

    @AfterEach
    void cleanupDb() {
        jdbcTemplate.execute("TRUNCATE dispatch.tenant_user, dispatch.tenant, dispatch.app_user, dispatch.cluster CASCADE");
    }

    @Test
    void createTenant_persistsAllFieldsToDb() {
        CreateTenantRequest req = new CreateTenantRequest();
        req.setName("Acme Corp");
        req.setClusterId(clusterId);

        Tenant tenant = tenantService.createTenant(req);

        assertThat(tenant.getId()).isNotNull();
        assertThat(tenant.getName()).isEqualTo("Acme Corp");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.getCluster().getId()).isEqualTo(clusterId);

        Tenant stored = tenantRepository.findById(tenant.getId()).orElseThrow();
        assertThat(stored.getCreatedAt()).isNotNull();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispatch.tenant WHERE id = ?::uuid AND name = ? AND status = 'ACTIVE' AND cluster_id = ?::uuid",
                Integer.class,
                tenant.getId().toString(), "Acme Corp", clusterId.toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void getTenant_returnsCorrectDataWithClusterInfo() {
        Tenant created = createTenant("Globex Corp");

        Tenant fetched = tenantService.getTenant(created.getId());

        assertThat(fetched.getId()).isEqualTo(created.getId());
        assertThat(fetched.getName()).isEqualTo("Globex Corp");
        assertThat(fetched.getCluster().getId()).isEqualTo(clusterId);
        assertThat(fetched.getCluster().getUrl()).isEqualTo("https://test.cluster.internal");
    }

    @Test
    void getTenants_paginationWorks() {
        for (int i = 1; i <= 4; i++) {
            createTenant("Tenant Paged " + i);
        }

        Page<Tenant> page = tenantService.getTenants(0, 2);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void updateTenantStatus_modifiesStatusInDb() {
        Tenant created = createTenant("Status Corp");
        UUID id = created.getId();

        tenantService.updateTenantStatus(id, TenantStatus.INACTIVE);

        Tenant stored = tenantRepository.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(TenantStatus.INACTIVE);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispatch.tenant WHERE id = ?::uuid AND status = 'INACTIVE'",
                Integer.class, id.toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void assignCluster_updatesClusterIdInDb() {
        Tenant created = createTenant("Mobile Corp");

        CreateClusterRequest req2 = new CreateClusterRequest();
        req2.setName("Cluster B " + UUID.randomUUID());
        req2.setUrl("https://b.cluster.internal");
        UUID clusterBId = clusterService.createCluster(req2).getId();

        tenantService.assignCluster(created.getId(), clusterBId);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispatch.tenant WHERE id = ?::uuid AND cluster_id = ?::uuid",
                Integer.class, created.getId().toString(), clusterBId.toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void deleteTenant_setsStatusArchivedInDb() {
        Tenant created = createTenant("Archived Corp");
        UUID id = created.getId();

        tenantService.deleteTenant(id);

        Tenant stored = tenantRepository.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(TenantStatus.ARCHIVED);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispatch.tenant WHERE id = ?::uuid AND status = 'ARCHIVED'",
                Integer.class, id.toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void assignUser_createsTenantUserRowInDb() {
        Tenant tenant = createTenant("User Corp");
        UUID tenantId = tenant.getId();
        AppUser user = createUser("john@test.local");

        AssignTenantUserRequest req = new AssignTenantUserRequest();
        req.setUserId(user.getId());
        req.setRole(UserRole.USER);

        tenantService.assignUser(tenantId, req);

        TenantUser tu = tenantUserRepository.findById(new TenantUserId(tenantId, user.getId())).orElseThrow();
        assertThat(tu.getRole()).isEqualTo(UserRole.USER);
        assertThat(tu.getStatus()).isEqualTo(UserStatus.ACTIVE);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispatch.tenant_user WHERE tenant_id = ?::uuid AND user_id = ?::uuid AND role = 'USER' AND status = 'ACTIVE'",
                Integer.class, tenantId.toString(), user.getId().toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void removeUser_setsUserStatusInactiveInDb() {
        Tenant tenant = createTenant("Leave Corp");
        UUID tenantId = tenant.getId();
        AppUser user = createUser("jane@test.local");

        AssignTenantUserRequest req = new AssignTenantUserRequest();
        req.setUserId(user.getId());
        req.setRole(UserRole.ADMIN);
        tenantService.assignUser(tenantId, req);

        tenantService.removeUser(tenantId, user.getId());

        TenantUser tu = tenantUserRepository.findById(new TenantUserId(tenantId, user.getId())).orElseThrow();
        assertThat(tu.getStatus()).isEqualTo(UserStatus.INACTIVE);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispatch.tenant_user WHERE tenant_id = ?::uuid AND user_id = ?::uuid AND status = 'INACTIVE'",
                Integer.class, tenantId.toString(), user.getId().toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void getTenantUsers_returnsAssignedMembers() {
        Tenant tenant = createTenant("Team Corp");
        UUID tenantId = tenant.getId();
        AppUser u1 = createUser("member1@test.local");
        AppUser u2 = createUser("member2@test.local");

        AssignTenantUserRequest r1 = new AssignTenantUserRequest();
        r1.setUserId(u1.getId());
        r1.setRole(UserRole.ADMIN);
        tenantService.assignUser(tenantId, r1);

        AssignTenantUserRequest r2 = new AssignTenantUserRequest();
        r2.setUserId(u2.getId());
        r2.setRole(UserRole.USER);
        tenantService.assignUser(tenantId, r2);

        List<TenantUser> members = tenantService.getTenantUsers(tenantId);
        assertThat(members).hasSize(2);
    }

    // --- helpers ---

    private Tenant createTenant(String name) {
        CreateTenantRequest req = new CreateTenantRequest();
        req.setName(name);
        req.setClusterId(clusterId);
        return tenantService.createTenant(req);
    }

    private AppUser createUser(String email) {
        AppUser user = AppUser.builder()
                .email(email)
                .userType(UserType.TENANT)
                .build();
        return appUserRepository.save(user);
    }
}
