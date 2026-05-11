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
 * Generic paginated result wrapper. Mirrors the essential fields exposed by
 * Spring Data's {@code Page} without coupling callers to that interface.
 *
 * @param <T> the type of items in this page
 */
@Data
@Builder
public class PageDto<T> {

    /** Items on the current page. */
    private List<T> content;

    /** Total number of items across all pages. */
    private long totalElements;

    /** Total number of pages. */
    private int totalPages;

    /** Zero-based index of the current page. */
    private int pageNumber;

    /** Maximum number of items per page. */
    private int pageSize;
}
