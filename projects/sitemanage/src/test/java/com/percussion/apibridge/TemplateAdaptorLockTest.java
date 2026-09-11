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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.ObjectLockSummary;
import com.percussion.rest.templates.TemplateDetail;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
import com.percussion.webservices.assembly.data.PSAssemblyTemplateWs;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** AS-08 remainder: Admin lock / PUT / unlock for assembly templates. */
@Tag("UnitTest")
class TemplateAdaptorLockTest {

  private IPSAssemblyService asm;
  private IPSContentWs contentWs;
  private IPSAssemblyDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private TemplateAdaptor adaptor;
  private IPSGuid guid;

  @BeforeEach
  void setUp() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    asm = mock(IPSAssemblyService.class);
    contentWs = mock(IPSContentWs.class);
    designWs = mock(IPSAssemblyDesignWs.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    adaptor = new TemplateAdaptor(asm, contentWs, designWs, systemDesign, () -> true);
    guid = new PSGuid(PSTypeEnum.TEMPLATE, 42L);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void lock_thenPut_thenUnlock() throws Exception {
    PSAssemblyTemplate template = stubTemplate("perc.page");
    stubNameLookup("perc.page", template);
    stubHeldLock();
    when(designWs.loadAssemblyTemplates(anyList(), eq(true), anyBoolean(), any(), any()))
        .thenReturn(List.of(new PSAssemblyTemplateWs(template, Collections.emptyMap())));

    ObjectLockSummary locked = adaptor.lockTemplate(null, "perc.page");
    assertEquals("Admin", locked.getLocker());
    assertEquals("test-session", locked.getSession());

    TemplateDetail body = new TemplateDetail();
    body.setLabel("Page locked");
    TemplateDetail updated = adaptor.updateTemplate(null, "perc.page", body);
    assertEquals("perc.page", updated.getName());
    verify(template).setLabel("Page locked");
    verify(asm).saveTemplate(template);

    assertTrue(adaptor.unlockTemplate(null, "perc.page"));
    verify(systemDesign).releaseLocks(eq(List.of(guid)), eq("test-session"), eq("Admin"));
  }

  @Test
  void put_unlocked_is409() throws Exception {
    PSAssemblyTemplate template = stubTemplate("perc.page");
    stubNameLookup("perc.page", template);
    PSObjectSummary unlocked = new PSObjectSummary(guid, "perc.page");
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(unlocked));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateTemplate(null, "perc.page", new TemplateDetail()));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveAssemblyTemplates(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void lock_sameUserStaleSession_overrides() throws Exception {
    PSAssemblyTemplate template = stubTemplate("perc.page");
    stubNameLookup("perc.page", template);
    stubHeldLock();
    when(designWs.loadAssemblyTemplates(anyList(), eq(true), eq(true), any(), any()))
        .thenReturn(List.of(new PSAssemblyTemplateWs(template, Collections.emptyMap())));

    ObjectLockSummary locked = adaptor.lockTemplate(null, "perc.page");
    assertEquals("Admin", locked.getLocker());
    verify(designWs)
        .loadAssemblyTemplates(anyList(), eq(true), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void lock_otherUser_is409() throws Exception {
    PSAssemblyTemplate template = stubTemplate("perc.page");
    stubNameLookup("perc.page", template);
    PSObjectSummary other = new PSObjectSummary(guid, "perc.page");
    other.setLockedInfo("other-session", "editor2", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(other));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.lockTemplate(null, "perc.page"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void lock_unknown_returnsNull() throws Exception {
    when(asm.findTemplateByName("missing")).thenReturn(null);
    assertNull(adaptor.lockTemplate(null, "missing"));
  }

  @Test
  void lock_forbiddenWhenNotAdmin() {
    TemplateAdaptor denied =
        new TemplateAdaptor(asm, contentWs, designWs, systemDesign, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> denied.lockTemplate(null, "perc.page"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void unlock_otherUser_is409() throws Exception {
    PSAssemblyTemplate template = stubTemplate("perc.page");
    stubNameLookup("perc.page", template);
    PSObjectSummary other = new PSObjectSummary(guid, "perc.page");
    other.setLockedInfo("other-session", "editor2", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(other));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.unlockTemplate(null, "perc.page"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(systemDesign, never()).releaseLocks(anyList(), any(), any());
  }

  @Test
  void lock_requiresSessionUser() {
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.lockTemplate(null, "perc.page"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  private PSAssemblyTemplate stubTemplate(String name) {
    PSAssemblyTemplate template = mock(PSAssemblyTemplate.class);
    when(template.getGUID()).thenReturn(guid);
    when(template.getName()).thenReturn(name);
    when(template.getLabel()).thenReturn(name);
    when(template.getBindings()).thenReturn(new java.util.ArrayList<>());
    when(template.getSlots()).thenReturn(new java.util.HashSet<>());
    return template;
  }

  private void stubNameLookup(String name, PSAssemblyTemplate template) throws Exception {
    when(asm.findTemplateByName(name)).thenReturn(template);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn(name);
    when(sum.getLabel()).thenReturn(name);
    when(designWs.findAssemblyTemplates(eq(name), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of(sum));
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary held = new PSObjectSummary(guid, "perc.page");
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }
}
