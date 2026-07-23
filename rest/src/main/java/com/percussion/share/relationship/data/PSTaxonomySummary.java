/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.share.relationship.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary of the taxonomy / site edges incident on the supplied item (US8 / T092–T104).
 *
 * <p>Backed by the JCR node layer in {@code projects/sitemanage/src/main/java/.../share/dao/}
 * via PSJcrNodeFinder: for each site folder the supplied item
 * sits under, the service counts the child nodes that fall under taxonomy.
 *
 * <p>The {@link #nodes} field carries the taxonomy node paths so the dependency view can show them
 * under "Site / taxonomy edges" without a second round-trip. Empty (rather than {@code null})
 * when the supplied item has no taxonomy edges.
 *
 * <p>Wire envelope: {@code {"PSTaxonomySummary": { ... }}}.
 *
 * @author Kilo (US8 / T092–T104)
 */
@XmlRootElement(name = "PSTaxonomySummary")
@JsonRootName("PSTaxonomySummary")
public class PSTaxonomySummary extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  /** Number of taxonomy node paths incident on the supplied item. */
  private long count;

  /** The taxonomy node paths. Empty (not {@code null}) when {@link #count} is 0. */
  private List<String> nodes = new ArrayList<>();

  public PSTaxonomySummary() {
    super();
  }

  public PSTaxonomySummary(long count, List<String> nodes) {
    this.count = count;
    this.nodes = nodes == null ? new ArrayList<>() : nodes;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

  public List<String> getNodes() {
    return nodes;
  }

  public void setNodes(List<String> nodes) {
    this.nodes = nodes == null ? new ArrayList<>() : nodes;
  }
}
