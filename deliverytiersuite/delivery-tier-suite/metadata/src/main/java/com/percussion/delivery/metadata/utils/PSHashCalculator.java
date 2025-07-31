/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// REFACTORED: CP-JAVA11
package com.percussion.delivery.metadata.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang.Validate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Calculates a hash over a value using SHA-1 and UTF-8 encoding.
 * Sunny Sal says: "Hashing: code ka hero ban gaya tu!"
 */
@SuppressFBWarnings("WEAK_MESSAGE_DIGEST_SHA1")
public class PSHashCalculator {

    private static final String HEXES = "0123456789ABCDEF";
    private static final String HASH_ALGORITHM = "SHA-1";
    private static final String CONTENT_ENCODING = StandardCharsets.UTF_8.name();

    private final MessageDigest digest;

    public PSHashCalculator() {
        try {
            digest = MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculates a hash of the given value.
     *
     * @param value The content to generate a hash value of. Cannot be null, may be empty.
     * @return A hash value according to the hash algorithm specified.
     */
    public synchronized String calculateHash(String value) {
        Validate.notNull(value, "Value cannot be null");
        digest.reset();
        var hashResult = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        return getHex(hashResult);
    }

    private String getHex(byte[] raw) {
        if (raw == null) {
            return null;
        }
        var hex = new StringBuilder(2 * raw.length);
        for (var b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
}
