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

package com.percussion.install;

import com.percussion.util.IPSBrandCodeConstants.ServerTypes;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Interface for brand code mapping. */
public interface IPSBrandCodeMap {

  /**
   * Determines whether the specified brand code map version is supported by this brand code map.
   *
   * @param bcmv the brand code map version
   * @return <code>true</code> if the specified brand code map version is supported, <code>false
   *     </code> otherwise
   */
  public boolean isValidBrandCodeMapVersion(int bcmv);

  /**
   * Determine if extended product info is supported by the specified map version. This means that
   * additional server types and evaluation periods may be specified.
   *
   * @param bcmv the brand code map version
   * @return <code>true</code> if supported, <code>false</code> if not.
   * @throws CodeException if the specified version is not valid.
   */
  public boolean supportsExtendedProductInfo(int bcmv) throws CodeException;

  /**
   * Returns an iterator over a list of brand code map versions.
   *
   * @return the iterator over a list containing brand code map version (<code>PSBrandCodeMapVersion
   *     </code>) objects, never <code>null</code>
   */
  public Iterator<PSBrandCodeMapVersion> getBrandCodeMapVersions();

  /**
   * Returns the brand code map version which will be used for generating the brand code, based on
   * the Rhythmyx version and build number.
   *
   * @param rxVersion the Rhythmyx version, may not be <code>null</code> or empty
   * @param buildFrom the minimum supported build number, should either be <code>-1</code> or a 8
   *     digit number
   * @param buildTo the maximum supported build number, should either be <code>-1</code> or a 8
   *     digit number. If not <code>-1</code>, then should be greater than <code>buildFrom</code>
   * @return the brand code map version which will be used for generating the brand code, based on
   *     the specified Rhythmyx version and build number.
   * @throws IllegalArgumentException if rxVersion is <code>null</code> or empty
   * @throws CodeException if any currentVersions/currentVersion element does not have the
   *     RhythmyxVersion or value attribute defined, or if no matching brand code map version is
   *     found for the specified Rhythmyx version and build number.
   */
  public int getBrandCodeMapVersion(String rxVersion, int buildFrom, int buildTo)
      throws CodeException;

  /**
   * Returns the map of licenses, the id of the license is the key and the license name is the
   * value.
   *
   * @param brandCodeMapVersion the brand code map version to use, should be greater than 0, and the
   *     code map version should be defined in the component map Xml document.
   * @return a map containing the id of the license as key and license name as value, never <code>
   *     null</code>
   * @throws CodeException if any license element does not have name or id attribute defined.
   */
  public Map<String, String> getLicenses(int brandCodeMapVersion) throws CodeException;

  /**
   * Returns the name of the license, corresponding to the specified brand code map version and
   * license id.
   *
   * @param brandCodeMapVersion the brand code map version to use
   * @param licenseId id of the license whose name is required
   * @return the name of the license, never <code>null</code> or empty
   * @throws CodeException if any error occurs obtaining the name of the specified license.
   */
  public String getLicenseName(int brandCodeMapVersion, int licenseId) throws CodeException;

  /**
   * Get the list of types the specified license should be limited to.
   *
   * @param brandCodeMapVersion The brand code map version to use.
   * @param licenseId The id of the license to check
   * @return The list, never <code>null</code>, empty if all types are valid for the license.
   * @throws CodeException if the license id is not valid.
   */
  public List<ServerTypes> getLimitedServerTypes(int brandCodeMapVersion, int licenseId)
      throws CodeException;

  /**
   * Returns the name of the part, corresponding to the specified brand code map version and part
   * id.
   *
   * @param brandCodeMapVersion the brand code map version to use
   * @param partId id of the part whose name is required
   * @return the name of the part, never <code>null</code> or empty
   * @throws CodeException if any error occurs obtaining the name of the specified part.
   */
  public String getPartName(int brandCodeMapVersion, int partId) throws CodeException;

