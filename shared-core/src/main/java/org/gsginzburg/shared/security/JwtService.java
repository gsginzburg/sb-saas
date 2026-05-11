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

package org.gsginzburg.shared.security;

/**
 * Contract for JWT generation, parsing, and validation within the platform.
 * Implementations live in the modules that own their private keys
 * (dispatch-service for BACKOFFICE/TENANT_EXCHANGE tokens, cluster-framework
 * for CLUSTER_SESSION tokens).
 */
public interface JwtService {

    /**
     * Generate a signed JWT string from the supplied claims.
     *
     * @param claims the claim set to embed
     * @return compact serialised JWT (header.payload.signature)
     */
    String generateToken(JwtClaims claims);

    /**
     * Parse and verify a JWT string, returning its claim set.
     *
     * @param token compact JWT string
     * @return the verified {@link JwtClaims}
     * @throws io.jsonwebtoken.JwtException if the token is malformed, expired, or has an invalid signature
     */
    JwtClaims parseToken(String token);

    /**
     * Validate a JWT string without returning its claims.
     *
     * @param token compact JWT string
     * @return {@code true} if the token is well-formed, unexpired, and validly signed
     */
    boolean validateToken(String token);

    /**
     * Return the public key set for this service's signing key in JWK Set
     * JSON format, so that remote parties can verify tokens without a
     * round-trip to the issuer.
     *
     * @return JSON string conforming to RFC 7517 JWK Set format
     */
    String getPublicKeyJwks();
}
