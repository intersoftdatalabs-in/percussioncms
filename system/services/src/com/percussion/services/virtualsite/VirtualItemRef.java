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
package com.percussion.services.virtualsite;

import java.nio.file.Path;
import java.util.Objects;

/** Lightweight discovery handle for a Virtual Site page. */
public final class VirtualItemRef {

  private final String id;
  private final String versionId;
  private final Path relativePath;
  private final int order;
  private final String title;

  public VirtualItemRef(
      String id, String versionId, Path relativePath, int order, String title) {
    this.id = Objects.requireNonNull(id, "id");
    this.versionId = Objects.requireNonNull(versionId, "versionId");
    this.relativePath = Objects.requireNonNull(relativePath, "relativePath");
    this.order = order;
    this.title = title != null ? title : id;
  }

  public String id() {
    return id;
  }

  public String versionId() {
    return versionId;
  }

  public Path relativePath() {
    return relativePath;
  }

  public int order() {
    return order;
  }

  public String title() {
    return title;
  }
}
