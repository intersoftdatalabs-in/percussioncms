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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.itemmanagement.data.PSItemUserInfo;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService.PSItemWorkflowServiceException;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.rest.explorer.ItemIdentity;
import com.percussion.server.PSRequest;
import com.percussion.server.PSUserSession;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.service.exception.PSValidationException;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class ItemIdentityAdaptorTest {

  @Mock private IPSItemWorkflowService workflow;
  @Mock private IPSWorkflowHelper workflowHelper;

  private ItemIdentityAdaptor adaptor;
  private final URI base = URI.create("http://localhost/rest");

  @BeforeEach
  void init() {
    adaptor = new ItemIdentityAdaptor(workflow, workflowHelper);
  }

  @Test
  void batchResolvesTokenAndNumericIds() throws Exception {
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "admin", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(summary(101L, 2, 5, 1001));
    List<ItemIdentity> out = lookupWithCommunities("42", List.of("1001"));
    assertEquals(1, out.size());
    ItemIdentity identity = out.get(0);
    assertEquals("42", identity.getId());
    assertEquals("Checked Out By Me", identity.getCheckoutStatus());
    assertEquals(101L, identity.getContentTypeId());
    assertEquals(2, identity.getObjectType());
    assertEquals(5, identity.getWorkflowId());
    assertEquals(1001, identity.getCommunityId());
    assertNull(identity.getAssignmentType());
    assertNull(identity.getPublishableType());
  }

  @Test
  void batchOtherUserCheckoutToken() throws Exception {
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "editor", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(summary(101L, 2, 5, 1001));
    assertEquals(
        "Checked Out", lookupWithCommunities("42", List.of("1001")).get(0).getCheckoutStatus());
  }

  @Test
  void batchNoCheckoutIsEmptyToken() throws Exception {
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(summary(101L, 2, 5, 1001));
    assertEquals("", lookupWithCommunities("42", List.of("1001")).get(0).getCheckoutStatus());
  }

  @Test
  void batchNonPositiveIdsMapToNull() throws Exception {
    when(workflow.lookupCheckoutOwner("7"))
        .thenReturn(new PSItemUserInfo("Sites", "", "admin", "Reader"));
    // Folders / all-community rows: workflow 0 and community -1 are "no id", not id 0.
    when(workflowHelper.getComponentSummary("7")).thenReturn(summary(0L, 1, 0, -1));
    ItemIdentity identity = adaptor.lookupBatch(base, List.of("7")).get(0);
    assertNull(identity.getContentTypeId());
    assertNull(identity.getWorkflowId());
    assertNull(identity.getCommunityId());
  }

  @Test
  void batchSkipsFailedRows() throws Exception {
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(summary(101L, 2, 5, 1001));
    when(workflow.lookupCheckoutOwner("missing"))
        .thenThrow(new PSItemWorkflowServiceException("not found"));
    List<ItemIdentity> out;
    try (MockedStatic<PSSecurityFilter> filter = Mockito.mockStatic(PSSecurityFilter.class)) {
      stubSession(filter, List.of("1001"), null);
      out = adaptor.lookupBatch(base, Arrays.asList("42", " ", "missing"));
    }
    assertEquals(1, out.size());
    assertEquals("42", out.get(0).getId());
  }

  @Test
  void batchOmitsUnauthorizedLikeUnknown() throws Exception {
    // Review #6814: an unauthorized id (typed validation failure from the service
    // layer) is omitted exactly like an unknown id — no metadata leaks.
    when(workflow.lookupCheckoutOwner("denied"))
        .thenThrow(new DeniedValidationException("not authorized"));
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(summary(101L, 2, 5, 1001));
    List<ItemIdentity> out;
    try (MockedStatic<PSSecurityFilter> filter = Mockito.mockStatic(PSSecurityFilter.class)) {
      stubSession(filter, List.of("1001"), null);
      out = adaptor.lookupBatch(base, Arrays.asList("42", "denied"));
    }
    assertEquals(1, out.size());
    assertEquals("42", out.get(0).getId());
  }

  @Test
  void batchOmitsNullSummary() throws Exception {
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(null);
    assertTrue(adaptor.lookupBatch(base, List.of("42")).isEmpty());
  }

  @Test
  void batchOmitsOutsideCallerCommunities() throws Exception {
    // Review #6814: summary loaders perform no caller-AuthZ check, so the adaptor
    // omits rows outside the caller's session communities instead of disclosing
    // their content-type / workflow / community ids and checkout owner.
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(summary(101L, 2, 5, 9999));
    try (MockedStatic<PSSecurityFilter> filter = Mockito.mockStatic(PSSecurityFilter.class)) {
      PSRequest req = Mockito.mock(PSRequest.class);
      PSUserSession session = Mockito.mock(PSUserSession.class);
      filter.when(PSSecurityFilter::getCurrentRequest).thenReturn(req);
      when(req.getUserSession()).thenReturn(session);
      when(session.getUserCommunities(req)).thenReturn(List.of("1001"));
      assertTrue(adaptor.lookupBatch(base, List.of("42")).isEmpty());
    }
  }

  @Test
  void batchKeepsInsideCallerCommunities() throws Exception {
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(summary(101L, 2, 5, 1001));
    try (MockedStatic<PSSecurityFilter> filter = Mockito.mockStatic(PSSecurityFilter.class)) {
      PSRequest req = Mockito.mock(PSRequest.class);
      PSUserSession session = Mockito.mock(PSUserSession.class);
      filter.when(PSSecurityFilter::getCurrentRequest).thenReturn(req);
      when(req.getUserSession()).thenReturn(session);
      when(session.getUserCommunities(req)).thenReturn(List.of("1001"));
      assertEquals(1, adaptor.lookupBatch(base, List.of("42")).size());
    }
  }

  @Test
  void batchOmitsCommunityScopedRowWithoutRequest() throws Exception {
    // Issue #6852: a missing request must fail closed, not disclose the row.
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(summary(101L, 2, 5, 1001));
    try (MockedStatic<PSSecurityFilter> filter = Mockito.mockStatic(PSSecurityFilter.class)) {
      filter.when(PSSecurityFilter::getCurrentRequest).thenReturn(null);
      assertTrue(adaptor.lookupBatch(base, List.of("42")).isEmpty());
    }
  }

  @Test
  void batchOmitsCommunityScopedRowWithoutSession() throws Exception {
    // Issue #6852: a request with no user session must fail closed.
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(summary(101L, 2, 5, 1001));
    try (MockedStatic<PSSecurityFilter> filter = Mockito.mockStatic(PSSecurityFilter.class)) {
      PSRequest req = Mockito.mock(PSRequest.class);
      filter.when(PSSecurityFilter::getCurrentRequest).thenReturn(req);
      when(req.getUserSession()).thenReturn(null);
      assertTrue(adaptor.lookupBatch(base, List.of("42")).isEmpty());
    }
  }

  @Test
  void batchOmitsCommunityScopedRowWithEmptyCommunities() throws Exception {
    // Issue #6852: a user assigned to no communities sees no community-scoped rows.
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(summary(101L, 2, 5, 1001));
    try (MockedStatic<PSSecurityFilter> filter = Mockito.mockStatic(PSSecurityFilter.class)) {
      stubSession(filter, List.of(), List.of("Editor"));
      assertTrue(adaptor.lookupBatch(base, List.of("42")).isEmpty());
    }
  }

  @Test
  void batchOmitsCommunityScopedRowWithNullCommunities() throws Exception {
    // Issue #6852: a null community list is uncertainty and fails closed.
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(summary(101L, 2, 5, 1001));
    try (MockedStatic<PSSecurityFilter> filter = Mockito.mockStatic(PSSecurityFilter.class)) {
      stubSession(filter, null, List.of());
      assertTrue(adaptor.lookupBatch(base, List.of("42")).isEmpty());
    }
  }

  @Test
  void batchAdminSessionWithEmptyCommunitiesKeepsRow() throws Exception {
    // Issue #6852: only a verified Admin-role session is unrestricted on an
    // empty community list.
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(summary(101L, 2, 5, 1001));
    try (MockedStatic<PSSecurityFilter> filter = Mockito.mockStatic(PSSecurityFilter.class)) {
      stubSession(filter, List.of(), List.of("Admin"));
      assertEquals(1, adaptor.lookupBatch(base, List.of("42")).size());
    }
  }

  @Test
  void batchNonCommunityRowVisibleWithoutRequest() throws Exception {
    // Non-community-scoped rows (community id <= 0) stay visible even with no
    // verifiable caller.
    when(workflow.lookupCheckoutOwner("7"))
        .thenReturn(new PSItemUserInfo("Sites", "", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("7")).thenReturn(summary(0L, 1, 0, -1));
    try (MockedStatic<PSSecurityFilter> filter = Mockito.mockStatic(PSSecurityFilter.class)) {
      filter.when(PSSecurityFilter::getCurrentRequest).thenReturn(null);
      assertEquals(1, adaptor.lookupBatch(base, List.of("7")).size());
    }
  }
  @Test
  void batchUnexpectedFailurePropagatesTo500() throws Exception {
    // Review #6814: a service outage must not masquerade as "not found" (which
    // would silently fall back to permissive visibility). The resource maps the
    // propagated exception to HTTP 500.
    when(workflow.lookupCheckoutOwner("42")).thenThrow(new IllegalStateException("db down"));
    assertThrows(
        IllegalStateException.class, () -> adaptor.lookupBatch(base, List.of("42")));
  }

  @Test
  void batchEmptyIdsIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.lookupBatch(base, null));
    assertEquals(400, ex.getResponse().getStatus());
    WebApplicationException empty =
        assertThrows(
            WebApplicationException.class, () -> adaptor.lookupBatch(base, Collections.emptyList()));
    assertEquals(400, empty.getResponse().getStatus());
    assertTrue(true);
  }

  @Test
  void toCheckoutStatusMapping() {
    assertEquals("", ItemIdentityAdaptor.toCheckoutStatus(null, "admin"));
    assertEquals("", ItemIdentityAdaptor.toCheckoutStatus("  ", "admin"));
    assertEquals("Checked Out By Me", ItemIdentityAdaptor.toCheckoutStatus("Admin", "admin"));
    assertEquals("Checked Out", ItemIdentityAdaptor.toCheckoutStatus("editor", "admin"));
    assertEquals("Checked Out", ItemIdentityAdaptor.toCheckoutStatus("editor", null));
  }

  /**
   * Runs a single-id batch lookup with the security filter stubbed to a session carrying the
   * given communities (#6852 fail-closed guard needs a verifiable caller).
   */
  private List<ItemIdentity> lookupWithCommunities(String id, List<String> communities)
      throws Exception {
    try (MockedStatic<PSSecurityFilter> filter = Mockito.mockStatic(PSSecurityFilter.class)) {
      stubSession(filter, communities, null);
      return adaptor.lookupBatch(base, List.of(id));
    }
  }

  /**
   * Stubs the current request/session with the given communities and roles. Roles are only
   * stubbed when non-null: a non-empty community list never consults them, and Mockito
   * strict stubs reject stubbings the code under test does not use.
   */
  private static void stubSession(
      MockedStatic<PSSecurityFilter> filter, List<String> communities, List<String> roles)
      throws Exception {
    PSRequest req = Mockito.mock(PSRequest.class);
    PSUserSession session = Mockito.mock(PSUserSession.class);
    filter.when(PSSecurityFilter::getCurrentRequest).thenReturn(req);
    when(req.getUserSession()).thenReturn(session);
    when(session.getUserCommunities(req)).thenReturn(communities);
    if (roles != null) {
      when(session.getUserRoles()).thenReturn(roles);
    }
  }

  private static PSComponentSummary summary(
      long contentTypeId, int objectType, int workflowAppId, int communityId) {
    PSComponentSummary summary = new PSComponentSummary();
    summary.setContentTypeId(contentTypeId);
    summary.setObjectType(objectType);
    summary.setWorkflowAppId(workflowAppId);
    summary.setCommunityId(communityId);
    return summary;
  }

  /** Concrete stand-in for the abstract {@link PSValidationException} (unauthorized-id path). */
  private static final class DeniedValidationException extends PSValidationException {
    private static final long serialVersionUID = 1L;

    DeniedValidationException(String message) {
      super(message);
    }
  }
}
