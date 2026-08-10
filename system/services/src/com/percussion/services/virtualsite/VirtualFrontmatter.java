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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Parsed page frontmatter for a Virtual Site Markdown page. */
public final class VirtualFrontmatter {

  private final String id;
  private final String title;
  private final String description;
  private final String version;
  private final boolean sidebar;
  private final int order;
  private final List<String> tags;
  private final boolean deprecated;

  public VirtualFrontmatter(
      String id,
      String title,
      String description,
      String version,
      boolean sidebar,
      int order,
      List<String> tags,
      boolean deprecated) {
    this.id = Objects.requireNonNull(id, "id");
    this.title = Objects.requireNonNull(title, "title");
    this.description = description != null ? description : "";
    this.version = version;
    this.sidebar = sidebar;
    this.order = order;
    this.tags =
        tags == null
            ? List.of()
            : Collections.unmodifiableList(new ArrayList<>(tags));
    this.deprecated = deprecated;
  }

  public String id() {
    return id;
  }

  public String title() {
    return title;
  }

  public String description() {
    return description;
  }

  public String version() {
    return version;
  }

  public boolean sidebar() {
    return sidebar;
  }

  public int order() {
    return order;
  }

  public List<String> tags() {
    return tags;
  }

  public boolean deprecated() {
    return deprecated;
  }
}
