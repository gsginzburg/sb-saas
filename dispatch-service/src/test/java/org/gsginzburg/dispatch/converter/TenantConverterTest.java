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

import org.gsginzburg.dispatch.domain.dto.TenantDto;
import org.gsginzburg.dispatch.domain.model.Cluster;
import org.gsginzburg.dispatch.domain.model.ClusterStatus;
import org.gsginzburg.dispatch.domain.model.Tenant;
import org.gsginzburg.dispatch.domain.model.TenantStatus;

import static org.assertj.core.api.Assertions.assertThat;

class TenantConverterTest {

    private final TenantConverter converter = new TenantConverter();

    // ── toDto ────────────────────────────────────────────────────────────────

    @Test
    void toDto_scalarFieldsCopied() {
        Tenant model = tenantWithCluster();
        model.setName("Acme Corp");

        TenantDto dto = converter.toDto(model);

        assertThat(dto.getName()).isEqualTo("Acme Corp");
    }

    @Test
    void toDto_idConvertedToString() {
        UUID id = UUID.randomUUID();
        Tenant model = tenantWithCluster();
        model.setId(id);

        assertThat(converter.toDto(model).getId()).isEqualTo(id.toString());
    }

    @Test
    void toDto_enumStatusConvertedToString() {
        Tenant model = tenantWithCluster();
        model.setStatus(TenantStatus.INACTIVE);

        assertThat(converter.toDto(model).getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void toDto_timestampsCopied() {
        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        OffsetDateTime updated = OffsetDateTime.now();
        Tenant model = tenantWithCluster();
        model.setCreatedAt(created);
        model.setUpdatedAt(updated);

        TenantDto dto = converter.toDto(model);

        assertThat(dto.getCreatedAt()).isEqualTo(created);
        assertThat(dto.getUpdatedAt()).isEqualTo(updated);
    }

    @Test
    void toDto_clusterIdExtractedFromNestedCluster() {
        UUID clusterId = UUID.randomUUID();
        Cluster cluster = cluster(clusterId, "C1", "https://c1.internal");
        Tenant model = tenantWithCluster(cluster);

        assertThat(converter.toDto(model).getClusterId()).isEqualTo(clusterId.toString());
    }

    @Test
    void toDto_clusterNameExtractedFromNestedCluster() {
        Cluster cluster = cluster(UUID.randomUUID(), "My Cluster", "https://my.internal");
        Tenant model = tenantWithCluster(cluster);

        assertThat(converter.toDto(model).getClusterName()).isEqualTo("My Cluster");
    }

    @Test
    void toDto_clusterUrlExtractedFromNestedCluster() {
        Cluster cluster = cluster(UUID.randomUUID(), "C", "https://cluster.internal");
        Tenant model = tenantWithCluster(cluster);

        assertThat(converter.toDto(model).getClusterUrl()).isEqualTo("https://cluster.internal");
    }

    // ── toModel ──────────────────────────────────────────────────────────────

    @Test
    void toModel_scalarFieldsCopied() {
        UUID id = UUID.randomUUID();
        TenantDto dto = new TenantDto();
        dto.setId(id.toString());
        dto.setName("Globex Corp");
        dto.setStatus("ACTIVE");

        Tenant model = converter.toModel(dto);

        assertThat(model.getId()).isEqualTo(id);
        assertThat(model.getName()).isEqualTo("Globex Corp");
    }

    @Test
    void toModel_stringStatusConvertedToEnum() {
        TenantDto dto = new TenantDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setName("n");
        dto.setStatus("ARCHIVED");

        assertThat(converter.toModel(dto).getStatus()).isEqualTo(TenantStatus.ARCHIVED);
    }

    @Test
    void toModel_nullStatusKeepsEntityDefault() {
        TenantDto dto = new TenantDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setName("n");
        // status null — entity default (ACTIVE) is preserved

        assertThat(converter.toModel(dto).getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Cluster cluster(UUID id, String name, String url) {
        Cluster c = new Cluster();
        c.setId(id);
        c.setName(name);
        c.setUrl(url);
        c.setStatus(ClusterStatus.ACTIVE);
        return c;
    }

    private static Tenant tenantWithCluster() {
        return tenantWithCluster(cluster(UUID.randomUUID(), "Default", "https://default.internal"));
    }

    private static Tenant tenantWithCluster(Cluster cluster) {
        Tenant t = new Tenant();
        t.setId(UUID.randomUUID());
        t.setName("Tenant");
        t.setStatus(TenantStatus.ACTIVE);
        t.setCluster(cluster);
        return t;
    }
}
