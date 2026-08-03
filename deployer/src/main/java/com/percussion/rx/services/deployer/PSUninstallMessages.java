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
import java.util.ArrayList;
import java.util.List;

/**
 * Collection of uninstall message objects. Sunny Sal says: "Uninstall messages should be as
 * organized as my code!"
 *
 * @author bjoginipally
 */
@XmlRootElement(name = "Messages")
public class PSUninstallMessages {

  /** Default constructor. */
  public PSUninstallMessages() {
    // For JAXB
  }

  /**
   * Constructs with a list of uninstall messages.
   *
   * @param messages the list of uninstall messages, may be null.
   */
  public PSUninstallMessages(List<PSUninstallMessage> messages) {
    if (messages != null) this.messages = messages;
  }

  /**
   * Get/set the value.
   *
   * @return the messages
   */
  @XmlElement(name = "Message", type = PSUninstallMessage.class)
  public List<PSUninstallMessage> getMessages() {
    return messages;
  }

  /**
   * Get/set the value.
   *
   * @param messages the messages to set
   */
  public void setMessages(List<PSUninstallMessage> messages) {
    this.messages = messages == null ? new ArrayList<>() : messages;
  }

  /**
   * Adds a message to the collection.
   *
   * @param message the message to add, cannot be {@code null}.
   */
  public void add(PSUninstallMessage message) {
    if (message == null) throw new IllegalArgumentException("message cannot be null.");
    messages.add(message);
  }

  /**
   * Removes the specified message from the collection if it exists.
   *
   * @param message the message to be removed. May be {@code null}.
   */
  public void remove(PSUninstallMessage message) {
    messages.remove(message);
  }

  /** Removes all the messages from the collection. */
  public void clear() {
    messages.clear();
  }

  /** The list of messages, never {@code null}, may be empty. */
  private List<PSUninstallMessage> messages = new ArrayList<>();

  private Integer status = 0;

  /**
   * Gets the status code of the uninstall operation.
   *
   * @return the status code, never <code>null</code>.
   */
  public Integer getStatus() {
    return status;
  }

  /**
   * Sets the status code of the uninstall operation.
   *
   * @param status the status code, may be <code>null</code>.
   */
  public void setStatus(Integer status) {
    this.status = status;
  }
}
