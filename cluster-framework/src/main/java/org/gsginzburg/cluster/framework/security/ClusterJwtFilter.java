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

package org.gsginzburg.cluster.framework.security;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import org.gsginzburg.cluster.framework.datasource.TenantContext;
import org.gsginzburg.cluster.framework.datasource.TenantContextHolder;
import org.gsginzburg.shared.security.JwtClaims;
import org.gsginzburg.shared.security.TokenType;

@Slf4j
@RequiredArgsConstructor
public class ClusterJwtFilter extends OncePerRequestFilter {

    private final ClusterJwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        TenantContextHolder.clear();
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.validateToken(token)) {
                JwtClaims claims = jwtService.parseToken(token);
                if (claims.getType() == TokenType.CLUSTER_SESSION) {
                    TenantContext ctx = TenantContext.builder()
                            .userId(claims.getSub())
                            .tenantId(claims.getTenantId())
                            .email(claims.getEmail())
                            .roles(claims.getRoles())
                            .userType(claims.getUserType())
                            .build();
                    TenantContextHolder.set(ctx);

                    List<SimpleGrantedAuthority> authorities = claims.getRoles() != null
                            ? claims.getRoles().stream()
                                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                    .collect(Collectors.toList())
                            : List.of();

                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(claims, token, authorities));
                } else {
                    log.debug("JWT rejected: token type '{}' is not CLUSTER_SESSION uri={}",
                            claims.getType(), request.getRequestURI());
                }
            } else {
                log.warn("JWT validation failed uri={}", request.getRequestURI());
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
