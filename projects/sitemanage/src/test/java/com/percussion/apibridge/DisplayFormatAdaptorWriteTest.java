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
import com.percussion.cms.objectstore.PSDisplayColumn;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.cms.objectstore.PSKey;
import com.percussion.rest.Guid;
import com.percussion.rest.displayformat.DisplayFormat;
import com.percussion.rest.displayformat.DisplayFormatCommunity;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * UI-05 POST create / PUT update / DELETE persist via {@code createDisplayFormats}/{@code
 * saveDisplayFormats} and name-keyed {@code deleteDisplayFormats} with a resolved DISPLAYID
 * (JDBC row delete). Admin only; unique name; no lock steal; never an empty id list.
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
  void create_reloadRejectsByAuthorReplay_returnsCreatedName() throws Exception {
    PSDisplayFormat created = nativeDisplayFormat(99, "MyFmt");
    created.setDisplayName("My Format");
    created.setDescription("created via REST");
    PSDisplayFormat byAuthor = nativeDisplayFormat(5, "By_Author");
    byAuthor.setDisplayName("By Author");
    when(designWs.createDisplayFormats(eq(List.of("MyFmt")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(created));
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(byAuthor);
    when(designWs.findDisplayFormat(eq(created.getGUID()))).thenReturn(byAuthor);

    DisplayFormat body = new DisplayFormat();
    body.setName("MyFmt");
    body.setLabel("My Format");
    body.setDescription("created via REST");

    DisplayFormat out = adaptor.createDisplayFormat(body);

    assertEquals("MyFmt", out.getName());
    assertEquals("My Format", out.getLabel());
    assertEquals("created via REST", out.getDescription());
  }

  @Test
  void findByKey_rejectsByAuthorReplay_usesCatalogSummary() throws Exception {
    PSDisplayFormat byAuthor = nativeDisplayFormat(5, "By_Author");
    byAuthor.setDisplayName("By Author");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(byAuthor);
    IPSCatalogSummary summary = mock(IPSCatalogSummary.class);
    when(summary.getName()).thenReturn("MyFmt");
    when(summary.getLabel()).thenReturn("My Format");
    when(summary.getDescription()).thenReturn("from catalog");
    when(summary.getGUID()).thenReturn(guid);
    when(designWs.findDisplayFormats(eq("MyFmt"), nullable(String.class)))
        .thenReturn(List.of(summary));
    when(designWs.findDisplayFormat(eq(guid))).thenReturn(byAuthor);

    DisplayFormat got = adaptor.findDisplayFormatByKey("MyFmt");

    assertEquals("MyFmt", got.getName());
    assertEquals("My Format", got.getLabel());
  }

  @Test
  void identityMatchesKey_requiresNameOrGuid() {
    DisplayFormat df = new DisplayFormat();
    df.setName("MyFmt");
    df.setInternalName("MyFmt");
    df.setGuidString("0-31-99");
    assertTrue(DisplayFormatAdaptor.identityMatchesKey(df, "MyFmt"));
    assertTrue(DisplayFormatAdaptor.identityMatchesKey(df, "0-31-99"));
    assertFalse(DisplayFormatAdaptor.identityMatchesKey(df, "By_Author"));
    assertFalse(DisplayFormatAdaptor.identityMatchesKey(null, "MyFmt"));
    DisplayFormat unnamed = new DisplayFormat();
    unnamed.setGuidString("0-31-301");
    assertFalse(DisplayFormatAdaptor.identityMatchesKey(unnamed, "By_Author"));
    assertTrue(DisplayFormatAdaptor.identityMatchesKey(unnamed, "0-31-301"));
    unnamed.setName("display_format_1");
    assertFalse(DisplayFormatAdaptor.identityMatchesKey(unnamed, "By_Author"));
    assertTrue(DisplayFormatAdaptor.identityMatchesKey(unnamed, "display_format_1"));
  }

  @Test
  void findByGuid_rejectsByAuthorReplay() throws Exception {
    PSDisplayFormat byAuthor = nativeDisplayFormat(5, "By_Author");
    IPSGuid requested = new PSGuid(PSTypeEnum.DISPLAY_FORMAT, 1031L);
    when(designWs.findDisplayFormat(eq(requested))).thenReturn(byAuthor);

    assertNull(adaptor.findDisplayFormat(requested));
    assertNull(adaptor.findDisplayFormatByKey(requested.toString()));
  }

  @Test
  void findByKey_guidUsesCatalogNameWhenLoadReplaysByAuthor() throws Exception {
    IPSGuid requested = new PSGuid(PSTypeEnum.DISPLAY_FORMAT, 1031L);
    PSDisplayFormat replayed = nativeDisplayFormat(1031, "By_Author");
    IPSCatalogSummary summary = mock(IPSCatalogSummary.class);
    when(summary.getName()).thenReturn("MyFmt");
    when(summary.getLabel()).thenReturn("My Format");
    when(summary.getGUID()).thenReturn(requested);
    when(designWs.findDisplayFormats(nullable(String.class), nullable(String.class)))
        .thenReturn(List.of(summary));
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(null);
    when(designWs.findDisplayFormat(eq(requested))).thenReturn(replayed);

    DisplayFormat got = adaptor.findDisplayFormatByKey(requested.toString());

    assertEquals("MyFmt", got.getName());
    assertEquals("My Format", got.getLabel());
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
  void update_allowedCommunities_restrictsToKnownCommunity() throws Exception {
    IPSGuid communityGuid = new PSGuid(PSTypeEnum.COMMUNITY_DEF, 1001L);
    Map<IPSGuid, String> catalog = new HashMap<>();
    catalog.put(communityGuid, "Default");
    adaptor =
        new DisplayFormatAdaptor(
            designWs, () -> true, (df, id, session, user) -> xmlDeleted.add(df), () -> catalog);
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));

    DisplayFormat body = new DisplayFormat();
    body.setAllowedCommunities(
        List.of(new DisplayFormatCommunity(communityGuid.toString(), "Default")));

    DisplayFormat out = adaptor.updateDisplayFormat("MyFmt", body);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSDisplayFormat>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveDisplayFormats(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    PSDisplayFormat persisted = saved.getValue().get(0);
    assertTrue(
        persisted.doesPropertyHaveValue(
            PSDisplayFormat.PROP_COMMUNITY, String.valueOf(communityGuid.longValue())));
    assertFalse(
        persisted.doesPropertyHaveValue(
            PSDisplayFormat.PROP_COMMUNITY, PSDisplayFormat.PROP_COMMUNITY_ALL));
    assertEquals(1, out.getAllowedCommunities().size());
    assertEquals("Default", out.getAllowedCommunities().get(0).getName());
  }

  @Test
  void update_allCommunitiesSentinelRow_isAllCommunities() throws Exception {
    IPSGuid communityGuid = new PSGuid(PSTypeEnum.COMMUNITY_DEF, 1001L);
    Map<IPSGuid, String> catalog = new HashMap<>();
    catalog.put(communityGuid, "Default");
    adaptor =
        new DisplayFormatAdaptor(
            designWs, () -> true, (df, id, session, user) -> xmlDeleted.add(df), () -> catalog);
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    nativeDf.addCommunity(String.valueOf(communityGuid.longValue()));
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));

    DisplayFormat body = new DisplayFormat();
    body.setAllowedCommunities(
        List.of(new DisplayFormatCommunity(PSDisplayFormat.PROP_COMMUNITY_ALL, "-1")));

    adaptor.updateDisplayFormat("MyFmt", body);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSDisplayFormat>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveDisplayFormats(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    PSDisplayFormat persisted = saved.getValue().get(0);
    assertTrue(
        persisted.doesPropertyHaveValue(
            PSDisplayFormat.PROP_COMMUNITY, PSDisplayFormat.PROP_COMMUNITY_ALL));
  }

  @Test
  void update_emptyAllowedCommunities_isAllCommunities() throws Exception {
    IPSGuid communityGuid = new PSGuid(PSTypeEnum.COMMUNITY_DEF, 1001L);
    Map<IPSGuid, String> catalog = new HashMap<>();
    catalog.put(communityGuid, "Default");
    adaptor =
        new DisplayFormatAdaptor(
            designWs, () -> true, (df, id, session, user) -> xmlDeleted.add(df), () -> catalog);
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    nativeDf.addCommunity(String.valueOf(communityGuid.longValue()));
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));

    DisplayFormat body = new DisplayFormat();
    body.setAllowedCommunities(new ArrayList<>());

    adaptor.updateDisplayFormat("MyFmt", body);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSDisplayFormat>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveDisplayFormats(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    PSDisplayFormat persisted = saved.getValue().get(0);
    assertTrue(
        persisted.doesPropertyHaveValue(
            PSDisplayFormat.PROP_COMMUNITY, PSDisplayFormat.PROP_COMMUNITY_ALL));
  }

  @Test
  void update_unknownCommunity_is400() throws Exception {
    IPSGuid communityGuid = new PSGuid(PSTypeEnum.COMMUNITY_DEF, 1001L);
    Map<IPSGuid, String> catalog = new HashMap<>();
    catalog.put(communityGuid, "Default");
    adaptor =
        new DisplayFormatAdaptor(
            designWs, () -> true, (df, id, session, user) -> xmlDeleted.add(df), () -> catalog);
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));

    DisplayFormat body = new DisplayFormat();
    body.setAllowedCommunities(List.of(new DisplayFormatCommunity("0-10-99999", "Missing")));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.updateDisplayFormat("MyFmt", body));
    assertTrue(ex.getMessage().contains("unknown community"), ex.getMessage());
    verify(designWs, never()).saveDisplayFormats(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_allowedCommunities_nonAdmin_is403() throws Exception {
    adaptor = new DisplayFormatAdaptor(designWs, () -> false);
    DisplayFormat body = new DisplayFormat();
    body.setAllowedCommunities(List.of(new DisplayFormatCommunity("0-10-1001", "Default")));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.updateDisplayFormat("MyFmt", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).saveDisplayFormats(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_omittedAllowedCommunities_leavesExisting() throws Exception {
    IPSGuid communityGuid = new PSGuid(PSTypeEnum.COMMUNITY_DEF, 1001L);
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    nativeDf.addCommunity(String.valueOf(communityGuid.longValue()));
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));

    DisplayFormat body = new DisplayFormat();
    body.setLabel("Updated");

    adaptor.updateDisplayFormat("MyFmt", body);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSDisplayFormat>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveDisplayFormats(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    PSDisplayFormat persisted = saved.getValue().get(0);
    assertTrue(
        persisted.doesPropertyHaveValue(
            PSDisplayFormat.PROP_COMMUNITY, String.valueOf(communityGuid.longValue())));
  }

  @Test
  void isAllCommunitiesSentinel_isGuidOrKeyOnly() {
    assertTrue(DisplayFormatAdaptor.isAllCommunitiesSentinel("-1", "Default"));
    assertTrue(DisplayFormatAdaptor.isAllCommunitiesSentinel("-1", "-1"));
    assertFalse(DisplayFormatAdaptor.isAllCommunitiesSentinel("0-13-10", "-1"));
    assertFalse(DisplayFormatAdaptor.isAllCommunitiesSentinel("", "-1"));
    assertFalse(DisplayFormatAdaptor.isAllCommunitiesSentinel(null, "-1"));
  }

  @Test
  void requireKnownCommunityId_skipsNullCatalogName() {
    IPSGuid named = new PSGuid(PSTypeEnum.COMMUNITY_DEF, 1001L);
    IPSGuid unnamed = new PSGuid(PSTypeEnum.COMMUNITY_DEF, 1002L);
    Map<IPSGuid, String> catalog = new HashMap<>();
    catalog.put(named, "Default");
    catalog.put(unnamed, null);
    assertEquals(
        String.valueOf(named.longValue()),
        DisplayFormatAdaptor.requireKnownCommunityId("", "Default", catalog));
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> DisplayFormatAdaptor.requireKnownCommunityId("", "Nope", catalog));
    assertTrue(ex.getMessage().contains("unknown community"), ex.getMessage());
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
  void update_rejectsByAuthorReplayLoad() throws Exception {
    PSDisplayFormat catalog = nativeDisplayFormat(1031, "MyFmt");
    PSDisplayFormat replayed = nativeDisplayFormat(5, "By_Author");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(catalog);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(replayed));

    DisplayFormat body = new DisplayFormat();
    body.setLabel("Updated");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.updateDisplayFormat("MyFmt", body));
    assertEquals(409, ex.getResponse().getStatus());
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
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> deleted = ArgumentCaptor.forClass(List.class);
    verify(designWs)
        .deleteDisplayFormats(deleted.capture(), eq(false), eq("test-session"), eq("Admin"));
    assertEquals(1, deleted.getValue().size());
    assertEquals(42, deleted.getValue().get(0).getUUID());
    verify(designWs, never()).saveDisplayFormats(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_nameKeyResolvesNativeDisplayId_neverEmptyIds() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "qa4091fmt");
    when(designWs.findDisplayFormat(eq("qa4091fmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));

    IPSGuid resolved = adaptor.resolvePersistedGuid("qa4091fmt");
    assertEquals(42, resolved.getUUID());
    assertTrue(adaptor.deleteDisplayFormat("qa4091fmt"));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> deleted = ArgumentCaptor.forClass(List.class);
    verify(designWs).deleteDisplayFormats(deleted.capture(), eq(false), any(), any());
    assertFalse(deleted.getValue().isEmpty());
    assertEquals(42, deleted.getValue().get(0).getUUID());
  }

  @Test
  void resolvePersistedGuid_rejectsUnpersisted() throws Exception {
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(null);
    assertNull(adaptor.resolvePersistedGuid("MyFmt"));
    assertFalse(adaptor.deleteDisplayFormat("MyFmt"));
    verify(designWs, never()).deleteDisplayFormats(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadDisplayFormats(anyList(), anyBoolean(), anyBoolean(), any(), any());
  }

  @Test
  void isEmptyIdsFailure_detectsValidateParametersMessage() {
    assertTrue(
        DisplayFormatAdaptor.isEmptyIdsFailure(
            new IllegalArgumentException("ids cannot be null or empty")));
    assertFalse(
        DisplayFormatAdaptor.isEmptyIdsFailure(new IllegalArgumentException("name is required")));
    assertFalse(DisplayFormatAdaptor.isEmptyIdsFailure(null));
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
    PSErrorsException deps = new PSErrorsException();
    deps.addError(guid, new PSErrorException("Display format has dependents"));
    org.mockito.Mockito.doThrow(deps)
        .when(designWs)
        .deleteDisplayFormats(anyList(), anyBoolean(), any(), any());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteDisplayFormat("MyFmt"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("depend"), ex.getMessage());
  }

  @Test
  void delete_xmlPersistFailure_is500() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));
    PSErrorsException failed = new PSErrorsException();
    failed.addError(guid, new PSErrorException("Xml Document Expected, none supplied"));
    org.mockito.Mockito.doThrow(failed)
        .when(designWs)
        .deleteDisplayFormats(anyList(), anyBoolean(), any(), any());

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.deleteDisplayFormat("MyFmt"));
    assertTrue(ex.getMessage().toLowerCase().contains("delete"), ex.getMessage());
  }

  @Test
  void delete_lockLoadEmptyIds_stillDeletesResolvedId() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenThrow(new IllegalArgumentException("ids cannot be null or empty"));

    assertTrue(adaptor.deleteDisplayFormat("MyFmt"));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> deleted = ArgumentCaptor.forClass(List.class);
    verify(designWs).deleteDisplayFormats(deleted.capture(), eq(false), any(), any());
    assertEquals(42, deleted.getValue().get(0).getUUID());
  }

  @Test
  void delete_emptyIdsFromDesignWs_isNot400IdsMessage() throws Exception {
    PSDisplayFormat nativeDf = nativeDisplayFormat(42, "MyFmt");
    when(designWs.findDisplayFormat(eq("MyFmt"))).thenReturn(nativeDf);
    when(designWs.loadDisplayFormats(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(nativeDf));
    org.mockito.Mockito.doThrow(new IllegalArgumentException("ids cannot be null or empty"))
        .when(designWs)
        .deleteDisplayFormats(anyList(), anyBoolean(), any(), any());

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.deleteDisplayFormat("MyFmt"));
    assertTrue(ex.getMessage().contains("empty id list"), ex.getMessage());
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