  /**
   * Returns the map of required or optional parts for a specified license, the id of the part is
   * the key and the part name is the value.
   *
   * @param brandCodeMapVersion the brand code map version to use, should be greater than 0, and the
   *     code map version should be defined in the component map Xml document.
   * @param licenseId the id of the license for which the map of parts is to be returned
   * @param partsType the type of part, must be one of the following values: <code>
   *     PARTS_TYPE_REQUIRED</code> <code>PARTS_TYPE_OPTIONAL_SELECTED</code> <code>
   *     PARTS_TYPE_OPTIONAL_UNSELECTED</code> <code>PARTS_TYPE_OPTIONAL</code>
   * @return a map containing the id of the part as key and part name as value, never <code>null
   *     </code>
   * @throws CodeException if the specified license element could not be found for the specified
   *     brand code map version
   * @throws IllegalArgumentException if <code>partsType</code> is invalid
   */
  public Map<String, String> getParts(int brandCodeMapVersion, int licenseId, int partsType)
      throws CodeException;

  /**
   * Returns a map containing the components id as key and component name as value, based on the
   * specified brand code map version, license id and part ids in the partsIdList.
   *
   * @param brandCodeMapVersion the brand code map version to use, should be greater than 0, and the
   *     code map version should be defined in the component map Xml document.
   * @param partsIdList list containing the id of parts, may not be <code>null</code>
   * @return map containing the components id as key and component name as value
   * @throws CodeException if any error occurs retrieving the list of components
   * @throws IllegalArgumentException if partsIdList is <code>null</code>
   */
  public Map<String, String> getComponents(int brandCodeMapVersion, List<String> partsIdList)
      throws CodeException;

  /**
   * Returns the map of properties, the id of the property is the key and the property name is the
   * value.
   *
   * @return a map containing the id of the property as key and property name as value, never <code>
   *     null</code>
   * @throws CodeException if any property element does not have name or id attribute defined.
   */
  public Map<String, String> getProperties() throws CodeException;

  /**
   * Returns a list of property ids corresponding to the properties supported by the license
   * specified by <code>licenseId</code>
   *
   * @param brandCodeMapVersion the brand code map version to use, should be greater than 0, and the
   *     code map version should be defined in the component map Xml document.
   * @param licenseId the id of the license whose properties is to be obtained
   * @return the list of property ids (<code>String</code>), never <code>null</code>, may be empty
   * @throws CodeException if the specified license element could not be found
   */
  public List<String> getLicenseProperties(int brandCodeMapVersion, int licenseId)
      throws CodeException;

  /**
   * Returns the list of valid Rhythmyx versions.
   *
   * @return the list of valid Rhythmyx versions, never <code>null</code>
   * @throws CodeException if any error occurs retrieving the list of Rhythmyx Versions
   */
  public List<String> getRhythmyxVersions() throws CodeException;

  /** Name of the component map XML file. */
  public static final String COMPONENT_MAP_FILE = "ComponentMap.xml";

  /** Constants for the Xml Elements and Attributes in the component map xml */
  public static final String EL_COMPONENT_MAP = "componentmap";

  /** Element name for the list of current versions. */
  public static final String EL_CURRENT_VERSIONS = "currentVersions";

  /** Element name for a single current version. */
  public static final String EL_CURRENT_VERSION = "currentVersion";

  /** Element name for the components collection. */
  public static final String EL_COMPONENTS = "components";

  /** Element name for a single component. */
  public static final String EL_COMPONENT = "component";

  /** Element name for the properties collection. */
  public static final String EL_PROPERTIES = "properties";

  /** Element name for a single property. */
  public static final String EL_PROPERTY = "property";

  /** Element name for a brand-code map. */
  public static final String EL_MAP = "map";

  /** Element name for the supported versions collection. */
  public static final String EL_SUPPORTED_VERSIONS = "supportedVersions";

  /** Element name for a single Rhythmyx version. */
  public static final String EL_RX_VERSION = "rxversion";

  /** Element name for the parts collection. */
  public static final String EL_PARTS = "parts";

  /** Element name for a single part. */
  public static final String EL_PART = "part";

  /** Element name for the licenses collection. */
  public static final String EL_LICENSES = "licenses";

  /** Element name for a single license. */
  public static final String EL_LICENSE = "license";

  /** Attribute name carrying a string value. */
  public static final String ATTR_VALUE = "value";

  /** Attribute name carrying a display name. */
  public static final String ATTR_NAME = "name";

  /** Attribute name carrying the numeric id. */
  public static final String ATTR_ID = "id";

