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

import com.percussion.cms.objectstore.PSAaRelationship;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.rest.slotrelationships.ISlotRelationshipAdaptor;
import com.percussion.rest.slotrelationships.SlotAddRequest;
import com.percussion.rest.slotrelationships.SlotAllowedChoice;
import com.percussion.rest.slotrelationships.SlotAllowedChoiceList;
import com.percussion.rest.slotrelationships.SlotCanvas;
import com.percussion.rest.slotrelationships.SlotCanvasSlot;
import com.percussion.rest.slotrelationships.SlotMoveRequest;
import com.percussion.rest.slotrelationships.SlotRelationship;
import com.percussion.rest.slotrelationships.SlotTemplateSlotRequest;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Active Assembly slot relationships via {@link IPSContentWs}. Same backends as classic
 * {@code PSAddSnippetAction} / arrange — not Data Flow HTML.
 */
@PSSiteManageBean
public class SlotRelationshipAdaptor implements ISlotRelationshipAdaptor {

  private static final Logger log = LogManager.getLogger(SlotRelationshipAdaptor.class);

  private final ContentWs contentWs;
  private final AssemblyLookup assembly;
  private final GuidFactory guids;
  private final TypeNamer typeNamer;

  public SlotRelationshipAdaptor() {
    this(liveContentWs(), liveAssembly(), liveGuids(), SlotRelationshipAdaptor::liveTypeName);
  }

  SlotRelationshipAdaptor(
      ContentWs contentWs, AssemblyLookup assembly, GuidFactory guids, TypeNamer typeNamer) {
    this.contentWs = contentWs;
    this.assembly = assembly;
    this.guids = guids;
    this.typeNamer = typeNamer;
  }

  @Override
  public SlotCanvas canvas(int ownerId, Integer templateId) {
    try {
      List<SlotCanvasSlot> slots = new ArrayList<>();
      if (templateId != null && templateId > 0) {
        IPSAssemblyTemplate template = assembly.findTemplate(guids.template(templateId));
        if (template != null && template.getSlots() != null) {
          List<IPSTemplateSlot> ordered = new ArrayList<>(template.getSlots());
          ordered.sort(Comparator.comparing(s -> StringUtils.defaultString(s.getName())));
          for (IPSTemplateSlot slot : ordered) {
            slots.add(toCanvasSlot(ownerId, slot));
          }
        }
      } else {
        slots.addAll(canvasFromRelationships(ownerId));
      }
      return new SlotCanvas(ownerId, templateId, slots);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.debug("Failed to load slot canvas for {}: {}", ownerId, e.toString());
      throw new WebApplicationException(e, 500);
    }
  }

  @Override
  public SlotRelationship add(SlotAddRequest request) {
    try {
      IPSGuid owner = guids.content(request.getOwnerId());
      IPSGuid dependent = guids.content(request.getDependentId());
      IPSGuid slot = guids.slot(request.getSlotId());
      IPSGuid template = guids.template(request.getTemplateId());
      IPSGuid folder = request.getFolderId() != null ? guids.folder(request.getFolderId()) : null;
      IPSGuid site = request.getSiteId() != null ? guids.site(request.getSiteId()) : null;
      int index = request.getIndex() == null ? -1 : request.getIndex();
      List<PSAaRelationship> created =
          contentWs.addContentRelations(
              owner, Collections.singletonList(dependent), folder, site, slot, template, index);
      if (created == null || created.isEmpty()) {
        return null;
      }
      return toWire(created.get(0));
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.debug("Failed to add slot relationship: {}", e.toString());
      throw new WebApplicationException(e, 500);
    }
  }

  @Override
  public void remove(int relationshipId) {
    try {
      contentWs.deleteContentRelations(Collections.singletonList(guids.relationship(relationshipId)));
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.debug("Failed to remove relationship {}: {}", relationshipId, e.toString());
      throw new WebApplicationException(e, 500);
    }
  }

