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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.rest.contenttypes.ContentTypeDetail;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * CD-01 rename requires a held design-session lock and persists a unique new name without
 * releasing the lock. GET by old name is not found; GET by id returns the new name.
 */
@Tag("UnitTest")
class ContentTypeAdaptorRenameTest {

  private IPSContentDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private PSItemDefManager itemDefManager;
  private ContentTypeAdaptor adaptor;
  private IPSGuid guid;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    itemDefManager = mock(PSItemDefManager.class);
    adaptor = new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> true);
    guid = new PSGuid(PSTypeEnum.NODEDEF, 311L);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void rename_persistsNameWhenLockHeld() throws Exception {
    stubHeldLock();
    stubCatalog("percPage", 311);
    PSItemDefinition def = stubDefinition("percPage");

    ContentTypeDetail out = adaptor.renameContentType(null, "311", "percRenamedPage");

    assertEquals("percRenamedPage", out.getName());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSItemDefinition>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveContentTypes(saved.capture(), eq(false), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    verify(def).setName("percRenamedPage");
    verify(systemDesign).isLocked(anyList(), eq("Admin"));
  }

  @Test
  void get_oldName404_idReturnsNewName() throws Exception {
    stubHeldLock();
    stubCatalog("percPage", 311);
    stubDefinition("percPage");

    ContentTypeDetail renamed = adaptor.renameContentType(null, "311", "percRenamedPage");
    assertEquals("percRenamedPage", renamed.getName());

    when(itemDefManager.getItemDef("percPage", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new PSInvalidContentTypeException("percPage"));

    assertNull(adaptor.getContentType(null, "percPage"));
    ContentTypeDetail byId = adaptor.getContentType(null, "311");
    assertEquals("percRenamedPage", byId.getName());
  }

  @Test
  void rename_conflictWhenLockNotHeld() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(Collections.singletonList(null));
    stubCatalog("percPage", 311);
    stubDefinition("percPage");

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.renameContentType(null, "311", "percRenamedPage"));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).loadContentTypes(anyList(), eq(true), anyBoolean(), any(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void rename_conflictWhenLockedByOtherUser() throws Exception {
    PSObjectSummary other = new PSObjectSummary(guid, "percPage");
    other.setLockedInfo("other-session", "editor2", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(other));
    stubCatalog("percPage", 311);
    stubDefinition("percPage");

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.renameContentType(null, "311", "percRenamedPage"));
    assertTrue(ex.getMessage().contains("editor2"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void rename_conflictWhenLockedInAnotherSession() throws Exception {
    stubHeldLock();
    stubCatalog("percPage", 311);
    stubDefinition("percPage");
    PSErrorResultsException errors = new PSErrorResultsException();
    errors.addError(
        guid, new PSLockErrorException(1, "LOCK_EXTENSION_INVALID_SESSION", "stack", "Admin", 12));
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenThrow(errors);

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.renameContentType(null, "311", "percRenamedPage"));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void rename_spaces_isBadRequest() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.renameContentType(null, "311", "perc Renamed"));
    assertTrue(ex.getMessage().toLowerCase().contains("space"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void rename_collision_isBadRequest() throws Exception {
    stubCatalog("percPage", 311, "percEventAsset", 329);
    stubDefinition("percPage");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.renameContentType(null, "311", "percEventAsset"));
    assertTrue(ex.getMessage().toLowerCase().contains("exists"), ex.getMessage());
    verify(systemDesign, never()).isLocked(anyList(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void rename_caseInsensitiveCollision_isBadRequest() throws Exception {
    stubCatalog("percPage", 311, "percEventAsset", 329);
    stubDefinition("percPage");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.renameContentType(null, "311", "PERCEVENTASSET"));
    assertTrue(ex.getMessage().toLowerCase().contains("exists"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void rename_sameName_isIdempotentWhenLockHeld() throws Exception {
    stubHeldLock();
    stubCatalog("percPage", 311);
    stubDefinition("percPage");

    ContentTypeDetail out = adaptor.renameContentType(null, "311", "percPage");

    assertEquals("percPage", out.getName());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void rename_unknownName_returnsNull() throws Exception {
    when(itemDefManager.getItemDef("missing", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new PSInvalidContentTypeException("missing"));
    assertNull(adaptor.renameContentType(null, "missing", "percRenamed"));
    verify(systemDesign, never()).isLocked(anyList(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void rename_forbiddenWhenNotAdmin() {
    ContentTypeAdaptor denied =
        new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.renameContentType(null, "311", "percRenamed"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void rename_wildcardName_isBadRequest() {
    assertThrows(
        IllegalArgumentException.class, () -> adaptor.renameContentType(null, "perc*", "percNew"));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void rename_blankIdOrName_isBadRequestBeforeSessionCheck() throws Exception {
    PSRequestInfoBase.resetRequestInfo();
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    IllegalArgumentException blank =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.renameContentType(null, "  ", "percRenamed"));
    assertTrue(blank.getMessage().contains("idOrName"), blank.getMessage());
    IllegalArgumentException missing =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.renameContentType(null, null, "percRenamed"));
    assertTrue(missing.getMessage().contains("idOrName"), missing.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
    verify(systemDesign, never()).isLocked(anyList(), any());
  }

  @Test
  void validateNewContentTypeName_rejectsBlankAndInvalid() {
    assertThrows(
        IllegalArgumentException.class, () -> ContentTypeAdaptor.validateNewContentTypeName(""));
    assertThrows(
        IllegalArgumentException.class,
        () -> ContentTypeAdaptor.validateNewContentTypeName("perc New"));
    assertThrows(
        IllegalArgumentException.class,
        () -> ContentTypeAdaptor.validateNewContentTypeName("perc*"));
    assertEquals("percRenamed", ContentTypeAdaptor.validateNewContentTypeName("percRenamed"));
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary held = new PSObjectSummary(guid, "percPage");
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }

  private void stubCatalog(String name, int typeId) {
    stubCatalog(name, typeId, null, -1);
  }

  private void stubCatalog(String name, int typeId, String otherName, int otherTypeId) {
    PSObjectSummary self = new PSObjectSummary(new PSGuid(PSTypeEnum.NODEDEF, typeId), name);
    if (otherName != null) {
      PSObjectSummary other =
          new PSObjectSummary(new PSGuid(PSTypeEnum.NODEDEF, otherTypeId), otherName);
      when(designWs.findContentTypes("*")).thenReturn(List.of(self, other));
    } else {
      when(designWs.findContentTypes("*")).thenReturn(List.of(self));
    }
  }

  private PSItemDefinition stubDefinition(String currentName) throws Exception {
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn(currentName);
    when(def.getLabel()).thenReturn("Page");
    when(def.getDescription()).thenReturn("desc");
    when(def.isEnabled()).thenReturn(true);
    when(def.isHidden()).thenReturn(false);
    when(def.getAppName()).thenReturn("rx_cePage");
    when(def.getEditorUrl()).thenReturn("/Rhythmyx/rx_cePage/percPage.html");
    when(def.getTypeId()).thenReturn(311);
    when(def.getFieldSet()).thenReturn(null);
    when(def.getContentEditor()).thenReturn(null);
    doAnswer(
            inv -> {
              when(def.getName()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(def)
        .setName(any());
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));
    return def;
  }
}
