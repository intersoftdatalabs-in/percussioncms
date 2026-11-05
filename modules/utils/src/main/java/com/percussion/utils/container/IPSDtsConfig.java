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

package com.percussion.utils.container;

/**
 * Configuration for the Delivery Tier Service (DTS).
 */
public interface IPSDtsConfig {
  /**
   * Whether local staging DTS is enabled.
   *
   * @return <code>true</code> if enabled, <code>false</code> otherwise.
   */
  boolean isLocalStagingDTSEnabled();

  /**
   * Whether local DTS is enabled.
   *
   * @return <code>true</code> if enabled, <code>false</code> otherwise.
   */
  boolean isLocalDTSEnabled();

  /**
   * Get the DTS connector info.
   *
   * @return the connector info, may be <code>null</code>.
   */
  PSAbstractConnectors getDtsConnectorInfo();

  /**
   * Get the staging DTS connector info.
   *
   * @return the connector info, may be <code>null</code>.
   */
  PSAbstractConnectors getStagingDtsConnectorInfo();
}