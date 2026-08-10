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

/** Navigation node for a Virtual Site version tree. */
public final class VirtualNavNode {

  private final String title;
  private final String id;
  private final String href;
  private final int order;
  private final List<VirtualNavNode> children;

  public VirtualNavNode(
      String title, String id, String href, int order, List<VirtualNavNode> children) {
    this.title = Objects.requireNonNull(title, "title");
    this.id = id;
    this.href = href;
    this.order = order;
    this.children =
        children == null
            ? List.of()
            : Collections.unmodifiableList(new ArrayList<>(children));
  }

  public String title() {
    return title;
  }

  public String id() {
    return id;
  }

  public String href() {
    return href;
  }

  public int order() {
    return order;
  }

  public List<VirtualNavNode> children() {
    return children;
  }
}
