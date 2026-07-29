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

package com.ibm.cadf;

/**
 * Catalog of localized message templates used throughout the CADF audit middleware. Values are
 * looked up by {@link java.text.MessageFormat} so each {@code {0}} placeholder is replaced with the
 * supplied argument at call time.
 */
public interface Messages {

  /**
   * Message template raised when a CADF type fails validation because a required field is blank.
   */
  public static String MISSING_MANDATORY_FIELDS =
      "Mandatory fields are missing, pass [{0}] the required fields";
}
