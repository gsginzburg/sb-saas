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
 * Stub for GCP Firebase authentication.
 * To fully implement: add com.google.firebase:firebase-admin dependency,
 * initialize FirebaseApp and use FirebaseAuth.getInstance().verifyIdToken(token).
 */
@Slf4j
@Component
public class FirebaseAuthProvider implements ExternalAuthProvider {

    @Value("${dispatch.auth.firebase.project-id:}")
    private String projectId;

    @Override
    public String getProviderId() {
        return "firebase";
    }

    @Override
    public String verifyTokenAndGetEmail(String externalToken) {
        // TODO: Implement Firebase JWT verification
        // 1. Initialize Firebase Admin SDK with service account credentials
        // 2. Call FirebaseAuth.getInstance().verifyIdToken(externalToken)
        // 3. Extract email from decoded token
        log.warn("Firebase auth provider is not fully implemented - stub returning null");
        return null;
    }

    @Override
    public boolean isEnabled() {
        return projectId != null && !projectId.isBlank();
    }
}
