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

/** Fully loaded Virtual Site page (frontmatter + Markdown body). */
public final class VirtualItem {

  private final VirtualItemRef ref;
  private final VirtualFrontmatter frontmatter;
  private final String markdownBody;
  private final Path absolutePath;

  public VirtualItem(
      VirtualItemRef ref,
      VirtualFrontmatter frontmatter,
      String markdownBody,
      Path absolutePath) {
    this.ref = Objects.requireNonNull(ref, "ref");
    this.frontmatter = Objects.requireNonNull(frontmatter, "frontmatter");
    this.markdownBody = markdownBody != null ? markdownBody : "";
    this.absolutePath = Objects.requireNonNull(absolutePath, "absolutePath");
  }

  public VirtualItemRef ref() {
    return ref;
  }

  public VirtualFrontmatter frontmatter() {
    return frontmatter;
  }

  public String markdownBody() {
    return markdownBody;
  }

  public Path absolutePath() {
    return absolutePath;
  }
}