  @Override
  public void move(int relationshipId, SlotMoveRequest request) {
    try {
      PSAaRelationship current = requireRelationship(relationshipId);
      String direction =
          StringUtils.defaultString(request.getDirection()).trim().toUpperCase(Locale.ROOT);
      List<PSAaRelationship> siblings =
          contentWs.loadSlotContentRelationships(
              guids.content(current.getOwner().getId()), current.getSlotId());
      if (siblings == null) {
        siblings = List.of();
      }
      int currentIndex = indexOf(siblings, relationshipId);
      if (currentIndex < 0) {
        throw new WebApplicationException("Relationship is not in its slot", 400);
      }
      int target;
      if ("UP".equals(direction)) {
        if (currentIndex == 0) {
          return;
        }
        target = currentIndex - 1;
      } else if ("DOWN".equals(direction)) {
        if (currentIndex >= siblings.size() - 1) {
          return;
        }
        target = currentIndex + 1;
      } else if ("INDEX".equals(direction)) {
        if (request.getIndex() == null) {
          throw new WebApplicationException("index is required when direction is INDEX", 400);
        }
        target = request.getIndex();
      } else {
        throw new WebApplicationException("direction must be UP, DOWN, or INDEX", 400);
      }
      contentWs.reorderContentRelations(
          Collections.singletonList(guids.relationship(relationshipId)), target);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.debug("Failed to move relationship {}: {}", relationshipId, e.toString());
      throw new WebApplicationException(e, 500);
    }
  }

  @Override
  public SlotRelationship changeTemplateSlot(int relationshipId, SlotTemplateSlotRequest request) {
    try {
      PSAaRelationship current = requireRelationship(relationshipId);
      int ownerId = current.getOwner().getId();
      int dependentId = current.getDependent().getId();
      contentWs.deleteContentRelations(Collections.singletonList(guids.relationship(relationshipId)));
      SlotAddRequest add = new SlotAddRequest();
      add.setOwnerId(ownerId);
      add.setDependentId(dependentId);
      add.setSlotId(request.getSlotId());
      add.setTemplateId(request.getTemplateId());
      add.setIndex(request.getIndex() == null ? -1 : request.getIndex());
      return add(add);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.debug("Failed to change template/slot for {}: {}", relationshipId, e.toString());
      throw new WebApplicationException(e, 500);
    }
  }

  @Override
  public SlotAllowedChoiceList allowedTypes(int slotId) {
    try {
      IPSTemplateSlot slot = assembly.findSlot(guids.slot(slotId));
      if (slot == null) {
        return new SlotAllowedChoiceList(List.of());
      }
      Map<Integer, SlotAllowedChoice> unique = new LinkedHashMap<>();
      for (PSPair<IPSGuid, IPSGuid> pair : safeAssociations(slot)) {
        if (pair == null || pair.getFirst() == null) {
          continue;
        }
        int id = pair.getFirst().getUUID();
        if (unique.containsKey(id)) {
          continue;
        }
        String name = typeNamer.name(id);
        unique.put(id, new SlotAllowedChoice(id, name, StringUtils.defaultIfBlank(name, String.valueOf(id))));
      }
      return new SlotAllowedChoiceList(new ArrayList<>(unique.values()));
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.debug("Failed to list allowed types for slot {}: {}", slotId, e.toString());
      throw new WebApplicationException(e, 500);
    }
  }

  @Override
  public SlotAllowedChoiceList allowedTemplates(int slotId, Integer contentTypeId) {
    try {
      IPSTemplateSlot slot = assembly.findSlot(guids.slot(slotId));
      if (slot == null) {
        return new SlotAllowedChoiceList(List.of());
      }
      Map<Integer, SlotAllowedChoice> unique = new LinkedHashMap<>();
      for (PSPair<IPSGuid, IPSGuid> pair : safeAssociations(slot)) {
        if (pair == null || pair.getSecond() == null) {
          continue;
        }
        if (contentTypeId != null
            && contentTypeId > 0
            && (pair.getFirst() == null || pair.getFirst().getUUID() != contentTypeId)) {
          continue;
        }
        int id = pair.getSecond().getUUID();
        if (unique.containsKey(id)) {
          continue;
        }
        IPSAssemblyTemplate template = assembly.findTemplate(pair.getSecond());
        String name = template != null ? template.getName() : String.valueOf(id);
        String label =
            template != null
                ? StringUtils.defaultIfBlank(template.getLabel(), name)
                : name;
        unique.put(id, new SlotAllowedChoice(id, name, label));
      }
      return new SlotAllowedChoiceList(new ArrayList<>(unique.values()));
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.debug("Failed to list allowed templates for slot {}: {}", slotId, e.toString());
      throw new WebApplicationException(e, 500);
    }
  }

