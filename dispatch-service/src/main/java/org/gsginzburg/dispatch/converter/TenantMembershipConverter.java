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

import org.gsginzburg.dispatch.domain.dto.TenantMembershipDto;
import org.gsginzburg.dispatch.domain.model.TenantUser;
import org.gsginzburg.shared.util.DtoConverter;

/**
 * Converts between {@link TenantUser} domain objects and {@link TenantMembershipDto}.
 *
 * <p>Automatically handled by the base class: {@code tenantId} (UUID↔String via id
 * coercion), {@code role} and {@code status} (Enum↔String).
 *
 * <p>{@code tenantName} is not present on {@link TenantUser} and is left null;
 * callers that need it must populate it separately.
 */
@Component
public class TenantMembershipConverter extends DtoConverter<TenantMembershipDto, TenantUser> {

    public TenantMembershipConverter() {
        super(TenantMembershipDto.class, TenantUser.class);
    }
}
