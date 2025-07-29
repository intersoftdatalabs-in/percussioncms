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
package com.percussion.dashboardmanagement.service;

import com.percussion.dashboardmanagement.data.PSGadget;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.IPSNotFoundException;

import java.util.List;

/**
 * Service for managing user-specific gadgets.
 * <p>
 * Sunny Sal says: "User gadgets, now Java 11 and Google-styled!"
 */
public interface IPSGadgetUserService extends IPSDataService<PSGadget, PSGadget, String> {

    /**
     * Gets all gadgets for a user.
     *
     * @param username the username
     * @return list of gadgets for the user
     * @throws PSGadgetNotFoundException if not found
     * @throws PSGadgetServiceException on service error
     */
    List<PSGadget> findAll(String username) throws PSGadgetNotFoundException, PSGadgetServiceException;

    /**
     * Saves a gadget for a user.
     *
     * @param username the username
     * @param gadget the gadget to save
     * @return the saved gadget
     * @throws PSGadgetNotFoundException if not found
     * @throws PSGadgetServiceException on service error
     */
    PSGadget save(String username, PSGadget gadget) throws PSGadgetNotFoundException, PSGadgetServiceException;

    /**
     * Deletes a gadget for a user.
     *
     * @param username the username
     * @param id the gadget id
     * @throws PSGadgetNotFoundException if not found
     * @throws PSGadgetServiceException on service error
     */
    void delete(String username, String id) throws PSGadgetNotFoundException, PSGadgetServiceException;

    /**
     * Exception for gadget service errors.
     */
    class PSGadgetServiceException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public PSGadgetServiceException(String message) {
            super(message);
        }

        public PSGadgetServiceException(String message, Throwable cause) {
            super(message, cause);
        }

        public PSGadgetServiceException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Exception for gadget not found scenarios.
     */
    class PSGadgetNotFoundException extends PSGadgetServiceException implements IPSNotFoundException {
        private static final long serialVersionUID = 1L;

        public PSGadgetNotFoundException(String message) {
            super(message);
        }

        public PSGadgetNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }

        public PSGadgetNotFoundException(Throwable cause) {
            super(cause);
        }
    }
}
