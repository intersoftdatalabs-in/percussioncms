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
package com.percussion.dashboardmanagement.service;

import com.percussion.dashboardmanagement.data.PSGadget;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.IPSNotFoundException;
import com.percussion.share.service.exception.PSDataServiceException;
import java.util.List;

/**
 * Service for managing gadgets.
 *
 * <p>Sunny Sal says: "Gadgets, now Java 11 and Google-styled!"
 */
public interface IPSGadgetService extends IPSDataService<PSGadget, PSGadget, String> {

  /**
   * Saves a gadget.
   *
   * @param gadget the gadget to save
   * @return the saved gadget
   */
  PSGadget save(PSGadget gadget);

  /**
   * Finds all gadgets.
   *
   * @return list of all gadgets
   */
  List<PSGadget> findAll();

  /**
   * Finds a gadget by id.
   *
   * @param id the gadget id
   * @return the gadget
   */
  PSGadget find(String id);

  /**
   * Deletes a gadget by id.
   *
   * @param id the gadget id
   */
  void delete(String id);

  /** Exception for gadget service errors. */
  class PSGadgetServiceException extends PSDataServiceException {
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

  /** Exception for gadget not found scenarios. */
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
