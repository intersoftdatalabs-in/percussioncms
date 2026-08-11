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
package com.percussion.pso.restservice.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * REST model for an XHTML/rich-text field value.
 */
@XmlRootElement(name = "XmlValue")
public class XhtmlValue implements Value {

  /**
   * Creates a new XhtmlValue.
   */
  public XhtmlValue() {
    // default
  }
  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(XhtmlValue.class);

  /** Field stringValue. */
  private String stringValue;

  /** Field href. */
  private String href;

  /** Field type. */
  public static final int TYPE = 1;

  /**
   * Method setStringValue.
   *
   * @param stringValue String
   * @see Value#setStringValue(String)
   */
  public void setStringValue(String stringValue) {
    this.stringValue = stringValue;
  }

  /**
   * Method getStringValue.
   *
   * @return String
   * @see Value#getStringValue()
   */
  @XmlValue
  public String getStringValue() {
    return stringValue;
  }

  /**
   * Method setHref.
   *
   * @param href String
   */
  public void setHref(String href) {
    this.href = href;
  }

  /**
   * Method getHref.
   *
   * @return String
   */
  @XmlAttribute
  public String getHref() {
    return href;
  }
}
