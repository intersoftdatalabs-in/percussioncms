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
import com.percussion.rest.slots.SlotAssociationSummary;
import com.percussion.rest.slots.SlotDetail;
import com.percussion.rest.slots.SlotSummary;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Lists and loads assembly slots for the Developer module design catalog. */
@PSSiteManageBean
public class SlotsAdaptor implements ISlotsAdaptor {

  private static final Logger log = LogManager.getLogger(SlotsAdaptor.class);

  /** Known read-only API limitations (same for every slot; not per-slot configuration). */
  static final List<String> SLOT_DESIGN_GAPS =
      List.of(
          "Create / update / delete / lock not supported (read-only API limitation)",
          "Association editing not supported via this API",
          "Content-type and template names not resolved (GUIDs only)");

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
          log.debug("Could not convert GUID for slot {}: {}", slot.getName(), e.getMessage());
        }
        s.setName(slot.getName());
        s.setLabel(StringUtils.defaultIfBlank(slot.getLabel(), slot.getName()));
        s.setDescription(slot.getDescription());
        out.add(s);
      }
    }
    out.sort(
        Comparator.comparing(
            SlotSummary::getLabel, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    return out;
  }

  @Override
  public SlotDetail getSlot(URI baseUri, String idOrName) {
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    try {
      IPSTemplateSlot slot = resolveSlot(idOrName.trim());
      if (slot == null) {
        return null;
      }
      return toDetail(slot);
    } catch (PSAssemblyException e) {
      log.debug("Slot not found {}: {}", idOrName, e.getMessage());
      return null;
    } catch (Exception e) {
      log.error(
          "Failed to load slot {} ({}): {}",
          idOrName,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new RuntimeException(
          "Failed to load slot (" + e.getClass().getName() + "): " + e.getMessage(), e);
    }
  }

  private IPSTemplateSlot resolveSlot(String idOrName) throws PSAssemblyException {
    if (StringUtils.isNumeric(idOrName)) {
      long uuid = Long.parseLong(idOrName);
      IPSGuid g = new PSGuid(PSTypeEnum.SLOT, uuid);
      return asmSvc.loadSlot(g);
    }
    // GUID-shaped only (digits and dashes), not arbitrary names containing "-"
    if (idOrName.matches("\\d+-\\d+(-\\d+)?")) {
      try {
        PSGuid g = new PSGuid(idOrName);
        if (g.getType() == 0) {
          g = new PSGuid(PSTypeEnum.SLOT, g.getUUID());
        }
        return asmSvc.loadSlot(g);
      } catch (PSAssemblyException e) {
        log.debug("Slot GUID load not found for {}: {}", idOrName, e.getMessage());
        return null;
      } catch (Exception e) {
        log.warn(
            "Failed to parse/load slot GUID {}, falling back to name: {}",
            idOrName,
            e.getMessage(),
            e);
      }
    }
    try {
      return asmSvc.findSlotByName(idOrName);
    } catch (PSAssemblyException e) {
      return null;
    }
  }

  private SlotDetail toDetail(IPSTemplateSlot slot) {
    SlotDetail d = new SlotDetail();
    try {
      if (slot.getGUID() != null) {
        d.setGuid(ApiUtils.convertGuid(slot.getGUID()));
      }
    } catch (Exception e) {
      log.debug("Could not convert slot GUID for {}: {}", slot.getName(), e.getMessage());
    }
    d.setName(slot.getName());
    d.setLabel(StringUtils.defaultIfBlank(slot.getLabel(), slot.getName()));
    d.setDescription(slot.getDescription());
    if (slot.getSlottypeEnum() != null) {
      d.setSlotType(slot.getSlottypeEnum().name());
    }
    d.setSystemSlot(slot.isSystemSlot());
    d.setFinderName(slot.getFinderName());
    d.setRelationshipName(slot.getRelationshipName());
    Map<String, String> args = slot.getFinderArguments();
    d.setFinderArguments(args != null ? new HashMap<>(args) : new HashMap<>());

    List<SlotAssociationSummary> associations = new ArrayList<>();
    if (slot.getSlotAssociations() != null) {
      for (PSPair<IPSGuid, IPSGuid> pair : slot.getSlotAssociations()) {
        if (pair == null) continue;
        SlotAssociationSummary a = new SlotAssociationSummary();
        try {
          if (pair.getFirst() != null) {
            a.setContentTypeGuid(ApiUtils.convertGuid(pair.getFirst()));
          }
          if (pair.getSecond() != null) {
            a.setTemplateGuid(ApiUtils.convertGuid(pair.getSecond()));
          }
        } catch (Exception e) {
          log.debug(
              "Could not convert association GUID for slot {}: {}",
              slot.getName(),
              e.getMessage());
        }
        associations.add(a);
      }
    }
    d.setAssociations(associations);
    d.setDesignGaps(new ArrayList<>(SLOT_DESIGN_GAPS));
    return d;
  }
}
