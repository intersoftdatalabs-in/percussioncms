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
package com.percussion.rx.config;

import com.percussion.services.catalog.PSTypeEnum;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Encapsulates an error or warning validation result. Used for validating configuration files in
 * one package against another.
 *
 * <p>Sunny Sal says: "Validation is like a seatbelt—better safe than sorry!"
 *
 * @author YuBingChen
 */
public class PSConfigValidation {

  /**
   * Creates a validation result.
   *
   * @param name the name of the validated object, never {@code null} or empty.
   * @param propName the name of the property, may be {@code null} or empty.
   * @param isError {@code true} if it is an error; otherwise, a warning.
   * @param message the validation message, never {@code null} or empty.
   */
  public PSConfigValidation(String name, String propName, boolean isError, String message) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be null or empty.");
    }
    if (StringUtils.isBlank(message)) {
      throw new IllegalArgumentException("message may not be null or empty.");
    }
    this.objName = name;
    this.propertyName = propName;
    this.isError = isError;
    this.message = message;
    this.exception = null;
  }

  /**
   * Constructs a validation result from an exception.
   *
   * @param pkgName the package name, never {@code null} or empty.
   * @param e the exception caught during validation, not {@code null}.
   */
  public PSConfigValidation(String pkgName, Exception e) {
    if (StringUtils.isBlank(pkgName)) {
      throw new IllegalArgumentException("pkgName may not be blank.");
    }
    if (e == null) {
      throw new IllegalArgumentException("Exception cannot be null.");
    }
    this.pkgName = pkgName;
    this.exception = e;
  }

  /**
   * Gets the validation message.
   *
   * @return the validation message, never {@code null} or empty.
   */
  public String getValidationMsg() {
    if (exception != null) {
      String expMsg =
          StringUtils.isBlank(exception.getLocalizedMessage())
              ? exception.toString()
              : exception.getLocalizedMessage();
      return "While verifying package \"" + pkgName + "\", an error occurred: " + expMsg;
    }
    String msgType = isError ? "error" : "warning";
    String type = objType == null ? "" : " (type=" + objType.name() + ")";
    if (propertyName != null) {
      return "While verifying package \""
          + pkgName
          + "\", found a conflict on property \""
          + propertyName
          + "\" of design object \""
          + objName
          + "\""
          + type
          + " in package \""
          + otherPkgName
          + "\". The "
          + msgType
          + " is: "
          + message;
    } else {
      return "While verifying package \""
          + pkgName
          + "\", found a conflict on design object \""
          + objName
          + "\""
          + type
          + " in package \""
          + otherPkgName
          + "\". The "
          + msgType
          + " is: "
          + message;
    }
  }

  /**
   * Sets the object type.
   *
   * @param type the object type, not {@code null}.
   */
  public void setObjectType(PSTypeEnum type) {
    if (type == null) {
      throw new IllegalArgumentException("type may not be null.");
    }
    this.objType = type;
  }

  /**
   * Gets the name of the validated object.
   *
   * @return the name, never {@code null} or empty.
   */
  public String getObjectName() {
    return objName;
  }

  /**
   * Sets the current package name.
   *
   * @param pkgName the new package name, not {@code null} or empty.
   */
  public void setPkgName(String pkgName) {
    if (StringUtils.isBlank(pkgName)) {
      throw new IllegalArgumentException("pkgName may not be null or empty.");
    }
    this.pkgName = pkgName;
  }

  /**
   * Sets the package name that was used to validate against the current package.
   *
   * @param otherPkgName the other package name, not {@code null} or empty.
   */
  public void setOtherPkgName(String otherPkgName) {
    if (StringUtils.isBlank(otherPkgName)) {
      throw new IllegalArgumentException("otherPkgName may not be null or empty.");
    }
    this.otherPkgName = otherPkgName;
  }

  /**
   * Gets the type of the validated object.
   *
   * @return the object type, may be {@code null}.
   */
  public PSTypeEnum getObjectType() {
    return objType;
  }

  /**
   * Determines if this is an error or warning.
   *
   * @return {@code true} if this is an error.
   */
  public boolean isError() {
    return isError;
  }

  /**
   * Gets the validation message.
   *
   * @return the message, never {@code null} or empty.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the message.
   *
   * @param msg the new message, never {@code null} or empty.
   */
  public void setMessage(String msg) {
    if (StringUtils.isBlank(msg)) {
      throw new IllegalArgumentException("msg may not be null or empty.");
    }
    this.message = msg;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PSConfigValidation)) {
      return false;
    }
    var second = (PSConfigValidation) obj;
    return new EqualsBuilder()
        .append(objName, second.objName)
        .append(objType, second.objType)
        .append(isError, second.isError)
        .append(message, second.message)
        .append(propertyName, second.propertyName)
        .append(pkgName, second.pkgName)
        .append(otherPkgName, second.otherPkgName)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(objName)
        .append(objType)
        .append(isError)
        .append(message)
        .append(propertyName)
        .append(pkgName)
        .append(otherPkgName)
        .toHashCode();
  }

  /** The name of the design object, never {@code null} after construction. */
  private String objName;

  /** The type of the design object, may be {@code null}. */
  private PSTypeEnum objType;

  /** {@code true} for error, {@code false} for warning. */
  private boolean isError;

  /** The name of the current package. */
  private String pkgName;

  /** The name of the package validated against. */
  private String otherPkgName;

  /** The name of the property with error or warning. */
  private String propertyName;

  /** The validation message. */
  private String message;

  /** The exception caught during validation. */
  private Exception exception;
}
