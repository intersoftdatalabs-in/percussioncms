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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.IPSDbComponent;
import com.percussion.cms.objectstore.PSDbComponent;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.cms.objectstore.PSKey;
import com.percussion.rest.Guid;
import com.percussion.rest.displayformat.DisplayFormat;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.ui.IPSUiDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * UI-05 POST create / PUT update / DELETE persist via {@code createDisplayFormats}/{@code
 * saveDisplayFormats} and XML component delete (not locator {@code deleteDisplayFormats}). Admin
 * only; unique name; no lock steal.
 */
@Tag("UnitTest")
class DisplayFormatAdaptorWriteTest {

  private IPSUiDesignWs designWs;
  private DisplayFormatAdaptor adaptor;
  private IPSGuid guid;
  private List<PSDisplayFormat> xmlDeleted;

  @BeforeEach
  void setUp() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    designWs = mock(IPSUiDesignWs.class);
    xmlDeleted = new ArrayList<>();
    adaptor =
        new DisplayFormatAdaptor(
            designWs,
            () -> true,
            (df, id, session, user) -> xmlDeleted.add(df));
    guid = new PSGuid(PSTypeEnum.DISPLAY_FORMAT, 42L);
    when(designWs.findDisplayFormats(any(), nullable(String.class)))
        .thenReturn(Collections.emptyList());
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void create_usesCreateThenSaveAndReleasesLock() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.createDisplayFormats(eq(List.of("MyFmt")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(nativeDf));
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);

    DisplayFormat body = new DisplayFormat();
    body.setName("MyFmt");
    body.setLabel("My Format");
    body.setDescription("created via REST");

    DisplayFormat out = adaptor.createDisplayFormat(body);

