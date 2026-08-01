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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.apibridge;

import com.percussion.rest.slots.ISlotsAdaptor;
import com.percussion.rest.slots.SlotAssociationSummary;
import com.percussion.rest.slots.SlotDetail;
import com.percussion.rest.slots.SlotSummary;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.types.PSPair;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
import com.percussion.webservices.assembly.PSAssemblyWsLocator;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Assembly slots catalog for Developer REST.
 *
 * <p>Workbench parity: routes through {@link IPSAssemblyDesignWs} (same design web service assembly
 * SOAP design uses) for find / load / save — not a parallel path via raw {@code IPSAssemblyService}
 * alone.
 */
@PSSiteManageBean
public class SlotsAdaptor implements ISlotsAdaptor {

  private static final Logger log = LogManager.getLogger(SlotsAdaptor.class);

  public static final List<String> SLOT_DESIGN_GAPS =
      List.of(
          "Create / delete not supported via this REST API (use design WS createSlots/deleteSlots)",
          "Content-type and template names not resolved (GUIDs only)");

  private final IPSAssemblyDesignWs designWs;

  public SlotsAdaptor() {
    this(PSAssemblyWsLocator.getAssemblyDesignWebservice());
  }

  /** Package-visible for unit tests. */
  SlotsAdaptor(IPSAssemblyDesignWs designWs) {
    this.designWs = designWs;
  }

