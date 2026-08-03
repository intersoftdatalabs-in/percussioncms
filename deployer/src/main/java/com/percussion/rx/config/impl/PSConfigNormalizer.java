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

package com.percussion.rx.config.impl;

import com.percussion.rx.config.impl.jaxb.Pair;
import com.percussion.rx.config.impl.jaxb.Property;
import com.percussion.rx.config.impl.jaxb.PropertySet;
import com.percussion.rx.config.impl.jaxb.SolutionConfigurations;
import com.percussion.utils.types.PSPair;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class used for normalizing the Percussion CM System configuration files and merging the
 * normalized properties. The configuration files are in XML and defined by the XML schema file
 * <code>localConfig.xsd</code>.
 */
public class PSConfigNormalizer {

  /** Default constructor for use by Spring. */
  public PSConfigNormalizer() {}

  /**
   * Normalizes the supplied configuration file.
   *
   * @param in Input stream corresponding to the config file. Must not be null. Assumes the caller
   *     closes the stream.
   * @return Map of name-value pairs of normalized properties, may be empty but never null.
   * @throws JAXBException if error occurs.
   */
  public Map<String, Object> getNormalizedMap(InputStream in) throws JAXBException {
    return getNormalizedMap(in, true);
  }

  /**
   * This is the same as {@link #getNormalizedMap(InputStream)}, except it has option to include the
   * fully qualified names for all the map values.
   *
   * @param in Input stream corresponding to the config file. Must not be null. Assumes the caller
   *     closes the stream.
   * @param resolveValueMap true if the returned map includes the fully qualified names for all the
   *     map values.
   * @return Map of name-value pairs of normalized properties, may be empty but never null.
   * @throws JAXBException if error occurs.
   */
  public Map<String, Object> getNormalizedMap(InputStream in, boolean resolveValueMap)
      throws JAXBException {
    Objects.requireNonNull(in, "InputStream must not be null");
    var sc = getSolutionConfigurations(in);
    var result = solConfToNormMap(sc);
    if (resolveValueMap) {
      var tgtMap = new HashMap<String, Object>(result);
      appendFQNames(tgtMap, null, result);
      result = tgtMap;
    }
    return result;
  }

  private void appendFQNames(
      Map<String, Object> tgtMap, String prefix, Map<String, Object> srcMap) {
    for (var k : srcMap.keySet()) {
      var key = prefix == null ? k : prefix + "." + k;
      var value = srcMap.get(k);
      if (prefix != null) {
        tgtMap.put(key, value);
      }
      if (value instanceof Map) {
        appendFQNames(tgtMap, key, (Map<String, Object>) value);
      }
    }
  }

  /**
   * Unmarshalls config file.
   *
   * @param is Input stream corresponding to the config file. Must not be null. Assumes the caller
   *     closes the stream.
   * @return unmarshalled SolutionConfigurations
   * @throws JAXBException if error occurs.
   */
  private SolutionConfigurations getSolutionConfigurations(InputStream is) throws JAXBException {
    var jc = JAXBContext.newInstance("com.percussion.rx.config.impl.jaxb");
    var unmarshaller = jc.createUnmarshaller();
    return (SolutionConfigurations) unmarshaller.unmarshal(is);
  }

  /**
   * Processes the "Solution Configurations" within the configuration file and returns a normalized
   * map of Property Name to Property value.
   *
   * @param sc The JAXB Collection of "Solution Configurations".
   * @return Map of name-value pairs of normalized properties, may be empty but never null.
   */
  private Map<String, Object> solConfToNormMap(SolutionConfigurations sc) {
    Map<String, Object> nameMap = new HashMap<>();
    var solConfList = sc.getSolutionConfig();
    for (var solConf : solConfList) {
      var propOrPropSetList = solConf.getPropertyOrPropertySet();
      var prefix = sc.getPublisherPrefix().trim() + "." + solConf.getName().trim();
      nameMap = processPropertyOrPropertySetList(propOrPropSetList, prefix, nameMap);
    }
    return nameMap;
  }

