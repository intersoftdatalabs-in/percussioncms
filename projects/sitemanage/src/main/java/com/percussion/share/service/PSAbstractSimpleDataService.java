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
package com.percussion.share.service;

import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import java.io.Serializable;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract data service for simple objects (no summary).
 *
 * @param <T> Object type
 * @param <PK> Primary key type
 */
public abstract class PSAbstractSimpleDataService<T, PK extends Serializable>
    extends PSAbstractDataService<T, T, PK>
    implements IPSDataService<T, T, PK> {

    public PSAbstractSimpleDataService(IPSGenericDao<T, PK> dao) {
        super(dao);
    }

    @Override
    public T find(PK id)
            throws DataServiceLoadException, DataServiceNotFoundException, PSValidationException {
        validateIdParameter("find", id);
        return load(id);
    }

    @Override
    public List<T> findAll() throws PSDataServiceException {
        try {
            return getDao().findAll();
        } catch (IPSGenericDao.LoadException e) {
            var error = "Error loading all objects";
            log.error(error, e);
            throw new DataServiceLoadException(error, e);
        }
    }

    /**
     * Logger instance for this class, never {@code null}.
     */
    protected final Logger log = LogManager.getLogger(getClass());
}
