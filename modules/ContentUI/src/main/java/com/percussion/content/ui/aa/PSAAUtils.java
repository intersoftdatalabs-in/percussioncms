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
package com.percussion.content.ui.aa;

import com.percussion.design.objectstore.PSRelationship;
import com.percussion.error.PSException;
import java.util.Optional;
import com.percussion.error.PSMissingBeanConfigurationException;
import com.percussion.extension.IPSJexlMethod;
import com.percussion.extension.IPSJexlParam;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.relationship.IPSRelationshipService;
import com.percussion.services.relationship.PSRelationshipServiceLocator;
import com.percussion.system.utils.IPSHtmlParameters;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;

/**
 * A utility class with static methods for Active Assembly operations. This class is exposed to
 * Velocity macros and provides object identification functionality for pages, slots, and snippets.
 *
 * <p>This class follows Java 11 best practices with proper logging, parameter validation, and
 * exception handling.
 *
 * @since 8.1.6-SNAPSHOT
 */
public final class PSAAUtils {
  /** Logger for this class using log4j2 */
  private static final Logger logger = LogManager.getLogger(PSAAUtils.class);

  /** Private constructor to prevent instantiation of utility class */
  private PSAAUtils() {
    // Utility class - prevent instantiation
  }

  /**
   * Gets the page active assembly object id
   *
   * @see PSAAObjectId for further details.
   * @param item The current working item, must not be {@code null}.
   * @return String representation of JSONArray object id, never {@code null}.
   * @throws PSAssemblyException if assembly processing fails
   * @throws PSMissingBeanConfigurationException if required beans are not configured
   * @throws JSONException if JSON processing fails
   * @throws IllegalArgumentException if item is {@code null}
   */
  @IPSJexlMethod(
      description = "Creates active assembly object id for page and returns JSON string",
      params = {
        @IPSJexlParam(
            name = "item",
            type = "PSAssemblyWorkItem",
            description = "Current assembly item to look for the assembly parameters")
      },
      returns = "JSONArray string to uniquely identify the parent page")
  public static String getPageObjectId(IPSAssemblyItem item)
      throws PSAssemblyException, PSMissingBeanConfigurationException, JSONException {
    if (item == null) {
      logger.error("getPageObjectId called with null item");
      throw new IllegalArgumentException("item must not be null");
    }

    logger.debug("Creating page object ID for item: {}", item);
    PSAAObjectId objid = new PSAAObjectId(item);
    return objid.toString();
  }

  /**
   * Gets the slot active assembly object id
   *
   * @see PSAAObjectId for further details.
   * @param item The current working item, must not be {@code null}.
   * @param slotname The name of the slot, must not be {@code null} or empty.
   * @return objectid as a string, never {@code null} or empty.
   * @throws PSAssemblyException if assembly processing fails
   * @throws PSMissingBeanConfigurationException if required beans are not configured
   * @throws IllegalArgumentException if parameters are invalid
   * @throws JSONException if JSON processing fails
   */
  @IPSJexlMethod(
      description = "Creates active assembly object id for slot and returns JSON string",
      params = {
        @IPSJexlParam(
            name = "item",
            type = "PSAssemblyWorkItem",
            description = "Current assembly item to look for the assembly parameters"),
        @IPSJexlParam(name = "slotName", type = "String", description = "Slot name")
      },
      returns = "JSON object string to uniquely identify the slot on the page/snippet")
  public static String getSlotObjectId(IPSAssemblyItem item, String slotname)
      throws PSAssemblyException,
          PSMissingBeanConfigurationException,
          IllegalArgumentException,
          JSONException {
    if (item == null) {
      logger.error("getSlotObjectId called with null item");
      throw new IllegalArgumentException("item must not be null");
    }
    if (StringUtils.isBlank(slotname)) {
      logger.error("getSlotObjectId called with null or empty slotname");
      throw new IllegalArgumentException("slotname must not be null or empty");
    }

    logger.debug("Creating slot object ID for item: {} and slot: {}", item, slotname);
    PSAAObjectId objid = new PSAAObjectId(PSAANodeType.valueOf(1), item, slotname, null);
    return objid.toString();
  }

