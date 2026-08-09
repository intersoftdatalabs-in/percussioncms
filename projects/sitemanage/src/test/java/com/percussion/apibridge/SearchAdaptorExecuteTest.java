/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSSearch;
import com.percussion.rest.searches.SearchExecuteRequest;
import com.percussion.rest.searches.SearchExecuteResult;
import com.percussion.rest.searches.SearchResultItem;
import com.percussion.search.IPSSearchResultRow;
import com.percussion.search.PSWSSearchResponse;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.ui.IPSUiDesignWs;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class SearchAdaptorExecuteTest {

  private IPSUiDesignWs designWs;
  private IPSFolderHelper folderHelper;
  private IPSIdMapper idMapper;
  private SearchAdaptor adaptor;

  @BeforeEach
  void setUp() {
    designWs = mock(IPSUiDesignWs.class);
    folderHelper = mock(IPSFolderHelper.class);
    idMapper = mock(IPSIdMapper.class);
    adaptor = spy(new SearchAdaptor(designWs, folderHelper, idMapper));
  }

  @Test
  void normalizeRequest_nullBecomesEmpty() {
    SearchExecuteRequest n = SearchAdaptor.normalizeRequest(null);
    assertNotNull(n);
    assertNull(n.getStartIndex());
    assertNull(n.getMaxResults());
  }

  @Test
  void normalizeRequest_rejectsBadStartIndex() {
    SearchExecuteRequest r = new SearchExecuteRequest();
    r.setStartIndex(0);
    assertThrows(IllegalArgumentException.class, () -> SearchAdaptor.normalizeRequest(r));
  }

  @Test
  void normalizeRequest_rejectsBadMaxResults() {
    SearchExecuteRequest r = new SearchExecuteRequest();
    r.setMaxResults(0);
    assertThrows(IllegalArgumentException.class, () -> SearchAdaptor.normalizeRequest(r));
  }

  @Test
  void normalizeRequest_rejectsBadSortOrder() {
    SearchExecuteRequest r = new SearchExecuteRequest();
    r.setSortOrder("sideways");
    assertThrows(IllegalArgumentException.class, () -> SearchAdaptor.normalizeRequest(r));
  }

  @Test
  void normalizeRequest_normalizesSortOrderCase() {
    SearchExecuteRequest r = new SearchExecuteRequest();
    r.setSortOrder("DESC");
    assertEquals("desc", SearchAdaptor.normalizeRequest(r).getSortOrder());
  }

  @Test
  void applyExecuteOverrides_folderAndMax() {
    PSSearch s = mock(PSSearch.class);
    when(s.getMaximumResultSize()).thenReturn(100);
    when(s.getProperty(PSSearch.PROP_FOLDER_PATH_RECURSE)).thenReturn(null);

    SearchExecuteRequest r = new SearchExecuteRequest();
    r.setFolderPath("//Sites/Demo");
    r.setMaxResults(10);
    SearchAdaptor.applyExecuteOverrides(s, r);

    verify(s).setProperty(PSSearch.PROP_FOLDER_PATH, "//Sites/Demo");
    verify(s).setProperty(PSSearch.PROP_FOLDER_PATH_RECURSE, "true");
    verify(s).setMaximumNumber(10);
  }

  @Test
  void applyExecuteOverrides_defaultsUnlimitedMax() {
    PSSearch s = mock(PSSearch.class);
    when(s.getMaximumResultSize()).thenReturn(0);
    SearchAdaptor.applyExecuteOverrides(s, new SearchExecuteRequest());
    verify(s).setMaximumNumber(SearchAdaptor.DEFAULT_PAGE_SIZE);
  }

  @Test
  void sortItems_byTitleDesc() {
    List<SearchResultItem> items = new ArrayList<>();
    items.add(item("1", "Alpha"));
    items.add(item("2", "Charlie"));
    items.add(item("3", "Bravo"));
    SearchAdaptor.sortItems(items, "sys_title", "desc");
    assertEquals("Charlie", items.get(0).getTitle());
    assertEquals("Bravo", items.get(1).getTitle());
    assertEquals("Alpha", items.get(2).getTitle());
  }

  @Test
  void executeSearch_unsafeKeyReturnsNull() {
    assertNull(adaptor.executeSearch("../x", new SearchExecuteRequest()));
    assertNull(adaptor.executeSearch("a/b", null));
    assertNull(adaptor.executeSearch(null, null));
  }

  @Test
  void executeSearch_missingReturnsNull() throws Exception {
    when(designWs.findSearches(isNull(), isNull())).thenReturn(List.of());
    assertNull(adaptor.executeSearch("Missing", new SearchExecuteRequest()));
  }

  @Test
  void executeSearch_customUrlThrows400Style() throws Exception {
    PSSearch custom = mockSearch("Custom", true, false);
    stubLoadedSearches(List.of(custom));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.executeSearch("Custom", new SearchExecuteRequest()));
    assertTrue(ex.getMessage().toLowerCase().contains("custom"));
  }

  @Test
  void executeSearch_happyPathPagesAndMaps() throws Exception {
    PSSearch design = mockSearch("All Content", false, true);
    when(design.getDisplayFormatId()).thenReturn("0-1-1");
    when(design.getMaximumResultSize()).thenReturn(100);
    when(design.clone()).thenReturn(design);
    stubLoadedSearches(List.of(design));

    SearchResultItem a = item("id-a", "A");
    SearchResultItem b = item("id-b", "B");
    SearchResultItem c = item("id-c", "C");
    doReturn(List.of(a, b, c)).when(adaptor).runDesignSearch(any(PSSearch.class));

    SearchExecuteRequest req = new SearchExecuteRequest();
    req.setStartIndex(2);
    req.setMaxResults(1);
    SearchExecuteResult result = adaptor.executeSearch("All Content", req);

    assertNotNull(result);
    assertEquals("All Content", result.getSearchName());
    assertEquals("0-1-1", result.getDisplayFormatId());
    assertEquals(3, result.getTotalCount());
    assertEquals(2, result.getStartIndex());
    assertEquals(1, result.getChildren().size());
    assertEquals("B", result.getChildren().get(0).getTitle());
  }

  @Test
  void executeSearch_emptyResults() throws Exception {
    PSSearch design = mockSearch("Empty", false, true);
    when(design.getDisplayFormatId()).thenReturn("0-1-1");
    when(design.getMaximumResultSize()).thenReturn(25);
    when(design.clone()).thenReturn(design);
    stubLoadedSearches(List.of(design));
    doReturn(List.of()).when(adaptor).runDesignSearch(any(PSSearch.class));

    SearchExecuteResult result = adaptor.executeSearch("Empty", null);
    assertNotNull(result);
    assertEquals(0, result.getTotalCount());
    assertTrue(result.getChildren().isEmpty());
    assertEquals(1, result.getStartIndex());
  }

  @Test
  void mapSearchResponse_skipsNullMappedRowsAndKeepsItems() {
    IPSSearchResultRow blank = mock(IPSSearchResultRow.class);
    IPSSearchResultRow ok = mock(IPSSearchResultRow.class);

    // Stub row mapping so this unit test does not depend on PSGuidUtils/static CMS context
    doReturn(null).when(adaptor).mapResultRow(blank);
    SearchResultItem kept = item("guid-42", "Title");
    kept.setFolderPath("/Sites/Demo");
    kept.setType("Page");
    doReturn(kept).when(adaptor).mapResultRow(ok);

    PSWSSearchResponse response = mock(PSWSSearchResponse.class);
    when(response.getRows()).thenReturn(List.of(blank, ok).iterator());

    List<SearchResultItem> mapped = adaptor.mapSearchResponse(response);
    assertEquals(1, mapped.size());
    assertEquals("guid-42", mapped.get(0).getId());
    assertEquals("/Sites/Demo", mapped.get(0).getFolderPath());
    assertEquals("Page", mapped.get(0).getType());
  }

  @Test
  void mapResultRow_blankContentIdReturnsNull() {
    IPSSearchResultRow blank = mock(IPSSearchResultRow.class);
    when(blank.getColumnValue("sys_contentid")).thenReturn("  ");
    assertNull(adaptor.mapResultRow(blank));
  }

  @Test
  void mapSearchResponse_nullSafe() {
    assertTrue(adaptor.mapSearchResponse(null).isEmpty());
  }

  @Test
  void isSafeSearchKey_stillHoldsForExecutePath() {
    assertTrue(SearchAdaptor.isSafeSearchKey("All Content"));
    assertFalse(SearchAdaptor.isSafeSearchKey("a\\b"));
  }

  private static SearchResultItem item(String id, String title) {
    SearchResultItem i = new SearchResultItem();
    i.setId(id);
    i.setName(title);
    i.setTitle(title);
    return i;
  }

  private PSSearch mockSearch(String name, boolean custom, boolean standard) {
    PSSearch s = mock(PSSearch.class);
    when(s.getName()).thenReturn(name);
    when(s.isCustomSearch()).thenReturn(custom);
    when(s.isStandardSearch()).thenReturn(standard);
    when(s.getId()).thenReturn(10);
    when(s.getGUID()).thenReturn(null);
    return s;
  }

  private void stubLoadedSearches(List<PSSearch> searches) throws Exception {
    List<IPSCatalogSummary> summaries = new ArrayList<>();
    List<IPSGuid> guids = new ArrayList<>();
    for (int i = 0; i < searches.size(); i++) {
      IPSGuid g = mock(IPSGuid.class);
      when(g.toString()).thenReturn("0-301-" + i);
      IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
      when(sum.getGUID()).thenReturn(g);
      summaries.add(sum);
      guids.add(g);
      PSSearch s = searches.get(i);
      when(s.getGUID()).thenReturn(g);
    }
    when(designWs.findSearches(isNull(), isNull())).thenReturn(summaries);
    when(designWs.loadSearches(anyList(), eq(false), eq(false), isNull(), isNull()))
        .thenReturn(searches);
  }
}
