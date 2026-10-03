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
package com.percussion.itemmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.itemmanagement.data.PSItemCommunityChoices;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.security.IPSSecurityWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Explorer assign-community writes the summary only after the assignment rules pass (#5077). */
@ExtendWith(MockitoExtension.class)
class PSItemCommunityServiceTest {

  @Mock private IPSWorkflowHelper workflowHelper;
  @Mock private IPSSecurityWs securityWs;
  @Mock private IPSCmsObjectMgr objectMgr;

  private PSItemCommunityService service;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.resetRequestInfo();
    Map<String, Object> info = new HashMap<>();
    info.put(PSRequestInfoBase.KEY_USER, "Admin");
    PSRequestInfoBase.initRequestInfo(info);
    service = new PSItemCommunityService(workflowHelper, securityWs, objectMgr);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void savesADifferentListedCommunityAndEvictsTheSummary() throws Exception {
    stubCatalog();
    PSComponentSummary sum = item(null);
    PSItemCommunityChoices out = service.change("42", "20");
    assertEquals("20", out.getCurrentCommunityId());
    assertEquals(20, sum.getCommunityId());
    verify(objectMgr)
        .saveComponentSummaries(argThat(rows -> rows.size() == 1 && rows.get(0) == sum));
    verify(objectMgr).evictComponentSummaries(List.of(42));
  }

  @Test
  void allowsTheChangeWhenTheItemIsCheckedOutToTheCurrentUser() throws Exception {
    stubCatalog();
    PSComponentSummary sum = item("Admin");
    PSItemCommunityChoices out = service.change("42", "20");
    assertEquals(20, sum.getCommunityId());
    assertEquals("20", out.getCurrentCommunityId());
    verify(objectMgr).saveComponentSummaries(anyList());
  }

  @Test
  void rejectsCheckoutHeldBySomeoneElse() throws Exception {
    stubCatalog();
    PSComponentSummary sum = item("pat");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.change("42", "20"));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(10, sum.getCommunityId());
    verify(objectMgr, never()).saveComponentSummaries(anyList());
  }

  @Test
  void rejectsUnchangedForbiddenBlankAndMalformedWithoutSaving() throws Exception {
    stubCatalog();
    item(null);
    assertEquals(403, status(() -> service.change("42", "10")));
    assertEquals(403, status(() -> service.change("42", "99")));
    assertEquals(400, status(() -> service.change("42", "  ")));
    assertEquals(400, status(() -> service.change("42", "nope")));
    verify(objectMgr, never()).saveComponentSummaries(anyList());
  }

  @Test
  void rejectsAFolder() throws Exception {
    PSComponentSummary folder = new PSComponentSummary();
    folder.setContentId(9);
    folder.setObjectType(PSComponentSummary.TYPE_FOLDER);
    when(workflowHelper.getComponentSummary("9")).thenReturn(folder);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.change("9", "20"));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("Folders are not assigned a community"));
    verify(objectMgr, never()).saveComponentSummaries(anyList());
  }

  @Test
  void listsTheCurrentCommunityWhenTheColumnIsMissing() throws Exception {
    stubCatalog();
    PSComponentSummary sum = new PSComponentSummary();
    sum.setContentId(42);
    sum.setObjectType(PSComponentSummary.TYPE_ITEM);
    when(workflowHelper.getComponentSummary("42")).thenReturn(sum);
    PSItemCommunityChoices catalog = service.allowed("42");
    assertEquals("", catalog.getCurrentCommunityId());
    assertEquals(2, catalog.getChoices().size());
  }

  private PSComponentSummary item(String checkoutUser) throws Exception {
    PSComponentSummary sum = new PSComponentSummary();
    sum.setContentId(42);
    sum.setObjectType(PSComponentSummary.TYPE_ITEM);
    sum.setCommunityId(10);
    if (checkoutUser != null) {
      sum.setCheckoutUserName(checkoutUser);
    }
    when(workflowHelper.getComponentSummary("42")).thenReturn(sum);
    return sum;
  }

  private void stubCatalog() {
    PSCommunity def = mock(PSCommunity.class);
    when(def.getId()).thenReturn(10L);
    when(def.getName()).thenReturn("Default");
    PSCommunity enterprise = mock(PSCommunity.class);
    when(enterprise.getId()).thenReturn(20L);
    when(enterprise.getName()).thenReturn("Enterprise");
    when(securityWs.loadCommunities("*")).thenReturn(List.of(def, enterprise));
  }

  private static int status(ChangeCall call) {
    WebApplicationException ex = assertThrows(WebApplicationException.class, call::run);
    return ex.getResponse().getStatus();
  }

  @FunctionalInterface
  private interface ChangeCall {
    void run() throws Exception;
  }
}
