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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;

import org.springframework.web.filter.OncePerRequestFilter;

import org.gsginzburg.cluster.framework.autoconfigure.ClusterProperties;
import org.gsginzburg.cluster.framework.datasource.TenantContext;
import org.gsginzburg.cluster.framework.datasource.TenantContextHolder;
import org.gsginzburg.shared.util.Base62;

/**
 * Extracts the tenant context from a {@code /c/{base62TenantId}/...} URL path segment
 * and treats it identically to a tenant ID carried in the JWT claim.
 *
 * <p>The filter runs after {@link ClusterJwtFilter}, which means the JWT has already been
 * validated and {@link TenantContextHolder} holds the authenticated user's context.
 * This filter then:
 * <ol>
 *   <li>Checks the feature is enabled via {@code cluster.path-tenant.enabled}.</li>
 *   <li>Matches the URL against {@code /c/<22-char-base62>/<rest>}.</li>
 *   <li>Verifies the caller's {@code user_type} is in {@code cluster.path-tenant.allowed-user-types}.</li>
 *   <li>Decodes the base62 segment to a UUID and pushes a new {@link TenantContext} with the
 *       overridden {@code tenantId} onto the {@link TenantContextHolder} stack.</li>
 *   <li>Rewrites the request path to strip the {@code /c/<base62>} prefix so that
 *       downstream controllers see the normal API path.</li>
 * </ol>
 *
 * <p>Example: {@code GET /c/0BmSmBmYGzaaaaaaaaaaaa/api/records} is forwarded downstream
 * as {@code GET /api/records} with the tenant context set to the decoded UUID.
 */
@Slf4j
@RequiredArgsConstructor
public class TenantPathFilter extends OncePerRequestFilter {

    /** Matches /c/<22 base62 chars>/<rest-of-path> */
    private static final Pattern PATH_PATTERN =
            Pattern.compile("^/c/([A-Za-z0-9]{" + Base62.LENGTH + "})(/.*)$");

    private final ClusterProperties properties;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        ClusterProperties.PathTenant config = properties.getPathTenant();
        if (!config.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String ctxPath = request.getContextPath();
        String fullUri = request.getRequestURI();
        // Strip servlet context path to get the plain application path
        String appPath = ctxPath.isEmpty() ? fullUri : fullUri.substring(ctxPath.length());

        Matcher matcher = PATH_PATTERN.matcher(appPath);
        if (!matcher.matches()) {
            filterChain.doFilter(request, response);
            return;
        }

        // JWT filter runs first; if the user is unauthenticated, context is null and
        // Spring Security's authorization check will return 401 — no need to handle here.
        TenantContext existingCtx = TenantContextHolder.get();
        if (existingCtx == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String userType = existingCtx.getUserType();
        if (userType == null || !config.getAllowedUserTypes().contains(userType)) {
            log.warn("Path-tenant injection denied: userType='{}' not in allowedUserTypes={} uri={}",
                    userType, config.getAllowedUserTypes(), request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Path-based tenant context is not permitted for this user type");
            return;
        }

        UUID tenantId;
        try {
            tenantId = Base62.decode(matcher.group(1));
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid tenant identifier in URL path");
            return;
        }

        log.debug("Path-tenant injection: overriding tenantId to {} for user {}",
                tenantId, existingCtx.getUserId());

        // Push a new context with the overridden tenant ID. ClusterJwtFilter's finally
        // block will call clear() after we return, collapsing the stack entirely.
        TenantContext overriddenCtx = TenantContext.builder()
                .userId(existingCtx.getUserId())
                .tenantId(tenantId.toString())
                .email(existingCtx.getEmail())
                .roles(existingCtx.getRoles())
                .userType(existingCtx.getUserType())
                .build();

        TenantContextHolder.push(overriddenCtx);
        try {
            String rewrittenPath = ctxPath + matcher.group(2);
            filterChain.doFilter(new RewrittenPathRequest(request, rewrittenPath), response);
        } finally {
            TenantContextHolder.pop();
        }
    }

    /**
     * Wraps the original request and overrides path-related methods so that
     * Spring MVC routes the request as if the {@code /c/<base62>} prefix was never there.
     */
    private static final class RewrittenPathRequest extends HttpServletRequestWrapper {

        private final String rewrittenUri;

        RewrittenPathRequest(HttpServletRequest request, String rewrittenUri) {
            super(request);
            this.rewrittenUri = rewrittenUri;
        }

        @Override
        public String getRequestURI() {
            return rewrittenUri;
        }

        @Override
        public String getServletPath() {
            return rewrittenUri;
        }

        @Override
        public StringBuffer getRequestURL() {
            // Rebuild from scheme/host/port + rewritten path (no query string in URL)
            StringBuffer sb = new StringBuffer();
            sb.append(getScheme()).append("://").append(getServerName());
            int port = getServerPort();
            boolean defaultPort = ("http".equals(getScheme()) && port == 80)
                    || ("https".equals(getScheme()) && port == 443);
            if (!defaultPort) sb.append(':').append(port);
            sb.append(rewrittenUri);
            return sb;
        }
    }
}
