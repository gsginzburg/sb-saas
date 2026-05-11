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

import org.gsginzburg.dispatch.converter.ClusterConverter;
import org.gsginzburg.dispatch.domain.dto.ClusterDto;
import org.gsginzburg.dispatch.domain.dto.CreateClusterRequest;
import org.gsginzburg.dispatch.domain.model.Cluster;
import org.gsginzburg.dispatch.service.ClusterService;
import org.gsginzburg.shared.dto.ApiResponse;
import org.gsginzburg.shared.dto.PageDto;

@RestController
@RequestMapping("/api/backoffice/clusters")
@PreAuthorize("hasRole('BACKOFFICE')")
@RequiredArgsConstructor
public class ClusterController {

    private final ClusterService clusterService;
    private final ClusterConverter clusterConverter;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ClusterDto>>> getAllClusters() {
        List<ClusterDto> dtos = clusterService.getAllClusters().stream()
                .map(clusterConverter::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageDto<ClusterDto>>> getClusters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Cluster> result = clusterService.getClusters(page, size);
        PageDto<ClusterDto> dto = PageDto.<ClusterDto>builder()
                .content(result.getContent().stream().map(clusterConverter::toDto).toList())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .pageNumber(page)
                .pageSize(size)
                .build();
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClusterDto>> getCluster(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(clusterConverter.toDto(clusterService.getCluster(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClusterDto>> createCluster(@Valid @RequestBody CreateClusterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(clusterConverter.toDto(clusterService.createCluster(request))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClusterDto>> updateCluster(
            @PathVariable UUID id,
            @Valid @RequestBody CreateClusterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(clusterConverter.toDto(clusterService.updateCluster(id, request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCluster(@PathVariable UUID id) {
        clusterService.deleteCluster(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
