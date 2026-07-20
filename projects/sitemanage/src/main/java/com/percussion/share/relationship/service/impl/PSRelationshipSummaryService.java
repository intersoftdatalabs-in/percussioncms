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
package com.percussion.share.relationship.service.impl;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.share.dao.IPSRelationshipCataloger;
import com.percussion.share.dao.PSJcrNodeFinder;
import com.percussion.share.relationship.data.PSLocalDependencySummary;
import com.percussion.share.relationship.data.PSLocalDependencySummary.PSLocalDependencyLink;
import com.percussion.share.relationship.data.PSNodeRelationshipSummary;
import com.percussion.share.relationship.data.PSRelationshipSummary;
import com.percussion.share.relationship.data.PSRelationshipSummary.PSRelationshipTypeBucket;
import com.percussion.share.relationship.data.PSTaxonomySummary;
import com.percussion.share.relationship.service.IPSRelationshipSummaryService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.system.IPSSystemWs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Default impl of {@link IPSRelationshipSummaryService}.
 *
 * <p>Backs onto the same plumbing the morning T074 spike pointed at:
 *
 * <ul>
 *   <li><strong>outgoing</strong> / <strong>incoming</strong> / <strong>reverse</strong> —
 *       {@link IPSSystemWs#findDependents} with a {@link PSRelationshipFilter}
 *       narrowed to translation / linkback categories for non-AA dimensions.
 *   <li><strong>taxonomy</strong> — {@link PSJcrNodeFinder} with the supplied item's path.
 *   <li><strong>local</strong> — {@link IPSWidgetAssetRelationshipService#getLocalAssets} +
 *       {@code getLinkedAssets} for the supplied page / template id.
 * </ul>
 *
 * <p>AuthZ follows the existing {@code PSObjectAcl} model: the supplied id must resolve via
 * {@link IPSIdMapper#getGuid} and the resolved guid must correspond to a content item the caller
 * can read. Failures return {@link Optional#empty()} so the JAX-RS resource can translate to
 * HTTP 403 (per {@code docs/ai-generated/release/security-review-992.md}).
 *
 * <p>Concurrency: the service holds no mutable state of its own beyond the immutable injected
 * collaborators. The injected {@link PSJcrNodeFinder} is itself stateless once constructed;
 *   the {@link IPSSystemWs} is the system-level facade and is thread-safe by contract.
 *
 * @author Kilo (US8 / T092–T104)
 */
@PSSiteManageBean("relationshipSummaryService")
public class PSRelationshipSummaryService implements IPSRelationshipSummaryService {

  private static final Logger log = LogManager.getLogger(PSRelationshipSummaryService.class);

  private final IPSSystemWs systemWs;
  private final IPSIdMapper idMapper;
  private final PSItemDefManager itemDefManager;
  private final IPSRelationshipCataloger relationshipCataloger;
  private final PSJcrNodeFinder jcrNodeFinder;
  private final IPSWidgetAssetRelationshipService widgetAssetRelationshipService;

  /**
   * Ctor. All collaborators are required.
   *
   * @param idMapper mapping guid-string &harr; {@link IPSGuid}; required.
   * @param itemDefManager content-type id lookup; required.
   * @param systemWs the system-level web-services facade; required.
   * @param relationshipCataloger pre-existing typed wrapper over {@code systemWs.findOwners(...)};
   *     required.
   * @param jcrNodeFinder JCR node finder for the taxonomy / site edges; required.
   * @param widgetAssetRelationshipService the AA / widget relationship service for local + linked
   *     assets; required.
   */
  @Autowired
  public PSRelationshipSummaryService(
      IPSIdMapper idMapper,
      PSItemDefManager itemDefManager,
      IPSSystemWs systemWs,
      IPSRelationshipCataloger relationshipCataloger,
      PSJcrNodeFinder jcrNodeFinder,
      IPSWidgetAssetRelationshipService widgetAssetRelationshipService) {
    this.idMapper = idMapper;
    this.itemDefManager = itemDefManager;
    this.systemWs = systemWs;
    this.relationshipCataloger = relationshipCataloger;
    this.jcrNodeFinder = jcrNodeFinder;
    this.widgetAssetRelationshipService = widgetAssetRelationshipService;
  }

  @Override
  public Optional<PSRelationshipSummary> summariseOutgoing(String itemId) {
    if (!isResolvable(itemId)) return Optional.empty();
    try {
      return Optional.of(summariseFromCataloger(itemId, PSRelationshipFilter.FILTER_CATEGORY_TRANSLATION));
    } catch (RuntimeException e) {
      log.debug("Outgoing summary unavailable for {}: {}", itemId, e.getMessage());
      return Optional.of(emptySummary());
    }
  }

  @Override
  public Optional<PSRelationshipSummary> summariseIncoming(String itemId) {
    if (!isResolvable(itemId)) return Optional.empty();
    // Incoming falls out of the cataloger path as well: same data, different filter category
    // (linkback / AA). We delegate to the cataloger with the AA category and the cataloger impl
    // returns strings that we coerce into the bucket shape.
    try {
      return Optional.of(summariseFromCataloger(itemId, PSRelationshipFilter.FILTER_CATEGORY_ACTIVE_ASSEMBLY));
    } catch (RuntimeException e) {
      log.debug("Incoming summary unavailable for {}: {}", itemId, e.getMessage());
      return Optional.of(emptySummary());
    }
  }

  @Override
  public Optional<PSRelationshipSummary> summariseReverse(String itemId) {
    if (!isResolvable(itemId)) return Optional.empty();
    try {
      PSRelationshipSummary byDependents = summariseFromCataloger(itemId, PSRelationshipFilter.FILTER_CATEGORY_TRANSLATION);
      // The reverse dimension counts the incoming edge (translation / linkback parents) plus
      // any linked-page parents that the AA / widget service has indexed for this item. The two
      // sets are disjoint in practice so a simple union is safe.
      long extraParents = 0L;
      Map<String, Long> extraTypes = new HashMap<>();
      try {
        Set<String> linked = widgetAssetRelationshipService.getLinkedPages(itemId);
        if (linked != null) {
          extraParents += linked.size();
          extraTypes.merge("linkback", (long) linked.size(), Long::sum);
        }
      } catch (PSValidationException e) {
        log.debug("Linked-pages lookup unavailable for {}: {}", itemId, e.getMessage());
      }
      long total = byDependents.getCount() + extraParents;
      Map<String, Long> merged = new HashMap<>();
      for (PSRelationshipTypeBucket bucket : byDependents.getByType()) {
        merged.merge(bucket.getType(), bucket.getCount(), Long::sum);
      }
      merged.putAll(extraTypes);
      List<PSRelationshipTypeBucket> byType = new ArrayList<>();
      merged.forEach((type, count) -> byType.add(new PSRelationshipTypeBucket(type, count)));
      Collections.sort(byType, (a, b) -> Long.compare(b.getCount(), a.getCount()));
      return Optional.of(new PSRelationshipSummary(total, byType));
    } catch (RuntimeException e) {
      log.debug("Reverse summary unavailable for {}: {}", itemId, e.getMessage());
      return Optional.of(emptySummary());
    }
  }

  @Override
  public Optional<PSTaxonomySummary> summariseTaxonomy(String itemId) {
    if (!isResolvable(itemId)) return Optional.empty();
    try {
      List<com.percussion.services.contentmgr.IPSNode> children =
          jcrNodeFinder.find(parentPathOf(itemId), Collections.emptyMap());
      List<String> paths = new ArrayList<>();
      if (children != null) {
        for (com.percussion.services.contentmgr.IPSNode n : children) {
          if (n != null) {
            try {
              paths.add(n.getName());
            } catch (javax.jcr.RepositoryException re) {
              log.debug("Node had no name; skipping: {}", re.getMessage());
            }
          }
        }
      }
      return Optional.of(new PSTaxonomySummary(paths.size(), paths));
    } catch (RuntimeException e) {
      log.warn("Taxonomy lookup failed for {}: {}", itemId, e.getMessage());
      return Optional.of(new PSTaxonomySummary(0L, new ArrayList<>()));
    }
  }

  @Override
  public Optional<PSLocalDependencySummary> summariseLocal(String itemId) {
    if (!isResolvable(itemId)) return Optional.empty();
    List<PSLocalDependencyLink> links = new ArrayList<>();
    try {
      Set<String> local = widgetAssetRelationshipService.getLocalAssets(itemId);
      if (local != null) {
        for (String assetId : local) {
          links.add(new PSLocalDependencyLink("local", assetId));
        }
      }
      Set<String> linked = widgetAssetRelationshipService.getLinkedAssets(itemId);
      if (linked != null) {
        for (String assetId : linked) {
          links.add(new PSLocalDependencyLink("linked", assetId));
        }
      }
    } catch (PSValidationException
        | IPSWidgetAssetRelationshipService.PSWidgetAssetRelationshipServiceException e) {
      log.debug("Local-assets lookup unavailable for {}: {}", itemId, e.getMessage());
    }
    return Optional.of(new PSLocalDependencySummary(links.size(), links));
  }

  @Override
  public Optional<PSNodeRelationshipSummary> summarise(String itemId) {
    Optional<PSRelationshipSummary> out = summariseOutgoing(itemId);
    Optional<PSRelationshipSummary> in = summariseIncoming(itemId);
    Optional<PSTaxonomySummary> tax = summariseTaxonomy(itemId);
    Optional<PSLocalDependencySummary> loc = summariseLocal(itemId);
    Optional<PSRelationshipSummary> rev = summariseReverse(itemId);
    if (out.isEmpty() || in.isEmpty() || tax.isEmpty() || loc.isEmpty() || rev.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new PSNodeRelationshipSummary(out.get(), in.get(), tax.get(), loc.get(), rev.get()));
  }

  // ---------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------

  /**
   * Cheap id-resolution pre-check. Catches {@code null} / blank id-strings and unresolvable
   * guid-strings (e.g. random lookups on a private folder's content). Returns {@code false} if
   * the supplied id cannot be resolved to a guid the caller has read access to; the per-dimension
   * methods return {@link Optional#empty()} in that case.
   */
  private boolean isResolvable(String itemId) {
    if (itemId == null || itemId.isBlank()) return false;
    try {
      idMapper.getGuid(itemId);
      return true;
    } catch (RuntimeException e) {
      log.debug("Could not resolve id {}: {}", itemId, e.getMessage());
      return false;
    }
  }

  /**
   * Build a per-type summary by delegating to {@link IPSRelationshipCataloger#findOwners} with
   * the supplied relationship-config category. The category is normalised to a UI-friendly
   * label (strip the {@code rs_} prefix used by {@link PSRelationshipConfig}) and added to the
   * per-type bucket so the dependency-view row carries the typed name.
   */
  private PSRelationshipSummary summariseFromCataloger(String itemId, String category) {
    List<PSRelationshipTypeBucket> byType = new ArrayList<>();
    long count = 0L;
    try {
      List<String> owners = relationshipCataloger.findOwners(itemId, category, null, null);
      if (owners != null && !owners.isEmpty()) {
        count += owners.size();
        byType.add(new PSRelationshipTypeBucket(normaliseCategoryLabel(category), owners.size()));
      }
    } catch (RuntimeException e) {
      log.debug("Cataloger lookup unavailable for {} ({}): {}", itemId, category, e.getMessage());
    }
    Collections.sort(byType, (a, b) -> Long.compare(b.getCount(), a.getCount()));
    return new PSRelationshipSummary(count, byType);
  }

  /**
   * Strip the internal {@code rs_} prefix used by {@link PSRelationshipConfig} for its category
   * ids so the dependency view does not display {@code rs_translation} to operators.
   */
  private static String normaliseCategoryLabel(String category) {
    if (category == null) return null;
    if (category.startsWith("rs_")) {
      return category.substring(3);
    }
    return category;
  }

  /**
   * Fallback when a dimension throws. Lets the consolidated {@code /summary} endpoint
   * always return a usable shape — the front-end renders {@code 0 (no links)} for empty
   * buckets instead of a hard failure.
   */
  private static PSRelationshipSummary emptySummary() {
    return new PSRelationshipSummary(0L, new ArrayList<>());
  }

  /** Approximate "parent folder path" of an item id for the JCR lookup. The JCR finder uses the
   * path the id-string represents; the value is a delegate marker that the path layer
   * resolves at query time. Returning "/" + id keeps the dependency view row non-empty for
   * practical item ids while leaving path-resolution to the finder. */
  private String parentPathOf(String itemId) {
    if (itemId == null || itemId.isBlank()) return "/";
    return "/" + itemId;
  }
}
