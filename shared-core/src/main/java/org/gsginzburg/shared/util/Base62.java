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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;

/**
 * Encodes and decodes UUIDs to/from a 22-character base62 string.
 *
 * <p>The alphabet is {@code [0-9A-Za-z]}, producing URL-safe strings that are
 * shorter than the standard hyphenated UUID representation (22 vs 36 chars).
 * Every UUID encodes to exactly 22 characters; leading zeros are preserved via
 * left-padding with {@code '0'}.
 *
 * <p>22 characters are sufficient because {@code 62^22 > 2^128}.
 */
public final class Base62 {

    public static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public static final int LENGTH = 22;
    private static final BigInteger BASE = BigInteger.valueOf(62);
    private static final BigInteger MAX_VALUE = BigInteger.ONE.shiftLeft(128); // 2^128

    private Base62() {}

    /**
     * Encodes a UUID to a 22-character base62 string.
     *
     * @param uuid the UUID to encode; must not be null
     * @return a 22-character base62 representation, left-padded with '0' as needed
     */
    public static String encode(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        // 17-byte array: leading 0x00 forces BigInteger to treat it as positive
        byte[] bytes = new byte[17];
        for (int i = 0; i < 8; i++) {
            bytes[i + 1] = (byte) (msb >>> (56 - 8 * i));
            bytes[i + 9] = (byte) (lsb >>> (56 - 8 * i));
        }
        BigInteger value = new BigInteger(bytes);

        char[] result = new char[LENGTH];
        Arrays.fill(result, CHARS.charAt(0));
        int pos = LENGTH - 1;
        while (value.signum() > 0) {
            BigInteger[] divRem = value.divideAndRemainder(BASE);
            result[pos--] = CHARS.charAt(divRem[1].intValue());
            value = divRem[0];
        }
        return new String(result);
    }

    /**
     * Decodes a 22-character base62 string back to a UUID.
     *
     * @param encoded the base62 string; must be exactly {@value #LENGTH} characters
     *                from the alphabet {@code [0-9A-Za-z]}
     * @return the decoded UUID
     * @throws IllegalArgumentException if the string is invalid
     */
    public static UUID decode(String encoded) {
        if (encoded == null || encoded.length() != LENGTH) {
            throw new IllegalArgumentException(
                    "Base62 UUID must be exactly " + LENGTH + " characters, got: "
                    + (encoded == null ? "null" : encoded.length()));
        }

        BigInteger value = BigInteger.ZERO;
        for (char c : encoded.toCharArray()) {
            int idx = CHARS.indexOf(c);
            if (idx < 0) {
                throw new IllegalArgumentException("Invalid base62 character: '" + c + "'");
            }
            value = value.multiply(BASE).add(BigInteger.valueOf(idx));
        }

        if (value.compareTo(MAX_VALUE) >= 0) {
            throw new IllegalArgumentException("Base62 value exceeds 128-bit UUID range");
        }

        // toByteArray() may be shorter than 16 bytes (leading zeros) or 17 bytes
        // (extra sign byte when bit 127 is set). Normalise to exactly 16 bytes.
        byte[] raw = value.toByteArray();
        byte[] padded = new byte[16];
        int srcStart = raw.length > 16 ? raw.length - 16 : 0;
        int dstStart = 16 - (raw.length - srcStart);
        System.arraycopy(raw, srcStart, padded, dstStart, raw.length - srcStart);

        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) msb = (msb << 8) | (padded[i] & 0xFF);
        for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (padded[i] & 0xFF);

        return new UUID(msb, lsb);
    }
}
