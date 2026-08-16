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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSAaRelationship;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.rest.slotrelationships.SlotAddRequest;
import com.percussion.rest.slotrelationships.SlotAllowedChoiceList;
import com.percussion.rest.slotrelationships.SlotCanvas;
import com.percussion.rest.slotrelationships.SlotMoveRequest;
import com.percussion.rest.slotrelationships.SlotRelationship;
import com.percussion.rest.slotrelationships.SlotTemplateSlotRequest;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class SlotRelationshipAdaptorTest {

  @Test
  void canvas_listsTemplateSlotsAndRelationships() {
    FakeContentWs ws = new FakeContentWs();
    FakeAssembly assembly = new FakeAssembly();
    IPSTemplateSlot slot = fakeSlot(5, "sidebar", "Sidebar", List.of());
    assembly.slots.put(5, slot);
    assembly.templates.put(7, fakeTemplate(7, "rffPgGeneric", Set.of(slot)));
    ws.slotRels.put(key(10, 5), List.of(fakeRel(99, 10, 20, 5, 4, 0)));

    SlotCanvas canvas = adaptor(ws, assembly).canvas(10, 7);
    assertEquals(10, canvas.getOwnerId());
    assertEquals(7, canvas.getTemplateId());
    assertEquals(1, canvas.getSlots().size());
    assertEquals(5, canvas.getSlots().get(0).getSlotId());
    assertEquals("sidebar", canvas.getSlots().get(0).getName());
    assertEquals(1, canvas.getSlots().get(0).getItems().size());
    assertEquals(99, canvas.getSlots().get(0).getItems().get(0).getRelationshipId());
  }

  @Test
  void add_delegatesToContentWs() {
    FakeContentWs ws = new FakeContentWs();
    ws.addResult = fakeRel(12, 10, 20, 5, 4, 1);
    SlotAddRequest req = new SlotAddRequest();
    req.setOwnerId(10);
    req.setDependentId(20);
    req.setSlotId(5);
    req.setTemplateId(4);
    SlotRelationship created = adaptor(ws, new FakeAssembly()).add(req);
    assertEquals(12, created.getRelationshipId());
    assertEquals(20, created.getDependentId());
    assertEquals(1, ws.addCalls);
  }

  @Test
  void moveUp_reordersToPreviousIndex() {
    FakeContentWs ws = new FakeContentWs();
    PSAaRelationship first = fakeRel(1, 10, 21, 5, 4, 0);
    PSAaRelationship second = fakeRel(2, 10, 22, 5, 4, 1);
    ws.byId.put(2, second);
    ws.slotRels.put(key(10, 5), List.of(first, second));
    SlotMoveRequest req = new SlotMoveRequest();
    req.setDirection("UP");
    adaptor(ws, new FakeAssembly()).move(2, req);
    assertEquals(List.of(2), ws.reorderedIds);
    assertEquals(0, ws.reorderIndex);
  }

  @Test
  void moveInvalidDirection_is400() {
    FakeContentWs ws = new FakeContentWs();
    ws.byId.put(2, fakeRel(2, 10, 22, 5, 4, 1));
    ws.slotRels.put(key(10, 5), List.of(fakeRel(2, 10, 22, 5, 4, 1)));
    SlotMoveRequest req = new SlotMoveRequest();
    req.setDirection("SIDEWAYS");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor(ws, new FakeAssembly()).move(2, req));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void changeTemplateSlot_deletesThenAdds() {
    FakeContentWs ws = new FakeContentWs();
    ws.byId.put(9, fakeRel(9, 10, 20, 5, 4, 0));
    ws.addResult = fakeRel(11, 10, 20, 6, 8, 0);
    SlotTemplateSlotRequest req = new SlotTemplateSlotRequest();
    req.setSlotId(6);
    req.setTemplateId(8);
    SlotRelationship updated = adaptor(ws, new FakeAssembly()).changeTemplateSlot(9, req);
    assertEquals(1, ws.deleteCalls);
    assertEquals(1, ws.addCalls);
    assertEquals(11, updated.getRelationshipId());
    assertEquals(6, updated.getSlotId());
  }

  @Test
  void allowedTypes_dedupesAssociations() {
    FakeAssembly assembly = new FakeAssembly();
    IPSGuid ct = new PSGuid(PSTypeEnum.NODEDEF, 301);
    IPSGuid t1 = new PSGuid(PSTypeEnum.TEMPLATE, 4);
    IPSGuid t2 = new PSGuid(PSTypeEnum.TEMPLATE, 8);
    assembly.slots.put(
        5,
        fakeSlot(
            5,
            "sidebar",
            "Sidebar",
            List.of(new PSPair<>(ct, t1), new PSPair<>(ct, t2))));
    SlotAllowedChoiceList types = adaptor(new FakeContentWs(), assembly).allowedTypes(5);
    assertEquals(1, types.getItems().size());
    assertEquals(301, types.getItems().get(0).getId());
    assertEquals("rffEvent", types.getItems().get(0).getName());
  }

  @Test
  void allowedTemplates_canFilterByType() {
    FakeAssembly assembly = new FakeAssembly();
    IPSGuid ctA = new PSGuid(PSTypeEnum.NODEDEF, 301);
    IPSGuid ctB = new PSGuid(PSTypeEnum.NODEDEF, 302);
    IPSGuid t1 = new PSGuid(PSTypeEnum.TEMPLATE, 4);
    IPSGuid t2 = new PSGuid(PSTypeEnum.TEMPLATE, 8);
    assembly.templates.put(4, fakeTemplate(4, "Title", Set.of()));
    assembly.templates.put(8, fakeTemplate(8, "Callout", Set.of()));
    assembly.slots.put(
        5,
        fakeSlot(
            5,
            "sidebar",
            "Sidebar",
            List.of(new PSPair<>(ctA, t1), new PSPair<>(ctB, t2))));
    SlotRelationshipAdaptor adaptor = adaptor(new FakeContentWs(), assembly);
    SlotAllowedChoiceList filtered = adaptor.allowedTemplates(5, 301);
    assertEquals(1, filtered.getItems().size());
    assertEquals(4, filtered.getItems().get(0).getId());
    assertTrue(adaptor.allowedTemplates(5, null).getItems().size() >= 2);
  }

  private static SlotRelationshipAdaptor adaptor(FakeContentWs ws, FakeAssembly assembly) {
    return new SlotRelationshipAdaptor(ws, assembly, new FakeGuids(), id -> "rffEvent");
  }

  private static String key(int ownerId, int slotId) {
    return ownerId + ":" + slotId;
  }

  private static PSAaRelationship fakeRel(
      int id, int ownerId, int dependentId, int slotId, int templateId, int sortRank) {
    PSAaRelationship rel = mock(PSAaRelationship.class);
    when(rel.getId()).thenReturn(id);
    when(rel.getOwner()).thenReturn(new PSLocator(ownerId));
    when(rel.getDependent()).thenReturn(new PSLocator(dependentId));
    when(rel.getSlotId()).thenReturn(new PSGuid(PSTypeEnum.SLOT, slotId));
    when(rel.getTemplateId()).thenReturn(new PSGuid(PSTypeEnum.TEMPLATE, templateId));
    when(rel.getSortRank()).thenReturn(sortRank);
    return rel;
  }

  private static IPSTemplateSlot fakeSlot(
      int id, String name, String label, List<PSPair<IPSGuid, IPSGuid>> associations) {
    IPSTemplateSlot slot = mock(IPSTemplateSlot.class);
    when(slot.getGUID()).thenReturn(new PSGuid(PSTypeEnum.SLOT, id));
    when(slot.getName()).thenReturn(name);
    when(slot.getLabel()).thenReturn(label);
    when(slot.getSlotAssociations()).thenReturn(new ArrayList<>(associations));
    return slot;
  }

  private static IPSAssemblyTemplate fakeTemplate(
      int id, String name, Set<IPSTemplateSlot> slots) {
    IPSAssemblyTemplate template = mock(IPSAssemblyTemplate.class);
    when(template.getGUID()).thenReturn(new PSGuid(PSTypeEnum.TEMPLATE, id));
    when(template.getName()).thenReturn(name);
    when(template.getLabel()).thenReturn(name);
    when(template.getSlots()).thenReturn(new LinkedHashSet<>(slots));
    return template;
  }

  private static final class FakeGuids implements SlotRelationshipAdaptor.GuidFactory {
    @Override
    public IPSGuid content(int id) {
      return new PSGuid(PSTypeEnum.LEGACY_CONTENT, id);
    }

    @Override
    public IPSGuid folder(int id) {
      return new PSGuid(PSTypeEnum.LEGACY_CONTENT, id);
    }

    @Override
    public IPSGuid site(int id) {
      return new PSGuid(PSTypeEnum.SITE, id);
    }

    @Override
    public IPSGuid slot(int id) {
      return new PSGuid(PSTypeEnum.SLOT, id);
    }

    @Override
    public IPSGuid template(int id) {
      return new PSGuid(PSTypeEnum.TEMPLATE, id);
    }

    @Override
    public IPSGuid relationship(int id) {
      return new PSGuid(PSTypeEnum.RELATIONSHIP, id);
    }
  }

  private static final class FakeContentWs implements SlotRelationshipAdaptor.ContentWs {
    int addCalls;
    int deleteCalls;
    int reorderIndex = Integer.MIN_VALUE;
    List<Integer> reorderedIds = new ArrayList<>();
    PSAaRelationship addResult;
    Map<Integer, PSAaRelationship> byId = new HashMap<>();
    Map<String, List<PSAaRelationship>> slotRels = new HashMap<>();

    @Override
    public List<PSAaRelationship> addContentRelations(
        IPSGuid owner,
        List<IPSGuid> dependents,
        IPSGuid folderId,
        IPSGuid siteId,
        IPSGuid slotId,
        IPSGuid templateId,
        int index) {
      addCalls++;
      return addResult == null ? List.of() : List.of(addResult);
    }

    @Override
    public void deleteContentRelations(List<IPSGuid> ids) {
      deleteCalls++;
    }

    @Override
    public void reorderContentRelations(List<IPSGuid> ids, int index) {
      reorderIndex = index;
      reorderedIds = new ArrayList<>();
      for (IPSGuid id : ids) {
        reorderedIds.add((int) id.getUUID());
      }
    }

    @Override
    public List<PSAaRelationship> loadSlotContentRelationships(IPSGuid ownerId, IPSGuid slotId) {
      return slotRels.getOrDefault(
          (int) ownerId.getUUID() + ":" + (int) slotId.getUUID(), List.of());
    }

    @Override
    public List<PSAaRelationship> loadContentRelations(
        PSRelationshipFilter filter, boolean loadRefs) {
      if (filter.getRelationshipId() > 0) {
        PSAaRelationship found = byId.get(filter.getRelationshipId());
        return found == null ? List.of() : List.of(found);
      }
      return List.of();
    }
  }

  private static final class FakeAssembly implements SlotRelationshipAdaptor.AssemblyLookup {
    private final Map<Integer, IPSAssemblyTemplate> templates = new HashMap<>();
    private final Map<Integer, IPSTemplateSlot> slots = new HashMap<>();

    @Override
    public IPSAssemblyTemplate findTemplate(IPSGuid id) {
      return templates.get((int) id.getUUID());
    }

    @Override
    public IPSTemplateSlot findSlot(IPSGuid id) {
      return slots.get((int) id.getUUID());
    }
  }
}