  /**
   * Process a list of Property or PropertySet elements from the Deployer configuration file. Adds
   * the elements found to the provided property map.
   *
   * @param propOrPropSetList The list of Property or PropertySet elements to be processed.
   * @param prefix The prefix of the property key under which these Property or PropertySet elements
   *     should be stored. May not be null, but may be empty.
   * @param map The Map into which to store the Property data. May be null.
   * @return The map with the property data. May be null.
   */
  private Map<String, Object> processPropertyOrPropertySetList(
      List<Object> propOrPropSetList, String prefix, Map<String, Object> map) {
    Objects.requireNonNull(propOrPropSetList, "propOrPropSetList may not be null");
    if (map == null) {
      map = new HashMap<>();
    }
    m_tagLevel++;
    for (var obj : propOrPropSetList) {
      if (obj == null) {
        continue;
      }
      if (obj instanceof Property) {
        map = processProperty((Property) obj, prefix, map);
      } else if (obj instanceof PropertySet) {
        var propSet = (PropertySet) obj;
        boolean isValid = isValidPropertySet(propSet, propOrPropSetList, prefix);
        if (!isValid) {
          continue;
        }
        map = processPropertySet(propSet, m_tagLevel == 1 ? prefix : null, map);
      }
    }
    m_tagLevel--;
    return map;
  }

  /** Validates the given propertySet. Make sure it has non-blank name if it is at the 1st level. */
  private boolean isValidPropertySet(
      PropertySet propSet, List<Object> propOrPropSetList, String prefix) {
    var psName = propSet.getName();
    var hasSiblingProperties = checkForSiblingProperties(propOrPropSetList);
    if (psName == null) {
      if (m_tagLevel <= 1) {
        ms_log.warn(
            "PropertySet [{}] at first tag level must have a name. This PropertySet has been"
                + " ignored.",
            prefix);
        return false;
      }
      if (hasSiblingProperties) {
        ms_log.warn(
            "PropertySet [{}] whose siblings are Properties must have a name. This PropertySet has"
                + " been ignored.",
            prefix);
        return false;
      }
    }
    return true;
  }

  /**
   * Process a PropertySet element of the Deployer configuration file. Adds the elements found to
   * the provided property map.
   */
  private Map<String, Object> processPropertySet(
      PropertySet propertySet, String prefix, Map<String, Object> map) {
    Objects.requireNonNull(propertySet, "propertySet may not be null");
    var returnMap = map == null ? new HashMap<String, Object>() : map;
    var propKey = getPropKey(prefix, propertySet.getName());
    var propOrPropSet = propertySet.getPropertySetOrProperty();
    var valueObj = getPropertySetValue(propOrPropSet);
    returnMap.put(propKey, valueObj);
    return returnMap;
  }

  /** Gets a property key from a prefix and a property name. */
  private String getPropKey(String prefix, String propName) {
    propName = propName != null ? propName.trim() : "";
    return StringUtils.isBlank(prefix) ? propName : prefix + "." + propName;
  }

  /**
   * Process a Property element of the Deployer configuration file. Adds the elements found to the
   * provided property map.
   */
  private Map<String, Object> processProperty(
      Property prop, String prefix, Map<String, Object> map) {
    Objects.requireNonNull(prop, "prop may not be null");
    var returnMap = map == null ? new HashMap<String, Object>() : map;
    var propName = prop.getName();
    propName = propName != null ? propName.trim() : "";
    var propKey = getPropKey(prefix, propName);
    var value = prop.getValue();
    var propValue = prop.getPvalue();
    Property.Pvalues values;
    List<PropertySet> propertySetList;

    if (value != null && propValue == null) {
      returnMap.put(propKey, value);
    } else if (propValue != null) {
      returnMap.put(propKey, propValue);
    } else if ((values = prop.getPvalues()) != null) {
      var valuesObj = getValues(values);
      returnMap.put(propKey, valuesObj);
    } else if ((propertySetList = prop.getPropertySet()) != null && !propertySetList.isEmpty()) {
      var objectList = new ArrayList<Object>(propertySetList);
      var valueObj = getPropertySetValue(objectList);
      returnMap.put(propKey, valueObj);
    } else {
      returnMap.put(propKey, null);
    }
    return returnMap;
  }

