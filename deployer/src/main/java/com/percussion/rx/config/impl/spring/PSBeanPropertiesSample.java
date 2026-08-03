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
package com.percussion.rx.config.impl.spring;

import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A sample bean used to demonstrate how to use the custom XML bean to retrieve data from meta-data
 * service. An example is defined in <code>user-beans.xml</code>.
 *
 * @author YuBingChen
 */
public class PSBeanPropertiesSample {

  /** Default constructor for use by Spring. */
  public PSBeanPropertiesSample() {}

  /**
   * Returns the string property, which may be set by {@link #setStringValue(String)}.
   *
   * @return the string. It may be {@code null} or empty.
   */
  public String getStringValue() {
    return m_stringValue;
  }

  /**
   * Sets string property.
   *
   * @param stringValue the new string, it may be {@code null} or empty.
   */
  public void setStringValue(String stringValue) {
    ms_log.info("setStringValue(): {}", stringValue == null ? "null" : stringValue);
    m_stringValue = stringValue;
  }

  /**
   * Gets the integer property.
   *
   * @return the integer.
   */
  public int getIntValue() {
    return m_intValue;
  }

  /**
   * Sets the integer property.
   *
   * @param intValue the new integer.
   */
  public void setIntValue(int intValue) {
    ms_log.info("setIntValue(): {}", intValue);
    m_intValue = intValue;
  }

  /**
   * Gets the list property.
   *
   * @return the list, it may be {@code null} or empty.
   */
  public List<Object> getListValue() {
    return m_listValue;
  }

  /**
   * Sets the list property.
   *
   * @param listValue the new list property, it may be {@code null} or empty.
   */
  public void setListValue(List<Object> listValue) {
    ms_log.info("setListValue(): {}", listValue == null ? "null" : listValue);
    m_listValue = listValue;
  }

  /**
   * Gets the map property.
   *
   * @return the map, may be {@code null} or empty.
   */
  public Map<String, Object> getMapValue() {
    return m_mapValue;
  }

  /**
   * Sets the map property.
   *
   * @param mapValue the new map property, it may be {@code null} or empty.
   */
  public void setMapValue(Map<String, Object> mapValue) {
    ms_log.info("setMapValue(): {}", mapValue == null ? "null" : mapValue);
    m_mapValue = mapValue;
  }

  /** The integer property. */
  private int m_intValue;

  /** The string property. It may be {@code null} or empty. */
  private String m_stringValue;

  /** The list property. It may be {@code null} or empty. */
  private List<Object> m_listValue;

  /** The map property. It may be {@code null} or empty. */
  private Map<String, Object> m_mapValue;

  /** Logger for this class. */
  private static final Logger ms_log = LogManager.getLogger("PSPropertyValueBeanSample");
}
