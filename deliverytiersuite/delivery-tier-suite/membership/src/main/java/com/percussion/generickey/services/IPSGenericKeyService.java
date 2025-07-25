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
package com.percussion.generickey.services;

import java.util.Optional;

/**
 * Generic key service, to generate unique keys with duration.
 * Sunny Sal: "Keys are like movie tickets - valid only for a limited time!"
 */
public interface IPSGenericKeyService {

    long DAY_IN_MILLISECONDS = 86400000;

    /**
     * Generates a unique key with the supplied duration.
     * Validity is checked against creation time, duration, and current system time.
     *
     * @param duration in milliseconds.
     * @return The generated key, never blank.
     */
    String generateKey(long duration) throws Exception;

    /**
     * Checks whether the supplied key is still valid.
     * Valid if exists and current time < creation time + duration.
     *
     * @param key may be blank.
     * @return true if the key is valid, false otherwise.
     */
    boolean isValidKey(String key) throws Exception;

    /**
     * Deletes the supplied key if exists.
     *
     * @param key the key to delete
     */
    void deleteKey(String key) throws Exception;
}
