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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSSearch;
import com.percussion.rest.views.ViewDef;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.ui.IPSUiDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * UI-07 POST create / PUT update / DELETE persist via {@code createViews}/{@code saveViews}/{@code
 * deleteViews}. Admin only; unique name; no lock steal; Inbox/custom URL is 409.
 */
@Tag("UnitTest")
class ViewAdaptorWriteTest {

  private IPSUiDesignWs designWs;
  private ViewAdaptor adaptor;
  private IPSGuid guid;

  @BeforeEach
  void setUp() throws Exception {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    designWs = mock(IPSUiDesignWs.class);
    adaptor =
        new ViewAdaptor(
            designWs, mock(IPSFolderHelper.class), mock(IPSIdMapper.class), () -> true);
    guid = mock(IPSGuid.class);
    when(guid.toString()).thenReturn("0-18-42");
    when(guid.toStringUntyped()).thenReturn("42");
    when(guid.getHostId()).thenReturn(0L);
    when(guid.longValue()).thenReturn(42L);
    when(guid.getType()).thenReturn((short) 18);
    when(guid.getUUID()).thenReturn(42);
    when(designWs.findViews(any(), isNull())).thenReturn(List.of());
    when(designWs.findSearches(any(), isNull())).thenReturn(List.of());
    when(designWs.findAllViews()).thenReturn(List.of());
    when(designWs.findAllSearches()).thenReturn(List.of());
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void create_usesCreateThenSaveAndReleasesLock() throws Exception {
    PSSearch view = stubView("MyView", false);
    when(view.getLabel()).thenReturn("My View");
    when(view.getDescription()).thenReturn("created via REST");
    when(designWs.createViews(eq(List.of("MyView")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(view));
    stubCatalogVisibleAfterSave(view);

    ViewDef body = new ViewDef();
    body.setName("MyView");
    body.setLabel("My View");
    body.setDescription("created via REST");
    body.setDisplayFormatId("1");

    ViewDef out = adaptor.createView(body);

    assertEquals("MyView", out.getName());
    assertEquals("My View", out.getLabel());
    assertEquals("created via REST", out.getDescription());
    verify(designWs).createViews(eq(List.of("MyView")), eq("test-session"), eq("Admin"));
    verify(designWs, org.mockito.Mockito.atLeastOnce()).findAllViews();
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSSearch>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveViews(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    verify(view).setDisplayName("My View");
    verify(view).setDescription("created via REST");
    verify(view).setDisplayFormatId("1");
  }

  @Test
  void create_failsIfNotVisibleToFindAfterSave() throws Exception {
    PSSearch view = stubView("MyView", false);
    when(designWs.createViews(eq(List.of("MyView")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(view));
    when(designWs.findAllViews()).thenReturn(List.of());
    when(designWs.loadViews(eq(List.of(guid)), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(view));

    ViewDef body = new ViewDef();
    body.setName("MyView");
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> adaptor.createView(body));
    assertTrue(ex.getMessage().contains("findViews"), ex.getMessage());
  }

  @Test
  void create_duplicateName_is409BeforeCreate() throws Exception {
    IPSCatalogSummary existing = mock(IPSCatalogSummary.class);
    when(existing.getName()).thenReturn("MyView");
    when(designWs.findViews(eq("MyView"), isNull())).thenReturn(List.of(existing));

    ViewDef body = new ViewDef();
    body.setName("MyView");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createView(body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("already exists"));
    verify(designWs, never()).createViews(anyList(), any(), any());
    verify(designWs, never()).saveViews(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_duplicateName_fromFindAllViews_is409() throws Exception {
    PSSearch existing = stubView("MyView", false);
    when(designWs.findAllViews()).thenReturn(List.of(existing));

    ViewDef body = new ViewDef();
    body.setName("MyView");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createView(body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("already exists"));
    verify(designWs, never()).createViews(anyList(), any(), any());
  }

  @Test
  void create_persistTimeDuplicate_is409() throws Exception {
    when(designWs.createViews(eq(List.of("MyView")), eq("test-session"), eq("Admin")))
        .thenThrow(
            new IllegalArgumentException("The name 'MyView' for type 'SEARCH_DEF' already exists."));
    ViewDef body = new ViewDef();
    body.setName("MyView");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createView(body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveViews(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_blankName_throwsBeforeDesignWs() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.createView(null));
    ViewDef blank = new ViewDef();
    blank.setName("  ");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createView(blank));
    assertTrue(ex.getMessage().contains("name is required"));
    verify(designWs, never()).createViews(anyList(), any(), any());
  }

  @Test
  void create_nameWithSpaces_throwsBeforeDesignWs() {
    ViewDef body = new ViewDef();
    body.setName("has space");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createView(body));
    assertEquals("name cannot contain whitespace", ex.getMessage());
    verify(designWs, never()).createViews(anyList(), any(), any());
  }

  @Test
  void create_wildcardName_throwsBeforeDesignWs() {
    ViewDef body = new ViewDef();
    body.setName("My*View");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createView(body));
    assertEquals("name must not contain wildcards", ex.getMessage());
  }

  @Test
  void create_customUrl_is400() {
    ViewDef body = new ViewDef();
    body.setName("MyInbox");
    body.setUrl("../sys_cxViews/inbox.xml");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createView(body));
    assertTrue(ex.getMessage().toLowerCase().contains("custom"));
    verify(designWs, never()).createViews(anyList(), any(), any());
  }

  @Test
  void create_searchType_is400() {
    ViewDef body = new ViewDef();
    body.setName("MyView");
    body.setType("StandardSearch");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createView(body));
    assertTrue(ex.getMessage().toLowerCase().contains("search"));
    verify(designWs, never()).createViews(anyList(), any(), any());
  }

  @Test
  void create_nonAdmin_is403() {
    adaptor =
        new ViewAdaptor(
            designWs, mock(IPSFolderHelper.class), mock(IPSIdMapper.class), () -> false);
    ViewDef body = new ViewDef();
    body.setName("MyView");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createView(body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).createViews(anyList(), any(), any());
  }

  @Test
  void create_missingSession_is403() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    ViewDef body = new ViewDef();
    body.setName("MyView");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createView(body));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("session"), ex.getMessage());
  }

  @Test
  void update_loadsWithLockNoStealAndSavesFields() throws Exception {
    PSSearch existing = stubView("MyView", false);
    stubCatalogLoad(existing);
    PSSearch locked = stubView("MyView", false);
    when(designWs.loadViews(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    ViewDef body = new ViewDef();
    body.setLabel("Updated");
    body.setDescription("new desc");
    body.setType("View");
    body.setDisplayFormatId("2");

    ViewDef out = adaptor.saveView("MyView", body);

    assertEquals("MyView", out.getName());
    verify(locked).setDisplayName("Updated");
    verify(locked).setDescription("new desc");
    verify(locked).setDisplayFormatId("2");
    verify(designWs).saveViews(anyList(), eq(true), eq("test-session"), eq("Admin"));
    verify(designWs, never()).createViews(anyList(), any(), any());
    verify(designWs).loadViews(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void update_unknown_returnsNull() throws Exception {
    when(designWs.findViews(isNull(), isNull())).thenReturn(List.of());
    ViewDef body = new ViewDef();
    body.setLabel("Updated");
    assertNull(adaptor.saveView("missing", body));
    verify(designWs, never()).saveViews(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_inbox_is409() throws Exception {
    PSSearch inbox = stubView("Inbox", true);
    stubCatalogLoad(inbox);
    ViewDef body = new ViewDef();
    body.setLabel("Inbox");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.saveView("Inbox", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("inbox"));
    verify(designWs, never()).saveViews(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_lockConflict_is409() throws Exception {
    PSSearch existing = stubView("MyView", false);
    stubCatalogLoad(existing);
    when(designWs.loadViews(anyList(), eq(true), eq(false), any(), any()))
        .thenThrow(new PSErrorResultsException());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.saveView("MyView", new ViewDef()));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveViews(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_nonAdmin_is403() {
    adaptor =
        new ViewAdaptor(
            designWs, mock(IPSFolderHelper.class), mock(IPSIdMapper.class), () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.saveView("MyView", new ViewDef()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void delete_thenFindIsMissing() throws Exception {
    PSSearch existing = stubView("MyView", false);
    stubCatalogLoad(existing);
    when(designWs.loadViews(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(existing));

    assertTrue(adaptor.deleteView("MyView"));
    verify(designWs).deleteViews(eq(List.of(guid)), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void delete_unknown_returnsFalse() throws Exception {
    when(designWs.findViews(isNull(), isNull())).thenReturn(List.of());
    assertFalse(adaptor.deleteView("missing"));
    verify(designWs, never()).deleteViews(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_inbox_is409() throws Exception {
    PSSearch inbox = stubView("Inbox", true);
    stubCatalogLoad(inbox);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteView("Inbox"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).deleteViews(anyList(), anyBoolean(), any(), any());
    verify(designWs, never()).loadViews(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void delete_customUrl_is409() throws Exception {
    PSSearch custom = stubView("MyCustom", true);
    stubCatalogLoad(custom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteView("MyCustom"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).deleteViews(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_lockConflict_is409() throws Exception {
    PSSearch existing = stubView("MyView", false);
    stubCatalogLoad(existing);
    when(designWs.loadViews(anyList(), eq(true), eq(false), any(), any()))
        .thenThrow(new PSErrorResultsException());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteView("MyView"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).deleteViews(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_dependents_is409() throws Exception {
    PSSearch existing = stubView("MyView", false);
    stubCatalogLoad(existing);
    when(designWs.loadViews(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(existing));
    PSErrorsException errors = new PSErrorsException();
    errors.addError(guid, new PSErrorException("Object has dependents"));
    doThrow(errors).when(designWs).deleteViews(anyList(), eq(false), any(), any());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteView("MyView"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("depend"), ex.getMessage());
  }

  @Test
  void delete_nonAdmin_is403() {
    adaptor =
        new ViewAdaptor(
            designWs, mock(IPSFolderHelper.class), mock(IPSIdMapper.class), () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteView("MyView"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).deleteViews(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void resolveViewType_defaultsAndAliases() {
    assertEquals(PSSearch.TYPE_VIEW, ViewAdaptor.resolveViewType(null, true));
    assertNull(ViewAdaptor.resolveViewType(null, false));
    assertEquals(PSSearch.TYPE_VIEW, ViewAdaptor.resolveViewType("standard", true));
    assertEquals(PSSearch.TYPE_VIEW, ViewAdaptor.resolveViewType("View", true));
    assertThrows(IllegalArgumentException.class, () -> ViewAdaptor.resolveViewType("custom", true));
    assertThrows(
        IllegalArgumentException.class, () -> ViewAdaptor.resolveViewType("StandardSearch", true));
    assertThrows(IllegalArgumentException.class, () -> ViewAdaptor.resolveViewType("nope", true));
  }

  @Test
  void requireValidName_rejectsSpacesAndWildcards() {
    assertEquals("MyView", ViewAdaptor.requireValidName("MyView"));
    assertThrows(IllegalArgumentException.class, () -> ViewAdaptor.requireValidName("  "));
    assertThrows(IllegalArgumentException.class, () -> ViewAdaptor.requireValidName("has space"));
    assertThrows(IllegalArgumentException.class, () -> ViewAdaptor.requireValidName("x*y"));
  }

  private PSSearch stubView(String name, boolean customUrl) {
    PSSearch s = mock(PSSearch.class);
    when(s.getName()).thenReturn(name);
    when(s.getLabel()).thenReturn(name);
    when(s.getDescription()).thenReturn("");
    when(s.getType()).thenReturn(PSSearch.TYPE_VIEW);
    when(s.getDisplayFormatId()).thenReturn("1");
    when(s.getUrl()).thenReturn(customUrl ? "../sys_cxViews/inbox.xml" : null);
    when(s.getParentCategory()).thenReturn(1);
    when(s.getMaximumResultSize()).thenReturn(100);
    when(s.isUserSearch()).thenReturn(false);
    when(s.isCustomSearch()).thenReturn(false);
    when(s.isCustomView()).thenReturn(customUrl);
    when(s.isView()).thenReturn(true);
    when(s.isStandardView()).thenReturn(!customUrl);
    when(s.isUserCustomizable()).thenReturn(false);
    when(s.isCaseSensitive()).thenReturn(false);
    when(s.getFieldContainer()).thenReturn(null);
    when(s.getGUID()).thenReturn(guid);
    when(s.getId()).thenReturn(42);
    return s;
  }

  private void stubCatalogLoad(PSSearch search) throws Exception {
    String catalogName = search.getName();
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn(catalogName);
    when(designWs.findViews(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.findAllViews()).thenReturn(List.of(search));
    when(designWs.loadViews(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(search));
  }

  private void stubCatalogVisibleAfterSave(PSSearch search) throws Exception {
    when(designWs.findAllViews()).thenReturn(List.of()).thenReturn(List.of(search));
  }
}
