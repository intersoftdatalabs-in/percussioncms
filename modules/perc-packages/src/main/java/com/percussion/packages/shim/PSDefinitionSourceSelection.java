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

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of {@link PSLegacyDefinitionXmlShim} selection: which source kind won and the primary path
 * used for that decision (manifest file, XML file, or package root).
 */
public final class PSDefinitionSourceSelection {

  private final PSDefinitionSourceKind kind;
  private final String definitionId;
  private final Path primaryPath;

  /**
   * Creates a selection result for the shim.
   *
   * @param kind selected source kind (required)
   * @param definitionId optional definition or package id
   * @param primaryPath optional primary path (manifest or XML); may be {@code null} for pure
   *     presence-based selection
   */
  public PSDefinitionSourceSelection(
      PSDefinitionSourceKind kind, String definitionId, Path primaryPath) {
    this.kind = Objects.requireNonNull(kind, "kind");
    this.definitionId = definitionId;
    this.primaryPath = primaryPath;
  }

  /**
   * Returns the selected source kind.
   *
   * @return selected source kind
   */
  public PSDefinitionSourceKind getKind() {
    return kind;
  }

  /**
   * Returns the optional definition or package id associated with this selection.
   *
   * @return optional definition or package id
   */
  public Optional<String> getDefinitionId() {
    return Optional.ofNullable(definitionId);
  }

  /**
   * Primary filesystem path for the selected source (e.g. {@code component-package.json} or a
   * Widget XML file). May be empty only for pure presence-based selection without paths.
   *
   * @return optional primary path
   */
  public Optional<Path> getPrimaryPath() {
    return Optional.ofNullable(primaryPath);
  }

  /**
   * Whether the modern component package was selected.
   *
   * @return {@code true} when modern
   */
  public boolean isModern() {
    return kind == PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE;
  }

  /**
   * Whether a legacy Widget/Page/Gadget XML source was selected.
   *
   * @return {@code true} when legacy XML
   */
  public boolean isLegacyXml() {
    return !isModern();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PSDefinitionSourceSelection that)) {
      return false;
    }
    return kind == that.kind
        && Objects.equals(definitionId, that.definitionId)
        && Objects.equals(primaryPath, that.primaryPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, definitionId, primaryPath);
  }

  @Override
  public String toString() {
    return "PSDefinitionSourceSelection{kind="
        + kind
        + ", definitionId='"
        + definitionId
        + "', primaryPath="
        + primaryPath
        + '}';
  }
}
