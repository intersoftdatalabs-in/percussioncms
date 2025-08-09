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

/**
 * Implementation of {@link IPSUninstallResult}, see interface for details.
 */
public class PSUninstallResult implements IPSUninstallResult {

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

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public Exception getException() {
    return exception;
  }

  @Override
  public PSUninstallResultType getResultType() {
    return resultType;
  }

  @Override
  public IPSGuid getObjectGuid() {
    return objectGuid;
  }

  @Override
  public String getObjectName() {
    return objectName;
  }

  @Override
  public IPSGuid getPackageGuid() {
    return packageGuid;
  }

  @Override
  public String getPackageName() {
    return packageName;
  }

  public void setException(Exception exception) {
    this.exception = exception;
  }

  public void setResultType(PSUninstallResultType resultType) {
    this.resultType = resultType;
  }

  public void setObjectGuid(IPSGuid objectGuid) {
    this.objectGuid = objectGuid;
  }

  public void setObjectName(String objectName) {
    this.objectName = objectName;
  }

  public void setPackageGuid(IPSGuid packageGuid) {
    this.packageGuid = packageGuid;
  }

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
