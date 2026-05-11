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

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.gsginzburg.dispatch.converter.TenantConverter;
import org.gsginzburg.dispatch.converter.TenantMembershipConverter;
import org.gsginzburg.dispatch.domain.dto.AssignTenantUserRequest;
import org.gsginzburg.dispatch.domain.dto.CreateTenantRequest;
import org.gsginzburg.dispatch.domain.dto.TenantDto;
import org.gsginzburg.dispatch.domain.dto.TenantMembershipDto;
import org.gsginzburg.dispatch.domain.model.Tenant;
import org.gsginzburg.dispatch.domain.model.TenantStatus;
import org.gsginzburg.dispatch.service.TenantService;
import org.gsginzburg.shared.dto.ApiResponse;
import org.gsginzburg.shared.dto.PageDto;

@RestController
@RequestMapping("/api/backoffice/tenants")
@PreAuthorize("hasRole('BACKOFFICE')")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final TenantConverter tenantConverter;
    private final TenantMembershipConverter membershipConverter;

    @GetMapping
    public ResponseEntity<ApiResponse<PageDto<TenantDto>>> getTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Tenant> result = tenantService.getTenants(page, size);
        PageDto<TenantDto> dto = PageDto.<TenantDto>builder()
                .content(result.getContent().stream().map(tenantConverter::toDto).toList())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .pageNumber(page)
                .pageSize(size)
                .build();
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantDto>> getTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(tenantConverter.toDto(tenantService.getTenant(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TenantDto>> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(tenantConverter.toDto(tenantService.createTenant(request))));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TenantDto>> updateStatus(
            @PathVariable UUID id,
            @RequestParam TenantStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(tenantConverter.toDto(tenantService.updateTenantStatus(id, status))));
    }

    @PutMapping("/{id}/cluster")
    public ResponseEntity<ApiResponse<TenantDto>> assignCluster(
            @PathVariable UUID id,
            @RequestParam UUID clusterId) {
        return ResponseEntity.ok(ApiResponse.ok(tenantConverter.toDto(tenantService.assignCluster(id, clusterId))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<ApiResponse<List<TenantMembershipDto>>> getTenantUsers(@PathVariable UUID id) {
        List<TenantMembershipDto> dtos = tenantService.getTenantUsers(id).stream()
                .map(membershipConverter::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @PostMapping("/{id}/users")
    public ResponseEntity<ApiResponse<Void>> assignUser(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTenantUserRequest request) {
        tenantService.assignUser(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @DeleteMapping("/{id}/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeUser(
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        tenantService.removeUser(id, userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
