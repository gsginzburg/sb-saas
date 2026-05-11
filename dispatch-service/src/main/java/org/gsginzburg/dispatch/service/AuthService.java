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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.gsginzburg.dispatch.auth.provider.ExternalAuthProvider;
import org.gsginzburg.dispatch.domain.dto.LoginRequest;
import org.gsginzburg.dispatch.domain.dto.LoginResponse;
import org.gsginzburg.dispatch.domain.model.AppUser;
import org.gsginzburg.dispatch.domain.model.RefreshToken;
import org.gsginzburg.dispatch.domain.model.Tenant;
import org.gsginzburg.dispatch.domain.model.TenantUser;
import org.gsginzburg.dispatch.domain.model.UserStatus;
import org.gsginzburg.dispatch.domain.repository.AppUserRepository;
import org.gsginzburg.dispatch.domain.repository.RefreshTokenRepository;
import org.gsginzburg.dispatch.domain.repository.TenantRepository;
import org.gsginzburg.dispatch.domain.repository.TenantUserRepository;
import org.gsginzburg.shared.exception.DispatchException;
import org.gsginzburg.shared.security.JwtClaims;
import org.gsginzburg.shared.security.JwtService;
import org.gsginzburg.shared.security.TokenType;
import org.gsginzburg.shared.security.UserType;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final TenantUserRepository tenantUserRepository;
    private final TenantRepository tenantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final List<ExternalAuthProvider> externalAuthProviders;

    @Value("${dispatch.jwt.backoffice-token-expiry-seconds:28800}")
    private long backofficeTokenExpiry;

    @Value("${dispatch.jwt.exchange-token-expiry-seconds:300}")
    private long exchangeTokenExpiry;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmailAndStatus(request.getEmail(), UserStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found or inactive email={}", request.getEmail());
                    return new DispatchException("Invalid credentials", 401);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password email={}", request.getEmail());
            throw new DispatchException("Invalid credentials", 401);
        }

        log.debug("Login successful email={}", request.getEmail());
        return buildLoginResponse(user);
    }

    @Transactional
    public LoginResponse loginWithExternalToken(String externalToken, String providerName) {
        ExternalAuthProvider provider = externalAuthProviders.stream()
                .filter(p -> p.getProviderId().equals(providerName) && p.isEnabled())
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("External login failed: provider '{}' not available", providerName);
                    return new DispatchException("Auth provider not available: " + providerName, 400);
                });

        String email = provider.verifyTokenAndGetEmail(externalToken);
        if (email == null) {
            log.warn("External login failed: provider '{}' returned no email", providerName);
            throw new DispatchException("Invalid external token", 401);
        }

        AppUser user = userRepository.findByEmailAndStatus(email, UserStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.warn("External login failed: user not found or inactive email={} provider={}", email, providerName);
                    return new DispatchException("Invalid credentials", 401);
                });

        log.debug("External login successful email={} provider={}", email, providerName);
        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(AppUser user) {
        long expiry;
        TokenType tokenType;
        String clusterUrl = null;
        String tenantId = null;
        List<String> roles;

        if (user.getUserType() == UserType.BACKOFFICE) {
            expiry = backofficeTokenExpiry;
            tokenType = TokenType.BACKOFFICE;
            roles = List.of("BACKOFFICE");
        } else {
            expiry = exchangeTokenExpiry;
            tokenType = TokenType.TENANT_EXCHANGE;
            roles = List.of("TENANT");
            // find tenant assignments
            List<TenantUser> tenantUsers = tenantUserRepository.findActiveByUserId(user.getId());
            if (!tenantUsers.isEmpty()) {
                TenantUser tu = tenantUsers.get(0);
                tenantId = tu.getTenantId().toString();
                Tenant tenant = tenantRepository.findById(tu.getTenantId()).orElseThrow();
                clusterUrl = tenant.getCluster().getUrl();
                roles = List.of(tu.getRole().name());
            }
        }

        Instant now = Instant.now();
        JwtClaims claims = JwtClaims.builder()
                .sub(user.getId().toString())
                .type(tokenType)
                .email(user.getEmail())
                .tenantId(tenantId)
                .clusterUrl(clusterUrl)
                .roles(roles)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiry))
                .build();

        String accessToken = jwtService.generateToken(claims);
        String rawRefreshToken = generateRefreshToken();
        saveRefreshToken(user.getId(), rawRefreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(expiry)
                .userType(user.getUserType().name())
                .clusterUrl(clusterUrl)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> {
                    log.warn("Refresh token rejected: not found or already revoked");
                    return new DispatchException("Invalid or expired refresh token", 401);
                });

        if (storedToken.getExpiresAt().toInstant().isBefore(Instant.now())) {
            log.warn("Refresh token expired userId={}", storedToken.getUserId());
            throw new DispatchException("Refresh token expired", 401);
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        AppUser user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new DispatchException("User not found", 401));
        return buildLoginResponse(user);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void saveRefreshToken(UUID userId, String rawToken) {
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(hashToken(rawToken))
                .expiresAt(OffsetDateTime.now().plusDays(30))
                .revoked(false)
                .build());
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
