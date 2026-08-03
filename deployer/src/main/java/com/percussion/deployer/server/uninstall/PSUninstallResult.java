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
package com.percussion.deployer.server.uninstall;

import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang3.StringUtils;

/** Implementation of {@link IPSUninstallResult}, see interface for details. */
public class PSUninstallResult implements IPSUninstallResult {

  /**
   * Constructs a new uninstall result.
   *
   * @param pkgName the package name, may not be blank.
   * @param resultType the result type, may not be <code>null</code>.
   */
  public PSUninstallResult(String pkgName, PSUninstallResultType resultType) {
    if (StringUtils.isBlank(pkgName)) {
      throw new IllegalArgumentException("pkgName must not be empty");
    }
    if (resultType == null) {
      throw new IllegalArgumentException("resultType must not be null");
    }
    this.packageName = pkgName;
    this.resultType = resultType;
  }

  /**
   * Gets the message associated with this result.
   *
   * @return the message, may be <code>null</code>.
   */
  @Override
  public String getMessage() {
    return message;
  }

  /**
   * Gets the exception associated with this result.
   *
   * @return the exception, may be <code>null</code>.
   */
  @Override
  public Exception getException() {
    return exception;
  }

  /**
   * Gets the result type.
   *
   * @return the result type, may be <code>null</code>.
   */
  @Override
  public PSUninstallResultType getResultType() {
    return resultType;
  }

  /**
   * Gets the object GUID.
   *
   * @return the object GUID, may be <code>null</code>.
   */
  @Override
  public IPSGuid getObjectGuid() {
    return objectGuid;
  }

  /**
   * Gets the object name.
   *
   * @return the object name, may be <code>null</code>.
   */
  @Override
  public String getObjectName() {
    return objectName;
  }

  /**
   * Gets the package GUID.
   *
   * @return the package GUID, may be <code>null</code>.
   */
  @Override
  public IPSGuid getPackageGuid() {
    return packageGuid;
  }

  /**
   * Gets the package name.
   *
   * @return the package name, may be <code>null</code>.
   */
  @Override
  public String getPackageName() {
    return packageName;
  }

  /**
   * Sets the exception associated with this result.
   *
   * @param exception the exception, may be <code>null</code>.
   */
  public void setException(Exception exception) {
    this.exception = exception;
  }

  /**
   * Sets the result type.
   *
   * @param resultType the result type, may be <code>null</code>.
   */
  public void setResultType(PSUninstallResultType resultType) {
    this.resultType = resultType;
  }

  /**
   * Sets the object GUID.
   *
   * @param objectGuid the object GUID, may be <code>null</code>.
   */
  public void setObjectGuid(IPSGuid objectGuid) {
    this.objectGuid = objectGuid;
  }

  /**
   * Sets the object name.
   *
   * @param objectName the object name, may be <code>null</code>.
   */
  public void setObjectName(String objectName) {
    this.objectName = objectName;
  }

  /**
   * Sets the package GUID.
   *
   * @param packageGuid the package GUID, may be <code>null</code>.
   */
  public void setPackageGuid(IPSGuid packageGuid) {
    this.packageGuid = packageGuid;
  }

  /**
   * Sets the message associated with this result.
   *
   * @param message the message, may be <code>null</code>.
   */
  public void setMessage(String message) {
    this.message = message;
  }

  private IPSGuid packageGuid;
  private IPSGuid objectGuid;
  private String packageName;
  private String objectName;
  private Exception exception;
  private PSUninstallResultType resultType;
  private String message;
}
