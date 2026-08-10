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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.slots.SlotDetail;
import com.percussion.rest.slots.SlotSummary;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.data.PSSlotLayoutStyles;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag("UnitTest")
class SlotsAdaptorDesignWsTest {

  @BeforeEach
  void setRequestInfo() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "test-user");
  }

  @AfterEach
  void clearRequestInfo() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void listSlots_usesFindAndLoadOnDesignWs() throws Exception {
    IPSAssemblyDesignWs designWs = mock(IPSAssemblyDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.SLOT, 7L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);

    IPSTemplateSlot slot = mock(IPSTemplateSlot.class);
    when(slot.getName()).thenReturn("rffList");
    when(slot.getLabel()).thenReturn("List");
    when(slot.getGUID()).thenReturn(guid);

    when(designWs.findSlots(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.loadSlots(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(slot));

    SlotsAdaptor adaptor = new SlotsAdaptor(designWs);
    List<SlotSummary> list = adaptor.listSlots(null);

    assertEquals(1, list.size());
    assertEquals("List", list.get(0).getLabel());
    verify(designWs).findSlots(null, null);
    verify(designWs).loadSlots(anyList(), eq(false), eq(false), any(), any());
  }

  @Test
  void listSlots_emptyFind() throws Exception {
    IPSAssemblyDesignWs designWs = mock(IPSAssemblyDesignWs.class);
    when(designWs.findSlots(isNull(), isNull())).thenReturn(List.of());
    assertTrue(new SlotsAdaptor(designWs).listSlots(null).isEmpty());
  }

  @Test
  void getSlot_exposesLayoutAndStyles() throws Exception {
    IPSAssemblyDesignWs designWs = mock(IPSAssemblyDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.SLOT, 11L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn("rffList");
    when(sum.getLabel()).thenReturn("List");

    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put(PSSlotLayoutStyles.KEY_SCHEMA_VERSION, PSSlotLayoutStyles.SCHEMA_VERSION);
    layout.put(PSSlotLayoutStyles.KEY_ORIENTATION, "horizontal");
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put(PSSlotLayoutStyles.KEY_SCHEMA_VERSION, PSSlotLayoutStyles.SCHEMA_VERSION);
    styles.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "slot-root");

    IPSTemplateSlot slot = mock(IPSTemplateSlot.class);
    when(slot.getName()).thenReturn("rffList");
    when(slot.getLabel()).thenReturn("List");
    when(slot.getGUID()).thenReturn(guid);
    when(slot.getSlotLayout()).thenReturn(layout);
    when(slot.getSlotStyles()).thenReturn(styles);
    when(slot.isSystemSlot()).thenReturn(false);

    when(designWs.findSlots(eq("rffList"), isNull())).thenReturn(List.of(sum));
    when(designWs.loadSlots(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(slot));

    SlotDetail detail = new SlotsAdaptor(designWs).getSlot(null, "rffList");
    assertNotNull(detail);
    assertEquals("horizontal", detail.getSlotLayout().get(PSSlotLayoutStyles.KEY_ORIENTATION));
    assertEquals("slot-root", detail.getSlotStyles().get(PSSlotLayoutStyles.KEY_ROOTCLASS));
  }

  @Test
  void updateSlot_writesLayoutAndStyles() throws Exception {
    IPSAssemblyDesignWs designWs = mock(IPSAssemblyDesignWs.class);
    IPSGuid guid = new PSGuid(PSTypeEnum.SLOT, 12L);
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn("rffList");
    when(sum.getLabel()).thenReturn("List");

    IPSTemplateSlot slot = mock(IPSTemplateSlot.class);
    when(slot.getName()).thenReturn("rffList");
    when(slot.getLabel()).thenReturn("List");
    when(slot.getGUID()).thenReturn(guid);
    when(slot.getSlotLayout()).thenReturn(new HashMap<>(PSSlotLayoutStyles.defaultLayout()));
    when(slot.getSlotStyles()).thenReturn(new HashMap<>(PSSlotLayoutStyles.defaultStyles()));
    when(slot.isSystemSlot()).thenReturn(false);

    when(designWs.findSlots(eq("rffList"), isNull())).thenReturn(List.of(sum));
    // forEdit=true then reload forEdit=false
    when(designWs.loadSlots(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(slot));
    when(designWs.loadSlots(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(slot));

    SlotDetail body = new SlotDetail();
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put(PSSlotLayoutStyles.KEY_ORIENTATION, "vertical");
    layout.put(PSSlotLayoutStyles.KEY_COLUMNS, "3");
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "updated-root");
    body.setSlotLayout(layout);
    body.setSlotStyles(styles);

    new SlotsAdaptor(designWs).updateSlot(null, "rffList", body);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> layoutCap = ArgumentCaptor.forClass(Map.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> stylesCap = ArgumentCaptor.forClass(Map.class);
    verify(slot).setSlotLayout(layoutCap.capture());
    verify(slot).setSlotStyles(stylesCap.capture());
    assertEquals("vertical", layoutCap.getValue().get(PSSlotLayoutStyles.KEY_ORIENTATION));
    assertEquals("3", layoutCap.getValue().get(PSSlotLayoutStyles.KEY_COLUMNS));
    assertEquals("updated-root", stylesCap.getValue().get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    verify(designWs).saveSlots(anyList(), eq(true), eq("test-session"), eq("test-user"));
  }
}
