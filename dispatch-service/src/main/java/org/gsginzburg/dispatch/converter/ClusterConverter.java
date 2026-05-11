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

import org.gsginzburg.dispatch.domain.dto.ClusterDto;
import org.gsginzburg.dispatch.domain.model.Cluster;
import org.gsginzburg.shared.util.DtoConverter;

/**
 * Converts between {@link Cluster} domain objects and {@link ClusterDto}.
 *
 * <p>All fields are handled automatically by the base class:
 * {@code id} (UUID↔String via id coercion), {@code name}, {@code url},
 * {@code createdAt} (direct copy), {@code status} (Enum↔String).
 */
@Component
public class ClusterConverter extends DtoConverter<ClusterDto, Cluster> {

    public ClusterConverter() {
        super(ClusterDto.class, Cluster.class);
    }
}
