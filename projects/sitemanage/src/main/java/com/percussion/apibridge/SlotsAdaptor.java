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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.apibridge;

import com.percussion.rest.DesignGap;
import com.percussion.rest.ObjectLockSummary;
import com.percussion.rest.slots.ISlotsAdaptor;
import com.percussion.rest.slots.SlotAssociationSummary;
import com.percussion.rest.slots.SlotDetail;
import com.percussion.rest.slots.SlotSummary;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSSlotContentFinder;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.IPSTemplateSlot.SlotType;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.locking.data.PSObjectLock;
import com.percussion.services.locking.data.PSObjectLockSummary;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.types.PSPair;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.assembly.IPSAssemblyDesignWs;
import com.percussion.webservices.assembly.PSAssemblyWsLocator;
import com.percussion.webservices.system.IPSSystemDesignWs;
import com.percussion.webservices.system.PSSystemWsLocator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Assembly slots catalog for Developer REST.
 *
 * <p>Workbench parity: routes through {@link IPSAssemblyDesignWs} (same design web service assembly
 * SOAP design uses) for find / load / save / create / delete — not a parallel path via raw {@code
 * IPSAssemblyService} alone.
 */
@PSSiteManageBean
public class SlotsAdaptor implements ISlotsAdaptor {

  private static final Logger log = LogManager.getLogger(SlotsAdaptor.class);

  static final String ADMIN_REQUIRED =
      "Admin role required to create, delete, lock, or write slot finder/relationship";

  /** Typical design-session lock duration in minutes ({@link PSObjectLock#LOCK_INTERVAL}). */
  static final long DESIGN_LOCK_MINUTES = PSObjectLock.LOCK_INTERVAL / 60_000L;

  public static final List<DesignGap> SLOT_DESIGN_GAPS =
      List.of(
          DesignGap.of(
              "SLOT_ASSOC_GUIDS_ONLY",
              "Content-type and template names not resolved (GUIDs only)"));

  private final IPSAssemblyDesignWs designWs;
  private final IPSAssemblyService assemblyService;
  private final IPSSystemDesignWs systemDesign;
  private final BooleanSupplier adminChecker;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public SlotsAdaptor() {
    this(
        PSAssemblyWsLocator.getAssemblyDesignWebservice(),
        null,
        PSAssemblyServiceLocator.getAssemblyService(),
        PSSystemWsLocator.getSystemDesignWebservice());
  }

  /** Package-visible for unit tests. Admin is allowed so existing PUT tests stay focused. */
  SlotsAdaptor(IPSAssemblyDesignWs designWs) {
    this(designWs, () -> true, null, null);
  }

  /** Package-visible for unit tests that inject a fake Admin gate. */
  SlotsAdaptor(IPSAssemblyDesignWs designWs, BooleanSupplier adminChecker) {
    this(designWs, adminChecker, null, null);
  }

  /**
   * Package-visible for unit tests that exercise finder/relationship write and design-session
   * lock.
   */
  SlotsAdaptor(
      IPSAssemblyDesignWs designWs,
      BooleanSupplier adminChecker,
      IPSAssemblyService assemblyService,
      IPSSystemDesignWs systemDesign) {
    this.designWs = designWs;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
    this.assemblyService = assemblyService;
    this.systemDesign = systemDesign;
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
    boolean finderWrite = wantsFinderWrite(body);
    if (finderWrite) {
      requireAdmin();
      requireSessionUserForDesignWrite();
    } else {
      requireSessionUserForWrite();
    }
    String session = currentSession();
    String user = currentUser();
    String trimmed = idOrName.trim();
    try {
      if (finderWrite) {
        IPSTemplateSlot current = resolveSlot(trimmed, false);
        if (current == null || current.getGUID() == null) {
          return null;
        }
        requireHeldLock(current.getGUID(), "Could not save slot finder/relationship");
        applyFinderRelationshipUpdates(body);
      }
      IPSTemplateSlot slot = resolveSlot(trimmed, true);
      if (slot == null) {
        if (finderWrite) {
          throw new WebApplicationException(
              "Could not save slot finder/relationship; design lock required or held by another"
                  + " user",
              409);
        }
        return null;
      }
      applyMutableSlotUpdates(slot, body);
      // Finder write keeps the held lock (clients unlock via POST .../unlock). Label-only
      // updates continue to acquire+release in one request.
      designWs.saveSlots(Collections.singletonList(slot), !finderWrite, session, user);
      IPSTemplateSlot reloaded = resolveSlot(trimmed, false);
      return reloaded != null ? toDetail(reloaded) : toDetail(slot);
    } catch (IllegalArgumentException | IllegalStateException | WebApplicationException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      if (finderWrite) {
        throw new WebApplicationException(
            "Could not save slot finder/relationship; design lock required or held by another"
                + " user",
            409);
      }
      log.error("Failed to load slot for update {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to update slot", e);
    } catch (PSErrorsException e) {
      if (finderWrite && isLockFailure(e)) {
        throw new WebApplicationException(
            "Could not save slot finder/relationship; design lock required or held by another"
                + " user",
            409);
      }
      log.error("Failed to save slot {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to save slot", e);
    } catch (Exception e) {
      log.error("Failed to update slot {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to update slot", e);
    }
  }

