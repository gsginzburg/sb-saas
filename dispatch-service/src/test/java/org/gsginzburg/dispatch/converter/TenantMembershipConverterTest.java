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

package org.gsginzburg.dispatch.converter;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.gsginzburg.dispatch.domain.dto.TenantMembershipDto;
import org.gsginzburg.dispatch.domain.model.TenantUser;
import org.gsginzburg.dispatch.domain.model.UserStatus;
import org.gsginzburg.shared.security.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class TenantMembershipConverterTest {

    private final TenantMembershipConverter converter = new TenantMembershipConverter();

    // ── toDto ────────────────────────────────────────────────────────────────

    @Test
    void toDto_tenantIdConvertedToString() {
        UUID tenantId = UUID.randomUUID();
        TenantUser model = tenantUser(tenantId, UserRole.USER, UserStatus.ACTIVE);

        assertThat(converter.toDto(model).getTenantId()).isEqualTo(tenantId.toString());
    }

    @Test
    void toDto_roleEnumConvertedToString() {
        TenantUser model = tenantUser(UUID.randomUUID(), UserRole.ADMIN, UserStatus.ACTIVE);

        assertThat(converter.toDto(model).getRole()).isEqualTo("ADMIN");
    }

    @Test
    void toDto_statusEnumConvertedToString() {
        TenantUser model = tenantUser(UUID.randomUUID(), UserRole.USER, UserStatus.INACTIVE);

        assertThat(converter.toDto(model).getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void toDto_tenantNameIsAlwaysNull() {
        // TenantUser carries no tenant name; callers must populate this separately.
        TenantUser model = tenantUser(UUID.randomUUID(), UserRole.USER, UserStatus.ACTIVE);

        assertThat(converter.toDto(model).getTenantName()).isNull();
    }

    // ── toModel ──────────────────────────────────────────────────────────────

    @Test
    void toModel_tenantIdConvertedToUuid() {
        UUID tenantId = UUID.randomUUID();
        TenantMembershipDto dto = new TenantMembershipDto();
        dto.setTenantId(tenantId.toString());
        dto.setRole("USER");
        dto.setStatus("ACTIVE");

        assertThat(converter.toModel(dto).getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void toModel_stringRoleConvertedToEnum() {
        TenantMembershipDto dto = new TenantMembershipDto();
        dto.setTenantId(UUID.randomUUID().toString());
        dto.setRole("ADMIN");
        dto.setStatus("ACTIVE");

        assertThat(converter.toModel(dto).getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void toModel_stringStatusConvertedToEnum() {
        TenantMembershipDto dto = new TenantMembershipDto();
        dto.setTenantId(UUID.randomUUID().toString());
        dto.setRole("USER");
        dto.setStatus("INACTIVE");

        assertThat(converter.toModel(dto).getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static TenantUser tenantUser(UUID tenantId, UserRole role, UserStatus status) {
        TenantUser tu = new TenantUser();
        tu.setTenantId(tenantId);
        tu.setUserId(UUID.randomUUID());
        tu.setRole(role);
        tu.setStatus(status);
        return tu;
    }
}
