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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.share.dao.IPSRelationshipCataloger;
import com.percussion.share.dao.PSJcrNodeFinder;
import com.percussion.share.relationship.data.PSNodeRelationshipSummary;
import com.percussion.share.relationship.service.IPSRelationshipSummaryService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.system.IPSSystemWs;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Pure unit tests for {@link PSRelationshipSummaryService} (US8 / T097).
 *
 * <p>Mocks the six collaborators and asserts the summary shape per dimension plus the
 * consolidated {@code /summary} endpoint. AuthZ denial is exercised by failing the guid
 * resolution and asserting {@link Optional#empty()}.
 */
class PSRelationshipSummaryServiceTest {

  @Mock private IPSIdMapper idMapper;
  @Mock private PSItemDefManager itemDefManager;
  @Mock private IPSSystemWs systemWs;
  @Mock private IPSRelationshipCataloger relationshipCataloger;
  @Mock private PSJcrNodeFinder jcrNodeFinder;
  @Mock private IPSWidgetAssetRelationshipService widgetAssetRelationshipService;

  private IPSRelationshipSummaryService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service =
        new PSRelationshipSummaryService(
            idMapper,
            itemDefManager,
            systemWs,
            relationshipCataloger,
            jcrNodeFinder,
            widgetAssetRelationshipService);
  }

  @Test
  void summariseOutgoingReportsTranslationRows() {
    IPSGuid guid = stubGuid("123");
    when(relationshipCataloger.findOwners(
            org.mockito.ArgumentMatchers.eq("123"), anyString(), any(), any()))
        .thenReturn(List.of("a", "b", "c"));

    Optional<com.percussion.share.relationship.data.PSRelationshipSummary> out =
        service.summariseOutgoing("123");

    assertTrue(out.isPresent());
    assertEquals(3L, out.get().getCount());
    assertEquals(1, out.get().getByType().size());
    assertEquals("translation", out.get().getByType().get(0).getType());
  }

  @Test
  void summariseIncomingReportsDependents() {
    when(relationshipCataloger.findOwners(
            org.mockito.ArgumentMatchers.eq("456"), anyString(), any(), any()))
        .thenReturn(List.of("only-1"));

    Optional<com.percussion.share.relationship.data.PSRelationshipSummary> out =
        service.summariseIncoming("456");

    assertTrue(out.isPresent());
    assertEquals(1L, out.get().getCount());
  }

  @Test
  void summariseOutgoingReturnsEmptyOnIdResolutionFailure() {
    when(idMapper.getGuid("missing")).thenThrow(new RuntimeException("not found"));

    Optional<com.percussion.share.relationship.data.PSRelationshipSummary> out =
        service.summariseOutgoing("missing");

    assertFalse(out.isPresent());
  }

  @Test
  void summariseReverseCombinesDependentsAndLinkedPages() throws Exception {
    when(relationshipCataloger.findOwners(
            org.mockito.ArgumentMatchers.eq("777"),
            org.mockito.ArgumentMatchers.eq(PSRelationshipFilter.FILTER_CATEGORY_TRANSLATION),
            any(),
            any()))
        .thenReturn(List.of("d1"));
    Set<String> linked = new HashSet<>();
    linked.add("parent-1");
    linked.add("parent-2");
    when(widgetAssetRelationshipService.getLinkedPages("777")).thenReturn(linked);

    Optional<com.percussion.share.relationship.data.PSRelationshipSummary> out =
        service.summariseReverse("777");

    assertTrue(out.isPresent());
    assertEquals(3L, out.get().getCount());
    assertEquals(2, out.get().getByType().size());
    // Sorted descending by count: linkback (2) before translation (1)
    assertEquals("linkback", out.get().getByType().get(0).getType());
    assertEquals("translation", out.get().getByType().get(1).getType());
  }

  @Test
  void summariseLocalAggregatesLocalAndLinked() throws Exception {
    Set<String> local = new HashSet<>();
    local.add("asset-a");
    local.add("asset-b");
    when(widgetAssetRelationshipService.getLocalAssets("page-1")).thenReturn(local);
    Set<String> linked = new HashSet<>();
    linked.add("asset-c");
    when(widgetAssetRelationshipService.getLinkedAssets("page-1")).thenReturn(linked);

    Optional<com.percussion.share.relationship.data.PSLocalDependencySummary> out =
        service.summariseLocal("page-1");

    assertTrue(out.isPresent());
    assertEquals(3L, out.get().getCount());
    assertEquals(3, out.get().getLinks().size());
  }

  @Test
  void summariseTaxonomyReportsChildNodes() {
    com.percussion.services.contentmgr.IPSNode node = stubNode("child-1");
    com.percussion.services.contentmgr.IPSNode node2 = stubNode("child-2");
    Map<String, String> emptyWhere = new HashMap<>();
    when(jcrNodeFinder.find(anyString(), org.mockito.ArgumentMatchers.<Map<String, String>>any()))
        .thenReturn(List.of(node, node2));

    Optional<com.percussion.share.relationship.data.PSTaxonomySummary> out =
        service.summariseTaxonomy("folder-1");

    assertTrue(out.isPresent());
    assertEquals(2L, out.get().getCount());
  }

  @Test
  void summariseToleratesCatalogerRuntimeExceptions() throws Exception {
    when(relationshipCataloger.findOwners(anyString(), anyString(), any(), any()))
        .thenThrow(new RuntimeException("cataloger down"));

    Optional<PSNodeRelationshipSummary> out = service.summarise("ok");

    // Each per-dimension method catches RuntimeException and substitutes an empty summary,
    // so the consolidated endpoint still returns a usable Optional with all-empty buckets.
    // The dependency viewer renders "0 (no links)" rather than a hard failure.
    assertTrue(out.isPresent());
    assertEquals(0L, out.get().getOutgoing().getCount());
    assertEquals(0L, out.get().getIncoming().getCount());
    assertEquals(0L, out.get().getReverse().getCount());
  }

  @Test
  void summariseReturnsEmptyWhenIdDoesNotResolve() {
    when(idMapper.getGuid("missing")).thenThrow(new RuntimeException("not found"));

    Optional<PSNodeRelationshipSummary> out = service.summarise("missing");

    assertFalse(out.isPresent());
  }

  @Test
  void rejectBlankedIdStrings() {
    assertFalse(service.summariseOutgoing(null).isPresent());
    assertFalse(service.summariseOutgoing("").isPresent());
    assertFalse(service.summariseOutgoing("   ").isPresent());
  }

  // ---- helpers ----

  private IPSGuid stubGuid(String id) {
    IPSGuid guid = org.mockito.Mockito.mock(IPSGuid.class);
    when(idMapper.<String>getGuid(id)).thenReturn(guid);
    when(idMapper.getString(guid)).thenReturn(id);
    when(idMapper.getLocator(guid)).thenReturn(new PSLocator(0, 1));
    return guid;
  }

  private com.percussion.services.contentmgr.IPSNode stubNode(String name) {
    com.percussion.services.contentmgr.IPSNode node =
        org.mockito.Mockito.mock(com.percussion.services.contentmgr.IPSNode.class);
    try {
      when(node.getName()).thenReturn(name);
    } catch (javax.jcr.RepositoryException re) {
      throw new RuntimeException(re);
    }
    return node;
  }
}