  @Override
  public List<SlotSummary> listSlots(URI baseUri) {
    try {
      List<IPSCatalogSummary> summaries = designWs.findSlots(null, null);
      if (summaries == null || summaries.isEmpty()) {
        return List.of();
      }
      List<IPSGuid> guids = new ArrayList<>();
      for (IPSCatalogSummary sum : summaries) {
        if (sum != null && sum.getGUID() != null) {
          guids.add(sum.getGUID());
        }
      }
      if (guids.isEmpty()) {
        return List.of();
      }
      List<IPSTemplateSlot> slots =
          designWs.loadSlots(guids, false, false, currentSession(), currentUser());
      List<SlotSummary> out = new ArrayList<>();
      if (slots != null) {
        for (IPSTemplateSlot slot : slots) {
          if (slot == null) continue;
          out.add(toSummary(slot));
        }
      }
      out.sort(
          Comparator.comparing(
              SlotSummary::getLabel, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
      return out;
    } catch (PSErrorResultsException e) {
      log.error("Failed to list slots via assembly design WS", e);
      throw new IllegalStateException("Failed to list slots", e);
    }
  }

  @Override
  public SlotDetail getSlot(URI baseUri, String idOrName) {
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    try {
      IPSTemplateSlot slot = resolveSlot(idOrName.trim(), false);
      return slot == null ? null : toDetail(slot);
    } catch (PSErrorResultsException e) {
      log.debug("Slot not found {}: {}", idOrName, e.getMessage());
      return null;
    } catch (Exception e) {
      log.error("Failed to load slot {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to load slot", e);
    }
  }

  @Override
  public SlotDetail updateSlot(URI baseUri, String idOrName, SlotDetail body) {
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    requireSessionUserForWrite();
    String session = currentSession();
    String user = currentUser();
    try {
      IPSTemplateSlot slot = resolveSlot(idOrName.trim(), true);
      if (slot == null) {
        return null;
      }
      if (body.getLabel() != null) {
        slot.setLabel(body.getLabel());
      }
      if (body.getDescription() != null) {
        slot.setDescription(body.getDescription());
      }
      if (body.getAssociations() != null) {
        slot.setSlotAssociations(toAssociationPairs(body.getAssociations()));
      }
      designWs.saveSlots(Collections.singletonList(slot), true, session, user);
      IPSTemplateSlot reloaded = resolveSlot(idOrName.trim(), false);
      return reloaded != null ? toDetail(reloaded) : toDetail(slot);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      log.error("Failed to load slot for update {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to update slot", e);
    } catch (PSErrorsException e) {
      log.error("Failed to save slot {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to save slot", e);
    } catch (Exception e) {
      log.error("Failed to update slot {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to update slot", e);
    }
  }

  private List<PSPair<IPSGuid, IPSGuid>> toAssociationPairs(
      List<SlotAssociationSummary> associations) {
    List<PSPair<IPSGuid, IPSGuid>> pairs = new ArrayList<>();
    if (associations == null) {
      return pairs;
    }
    int i = 0;
    for (SlotAssociationSummary a : associations) {
      i++;
      if (a == null) {
        throw new IllegalArgumentException("association[" + (i - 1) + "] is null");
      }
      IPSGuid ct = toIpsGuid(a.getContentTypeGuid(), "contentTypeGuid", i - 1);
      IPSGuid tpl = toIpsGuid(a.getTemplateGuid(), "templateGuid", i - 1);
      pairs.add(new PSPair<>(ct, tpl));
    }
    return pairs;
  }

  private IPSGuid toIpsGuid(com.percussion.rest.Guid g, String field, int index) {
    if (g == null) {
      throw new IllegalArgumentException("association[" + index + "]." + field + " is required");
    }
    String sv = g.getStringValue().orElse(null);
    if (StringUtils.isNotBlank(sv)) {
      try {
        return ApiUtils.convertGuid(g);
      } catch (Exception e) {
        throw new IllegalArgumentException(
            "association[" + index + "]." + field + " is not a valid GUID: " + sv, e);
      }
    }
    if (g.getUuid() > 0 && g.getType() > 0) {
      return new PSGuid(PSTypeEnum.valueOf(g.getType()), g.getUuid());
    }
    if (g.getLongValue() != 0) {
      return new PSGuid(g.getLongValue());
    }
    throw new IllegalArgumentException(
        "association[" + index + "]." + field + " requires stringValue");
  }

  /**
   * @param forEdit when true, load with design lock
   */
  private IPSTemplateSlot resolveSlot(String idOrName, boolean forEdit)
      throws PSErrorResultsException {
    String session = currentSession();
    String user = currentUser();
    if (StringUtils.isNumeric(idOrName) || idOrName.matches("\\d+-\\d+(-\\d+)?")) {
      IPSGuid g = parseSlotGuid(idOrName);
      if (g != null) {
        List<IPSTemplateSlot> loaded =
            designWs.loadSlots(Collections.singletonList(g), forEdit, false, session, user);
        if (loaded != null && !loaded.isEmpty()) {
          return loaded.get(0);
        }
        return null;
      }
    }
    // Name lookup via findSlots then load
    List<IPSCatalogSummary> found = designWs.findSlots(idOrName, null);
    if (found == null || found.isEmpty()) {
      return null;
    }
    for (IPSCatalogSummary sum : found) {
      if (sum == null || sum.getGUID() == null) {
        continue;
      }
      if (idOrName.equalsIgnoreCase(sum.getName()) || idOrName.equalsIgnoreCase(sum.getLabel())) {
        List<IPSTemplateSlot> loaded =
            designWs.loadSlots(
                Collections.singletonList(sum.getGUID()), forEdit, false, session, user);
        if (loaded != null && !loaded.isEmpty()) {
          return loaded.get(0);
        }
      }
    }
    // Fallback: first match if single result
    if (found.size() == 1 && found.get(0) != null && found.get(0).getGUID() != null) {
      List<IPSTemplateSlot> loaded =
          designWs.loadSlots(
              Collections.singletonList(found.get(0).getGUID()), forEdit, false, session, user);
      if (loaded != null && !loaded.isEmpty()) {
        return loaded.get(0);
      }
    }
    return null;
  }

  private static IPSGuid parseSlotGuid(String idOrName) {
    try {
      if (StringUtils.isNumeric(idOrName)) {
        return new PSGuid(PSTypeEnum.SLOT, Long.parseLong(idOrName));
      }
      if (idOrName.matches("\\d+-\\d+(-\\d+)?")) {
        PSGuid g = new PSGuid(idOrName);
        if (g.getType() == 0) {
          g = new PSGuid(PSTypeEnum.SLOT, g.getUUID());
        }
        return g;
      }
    } catch (Exception e) {
      log.debug("Not a slot GUID: {}", idOrName);
    }
    return null;
  }

  private SlotSummary toSummary(IPSTemplateSlot slot) {
    SlotSummary s = new SlotSummary();
    try {
      if (slot.getGUID() != null) {
        s.setGuid(ApiUtils.convertGuid(slot.getGUID()));
      }
    } catch (Exception e) {
      log.debug("Could not convert GUID for slot {}: {}", slot.getName(), e.getMessage());
    }
    s.setName(slot.getName());
    s.setLabel(labelOrName(slot));
    s.setDescription(slot.getDescription());
    return s;
  }

  private SlotDetail toDetail(IPSTemplateSlot slot) {
    SlotDetail d = new SlotDetail();
    try {
      if (slot.getGUID() != null) {
        d.setGuid(ApiUtils.convertGuid(slot.getGUID()));
      }
    } catch (Exception e) {
      log.warn("Could not convert slot GUID for {}: {}", slot.getName(), e.getMessage(), e);
    }
    d.setName(slot.getName());
    d.setLabel(labelOrName(slot));
    d.setDescription(slot.getDescription());
    if (slot.getSlottypeEnum() != null) {
      d.setSlotType(slot.getSlottypeEnum().name());
    }
    d.setSystemSlot(slot.isSystemSlot());
    d.setFinderName(slot.getFinderName());
    d.setRelationshipName(slot.getRelationshipName());
    Map<String, String> args = slot.getFinderArguments();
    if (args != null && !args.isEmpty()) {
      d.setFinderArguments(new HashMap<>(args));
    } else {
      d.setFinderArguments(null);
    }

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
          log.warn(
              "Could not convert association GUID for slot {}: {}",
              slot.getName(),
              e.getMessage(),
              e);
        }
        associations.add(a);
      }
    }
    d.setAssociations(associations.isEmpty() ? null : associations);
    d.setDesignGaps(new ArrayList<>(SLOT_DESIGN_GAPS));
    return d;
  }

  static String labelOrName(IPSTemplateSlot slot) {
    String label = slot.getLabel();
    if (StringUtils.isNotBlank(label)) {
      return label;
    }
    return StringUtils.defaultString(slot.getName());
  }

  private static String currentSession() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
  }

  private static String currentUser() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
  }

  private static void requireSessionUserForWrite() {
    if (StringUtils.isBlank(currentSession()) || StringUtils.isBlank(currentUser())) {
      throw new IllegalStateException(
          "session and user are required for slot design update (IPSAssemblyDesignWs)");
    }
  }
}
