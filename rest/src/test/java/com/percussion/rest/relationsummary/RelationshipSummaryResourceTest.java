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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.rest.relationsummary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.share.relationship.data.PSExplorerRelationshipList;
import com.percussion.share.relationship.data.PSLocalDependencySummary;
import com.percussion.share.relationship.data.PSNodeRelationshipSummary;
import com.percussion.share.relationship.data.PSRelationshipSummary;
import com.percussion.share.relationship.data.PSTaxonomySummary;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the {@link RelationshipSummaryResource} HTTP-layer (US8 / T098 happy path).
 *
 * <p>AuthZ failure and wire-envelope round-trip are exercised by the adaptor unit tests; here we
 * only confirm that the {@link Response} builders return 200 with the expected body shape.
 */
@ExtendWith(MockitoExtension.class)
class RelationshipSummaryResourceTest {

  @Mock private IRelationshipSummaryAdaptor adaptor;

  @Mock private UriInfo uriInfo;

  private RelationshipSummaryResource resource;

  @BeforeEach
  void init() {
    resource = new RelationshipSummaryResource(adaptor);
    resource.setUriInfo(uriInfo);
    lenient()
        .when(uriInfo.getBaseUri())
        .thenReturn(UriBuilder.fromUri("http://localhost/api").build());
  }

  @Test
  void outgoingReturns200WithBody() {
    PSRelationshipSummary summary = new PSRelationshipSummary(3L, Collections.emptyList());
    when(adaptor.outgoing(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("123")))
        .thenReturn(summary);

    Response resp = resource.outgoing("123");

    assertEquals(200, resp.getStatus());
    assertEquals(summary, resp.getEntity());
  }

  @Test
  void taxonomyReturns200WithBody() {
    PSTaxonomySummary summary = new PSTaxonomySummary(2L, java.util.List.of("a"));
    when(adaptor.taxonomy(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("folder-1")))
        .thenReturn(summary);

    Response resp = resource.taxonomy("folder-1");
    assertEquals(200, resp.getStatus());
    assertEquals(summary, resp.getEntity());
  }

  @Test
  void localReturns200WithBody() {
    PSLocalDependencySummary summary = new PSLocalDependencySummary(4L, Collections.emptyList());
    when(adaptor.local(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("page-1")))
        .thenReturn(summary);

    Response resp = resource.local("page-1");
    assertEquals(200, resp.getStatus());
    assertEquals(summary, resp.getEntity());
  }

  @Test
  void summaryReturns200WithBody() {
    PSNodeRelationshipSummary consolidated =
        new PSNodeRelationshipSummary(
            new PSRelationshipSummary(0L, Collections.emptyList()),
            new PSRelationshipSummary(0L, Collections.emptyList()),
            new PSTaxonomySummary(0L, Collections.emptyList()),
            new PSLocalDependencySummary(0L, Collections.emptyList()),
            new PSRelationshipSummary(0L, Collections.emptyList()));
    when(adaptor.summary(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("node-1")))
        .thenReturn(consolidated);

    Response resp = resource.summary("node-1");
    assertEquals(200, resp.getStatus());
    assertEquals(consolidated, resp.getEntity());
  }

  @Test
  void removeEdgeReturns204() {
    Response resp = resource.removeEdge("42", 7);
    assertEquals(204, resp.getStatus());
    verify(adaptor).removeEdge(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("42"), org.mockito.ArgumentMatchers.eq(7));
  }

  @Test
  void removeEdgeRejectsNonPositiveId() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.removeEdge("42", 0));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void edgesReturnsList() {
    PSExplorerRelationshipList list = new PSExplorerRelationshipList();
    when(adaptor.listEdges(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("42")))
        .thenReturn(list);
    Response resp = resource.edges("42");
    assertEquals(200, resp.getStatus());
    assertEquals(list, resp.getEntity());
  }
}
