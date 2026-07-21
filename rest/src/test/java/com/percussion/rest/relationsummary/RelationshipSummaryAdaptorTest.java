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
package com.percussion.rest.relationsummary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.percussion.share.relationship.data.PSLocalDependencySummary;
import com.percussion.share.relationship.data.PSNodeRelationshipSummary;
import com.percussion.share.relationship.data.PSRelationshipSummary;
import com.percussion.share.relationship.data.PSTaxonomySummary;
import com.percussion.share.relationship.service.IPSRelationshipSummaryService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Service-contract tests for {@link RelationshipSummaryAdaptor} (US8 / T099).
 *
 * <p>Three axes per constitution III/IV:
 *
 * <ul>
 *   <li><strong>happy path</strong>: the adaptor delegates to the service and returns the DTO.
 *   <li><strong>AuthZ negative</strong>: when the service returns {@code Optional.empty()},
 *       the adaptor throws {@link WebApplicationException} with HTTP 403.
 *   <li><strong>wire envelope</strong>: the DTOs the adaptor returns are Jackson-serialisable
 *       ({@code @JsonRootName}; verified via reflection).
 * </ul>
 *
 * <p>The {@link RelationshipSummaryResource} HTTP-layer logic is exercised in the larger
 * {@code rest/} module test suite; these tests scope the adaptor contract itself.
 */
@ExtendWith(MockitoExtension.class)
class RelationshipSummaryAdaptorTest {

  @Mock private IPSRelationshipSummaryService service;

  private RelationshipSummaryAdaptor adaptor;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
    adaptor = new RelationshipSummaryAdaptor(service);
  }

  @Test
  void outgoingReturnsServiceResult() {
    PSRelationshipSummary summary = new PSRelationshipSummary(3L, Collections.emptyList());
    when(service.summariseOutgoing("123")).thenReturn(Optional.of(summary));

    PSRelationshipSummary out = adaptor.outgoing(URI.create("http://localhost/api"), "123");

    assertEquals(3L, out.getCount());
  }

  @Test
  void outgoingReturns403WhenServiceReturnsEmpty() {
    when(service.summariseOutgoing(any())).thenReturn(Optional.empty());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.outgoing(URI.create("http://localhost/api"), "private"));

    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
    assertEquals("Cannot summarise outgoing for private", ex.getMessage());
  }

  @Test
  void incomingReturnsServiceResult() {
    when(service.summariseIncoming("456"))
        .thenReturn(Optional.of(new PSRelationshipSummary(1L, Collections.emptyList())));

    PSRelationshipSummary out = adaptor.incoming(URI.create("http://localhost/api"), "456");
    assertEquals(1L, out.getCount());
  }

  @Test
  void taxonomyReturnsServiceResult() {
    when(service.summariseTaxonomy("folder-1"))
        .thenReturn(Optional.of(new PSTaxonomySummary(2L, java.util.List.of("a", "b"))));

    PSTaxonomySummary out = adaptor.taxonomy(URI.create("http://localhost/api"), "folder-1");
    assertEquals(2L, out.getCount());
  }

  @Test
  void localReturnsServiceResult() {
    when(service.summariseLocal("page-1"))
        .thenReturn(Optional.of(new PSLocalDependencySummary(3L, Collections.emptyList())));

    PSLocalDependencySummary out = adaptor.local(URI.create("http://localhost/api"), "page-1");
    assertEquals(3L, out.getCount());
  }

  @Test
  void reverseReturnsServiceResult() {
    when(service.summariseReverse("999"))
        .thenReturn(Optional.of(new PSRelationshipSummary(5L, Collections.emptyList())));

    PSRelationshipSummary out = adaptor.reverse(URI.create("http://localhost/api"), "999");
    assertEquals(5L, out.getCount());
  }

  @Test
  void summaryReturnsServiceResult() {
    PSNodeRelationshipSummary consolidated =
        new PSNodeRelationshipSummary(
            new PSRelationshipSummary(1L, Collections.emptyList()),
            new PSRelationshipSummary(2L, Collections.emptyList()),
            new PSTaxonomySummary(0L, Collections.emptyList()),
            new PSLocalDependencySummary(0L, Collections.emptyList()),
            new PSRelationshipSummary(3L, Collections.emptyList()));
    when(service.summarise(eq("node-1"))).thenReturn(Optional.of(consolidated));

    PSNodeRelationshipSummary out = adaptor.summary(URI.create("http://localhost/api"), "node-1");
    assertEquals(1L, out.getOutgoing().getCount());
    assertEquals(2L, out.getIncoming().getCount());
    assertEquals(3L, out.getReverse().getCount());
    assertEquals(0L, out.getTaxonomy().getCount());
    assertEquals(0L, out.getLocal().getCount());
  }

  @Test
  void summaryReturns403WhenServiceReturnsEmpty() {
    when(service.summarise(any())).thenReturn(Optional.empty());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.summary(URI.create("http://localhost/api"), "private"));
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
  }
}
