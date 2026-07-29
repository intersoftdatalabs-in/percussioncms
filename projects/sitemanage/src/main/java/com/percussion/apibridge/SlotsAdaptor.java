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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.apibridge;

import com.percussion.rest.slots.ISlotsAdaptor;
import com.percussion.rest.slots.SlotSummary;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.system.utils.PSSiteManageBean;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@PSSiteManageBean
public class SlotsAdaptor implements ISlotsAdaptor {

  private static final Logger log = LogManager.getLogger(SlotsAdaptor.class);

  private final IPSAssemblyService asmSvc = PSAssemblyServiceLocator.getAssemblyService();

  @Override
  public List<SlotSummary> listSlots(URI baseUri) {
    // baseUri reserved for HATEOAS link building (interface contract)
    // null name = all slots (see findSlotsByName implementation)
    List<IPSTemplateSlot> slots = asmSvc.findSlotsByName(null);
    List<SlotSummary> out = new ArrayList<>();
    if (slots != null) {
      for (IPSTemplateSlot slot : slots) {
        if (slot == null) continue;
        SlotSummary s = new SlotSummary();
        try {
          if (slot.getGUID() != null) {
            s.setGuid(ApiUtils.convertGuid(slot.getGUID()));
          }
        } catch (Exception e) {
          // GUID is optional on the summary; keep the slot row
          log.debug(
              "Could not convert GUID for slot {}: {}", slot.getName(), e.getMessage());
        }
        s.setName(slot.getName());
        s.setLabel(StringUtils.defaultIfBlank(slot.getLabel(), slot.getName()));
        s.setDescription(slot.getDescription());
        out.add(s);
      }
    }
    out.sort(
        Comparator.comparing(
            x -> x.getLabel() != null ? x.getLabel() : "", String.CASE_INSENSITIVE_ORDER));
    return out;
  }
}
