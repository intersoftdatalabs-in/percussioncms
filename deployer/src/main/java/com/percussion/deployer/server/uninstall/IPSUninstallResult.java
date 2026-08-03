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

/**
 * Interface for the uninstall results. Package name and type are required and the rest of the data
 * is filled based on the stage at which the failure occurred.
 */
public interface IPSUninstallResult {

  /**
   * Returns the package GUID, may be <code>null</code> if the supplied package name does not have a
   * corresponding GUID.
   *
   * @return IPSGuid of the package, may be <code>null</code>.
   */
  IPSGuid getPackageGuid();

  /**
   * Returns the name of the package.
   *
   * @return Name of the package, never <code>null</code>.
   */
  String getPackageName();

  /**
   * Returns the type of the result.
   *
   * @return Type of the result, never <code>null</code>.
   */
  PSUninstallResultType getResultType();

  /**
   * Returns the message.
   *
   * @return The message of the result, may be <code>null</code> or empty.
   */
  String getMessage();

  /**
   * Exception associated with the result.
   *
   * @return May be <code>null</code> if no exceptions occurred.
   */
  Exception getException();

  /**
   * Gets the GUID of the object that caused the error.
   *
   * @return IPSGuid object GUID, may be <code>null</code>.
   */
  IPSGuid getObjectGuid();

  /**
   * Gets the name of the object that caused the error.
   *
   * @return String object name, may be <code>null</code>.
   */
  String getObjectName();

  /** The uninstall message type enum. */
  enum PSUninstallResultType {
    /** Uninstall completed successfully. */
    SUCCESS(1),
    /** Uninstall produced an informational message. */
    INFO(2),
    /** Uninstall produced a warning. */
    WARN(3),
    /** Uninstall failed with an error. */
    ERROR(4);

    PSUninstallResultType(int type) {
      this.type = type;
    }

    /**
     * Returns the integer value that identifies this uninstall result type.
     *
     * @return the integer value of this result type
     */
    public int getValue() {
      return type;
    }

    private final int type;
  }
}
