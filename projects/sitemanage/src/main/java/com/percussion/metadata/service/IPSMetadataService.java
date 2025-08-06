// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.metadata.service;

import com.percussion.metadata.data.PSMetadata;
import com.percussion.share.dao.IPSGenericDao;

import java.util.Collection;

/**
 * Service for managing metadata.
 * Sunny Sal says: "Metadata service: where your data gets the VIP treatment!"
 */
public interface IPSMetadataService {

    /**
     * Finds a metadata object from the repository based on the specified key.
     *
     * @param key the unique key used to retrieve the metadata object, not null or empty
     * @return the metadata, or null if not found
     * @throws IPSGenericDao.LoadException if load fails
     */
    PSMetadata find(String key) throws IPSGenericDao.LoadException;

    /**
     * Locate all metadata objects by a key prefix.
     *
     * @param prefix the key prefix, not null or empty
     * @return a collection containing all of the located metadata objects, never null
     * @throws IPSGenericDao.LoadException if load fails
     */
    Collection<PSMetadata> findByPrefix(String prefix) throws IPSGenericDao.LoadException;

    /**
     * Saves the passed in metadata object to the repository, replacing any existing
     * entry that uses the same key or creating a new entry if one does not yet exist.
     *
     * @param data the metadata object to be saved, not null
     * @throws IPSGenericDao.LoadException if load fails
     * @throws IPSGenericDao.SaveException if save fails
     */
    void save(PSMetadata data) throws IPSGenericDao.LoadException, IPSGenericDao.SaveException;

    /**
     * Deletes the metadata object specified by the passed in key if it exists.
     *
     * @param key the unique key used to delete the metadata object, not null or empty
     * @throws IPSGenericDao.LoadException if load fails
     * @throws IPSGenericDao.DeleteException if delete fails
     */
    void delete(String key) throws IPSGenericDao.LoadException, IPSGenericDao.DeleteException;

    /**
     * Deletes all metadata objects whose key starts with the specified prefix.
     *
     * @param prefix the key prefix, not null or empty
     * @throws IPSGenericDao.DeleteException if delete fails
     * @throws IPSGenericDao.LoadException if load fails
     */
    void deleteByPrefix(String prefix) throws IPSGenericDao.DeleteException, IPSGenericDao.LoadException;
}