  @Override
  public ObjectLockSummary lockSlot(URI baseUri, String idOrName) {
    requireAdmin();
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    requireSessionUserForDesignWrite();
    String trimmed = idOrName.trim();
    if (trimmed.contains("*")) {
      throw new IllegalArgumentException("idOrName must not contain wildcards");
    }
    try {
      IPSTemplateSlot current = resolveSlot(trimmed, false);
      if (current == null || current.getGUID() == null) {
        return null;
      }
      IPSTemplateSlot locked = resolveSlot(trimmed, true);
      if (locked == null || locked.getGUID() == null) {
        throw new WebApplicationException(
            "Could not acquire design lock for slot; locked by another user", 409);
      }
      return toLockSummary(currentSession(), currentUser(), remainingLockMinutes(locked.getGUID()));
    } catch (IllegalArgumentException | IllegalStateException | WebApplicationException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      throw new WebApplicationException(
          "Could not acquire design lock for slot; locked by another user", 409);
    } catch (Exception e) {
      log.error("Failed to lock slot {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to lock slot", e);
    }
  }

  @Override
  public Boolean unlockSlot(URI baseUri, String idOrName) {
    requireAdmin();
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    requireSessionUserForDesignWrite();
    String trimmed = idOrName.trim();
    if (trimmed.contains("*")) {
      throw new IllegalArgumentException("idOrName must not contain wildcards");
    }
    if (systemDesign == null) {
      throw new IllegalStateException(
          "Could not release slot design session; design service unavailable");
    }
    try {
      IPSTemplateSlot current = resolveSlot(trimmed, false);
      if (current == null || current.getGUID() == null) {
        return null;
      }
      requireNotLockedByOther(current.getGUID(), "Could not release design lock for slot");
      systemDesign.releaseLocks(
          Collections.singletonList(current.getGUID()), currentSession(), currentUser());
      return Boolean.TRUE;
    } catch (IllegalArgumentException | IllegalStateException | WebApplicationException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      log.debug("Slot not found for unlock {}: {}", idOrName, e.getMessage());
      return null;
    } catch (Exception e) {
      log.error("Failed to unlock slot {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to unlock slot", e);
    }
  }

  @Override
  public SlotDetail createSlot(URI baseUri, SlotDetail body) {
    requireAdmin();
    if (body == null || StringUtils.isBlank(body.getName())) {
      throw new IllegalArgumentException("name is required");
    }
    String name = body.getName().trim();
    if (containsWhitespace(name)) {
      throw new IllegalArgumentException("name cannot contain whitespace");
    }
    if (name.contains("*")) {
      throw new IllegalArgumentException("name must not contain wildcards");
    }
    SlotType slotType = parseSlotType(body.getSlotType());
    requireSessionUserForDesignWrite();
    String session = currentSession();
    String user = currentUser();
    assertNameUnique(name);
    try {
      List<IPSTemplateSlot> created = designWs.createSlots(List.of(name), session, user);
      if (created == null || created.isEmpty() || created.get(0) == null) {
        throw new IllegalStateException("Design WS createSlots returned empty");
      }
      IPSTemplateSlot slot = created.get(0);
      String label = StringUtils.isNotBlank(body.getLabel()) ? body.getLabel().trim() : name;
      slot.setLabel(label);
      if (body.getDescription() != null) {
        slot.setDescription(body.getDescription());
      }
      if (slotType != null) {
        slot.setSlottype(slotType);
      }
      designWs.saveSlots(Collections.singletonList(slot), true, session, user);
      IPSTemplateSlot reloaded = resolveSlot(name, false);
      return reloaded != null ? toDetail(reloaded) : toDetail(slot);
    } catch (WebApplicationException | IllegalStateException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw mapCreateNameCollision(name, e);
    } catch (PSErrorsException e) {
      throw mapCreatePersistFailure(name, e, "Failed to save new slot");
    } catch (Exception e) {
      if (isAlreadyExistsFailure(e)) {
        throw new WebApplicationException("Slot already exists: " + name, 409);
      }
      log.error("Failed to create slot {}: {}", name, e.getMessage(), e);
      throw new IllegalStateException("Failed to create slot", e);
    }
  }

