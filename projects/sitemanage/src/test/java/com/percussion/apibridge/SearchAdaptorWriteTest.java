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
import com.percussion.rest.searches.SearchDef;
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
 * UI-06 POST create / PUT update / DELETE persist via {@code createSearches}/{@code
 * saveSearches}/{@code deleteSearches}. Admin only; unique name; no lock steal.
 */
@Tag("UnitTest")
class SearchAdaptorWriteTest {

  private IPSUiDesignWs designWs;
  private SearchAdaptor adaptor;
  private IPSGuid guid;

  @BeforeEach
  void setUp() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    designWs = mock(IPSUiDesignWs.class);
    adaptor =
        new SearchAdaptor(
            designWs, mock(IPSFolderHelper.class), mock(IPSIdMapper.class), () -> true);
    guid = mock(IPSGuid.class);
    when(guid.toString()).thenReturn("0-301-42");
    when(guid.toStringUntyped()).thenReturn("42");
    when(guid.getHostId()).thenReturn(0L);
    when(guid.longValue()).thenReturn(42L);
    when(guid.getType()).thenReturn((short) 301);
    when(guid.getUUID()).thenReturn(42);
    when(designWs.findSearches(any(), isNull())).thenReturn(List.of());
    when(designWs.findViews(any(), isNull())).thenReturn(List.of());
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void create_usesCreateThenSaveAndReleasesLock() throws Exception {
    PSSearch search = stubSearch("MySearch", PSSearch.TYPE_STANDARDSEARCH);
    when(search.getLabel()).thenReturn("My Search");
    when(search.getDescription()).thenReturn("created via REST");
    when(designWs.createSearches(
            eq(List.of("MySearch")),
            eq(List.of(PSSearch.TYPE_STANDARDSEARCH)),
            eq("test-session"),
            eq("Admin")))
        .thenReturn(List.of(search));

    SearchDef body = new SearchDef();
    body.setName("MySearch");
    body.setLabel("My Search");
    body.setDescription("created via REST");
    body.setDisplayFormatId("1");

    SearchDef out = adaptor.createSearch(body);

    assertEquals("MySearch", out.getName());
    assertEquals("My Search", out.getLabel());
    assertEquals("created via REST", out.getDescription());
    verify(designWs)
        .createSearches(
            eq(List.of("MySearch")),
            eq(List.of(PSSearch.TYPE_STANDARDSEARCH)),
            eq("test-session"),
            eq("Admin"));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSSearch>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveSearches(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    verify(search).setDisplayName("My Search");
    verify(search).setDescription("created via REST");
    verify(search).setDisplayFormatId("1");
  }

  @Test
  void create_duplicateName_is409BeforeCreate() throws Exception {
    IPSCatalogSummary existing = mock(IPSCatalogSummary.class);
    when(existing.getName()).thenReturn("MySearch");
    when(designWs.findSearches(eq("MySearch"), isNull())).thenReturn(List.of(existing));

    SearchDef body = new SearchDef();
    body.setName("MySearch");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createSearch(body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("already exists"));
    verify(designWs, never()).createSearches(anyList(), anyList(), any(), any());
    verify(designWs, never()).saveSearches(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_persistTimeDuplicate_is409() throws Exception {
    when(designWs.createSearches(
            eq(List.of("MySearch")),
            eq(List.of(PSSearch.TYPE_STANDARDSEARCH)),
            eq("test-session"),
            eq("Admin")))
        .thenThrow(
            new IllegalArgumentException("The name 'MySearch' for type 'SEARCH_DEF' already exists."));
    SearchDef body = new SearchDef();
    body.setName("MySearch");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createSearch(body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveSearches(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void create_blankName_throwsBeforeDesignWs() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.createSearch(null));
    SearchDef blank = new SearchDef();
    blank.setName("  ");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createSearch(blank));
    assertTrue(ex.getMessage().contains("name is required"));
    verify(designWs, never()).createSearches(anyList(), anyList(), any(), any());
  }

  @Test
  void create_nameWithSpaces_throwsBeforeDesignWs() {
    SearchDef body = new SearchDef();
    body.setName("has space");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createSearch(body));
    assertEquals("name cannot contain whitespace", ex.getMessage());
    verify(designWs, never()).createSearches(anyList(), anyList(), any(), any());
  }

  @Test
  void create_wildcardName_throwsBeforeDesignWs() {
    SearchDef body = new SearchDef();
    body.setName("My*Search");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createSearch(body));
    assertEquals("name must not contain wildcards", ex.getMessage());
  }

  @Test
  void create_viewType_is400() {
    SearchDef body = new SearchDef();
    body.setName("MyView");
    body.setType("View");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createSearch(body));
    assertTrue(ex.getMessage().toLowerCase().contains("view"));
    verify(designWs, never()).createSearches(anyList(), anyList(), any(), any());
  }

