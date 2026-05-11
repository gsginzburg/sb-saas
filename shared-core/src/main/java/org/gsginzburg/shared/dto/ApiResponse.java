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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Standard envelope for all REST API responses in the sb-saas platform.
 *
 * @param <T> the type of the response payload
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String error;
    private String message;
    /** Per-field validation errors; only present on 400 validation-failure responses. */
    private Map<String, List<String>> fieldErrors;

    /**
     * Successful response carrying a payload.
     */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * Successful response with no payload (e.g. DELETE, void operations).
     */
    public static <T> ApiResponse<T> ok() {
        return ApiResponse.<T>builder()
                .success(true)
                .build();
    }

    /**
     * Error response carrying a machine-readable error string.
     */
    public static <T> ApiResponse<T> error(String error) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .build();
    }

    /**
     * Validation-failure response with per-field error lists.
     * The top-level {@code error} is a human-readable summary; {@code fieldErrors}
     * maps each failing field name to its list of constraint messages.
     */
    public static ApiResponse<Void> validationError(Map<String, List<String>> fieldErrors) {
        return ApiResponse.<Void>builder()
                .success(false)
                .error("Validation failed")
                .fieldErrors(fieldErrors)
                .build();
    }
}
