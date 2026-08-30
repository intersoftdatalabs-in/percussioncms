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
import com.percussion.rest.slots.SlotDetail;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSSlotContentFinder;
import com.percussion.services.assembly.IPSTemplateSlot;

import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * AS-01 remainder: Admin PUT of finderName / relationshipName / finderArguments requires a held
 * design lock, does not steal, and round-trips on GET detail.
 */
@Tag("UnitTest")
class SlotsAdaptorFinderWriteTest {

  private static final String FINDER =
      "Java/global/percussion/slotcontentfinder/sys_RelationshipContentFinder";

  private IPSAssemblyDesignWs designWs;
  private IPSAssemblyService assemblyService;
  private IPSSystemDesignWs systemDesign;
  private SlotsAdaptor adaptor;
  private IPSGuid guid;

  @BeforeEach
  void setUp() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    designWs = mock(IPSAssemblyDesignWs.class);
    assemblyService = mock(IPSAssemblyService.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    adaptor = new SlotsAdaptor(designWs, () -> true, assemblyService, systemDesign);
    guid = new PSGuid(PSTypeEnum.SLOT, 42L);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void put_writesFinderRelationshipArgs_andRoundTripsOnGet() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    stubReloadByName("rffList", slot);
    stubHeldLock();
    when(designWs.loadSlots(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(slot));
    when(assemblyService.loadFinder(FINDER)).thenReturn(mock(IPSSlotContentFinder.class));
    IPSCatalogSummary rel = mock(IPSCatalogSummary.class);
    when(rel.getName()).thenReturn("Active Assembly");
    when(systemDesign.findRelationshipTypes(eq("Active Assembly"), isNull()))
        .thenReturn(List.of(rel));
    when(slot.getFinderName()).thenReturn(FINDER);
    when(slot.getRelationshipName()).thenReturn("Active Assembly");
    Map<String, String> args = new LinkedHashMap<>();
    args.put("template", "rffSnTitle");
    when(slot.getFinderArguments()).thenReturn(args);

    SlotDetail body = new SlotDetail();
    body.setFinderName(FINDER);
    body.setRelationshipName("Active Assembly");
    body.setFinderArguments(Map.of("template", "rffSnTitle"));

    SlotDetail updated = adaptor.updateSlot(null, "rffList", body);

    verify(slot).setFinderName(FINDER);
    verify(slot).setRelationshipName("Active Assembly");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> argsCap = ArgumentCaptor.forClass(Map.class);
    verify(slot).setFinderArguments(argsCap.capture());
    assertEquals("rffSnTitle", argsCap.getValue().get("template"));
    verify(designWs).saveSlots(anyList(), eq(false), eq("test-session"), eq("Admin"));
    assertEquals(FINDER, updated.getFinderName());
    assertEquals("Active Assembly", updated.getRelationshipName());
    assertEquals("rffSnTitle", updated.getFinderArguments().get("template"));

    SlotDetail got = adaptor.getSlot(null, "rffList");
    assertEquals(FINDER, got.getFinderName());
    assertEquals("Active Assembly", got.getRelationshipName());
    assertEquals("rffSnTitle", got.getFinderArguments().get("template"));
  }

  @Test
  void put_invalidFinder_is400() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    stubReloadByName("rffList", slot);
    stubHeldLock();
    when(assemblyService.loadFinder("nope")).thenReturn(null);

    SlotDetail body = new SlotDetail();
    body.setFinderName("nope");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.updateSlot(null, "rffList", body));
    assertTrue(ex.getMessage().contains("Invalid finder extension"), ex.getMessage());
    verify(designWs, never()).saveSlots(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_unknownRelationship_is400() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    stubReloadByName("rffList", slot);
    stubHeldLock();
    when(systemDesign.findRelationshipTypes(eq("Missing Rel"), isNull()))
        .thenReturn(Collections.emptyList());

    SlotDetail body = new SlotDetail();
    body.setRelationshipName("Missing Rel");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.updateSlot(null, "rffList", body));
    assertTrue(ex.getMessage().contains("Unknown relationship type"), ex.getMessage());
    verify(designWs, never()).saveSlots(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_unlocked_is409() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    stubReloadByName("rffList", slot);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(Collections.singletonList(null));

    SlotDetail body = new SlotDetail();
    body.setFinderName(FINDER);
    when(assemblyService.loadFinder(FINDER)).thenReturn(mock(IPSSlotContentFinder.class));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateSlot(null, "rffList", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).saveSlots(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_lockedByOther_is409() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    stubReloadByName("rffList", slot);
    PSObjectSummary other = new PSObjectSummary(guid, "rffList");
    other.setLockedInfo("other-session", "editor2", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(other));

    SlotDetail body = new SlotDetail();
    body.setFinderName(FINDER);
    when(assemblyService.loadFinder(FINDER)).thenReturn(mock(IPSSlotContentFinder.class));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateSlot(null, "rffList", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("editor2"), ex.getMessage());
    verify(designWs, never()).saveSlots(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_nonAdmin_is403() {
    adaptor = new SlotsAdaptor(designWs, () -> false, assemblyService, systemDesign);
    SlotDetail body = new SlotDetail();
    body.setFinderName(FINDER);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateSlot(null, "rffList", body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void put_missingSession_is403() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    SlotDetail body = new SlotDetail();
    body.setFinderName(FINDER);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.updateSlot(null, "rffList", body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void put_unknownSlot_returnsNull() throws Exception {
    when(designWs.findSlots(eq("missing"), isNull())).thenReturn(Collections.emptyList());
    SlotDetail body = new SlotDetail();
    body.setFinderName(FINDER);
    assertNull(adaptor.updateSlot(null, "missing", body));
    verify(systemDesign, never()).isLocked(anyList(), any());
  }

  @Test
  void lock_thenPut_thenGet_roundTrips() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    stubReloadByName("rffList", slot);
    stubHeldLock();
    when(designWs.loadSlots(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(slot));
    when(assemblyService.loadFinder(FINDER)).thenReturn(mock(IPSSlotContentFinder.class));
    when(slot.getFinderName()).thenReturn(FINDER);

    ObjectLockSummary locked = adaptor.lockSlot(null, "rffList");
    assertEquals("Admin", locked.getLocker());
    assertEquals("test-session", locked.getSession());

    SlotDetail body = new SlotDetail();
    body.setFinderName(FINDER);
    SlotDetail updated = adaptor.updateSlot(null, "rffList", body);
    assertEquals(FINDER, updated.getFinderName());
    assertEquals(FINDER, adaptor.getSlot(null, "rffList").getFinderName());
  }

  @Test
  void lock_otherUser_is409() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    stubReloadByName("rffList", slot);
    when(designWs.loadSlots(anyList(), eq(true), eq(false), any(), any()))
        .thenThrow(new PSErrorResultsException());
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.lockSlot(null, "rffList"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void unlock_otherUser_is409() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    stubReloadByName("rffList", slot);
    PSObjectSummary other = new PSObjectSummary(guid, "rffList");
    other.setLockedInfo("other-session", "editor2", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(other));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.unlockSlot(null, "rffList"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(systemDesign, never()).releaseLocks(anyList(), any(), any());
  }

  @Test
  void unlock_success() throws Exception {
    IPSTemplateSlot slot = stubSlot("rffList");
    stubReloadByName("rffList", slot);
    stubHeldLock();
    assertTrue(adaptor.unlockSlot(null, "rffList"));
    verify(systemDesign).releaseLocks(eq(List.of(guid)), eq("test-session"), eq("Admin"));
  }

  private IPSTemplateSlot stubSlot(String name) {
    IPSTemplateSlot slot = mock(IPSTemplateSlot.class);
    when(slot.getName()).thenReturn(name);
    when(slot.getLabel()).thenReturn(name);
    when(slot.getGUID()).thenReturn(guid);
    when(slot.isSystemSlot()).thenReturn(false);
    return slot;
  }

  private void stubReloadByName(String name, IPSTemplateSlot slot) throws Exception {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn(name);
    when(sum.getLabel()).thenReturn(name);
    when(designWs.findSlots(eq(name), isNull())).thenReturn(List.of(sum));
    when(designWs.loadSlots(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(slot));
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary held = new PSObjectSummary(guid, "rffList");
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }
}