  @Test
  void create_nonAdmin_is403() {
    adaptor =
        new SearchAdaptor(
            designWs, mock(IPSFolderHelper.class), mock(IPSIdMapper.class), () -> false);
    SearchDef body = new SearchDef();
    body.setName("MySearch");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createSearch(body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).createSearches(anyList(), anyList(), any(), any());
  }

  @Test
  void create_missingSession_is403() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    SearchDef body = new SearchDef();
    body.setName("MySearch");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createSearch(body));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("session"), ex.getMessage());
  }

  @Test
  void update_loadsWithLockNoStealAndSavesFields() throws Exception {
    PSSearch existing = stubSearch("MySearch", PSSearch.TYPE_STANDARDSEARCH);
    stubCatalogLoad(existing, false);
    PSSearch locked = stubSearch("MySearch", PSSearch.TYPE_STANDARDSEARCH);
    when(designWs.loadSearches(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    SearchDef body = new SearchDef();
    body.setLabel("Updated");
    body.setDescription("new desc");
    body.setType("StandardSearch");
    body.setDisplayFormatId("2");

    SearchDef out = adaptor.saveSearch("MySearch", body);

    assertEquals("MySearch", out.getName());
    verify(locked).setDisplayName("Updated");
    verify(locked).setDescription("new desc");
    verify(locked).setDisplayFormatId("2");
    verify(designWs).saveSearches(anyList(), eq(true), eq("test-session"), eq("Admin"));
    verify(designWs, never()).createSearches(anyList(), anyList(), any(), any());
    verify(designWs)
        .loadSearches(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void update_unknown_returnsNull() throws Exception {
    when(designWs.findSearches(isNull(), isNull())).thenReturn(List.of());
    when(designWs.findViews(isNull(), isNull())).thenReturn(List.of());
    SearchDef body = new SearchDef();
    body.setLabel("Updated");
    assertNull(adaptor.saveSearch("missing", body));
    verify(designWs, never()).saveSearches(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_view_is400() throws Exception {
    PSSearch view = stubSearch("View_All", PSSearch.TYPE_VIEW);
    when(view.isView()).thenReturn(true);
    stubCatalogLoad(view, true);
    SearchDef body = new SearchDef();
    body.setLabel("All");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.saveSearch("View_All", body));
    assertTrue(ex.getMessage().toLowerCase().contains("view"));
    verify(designWs, never()).saveSearches(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_lockConflict_is409() throws Exception {
    PSSearch existing = stubSearch("MySearch", PSSearch.TYPE_STANDARDSEARCH);
    stubCatalogLoad(existing, false);
    when(designWs.loadSearches(anyList(), eq(true), eq(false), any(), any()))
        .thenThrow(new PSErrorResultsException());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.saveSearch("MySearch", new SearchDef()));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveSearches(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_nonAdmin_is403() {
    adaptor =
        new SearchAdaptor(
            designWs, mock(IPSFolderHelper.class), mock(IPSIdMapper.class), () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.saveSearch("MySearch", new SearchDef()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void delete_thenFindIsMissing() throws Exception {
    PSSearch existing = stubSearch("MySearch", PSSearch.TYPE_STANDARDSEARCH);
    stubCatalogLoad(existing, false);
    when(designWs.loadSearches(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(existing));

    assertTrue(adaptor.deleteSearch("MySearch"));
    verify(designWs)
        .deleteSearches(eq(List.of(guid)), eq(false), eq("test-session"), eq("Admin"));
  }

  @Test
  void delete_unknown_returnsFalse() throws Exception {
    when(designWs.findSearches(isNull(), isNull())).thenReturn(List.of());
    when(designWs.findViews(isNull(), isNull())).thenReturn(List.of());
    assertFalse(adaptor.deleteSearch("missing"));
    verify(designWs, never()).deleteSearches(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_lockConflict_is409() throws Exception {
    PSSearch existing = stubSearch("MySearch", PSSearch.TYPE_STANDARDSEARCH);
    stubCatalogLoad(existing, false);
    when(designWs.loadSearches(anyList(), eq(true), eq(false), any(), any()))
        .thenThrow(new PSErrorResultsException());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteSearch("MySearch"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).deleteSearches(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void delete_dependents_is409() throws Exception {
    PSSearch existing = stubSearch("MySearch", PSSearch.TYPE_STANDARDSEARCH);
    stubCatalogLoad(existing, false);
    when(designWs.loadSearches(anyList(), eq(true), eq(false), any(), any()))
        .thenReturn(List.of(existing));
    PSErrorsException errors = new PSErrorsException();
    errors.addError(guid, new PSErrorException("Object has dependents"));
    doThrow(errors).when(designWs).deleteSearches(anyList(), eq(false), any(), any());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteSearch("MySearch"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("depend"), ex.getMessage());
  }

  @Test
  void delete_nonAdmin_is403() {
    adaptor =
        new SearchAdaptor(
            designWs, mock(IPSFolderHelper.class), mock(IPSIdMapper.class), () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteSearch("MySearch"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).deleteSearches(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void resolveSearchType_defaultsAndAliases() {
    assertEquals(PSSearch.TYPE_STANDARDSEARCH, SearchAdaptor.resolveSearchType(null, true));
    assertNull(SearchAdaptor.resolveSearchType(null, false));
    assertEquals(PSSearch.TYPE_STANDARDSEARCH, SearchAdaptor.resolveSearchType("standard", true));
    assertEquals(PSSearch.TYPE_CUSTOMSEARCH, SearchAdaptor.resolveSearchType("custom", false));
    assertEquals(PSSearch.TYPE_USERSEARCH, SearchAdaptor.resolveSearchType("Search", true));
    assertThrows(IllegalArgumentException.class, () -> SearchAdaptor.resolveSearchType("View", true));
    assertThrows(IllegalArgumentException.class, () -> SearchAdaptor.resolveSearchType("nope", true));
  }

  @Test
  void requireValidName_rejectsSpacesAndWildcards() {
    assertEquals("MySearch", SearchAdaptor.requireValidName("MySearch"));
    assertThrows(IllegalArgumentException.class, () -> SearchAdaptor.requireValidName("  "));
    assertThrows(IllegalArgumentException.class, () -> SearchAdaptor.requireValidName("has space"));
    assertThrows(IllegalArgumentException.class, () -> SearchAdaptor.requireValidName("x*y"));
  }

  private PSSearch stubSearch(String name, String type) {
    PSSearch s = mock(PSSearch.class);
    when(s.getName()).thenReturn(name);
    when(s.getLabel()).thenReturn(name);
    when(s.getDescription()).thenReturn("");
    when(s.getType()).thenReturn(type);
    when(s.getDisplayFormatId()).thenReturn("1");
    when(s.getUrl()).thenReturn(null);
    when(s.getParentCategory()).thenReturn(1);
    when(s.getMaximumResultSize()).thenReturn(100);
    when(s.isUserSearch()).thenReturn(PSSearch.TYPE_USERSEARCH.equals(type));
    when(s.isCustomSearch()).thenReturn(PSSearch.TYPE_CUSTOMSEARCH.equals(type));
    when(s.isCustomView()).thenReturn(false);
    when(s.isView()).thenReturn(PSSearch.TYPE_VIEW.equals(type));
    when(s.isStandardSearch()).thenReturn(PSSearch.TYPE_STANDARDSEARCH.equals(type));
    when(s.isUserCustomizable()).thenReturn(false);
    when(s.isCaseSensitive()).thenReturn(false);
    when(s.getFieldContainer()).thenReturn(null);
    when(s.getGUID()).thenReturn(guid);
    when(s.getId()).thenReturn(42);
    return s;
  }

  private void stubCatalogLoad(PSSearch search, boolean asView) throws Exception {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    if (asView) {
      when(designWs.findSearches(isNull(), isNull())).thenReturn(List.of());
      when(designWs.findViews(isNull(), isNull())).thenReturn(List.of(sum));
      when(designWs.loadViews(anyList(), eq(false), eq(false), any(), any()))
          .thenReturn(List.of(search));
    } else {
      when(designWs.findSearches(isNull(), isNull())).thenReturn(List.of(sum));
      when(designWs.findViews(isNull(), isNull())).thenReturn(List.of());
      when(designWs.loadSearches(anyList(), eq(false), eq(false), any(), any()))
          .thenReturn(List.of(search));
    }
  }
}
