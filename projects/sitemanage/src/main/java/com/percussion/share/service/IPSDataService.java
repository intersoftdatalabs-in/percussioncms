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
package com.percussion.share.service;

import com.percussion.dashboardmanagement.service.IPSGadgetService;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.service.exception.IPSNotFoundException;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSSpringValidationException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrors;

import java.io.Serializable;

/**
 * A generic service wrapper around a DAO.
 *
 * @param <FULL>    Full loaded object
 * @param <SUMMARY> The summary version of the object
 * @param <PK>      the primary key for that type
 */
public interface IPSDataService<FULL, SUMMARY, PK extends Serializable>
        extends
        IPSCatalogService<SUMMARY, PK>,
        IPSReadOnlyDataService<FULL, PK> {

    /**
     * Saves an object - handles both update and insert.
     *
     * @param object the object to save
     * @return the persisted object
     * @throws PSDataServiceException         if the object cannot be saved
     * @throws IPSGadgetService.PSGadgetServiceException if the gadget service fails
     */
    FULL save(FULL object) throws PSDataServiceException, IPSGadgetService.PSGadgetServiceException;

    /**
     * Deletes an object based on class and id.
     *
     * @param id the identifier (primary key) of the object to remove
     * @throws PSDataServiceException
     * @throws IPSGadgetService.PSGadgetNotFoundException
     * @throws IPSGadgetService.PSGadgetServiceException
     * @throws PSNotFoundException
     */
    void delete(PK id) throws PSDataServiceException, IPSGadgetService.PSGadgetNotFoundException,
            IPSGadgetService.PSGadgetServiceException, PSNotFoundException;

    /**
     * Validates the given object.
     *
     * @param object the object to validate
     * @return validation errors
     * @throws PSValidationException
     * @throws DataServiceSaveException
     */
    PSValidationErrors validate(FULL object) throws PSValidationException, DataServiceSaveException;

    /**
     * Exception thrown when a site cannot be saved successfully.
     */
    class DataServiceSaveException extends PSDataServiceException {
        private static final long serialVersionUID = 1L;

        public DataServiceSaveException() {
            super();
        }

        public DataServiceSaveException(String message, Throwable cause) {
            super(message, cause);
        }

        public DataServiceSaveException(String message) {
            super(message);
        }

        public DataServiceSaveException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Exception thrown when a site cannot be deleted successfully.
     */
    class DataServiceDeleteException extends PSDataServiceException {
        private static final long serialVersionUID = 1L;

        public DataServiceDeleteException() {
            super();
        }

        public DataServiceDeleteException(String message, Throwable cause) {
            super(message, cause);
        }

        public DataServiceDeleteException(String message) {
            super(message);
        }

        public DataServiceDeleteException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Exception thrown when a site cannot be loaded successfully.
     */
    class DataServiceLoadException extends PSDataServiceException {
        private static final long serialVersionUID = 1L;

        public DataServiceLoadException() {
            super();
        }

        public DataServiceLoadException(String message, Throwable cause) {
            super(message, cause);
        }

        public DataServiceLoadException(String message) {
            super(message);
        }

        public DataServiceLoadException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Exception thrown when a site cannot be found.
     */
    class DataServiceNotFoundException extends PSDataServiceException implements IPSNotFoundException {
        private static final long serialVersionUID = 1L;

        public DataServiceNotFoundException() {
            super();
        }

        public DataServiceNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }

        public DataServiceNotFoundException(String message) {
            super(message);
        }

        public DataServiceNotFoundException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Exception thrown when a theme is not found by the service.
     */
    class PSThemeNotFoundException extends DataServiceNotFoundException {
        private static final long serialVersionUID = 1L;

        public PSThemeNotFoundException() {
            super();
        }

        public PSThemeNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }

        public PSThemeNotFoundException(String message) {
            super(message);
        }

        public PSThemeNotFoundException(Throwable cause) {
            super(cause);
        }
    }
}
