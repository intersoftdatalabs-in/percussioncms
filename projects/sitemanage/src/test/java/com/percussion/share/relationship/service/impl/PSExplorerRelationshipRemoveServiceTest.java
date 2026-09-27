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

  private static PSRelationship folderRel() {
    PSRelationship rel = edge(8, 42, 3, "Folder", PSRelationshipConfig.CATEGORY_FOLDER);
    return rel;
  }
}
