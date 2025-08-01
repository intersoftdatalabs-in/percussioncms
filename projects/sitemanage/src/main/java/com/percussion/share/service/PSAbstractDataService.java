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
package com.percussion.share.service;

import com.percussion.error.PSExceptionUtils;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.dao.IPSGenericDao.DeleteException;
import com.percussion.share.dao.IPSGenericDao.LoadException;
import com.percussion.share.dao.IPSGenericDao.SaveException;
import com.percussion.share.service.exception.PSBeanValidationUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Optional;

import static com.percussion.share.service.exception.PSParameterValidationUtils.rejectIfNull;
import static java.text.MessageFormat.format;
import static org.apache.commons.lang.Validate.notNull;

/**
 * Abstract base for data services, providing CRUD and validation logic.
 *
 * @param <FULL> Full object type
 * @param <SUM> Summary object type
 * @param <PK> Primary key type
 */
public abstract class PSAbstractDataService<FULL, SUM, PK extends Serializable>
    implements IPSDataService<FULL, SUM, PK> {

    protected final IPSGenericDao<FULL, PK> dao;

    /**
     * @param dao never {@code null}
     */
    public PSAbstractDataService(IPSGenericDao<FULL, PK> dao) {
        super();
        notNull(dao, "DAO must not be null");
        this.dao = dao;
    }

    @Override
    public PSValidationErrors validate(FULL obj) throws PSValidationException {
        return PSBeanValidationUtils.getValidationErrorsOrFailIfInvalid(obj);
    }

    @Override
    public void delete(PK id) throws PSDataServiceException {
        validateIdParameter("delete", id);
        try {
            getDao().delete(id);
        } catch (DeleteException e) {
            var error = format("Error deleting object: {0}", id);
            log.error("Error: {}", e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new DataServiceDeleteException(error, e);
        }
    }

    @Override
    public FULL load(PK id)
            throws DataServiceLoadException, DataServiceNotFoundException, PSValidationException {
        validateIdParameter("load", id);
        try {
            var item = Optional.ofNullable(getDao().find(id))
                    .orElseThrow(() -> new DataServiceNotFoundException("Item not found: " + id));
            return item;
        } catch (PSDataServiceException e) {
            throw new DataServiceLoadException(e);
        }
    }

    @Override
    public FULL save(FULL object) throws PSDataServiceException {
        try {
            validate(object);
            return getDao().save(object);
        } catch (SaveException | LoadException | DeleteException e) {
            var error = format("Error saving object: {0}", object);
            throw new DataServiceSaveException(error, e);
        }
    }

    protected final IPSGenericDao<FULL, PK> getDao() {
        return dao;
    }

    protected void validateIdParameter(String action, PK id) throws PSValidationException {
        rejectIfNull(action, "id", id);
    }

    /**
     * Logger instance for this class, never {@code null}.
     */
    private static final Logger log = LogManager.getLogger(PSAbstractDataService.class);
}
