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

import com.percussion.dashboardmanagement.service.IPSGadgetService;
import com.percussion.share.service.exception.PSDataServiceException;
import java.io.Serializable;
import java.util.List;

/**
 * Represents the READ portion of a data service.
 *
 * @param <T> object type
 * @param <PK> object key
 * @author adamgent
 */
public interface IPSCatalogService<T, PK extends Serializable> {

  /**
   * Gets all objects of a particular type.
   *
   * @return list of populated objects
   * @throws PSDataServiceException
   * @throws IPSGadgetService.PSGadgetNotFoundException
   * @throws IPSGadgetService.PSGadgetServiceException
   */
  List<T> findAll()
      throws PSDataServiceException,
          IPSGadgetService.PSGadgetNotFoundException,
          IPSGadgetService.PSGadgetServiceException;

  /**
   * Gets an object by class and identifier.
   *
   * @param id the identifier (primary key) of the object to get
   * @return a populated object
   * @throws PSDataServiceException
   * @throws IPSGadgetService.PSGadgetNotFoundException
   * @throws IPSGadgetService.PSGadgetServiceException
   */
  T find(PK id)
      throws PSDataServiceException,
          IPSGadgetService.PSGadgetNotFoundException,
          IPSGadgetService.PSGadgetServiceException;
}
