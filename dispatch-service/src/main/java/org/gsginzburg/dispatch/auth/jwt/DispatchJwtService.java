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

package org.gsginzburg.dispatch.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.gsginzburg.shared.security.JwtClaims;
import org.gsginzburg.shared.security.JwtService;
import org.gsginzburg.shared.security.TokenType;

@Slf4j
@Service
public class DispatchJwtService implements JwtService {

    private final SecretKey signingKey;
    private final String issuer;

    public DispatchJwtService(
            @Value("${dispatch.jwt.secret}") String secret,
            @Value("${dispatch.jwt.issuer:dispatch}") String issuer) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    @Override
    public String generateToken(JwtClaims claims) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (claims.getType() != null) extraClaims.put("type", claims.getType().name());
        if (claims.getTenantId() != null) extraClaims.put("tenant_id", claims.getTenantId());
        if (claims.getClusterUrl() != null) extraClaims.put("cluster_url", claims.getClusterUrl());
        if (claims.getClusterId() != null) extraClaims.put("cluster_id", claims.getClusterId());
        if (claims.getEmail() != null) extraClaims.put("email", claims.getEmail());
        if (claims.getRoles() != null) extraClaims.put("roles", claims.getRoles());

        return Jwts.builder()
                .subject(claims.getSub())
                .issuer(issuer)
                .issuedAt(Date.from(claims.getIssuedAt() != null ? claims.getIssuedAt() : Instant.now()))
                .expiration(Date.from(claims.getExpiresAt()))
                .claims(extraClaims)
                .signWith(signingKey)
                .compact();
    }

    @Override
    public JwtClaims parseToken(String token) {
        Claims body = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return JwtClaims.builder()
                .sub(body.getSubject())
                .iss(body.getIssuer())
                .type(body.get("type") != null ? TokenType.valueOf((String) body.get("type")) : null)
                .tenantId((String) body.get("tenant_id"))
                .clusterUrl((String) body.get("cluster_url"))
                .clusterId((String) body.get("cluster_id"))
                .email((String) body.get("email"))
                .roles(body.get("roles") instanceof List<?> list ? list.stream().map(Object::toString).toList() : null)
                .issuedAt(body.getIssuedAt().toInstant())
                .expiresAt(body.getExpiration().toInstant())
                .build();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getPublicKeyJwks() {
        // HMAC signing doesn't have a public key; for RSA/EC signing this would return the JWK Set.
        // With HMAC, clusters share the same secret. Return a minimal descriptor.
        return "{\"keys\":[{\"kty\":\"oct\",\"use\":\"sig\",\"alg\":\"HS256\"}]}";
    }
}
