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

package org.gsginzburg.shared.exception;

/**
 * Base exception for all domain-level errors originating in the dispatch service
 * or shared platform layer.  Carries an HTTP status code so that exception
 * handlers can map it directly to a response without additional logic.
 */
public class DispatchException extends RuntimeException {

    private final int statusCode;

    public DispatchException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public DispatchException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