  private SlotCanvasSlot toCanvasSlot(int ownerId, IPSTemplateSlot slot) throws Exception {
    int slotId = slot.getGUID().getUUID();
    List<SlotRelationship> items = new ArrayList<>();
    List<PSAaRelationship> rels =
        contentWs.loadSlotContentRelationships(guids.content(ownerId), slot.getGUID());
    if (rels != null) {
      for (PSAaRelationship rel : rels) {
        items.add(toWire(rel));
      }
    }
    String name = StringUtils.defaultString(slot.getName());
    String label = StringUtils.defaultIfBlank(slot.getLabel(), name);
    return new SlotCanvasSlot(slotId, name, label, items);
  }

  private List<SlotCanvasSlot> canvasFromRelationships(int ownerId) throws Exception {
    PSRelationshipFilter filter = new PSRelationshipFilter();
    filter.setOwnerId(ownerId);
    filter.setCategory(PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY);
    List<PSAaRelationship> rels = contentWs.loadContentRelations(filter, true);
    Map<Integer, SlotCanvasSlot> bySlot = new LinkedHashMap<>();
    if (rels != null) {
      for (PSAaRelationship rel : rels) {
        SlotRelationship wire = toWire(rel);
        SlotCanvasSlot slot =
            bySlot.computeIfAbsent(
                wire.getSlotId(),
                id -> {
                  IPSTemplateSlot found = null;
                  try {
                    found = assembly.findSlot(guids.slot(id));
                  } catch (Exception ignored) {
                    // keep id-only slot
                  }
                  String name = found != null ? found.getName() : String.valueOf(id);
                  String label =
                      found != null ? StringUtils.defaultIfBlank(found.getLabel(), name) : name;
                  return new SlotCanvasSlot(id, name, label, new ArrayList<>());
                });
        slot.getItems().add(wire);
      }
    }
    return new ArrayList<>(bySlot.values());
  }

  private PSAaRelationship requireRelationship(int relationshipId) throws Exception {
    PSRelationshipFilter filter = new PSRelationshipFilter();
    filter.setRelationshipId(relationshipId);
    List<PSAaRelationship> rels = contentWs.loadContentRelations(filter, true);
    if (rels == null || rels.isEmpty()) {
      throw new WebApplicationException("Relationship not found", 404);
    }
    return rels.get(0);
  }

  private static int indexOf(List<PSAaRelationship> siblings, int relationshipId) {
    for (int i = 0; i < siblings.size(); i++) {
      if (siblings.get(i).getId() == relationshipId) {
        return i;
      }
    }
    return -1;
  }

  private static SlotRelationship toWire(PSAaRelationship rel) {
    int rid = rel.getId();
    int ownerId = rel.getOwner() != null ? rel.getOwner().getId() : 0;
    int dependentId = rel.getDependent() != null ? rel.getDependent().getId() : 0;
    int slotId = rel.getSlotId() != null ? rel.getSlotId().getUUID() : 0;
    int templateId = 0;
    try {
      if (rel.getTemplateId() != null) {
        templateId = rel.getTemplateId().getUUID();
      }
    } catch (RuntimeException ignored) {
      templateId = 0;
    }
    int sortRank = 0;
    try {
      sortRank = rel.getSortRank();
    } catch (RuntimeException ignored) {
      sortRank = 0;
    }
    return new SlotRelationship(rid, ownerId, dependentId, slotId, templateId, sortRank);
  }

  private static CollectionSafe<PSPair<IPSGuid, IPSGuid>> safeAssociations(IPSTemplateSlot slot) {
    return new CollectionSafe<>(slot.getSlotAssociations());
  }

  private static ContentWs liveContentWs() {
    return new LocatorContentWs();
  }

  private static AssemblyLookup liveAssembly() {
    return new LocatorAssembly();
  }

  private static GuidFactory liveGuids() {
    return new LocatorGuids();
  }

  private static String liveTypeName(int contentTypeId) {
    try {
      return PSItemDefManager.getInstance().contentTypeIdToName(contentTypeId);
    } catch (Exception e) {
      return String.valueOf(contentTypeId);
    }
  }

  @FunctionalInterface
  interface TypeNamer {
    String name(int contentTypeId);
  }

