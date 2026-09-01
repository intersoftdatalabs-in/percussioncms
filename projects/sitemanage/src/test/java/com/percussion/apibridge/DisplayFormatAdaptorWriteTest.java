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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSDbComponent;
import com.percussion.cms.objectstore.PSDisplayColumn;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.cms.objectstore.PSKey;
import com.percussion.rest.Guid;
import com.percussion.rest.displayformat.DisplayFormat;
import com.percussion.rest.displayformat.DisplayFormatColumn;
import com.percussion.rest.displayformat.DisplayFormatColumnList;
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
 * saveDisplayFormats}/{@code deleteDisplayFormats}. Admin only; unique name; no lock steal.
 */
@Tag("UnitTest")
class DisplayFormatAdaptorWriteTest {

  private IPSUiDesignWs designWs;
  private DisplayFormatAdaptor adaptor;
  private IPSGuid guid;

  @BeforeEach
  void setUp() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    designWs = mock(IPSUiDesignWs.class);
    adaptor = new DisplayFormatAdaptor(designWs, () -> true);
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
  void update_replacesColumnsWhenPresent() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));

    DisplayFormat body = new DisplayFormat();
    DisplayFormatColumnList cols = new DisplayFormatColumnList();
    DisplayFormatColumn title = new DisplayFormatColumn();
    title.setSource("sys_title");
    title.setDisplayName("Title");
    title.setPosition(0);
    DisplayFormatColumn created = new DisplayFormatColumn();
    created.setSource("sys_contentcreatedby");
    created.setDisplayName("Created by");
    created.setPosition(1);
    cols.add(title);
    cols.add(created);
    body.setColumns(cols);

    DisplayFormat out = adaptor.updateDisplayFormat("MyFmt", body);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSDisplayFormat>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveDisplayFormats(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    PSDisplayFormat persisted = saved.getValue().get(0);
    assertEquals(2, persisted.getColumnContainer().size());
    assertEquals("sys_title", ((PSDisplayColumn) persisted.getColumnContainer().get(0)).getSource());
    assertEquals(
        "sys_contentcreatedby",
        ((PSDisplayColumn) persisted.getColumnContainer().get(1)).getSource());
    assertEquals("MyFmt", out.getName());
  }

  @Test
  void update_invalidColumnSource_is400() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));

    DisplayFormat body = new DisplayFormat();
    DisplayFormatColumnList cols = new DisplayFormatColumnList();
    DisplayFormatColumn bad = new DisplayFormatColumn();
    bad.setSource("has space");
    cols.add(bad);
    body.setColumns(cols);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.updateDisplayFormat("MyFmt", body));
    assertTrue(ex.getMessage().contains("whitespace"), ex.getMessage());
    verify(designWs, never()).saveDisplayFormats(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_omittedColumns_leavesExisting() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    nativeDf.setColumnList(nativeDf.getColumnContainer());
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));

    DisplayFormat body = new DisplayFormat();
    body.setLabel("Updated");

    adaptor.updateDisplayFormat("MyFmt", body);
    verify(designWs).saveDisplayFormats(anyList(), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void requireValidColumnSource_rejectsBlankAndPath() {
    IllegalArgumentException blank =
        assertThrows(IllegalArgumentException.class, () -> DisplayFormatAdaptor.requireValidColumnSource("  "));
    assertTrue(blank.getMessage().contains("required"));
    IllegalArgumentException path =
        assertThrows(
            IllegalArgumentException.class,
            () -> DisplayFormatAdaptor.requireValidColumnSource("../sys_title"));
    assertTrue(path.getMessage().contains("invalid"));
    assertEquals("sys_title", DisplayFormatAdaptor.requireValidColumnSource("sys_title"));
  }

  @Test
  void findByName_rejectsByAuthorReplayAndLoadsSummaryGuid() throws Exception {
    PSDisplayFormat replayed = nativeDisplayFormat(5, "By_Author");
    PSDisplayFormat real = nativeDisplayFormat(42, "MyFmt");
    IPSCatalogSummary summary = mock(IPSCatalogSummary.class);
    when(summary.getName()).thenReturn("MyFmt");
    when(summary.getGUID()).thenReturn(guid);
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(replayed);
    when(designWs.findDisplayFormats(eq("MyFmt"), nullable(String.class)))
        .thenReturn(List.of(summary));
    when(designWs.findDisplayFormat(eq(guid))).thenReturn(real);

    DisplayFormat out = adaptor.findDisplayFormatByKey("MyFmt");

    assertEquals("MyFmt", out.getName());
    assertEquals(42, out.getDisplayId());
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
  void delete_thenGetByName_isNotFound() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));

    assertTrue(adaptor.deleteDisplayFormat("MyFmt"));
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(null);
    assertNull(adaptor.findDisplayFormatByKey("MyFmt"));
    verify(designWs)
        .deleteDisplayFormats(anyList(), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void delete_unknown_returnsFalse() {
    when(designWs.findDisplayFormat(eq("missing"))).thenReturn(null);
    assertFalse(adaptor.deleteDisplayFormat("missing"));
    verify(designWs, never()).deleteDisplayFormats(anyList(), anyBoolean(), any(), any());
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
    verify(designWs, never()).deleteDisplayFormats(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_inUse_is409() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));
    PSErrorsException errors = new PSErrorsException();
    errors.addError(guid, new PSErrorException("Object has dependents"));
    doThrow(errors)
        .when(designWs)
        .deleteDisplayFormats(anyList(), eq(false), any(), any());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteDisplayFormat("MyFmt"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("depend"), ex.getMessage());
  }

  @Test
  void delete_nonAdmin_is403() {
    adaptor = new DisplayFormatAdaptor(designWs, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteDisplayFormat("MyFmt"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).deleteDisplayFormats(anyList(), anyBoolean(), any(), any());
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
