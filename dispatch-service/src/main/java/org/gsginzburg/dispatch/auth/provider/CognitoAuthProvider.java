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

package org.gsginzburg.dispatch.auth.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Stub for AWS Cognito authentication.
 * To fully implement: add software.amazon.awssdk:cognitoidentityprovider dependency,
 * fetch JWKS from https://cognito-idp.{region}.amazonaws.com/{userPoolId}/.well-known/jwks.json
 * and validate the JWT locally.
 */
@Slf4j
@Component
public class CognitoAuthProvider implements ExternalAuthProvider {

    @Value("${dispatch.auth.cognito.user-pool-id:}")
    private String userPoolId;

    @Value("${dispatch.auth.cognito.region:us-east-1}")
    private String region;

    @Override
    public String getProviderId() {
        return "cognito";
    }

    @Override
    public String verifyTokenAndGetEmail(String externalToken) {
        // TODO: Implement Cognito JWT verification
        // 1. Fetch JWKs from Cognito endpoint
        // 2. Validate JWT signature, expiry, issuer
        // 3. Extract email claim
        log.warn("Cognito auth provider is not fully implemented - stub returning null");
        return null;
    }

    @Override
    public boolean isEnabled() {
        return userPoolId != null && !userPoolId.isBlank();
    }
}
