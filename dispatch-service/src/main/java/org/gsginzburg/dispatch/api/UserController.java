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

package org.gsginzburg.dispatch.api;

import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.gsginzburg.dispatch.converter.UserConverter;
import org.gsginzburg.dispatch.domain.dto.CreateUserRequest;
import org.gsginzburg.dispatch.domain.dto.UserDto;
import org.gsginzburg.dispatch.domain.model.AppUser;
import org.gsginzburg.dispatch.domain.model.UserStatus;
import org.gsginzburg.dispatch.service.UserService;
import org.gsginzburg.shared.dto.ApiResponse;
import org.gsginzburg.shared.dto.PageDto;

@RestController
@RequestMapping("/api/backoffice/users")
@PreAuthorize("hasRole('BACKOFFICE')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserConverter userConverter;

    @GetMapping
    public ResponseEntity<ApiResponse<PageDto<UserDto>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AppUser> result = userService.getUsers(page, size);
        PageDto<UserDto> dto = PageDto.<UserDto>builder()
                .content(result.getContent().stream().map(userConverter::toDto).toList())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .pageNumber(page)
                .pageSize(size)
                .build();
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userConverter.toDto(userService.getUser(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(userConverter.toDto(userService.createUser(request))));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserDto>> updateStatus(
            @PathVariable UUID id,
            @RequestParam UserStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(userConverter.toDto(userService.updateUserStatus(id, status))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