  /** Converts a list of propertySet(s) to a list or a map. */
  private Object getPropertySetValue(List<Object> propSetList) {
    var flags = checkForPropertySets(propSetList);
    var allSiblingsArePropertySets = flags[0];
    var allSiblingsHaveNames = flags[1];
    if (allSiblingsArePropertySets && !allSiblingsHaveNames) {
      return createPropSetNameList(propSetList);
    } else {
      return processPropertyOrPropertySetList(propSetList, null, null);
    }
  }

  /**
   * Converts the {@link Property.Pvalues} object. It may contain a list of pvalue or a list of
   * pairs.
   */
  private Object getValues(Property.Pvalues values) {
    List<String> valuesList;
    List<Pair> pairList;
    if ((valuesList = values.getPvalue()) != null && !valuesList.isEmpty()) {
      return new ArrayList<>(valuesList);
    } else if ((pairList = values.getPair()) != null && !pairList.isEmpty()) {
      return getPairList(pairList);
    }
    return Collections.emptyList();
  }

  /** Converts the list of pair (from Jaxb) to a list of {@link PSPair} */
  private List<PSPair<String, String>> getPairList(List<Pair> pairList) {
    var newPairs = new ArrayList<PSPair<String, String>>();
    for (var xpair : pairList) {
      var npair = new PSPair<String, String>();
      var pvalue1 = xpair.getPvalue1();
      var pvalue2 = xpair.getPvalue2();
      String firstVal;
      String secondVal;
      if (pvalue1 != null && pvalue2 != null) {
        firstVal = pvalue1;
        secondVal = pvalue2;
      } else {
        firstVal = xpair.getValue1();
        secondVal = xpair.getValue2();
      }
      if (firstVal == null && secondVal == null) {
        firstVal = "";
        secondVal = "";
      }
      npair.setFirst(firstVal);
      npair.setSecond(secondVal);
      newPairs.add(npair);
    }
    return newPairs;
  }

  /** Tests if any members of the provided list are instances of "Property". */
  private Boolean checkForSiblingProperties(List<Object> siblingList) {
    Objects.requireNonNull(siblingList, "siblingList may not be null");
    for (var sibling : siblingList) {
      if (sibling instanceof Property) {
        return true;
      }
    }
    return false;
  }

  /** Tests if all members of the provided list are instances of "PropertySet". */
  private Boolean[] checkForPropertySets(List<Object> objectList) {
    Objects.requireNonNull(objectList, "objectList may not be null");
    var allObjectsArePropSets = !objectList.isEmpty();
    var allObjectsHaveNames = true;
    for (var object : objectList) {
      if (!(object instanceof PropertySet)) {
        allObjectsArePropSets = false;
      } else if (StringUtils.isBlank(((PropertySet) object).getName())) {
        allObjectsHaveNames = false;
      }
    }
    return new Boolean[] {allObjectsArePropSets, allObjectsHaveNames};
  }

  /**
   * Creates a List of Maps of each of the (child) properties in each of the PropertySets in the
   * given list.
   */
  private List<Object> createPropSetNameList(List<Object> childPropSetList) {
    var listOfPropSetMaps = new ArrayList<Object>();
    for (var childPropSet : childPropSetList) {
      if (!(childPropSet instanceof PropertySet)) {
        return listOfPropSetMaps;
      }
      var propSetOrPropList = ((PropertySet) childPropSet).getPropertySetOrProperty();
      var propSetOrPropMap = processPropertyOrPropertySetList(propSetOrPropList, null, null);
      listOfPropSetMaps.add(propSetOrPropMap);
    }
    return listOfPropSetMaps;
  }

  // The "tag level" is the embedded depth at which the tag occurs.
  private int m_tagLevel = 0;

  /** Reference to Log4j singleton object used to log any errors or debug info. */
  private static final Logger ms_log =
      LogManager.getLogger("com.percussion.rx.config.impl.PSConfigNormalizer");
}
