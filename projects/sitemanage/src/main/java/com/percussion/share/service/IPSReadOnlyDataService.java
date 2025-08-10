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

import com.percussion.share.service.exception.PSDataServiceException;
import java.io.Serializable;

/**
 * Read-only data service for loading objects by id.
 *
 * @param <FULL> the full object type
 * @param <PK> the primary key type
 */
public interface IPSReadOnlyDataService<FULL, PK extends Serializable> {

  /**
   * Loads an object by id.
   *
   * @param id the primary key
   * @return the loaded object
   * @throws PSDataServiceException if the object cannot be loaded
   */
  FULL load(PK id) throws PSDataServiceException;
}
