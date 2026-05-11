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

package org.gsginzburg.cluster.framework.management;

import java.util.Map;

import lombok.RequiredArgsConstructor;

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

import org.gsginzburg.cluster.framework.datasource.TenantShardCache;
import org.gsginzburg.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/management/tenants")
@PreAuthorize("hasRole('BACKOFFICE')")
@RequiredArgsConstructor
public class TenantManagementController {

    private final TenantMigrationService migrationService;
    private final TenantShardCache tenantShardCache;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createTenant(@RequestBody Map<String, String> body) throws Exception {
        String tenantId = body.get("tenantId");
        String shardId = body.get("shardId");
        migrationService.createTenant(tenantId, shardId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable String tenantId) throws Exception {
        migrationService.deleteTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PutMapping("/{tenantId}/status")
    public ResponseEntity<ApiResponse<Void>> setTenantStatus(
            @PathVariable String tenantId,
            @RequestParam boolean active) {
        tenantShardCache.setTenantStatus(tenantId, active);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/{tenantId}/move")
    public ResponseEntity<ApiResponse<Void>> moveTenant(
            @PathVariable String tenantId,
            @RequestParam String targetShardId) {
        migrationService.moveTenant(tenantId, targetShardId);
        return ResponseEntity.accepted().body(ApiResponse.<Void>builder()
                .success(true).message("Migration started asynchronously").build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, ?>>> listTenants() {
        return ResponseEntity.ok(ApiResponse.ok(tenantShardCache.getAllTenants()));
    }
}
