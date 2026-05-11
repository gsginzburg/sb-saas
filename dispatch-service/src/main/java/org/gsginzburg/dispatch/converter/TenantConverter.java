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

import org.springframework.stereotype.Component;

import org.gsginzburg.dispatch.domain.dto.TenantDto;
import org.gsginzburg.dispatch.domain.model.Tenant;
import org.gsginzburg.shared.util.DtoConverter;

/**
 * Converts between {@link Tenant} domain objects and {@link TenantDto}.
 *
 * <p>Automatically handled by the base class: {@code id} (UUID↔String),
 * {@code name}, {@code createdAt}, {@code updatedAt}, {@code status} (Enum↔String).
 *
 * <p>{@code clusterId}, {@code clusterName}, and {@code clusterUrl} are extracted
 * from the already-loaded {@link org.gsginzburg.dispatch.domain.model.Cluster}
 * association in {@link #afterToDto}; callers must ensure the cluster is initialised
 * before invoking this converter.
 */
@Component
public class TenantConverter extends DtoConverter<TenantDto, Tenant> {

    public TenantConverter() {
        super(TenantDto.class, Tenant.class);
    }

    @Override
    protected void afterToDto(Tenant model, TenantDto dto) {
        dto.setClusterId(model.getCluster().getId().toString());
        dto.setClusterName(model.getCluster().getName());
        dto.setClusterUrl(model.getCluster().getUrl());
    }
}
