// REFACTORED: CP-JAVA11
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
package com.percussion.services.contentmgr;

import com.percussion.cms.objectstore.PSComponentSummary;

import java.util.Map;
import java.util.Set;

/**
 * Modern Java 11 constants interface for content management node properties.
 *
 * <p>This interface defines common node property names used in JCR content management.
 * Properties are included here when they:
 * <ul>
 *   <li>Are special properties with no corresponding field in system definitions</li>
 *   <li>Require special handling due to mapping from {@link PSComponentSummary}</li>
 *   <li>Are core JCR or Rhythmyx system properties used across multiple components</li>
 * </ul>
 *
 * <p>All constants follow modern Java naming conventions and are organized by functional groups.
 * The interface provides both individual constants and convenient collections for bulk operations.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public interface IPSContentPropertyConstants {

    // JCR Standard Properties
    /**
     * The property name for the path (or a path) of a content item.
     * This is a standard JCR property used for node location identification.
     */
    String JCR_PATH = "jcr:path";

    /**
     * The synthetic property that is set for nodes which are checked out.
     * This is a standard JCR property indicating checkout status.
     */
    String JCR_IS_CHECKEDOUT = "jcr:isCheckedOut";

    // Rhythmyx System Properties - Content Identification
    /**
     * The property name for the revision id.
     * Used to track content item revision history.
     */
    String RX_SYS_REVISION = "rx:sys_revision";

    /**
     * The property name for the content id.
     * Primary identifier for content items in the system.
     */
    String RX_SYS_CONTENTID = "rx:sys_contentid";

    /**
     * The property name for the content type id.
     * Links content items to their type definitions.
     */
    String RX_SYS_CONTENTTYPEID = "rx:sys_contenttypeid";

    // Rhythmyx System Properties - Content Metadata
    /**
     * The last modified date for the content.
     * Tracks when content was last updated.
     */
    String RX_SYS_CONTENTLASTMODIFIEDATE = "rx:sys_contentlastmodifieddate";

    /**
     * Who modified the content last.
     * User identifier for the last content modifier.
     */
    String RX_SYS_CONTENTLASTMODIFIER = "rx:sys_contentlastmodifier";

    /**
     * The creation date.
     * Timestamp when content was originally created.
     */
    String RX_SYS_CONTENTCREATEDDATE = "rx:sys_contentcreateddate";

    /**
     * The author.
     * User identifier for the content creator.
     */
    String RX_SYS_CONTENTCREATEDBY = "rx:sys_contentcreatedby";

    /**
     * The post date of the content.
     * When content should be published or made available.
     */
    String RX_SYS_CONTENTPOSTDATE = "rx:sys_contentpostdate";

    // Rhythmyx System Properties - Organizational
    /**
     * The property name for the folder parent of a content item.
     * Links content to its containing folder.
     */
    String RX_SYS_FOLDERID = "rx:sys_folderid";

    /**
     * The community id, included here because it is an integer field.
     * Determines content visibility and access control.
     */
    String RX_SYS_COMMUNITYID = "rx:sys_communityid";

    /**
     * The object type, included here because it is an integer field.
     * Categorizes the type of content object.
     */
    String RX_SYS_OBJECTTYPE = "rx:sys_objecttype";

    // Rhythmyx System Properties - Workflow
    /**
     * The workflow state id, included here because it is an integer field.
     * Current state in the content workflow process.
     */
    String RX_SYS_CONTENTSTATEID = "rx:sys_contentstateid";

    /**
     * The workflow id, included here because it is an integer field.
     * Identifies which workflow controls this content.
     */
    String RX_SYS_WORKFLOWID = "rx:sys_workflowid";

    // Convenient Collections for Bulk Operations
    /**
     * All JCR standard properties used in content management.
     * Useful for filtering or processing standard JCR properties.
     */
    Set<String> JCR_PROPERTIES = Set.of(
            JCR_PATH,
            JCR_IS_CHECKEDOUT
    );

    /**
     * All Rhythmyx system properties for content identification.
     * Core properties that uniquely identify content items.
     */
    Set<String> RX_CONTENT_ID_PROPERTIES = Set.of(
            RX_SYS_CONTENTID,
            RX_SYS_REVISION,
            RX_SYS_CONTENTTYPEID
    );

    /**
     * All Rhythmyx system properties for content metadata.
     * Properties that track content lifecycle and authorship.
     */
    Set<String> RX_CONTENT_METADATA_PROPERTIES = Set.of(
            RX_SYS_CONTENTLASTMODIFIEDATE,
            RX_SYS_CONTENTLASTMODIFIER,
            RX_SYS_CONTENTCREATEDDATE,
            RX_SYS_CONTENTCREATEDBY,
            RX_SYS_CONTENTPOSTDATE
    );

    /**
     * All Rhythmyx system properties for organizational structure.
     * Properties that define content location and access control.
     */
    Set<String> RX_ORGANIZATIONAL_PROPERTIES = Set.of(
            RX_SYS_FOLDERID,
            RX_SYS_COMMUNITYID,
            RX_SYS_OBJECTTYPE
    );

    /**
     * All Rhythmyx system properties for workflow management.
     * Properties that control content workflow state and processing.
     */
    Set<String> RX_WORKFLOW_PROPERTIES = Set.of(
            RX_SYS_CONTENTSTATEID,
            RX_SYS_WORKFLOWID
    );

    /**
     * All defined property constants for comprehensive operations.
     * Combines all property categories into a single collection.
     */
    Set<String> ALL_PROPERTIES = Set.of(
            // JCR Properties
            JCR_PATH, JCR_IS_CHECKEDOUT,
            // Content ID Properties
            RX_SYS_CONTENTID, RX_SYS_REVISION, RX_SYS_CONTENTTYPEID,
            // Metadata Properties
            RX_SYS_CONTENTLASTMODIFIEDATE, RX_SYS_CONTENTLASTMODIFIER,
            RX_SYS_CONTENTCREATEDDATE, RX_SYS_CONTENTCREATEDBY, RX_SYS_CONTENTPOSTDATE,
            // Organizational Properties
            RX_SYS_FOLDERID, RX_SYS_COMMUNITYID, RX_SYS_OBJECTTYPE,
            // Workflow Properties
            RX_SYS_CONTENTSTATEID, RX_SYS_WORKFLOWID
    );

    /**
     * Property type mappings for validation and conversion operations.
     * Maps property names to their expected Java types.
     */
    Map<String, Class<?>> PROPERTY_TYPES = Map.ofEntries(
            // String properties
            Map.entry(JCR_PATH, String.class),
            Map.entry(RX_SYS_CONTENTLASTMODIFIER, String.class),
            Map.entry(RX_SYS_CONTENTCREATEDBY, String.class),
            // Boolean properties
            Map.entry(JCR_IS_CHECKEDOUT, Boolean.class),
            // Integer properties
            Map.entry(RX_SYS_CONTENTID, Integer.class),
            Map.entry(RX_SYS_REVISION, Integer.class),
            Map.entry(RX_SYS_CONTENTTYPEID, Integer.class),
            Map.entry(RX_SYS_FOLDERID, Integer.class),
            Map.entry(RX_SYS_COMMUNITYID, Integer.class),
            Map.entry(RX_SYS_OBJECTTYPE, Integer.class),
            Map.entry(RX_SYS_CONTENTSTATEID, Integer.class),
            Map.entry(RX_SYS_WORKFLOWID, Integer.class)
    );

    /**
     * Checks if a property name is a known content management property.
     *
     * @param propertyName the property name to check, must not be null
     * @return true if the property is defined in this interface, false otherwise
     * @throws IllegalArgumentException if propertyName is null
     */
    static boolean isKnownProperty(String propertyName) {
        if (propertyName == null) {
            throw new IllegalArgumentException("Property name cannot be null");
        }
        return ALL_PROPERTIES.contains(propertyName);
    }

    /**
     * Gets the expected Java type for a property name.
     *
     * @param propertyName the property name to check, must not be null
     * @return the expected Java type, or null if property is unknown
     * @throws IllegalArgumentException if propertyName is null
     */
    static Class<?> getPropertyType(String propertyName) {
        if (propertyName == null) {
            throw new IllegalArgumentException("Property name cannot be null");
        }
        return PROPERTY_TYPES.get(propertyName);
    }

    /**
     * Checks if a property is a JCR standard property.
     *
     * @param propertyName the property name to check, must not be null
     * @return true if the property is a JCR standard property, false otherwise
     * @throws IllegalArgumentException if propertyName is null
     */
    static boolean isJcrProperty(String propertyName) {
        if (propertyName == null) {
            throw new IllegalArgumentException("Property name cannot be null");
        }
        return JCR_PROPERTIES.contains(propertyName);
    }

    /**
     * Checks if a property is a Rhythmyx system property.
     *
     * @param propertyName the property name to check, must not be null
     * @return true if the property is a Rhythmyx system property, false otherwise
     * @throws IllegalArgumentException if propertyName is null
     */
    static boolean isRhythmyxProperty(String propertyName) {
        if (propertyName == null) {
            throw new IllegalArgumentException("Property name cannot be null");
        }
        return propertyName.startsWith("rx:sys_") && ALL_PROPERTIES.contains(propertyName);
    }
}
