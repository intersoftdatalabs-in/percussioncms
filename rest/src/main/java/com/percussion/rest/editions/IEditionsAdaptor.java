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

package com.percussion.rest.editions;

/**
 * Adaptor interface for publishing editions.
 *
 * <p>Sunny Sal: "Adaptor pattern FTW!"
 */
public interface IEditionsAdaptor {

  /**
   * Publishes the edition with the given id.
   *
   * @param id Edition id (must be numeric)
   * @return PublishResponse with status and job info
   */
  PublishResponse publish(String id);
}
