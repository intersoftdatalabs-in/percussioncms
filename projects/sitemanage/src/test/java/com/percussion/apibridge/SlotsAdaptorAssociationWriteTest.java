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

import com.percussion.rest.Guid;
import com.percussion.rest.slots.SlotAssociationSummary;
import com.percussion.rest.slots.SlotDetail;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.types.PSPair;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * #4462: slot association GET names + PUT name/guid, Admin 403, lock 409, unknown pair 400.
 */
@Tag("UnitTest")
class SlotsAdaptorAssociationWriteTest {

  private IPSAssemblyDesignWs designWs;
  private IPSAssemblyService assemblyService;
  private IPSSystemDesignWs systemDesign;
  private IPSContentDesignWs contentDesign;
  private SlotsAdaptor adaptor;
  private IPSGuid slotGuid;
  private IPSGuid ctGuid;
  private IPSGuid tplGuid;

  @BeforeEach
  void setUp() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    designWs = mock(IPSAssemblyDesignWs.class);
    assemblyService = mock(IPSAssemblyService.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    contentDesign = mock(IPSContentDesignWs.class);
    adaptor =
        new SlotsAdaptor(designWs, () -> true, assemblyService, systemDesign, contentDesign);
    slotGuid = new PSGuid(PSTypeEnum.SLOT, 42L);
    ctGuid = new PSGuid(PSTypeEnum.NODEDEF, 301L);
    tplGuid = new PSGuid(PSTypeEnum.TEMPLATE, 1L);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void get_resolvesAssociationNames() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    when(slot.getSlotAssociations()).thenReturn(List.of(new PSPair<>(ctGuid, tplGuid)));
    stubReloadByName("rffList", slot);
    stubCatalogs();

    SlotDetail got = adaptor.getSlot(null, "rffList");
    assertEquals(1, got.getAssociations().size());
    SlotAssociationSummary a = got.getAssociations().get(0);
    assertEquals("percPage", a.getContentTypeName());
    assertEquals("Page", a.getContentTypeLabel());
    assertEquals("perc.page", a.getTemplateName());
    assertEquals("Page", a.getTemplateLabel());
    assertTrue(got.getDesignGaps() == null || got.getDesignGaps().isEmpty());
  }

  @Test
  void put_byName_roundTripsNames() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    stubReloadByName("rffList", slot);
    stubHeldLock();
    stubCatalogs();
    when(designWs.loadSlots(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(slot));
    when(slot.getSlotAssociations()).thenReturn(List.of(new PSPair<>(ctGuid, tplGuid)));

    SlotAssociationSummary row = new SlotAssociationSummary();
    row.setContentTypeName("percPage");
    row.setTemplateName("perc.page");
    SlotDetail body = new SlotDetail();
    body.setAssociations(List.of(row));

    SlotDetail updated = adaptor.updateSlot(null, "rffList", body);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSPair<IPSGuid, IPSGuid>>> cap = ArgumentCaptor.forClass(List.class);
    verify(slot).setSlotAssociations(cap.capture());
    assertEquals(ctGuid, cap.getValue().get(0).getFirst());
    assertEquals(tplGuid, cap.getValue().get(0).getSecond());
    verify(designWs).saveSlots(anyList(), eq(false), eq("test-session"), eq("Admin"));
    assertEquals("percPage", updated.getAssociations().get(0).getContentTypeName());
    assertEquals("perc.page", updated.getAssociations().get(0).getTemplateName());
  }

  @Test
  void put_byGuid_resolvesWhenCataloged() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    stubReloadByName("rffList", slot);
    stubHeldLock();
    stubCatalogs();
    when(designWs.loadSlots(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(slot));
    when(slot.getSlotAssociations()).thenReturn(List.of(new PSPair<>(ctGuid, tplGuid)));

    SlotAssociationSummary row = new SlotAssociationSummary();
    Guid ct = new Guid();
    ct.setStringValue("0-2-301");
    Guid tpl = new Guid();
    tpl.setStringValue("0-10-1");
    row.setContentTypeGuid(ct);
    row.setTemplateGuid(tpl);
    SlotDetail body = new SlotDetail();
    body.setAssociations(List.of(row));

    adaptor.updateSlot(null, "rffList", body);
    verify(slot).setSlotAssociations(anyList());
  }

