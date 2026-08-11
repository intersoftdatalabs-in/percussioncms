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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Items class.
 */
@XmlRootElement(name = "Items")
public class Items {
  /**
   * Creates a new Items.
   */
  public Items() {
    // default
  }

  private List<Item> items = new ArrayList<>();
  private List<Error> errors = null;

  /**
   * Returns the items.
   *
   * @return the result
   */
  @XmlElement(name = "Item")
  public List<Item> getItems() {
    return items;
  }

  /**
   * Sets the items.
   *
   * @param items the items
   */
  public void setItems(List<Item> items) {
    this.items = items;
  }

  private static final Logger log = LogManager.getLogger(Items.class);

  /**
   * Returns the errors.
   *
   * @return the result
   */
  @XmlElement(name = "Error")
  @XmlElementWrapper(name = "Errors")
  public List<Error> getErrors() {
    return errors;
  }

  /**
   * Sets the errors.
   *
   * @param errors the errors
   */
  public void setErrors(List<Error> errors) {
    this.errors = errors;
  }

  /**
   * addError operation.
   *
   * @param error the error
   * @param message the message
   */
  public void addError(Error.ErrorCode error, String message) {
    if (errors == null) errors = new ArrayList<>();
    errors.add(new Error(error, message));
    log.debug("Error is: {}", message);
  }

  /**
   * addError operation.
   *
   * @param error the error
   * @param e the e
   */
  public void addError(Error.ErrorCode error, Exception e) {
    if (errors == null) errors = new ArrayList<>();
    String message = e.getMessage();
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    errors.add(new Error(error, message + ":" + sw));
  }

  /**
   * addError operation.
   *
   * @param error the error
   * @param message the message
   * @param e the e
   */
  public void addError(Error.ErrorCode error, String message, Exception e) {
    if (errors == null) errors = new ArrayList<>();
    String messageex = e.getMessage() + "\n";
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    errors.add(new Error(error, messageex + "\n" + message + "\n" + sw));
    log.debug("Error is: {}", message);
  }

  /**
   * addError operation.
   *
   * @param error the error
   */
  public void addError(Error.ErrorCode error) {
    if (errors == null) errors = new ArrayList<>();
    errors.add(new Error(error));
    log.debug("Error is: {}", error);
  }

  /**
   * hasError operation.
   *
   * @param error the error
   * @return the result
   */
  public boolean hasError(Error.ErrorCode error) {
    if (errors != null) {
      for (Error errorTest : errors) {
        if (errorTest.getErrorCode() == error) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * hasItems operation.
   *
   * @return the result
   */
  public boolean hasItems() {
    if (items != null && !items.isEmpty()) {
      return true;
    }
    return false;
  }

  /**
   * hasErrors operation.
   *
   * @return the result
   */
  public boolean hasErrors() {
    if (errors != null && !errors.isEmpty()) {
      return true;
    }
    return false;
  }
}
