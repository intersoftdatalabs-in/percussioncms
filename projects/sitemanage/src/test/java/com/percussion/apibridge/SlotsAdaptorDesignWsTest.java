/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.slots.SlotSummary;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class SlotsAdaptorDesignWsTest {

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
}
