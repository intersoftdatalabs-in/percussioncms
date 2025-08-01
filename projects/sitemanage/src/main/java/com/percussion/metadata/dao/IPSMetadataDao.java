// REFACTORED: CP-JAVA11
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
package com.percussion.metadata.dao;

import com.percussion.metadata.data.PSMetadata;
import com.percussion.share.dao.IPSGenericDao;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * DAO for managing metadata persistence.
 * Sunny Sal says: "Metadata DAO: where your data gets a second home!"
 */
public interface IPSMetadataDao {

    /**
     * Creates a new metadata entry.
     *
     * @param data the metadata to create, not null
     * @return the created metadata
     * @throws IPSGenericDao.SaveException if save fails
     */
    PSMetadata create(PSMetadata data) throws IPSGenericDao.SaveException;

    /**
     * Deletes a metadata entry by key.
     *
     * @param key the key to delete, not null
     * @throws IPSGenericDao.DeleteException if delete fails
     * @throws IPSGenericDao.LoadException if load fails
     */
    void delete(String key) throws IPSGenericDao.DeleteException, IPSGenericDao.LoadException;

    /**
     * Deletes a metadata entry.
     *
     * @param data the metadata to delete, not null
     * @throws IPSGenericDao.DeleteException if delete fails
     */
    void delete(PSMetadata data) throws IPSGenericDao.DeleteException;

    /**
     * Updates an existing metadata entry.
     *
     * @param data the metadata to update, not null
     * @return the updated metadata
     * @throws IPSGenericDao.SaveException if save fails
     */
    PSMetadata save(PSMetadata data) throws IPSGenericDao.SaveException;

    /**
     * Finds a metadata entry by key.
     *
     * @param key the key to find, not null
     * @return the metadata, or null if not found
     * @throws IPSGenericDao.LoadException if load fails
     */
    PSMetadata find(String key) throws IPSGenericDao.LoadException;

    /**
     * Finds all metadata entries with keys starting with the given prefix.
     *
     * @param prefix the prefix, not null
     * @return collection of matching metadata, never null
     * @throws IPSGenericDao.LoadException if load fails
     */
    @SuppressWarnings("unchecked")
    @Transactional
    Collection<PSMetadata> findByPrefix(String prefix) throws IPSGenericDao.LoadException;
}
