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

import org.springframework.stereotype.Service;

import org.gsginzburg.cluster.framework.autoconfigure.ClusterProperties;
import org.gsginzburg.shared.security.JwtClaims;
import org.gsginzburg.shared.security.JwtService;
import org.gsginzburg.shared.security.TokenType;

@Slf4j
@Service
public class ClusterJwtService implements JwtService {

    private final SecretKey signingKey;
    private final String clusterId;
    private final String issuer;
    private final long sessionTokenExpiry;

    public ClusterJwtService(ClusterProperties props) {
        this.signingKey = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.clusterId = props.getId();
        this.issuer = props.getJwt().getIssuer();
        this.sessionTokenExpiry = props.getJwt().getSessionTokenExpirySeconds();
    }

    @Override
    public String generateToken(JwtClaims claims) {
        Map<String, Object> extra = new HashMap<>();
        if (claims.getType() != null) extra.put("type", claims.getType().name());
        if (claims.getTenantId() != null) extra.put("tenant_id", claims.getTenantId());
        if (claims.getClusterId() != null) extra.put("cluster_id", claims.getClusterId());
        if (claims.getEmail() != null) extra.put("email", claims.getEmail());
        if (claims.getRoles() != null) extra.put("roles", claims.getRoles());
        if (claims.getUserType() != null) extra.put("user_type", claims.getUserType());

        return Jwts.builder()
                .subject(claims.getSub())
                .issuer(issuer)
                .issuedAt(Date.from(claims.getIssuedAt() != null ? claims.getIssuedAt() : Instant.now()))
                .expiration(Date.from(claims.getExpiresAt()))
                .claims(extra)
                .signWith(signingKey)
                .compact();
    }

    public String issueSessionToken(JwtClaims exchangeClaims) {
        Instant now = Instant.now();
        JwtClaims sessionClaims = JwtClaims.builder()
                .sub(exchangeClaims.getSub())
                .type(TokenType.CLUSTER_SESSION)
                .email(exchangeClaims.getEmail())
                .tenantId(exchangeClaims.getTenantId())
                .clusterId(clusterId)
                .roles(exchangeClaims.getRoles())
                .userType(exchangeClaims.getUserType())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(sessionTokenExpiry))
                .build();
        return generateToken(sessionClaims);
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
                .clusterId((String) body.get("cluster_id"))
                .email((String) body.get("email"))
                .roles(body.get("roles") instanceof List<?> list
                        ? list.stream().map(Object::toString).toList()
                        : null)
                .userType((String) body.get("user_type"))
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
        return "{\"keys\":[{\"kty\":\"oct\",\"use\":\"sig\",\"alg\":\"HS256\"}]}";
    }
}
