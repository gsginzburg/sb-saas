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

package org.gsginzburg.dispatch.domain.repository;

import org.gsginzburg.dispatch.domain.model.Tenant;
import org.gsginzburg.dispatch.domain.model.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    // Cluster is always needed (TenantConverter accesses it); load it in the same query.
    @EntityGraph(attributePaths = "cluster")
    @Override
    Optional<Tenant> findById(UUID id);

    Optional<Tenant> findByName(String name);
    Page<Tenant> findByStatus(TenantStatus status, Pageable pageable);
    List<Tenant> findByClusterId(UUID clusterId);
}
