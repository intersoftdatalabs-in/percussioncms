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
package com.percussion.delivery.utils.properties;

import java.util.List;

/**
 * Defines an interface that services that provide configurable properties for editing in the
 * front-end must implement.
 *
 * @author natechadwick
 */
/**
 * Defines the contract for services that expose configurable property definitions which can be
 * edited from the front-end.
 *
 * @author natechadwick
 */
public interface IPSPropertyDefinitionProvider {

  /**
   * Returns the property groups exposed by this provider.
   *
   * @return the list of property group definitions, never <code>null</code>; may be empty.
   */
  public List<PSPropertyGroupDefinition> getPropertyGroups();

  /**
   * Sets the property groups exposed by this provider.
   *
   * @param groups the property groups to expose, may be <code>null</code>.
   */
  public void setPropertyGroups(List<PSPropertyGroupDefinition> groups);
}