    assertEquals("MyFmt", out.getName());
    assertEquals("My Format", out.getLabel());
    assertEquals("created via REST", out.getDescription());
    verify(designWs).createDisplayFormats(eq(List.of("MyFmt")), eq("test-session"), eq("Admin"));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSDisplayFormat>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs)
        .saveDisplayFormats(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    assertEquals("My Format", saved.getValue().get(0).getDisplayName());
    assertEquals("created via REST", saved.getValue().get(0).getDescription());
  }

  @Test
  void create_duplicateName_is409BeforeCreate() throws Exception {
    IPSCatalogSummary existing = mock(IPSCatalogSummary.class);
    when(existing.getName()).thenReturn("MyFmt");
    when(designWs.findDisplayFormats(eq("MyFmt"), nullable(String.class)))
        .thenReturn(List.of(existing));

    DisplayFormat body = new DisplayFormat();
    body.setName("MyFmt");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createDisplayFormat(body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("already exists"));
    verify(designWs, never()).createDisplayFormats(anyList(), any(), any());
    verify(designWs, never()).saveDisplayFormats(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_persistTimeDuplicate_is409() throws Exception {
    when(designWs.createDisplayFormats(eq(List.of("MyFmt")), eq("test-session"), eq("Admin")))
        .thenThrow(
            new IllegalArgumentException(
                "The name 'MyFmt' for type 'DISPLAY_FORMAT' already exists."));
    DisplayFormat body = new DisplayFormat();
    body.setName("MyFmt");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createDisplayFormat(body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveDisplayFormats(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_blankName_throwsBeforeDesignWs() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.createDisplayFormat(null));
    DisplayFormat blank = new DisplayFormat();
    blank.setName("  ");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createDisplayFormat(blank));
    assertTrue(ex.getMessage().contains("name is required"));
    verify(designWs, never()).createDisplayFormats(anyList(), any(), any());
  }

  @Test
  void create_nameWithSpaces_throwsBeforeDesignWs() {
    DisplayFormat body = new DisplayFormat();
    body.setName("has space");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createDisplayFormat(body));
    assertEquals("name cannot contain whitespace", ex.getMessage());
    verify(designWs, never()).createDisplayFormats(anyList(), any(), any());
  }

  @Test
  void create_wildcardName_throwsBeforeDesignWs() {
    DisplayFormat body = new DisplayFormat();
    body.setName("My*Fmt");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createDisplayFormat(body));
    assertEquals("name must not contain wildcards", ex.getMessage());
    verify(designWs, never()).createDisplayFormats(anyList(), any(), any());
  }

  @Test
  void create_nonAdmin_is403() {
    adaptor = new DisplayFormatAdaptor(designWs, () -> false);
    DisplayFormat body = new DisplayFormat();
    body.setName("MyFmt");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createDisplayFormat(body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).createDisplayFormats(anyList(), any(), any());
  }

  @Test
  void create_missingSession_is403() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    DisplayFormat body = new DisplayFormat();
    body.setName("MyFmt");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createDisplayFormat(body));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("session"), ex.getMessage());
  }

  @Test
  void create_thenGetByName_returnsFormat() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    nativeDf.setDisplayName("My Format");
    nativeDf.setDescription("created via REST");
    when(designWs.createDisplayFormats(eq(List.of("MyFmt")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(nativeDf));
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);

    DisplayFormat body = new DisplayFormat();
    body.setName("MyFmt");
    body.setLabel("My Format");
    body.setDescription("created via REST");

    adaptor.createDisplayFormat(body);
    DisplayFormat got = adaptor.findDisplayFormatByKey("MyFmt");

    assertEquals("MyFmt", got.getName());
    assertEquals("My Format", got.getLabel());
    assertEquals("created via REST", got.getDescription());
  }

  @Test
  void update_loadsWithLockAndSavesLabelDescription() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));

    DisplayFormat body = new DisplayFormat();
    body.setLabel("Updated");
    body.setDescription("updated desc");

    DisplayFormat out = adaptor.updateDisplayFormat("MyFmt", body);

    assertEquals("MyFmt", out.getName());
    assertEquals("Updated", out.getLabel());
    assertEquals("updated desc", out.getDescription());
    verify(designWs)
        .loadDisplayFormats(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin"));
    verify(designWs).saveDisplayFormats(anyList(), eq(true), eq("test-session"), eq("Admin"));
    verify(designWs, never()).createDisplayFormats(anyList(), any(), any());
  }

  @Test
  void update_unknown_returnsNull() {
    when(designWs.findDisplayFormat(eq("missing"))).thenReturn(null);
    DisplayFormat body = new DisplayFormat();
    body.setLabel("Updated");
    assertNull(adaptor.updateDisplayFormat("missing", body));
    verify(designWs, never()).saveDisplayFormats(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_lockConflict_is409() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenThrow(lockResultsException());

    DisplayFormat body = new DisplayFormat();
    body.setLabel("Updated");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.updateDisplayFormat("MyFmt", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveDisplayFormats(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_nonAdmin_is403() {
    adaptor = new DisplayFormatAdaptor(designWs, () -> false);
    DisplayFormat body = new DisplayFormat();
    body.setLabel("Updated");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.updateDisplayFormat("MyFmt", body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void nativeForDelete_rejectsReplayUnpersistedLoad() throws Exception {
    PSDisplayFormat replayed = new PSDisplayFormat();
    replayed.setName("By_Type");
    replayed.setInternalName("By_Type");
    assertTrue(replayed.getDisplayId() <= 0);
    DisplayFormat existing = new DisplayFormat();
    existing.setName("MyFmt");
    existing.setDisplayId(42);
    PSDisplayFormat out = DisplayFormatAdaptor.nativeForDelete(replayed, existing, "MyFmt");
    assertEquals("MyFmt", out.getName());
    assertEquals(42, out.getDisplayId());
    assertTrue(out.getDisplayId() > 0);
  }

  @Test
  void nativeForDelete_keepsMatchingPersistedLoad() throws Exception {
    PSDisplayFormat loaded = nativeDisplayFormat(42, "MyFmt");
    DisplayFormat existing = new DisplayFormat();
    existing.setName("MyFmt");
    existing.setDisplayId(42);
    PSDisplayFormat out = DisplayFormatAdaptor.nativeForDelete(loaded, existing, "MyFmt");
    assertEquals(42, out.getDisplayId());
    assertEquals("MyFmt", out.getName());
  }

  @Test
  void ensureMarkedForDeletion_setsMarkedStateOnPersistedKey() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    DisplayFormatAdaptor.ensureMarkedForDeletion(nativeDf);
    assertEquals(IPSDbComponent.DBSTATE_MARKEDFORDELETE, nativeDf.getState());
  }

  @Test
  void delete_thenGetByName_isNotFound() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));

    assertTrue(adaptor.deleteDisplayFormat("MyFmt"));
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(null);
    assertNull(adaptor.findDisplayFormatByKey("MyFmt"));
    assertEquals(1, xmlDeleted.size());
    assertEquals("MyFmt", xmlDeleted.get(0).getName());
    assertEquals(IPSDbComponent.DBSTATE_MARKEDFORDELETE, xmlDeleted.get(0).getState());
    verify(designWs, never()).deleteDisplayFormats(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).saveDisplayFormats(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_unknown_returnsFalse() throws Exception {
    when(designWs.findDisplayFormat(eq("missing"))).thenReturn(null);
    assertFalse(adaptor.deleteDisplayFormat("missing"));
    assertTrue(xmlDeleted.isEmpty());
    verify(designWs, never()).deleteDisplayFormats(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadDisplayFormats(anyList(), anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void delete_lockConflict_is409() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenThrow(new PSErrorResultsException());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteDisplayFormat("MyFmt"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(xmlDeleted.isEmpty());
    verify(designWs, never()).deleteDisplayFormats(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_inUse_is409() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));
    adaptor =
        new DisplayFormatAdaptor(
            designWs,
            () -> true,
            (df, id, session, user) -> {
              throw new WebApplicationException(
                  "Display format has dependents and cannot be deleted", 409);
            });

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteDisplayFormat("MyFmt"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("depend"), ex.getMessage());
    verify(designWs, never()).deleteDisplayFormats(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_xmlPersistFailure_is500() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));
    adaptor =
        new DisplayFormatAdaptor(
            designWs,
            () -> true,
            (df, id, session, user) -> {
              throw new PSCmsException(0, "Xml Document Expected, none supplied");
            });

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.deleteDisplayFormat("MyFmt"));
    assertTrue(ex.getMessage().toLowerCase().contains("delete"), ex.getMessage());
    verify(designWs, never()).deleteDisplayFormats(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_nonAdmin_is403() throws Exception {
    adaptor = new DisplayFormatAdaptor(designWs, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteDisplayFormat("MyFmt"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).deleteDisplayFormats(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadDisplayFormats(anyList(), anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void isLockError_detectsTypedLockErrorException() {
    PSErrorsException errors = new PSErrorsException();
    errors.addError(guid, new PSLockErrorException(0, "lock failed", "stack"));
    assertTrue(DisplayFormatAdaptor.isLockError(errors));
    PSErrorsException unrelated = new PSErrorsException();
    unrelated.addError(guid, new PSErrorException("unrelated failure"));
    assertFalse(DisplayFormatAdaptor.isLockError(unrelated));
    assertFalse(DisplayFormatAdaptor.isLockError(null));
  }

  @Test
  void toIpsGuid_parsesStringValue() {
    Guid g = restGuid(guid);
    IPSGuid parsed = DisplayFormatAdaptor.toIpsGuid(g);
    assertEquals(guid.toString(), parsed.toString());
    assertNull(DisplayFormatAdaptor.toIpsGuid(null));
  }

  private PSErrorResultsException lockResultsException() {
    PSErrorResultsException e = new PSErrorResultsException();
    e.addError(guid, new PSErrorException("Object is locked by another user"));
    return e;
  }

  private static PSDisplayFormat nativeDisplayFormat(int displayId, String name) throws Exception {
    PSDisplayFormat nativeDf = new PSDisplayFormat();
    PSKey key = PSDisplayFormat.createKey(new String[] {String.valueOf(displayId)});
    Method setKey = PSDbComponent.class.getDeclaredMethod("setKey", PSKey.class);
    setKey.setAccessible(true);
    setKey.invoke(nativeDf, key);
    nativeDf.setName(name);
    nativeDf.setInternalName(name);
    return nativeDf;
  }

  private static Guid restGuid(IPSGuid g) {
    Guid out = new Guid();
    out.setStringValue(g.toString());
    return out;
  }
}
