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

import org.gsginzburg.dispatch.domain.dto.ClusterDto;
import org.gsginzburg.dispatch.domain.model.Cluster;
import org.gsginzburg.dispatch.domain.model.ClusterStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ClusterConverterTest {

    private final ClusterConverter converter = new ClusterConverter();

    // ── toDto ────────────────────────────────────────────────────────────────

    @Test
    void toDto_scalarStringFieldsCopied() {
        Cluster model = new Cluster();
        model.setId(UUID.randomUUID());
        model.setName("Alpha Cluster");
        model.setUrl("https://alpha.internal");
        model.setStatus(ClusterStatus.ACTIVE);

        ClusterDto dto = converter.toDto(model);

        assertThat(dto.getName()).isEqualTo("Alpha Cluster");
        assertThat(dto.getUrl()).isEqualTo("https://alpha.internal");
    }

    @Test
    void toDto_idConvertedToString() {
        UUID id = UUID.randomUUID();
        Cluster model = new Cluster();
        model.setId(id);
        model.setName("n");
        model.setUrl("https://x.internal");
        model.setStatus(ClusterStatus.ACTIVE);

        assertThat(converter.toDto(model).getId()).isEqualTo(id.toString());
    }

    @Test
    void toDto_enumStatusConvertedToString() {
        Cluster model = new Cluster();
        model.setId(UUID.randomUUID());
        model.setName("n");
        model.setUrl("https://x.internal");
        model.setStatus(ClusterStatus.INACTIVE);

        assertThat(converter.toDto(model).getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void toDto_createdAtCopied() {
        OffsetDateTime now = OffsetDateTime.now();
        Cluster model = new Cluster();
        model.setId(UUID.randomUUID());
        model.setName("n");
        model.setUrl("https://x.internal");
        model.setStatus(ClusterStatus.ACTIVE);
        model.setCreatedAt(now);

        assertThat(converter.toDto(model).getCreatedAt()).isEqualTo(now);
    }

    @Test
    void toDto_createdAtNullWhenNotSet() {
        Cluster model = new Cluster();
        model.setId(UUID.randomUUID());
        model.setName("n");
        model.setUrl("https://x.internal");
        model.setStatus(ClusterStatus.ACTIVE);
        // createdAt has no @Builder.Default, so it is truly null

        assertThat(converter.toDto(model).getCreatedAt()).isNull();
    }

    // ── toModel ──────────────────────────────────────────────────────────────

    @Test
    void toModel_scalarStringFieldsCopied() {
        ClusterDto dto = new ClusterDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setName("Beta Cluster");
        dto.setUrl("https://beta.internal");
        dto.setStatus("ACTIVE");

        Cluster model = converter.toModel(dto);

        assertThat(model.getName()).isEqualTo("Beta Cluster");
        assertThat(model.getUrl()).isEqualTo("https://beta.internal");
    }

    @Test
    void toModel_idConvertedToUuid() {
        UUID id = UUID.randomUUID();
        ClusterDto dto = new ClusterDto();
        dto.setId(id.toString());
        dto.setName("n");
        dto.setUrl("https://x.internal");
        dto.setStatus("ACTIVE");

        assertThat(converter.toModel(dto).getId()).isEqualTo(id);
    }

    @Test
    void toModel_stringStatusConvertedToEnum() {
        ClusterDto dto = new ClusterDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setName("n");
        dto.setUrl("https://x.internal");
        dto.setStatus("INACTIVE");

        assertThat(converter.toModel(dto).getStatus()).isEqualTo(ClusterStatus.INACTIVE);
    }

    @Test
    void toModel_nullStatusKeepsEntityDefault() {
        ClusterDto dto = new ClusterDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setName("n");
        dto.setUrl("https://x.internal");
        // status null — entity default (ACTIVE) is preserved

        assertThat(converter.toModel(dto).getStatus()).isEqualTo(ClusterStatus.ACTIVE);
    }
}
