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
package com.percussion.delivery.utils.properties;

import java.util.List;

/**
 * Defines an interface for services that provide configurable
 * properties for editing in the front-end.
 *
 * @author natechadwick
 */
public interface IPSPropertyDefinitionProvider {

  /**
   * Returns the property groups for this provider.
   *
   * @return list of property group definitions, never null
   */
  List<PSPropertyGroupDefinition> getPropertyGroups();

  /**
   * Sets the property groups for this provider.
   *
   * @param groups list of property group definitions
   */
  void setPropertyGroups(List<PSPropertyGroupDefinition> groups);
}
