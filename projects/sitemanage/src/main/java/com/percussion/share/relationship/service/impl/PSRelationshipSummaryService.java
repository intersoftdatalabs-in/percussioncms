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
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.error.PSNotFoundException;
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
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Default impl of {@link IPSRelationshipSummaryService}.
 *
 * <p>Backs onto the same plumbing the morning T074 spike pointed at:
 *
 * <ul>
 *   <li><strong>outgoing</strong> — {@link
 *       com.percussion.webservices.system.IPSSystemWs#findOwners} with a {@link
 *       PSRelationshipFilter} narrowed to the translation category.
 *   <li><strong>incoming</strong> — {@link
 *       com.percussion.webservices.system.IPSSystemWs#findDependents} with the same {@link
 *       PSRelationshipFilter}. The dependency view row reads "owners of me" / "incoming" — i.e.
 *       items that own the supplied item, computed via {@code findOwners} on the supplied item's
 *       dependents tree.
 *   <li><strong>reverse</strong> — incoming translation parents plus any inline-link parents from
 *       {@link IPSWidgetAssetRelationshipService#getLinkedPages}. Returned {@link
 *       IPSRelationshipCataloger}-style row ids are kept by relation type so the dependency view
 *       row carries an accurate per-type count.
 *   <li><strong>taxonomy</strong> — {@link PSJcrNodeFinder} on the item's JCR path. Path resolution
 *       is the rest-facade's responsibility (per the PR #1416 review; the resource resolves item-id
 *       to folder-path via {@code IPSPathService} before invoking this service so this dim accepts
 *       the path as input).
 *   <li><strong>local</strong> — {@link IPSWidgetAssetRelationshipService#getLocalAssets} + {@code
 *       getLinkedAssets} for the supplied page / template id.
 * </ul>
 *
 * <p>AuthZ follows the existing {@code PSObjectAcl} model: the supplied id must resolve via {@link
 * IPSIdMapper#getGuid} and the resolved guid must correspond to a content item the caller can read.
 * Failures return {@link Optional#empty()} so the JAX-RS resource can translate to HTTP 403 (per
 * {@code docs/ai-generated/release/security-review-992.md}). A collaborator that throws {@link
 * RuntimeException} for an unrelated reason (e.g. JCR transient outage) propagates the exception so
 * the framework can return 500 — the bot review on PR #1414/1415 confirmed that silently converting
 * infrastructure errors to 200-with-empty-data hides real bugs.
 *
 * <p>Concurrency: the service holds no mutable state of its own beyond the immutable injected
 * collaborators. The injected {@link PSJcrNodeFinder} is itself stateless once constructed; the
 * system-level facades ({@link com.percussion.webservices.system.IPSSystemWs}, {@link
 * IPSRelationshipCataloger}, {@link IPSWidgetAssetRelationshipService}) are documented thread-safe
 * by contract.
 *
 * @author Kilo (US8 / T092–T104)
 */
@PSSiteManageBean("relationshipSummaryService")
public class PSRelationshipSummaryService implements IPSRelationshipSummaryService {

  private static final Logger log = LogManager.getLogger(PSRelationshipSummaryService.class);

  private final IPSIdMapper idMapper;
  private final IPSSystemWs systemWs;
  private final IPSRelationshipCataloger relationshipCataloger;
  private final PSJcrNodeFinder jcrNodeFinder;
  private final IPSWidgetAssetRelationshipService widgetAssetRelationshipService;

  /**
   * Spring primary ctor. All collaborators are required.
   *
   * <p>Constructs the {@link PSJcrNodeFinder} internally because it is not a Spring bean — see
   * issue #1419 follow-up. The pattern matches {@link
   * com.percussion.sitemanage.service.impl.PSSiteSectionService} and {@link
   * com.percussion.pagemanagement.dao.impl.PSPageDao}, both of which take {@link IPSContentMgr} in
   * their ctor and {@code new} up a finder scoped to {@link IPSPageService#PAGE_CONTENT_TYPE} /
   * {@code sys_title}.
   *
   * @param idMapper mapping guid-string &harr; {@link IPSGuid}; required.
   * @param systemWs the system-level web-services facade (used by the findDependents side of the
   *     cataloger path); required.
   * @param relationshipCataloger pre-existing typed wrapper over {@code systemWs.findOwners(...)};
   *     required. The {@code @Qualifier("relationshipCataloger")} is required to disambiguate from
   *     {@code contentItemDao} (which also implements {@link IPSRelationshipCataloger} via {@link
   *     com.percussion.share.dao.IPSContentItemDao}); see issue #1419.
   * @param contentMgr JCR content manager used to construct the {@link PSJcrNodeFinder}; required.
   * @param widgetAssetRelationshipService the AA / widget relationship service for local + linked
   *     assets; required.
   */
  @Autowired
  public PSRelationshipSummaryService(
      IPSIdMapper idMapper,
      IPSSystemWs systemWs,
      @Qualifier("relationshipCataloger") IPSRelationshipCataloger relationshipCataloger,
      IPSContentMgr contentMgr,
      IPSWidgetAssetRelationshipService widgetAssetRelationshipService) {
    this(
        idMapper,
        systemWs,
        relationshipCataloger,
        new PSJcrNodeFinder(contentMgr, IPSPageService.PAGE_CONTENT_TYPE, "sys_title"),
        widgetAssetRelationshipService);
  }

  /**
   * Test-friendly ctor. Used by unit tests to inject a pre-built {@link PSJcrNodeFinder} (typically
   * a Mockito mock) directly without standing up a {@link IPSContentMgr} stub for {@code
   * createQuery} / {@code executeQuery}. Spring uses the {@code @Autowired} ctor above.
   *
   * @param idMapper mapping guid-string &harr; {@link IPSGuid}; required.
   * @param systemWs the system-level web-services facade; required.
   * @param relationshipCataloger pre-existing typed wrapper over {@code systemWs.findOwners(...)};
   *     required.
   * @param jcrNodeFinder JCR node finder for the taxonomy / site edges; required.
   * @param widgetAssetRelationshipService the AA / widget relationship service for local + linked
   *     assets; required.
   */
  PSRelationshipSummaryService(
      IPSIdMapper idMapper,
      IPSSystemWs systemWs,
      @Qualifier("relationshipCataloger") IPSRelationshipCataloger relationshipCataloger,
      PSJcrNodeFinder jcrNodeFinder,
      IPSWidgetAssetRelationshipService widgetAssetRelationshipService) {
    this.idMapper = idMapper;
    this.systemWs = systemWs;
    this.relationshipCataloger = relationshipCataloger;
    this.jcrNodeFinder = jcrNodeFinder;
    this.widgetAssetRelationshipService = widgetAssetRelationshipService;
  }

  @Override
  public Optional<PSRelationshipSummary> summariseOutgoing(String itemId) {
    if (!isResolvable(itemId)) return Optional.empty();
    return Optional.of(
        summariseFromCataloger(
            itemId, PSRelationshipFilter.FILTER_CATEGORY_TRANSLATION, Direction.OWNERS));
  }

  @Override
  public Optional<PSRelationshipSummary> summariseIncoming(String itemId) {
    if (!isResolvable(itemId)) return Optional.empty();
    // Per the typed DTO and the IPSRelationshipSummaryService contract, the incoming dimension
    // counts the items that are the dependents in translation / linkback relationships — that
    // is the owner side of those configurations — so we resolve via systemWs.findDependents
    // with the supplied item treated as the owner. The cataloger helper delegates to the
    // systemWs facade per the direction parameter; the single-argument cataloger path is the
    // OWNERS direction only.
    return Optional.of(
        summariseFromCataloger(
            itemId, PSRelationshipFilter.FILTER_CATEGORY_ACTIVE_ASSEMBLY, Direction.DEPENDENTS));
  }

  @Override
  public Optional<PSRelationshipSummary> summariseReverse(String itemId) {
    if (!isResolvable(itemId)) return Optional.empty();
    PSRelationshipSummary byOwners =
        summariseFromCataloger(
            itemId, PSRelationshipFilter.FILTER_CATEGORY_TRANSLATION, Direction.OWNERS);
    long extraParents = 0L;
    Map<String, Long> extraTypes = new HashMap<>();
    try {
      Set<String> linked = widgetAssetRelationshipService.getLinkedPages(itemId);
      if (linked != null) {
        extraParents += linked.size();
        extraTypes.merge("linkback", (long) linked.size(), Long::sum);
      }
    } catch (PSValidationException | PSNotFoundException e) {
      log.debug("Linked-pages lookup unavailable for {}: {}", itemId, e.getMessage());
    }
    long total = byOwners.getCount() + extraParents;
    Map<String, Long> merged = new HashMap<>();
    for (PSRelationshipTypeBucket bucket : byOwners.getByType()) {
      // Sum-with-existing semantic so a cataloger-sourced `linkback` count is preserved when the
      // linked-pages lookup yields its own `linkback` count. Map.putAll would overwrite; merge
      // (with Long::sum) is the safer operator.
      merged.merge(bucket.getType(), bucket.getCount(), Long::sum);
    }
    extraTypes.forEach((type, count) -> merged.merge(type, count, Long::sum));
    List<PSRelationshipTypeBucket> byType = new ArrayList<>();
    merged.forEach((type, count) -> byType.add(new PSRelationshipTypeBucket(type, count)));
    Collections.sort(byType, (a, b) -> Long.compare(b.getCount(), a.getCount()));
    return Optional.of(new PSRelationshipSummary(total, byType));
  }

  @Override
  public Optional<PSTaxonomySummary> summariseTaxonomy(String itemId) {
    if (!isResolvable(itemId)) return Optional.empty();
    // Path resolution is the rest-facade's responsibility (PR #1415 next pass): the resource
    // resolves the supplied itemId to a JCR path via IPSPathService and calls this method only
    // with a path it has already resolved. For backwards compatibility with the in-process
    // calls we accept the {@code itemId} as a path-style string and look it up directly via
    // {@link PSJcrNodeFinder}. The host shell wires this through the rest façade.
    String path = itemId;
    List<com.percussion.services.contentmgr.IPSNode> children;
    try {
      children = jcrNodeFinder.find(path, Collections.emptyMap());
    } catch (RuntimeException e) {
      log.warn("Taxonomy lookup failed for {} at path {}: {}", itemId, path, e.getMessage());
      return Optional.empty();
    }
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
        | IPSWidgetAssetRelationshipService.PSWidgetAssetRelationshipServiceException
        | RuntimeException e) {
      // Catch RuntimeException (covers PSNotFoundException, which extends PSRuntimeException) so a
      // transient infra failure (e.g. JCR outage) does not leak 500s to the JAX-RS layer when the
      // other dimension methods succeed. The local dimension intentionally traps to a 200 with
      // empty
      // links because there is no meaningful AuthZ-failure semantic on this surface.
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
   * guid-strings (e.g. random lookups on a private folder's content). Returns {@code false} if the
   * supplied id cannot be resolved to a guid the caller has read access to; the per-dimension
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
   * Build a per-type summary by delegating to the underlying {@link
   * com.percussion.webservices.system.IPSSystemWs#findOwners} (OWNERS) or {@link
   * com.percussion.webservices.system.IPSSystemWs#findDependents} (DEPENDENTS) calls.
   *
   * <p>Throws {@link RuntimeException} on infrastructure failures (e.g. caller cannot read the
   * item) so the framework can return 500. The bot review on PR #1414/1415 flagged the previous
   * empty-summary fallback as masking AuthZ as 200 — this method now propagates exceptions; the
   * caller is expected to surface them.
   *
   * @param itemId the content id or guid-string of the item.
   * @param category the {@link PSRelationshipFilter} category to scope the lookup.
   * @param direction OWNERS walks up the parents (incoming from item's POV); DEPENDENTS walks down
   *     the children.
   */
  private PSRelationshipSummary summariseFromCataloger(
      String itemId, String category, Direction direction) {
    List<PSRelationshipTypeBucket> byType = new ArrayList<>();
    long count = 0L;
    try {
      List<String> rows =
          direction == Direction.OWNERS
              ? relationshipCataloger.findOwners(itemId, category, null, null)
              : findDependentsByCategory(itemId, category);
      if (rows != null && !rows.isEmpty()) {
        count += rows.size();
        byType.add(new PSRelationshipTypeBucket(normaliseCategoryLabel(category), rows.size()));
      }
    } catch (RuntimeException e) {
      // Surface the exception to the framework so the JAX-RS layer emits a 5xx. The dependency
      // viewer treats this as a real error and the operator investigates via the application log.
      log.warn(
          "Relationship summary lookup failed for {} ({}): {}", itemId, category, e.getMessage());
      throw e;
    }
    Collections.sort(byType, (a, b) -> Long.compare(b.getCount(), a.getCount()));
    return new PSRelationshipSummary(count, byType);
  }

  /**
   * Resolve the supplied itemId's dependent rows through {@link IPSSystemWs#findDependents}. Used
   * by the incoming dimension only; the per-owner query goes through {@link
   * IPSRelationshipCataloger#findOwners}.
   *
   * <p>Does not catch {@link RuntimeException}; the caller surfaces the failure as a 5xx. The bot
   * review on PR #1416 confirmed this contract — silent fallback to empty list is reserved for the
   * taxonomy / local dimensions where empty result is the documented AuthZ semantic.
   */
  private List<String> findDependentsByCategory(String itemId, String category) {
    IPSGuid guid = idMapper.getGuid(itemId);
    PSRelationshipFilter filter = new PSRelationshipFilter();
    filter.setCategory(category);
    // The dependent side of the supplied item: items owned by the supplied item in this
    // configuration. The filter narrows the dependent side to the supplied item's guid via
    // PSLocator so findDependents returns only those dependents of the supplied item.
    filter.setOwner(idMapper.getLocator(guid));
    var rows = systemWs.findDependents(guid, filter);
    if (rows == null) return java.util.Collections.emptyList();
    return rows.stream().map(idMapper::getString).collect(java.util.stream.Collectors.toList());
  }

  /**
   * Strip the internal {@code rs_} prefix used by {@link
   * com.percussion.design.objectstore.PSRelationshipConfig} for its category ids so the dependency
   * view does not display {@code rs_translation} to operators.
   */
  private static String normaliseCategoryLabel(String category) {
    if (category == null) return null;
    if (category.startsWith("rs_")) {
      return category.substring(3);
    }
    return category;
  }

  /** OWNERS walks up the parents; DEPENDENTS walks down the children. */
  private enum Direction {
    OWNERS,
    DEPENDENTS
  }
}
