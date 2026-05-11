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

import org.gsginzburg.dispatch.domain.dto.CreateClusterRequest;
import org.gsginzburg.dispatch.domain.model.Cluster;
import org.gsginzburg.dispatch.domain.model.ClusterStatus;
import org.gsginzburg.dispatch.domain.repository.ClusterRepository;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final ClusterRepository clusterRepository;

    @Transactional(readOnly = true)
    public List<Cluster> getAllClusters() {
        return clusterRepository.findAll(Sort.by("name"));
    }

    @Transactional(readOnly = true)
    public Page<Cluster> getClusters(int page, int size) {
        return clusterRepository.findAll(PageRequest.of(page, size, Sort.by("name")));
    }

    @Transactional(readOnly = true)
    public Cluster getCluster(UUID id) {
        return clusterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cluster not found: " + id));
    }

    @Transactional
    public Cluster createCluster(CreateClusterRequest request) {
        Cluster cluster = Cluster.builder()
                .name(request.getName())
                .url(request.getUrl())
                .build();
        return clusterRepository.save(cluster);
    }

    @Transactional
    public Cluster updateCluster(UUID id, CreateClusterRequest request) {
        Cluster cluster = clusterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cluster not found: " + id));
        cluster.setName(request.getName());
        cluster.setUrl(request.getUrl());
        return clusterRepository.save(cluster);
    }

    @Transactional
    public void deleteCluster(UUID id) {
        Cluster cluster = clusterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cluster not found: " + id));
        cluster.setStatus(ClusterStatus.INACTIVE);
        clusterRepository.save(cluster);
    }
}
