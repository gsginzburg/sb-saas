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

package org.gsginzburg.dispatch.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.gsginzburg.dispatch.domain.dto.CreateUserRequest;
import org.gsginzburg.dispatch.domain.model.AppUser;
import org.gsginzburg.dispatch.domain.model.UserStatus;
import org.gsginzburg.dispatch.domain.repository.AppUserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<AppUser> getUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("email")));
    }

    @Transactional(readOnly = true)
    public AppUser getUser(UUID id) {
        return findUser(id);
    }

    @Transactional
    public AppUser createUser(CreateUserRequest request) {
        AppUser user = AppUser.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .userType(request.getUserType())
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public AppUser updateUserStatus(UUID id, UserStatus status) {
        AppUser user = findUser(id);
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        AppUser user = findUser(id);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    private AppUser findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
}