  interface GuidFactory {
    IPSGuid content(int id);

    IPSGuid folder(int id);

    IPSGuid site(int id);

    IPSGuid slot(int id);

    IPSGuid template(int id);

    IPSGuid relationship(int id);
  }

  interface AssemblyLookup {
    IPSAssemblyTemplate findTemplate(IPSGuid id) throws Exception;

    IPSTemplateSlot findSlot(IPSGuid id) throws Exception;
  }

  interface ContentWs {
    List<PSAaRelationship> addContentRelations(
        IPSGuid owner,
        List<IPSGuid> dependents,
        IPSGuid folderId,
        IPSGuid siteId,
        IPSGuid slotId,
        IPSGuid templateId,
        int index)
        throws Exception;

    void deleteContentRelations(List<IPSGuid> ids) throws Exception;

    void reorderContentRelations(List<IPSGuid> ids, int index) throws Exception;

    List<PSAaRelationship> loadSlotContentRelationships(IPSGuid ownerId, IPSGuid slotId)
        throws Exception;

    List<PSAaRelationship> loadContentRelations(PSRelationshipFilter filter, boolean loadRefs)
        throws Exception;
  }

  private static final class LocatorContentWs implements ContentWs {
    private IPSContentWs ws() {
      return PSContentWsLocator.getContentWebservice();
    }

    @Override
    public List<PSAaRelationship> addContentRelations(
        IPSGuid owner,
        List<IPSGuid> dependents,
        IPSGuid folderId,
        IPSGuid siteId,
        IPSGuid slotId,
        IPSGuid templateId,
        int index)
        throws Exception {
      return ws().addContentRelations(owner, dependents, folderId, siteId, slotId, templateId, index);
    }

    @Override
    public void deleteContentRelations(List<IPSGuid> ids) throws Exception {
      ws().deleteContentRelations(ids);
    }

    @Override
    public void reorderContentRelations(List<IPSGuid> ids, int index) throws Exception {
      ws().reorderContentRelations(ids, index);
    }

    @Override
    public List<PSAaRelationship> loadSlotContentRelationships(IPSGuid ownerId, IPSGuid slotId)
        throws Exception {
      return ws().loadSlotContentRelationships(ownerId, slotId);
    }

    @Override
    public List<PSAaRelationship> loadContentRelations(
        PSRelationshipFilter filter, boolean loadRefs) throws Exception {
      return ws().loadContentRelations(filter, loadRefs);
    }
  }

  private static final class LocatorAssembly implements AssemblyLookup {
    private IPSAssemblyService svc() {
      return PSAssemblyServiceLocator.getAssemblyService();
    }

    @Override
    public IPSAssemblyTemplate findTemplate(IPSGuid id) {
      return svc().findTemplate(id);
    }

    @Override
    public IPSTemplateSlot findSlot(IPSGuid id) {
      return svc().findSlot(id);
    }
  }

  private static final class LocatorGuids implements GuidFactory {
    private IPSGuidManager mgr() {
      return PSGuidManagerLocator.getGuidMgr();
    }

    @Override
    public IPSGuid content(int id) {
      return mgr().makeGuid(id, PSTypeEnum.LEGACY_CONTENT);
    }

    @Override
    public IPSGuid folder(int id) {
      return mgr().makeGuid(id, PSTypeEnum.LEGACY_CONTENT);
    }

    @Override
    public IPSGuid site(int id) {
      return mgr().makeGuid(id, PSTypeEnum.SITE);
    }

    @Override
    public IPSGuid slot(int id) {
      return mgr().makeGuid(id, PSTypeEnum.SLOT);
    }

    @Override
    public IPSGuid template(int id) {
      return mgr().makeGuid(id, PSTypeEnum.TEMPLATE);
    }

    @Override
    public IPSGuid relationship(int id) {
      return mgr().makeGuid(id, PSTypeEnum.RELATIONSHIP);
    }
  }

  /** Avoids null slot-association collections without extra imports in callers. */
  private static final class CollectionSafe<T> implements Iterable<T> {
    private final Iterable<T> inner;

    CollectionSafe(Iterable<T> inner) {
      this.inner = inner != null ? inner : List.of();
    }

    @Override
    public java.util.Iterator<T> iterator() {
      return inner.iterator();
    }
  }
}
