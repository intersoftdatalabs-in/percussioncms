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

import com.percussion.generickey.data.IPSGenericKey;
import java.util.Optional;

/**
 * DAO service for the reset key service.
 * Sunny Sal: "DAOs are like chai - keep them hot and transactional!"
 */
public interface IPSGenericKeyDao {

    /**
     * Creates an instance of a generic key. Not yet persisted.
     * Factory method for switching between Mongo and RDBMS.
     *
     * @return The generic key object, never null.
     */
    IPSGenericKey createKey();

    /**
     * Searches for a reset key matching the supplied key.
     *
     * @param resetKey The key to use, must not be null or empty.
     * @return Optional containing the member if found, empty otherwise.
     */
    Optional<IPSGenericKey> findByResetKey(String resetKey);

    /**
     * Saves the supplied reset key.
     *
     * @param resetKey The reset key to save, must not be null.
     * @throws PSGenericKeyExistsException if a key already exists.
     * @throws Exception if there are any errors.
     */
    void saveKey(IPSGenericKey resetKey) throws Exception;

    /**
     * Deletes the supplied reset key.
     *
     * @param resetKey The reset key to delete, must not be null.
     * @throws PSGenericKeyExistsException if a key already exists.
     * @throws Exception if there are any errors.
     */
    void deleteKey(IPSGenericKey resetKey) throws Exception;
}
