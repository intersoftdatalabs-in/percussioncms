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
    List<ItemIdentity> out = adaptor.lookupBatch(base, List.of("42"));
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
    assertEquals("Checked Out", adaptor.lookupBatch(base, List.of("42")).get(0).getCheckoutStatus());
  }

  @Test
  void batchNoCheckoutIsEmptyToken() throws Exception {
    when(workflow.lookupCheckoutOwner("42"))
        .thenReturn(new PSItemUserInfo("Home", "", "admin", "Reader"));
    when(workflowHelper.getComponentSummary("42")).thenReturn(summary(101L, 2, 5, 1001));
    assertEquals("", adaptor.lookupBatch(base, List.of("42")).get(0).getCheckoutStatus());
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
    List<ItemIdentity> out = adaptor.lookupBatch(base, Arrays.asList("42", " ", "missing"));
    assertEquals(1, out.size());
    assertEquals("42", out.get(0).getId());
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

  private static PSComponentSummary summary(
      long contentTypeId, int objectType, int workflowAppId, int communityId) {
    PSComponentSummary summary = new PSComponentSummary();
    summary.setContentTypeId(contentTypeId);
    summary.setObjectType(objectType);
    summary.setWorkflowAppId(workflowAppId);
    summary.setCommunityId(communityId);
    return summary;
  }
}
