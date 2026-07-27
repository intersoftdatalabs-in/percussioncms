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
package com.percussion.share.relationship.service;

import com.percussion.share.relationship.data.PSLocalDependencySummary;
import com.percussion.share.relationship.data.PSNodeRelationshipSummary;
import com.percussion.share.relationship.data.PSRelationshipSummary;
import com.percussion.share.relationship.data.PSTaxonomySummary;

/**
 * Produces the per-node relationship summary served as endpoints 1, 2, 4, 5, 6, and 7 of the modern
 * Content Explorer's REST API.
 *
 * <p>Backed by:
 *
 * <ul>
 *   <li>{@code IPSSystemWs#findOwners(...)} / {@code findDependents(...)} — translation / linkback
 *       ownership chain;
 *   <li>{@code PSJcrNodeFinder#find(...)} — taxonomy / site edges;
 *   <li>{@code IPSWidgetAssetRelationshipService#getLocalAssets(...)} + {@code getLinkedPages(...)}
 *       + {@code getLinkedAssets(...)} — local page-assembly edges.
 * </ul>
 *
 * <p>AuthZ is enforced server-side per the existing {@code PSObjectAcl} model; calls over the REST
 * façade that cannot resolve the caller's identity or whose {@code accessLevel} is below {@code
 * READ} on the supplied item are answered with {@link java.util.Optional#empty()} so the JAX-RS
 * resource can translate to a 403 (per {@code docs/ai-generated/release/security-review-992.md}
 * §"US8 amendment").
 *
 * <p>The interface is read-only on purpose: the 8.2 surface uses it solely for the dependency
 * viewer; if a later spec needs write operations (e.g. reassign taxonomy nodes), it would land in a
 * sibling interface rather than grow this one.
 *
 * <p>Thread-safety: callers may share a single instance across threads; the impl holds no mutable
 * state of its own.
 *
 * @author Kilo (US8 / T092–T104)
 */
public interface IPSRelationshipSummaryService {

  /**
   * Counts the outgoing relationships for the supplied item, optionally restricted to a single
   * relationship config name.
   *
   * @param itemId the content id or guid-string of the item, never blank.
   * @return a non-null summary (count &ge; 0); {@code Optional.empty()} if the supplied id cannot
   *     be resolved to a guid the caller has read access to.
   */
  java.util.Optional<PSRelationshipSummary> summariseOutgoing(String itemId);

  /**
   * Counts the incoming relationships for the supplied item.
   *
   * @param itemId the content id or guid-string of the item, never blank.
   * @return non-null summary or {@code Optional.empty()} on AuthZ denial.
   */
  java.util.Optional<PSRelationshipSummary> summariseIncoming(String itemId);

  /**
   * Counts the taxonomy / site edges incident on the supplied item.
   *
   * @param itemId the content id or guid-string of the item, never blank.
   * @return non-null summary or {@code Optional.empty()} on AuthZ denial.
   */
  java.util.Optional<PSTaxonomySummary> summariseTaxonomy(String itemId);

  /**
   * Counts the local (page-assembly) assets incident on the supplied item.
   *
   * @param itemId the content id or guid-string of the page / template, never blank.
   * @return non-null summary or {@code Optional.empty()} on AuthZ denial.
   */
  java.util.Optional<PSLocalDependencySummary> summariseLocal(String itemId);

  /**
   * Counts the reverse (parents + inline-link parents) for the supplied item.
   *
   * @param itemId the content id or guid-string of the item, never blank.
   * @return non-null summary or {@code Optional.empty()} on AuthZ denial.
   */
  java.util.Optional<PSRelationshipSummary> summariseReverse(String itemId);

  /**
   * The consolidated summary that the JAX-RS resource serves as endpoint #7.
   *
   * @param itemId the content id or guid-string of the item, never blank.
   * @return non-null consolidated summary or {@code Optional.empty()} on AuthZ denial.
   */
  java.util.Optional<PSNodeRelationshipSummary> summarise(String itemId);
}
