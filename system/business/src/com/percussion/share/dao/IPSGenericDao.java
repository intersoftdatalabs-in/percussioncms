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
// REFACTORED: CP-JAVA11
package com.percussion.share.dao;

import com.percussion.share.service.exception.PSDataServiceException;
import java.io.Serializable;
import java.util.List;

/**
 * Java 11 refactored: Generic DAO (Data Access Object) with common methods to CRUD POJOs.
 *
 * <p>Extend this interface for typesafe (no casting necessary) DAOs for your domain objects.
 *
 * <p>All returned lists must be non-null (may be empty). Implementations must be thread-safe.
 *
 * @author <a href="mailto:bwnoll@gmail.com">Bryan Noll</a>
 * @param <T> a type variable
 * @param <PK> the primary key for that type
 */
public interface IPSGenericDao<T, PK extends Serializable> {
    /**
     * Gets all objects of a particular type (all rows in a table).
     *
     * @return List of populated objects, never null
     * @throws PSDataServiceException if retrieval fails
     */
    List<T> findAll() throws PSDataServiceException;

    /**
     * Gets an object based on class and identifier. Throws
     * ObjectRetrievalFailureException if not found.
     *
     * @param id the identifier (primary key) of the object to get
     * @return a populated object, or null if not found
     * @throws PSDataServiceException if retrieval fails
     */
    T find(PK id) throws PSDataServiceException;

    /**
     * Saves an object (handles both update and insert).
     *
     * @param object the object to save
     * @return the persisted object
     * @throws PSDataServiceException if save fails
     */
    T save(T object) throws PSDataServiceException;

    /**
     * Removes an object from persistent storage.
     *
     * @param object the object to remove
     * @throws PSDataServiceException if removal fails
     */
    void remove(T object) throws PSDataServiceException;

    /**
     * Removes an object by its primary key.
     *
     * @param id the primary key of the object to remove
     * @throws PSDataServiceException if removal fails
     */
    void remove(PK id) throws PSDataServiceException;

    /*
     * Compatibility nested exceptions used by older code in sitemanage and other
     * modules.  The interface formerly declared these as public static inner
     * classes.  We keep them here as thin wrappers around
     * {@link PSDataServiceException} to avoid a massive refactor.  New code should
     * throw or catch {@code PSDataServiceException} directly.
     */
    @Deprecated
    class LoadException extends PSDataServiceException {
        public LoadException(String msg) { super(msg); }
        public LoadException(String msg, Throwable cause) { super(msg, cause); }
    }

    @Deprecated
    class SaveException extends PSDataServiceException {
        public SaveException(String msg) { super(msg); }
        public SaveException(String msg, Throwable cause) { super(msg, cause); }
    }

    @Deprecated
    class DeleteException extends PSDataServiceException {
        public DeleteException(String msg) { super(msg); }
        public DeleteException(String msg, Throwable cause) { super(msg, cause); }
    }
}
