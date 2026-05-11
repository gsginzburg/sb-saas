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

package org.gsginzburg.cluster.framework.api;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.gsginzburg.cluster.framework.datasource.TenantShardCache;
import org.gsginzburg.cluster.framework.security.ClusterJwtService;
import org.gsginzburg.shared.dto.ApiResponse;
import org.gsginzburg.shared.exception.TenantInactiveException;
import org.gsginzburg.shared.exception.TenantNotFoundException;
import org.gsginzburg.shared.security.JwtClaims;
import org.gsginzburg.shared.security.TokenType;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthExchangeController {

    private final ClusterJwtService jwtService;
    private final TenantShardCache tenantShardCache;

    @PostMapping("/exchange")
    public ResponseEntity<ApiResponse<Map<String, String>>> exchangeToken(
            @RequestBody Map<String, String> body) {
        String exchangeToken = body.get("exchangeToken");
        if (exchangeToken == null || exchangeToken.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("exchangeToken is required"));
        }

        if (!jwtService.validateToken(exchangeToken)) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid or expired token"));
        }

        JwtClaims claims = jwtService.parseToken(exchangeToken);
        if (claims.getType() != TokenType.TENANT_EXCHANGE) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not an exchange token"));
        }

        String tenantId = claims.getTenantId();
        if (!tenantShardCache.getShardForTenant(tenantId).isPresent()) {
            throw new TenantNotFoundException(tenantId);
        }

        if (!tenantShardCache.isTenantActive(tenantId)) {
            throw new TenantInactiveException(tenantId);
        }

        String sessionToken = jwtService.issueSessionToken(claims);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "accessToken", sessionToken,
                "tokenType", "Bearer",
                "tenantId", tenantId
        )));
    }
}
