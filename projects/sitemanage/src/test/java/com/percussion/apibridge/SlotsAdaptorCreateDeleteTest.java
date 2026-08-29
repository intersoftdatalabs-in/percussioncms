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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.percussion.rest.slots.SlotDetail;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.IPSTemplateSlot.SlotType;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
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
 * AS-01 POST create / DELETE persist via {@code createSlots}/{@code saveSlots}/{@code deleteSlots}.
 * Admin only; unique name; no whitespace; system-slot delete rejected.
 */
@Tag("UnitTest")
class SlotsAdaptorCreateDeleteTest {

  private IPSAssemblyDesignWs designWs;
  private SlotsAdaptor adaptor;
  private IPSGuid guid;

  @BeforeEach
  void setUp() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    designWs = mock(IPSAssemblyDesignWs.class);
    adaptor = new SlotsAdaptor(designWs, () -> true);
    guid = new PSGuid(PSTypeEnum.SLOT, 42L);
    when(designWs.findSlots(any(), isNull())).thenReturn(Collections.emptyList());
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void create_usesCreateThenSaveAndReleasesLock() throws Exception {
    IPSTemplateSlot slot = stubSlot("mySlot", false);
    when(slot.getLabel()).thenReturn("My Slot");
    when(designWs.createSlots(eq(List.of("mySlot")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(slot));
    stubCreateThenReload("mySlot", slot);

    SlotDetail body = new SlotDetail();
    body.setName("mySlot");
    body.setLabel("My Slot");
    body.setDescription("created via REST");

    SlotDetail out = adaptor.createSlot(null, body);

    assertEquals("mySlot", out.getName());
    assertEquals("My Slot", out.getLabel());
    assertEquals("created via REST", out.getDescription());
    verify(designWs).createSlots(eq(List.of("mySlot")), eq("test-session"), eq("Admin"));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSTemplateSlot>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveSlots(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    verify(slot).setLabel("My Slot");
    verify(slot).setDescription("created via REST");
  }

  @Test
  void create_setsInlineSlotType() throws Exception {
    IPSTemplateSlot slot = stubSlot("inlineSlot", false);
    when(designWs.createSlots(eq(List.of("inlineSlot")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(slot));
    stubCreateThenReload("inlineSlot", slot);

    SlotDetail body = new SlotDetail();
    body.setName("inlineSlot");
    body.setSlotType("inline");

    adaptor.createSlot(null, body);

    verify(slot).setSlottype(SlotType.INLINE);
  }

  @Test
  void create_invalidSlotType_is400() throws Exception {
    SlotDetail body = new SlotDetail();
    body.setName("mySlot");
    body.setSlotType("WEIRD");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createSlot(null, body));
    assertTrue(ex.getMessage().contains("slotType"));
    verify(designWs, never()).createSlots(anyList(), any(), any());
  }

  @Test
  void create_duplicateName_is409BeforeCreate() throws Exception {
    IPSCatalogSummary existing = mock(IPSCatalogSummary.class);
    when(existing.getName()).thenReturn("rffList");
    when(designWs.findSlots(eq("rfflist"), isNull())).thenReturn(List.of(existing));

    SlotDetail body = new SlotDetail();
    body.setName("rfflist");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createSlot(null, body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("already exists"));
    verify(designWs, never()).createSlots(anyList(), any(), any());
    verify(designWs, never()).saveSlots(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_persistTimeDuplicate_is409() throws Exception {
    when(designWs.createSlots(eq(List.of("mySlot")), eq("test-session"), eq("Admin")))
        .thenThrow(
            new IllegalArgumentException("The name 'mySlot' for type 'SLOT' already exists."));
    SlotDetail body = new SlotDetail();
    body.setName("mySlot");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createSlot(null, body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveSlots(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_blankName_throwsBeforeDesignWs() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.createSlot(null, null));
    SlotDetail blank = new SlotDetail();
    blank.setName("  ");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createSlot(null, blank));
    assertTrue(ex.getMessage().contains("name is required"));
    verify(designWs, never()).createSlots(anyList(), any(), any());
  }

  @Test
  void create_nameWithSpaces_throwsBeforeDesignWs() {
    SlotDetail body = new SlotDetail();
    body.setName("has space");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createSlot(null, body));
    assertEquals("name cannot contain whitespace", ex.getMessage());
    verify(designWs, never()).createSlots(anyList(), any(), any());
  }

  @Test
  void create_nameWithTab_throwsBeforeDesignWs() {
    SlotDetail body = new SlotDetail();
    body.setName("has\ttab");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createSlot(null, body));
    assertEquals("name cannot contain whitespace", ex.getMessage());
    verify(designWs, never()).createSlots(anyList(), any(), any());
  }

  @Test
  void create_nonAdmin_is403() {
    adaptor = new SlotsAdaptor(designWs, () -> false);
    SlotDetail body = new SlotDetail();
    body.setName("mySlot");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createSlot(null, body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).createSlots(anyList(), any(), any());
  }

  @Test
  void create_missingSession_is403() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    SlotDetail body = new SlotDetail();
    body.setName("mySlot");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createSlot(null, body));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("session"), ex.getMessage());
  }

  @Test
  void create_thenGetByName_returnsDetail() throws Exception {
    IPSTemplateSlot slot = stubSlot("mySlot", false);
    when(slot.getLabel()).thenReturn("My Slot");
    when(designWs.createSlots(eq(List.of("mySlot")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(slot));
    stubCreateThenReload("mySlot", slot);

    SlotDetail body = new SlotDetail();
    body.setName("mySlot");
    body.setLabel("My Slot");

    adaptor.createSlot(null, body);
    SlotDetail got = adaptor.getSlot(null, "mySlot");

    assertEquals("mySlot", got.getName());
    assertEquals("My Slot", got.getLabel());
  }

  @Test
  void delete_thenGetByName_isNotFound() throws Exception {
    IPSTemplateSlot slot = stubSlot("mySlot", false);
    stubReloadByName("mySlot", slot);
    when(designWs.loadSlots(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(slot));

    assertTrue(adaptor.deleteSlot(null, "mySlot"));
    when(designWs.findSlots(eq("mySlot"), isNull())).thenReturn(Collections.emptyList());
    org.junit.jupiter.api.Assertions.assertNull(adaptor.getSlot(null, "mySlot"));
  }

  @Test
  void delete_callsDesignWsWithoutIgnoringDependents() throws Exception {
    IPSTemplateSlot slot = stubSlot("mySlot", false);
    stubReloadByName("mySlot", slot);
    when(designWs.loadSlots(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(slot));

    assertTrue(adaptor.deleteSlot(null, "mySlot"));

    verify(designWs)
        .deleteSlots(eq(List.of(guid)), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void delete_unknown_returnsFalse() throws Exception {
    when(designWs.findSlots(eq("missing"), isNull())).thenReturn(Collections.emptyList());
    assertFalse(adaptor.deleteSlot(null, "missing"));
    verify(designWs, never()).deleteSlots(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_systemSlot_is409() throws Exception {
    IPSTemplateSlot slot = stubSlot("sys_inline_link", true);
    stubReloadByName("sys_inline_link", slot);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.deleteSlot(null, "sys_inline_link"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("system"), ex.getMessage());
    verify(designWs, never()).deleteSlots(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_lockConflict_is409() throws Exception {
    IPSTemplateSlot slot = stubSlot("mySlot", false);
    stubReloadByName("mySlot", slot);
    when(designWs.loadSlots(anyList(), eq(true), eq(false), any(), any()))
        .thenThrow(new PSErrorResultsException());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteSlot(null, "mySlot"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).deleteSlots(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_nonAdmin_is403() throws Exception {
    adaptor = new SlotsAdaptor(designWs, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteSlot(null, "mySlot"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).deleteSlots(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_blankId_throwsBeforeDesignWs() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.deleteSlot(null, "  "));
    assertThrows(IllegalArgumentException.class, () -> adaptor.deleteSlot(null, null));
    verify(designWs, never()).deleteSlots(anyList(), anyBoolean(), any(), any());
  }

  private IPSTemplateSlot stubSlot(String name, boolean system) {
    IPSTemplateSlot slot = mock(IPSTemplateSlot.class);
    when(slot.getName()).thenReturn(name);
    when(slot.getLabel()).thenReturn(name);
    when(slot.getDescription()).thenReturn("created via REST");
    when(slot.getGUID()).thenReturn(guid);
    when(slot.isSystemSlot()).thenReturn(system);
    when(slot.getSlottypeEnum()).thenReturn(SlotType.REGULAR);
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

  /** First {@code findSlots} is the uniqueness check (empty); later calls reload the saved slot. */
  private void stubCreateThenReload(String name, IPSTemplateSlot slot) throws Exception {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn(name);
    when(sum.getLabel()).thenReturn(name);
    when(designWs.findSlots(eq(name), isNull()))
        .thenReturn(Collections.emptyList())
        .thenReturn(List.of(sum));
    when(designWs.loadSlots(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(slot));
  }
}
