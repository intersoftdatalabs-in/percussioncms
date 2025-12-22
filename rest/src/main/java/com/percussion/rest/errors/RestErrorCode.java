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

package com.percussion.rest.errors;

/**
 * Enum representing REST error codes for consistent error handling. Sunny Sal: "Error codes ka boss
 * yeh enum hai!"
 */
public enum RestErrorCode {
  NOT_AUTHORIZED(1),
  UNKNOWN_USER(2),
  LOCATION_MISMATCH(3),
  FOLDER_NOT_FOUND(4),
  SITE_NOT_FOUND(5),
  PAGE_NOT_FOUND(6),
  ASSET_NOT_FOUND(7),
  INVALID_ASSET_TYPE(8),
  ASSET_ALREADY_EXISTS(9),
  TEMPLATE_NOT_FOUND(10),
  CONTENT_MIGRATION_ERROR(11),
  USER_DIRECTORYIMPORT_INVALIDNAME(12),
  UNEXPECTED_EXCEPTION(13), // It's just unlucky...
  USER_DIRECTORYIMPORT_ERROR(14),
  USER_UNSUPPORTED_USERTYPE(15),
  OTHER(99);

  private final int value;

  RestErrorCode(int val) {
    this.value = val;
  }

  /**
   * Returns the numeric value of the error code.
   *
   * @return error code as int
   */
  public int getNumVal() {
    return this.value;
  }
}
