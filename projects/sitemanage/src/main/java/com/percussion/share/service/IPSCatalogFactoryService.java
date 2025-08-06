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
package com.percussion.share.service;

import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSDataService.DataServiceNotFoundException;
import java.io.Serializable;
import java.util.List;

/**
 * Factory service for cataloging objects by type and key.
 *
 * @param <T>  the object type
 * @param <PK> the primary key type
 */
public interface IPSCatalogFactoryService<T, PK extends Serializable> {

    /**
     * Factory interface for creating catalog items.
     *
     * @param <F>  the object type
     * @param <PK> the primary key type
     */
    interface IPSCatalogItemFactory<F, PK> {
        F create(PK id) throws DataServiceLoadException;
    }

    /**
     * Gets all objects of a particular type.
     *
     * @param factory the factory to create objects
     * @param <F>     the object type
     * @return list of populated objects
     * @throws DataServiceLoadException
     * @throws DataServiceNotFoundException
     */
    <F extends T> List<F> findAll(IPSCatalogItemFactory<F, PK> factory)
            throws DataServiceLoadException, DataServiceNotFoundException;

    /**
     * Gets an object by class and identifier.
     *
     * @param factory the factory to create the object
     * @param id      the identifier (primary key) of the object to get
     * @param <F>     the object type
     * @return a populated object
     * @throws DataServiceLoadException
     */
    <F extends T> F find(IPSCatalogItemFactory<F, PK> factory, PK id) throws DataServiceLoadException;

    /**
     * Gets an object by class, identifier, and relationship type.
     *
     * @param factory              the factory to create the object
     * @param id                   the identifier (primary key) of the object to get
     * @param relationshipTypeName the relationship type name
     * @param <F>                  the object type
     * @return a populated object
     * @throws DataServiceLoadException
     */
    <F extends T> F find(IPSCatalogItemFactory<F, PK> factory, PK id, String relationshipTypeName)
            throws DataServiceLoadException;
}
