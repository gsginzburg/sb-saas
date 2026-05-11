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

package org.gsginzburg.shared.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * User details returned by dispatch in response to a cluster-side lookup.
 */
@Data
@Builder
public class ClusterUserInfo {

    /** User UUID. */
    private String id;

    /** User email address. */
    private String email;

    /** User given name. */
    private String firstName;

    /** User family name. */
    private String lastName;

    /** User type string (BACKOFFICE or TENANT). */
    private String userType;

    /** Roles assigned to this user. */
    private List<String> roles;
}