  /**
   * Gets the snippet active assembly object id.
   *
   * @see PSAAObjectId for further details.
   * @param item The current working item, must not be {@code null}.
   * @param slotname The name of the slot, must not be {@code null} or empty.
   * @return objectid as a string, never {@code null} or empty.
   * @throws PSAssemblyException if assembly processing fails
   * @throws PSMissingBeanConfigurationException if required beans are not configured
   * @throws IllegalArgumentException if parameters are invalid
   * @throws JSONException if JSON processing fails
   */
  @IPSJexlMethod(
      description = "Creates active assembly object id for snippet and returns JSON string",
      params = {
        @IPSJexlParam(
            name = "item",
            type = "PSAssemblyWorkItem",
            description = "Current assembly item to look for the assembly parameters"),
        @IPSJexlParam(name = "slotName", type = "String", description = "Slot name")
      },
      returns = "JSON object string to uniquely identify the snippet in a page")
  public static String getSnippetObjectId(IPSAssemblyItem item, String slotname)
      throws PSAssemblyException,
          PSMissingBeanConfigurationException,
          IllegalArgumentException,
          JSONException {
    if (item == null) {
      logger.error("getSnippetObjectId called with null item");
      throw new IllegalArgumentException("item must not be null");
    }
    if (StringUtils.isBlank(slotname)) {
      logger.error("getSnippetObjectId called with null or empty slotname");
      throw new IllegalArgumentException("slotname must not be null or empty");
    }

    String sortrank = "0";

    try {
      sortrank = getSortRank(item);
    } catch (NumberFormatException e) {
      logger.debug("Failed to parse sort rank, defaulting to 0", e);
    } catch (PSException e) {
      logger.debug("Failed to get sort rank, defaulting to 0", e);
    }

    logger.debug(
        "Creating snippet object ID for item: {} and slot: {} with sort rank: {}",
        item,
        slotname,
        sortrank);
    PSAAObjectId objid = new PSAAObjectId(PSAANodeType.valueOf(2), item, slotname, sortrank);
    return objid.toString();
  }

  /**
   * Extract the sort rank for the assembly item.
   *
   * @param item assembly item, must not be {@code null}.
   * @return sort rank of the assembly item as string, may be {@code null}.
   * @throws PSMissingBeanConfigurationException if relationship service could not be loaded.
   * @throws NumberFormatException if the relationshipid is not parsable as a number.
   * @throws PSException if relationship could not be loaded for any other reason.
   */
  private static String getSortRank(IPSAssemblyItem item)
      throws PSMissingBeanConfigurationException, PSException {
    if (item == null) {
      throw new IllegalArgumentException("item must not be null");
    }

    String sortrank = null;
    IPSRelationshipService relsvc = PSRelationshipServiceLocator.getRelationshipService();
    String relationshipId = item.getParameterValue(IPSHtmlParameters.SYS_RELATIONSHIPID, "");

    if (!StringUtils.isBlank(relationshipId)) {
      int relid = Integer.parseInt(relationshipId);
      // loadRelationship now returns Optional<PSRelationship> for null safety
      Optional<PSRelationship> optRel = relsvc.loadRelationship(relid);
      if (optRel.isPresent()) {
        PSRelationship rel = optRel.get();
        sortrank = rel.getProperty(IPSHtmlParameters.SYS_SORTRANK);

        if (StringUtils.isBlank(sortrank)) {
          sortrank = "0";
        } else {
          sortrank = sortrank.trim();
        }
      } else {
        // relationship not found, default to 0
        sortrank = "0";
      }
    }

    return sortrank;
  }

  /**
   * Gets the field active assembly object id
   *
   * @see PSAAObjectId for further details.
   * @param item The current working item, must not be {@code null}.
   * @param fieldName The name of the field, must not be {@code null} or empty.
   * @return String representation of JSONArray object id, never {@code null}.
   * @throws PSAssemblyException if assembly processing fails
   * @throws PSMissingBeanConfigurationException if required beans are not configured
   * @throws JSONException if JSON processing fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  @IPSJexlMethod(
      description = "Creates active assembly object id for field and returns JSON string",
      params = {
        @IPSJexlParam(
            name = "item",
            type = "PSAssemblyWorkItem",
            description = "Current assembly item to look for the assembly parameters"),
        @IPSJexlParam(name = "fieldName", type = "String", description = "Field name")
      },
      returns = "JSONArray string to uniquely identify the field")
  public static String getFieldObjectId(IPSAssemblyItem item, String fieldName)
      throws PSAssemblyException,
          PSMissingBeanConfigurationException,
          IllegalArgumentException,
          JSONException {
    if (item == null) {
      logger.error("getFieldObjectId called with null item");
      throw new IllegalArgumentException("item must not be null");
    }
    if (StringUtils.isBlank(fieldName)) {
      logger.error("getFieldObjectId called with null or empty fieldName");
      throw new IllegalArgumentException("fieldName must not be null or empty");
    }

    logger.debug("Creating field object ID for item: {} and field: {}", item, fieldName);
    PSAAObjectId objid = new PSAAObjectId(PSAANodeType.valueOf(3), item, fieldName, null);
    return objid.toString();
  }
}
