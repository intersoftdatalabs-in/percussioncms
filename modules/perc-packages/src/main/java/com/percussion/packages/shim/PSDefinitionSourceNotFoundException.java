/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.packages.shim;

/**
 * Thrown when neither a modern component package nor legacy definition XML is available for a
 * definition id / package root. Message is operator-facing and lists expected locations.
 */
public final class PSDefinitionSourceNotFoundException extends Exception {

  /** Definition or package id that could not be resolved (may be {@code null}). */
  private final String definitionId;

  /**
   * Creates an exception for a missing modern package and missing legacy definition XML.
   *
   * @param definitionId definition or package id (may be {@code null})
   * @param message operator-facing detail (must not be {@code null})
   */
  public PSDefinitionSourceNotFoundException(String definitionId, String message) {
    super(message);
    this.definitionId = definitionId;
  }

  /**
   * Returns the definition or package id that failed resolution.
   *
   * @return definition or package id, or {@code null} if unknown
   */
  public String getDefinitionId() {
    return definitionId;
  }
}
