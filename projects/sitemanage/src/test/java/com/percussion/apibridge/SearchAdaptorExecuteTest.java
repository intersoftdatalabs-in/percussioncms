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
  void normalizeFolderPath_convertsExplorerSlashAndDropsRoot() {
    assertEquals("//Sites/Demo", SearchAdaptor.normalizeFolderPath("/Sites/Demo"));
    assertEquals("//Sites", SearchAdaptor.normalizeFolderPath("Sites"));
    assertEquals("//Sites/Foo", SearchAdaptor.normalizeFolderPath("//Sites/Foo"));
    assertNull(SearchAdaptor.normalizeFolderPath(null));
    assertNull(SearchAdaptor.normalizeFolderPath("  "));
    assertNull(SearchAdaptor.normalizeFolderPath("/"));
    assertNull(SearchAdaptor.normalizeFolderPath("//"));
    assertNull(SearchAdaptor.normalizeFolderPath("///"));
  }

  @Test
  void normalizeRequest_dropsExplorerRootFolderPath() {
    SearchExecuteRequest r = new SearchExecuteRequest();
    r.setFolderPath("/");
    r.setStartIndex(1);
    SearchExecuteRequest n = SearchAdaptor.normalizeRequest(r);
    assertNull(n.getFolderPath());
    assertEquals(1, n.getStartIndex());
  }

  @Test
  void applyExecuteOverrides_ignoresExplorerRootSoAllIsUnscoped() {
    PSSearch s = mock(PSSearch.class);
    when(s.getMaximumResultSize()).thenReturn(25);
    SearchExecuteRequest r = new SearchExecuteRequest();
    r.setFolderPath("/");
    SearchAdaptor.applyExecuteOverrides(s, r);
    verify(s, org.mockito.Mockito.never())
        .setProperty(eq(PSSearch.PROP_FOLDER_PATH), any());
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
    when(designWs.findViews(isNull(), isNull())).thenReturn(List.of());
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
  void executeSearch_viewAllWhenSearchCatalogThrows() throws Exception {
    PSSearch viewAll = mockSearch("View_All", false, false);
    when(viewAll.getLabel()).thenReturn("All");
    when(viewAll.isCustomView()).thenReturn(false);
    when(viewAll.getDisplayFormatId()).thenReturn("0-1-1");
    when(viewAll.getMaximumResultSize()).thenReturn(25);
    when(viewAll.clone()).thenReturn(viewAll);
    when(designWs.findSearches(isNull(), isNull()))
        .thenThrow(new IllegalStateException("PSAction missing"));
    stubLoadedViews(List.of(viewAll));
    doReturn(List.of(item("id-a", "Welcome"))).when(adaptor).runDesignSearch(any(PSSearch.class));

    SearchExecuteResult byName = adaptor.executeSearch("View_All", new SearchExecuteRequest());
    assertNotNull(byName);
    assertEquals("View_All", byName.getSearchName());
  }

  @Test
  void executeSearch_missingAfterSearchCatalogThrowReturnsNull() throws Exception {
    when(designWs.findSearches(isNull(), isNull()))
        .thenThrow(new IllegalStateException("PSAction missing"));
    when(designWs.findViews(isNull(), isNull())).thenReturn(List.of());

    assertNull(adaptor.executeSearch("Missing", new SearchExecuteRequest()));
  }

  @Test
  void matchLoaded_prefersNameOverLabel() {
    PSSearch named = mockSearch("View_All", false, false);
    when(named.getLabel()).thenReturn("All");
    PSSearch other = mockSearch("My Pages", false, true);
    when(other.getLabel()).thenReturn("All");
    assertEquals(named, SearchAdaptor.matchLoaded(List.of(other, named), "View_All"));
    // First label match wins when no internal name equals the key.
    assertEquals(other, SearchAdaptor.matchLoaded(List.of(other, named), "All"));
  }

  @Test
  void executeSearch_resolvesDefaultViewAllFromViewCatalog() throws Exception {
    PSSearch viewAll = mockSearch("View_All", false, false);
    when(viewAll.getLabel()).thenReturn("All");
    when(viewAll.isView()).thenReturn(true);
    when(viewAll.isCustomView()).thenReturn(false);
    when(viewAll.getDisplayFormatId()).thenReturn("0-1-1");
    when(viewAll.getMaximumResultSize()).thenReturn(25);
    when(viewAll.clone()).thenReturn(viewAll);
    when(designWs.findSearches(isNull(), isNull())).thenReturn(List.of());
    stubLoadedViews(List.of(viewAll));
    doReturn(List.of(item("id-a", "Welcome"))).when(adaptor).runDesignSearch(any(PSSearch.class));

    SearchExecuteResult byName = adaptor.executeSearch("View_All", new SearchExecuteRequest());
    assertNotNull(byName);
    assertEquals("View_All", byName.getSearchName());
    assertEquals(1, byName.getChildren().size());

    SearchExecuteResult byLabel = adaptor.executeSearch("All", new SearchExecuteRequest());
    assertNotNull(byLabel);
    assertEquals("View_All", byLabel.getSearchName());
  }

  @Test
  void listSearches_includeViewsMergesViewAll() throws Exception {
    PSSearch search = mockSearch("My Pages", false, true);
    PSSearch viewAll = mockSearch("View_All", false, false);
    when(viewAll.getLabel()).thenReturn("All");
    when(viewAll.isView()).thenReturn(true);
    when(viewAll.isCustomView()).thenReturn(false);
    stubLoadedSearches(List.of(search));
    stubLoadedViews(List.of(viewAll));

    List<com.percussion.rest.searches.SearchDef> developer = adaptor.listSearches(false);
    assertEquals(1, developer.size());
    assertEquals("My Pages", developer.get(0).getName());

    List<com.percussion.rest.searches.SearchDef> explorer = adaptor.listSearches(true);
    assertEquals(2, explorer.size());
    assertTrue(explorer.stream().anyMatch(d -> "View_All".equals(d.getName())));
    assertTrue(explorer.stream().anyMatch(d -> "My Pages".equals(d.getName())));
  }

  @Test
  void listSearches_includeViewsStillReturnsViewsWhenSearchCatalogFails() throws Exception {
    PSSearch viewAll = mockSearch("View_All", false, false);
    when(viewAll.getLabel()).thenReturn("All");
    when(viewAll.isCustomView()).thenReturn(false);
    when(designWs.findSearches(isNull(), isNull()))
        .thenThrow(new IllegalStateException("PSAction missing"));
    stubLoadedViews(List.of(viewAll));

    List<com.percussion.rest.searches.SearchDef> explorer = adaptor.listSearches(true);
    assertEquals(1, explorer.size());
    assertEquals("View_All", explorer.get(0).getName());
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

  @Test
  void findPsSearchByKey_matchesGuidStringAndUntyped() throws Exception {
    PSSearch s = mockSearch("ByGuid", false, true);
    stubLoadedSearches(List.of(s));
    // stubLoadedSearches attaches GUID "0-301-0"; add untyped for dual-key match
    IPSGuid g = s.getGUID();
    when(g.toStringUntyped()).thenReturn("42");

    assertEquals(s, adaptor.findPsSearchByKey("0-301-0"));
    assertEquals(s, adaptor.findPsSearchByKey("42"));
  }

  @Test
  void findPsSearchByKey_skipsBlankGuidString() throws Exception {
    PSSearch s = mockSearch("BlankGuid", false, true);
    stubLoadedSearches(List.of(s));
    IPSGuid g = s.getGUID();
    when(g.toString()).thenReturn("   ");
    when(g.toStringUntyped()).thenReturn("");
    when(s.getId()).thenReturn(99);

    // blank GUID tokens must not match; numeric id still works
    assertNull(adaptor.findPsSearchByKey("   "));
    assertEquals(s, adaptor.findPsSearchByKey("99"));
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
    when(s.getLabel()).thenReturn(name);
    when(s.isCustomSearch()).thenReturn(custom);
    when(s.isCustomView()).thenReturn(false);
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

  private void stubLoadedViews(List<PSSearch> views) throws Exception {
    List<IPSCatalogSummary> summaries = new ArrayList<>();
    for (int i = 0; i < views.size(); i++) {
      IPSGuid g = mock(IPSGuid.class);
      when(g.toString()).thenReturn("0-302-" + i);
      IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
      when(sum.getGUID()).thenReturn(g);
      summaries.add(sum);
      PSSearch s = views.get(i);
      when(s.getGUID()).thenReturn(g);
    }
    when(designWs.findViews(isNull(), isNull())).thenReturn(summaries);
    when(designWs.loadViews(anyList(), eq(false), eq(false), isNull(), isNull())).thenReturn(views);
  }
}