  @Test
  void put_unknownName_is400() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    stubReloadByName("rffList", slot);
    stubHeldLock();
    stubCatalogs();
    when(designWs.loadSlots(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(slot));

    SlotAssociationSummary row = new SlotAssociationSummary();
    row.setContentTypeName("noSuchType");
    row.setTemplateName("perc.page");
    SlotDetail body = new SlotDetail();
    body.setAssociations(List.of(row));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.updateSlot(null, "rffList", body));
    assertTrue(ex.getMessage().contains("not found"), ex.getMessage());
    verify(designWs, never()).saveSlots(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_unlocked_is409() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    stubReloadByName("rffList", slot);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(Collections.singletonList(null));
    stubCatalogs();

    SlotAssociationSummary row = new SlotAssociationSummary();
    row.setContentTypeName("percPage");
    row.setTemplateName("perc.page");
    SlotDetail body = new SlotDetail();
    body.setAssociations(List.of(row));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateSlot(null, "rffList", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).saveSlots(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_nonAdmin_is403() {
    adaptor = new SlotsAdaptor(designWs, () -> false, assemblyService, systemDesign, contentDesign);
    SlotAssociationSummary row = new SlotAssociationSummary();
    row.setContentTypeName("percPage");
    row.setTemplateName("perc.page");
    SlotDetail body = new SlotDetail();
    body.setAssociations(List.of(row));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateSlot(null, "rffList", body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void put_unknownSlot_returnsNull() throws Exception {
    when(designWs.findSlots(eq("missing"), isNull())).thenReturn(Collections.emptyList());
    SlotAssociationSummary row = new SlotAssociationSummary();
    row.setContentTypeName("percPage");
    row.setTemplateName("perc.page");
    SlotDetail body = new SlotDetail();
    body.setAssociations(List.of(row));
    assertNull(adaptor.updateSlot(null, "missing", body));
    verify(systemDesign, never()).isLocked(anyList(), any());
  }

  private IPSTemplateSlot stubSlot(String name) {
    IPSTemplateSlot slot = mock(IPSTemplateSlot.class);
    when(slot.getName()).thenReturn(name);
    when(slot.getLabel()).thenReturn(name);
    when(slot.getGUID()).thenReturn(slotGuid);
    when(slot.isSystemSlot()).thenReturn(false);
    return slot;
  }

  private void stubReloadByName(String name, IPSTemplateSlot slot) throws Exception {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(slotGuid);
    when(sum.getName()).thenReturn(name);
    when(sum.getLabel()).thenReturn(name);
    when(designWs.findSlots(eq(name), isNull())).thenReturn(List.of(sum));
    when(designWs.loadSlots(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(slot));
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary held = new PSObjectSummary(slotGuid, "rffList");
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }

  private void stubCatalogs() {
    IPSCatalogSummary ct = mock(IPSCatalogSummary.class);
    when(ct.getGUID()).thenReturn(ctGuid);
    when(ct.getName()).thenReturn("rx:percPage");
    when(ct.getLabel()).thenReturn("Page");
    when(contentDesign.findContentTypes(isNull())).thenReturn(List.of(ct));

    IPSCatalogSummary tpl = mock(IPSCatalogSummary.class);
    when(tpl.getGUID()).thenReturn(tplGuid);
    when(tpl.getName()).thenReturn("perc.page");
    when(tpl.getLabel()).thenReturn("Page");
    when(designWs.findAssemblyTemplates(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of(tpl));
  }
}
