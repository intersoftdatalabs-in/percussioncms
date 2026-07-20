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
 * Summary of the local (page-assembly) edges incident on the supplied item (US8 / T092–T104).
 *
 * <p>Backed by {@code IPSWidgetAssetRelationshipService.getLocalAssets(...)} +
 * {@code getLinkedPages(...)}: the service collects the assets used by the supplied page or
 * template (local + linked).
 *
 * <p>The {@link #links} field carries a {@link PSLocalDependencyLink} per local asset so the
 * dependency view can render "Local dependencies" with concrete target ids and types. Empty
 * (rather than {@code null}) when {@link #count} is 0.
 *
 * <p>Wire envelope: {@code {"PSLocalDependencySummary": { ... }}}.
 *
 * @author Kilo (US8 / T092–T104)
 */
@XmlRootElement(name = "PSLocalDependencySummary")
@JsonRootName("PSLocalDependencySummary")
public class PSLocalDependencySummary extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  /** Total number of local + linked assets incident on the supplied item. */
  private long count;

  /** Per-target rows; type is one of {@code local}, {@code linked}, {@code shared}. */
  private List<PSLocalDependencyLink> links = new ArrayList<>();

  public PSLocalDependencySummary() {
    super();
  }

  public PSLocalDependencySummary(long count, List<PSLocalDependencyLink> links) {
    this.count = count;
    this.links = links == null ? new ArrayList<>() : links;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

  public List<PSLocalDependencyLink> getLinks() {
    return links;
  }

  public void setLinks(List<PSLocalDependencyLink> links) {
    this.links = links == null ? new ArrayList<>() : links;
  }

  /** A single local or linked target on a page / template. */
  @XmlRootElement(name = "PSLocalDependencyLink")
  @JsonRootName("PSLocalDependencyLink")
  public static class PSLocalDependencyLink extends PSAbstractDataObject {
    private static final long serialVersionUID = 1L;

    /** One of {@code local}, {@code linked}, {@code shared}. */
    private String type;
    private String targetId;

    public PSLocalDependencyLink() {
      super();
    }

    public PSLocalDependencyLink(String type, String targetId) {
      this.type = type;
      this.targetId = targetId;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getTargetId() {
      return targetId;
    }

    public void setTargetId(String targetId) {
      this.targetId = targetId;
    }
  }
}
