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

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.gsginzburg.dispatch.domain.dto.UserDto;
import org.gsginzburg.dispatch.domain.model.AppUser;
import org.gsginzburg.dispatch.domain.model.UserStatus;
import org.gsginzburg.shared.security.UserType;

import static org.assertj.core.api.Assertions.assertThat;

class UserConverterTest {

    private final UserConverter converter = new UserConverter();

    // ── toDto ────────────────────────────────────────────────────────────────

    @Test
    void toDto_idConvertedToString() {
        UUID id = UUID.randomUUID();
        AppUser model = appUser(id, "a@b.com", UserType.TENANT, UserStatus.ACTIVE);

        assertThat(converter.toDto(model).getId()).isEqualTo(id.toString());
    }

    @Test
    void toDto_stringFieldsCopied() {
        AppUser model = new AppUser();
        model.setId(UUID.randomUUID());
        model.setEmail("john@example.com");
        model.setFirstName("John");
        model.setLastName("Doe");
        model.setUserType(UserType.TENANT);
        model.setStatus(UserStatus.ACTIVE);

        UserDto dto = converter.toDto(model);

        assertThat(dto.getEmail()).isEqualTo("john@example.com");
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getLastName()).isEqualTo("Doe");
    }

    @Test
    void toDto_userTypeEnumConvertedToString() {
        AppUser model = appUser(UUID.randomUUID(), "a@b.com", UserType.BACKOFFICE, UserStatus.ACTIVE);

        assertThat(converter.toDto(model).getUserType()).isEqualTo("BACKOFFICE");
    }

    @Test
    void toDto_statusEnumConvertedToString() {
        AppUser model = appUser(UUID.randomUUID(), "a@b.com", UserType.TENANT, UserStatus.INACTIVE);

        assertThat(converter.toDto(model).getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void toDto_createdAtCopied() {
        OffsetDateTime now = OffsetDateTime.now();
        AppUser model = appUser(UUID.randomUUID(), "a@b.com", UserType.TENANT, UserStatus.ACTIVE);
        model.setCreatedAt(now);

        assertThat(converter.toDto(model).getCreatedAt()).isEqualTo(now);
    }

    @Test
    void toDto_tenantMembershipsAlwaysNull() {
        // AppUser carries no membership collection; callers must populate it separately.
        AppUser model = appUser(UUID.randomUUID(), "a@b.com", UserType.TENANT, UserStatus.ACTIVE);

        assertThat(converter.toDto(model).getTenantMemberships()).isNull();
    }

    @Test
    void toDto_passwordHashNotExposedInDto() {
        AppUser model = appUser(UUID.randomUUID(), "a@b.com", UserType.TENANT, UserStatus.ACTIVE);
        model.setPasswordHash("$2a$10$hashed");

        // UserDto has no passwordHash field; the value must not appear anywhere.
        UserDto dto = converter.toDto(model);
        assertThat(dto).hasNoNullFieldsOrPropertiesExcept(
                "firstName", "lastName", "tenantMemberships", "createdAt");
    }

    // ── toModel ──────────────────────────────────────────────────────────────

    @Test
    void toModel_idConvertedToUuid() {
        UUID id = UUID.randomUUID();
        UserDto dto = new UserDto();
        dto.setId(id.toString());
        dto.setEmail("a@b.com");
        dto.setUserType("TENANT");
        dto.setStatus("ACTIVE");

        assertThat(converter.toModel(dto).getId()).isEqualTo(id);
    }

    @Test
    void toModel_stringFieldsCopied() {
        UserDto dto = new UserDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setEmail("jane@example.com");
        dto.setFirstName("Jane");
        dto.setLastName("Smith");
        dto.setUserType("BACKOFFICE");
        dto.setStatus("ACTIVE");

        AppUser model = converter.toModel(dto);

        assertThat(model.getEmail()).isEqualTo("jane@example.com");
        assertThat(model.getFirstName()).isEqualTo("Jane");
        assertThat(model.getLastName()).isEqualTo("Smith");
    }

    @Test
    void toModel_stringUserTypeConvertedToEnum() {
        UserDto dto = new UserDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setEmail("a@b.com");
        dto.setUserType("BACKOFFICE");
        dto.setStatus("ACTIVE");

        assertThat(converter.toModel(dto).getUserType()).isEqualTo(UserType.BACKOFFICE);
    }

    @Test
    void toModel_stringStatusConvertedToEnum() {
        UserDto dto = new UserDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setEmail("a@b.com");
        dto.setUserType("TENANT");
        dto.setStatus("INACTIVE");

        assertThat(converter.toModel(dto).getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void toModel_nullStatusKeepsEntityDefault() {
        UserDto dto = new UserDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setEmail("a@b.com");
        dto.setUserType("TENANT");
        // status null — entity default (ACTIVE) is preserved

        assertThat(converter.toModel(dto).getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static AppUser appUser(UUID id, String email, UserType userType, UserStatus status) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setEmail(email);
        u.setUserType(userType);
        u.setStatus(status);
        return u;
    }
}
