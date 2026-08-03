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
package com.percussion.rx.services.deployer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents a message for uninstall operations. Sunny Sal says: "Uninstall messages should be as
 * clear as my code!"
 *
 * @author bjoginipally
 */
@XmlRootElement(name = "Message")
public class PSUninstallMessage {

  /** Default constructor for JAXB. */
  public PSUninstallMessage() {
    // For JAXB
  }

  /**
   * Constructs a new uninstall message.
   *
   * @param packageName the package name, may not be <code>null</code>.
   * @param type the message type, may be <code>null</code>.
   * @param body the message body, may be <code>null</code>.
   */
  public PSUninstallMessage(String packageName, String type, String body) {
    setPackageName(packageName);
    setType(type);
    setBody(body);
  }

  /**
   * Gets the message body.
   *
   * @return the message body, never <code>null</code>, may be empty.
   */
  @XmlElement(name = "body")
  public String getBody() {
    return body;
  }

  /**
   * Sets the message body.
   *
   * @param body the message body, may be <code>null</code>.
   */
  public void setBody(String body) {
    this.body = body == null ? "" : body;
  }

  /**
   * Gets the package name.
   *
   * @return the package name, never <code>null</code>, may be empty.
   */
  @XmlElement(name = "package")
  public String getPackageName() {
    return packageName;
  }

  /**
   * Sets the package name.
   *
   * @param packageName the package name, may not be <code>null</code>.
   */
  public void setPackageName(String packageName) {
    if (packageName == null) throw new IllegalArgumentException("packageName must not be null");
    this.packageName = packageName;
  }

  /**
   * Gets the message type.
   *
   * @return the message type, never <code>null</code>, may be empty.
   */
  @XmlElement(name = "type")
  public String getType() {
    return type;
  }

  /**
   * Sets the message type.
   *
   * @param type the message type, may be <code>null</code>.
   */
  public void setType(String type) {
    this.type = type == null ? "" : type;
  }

  private String type = "";
  private String body = "";
  private String packageName = "";
}
