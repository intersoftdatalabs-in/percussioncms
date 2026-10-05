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
package com.percussion.share.relationship.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.share.relationship.service.ExplorerRelationshipAction;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PSExplorerRelationshipRemoveServiceTest {

  @Mock private IPSIdMapper idMapper;
  @Mock private IPSSystemWs systemWs;
  @Mock private IPSContentWs contentWs;
  @Mock private IPSGuid itemGuid;
  @Mock private IPSGuid relationshipGuid;

  private PSExplorerRelationshipRemoveService service;

  @BeforeEach
  void init() {
    service =
        new PSExplorerRelationshipRemoveService(
            idMapper, systemWs, contentWs, id -> relationshipGuid);
  }

  @Test
  void listSkipsFolderMembership() throws Exception {
    when(idMapper.getGuid("42")).thenReturn(itemGuid);
    when(itemGuid.getUUID()).thenReturn(42);
    PSRelationship translation = edge(7, 42, 9, "Translation", "rs_translation");
    PSRelationship folder = folderRel();
    when(systemWs.loadRelationships(any(PSRelationshipFilter.class)))
        .thenReturn(List.of(translation, folder));

    ExplorerRelationshipAction action = service.listOwned("42");

    assertEquals(ExplorerRelationshipAction.Status.LISTED, action.getStatus());
    assertEquals(1, action.getEdges().size());
    assertEquals(7, action.getEdges().get(0).getRelationshipId());
    assertEquals("Translation -> 9", action.getEdges().get(0).getLabel());
  }

  @Test
  void listOrdersActiveAssemblyInTheSameSlotBySortRank() throws Exception {
    when(idMapper.getGuid("42")).thenReturn(itemGuid);
    when(itemGuid.getUUID()).thenReturn(42);
    PSRelationship translation = edge(7, 42, 9, "Translation", "rs_translation");
    PSRelationship later = activeAssembly(11, 3, 2);
    PSRelationship otherSlot = activeAssembly(13, 9, 0);
    PSRelationship earlier = activeAssembly(12, 3, 0);
    when(systemWs.loadRelationships(any(PSRelationshipFilter.class)))
        .thenReturn(List.of(translation, later, otherSlot, earlier));

    ExplorerRelationshipAction action = service.listOwned("42");

    assertEquals(List.of(7, 12, 13, 11), ids(action));
    assertEquals(3, action.getEdges().get(1).getSlotId());
    assertEquals(0, action.getEdges().get(1).getSortRank());
    assertEquals(9, action.getEdges().get(2).getSlotId());
  }

  @Test
  void removeDeletesOwnedRelationship() throws Exception {
    when(idMapper.getGuid("42")).thenReturn(itemGuid);
    when(itemGuid.getUUID()).thenReturn(42);
    PSRelationship translation = edge(7, 42, 9, "Translation", "rs_translation");
    when(systemWs.loadRelationships(any(PSRelationshipFilter.class)))
        .thenReturn(List.of(translation));

    ExplorerRelationshipAction action = service.removeOwned("42", 7);

    assertEquals(ExplorerRelationshipAction.Status.REMOVED, action.getStatus());
    verify(systemWs).deleteRelationships(anyList());
    verify(contentWs, never()).deleteContentRelations(anyList());
  }

  @Test
  void removeRefusesRelationshipOwnedBySomeoneElse() throws Exception {
    when(idMapper.getGuid("42")).thenReturn(itemGuid);
    when(itemGuid.getUUID()).thenReturn(42);
    PSRelationship otherOwner = edge(7, 99, 9, "Translation", "rs_translation");
    when(systemWs.loadRelationships(any(PSRelationshipFilter.class)))
        .thenReturn(List.of(otherOwner));

    ExplorerRelationshipAction action = service.removeOwned("42", 7);

    assertEquals(ExplorerRelationshipAction.Status.CONFLICT, action.getStatus());
    assertTrue(action.getMessage().contains("selected item"));
    verify(systemWs, never()).deleteRelationships(anyList());
  }

  @Test
  void removeRefusesFolderRelationship() throws Exception {
    when(idMapper.getGuid("42")).thenReturn(itemGuid);
    when(itemGuid.getUUID()).thenReturn(42);
    PSRelationship folder = folderRel();
    when(systemWs.loadRelationships(any(PSRelationshipFilter.class))).thenReturn(List.of(folder));

    ExplorerRelationshipAction action = service.removeOwned("42", 8);

    assertEquals(ExplorerRelationshipAction.Status.CONFLICT, action.getStatus());
    verify(systemWs, never()).deleteRelationships(anyList());
  }

  @Test
  void blankItemIsBadRequestAndDoesNotDelete() {
    ExplorerRelationshipAction action = service.removeOwned("  ", 7);
    assertEquals(ExplorerRelationshipAction.Status.BAD_REQUEST, action.getStatus());
  }

  @Test
  void addCreatesOwnedNonFolderRelationship() throws Exception {
    IPSGuid targetGuid = org.mockito.Mockito.mock(IPSGuid.class);
    when(idMapper.getGuid("42")).thenReturn(itemGuid);
    when(idMapper.getGuid("9")).thenReturn(targetGuid);
    when(itemGuid.getUUID()).thenReturn(42);
    when(targetGuid.getUUID()).thenReturn(9);
    PSRelationship created = edge(11, 42, 9, "Translation", "rs_translation");
    when(systemWs.createRelationship("Translation", itemGuid, targetGuid)).thenReturn(created);

    ExplorerRelationshipAction action = service.addOwned("42", "9", "Translation");

    assertEquals(ExplorerRelationshipAction.Status.CREATED, action.getStatus());
    assertEquals(11, action.getEdges().get(0).getRelationshipId());
    verify(systemWs).saveRelationships(anyList());
  }

  @Test
  void addRefusesFolderTypeWithoutSaving() throws Exception {
    ExplorerRelationshipAction action = service.addOwned("42", "9", "rs_folder");

    assertEquals(ExplorerRelationshipAction.Status.CONFLICT, action.getStatus());
    verify(systemWs, never()).createRelationship(anyString(), any(), any());
    verify(systemWs, never()).saveRelationships(anyList());
  }

  @Test
  void addBlankTargetIsBadRequest() {
    ExplorerRelationshipAction action = service.addOwned("42", "  ", "Translation");
    assertEquals(ExplorerRelationshipAction.Status.BAD_REQUEST, action.getStatus());
  }

  @Test
  void addSaveFailureIsConflict() throws Exception {
    IPSGuid targetGuid = org.mockito.Mockito.mock(IPSGuid.class);
    when(idMapper.getGuid("42")).thenReturn(itemGuid);
    when(idMapper.getGuid("9")).thenReturn(targetGuid);
    when(itemGuid.getUUID()).thenReturn(42);
    when(targetGuid.getUUID()).thenReturn(9);
    PSRelationship created = edge(11, 42, 9, "Translation", "rs_translation");
    when(systemWs.createRelationship("Translation", itemGuid, targetGuid)).thenReturn(created);
    org.mockito.Mockito.doThrow(new com.percussion.webservices.PSErrorsException())
        .when(systemWs)
        .saveRelationships(anyList());

    ExplorerRelationshipAction action = service.addOwned("42", "9", "Translation");

    assertEquals(ExplorerRelationshipAction.Status.CONFLICT, action.getStatus());
    verify(systemWs).deleteRelationships(anyList());
  }

  @Test
  void addDeletesPersistedFolderReturnedByCreate() throws Exception {
    IPSGuid targetGuid = org.mockito.Mockito.mock(IPSGuid.class);
    when(idMapper.getGuid("42")).thenReturn(itemGuid);
    when(idMapper.getGuid("9")).thenReturn(targetGuid);
    when(itemGuid.getUUID()).thenReturn(42);
    when(targetGuid.getUUID()).thenReturn(9);
    PSRelationship created = edge(12, 42, 9, "CustomLink", PSRelationshipConfig.CATEGORY_FOLDER);
    when(systemWs.createRelationship("CustomLink", itemGuid, targetGuid)).thenReturn(created);

    ExplorerRelationshipAction action = service.addOwned("42", "9", "CustomLink");

    assertEquals(ExplorerRelationshipAction.Status.CONFLICT, action.getStatus());
    verify(systemWs).deleteRelationships(anyList());
    verify(systemWs, never()).saveRelationships(anyList());
  }

  @Test
  void addDeletesPersistedActiveAssemblyReturnedByCreate() throws Exception {
    IPSGuid targetGuid = org.mockito.Mockito.mock(IPSGuid.class);
    when(idMapper.getGuid("42")).thenReturn(itemGuid);
    when(idMapper.getGuid("9")).thenReturn(targetGuid);
    when(itemGuid.getUUID()).thenReturn(42);
    when(targetGuid.getUUID()).thenReturn(9);
    PSRelationship created = edge(13, 42, 9, "ActiveAssembly", "rs_activeassembly");
    when(created.getConfig().isActiveAssemblyRelationship()).thenReturn(true);
    when(systemWs.createRelationship("ActiveAssembly", itemGuid, targetGuid)).thenReturn(created);

    ExplorerRelationshipAction action = service.addOwned("42", "9", "ActiveAssembly");

    assertEquals(ExplorerRelationshipAction.Status.CONFLICT, action.getStatus());
    verify(contentWs).deleteContentRelations(anyList());
    verify(systemWs, never()).saveRelationships(anyList());
    verify(systemWs, never()).deleteRelationships(anyList());
  }

  @Test
  void unknownRelationshipIsNotFound() throws Exception {
    when(idMapper.getGuid("42")).thenReturn(itemGuid);
    when(itemGuid.getUUID()).thenReturn(42);
    when(systemWs.loadRelationships(any(PSRelationshipFilter.class))).thenReturn(List.of());

    ExplorerRelationshipAction action = service.removeOwned("42", 7);

    assertEquals(ExplorerRelationshipAction.Status.NOT_FOUND, action.getStatus());
    verify(systemWs, never()).deleteRelationships(anyList());
  }

  private static PSRelationship edge(
      int id, int ownerId, int dependentId, String name, String category) {
    PSRelationship rel = org.mockito.Mockito.mock(PSRelationship.class);
    PSRelationshipConfig config = org.mockito.Mockito.mock(PSRelationshipConfig.class);
    lenient().when(rel.getId()).thenReturn(id);
    lenient().when(rel.getOwner()).thenReturn(new PSLocator(ownerId, 1));
    lenient().when(rel.getDependent()).thenReturn(new PSLocator(dependentId, 1));
    lenient().when(rel.getConfig()).thenReturn(config);
    lenient().when(config.getName()).thenReturn(name);
    lenient().when(config.getCategory()).thenReturn(category);
    lenient().when(config.isActiveAssemblyRelationship()).thenReturn(false);
    return rel;
  }

  private static List<Integer> ids(ExplorerRelationshipAction action) {
    List<Integer> ids = new java.util.ArrayList<>();
    for (var edge : action.getEdges()) {
      ids.add(edge.getRelationshipId());
    }
    return ids;
  }

  private static PSRelationship activeAssembly(int id, int slotId, int sortRank) {
    PSRelationship rel = edge(id, 42, id + 1, "ActiveAssembly", "rs_activeassembly");
    when(rel.getConfig().isActiveAssemblyRelationship()).thenReturn(true);
    lenient()
        .when(rel.getProperty(IPSHtmlParameters.SYS_SLOTID))
        .thenReturn(Integer.toString(slotId));
    lenient()
        .when(rel.getProperty(IPSHtmlParameters.SYS_SORTRANK))
        .thenReturn(Integer.toString(sortRank));
    return rel;
  }

  private static PSRelationship folderRel() {
    PSRelationship rel = edge(8, 42, 3, "Folder", PSRelationshipConfig.CATEGORY_FOLDER);
    return rel;
  }
}
