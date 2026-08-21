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

import com.percussion.design.objectstore.PSLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.utils.guid.IPSGuid;
import java.util.List;

/**
 * Handles translation of IDs between {@link IPSGuid} and their respective string representations.
 */
public interface IPSIdMapper {

  /**
   * Returns a GUID from a content ID.
   *
   * @param id a valid content id
   * @return a valid guid for the supplied id
   */
  IPSGuid getGuidFromContentId(long id);

  /**
   * Performs string to {@link IPSGuid} translation.
   *
   * <p>Accepts a hyphenated {@code host-type-uuid} GUID, a packed long that already
   * carries a type, or a <strong>bare numeric content id</strong> (for example
   * {@code 594} from Explorer Preview {@code GET .../workflow/checkIn/594}). Bare
   * numeric tokens with no GUID type bits are mapped as {@link
   * PSTypeEnum#LEGACY_CONTENT} via {@link #getGuidFromContentId(long)}.
   *
   * @param id the string representation of the id, never blank
   * @return the id as an {@link IPSGuid}, never {@code null}
   */
  IPSGuid getGuid(String id);

  /**
   * Converts a guid string into a valid guid for the specified type.
   *
   * @param id string representation of a GUID
   * @param type the type of guid to create
   * @return a valid GUID
   */
  IPSGuid getGuid(String id, PSTypeEnum type);

  /**
   * Converts a guid string into a valid guid for the specified type.
   *
   * @param id string representation of a GUID
   * @param type the type of guid to create
   * @param forceType when true, the specified type will be forced on the returned guid
   * @return a valid GUID
   */
  IPSGuid getGuid(String id, PSTypeEnum type, boolean forceType);

  /**
   * Gets the content ID from an item GUID.
   *
   * @param guid the item GUID, not {@code null}
   * @return the content ID
   */
  int getContentId(IPSGuid guid);

  /**
   * Gets the content ID from the string representation of an item GUID.
   *
   * @param guid the string representation of an item GUID, not blank
   * @return the content ID
   */
  int getContentId(String guid);

  /**
   * Converts string to {@link IPSGuid} for an item ID.
   *
   * @param id the item ID, never blank
   * @return the item GUID with "correct" version number, never {@code null}
   */
  IPSGuid getItemGuid(String id);

  /**
   * Performs {@link IPSGuid} to string translation.
   *
   * @param id as an {@link IPSGuid}, never {@code null}
   * @return the string representation of the id, never {@code null}
   */
  String getString(IPSGuid id);

  /**
   * Calls {@link #getGuid(String)} for a list of strings.
   *
   * @param ids list of IDs in string representation, not {@code null}
   * @return a list of converted {@link IPSGuid} objects
   */
  List<IPSGuid> getGuids(List<String> ids);

  /**
   * Calls {@link #getString(IPSGuid)} for a list of {@link IPSGuid}
   *
   * @param ids list of IDs in {@link IPSGuid} objects, not {@code null}
   * @return a list of string representations of the IDs
   */
  List<String> getStrings(List<IPSGuid> ids);

  /**
   * Performs {@link IPSGuid} to string translation for the specified locator.
   *
   * @param locator the {@link PSLocator}, never {@code null}
   * @return the string representation of the locator's id, never {@code null}
   */
  String getString(PSLocator locator);

  /**
   * Performs {@link IPSGuid} translation for the specified locator.
   *
   * @param locator the {@link PSLocator}, never {@code null}
   * @return the id as an {@link IPSGuid}, never {@code null}
   */
  IPSGuid getGuid(PSLocator locator);

  /**
   * Performs string to {@link PSLocator} translation.
   *
   * @param id the string representation of the id, never blank
   * @return the id as a Locator, never {@code null}. Includes updated revision.
   */
  PSLocator getLocator(String id);

  /**
   * Performs {@link IPSGuid} to {@link PSLocator} translation.
   *
   * @param id as an {@link IPSGuid}, never {@code null}
   * @return the guid as a Locator, never {@code null}. Includes updated revision.
   */
  PSLocator getLocator(IPSGuid id);

  /**
   * Generates a new, unique id which can be used for local content items. The id is generated from
   * next number table's "PSX_LOCAL_CONTENT" value.
   *
   * @return the id, never {@code <= 0}
   */
  int getLocalContentId();
}
