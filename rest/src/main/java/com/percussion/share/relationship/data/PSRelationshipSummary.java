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
import java.util.Objects;

/**
 * Summary counts for one of the three relationship-class dimensions served by the modern Content
 * Explorer's DependencyViewer (US8 / T092–T104):
 *
 * <ul>
 *   <li><strong>outgoing</strong>: items that own the supplied item via a non-AA relationship (e.g.
 *       translation parents, linkback source pages). Backed by {@code IPSSystemWs.findOwners(...)}
 *       with {@link
 *       com.percussion.cms.objectstore.PSRelationshipFilter#FILTER_CATEGORY_TRANSLATION}.
 *   <li><strong>incoming</strong>: items that are owned by the supplied item via the same category.
 *       Backed by {@code IPSSystemWs.findDependents(...)}.
 *   <li><strong>reverse</strong>: the union of incoming plus any inline-link parents; computed by
 *       combining the incoming dimension with the supplied item's {@code
 *       IPSWidgetAssetRelationshipService.getLinkedPages(...)} and {@code
 *       IPSWidgetAssetRelationshipService.getLinkedAssets(...)} parents.
 * </ul>
 *
 * <p>The shape mirrors the live Java DTOs that the underlying catalogers produce — no invented
 * fields. Counts are integers because the modern Content Explorer never needs an enumerated list of
 * owner-ids on the dependency-view row; the consolidated {@code PSNodeRelationshipSummary} is the
 * only DTO that ever carries per-link rows.
 *
 * <p>Wire envelope: {@code {"PSRelationshipSummary": { ... }}} on the JAX-RS side; rendered as
 * plain JSON by the modern Content Explorer's {@code relationshipsApi.ts} client.
 *
 * @author Kilo (US8 / T092–T104)
 */
@XmlRootElement(name = "PSRelationshipSummary")
@JsonRootName("PSRelationshipSummary")
public class PSRelationshipSummary extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  /**
   * Total number of relationships that match the supplied filter. Always &ge; 0; never {@code
   * null}.
   */
  private long count;

  /**
   * Per-type breakdown, e.g. {@code [{"type": "translation", "count": 3}]}. Empty (rather than
   * {@code null}) when the supplied item has no relationships in this dimension. Stored as {@link
   * ArrayList} so the field type is {@link java.io.Serializable} under {@code -Xlint:serial}.
   */
  private ArrayList<PSRelationshipTypeBucket> byType = new ArrayList<>();

  public PSRelationshipSummary() {
    super();
  }

  public PSRelationshipSummary(long count, List<PSRelationshipTypeBucket> byType) {
    this.count = count;
    this.byType = byType == null ? new ArrayList<>() : new ArrayList<>(byType);
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

  public List<PSRelationshipTypeBucket> getByType() {
    return byType;
  }

  public void setByType(List<PSRelationshipTypeBucket> byType) {
    this.byType = byType == null ? new ArrayList<>() : new ArrayList<>(byType);
  }

  /** One per relationship config (translation / linkback / AA / local). */
  @XmlRootElement(name = "PSRelationshipTypeBucket")
  @JsonRootName("PSRelationshipTypeBucket")
  public static class PSRelationshipTypeBucket extends PSAbstractDataObject {
    private static final long serialVersionUID = 1L;

    private String type;
    private long count;

    public PSRelationshipTypeBucket() {
      super();
    }

    public PSRelationshipTypeBucket(String type, long count) {
      this.type = type;
      this.count = count;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public long getCount() {
      return count;
    }

    public void setCount(long count) {
      this.count = count;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSRelationshipTypeBucket)) return false;
      PSRelationshipTypeBucket that = (PSRelationshipTypeBucket) o;
      return count == that.count && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, count);
    }
  }
}
