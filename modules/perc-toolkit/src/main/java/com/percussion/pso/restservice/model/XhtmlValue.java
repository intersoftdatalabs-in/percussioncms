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

<<<<<<< HEAD
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
=======
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
>>>>>>> development-8.1.x
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** */
@XmlRootElement(name = "XmlValue")
public class XhtmlValue implements Value {
  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(XhtmlValue.class);

  /** Field stringValue. */
  private String stringValue;
<<<<<<< HEAD

  /** Field href. */
  private String href;

  /** Field type. */
  public static final int TYPE = 1;

=======
  /** Field href. */
  private String href;
  /** Field type. */
  public static final int TYPE = 1;

>>>>>>> development-8.1.x
  /**
   * Method setStringValue.
   *
   * @param stringValue String
   * @see Value#setStringValue(String)
   */
  public void setStringValue(String stringValue) {
    this.stringValue = stringValue;
  }
<<<<<<< HEAD

=======
>>>>>>> development-8.1.x
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
<<<<<<< HEAD

=======
>>>>>>> development-8.1.x
  /**
   * Method setHref.
   *
   * @param href String
   */
  public void setHref(String href) {
    this.href = href;
  }
<<<<<<< HEAD

=======
>>>>>>> development-8.1.x
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