  /** Attribute name carrying the Rhythmyx version. */
  public static final String ATTR_RHYTHMYX_VERSION = "RhythmyxVersion";

  /** Attribute name carrying the version. */
  public static final String ATTR_VERSION = "version";

  /** Attribute name carrying the minimum supported build number. */
  public static final String ATTR_BUILD_FROM = "buildFrom";

  /** Attribute name carrying the maximum supported build number. */
  public static final String ATTR_BUILD_TO = "buildTo";

  /** Attribute name carrying the owning component id. */
  public static final String ATTR_COMPONENT_ID = "componentid";

  /** Attribute name carrying the required part id. */
  public static final String ATTR_REQUIRED_PART_ID = "requiredpartid";

  /** Attribute name carrying the selected optional part id. */
  public static final String ATTR_SELECTED_OPTIONAL_PART_ID = "optionalpartidselected";

  /** Attribute name carrying the unselected optional part id. */
  public static final String ATTR_UNSELECTED_OPTIONAL_PART_ID = "optionalpartidunselected";

  /** Attribute name carrying the properties id. */
  public static final String ATTR_PROPERTIES_ID = "propertiesid";

  /** Attribute name carrying the deprecated version marker. */
  public static final String ATTR_DEPRECATED = "deprecatedVersion";

  /** Attribute name carrying the extended-product-info support flag. */
  public static final String ATTR_SUPPORTS_EXTENDED_PRODUCT_INFO = "supportsExtendedProductInfo";

  /** Attribute name carrying the limited server types. */
  public static final String ATTR_LIMITED_SERVER_TYPES = "limitedServerTypes";

  /** Required attributes for a {@link #EL_COMPONENT} element. */
  public static final String[] REQ_ATTRIBUTES_EL_COMPONENT = {ATTR_NAME, ATTR_ID};

  /** Required attributes for a {@link #EL_PROPERTY} element. */
  public static final String[] REQ_ATTRIBUTES_EL_PROPERTY = {ATTR_NAME, ATTR_ID};

  /** Required attributes for a {@link #EL_RX_VERSION} element. */
  public static final String[] REQ_ATTRIBUTES_EL_RX_VERSION = {
    ATTR_VALUE, ATTR_BUILD_FROM, ATTR_BUILD_TO
  };

  /** Required attributes for a {@link #EL_PART} element. */
  public static final String[] REQ_ATTRIBUTES_EL_PART = {ATTR_NAME, ATTR_ID, ATTR_COMPONENT_ID};

  /** Optional attributes for a {@link #EL_PART} element. */
  public static final String[] OPT_ATTRIBUTES_EL_PART = {ATTR_DEPRECATED};

  /** Required attributes for a {@link #EL_LICENSE} element. */
  public static final String[] REQ_ATTRIBUTES_EL_LICENSE = {ATTR_NAME, ATTR_ID, ATTR_PROPERTIES_ID};

  /** Optional attributes for a {@link #EL_LICENSE} element. */
  public static final String[] OPT_ATTRIBUTES_EL_LICENSE = {
    ATTR_REQUIRED_PART_ID,
    ATTR_SELECTED_OPTIONAL_PART_ID,
    ATTR_UNSELECTED_OPTIONAL_PART_ID,
    ATTR_LIMITED_SERVER_TYPES
  };

  /** Required attributes for a {@link #EL_CURRENT_VERSION} element. */
  public static final String[] REQ_ATTRIBUTES_EL_CURRENT_VERSION = {
    ATTR_RHYTHMYX_VERSION, ATTR_VALUE
  };

  /** Constants for the type of parts */
  public static final int PARTS_TYPE_REQUIRED = 1;

  /** Bit flag: the optional parts that have been selected by the customer. */
  public static final int PARTS_TYPE_OPTIONAL_SELECTED = 2;

  /** Bit flag: the optional parts that have not been selected by the customer. */
  public static final int PARTS_TYPE_OPTIONAL_UNSELECTED = 4;

  /** Bit flag: all optional parts, whether selected or not. */
  public static final int PARTS_TYPE_OPTIONAL = 6;

  /** Bit flag: every part type (required and optional combined). */
  public static final int PARTS_TYPE_ALL = 7;
}
