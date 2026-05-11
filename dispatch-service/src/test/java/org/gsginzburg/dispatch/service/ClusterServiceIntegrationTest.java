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
import org.gsginzburg.dispatch.domain.dto.CreateClusterRequest;
import org.gsginzburg.dispatch.domain.model.Cluster;
import org.gsginzburg.dispatch.domain.model.ClusterStatus;
import org.gsginzburg.dispatch.domain.repository.ClusterRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClusterServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired ClusterService clusterService;
    @Autowired ClusterRepository clusterRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void createCluster_persistsAllFieldsToDb() {
        CreateClusterRequest req = new CreateClusterRequest();
        req.setName("Alpha Cluster");
        req.setUrl("https://alpha.cluster.internal");

        Cluster cluster = clusterService.createCluster(req);

        assertThat(cluster.getId()).isNotNull();
        assertThat(cluster.getName()).isEqualTo("Alpha Cluster");
        assertThat(cluster.getUrl()).isEqualTo("https://alpha.cluster.internal");
        assertThat(cluster.getStatus()).isEqualTo(ClusterStatus.ACTIVE);

        Cluster stored = clusterRepository.findById(cluster.getId()).orElseThrow();
        assertThat(stored.getCreatedAt()).isNotNull();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispatch.cluster WHERE id = ?::uuid AND name = ? AND url = ? AND status = 'ACTIVE'",
                Integer.class,
                cluster.getId().toString(), "Alpha Cluster", "https://alpha.cluster.internal");
        assertThat(count).isEqualTo(1);
    }

    @Test
    void getCluster_returnsCorrectData() {
        Cluster created = createCluster("Beta Cluster", "https://beta.cluster.internal");

        Cluster fetched = clusterService.getCluster(created.getId());

        assertThat(fetched.getId()).isEqualTo(created.getId());
        assertThat(fetched.getName()).isEqualTo("Beta Cluster");
        assertThat(fetched.getUrl()).isEqualTo("https://beta.cluster.internal");
        assertThat(fetched.getStatus()).isEqualTo(ClusterStatus.ACTIVE);
    }

    @Test
    void getAllClusters_returnsSortedByName() {
        createCluster("Zebra Cluster", "https://zebra.internal");
        createCluster("Apple Cluster", "https://apple.internal");
        createCluster("Mango Cluster", "https://mango.internal");

        List<Cluster> all = clusterService.getAllClusters();

        List<String> names = all.stream()
                .map(Cluster::getName)
                .filter(n -> n.endsWith("Cluster") && !n.equals("Test Cluster"))
                .toList();
        assertThat(names).containsExactly("Apple Cluster", "Mango Cluster", "Zebra Cluster");
    }

    @Test
    void getClusters_paginationWorks() {
        for (int i = 1; i <= 5; i++) {
            createCluster("Page Cluster " + i, "https://page" + i + ".internal");
        }

        Page<Cluster> page0 = clusterService.getClusters(0, 3);
        Page<Cluster> page1 = clusterService.getClusters(1, 3);

        assertThat(page0.getContent()).hasSize(3);
        assertThat(page0.getTotalPages()).isGreaterThanOrEqualTo(2);
        assertThat(page1.getContent()).isNotEmpty();
    }

    @Test
    void updateCluster_modifiesNameAndUrlInDb() {
        Cluster created = createCluster("Old Name", "https://old.internal");

        CreateClusterRequest update = new CreateClusterRequest();
        update.setName("New Name");
        update.setUrl("https://new.internal");
        clusterService.updateCluster(created.getId(), update);

        Cluster stored = clusterRepository.findById(created.getId()).orElseThrow();
        assertThat(stored.getName()).isEqualTo("New Name");
        assertThat(stored.getUrl()).isEqualTo("https://new.internal");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispatch.cluster WHERE id = ?::uuid AND name = 'New Name' AND url = 'https://new.internal'",
                Integer.class, created.getId().toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void deleteCluster_setsStatusInactiveInDb() {
        Cluster created = createCluster("Doomed Cluster", "https://doomed.internal");

        clusterService.deleteCluster(created.getId());

        Cluster stored = clusterRepository.findById(created.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ClusterStatus.INACTIVE);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispatch.cluster WHERE id = ?::uuid AND status = 'INACTIVE'",
                Integer.class, created.getId().toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void createCluster_duplicateName_throwsConstraintViolation() {
        createCluster("Unique Cluster", "https://unique.internal");

        CreateClusterRequest dup = new CreateClusterRequest();
        dup.setName("Unique Cluster");
        dup.setUrl("https://other.internal");

        assertThatThrownBy(() -> clusterService.createCluster(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @AfterEach
    void cleanupDb() {
        jdbcTemplate.execute("TRUNCATE dispatch.cluster CASCADE");
    }

    // --- helpers ---

    private Cluster createCluster(String name, String url) {
        CreateClusterRequest req = new CreateClusterRequest();
        req.setName(name);
        req.setUrl(url);
        return clusterService.createCluster(req);
    }
}