  @Override
  public boolean deleteSlot(URI baseUri, String idOrName) {
    requireAdmin();
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    requireSessionUserForDesignWrite();
    String trimmed = idOrName.trim();
    if (trimmed.contains("*")) {
      throw new IllegalArgumentException("idOrName must not contain wildcards");
    }
    String session = currentSession();
    String user = currentUser();
    try {
      IPSTemplateSlot current = resolveSlot(trimmed, false);
      if (current == null) {
        return false;
      }
      if (current.isSystemSlot()) {
        throw new WebApplicationException(
            "System slots cannot be deleted: " + StringUtils.defaultString(current.getName()),
            409);
      }
      if (current.getGUID() == null) {
        throw new IllegalStateException(
            "Slot '" + trimmed + "' has no GUID (corrupt identifier); cannot delete");
      }
      IPSTemplateSlot locked;
      try {
        locked = resolveSlot(trimmed, true);
      } catch (PSErrorResultsException e) {
        throw new WebApplicationException(
            "Could not delete slot; design lock required or held by another user", 409);
      }
      if (locked == null || locked.getGUID() == null) {
        throw new WebApplicationException(
            "Could not delete slot; design lock required or held by another user", 409);
      }
      try {
        designWs.deleteSlots(Collections.singletonList(locked.getGUID()), false, session, user);
      } catch (PSErrorsException e) {
        if (isLockFailure(e)) {
          throw new WebApplicationException(
              "Could not delete slot; design lock required or held by another user", 409);
        }
        String details = formatDeleteErrors(e);
        throw new IllegalArgumentException("Could not delete slot: " + details, e);
      }
      return true;
    } catch (IllegalArgumentException | IllegalStateException | WebApplicationException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      log.debug("Slot not found for delete {}: {}", idOrName, e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("Failed to delete slot {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to delete slot", e);
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
    String sv = g.getStringValue();
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

    // ADR-003 / #2691: expose definition slot_layout / slot_styles on REST wire DTO.
    Map<String, Object> layout = slot.getSlotLayout();
    if (layout != null && !layout.isEmpty()) {
      d.setSlotLayout(new HashMap<>(layout));
    }
    Map<String, Object> styles = slot.getSlotStyles();
    if (styles != null && !styles.isEmpty()) {
      d.setSlotStyles(new HashMap<>(styles));
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

  static boolean wantsFinderWrite(SlotDetail body) {
    return body != null
        && (body.getFinderName() != null
            || body.getRelationshipName() != null
            || body.getFinderArguments() != null);
  }

  private void applyMutableSlotUpdates(IPSTemplateSlot slot, SlotDetail body) {
    if (body.getLabel() != null) {
      slot.setLabel(body.getLabel());
    }
    if (body.getDescription() != null) {
      slot.setDescription(body.getDescription());
    }
    // Non-null layout/styles replace definition maps (empty/schema-only clears to defaults).
    if (body.getSlotLayout() != null) {
      slot.setSlotLayout(body.getSlotLayout());
    }
    if (body.getSlotStyles() != null) {
      slot.setSlotStyles(body.getSlotStyles());
    }
    if (body.getAssociations() != null) {
      slot.setSlotAssociations(toAssociationPairs(body.getAssociations()));
    }
    if (body.getFinderName() != null) {
      slot.setFinderName(body.getFinderName().trim());
    }
    if (body.getRelationshipName() != null) {
      slot.setRelationshipName(body.getRelationshipName().trim());
    }
    if (body.getFinderArguments() != null) {
      slot.setFinderArguments(
          body.getFinderArguments().isEmpty() ? null : new HashMap<>(body.getFinderArguments()));
    }
  }

  private void applyFinderRelationshipUpdates(SlotDetail body) {
    if (body.getFinderName() != null) {
      requireValidFinder(body.getFinderName());
    }
    if (body.getRelationshipName() != null) {
      requireValidRelationship(body.getRelationshipName());
    }
  }

  private void requireValidFinder(String finderName) {
    if (StringUtils.isBlank(finderName)) {
      throw new IllegalArgumentException("finderName is required");
    }
    if (assemblyService == null) {
      throw new IllegalStateException("Assembly service unavailable to validate finder");
    }
    String trimmed = finderName.trim();
    try {
      IPSSlotContentFinder finder = assemblyService.loadFinder(trimmed);
      if (finder == null) {
        throw new IllegalArgumentException("Invalid finder extension: " + trimmed);
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid finder extension: " + trimmed, e);
    }
  }

  private void requireValidRelationship(String relationshipName) {
    if (StringUtils.isBlank(relationshipName)) {
      return;
    }
    if (systemDesign == null) {
      throw new IllegalStateException(
          "Design service unavailable to validate relationship type");
    }
    String trimmed = relationshipName.trim();
    try {
      List<IPSCatalogSummary> found = systemDesign.findRelationshipTypes(trimmed, null);
      if (found == null || found.isEmpty()) {
        throw new IllegalArgumentException("Unknown relationship type: " + trimmed);
      }
      for (IPSCatalogSummary summary : found) {
        if (summary != null && trimmed.equalsIgnoreCase(StringUtils.defaultString(summary.getName()))) {
          return;
        }
      }
      if (found.size() == 1 && found.get(0) != null) {
        return;
      }
      throw new IllegalArgumentException("Unknown relationship type: " + trimmed);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Unknown relationship type: " + trimmed, e);
    }
  }

  private void requireHeldLock(IPSGuid slotGuid, String prefix) {
    if (systemDesign == null) {
      throw new IllegalStateException(prefix + "; design service unavailable");
    }
    List<PSObjectSummary> locked;
    try {
      locked = systemDesign.isLocked(Collections.singletonList(slotGuid), currentUser());
    } catch (PSErrorResultsException e) {
      throw new WebApplicationException(prefix + "; design lock required", 409);
    }
    PSObjectSummary summary = locked == null || locked.isEmpty() ? null : locked.get(0);
    if (summary == null || !summary.isLocked()) {
      throw new WebApplicationException(prefix + "; design lock required", 409);
    }
    String user = currentUser();
    if (!summary.isLockedBy(user)) {
      PSObjectLockSummary info = summary.getLocked();
      String locker = info != null ? info.getLocker() : null;
      throw new WebApplicationException(
          locker != null ? prefix + "; locked by " + locker : prefix + "; design lock required",
          409);
    }
  }

  private void requireNotLockedByOther(IPSGuid slotGuid, String prefix) {
    List<PSObjectSummary> locked;
    try {
      locked = systemDesign.isLocked(Collections.singletonList(slotGuid), currentUser());
    } catch (PSErrorResultsException e) {
      throw new WebApplicationException(prefix, 409);
    }
    PSObjectSummary summary = locked == null || locked.isEmpty() ? null : locked.get(0);
    if (summary != null && summary.isLocked() && !summary.isLockedBy(currentUser())) {
      PSObjectLockSummary info = summary.getLocked();
      String locker = info != null ? info.getLocker() : null;
      throw new WebApplicationException(
          locker != null ? prefix + "; locked by " + locker : prefix, 409);
    }
  }

  private long remainingLockMinutes(IPSGuid slotGuid) {
    if (systemDesign == null || slotGuid == null) {
      return DESIGN_LOCK_MINUTES;
    }
    try {
      List<PSObjectSummary> locked =
          systemDesign.isLocked(Collections.singletonList(slotGuid), currentUser());
      if (locked != null && !locked.isEmpty() && locked.get(0) != null) {
        PSObjectLockSummary info = locked.get(0).getLocked();
        if (info != null && info.getRemainingTime() > 0) {
          return info.getRemainingTime();
        }
      }
    } catch (Exception e) {
      log.debug("Could not read remaining lock time: {}", e.getMessage());
    }
    return DESIGN_LOCK_MINUTES;
  }

  static ObjectLockSummary toLockSummary(String session, String user, long remainingMinutes) {
    ObjectLockSummary summary = new ObjectLockSummary();
    summary.setSession(session);
    summary.setLocker(user);
    summary.setRemainingTime(remainingMinutes);
    return summary;
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

  private static void requireSessionUserForDesignWrite() {
    if (StringUtils.isBlank(currentSession()) || StringUtils.isBlank(currentUser())) {
      throw new WebApplicationException(
          "Request session/user required for slot design session", Response.Status.FORBIDDEN);
    }
  }

  private void requireAdmin() {
    boolean allowed;
    try {
      allowed = adminChecker.getAsBoolean();
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.debug("Admin check failed: {}", e.getMessage());
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
    }
    if (!allowed) {
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
    }
  }

  boolean isCurrentUserAdmin() {
    if (userService == null) {
      return false;
    }
    try {
      PSCurrentUser current = userService.getCurrentUser();
      if (current == null || StringUtils.isBlank(current.getName())) {
        return false;
      }
      return userService.isAdminUser(current.getName());
    } catch (PSDataServiceException e) {
      log.debug("Unable to resolve current user for Admin check: {}", e.getMessage());
      return false;
    }
  }

  private void assertNameUnique(String name) {
    List<IPSCatalogSummary> existing = designWs.findSlots(name, null);
    if (existing == null) {
      return;
    }
    for (IPSCatalogSummary summary : existing) {
      if (summary != null && name.equalsIgnoreCase(StringUtils.defaultString(summary.getName()))) {
        throw new WebApplicationException("Slot already exists: " + name, 409);
      }
    }
  }

  private static SlotType parseSlotType(String slotType) {
    if (StringUtils.isBlank(slotType)) {
      return null;
    }
    String trimmed = slotType.trim();
    if ("REGULAR".equalsIgnoreCase(trimmed)) {
      return SlotType.REGULAR;
    }
    if ("INLINE".equalsIgnoreCase(trimmed)) {
      return SlotType.INLINE;
    }
    throw new IllegalArgumentException("slotType must be REGULAR or INLINE");
  }

  private static RuntimeException mapCreateNameCollision(String name, IllegalArgumentException e) {
    if (isAlreadyExistsFailure(e)) {
      return new WebApplicationException("Slot already exists: " + name, 409);
    }
    return e;
  }

  private RuntimeException mapCreatePersistFailure(String name, Exception e, String fallback) {
    if (isAlreadyExistsFailure(e)) {
      return new WebApplicationException("Slot already exists: " + name, 409);
    }
    log.error("{} {}: {}", fallback, name, e.getMessage(), e);
    return new IllegalStateException(fallback, e);
  }

  static boolean isAlreadyExistsFailure(Throwable t) {
    for (Throwable cur = t; cur != null && cur != cur.getCause(); cur = cur.getCause()) {
      String msg = cur.getMessage();
      if (cur instanceof PSErrorException pe && StringUtils.isNotBlank(pe.getErrorMessage())) {
        msg = pe.getErrorMessage();
      }
      if (msg != null && msg.toLowerCase().contains("already exists")) {
        return true;
      }
    }
    return false;
  }

  private static boolean isLockFailure(PSErrorsException e) {
    if (e == null || e.getErrors() == null) {
      return false;
    }
    for (Object err : e.getErrors().values()) {
      if (err == null) {
        continue;
      }
      String msg = err.toString();
      if (err instanceof PSErrorException pe && StringUtils.isNotBlank(pe.getErrorMessage())) {
        msg = pe.getErrorMessage();
      }
      if (msg != null) {
        String lower = msg.toLowerCase();
        if (lower.contains("lock") || lower.contains("locked")) {
          return true;
        }
      }
    }
    return false;
  }

  private static String formatDeleteErrors(PSErrorsException e) {
    if (e == null || e.getErrors() == null || e.getErrors().isEmpty()) {
      return e != null && e.getMessage() != null ? e.getMessage() : "unknown error";
    }
    StringBuilder sb = new StringBuilder();
    for (Object err : e.getErrors().values()) {
      if (sb.length() > 0) {
        sb.append("; ");
      }
      if (err instanceof PSErrorException pe && StringUtils.isNotBlank(pe.getErrorMessage())) {
        sb.append(pe.getErrorMessage());
      } else if (err != null) {
        sb.append(err);
      }
    }
    return sb.length() > 0 ? sb.toString() : "unknown error";
  }

  private static boolean containsWhitespace(String name) {
    for (int i = 0; i < name.length(); i++) {
      if (Character.isWhitespace(name.charAt(i))) {
        return true;
      }
    }
    return false;
  }
}
