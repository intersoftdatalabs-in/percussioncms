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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSKey;
import com.percussion.cms.objectstore.PSSearch;
import com.percussion.cms.objectstore.PSSearchField;
import com.percussion.rest.searches.SearchDef;
import com.percussion.rest.searches.SearchFieldSummary;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.ui.IPSUiDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * UI-08 PUT field criteria persist via {@link SearchAdaptor#applyWritableFields}. Unknown field is
 * 400; packaged/system searches are 409 (no lock steal).
 */
@Tag("UnitTest")
class SearchAdaptorFieldsTest {

  private IPSUiDesignWs designWs;
  private SearchAdaptor adaptor;
  private IPSGuid guid;

  @BeforeEach
  void setUp() throws Exception {
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
    when(designWs.findAllSearches()).thenReturn(List.of());
    when(designWs.findViews(any(), isNull())).thenReturn(List.of());
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void applyWritableFields_replacesCriteriaInOrder() throws Exception {
    PSSearch domain = new PSSearch();
    domain.setInternalName("MySearch");
    domain.addField(
        new PSSearchField("sys_contentid", "Content id", "", PSSearchField.TYPE_NUMBER, ""));

    SearchDef body = new SearchDef();
    body.setFields(
        List.of(
            field("sys_title", "equal", "Hello", 0),
            field("sys_contentcreatedby", "like", "admin", 1)));

    SearchAdaptor.applyWritableFields(domain, body);

    List<PSSearchField> persisted = collectFields(domain);
    assertEquals(2, persisted.size());
    assertEquals("sys_title", persisted.get(0).getFieldName());
    assertEquals(PSSearchField.OP_EQUALS, persisted.get(0).getOperator());
    assertEquals("Hello", persisted.get(0).getFieldValue());
    assertEquals(0, persisted.get(0).getPosition());
    assertEquals("sys_contentcreatedby", persisted.get(1).getFieldName());
    assertEquals(PSSearchField.OP_LIKE, persisted.get(1).getOperator());
    assertEquals("admin", persisted.get(1).getFieldValue());
    assertEquals(1, persisted.get(1).getPosition());
  }

  @Test
  void applyWritableFields_reordersExistingCriteria() throws Exception {
    PSSearch domain = new PSSearch();
    domain.setInternalName("MySearch");
    domain.addField(new PSSearchField("sys_title", "Title", "", PSSearchField.TYPE_TEXT, ""));
    domain.addField(
        new PSSearchField("sys_contentid", "Content id", "", PSSearchField.TYPE_NUMBER, ""));

    SearchDef body = new SearchDef();
    body.setFields(
        List.of(field("sys_contentid", "like", "", 0), field("sys_title", "like", "", 1)));
    SearchAdaptor.applyWritableFields(domain, body);

    List<PSSearchField> persisted = collectFields(domain);
    assertEquals(2, persisted.size());
    assertEquals("sys_contentid", persisted.get(0).getFieldName());
    assertEquals(0, persisted.get(0).getPosition());
    assertEquals("sys_title", persisted.get(1).getFieldName());
    assertEquals(1, persisted.get(1).getPosition());
  }

  @Test
  void applyWritableFields_emptyListClearsCriteria() throws Exception {
    PSSearch domain = new PSSearch();
    domain.setInternalName("MySearch");
    domain.addField(new PSSearchField("sys_title", "Title", "", PSSearchField.TYPE_TEXT, ""));

    SearchDef body = new SearchDef();
    body.setFields(new ArrayList<>());
    SearchAdaptor.applyWritableFields(domain, body);

    assertFalse(domain.getFields().hasNext());
  }

  @Test
  void applyWritableFields_omittedFieldsLeaveExisting() throws Exception {
    PSSearch domain = new PSSearch();
    domain.setInternalName("MySearch");
    domain.addField(new PSSearchField("sys_title", "Title", "", PSSearchField.TYPE_TEXT, ""));

    SearchDef body = new SearchDef();
    body.setLabel("Updated");
    SearchAdaptor.applyWritableFields(domain, body);

    List<PSSearchField> persisted = collectFields(domain);
    assertEquals(1, persisted.size());
    assertEquals("sys_title", persisted.get(0).getFieldName());
    assertEquals("Updated", domain.getDisplayName());
  }

  @Test
  void applyWritableFields_unknownFieldIs400() throws Exception {
    PSSearch domain = new PSSearch();
    domain.setInternalName("MySearch");
    SearchDef body = new SearchDef();
    body.setFields(List.of(field("has space", "equal", "x", 0)));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> SearchAdaptor.applyWritableFields(domain, body));
    assertTrue(ex.getMessage().contains("unknown field"), ex.getMessage());
  }

  @Test
  void applyWritableFields_packagedSearchFieldsIs409() throws Exception {
    PSSearch domain = new PSSearch();
    domain.setInternalName("Default_Search");
    SearchDef body = new SearchDef();
    body.setFields(List.of(field("sys_title", "equal", "x", 0)));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> SearchAdaptor.applyWritableFields(domain, body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().toLowerCase().contains("packaged"), ex.getMessage());
  }

  @Test
  void update_persistsFieldCriteriaViaSaveSearches() throws Exception {
    PSSearch existing = persistedSearch("MySearch");
    PSSearch locked = persistedSearch("MySearch");
    stubCatalogLoad(existing);
    when(designWs.loadSearches(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));
    stubCatalogVisibleAfterSave(locked, "MySearch");

    SearchDef body = new SearchDef();
    body.setFields(List.of(field("sys_title", "equal", "Hello", 0)));

    SearchDef out = adaptor.saveSearch("MySearch", body);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSSearch>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveSearches(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    List<PSSearchField> persisted = collectFields(saved.getValue().get(0));
    assertEquals(1, persisted.size());
    assertEquals("sys_title", persisted.get(0).getFieldName());
    assertEquals("Hello", persisted.get(0).getFieldValue());
    assertEquals("MySearch", out.getName());
  }

  @Test
  void update_unknownFieldDoesNotSave() throws Exception {
    PSSearch existing = persistedSearch("MySearch");
    PSSearch locked = persistedSearch("MySearch");
    stubCatalogLoad(existing);
    when(designWs.loadSearches(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    SearchDef body = new SearchDef();
    body.setFields(List.of(field("../sys_title", "equal", "x", 0)));

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.saveSearch("MySearch", body));
    assertTrue(ex.getMessage().contains("unknown field"), ex.getMessage());
    verify(designWs, never()).saveSearches(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_packagedSearchFieldsIs409NoSteal() throws Exception {
    PSSearch existing = persistedSearch("Default_Search");
    PSSearch locked = persistedSearch("Default_Search");
    stubCatalogLoad(existing);
    when(designWs.loadSearches(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    SearchDef body = new SearchDef();
    body.setFields(List.of(field("sys_title", "equal", "x", 0)));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.saveSearch("Default_Search", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveSearches(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void update_fieldsNonAdminIs403() {
    adaptor =
        new SearchAdaptor(
            designWs, mock(IPSFolderHelper.class), mock(IPSIdMapper.class), () -> false);
    SearchDef body = new SearchDef();
    body.setFields(List.of(field("sys_title", "equal", "x", 0)));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.saveSearch("MySearch", body));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).saveSearches(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void requireValidFieldName_rejectsBlankPathAndWildcards() {
    IllegalArgumentException blank =
        assertThrows(IllegalArgumentException.class, () -> SearchAdaptor.requireValidFieldName("  "));
    assertTrue(blank.getMessage().contains("unknown field"));
    IllegalArgumentException path =
        assertThrows(
            IllegalArgumentException.class, () -> SearchAdaptor.requireValidFieldName("../sys_title"));
    assertTrue(path.getMessage().contains("unknown field"));
    assertEquals("sys_title", SearchAdaptor.requireValidFieldName("sys_title"));
  }

  @Test
  void isPackagedSearch_installerNamesOnly() {
    assertTrue(SearchAdaptor.isPackagedSearch("Default_Search"));
    assertTrue(SearchAdaptor.isPackagedSearch("rc_search"));
    assertFalse(SearchAdaptor.isPackagedSearch("MySearch"));
    assertFalse(SearchAdaptor.isPackagedSearch("qa4110abcd"));
  }

  @Test
  void designGaps_noLongerClaimFieldCriterionGap() {
    assertTrue(
        SearchAdaptor.DESIGN_GAPS.stream()
            .noneMatch(g -> g.toLowerCase().contains("field criterion")));
  }

  private static PSSearch persistedSearch(String name) throws Exception {
    PSSearch search = new PSSearch(name);
    search.setType(PSSearch.TYPE_STANDARDSEARCH);
    PSKey key = PSSearch.createKey(new String[] {"42"});
    search.setLocator(key);
    return search;
  }

  private static SearchFieldSummary field(String name, String op, String value, int position) {
    SearchFieldSummary row = new SearchFieldSummary();
    row.setFieldName(name);
    row.setOperator(op);
    row.setFieldValue(value);
    row.setPosition(position);
    return row;
  }

  private static List<PSSearchField> collectFields(PSSearch search) {
    List<PSSearchField> out = new ArrayList<>();
    Iterator<PSSearchField> it = search.getFields();
    while (it.hasNext()) {
      out.add(it.next());
    }
    return out;
  }

  private void stubCatalogLoad(PSSearch search) throws Exception {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getGUID()).thenReturn(guid);
    when(sum.getName()).thenReturn(search.getName());
    when(designWs.findSearches(isNull(), isNull())).thenReturn(List.of(sum));
    when(designWs.findAllSearches()).thenReturn(List.of(search));
    when(designWs.findViews(isNull(), isNull())).thenReturn(List.of());
    when(designWs.loadSearches(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(search));
  }

  private void stubCatalogVisibleAfterSave(PSSearch search, String name) throws Exception {
    IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
    when(sum.getName()).thenReturn(name);
    when(sum.getGUID()).thenReturn(guid);
    when(designWs.findSearches(eq(name), isNull()))
        .thenReturn(List.of())
        .thenReturn(List.of(sum));
    when(designWs.findAllSearches()).thenReturn(List.of(search));
    when(designWs.loadSearches(
            eq(List.of(guid)), eq(false), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(search));
  }
}
