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

package org.gsginzburg.shared.util;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utility helpers for UUID strings used as entity identifiers throughout
 * the platform.
 */
public final class UuidUtils {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
    );

    private UuidUtils() {
        // utility class – no instances
    }

    /**
     * Return {@code true} if {@code s} is a valid lowercase-or-mixed-case
     * UUID string (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx).
     */
    public static boolean isUuid(String s) {
        return s != null && UUID_PATTERN.matcher(s.toLowerCase()).matches();
    }

    /**
     * Parse a UUID string, delegating to {@link UUID#fromString(String)}.
     *
     * @throws IllegalArgumentException if the string is not a valid UUID
     */
    public static UUID fromString(String s) {
        return UUID.fromString(s);
    }
}
