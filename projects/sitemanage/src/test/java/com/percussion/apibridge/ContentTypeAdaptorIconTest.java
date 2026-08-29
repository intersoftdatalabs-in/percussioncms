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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.rest.contenttypes.ContentTypeIcon;
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
 * CD-11 icon strategy requires a held design-session lock on PUT and persists via {@code
 * PSContentEditor.setContentTypeIcon}.
 */
@Tag("UnitTest")
class ContentTypeAdaptorIconTest {

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
  void specified_persistsFileWhenLockHeld() throws Exception {
    stubHeldLock();
    PSContentEditor editor = stubDefinition(PSContentEditor.ICON_SOURCE_NONE, null);

    ContentTypeIcon out =
        adaptor.setContentTypeIcon(
            null, "311", ContentTypeIcon.SOURCE_SPECIFIED, "rx_resources/images/page.gif");

    assertEquals(ContentTypeIcon.SOURCE_SPECIFIED, out.getSource());
    assertEquals("rx_resources/images/page.gif", out.getValue());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSItemDefinition>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveContentTypes(saved.capture(), eq(false), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    verify(editor)
        .setContentTypeIcon(
            PSContentEditor.ICON_SOURCE_SPECIFIED, "rx_resources/images/page.gif");
  }

  @Test
  void fromFileField_persistsFieldNameWhenLockHeld() throws Exception {
    stubHeldLock();
    PSContentEditor editor = stubDefinition(PSContentEditor.ICON_SOURCE_NONE, null);

    ContentTypeIcon out =
        adaptor.setContentTypeIcon(
            null, "311", ContentTypeIcon.SOURCE_FROM_FILE_FIELD, "item_file_attachment");

    assertEquals(ContentTypeIcon.SOURCE_FROM_FILE_FIELD, out.getSource());
    assertEquals("item_file_attachment", out.getValue());
    verify(editor)
        .setContentTypeIcon(PSContentEditor.ICON_SOURCE_FROMFILEEXT, "item_file_attachment");
  }

  @Test
  void none_clearsValueWhenLockHeld() throws Exception {
    stubHeldLock();
    PSContentEditor editor =
        stubDefinition(PSContentEditor.ICON_SOURCE_SPECIFIED, "rx_resources/images/page.gif");

    ContentTypeIcon out =
        adaptor.setContentTypeIcon(null, "311", ContentTypeIcon.SOURCE_NONE, "ignored.gif");

    assertEquals(ContentTypeIcon.SOURCE_NONE, out.getSource());
    assertNull(out.getValue());
    verify(editor).setContentTypeIcon(PSContentEditor.ICON_SOURCE_NONE, "");
  }

  @Test
  void get_reflectsIconAfterSpecifiedPut() throws Exception {
    stubHeldLock();
    stubDefinition(PSContentEditor.ICON_SOURCE_NONE, null);

    ContentTypeIcon saved =
        adaptor.setContentTypeIcon(
            null, "311", ContentTypeIcon.SOURCE_SPECIFIED, "rx_resources/images/page.gif");
    assertEquals(ContentTypeIcon.SOURCE_SPECIFIED, saved.getSource());

    ContentTypeIcon get = adaptor.getContentTypeIcon(null, "311");
    assertEquals(ContentTypeIcon.SOURCE_SPECIFIED, get.getSource());
    assertEquals("rx_resources/images/page.gif", get.getValue());
  }

  @Test
  void put_conflictWhenLockNotHeld() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(Collections.singletonList(null));
    stubDefinition(PSContentEditor.ICON_SOURCE_NONE, null);

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () ->
                adaptor.setContentTypeIcon(
                    null, "311", ContentTypeIcon.SOURCE_SPECIFIED, "page.gif"));
    assertTrue(
        ex.getMessage().startsWith("Could not set content type icon"), ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).loadContentTypes(anyList(), eq(true), anyBoolean(), any(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_conflictWhenLockedByOtherUser() throws Exception {
    PSObjectSummary other = new PSObjectSummary(guid, "percPage");
    other.setLockedInfo("other-session", "editor2", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(other));
    stubDefinition(PSContentEditor.ICON_SOURCE_NONE, null);

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () ->
                adaptor.setContentTypeIcon(
                    null, "311", ContentTypeIcon.SOURCE_SPECIFIED, "page.gif"));
    assertTrue(
        ex.getMessage().startsWith("Could not set content type icon"), ex.getMessage());
    assertTrue(ex.getMessage().contains("editor2"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_conflictWhenLockedInAnotherSession() throws Exception {
    stubHeldLock();
    stubDefinition(PSContentEditor.ICON_SOURCE_NONE, null);
    PSErrorResultsException errors = new PSErrorResultsException();
    errors.addError(
        guid, new PSLockErrorException(1, "LOCK_EXTENSION_INVALID_SESSION", "stack", "Admin", 12));
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenThrow(errors);

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () ->
                adaptor.setContentTypeIcon(
                    null, "311", ContentTypeIcon.SOURCE_SPECIFIED, "page.gif"));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_unknownName_returnsNull() throws Exception {
    when(itemDefManager.getItemDef("missing", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new PSInvalidContentTypeException("missing"));
    assertNull(adaptor.setContentTypeIcon(null, "missing", ContentTypeIcon.SOURCE_NONE, null));
    verify(systemDesign, never()).isLocked(anyList(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void get_unknownName_returnsNull() throws Exception {
    when(itemDefManager.getItemDef("missing", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new PSInvalidContentTypeException("missing"));
    assertNull(adaptor.getContentTypeIcon(null, "missing"));
  }

  @Test
  void put_forbiddenWhenNotAdmin() {
    ContentTypeAdaptor denied =
        new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                denied.setContentTypeIcon(
                    null, "311", ContentTypeIcon.SOURCE_SPECIFIED, "page.gif"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void put_invalidSource_isBadRequest() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.setContentTypeIcon(null, "311", "unknown", "page.gif"));
    assertTrue(ex.getMessage().toLowerCase().contains("source"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_blankValueForSpecified_isBadRequest() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.setContentTypeIcon(
                    null, "311", ContentTypeIcon.SOURCE_SPECIFIED, "  "));
    assertTrue(ex.getMessage().toLowerCase().contains("value"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_wildcardName_isBadRequest() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.setContentTypeIcon(null, "perc*", ContentTypeIcon.SOURCE_NONE, null));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void get_missingEditor_isNone() throws Exception {
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getContentEditor()).thenReturn(null);
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);

    ContentTypeIcon out = adaptor.getContentTypeIcon(null, "311");
    assertEquals(ContentTypeIcon.SOURCE_NONE, out.getSource());
    assertNull(out.getValue());
  }

  @Test
  void get_cacheMiss_usesObjectStoreFallback() throws Exception {
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY)))
        .thenThrow(new PSInvalidContentTypeException("311"));
    PSItemDefinition storeDef = stubStoreDefinition(PSContentEditor.ICON_SOURCE_NONE, null);
    ContentTypeAdaptor spy = spy(adaptor);
    doReturn(storeDef).when(spy).loadItemDefFromObjectStore("311");

    ContentTypeIcon get = spy.getContentTypeIcon(null, "311");

    assertEquals(ContentTypeIcon.SOURCE_NONE, get.getSource());
    verify(spy).loadItemDefFromObjectStore("311");
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary held = new PSObjectSummary(guid, "percPage");
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }

  private PSContentEditor stubDefinition(String soapSource, String soapValue) throws Exception {
    PSItemDefinition def = stubStoreDefinition(soapSource, soapValue);
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));
    return def.getContentEditor();
  }

  private PSItemDefinition stubStoreDefinition(String soapSource, String soapValue) {
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn("percPage");
    when(def.getLabel()).thenReturn("Page");
    when(def.getDescription()).thenReturn("desc");
    when(def.isEnabled()).thenReturn(true);
    when(def.isHidden()).thenReturn(false);
    when(def.getAppName()).thenReturn("rx_cePage");
    when(def.getEditorUrl()).thenReturn("/Rhythmyx/rx_cePage/percPage.html");
    when(def.getTypeId()).thenReturn(311);
    when(def.getFieldSet()).thenReturn(null);
    PSContentEditor editor = mock(PSContentEditor.class);
    when(editor.getIconSource()).thenReturn(soapSource);
    when(editor.getIconValue()).thenReturn(soapValue);
    doAnswer(
            inv -> {
              when(editor.getIconSource()).thenReturn(inv.getArgument(0));
              String persisted = inv.getArgument(1);
              when(editor.getIconValue())
                  .thenReturn(
                      PSContentEditor.ICON_SOURCE_NONE.equals(inv.getArgument(0))
                          ? null
                          : persisted);
              return null;
            })
        .when(editor)
        .setContentTypeIcon(any(), any());
    when(def.getContentEditor()).thenReturn(editor);
    return def;
  }
}
