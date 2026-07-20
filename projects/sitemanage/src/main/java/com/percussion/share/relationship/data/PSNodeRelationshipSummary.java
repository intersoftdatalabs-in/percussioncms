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

/**
 * Consolidated per-node relationship summary served as endpoint #7 of the modern Content Explorer's
 * relationship API (US8 / T092–T104).
 *
 * <p>{@code GET /Rhythmyx/rest/content-explorer/relationships/{itemId}/summary} returns one
 * instance of this DTO per request. Each of {@link #outgoing}, {@link #incoming},
 * {@link #taxonomy}, {@link #local}, {@link #reverse} maps to a dimension the DependencyViewer
 * renders.
 *
 * <p>The Active-Assembly dimension is not part of this DTO: AA counts are sourced client-side
 * from the existing {@code PSWidgetAssetRelationshipService.getRelationshipOwners(...)} count
 * via the host shell (the same source the morning DependencyViewer used). See
 * {@code research/relationship-rest-gaps.md} for the rationale.
 *
 * <p>Wire envelope: {@code {"PSNodeRelationshipSummary": { ... }}}.
 *
 * @author Kilo (US8 / T092–T104)
 */
@XmlRootElement(name = "PSNodeRelationshipSummary")
@JsonRootName("PSNodeRelationshipSummary")
public class PSNodeRelationshipSummary extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  private PSRelationshipSummary outgoing = new PSRelationshipSummary(0, java.util.Collections.emptyList());
  private PSRelationshipSummary incoming = new PSRelationshipSummary(0, java.util.Collections.emptyList());
  private PSTaxonomySummary taxonomy = new PSTaxonomySummary(0, java.util.Collections.emptyList());
  private PSLocalDependencySummary local = new PSLocalDependencySummary(0, java.util.Collections.emptyList());
  private PSRelationshipSummary reverse = new PSRelationshipSummary(0, java.util.Collections.emptyList());

  public PSNodeRelationshipSummary() {
    super();
  }

  public PSNodeRelationshipSummary(
      PSRelationshipSummary outgoing,
      PSRelationshipSummary incoming,
      PSTaxonomySummary taxonomy,
      PSLocalDependencySummary local,
      PSRelationshipSummary reverse) {
    this.outgoing = outgoing == null ? new PSRelationshipSummary(0, java.util.Collections.emptyList()) : outgoing;
    this.incoming = incoming == null ? new PSRelationshipSummary(0, java.util.Collections.emptyList()) : incoming;
    this.taxonomy = taxonomy == null ? new PSTaxonomySummary(0, java.util.Collections.emptyList()) : taxonomy;
    this.local = local == null ? new PSLocalDependencySummary(0, java.util.Collections.emptyList()) : local;
    this.reverse = reverse == null ? new PSRelationshipSummary(0, java.util.Collections.emptyList()) : reverse;
  }

  public PSRelationshipSummary getOutgoing() {
    return outgoing;
  }

  public void setOutgoing(PSRelationshipSummary outgoing) {
    this.outgoing = outgoing;
  }

  public PSRelationshipSummary getIncoming() {
    return incoming;
  }

  public void setIncoming(PSRelationshipSummary incoming) {
    this.incoming = incoming;
  }

  public PSTaxonomySummary getTaxonomy() {
    return taxonomy;
  }

  public void setTaxonomy(PSTaxonomySummary taxonomy) {
    this.taxonomy = taxonomy;
  }

  public PSLocalDependencySummary getLocal() {
    return local;
  }

  public void setLocal(PSLocalDependencySummary local) {
    this.local = local;
  }

  public PSRelationshipSummary getReverse() {
    return reverse;
  }

  public void setReverse(PSRelationshipSummary reverse) {
    this.reverse = reverse;
  }
}
