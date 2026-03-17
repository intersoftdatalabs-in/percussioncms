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

package com.percussion.extensions.security;

/**
 * Interface for validating input values against specific constraints.
 *
 * <p>This interface defines the contract for input validators that can be used to sanitize and
 * validate user input in Rhythmyx extensions. Implementations should ensure that only valid,
 * expected values are allowed through.
 *
 * <p>Common use cases include:
 *
 * <ul>
 *   <li>Ensuring GUIDs follow the expected format
 *   <li>Restricting numeric fields to valid integer ranges
 *   <li>Validating file paths for security
 *   <li>Allowing only specific characters in input
 * </ul>
 *
 * @see PSAllowOnlyGuidValues
 * @see PSAllowOnlyIntegerValues
 * @see PSAllowOnlyNumericValues
 * @see PSAllowOnlyPathValues
 * @see PSAllowOnlyTheseCharacters
 */
public interface IPSAllowOnlyItemInputValidator {

  /**
   * Validates and potentially sanitizes the provided input value.
   *
   * <p>This method should return the sanitized value if valid, or an empty string or null if the
   * value does not meet the validation criteria. The exact behavior depends on the implementation.
   *
   * @param value the input value to validate. May be null depending on implementation.
   * @param options optional configuration string that controls validation behavior. The format
   *     depends on the implementation. May be null.
   * @return the validated/sanitized value, or an empty string/null if validation fails.
   */
  public String validate(String value, String options);
}
