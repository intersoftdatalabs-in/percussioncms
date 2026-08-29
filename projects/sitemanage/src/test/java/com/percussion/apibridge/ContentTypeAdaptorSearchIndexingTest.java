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
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.rest.contenttypes.ContentTypeDesignLockException;
import com.percussion.rest.contenttypes.ContentTypeSearchIndexing;
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
 * CD-10 type-level search indexing requires a held design-session lock on PUT and persists root
 * field-set {@code isUserSearchable} without releasing the lock.
 */
@Tag("UnitTest")
class ContentTypeAdaptorSearchIndexingTest {

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
  void disable_persistsRootFieldSetWhenLockHeld() throws Exception {
    stubHeldLock();
    PSFieldSet fieldSet = stubDefinition(true);

    ContentTypeSearchIndexing out = adaptor.setContentTypeSearchIndexing(null, "311", false);

    assertEquals(Boolean.FALSE, out.getSearchIndexing());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSItemDefinition>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveContentTypes(saved.capture(), eq(false), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    verify(fieldSet).setUserSearchable(false);
    verify(systemDesign).isLocked(anyList(), eq("Admin"));
  }

  @Test
  void enable_persistsRootFieldSetWhenLockHeld() throws Exception {
    stubHeldLock();
    PSFieldSet fieldSet = stubDefinition(false);

    ContentTypeSearchIndexing out = adaptor.setContentTypeSearchIndexing(null, "311", true);

    assertEquals(Boolean.TRUE, out.getSearchIndexing());
    verify(fieldSet).setUserSearchable(true);
    verify(designWs).saveContentTypes(anyList(), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void get_roundTripsAfterDisable() throws Exception {
    stubHeldLock();
    stubDefinition(true);

    ContentTypeSearchIndexing disabled = adaptor.setContentTypeSearchIndexing(null, "311", false);
    assertEquals(Boolean.FALSE, disabled.getSearchIndexing());

    ContentTypeSearchIndexing get = adaptor.getContentTypeSearchIndexing(null, "311");
    assertEquals(Boolean.FALSE, get.getSearchIndexing());
  }

  @Test
  void get_defaultsOnWhenFieldSetNull() throws Exception {
    PSItemDefinition def = stubStoreDefinition(true);
    when(def.getFieldSet()).thenReturn(null);
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);

    ContentTypeSearchIndexing get = adaptor.getContentTypeSearchIndexing(null, "311");

    assertEquals(Boolean.TRUE, get.getSearchIndexing());
    verify(systemDesign, never()).isLocked(anyList(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void disable_conflictWhenLockNotHeld() throws Exception {
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(Collections.singletonList(null));
    stubDefinition(true);

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.setContentTypeSearchIndexing(null, "311", false));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).loadContentTypes(anyList(), eq(true), anyBoolean(), any(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void disable_conflictWhenLockedByOtherUser() throws Exception {
    PSObjectSummary other = new PSObjectSummary(guid, "percPage");
    other.setLockedInfo("other-session", "editor2", 12);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(other));
    stubDefinition(true);

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.setContentTypeSearchIndexing(null, "311", false));
    assertTrue(ex.getMessage().contains("editor2"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void disable_conflictWhenLockedInAnotherSession() throws Exception {
    stubHeldLock();
    stubDefinition(true);
    PSErrorResultsException errors = new PSErrorResultsException();
    errors.addError(
        guid, new PSLockErrorException(1, "LOCK_EXTENSION_INVALID_SESSION", "stack", "Admin", 12));
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenThrow(errors);

    ContentTypeDesignLockException ex =
        assertThrows(
            ContentTypeDesignLockException.class,
            () -> adaptor.setContentTypeSearchIndexing(null, "311", false));
    assertTrue(ex.getMessage().toLowerCase().contains("lock"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void disable_unknownName_returnsNull() throws Exception {
    when(itemDefManager.getItemDef("missing", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new PSInvalidContentTypeException("missing"));
    assertNull(adaptor.setContentTypeSearchIndexing(null, "missing", false));
    verify(systemDesign, never()).isLocked(anyList(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void get_unknownName_returnsNull() throws Exception {
    when(itemDefManager.getItemDef("missing", PSItemDefManager.COMMUNITY_ANY))
        .thenThrow(new PSInvalidContentTypeException("missing"));
    assertNull(adaptor.getContentTypeSearchIndexing(null, "missing"));
  }

  @Test
  void disable_forbiddenWhenNotAdmin() {
    ContentTypeAdaptor denied =
        new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> denied.setContentTypeSearchIndexing(null, "311", false));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void disable_missingFieldSet_isServerError() throws Exception {
    stubHeldLock();
    PSItemDefinition def = stubStoreDefinition(true);
    when(def.getFieldSet()).thenReturn(null);
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> adaptor.setContentTypeSearchIndexing(null, "311", false));
    assertTrue(ex.getMessage().toLowerCase().contains("field-set"), ex.getMessage());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void disable_wildcardName_isBadRequest() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.setContentTypeSearchIndexing(null, "perc*", false));
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void get_cacheMiss_usesObjectStoreFallback() throws Exception {
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY)))
        .thenThrow(new PSInvalidContentTypeException("311"));
    PSItemDefinition storeDef = stubStoreDefinition(false);
    ContentTypeAdaptor spy = spy(adaptor);
    doReturn(storeDef).when(spy).loadItemDefFromObjectStore("311");

    ContentTypeSearchIndexing get = spy.getContentTypeSearchIndexing(null, "311");

    assertEquals(Boolean.FALSE, get.getSearchIndexing());
    verify(spy).loadItemDefFromObjectStore("311");
  }

  @Test
  void enable_cacheMiss_usesObjectStoreFallback() throws Exception {
    stubHeldLock();
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY)))
        .thenThrow(new PSInvalidContentTypeException("311"));
    PSItemDefinition storeDef = stubStoreDefinition(false);
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(storeDef));
    ContentTypeAdaptor spy = spy(adaptor);
    doReturn(storeDef).when(spy).loadItemDefFromObjectStore("311");

    ContentTypeSearchIndexing out = spy.setContentTypeSearchIndexing(null, "311", true);

    assertEquals(Boolean.TRUE, out.getSearchIndexing());
    verify(spy).loadItemDefFromObjectStore("311");
    verify(storeDef.getFieldSet()).setUserSearchable(true);
  }

  private void stubHeldLock() throws Exception {
    PSObjectSummary held = new PSObjectSummary(guid, "percPage");
    held.setLockedInfo("test-session", "Admin", 30);
    when(systemDesign.isLocked(anyList(), eq("Admin"))).thenReturn(List.of(held));
  }

  private PSFieldSet stubDefinition(boolean initiallySearchable) throws Exception {
    PSItemDefinition def = stubStoreDefinition(initiallySearchable);
    when(itemDefManager.getItemDef(eq(311L), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);
    when(designWs.loadContentTypes(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(def));
    return def.getFieldSet();
  }

  private PSItemDefinition stubStoreDefinition(boolean initiallySearchable) {
    PSItemDefinition def = mock(PSItemDefinition.class);
    when(def.getName()).thenReturn("percPage");
    when(def.getLabel()).thenReturn("Page");
    when(def.getDescription()).thenReturn("desc");
    when(def.isEnabled()).thenReturn(true);
    when(def.isHidden()).thenReturn(false);
    when(def.getAppName()).thenReturn("rx_cePage");
    when(def.getEditorUrl()).thenReturn("/Rhythmyx/rx_cePage/percPage.html");
    when(def.getTypeId()).thenReturn(311);
    when(def.getContentEditor()).thenReturn(null);
    PSFieldSet fieldSet = mock(PSFieldSet.class);
    when(fieldSet.isUserSearchable()).thenReturn(initiallySearchable);
    doAnswer(
            inv -> {
              when(fieldSet.isUserSearchable()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(fieldSet)
        .setUserSearchable(anyBoolean());
    when(def.getFieldSet()).thenReturn(fieldSet);
    return def;
  }
}
