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
import com.percussion.rest.views.ViewExecuteRequest;
import com.percussion.rest.views.ViewExecuteResult;
import com.percussion.rest.views.ViewResultItem;
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
class ViewAdaptorExecuteTest {

  private IPSUiDesignWs designWs;
  private IPSFolderHelper folderHelper;
  private IPSIdMapper idMapper;
  private ViewAdaptor adaptor;

  @BeforeEach
  void setUp() {
    designWs = mock(IPSUiDesignWs.class);
    folderHelper = mock(IPSFolderHelper.class);
    idMapper = mock(IPSIdMapper.class);
    adaptor = spy(new ViewAdaptor(designWs, folderHelper, idMapper));
  }

  @Test
  void normalizeRequest_nullBecomesEmpty() {
    ViewExecuteRequest n = ViewAdaptor.normalizeRequest(null);
    assertNotNull(n);
    assertNull(n.getStartIndex());
    assertNull(n.getMaxResults());
  }

  @Test
  void normalizeRequest_rejectsBadStartIndex() {
    ViewExecuteRequest r = new ViewExecuteRequest();
    r.setStartIndex(0);
    assertThrows(IllegalArgumentException.class, () -> ViewAdaptor.normalizeRequest(r));
  }

  @Test
  void normalizeRequest_rejectsBadMaxResults() {
    ViewExecuteRequest r = new ViewExecuteRequest();
    r.setMaxResults(0);
    assertThrows(IllegalArgumentException.class, () -> ViewAdaptor.normalizeRequest(r));
  }

  @Test
  void normalizeRequest_rejectsBadSortOrder() {
    ViewExecuteRequest r = new ViewExecuteRequest();
    r.setSortOrder("sideways");
    assertThrows(IllegalArgumentException.class, () -> ViewAdaptor.normalizeRequest(r));
  }

  @Test
  void normalizeRequest_normalizesSortOrderCase() {
    ViewExecuteRequest r = new ViewExecuteRequest();
    r.setSortOrder("DESC");
    assertEquals("desc", ViewAdaptor.normalizeRequest(r).getSortOrder());
  }

  @Test
  void applyExecuteOverrides_folderAndMax() {
    PSSearch s = mock(PSSearch.class);
    when(s.getMaximumResultSize()).thenReturn(100);
    when(s.getProperty(PSSearch.PROP_FOLDER_PATH_RECURSE)).thenReturn(null);

    ViewExecuteRequest r = new ViewExecuteRequest();
    r.setFolderPath("//Sites/Demo");
    r.setMaxResults(10);
    ViewAdaptor.applyExecuteOverrides(s, r);

    verify(s).setProperty(PSSearch.PROP_FOLDER_PATH, "//Sites/Demo");
    verify(s).setProperty(PSSearch.PROP_FOLDER_PATH_RECURSE, "true");
    verify(s).setMaximumNumber(10);
  }

  @Test
  void applyExecuteOverrides_defaultsUnlimitedMax() {
    PSSearch s = mock(PSSearch.class);
    when(s.getMaximumResultSize()).thenReturn(0);
    ViewAdaptor.applyExecuteOverrides(s, new ViewExecuteRequest());
    verify(s).setMaximumNumber(ViewAdaptor.DEFAULT_PAGE_SIZE);
  }

  @Test
  void sortItems_byTitleDesc() {
    List<ViewResultItem> items = new ArrayList<>();
    items.add(item("1", "Alpha"));
    items.add(item("2", "Charlie"));
    items.add(item("3", "Bravo"));
    ViewAdaptor.sortItems(items, "sys_title", "desc");
    assertEquals("Charlie", items.get(0).getTitle());
    assertEquals("Bravo", items.get(1).getTitle());
    assertEquals("Alpha", items.get(2).getTitle());
  }

  @Test
  void executeView_unsafeKeyReturnsNull() {
    assertNull(adaptor.executeView("../x", new ViewExecuteRequest()));
    assertNull(adaptor.executeView("a/b", null));
    assertNull(adaptor.executeView(null, null));
  }

  @Test
  void executeView_missingReturnsNull() throws Exception {
    when(designWs.findViews(isNull(), isNull())).thenReturn(List.of());
    assertNull(adaptor.executeView("Missing", new ViewExecuteRequest()));
  }

  @Test
  void executeView_customUrlThrows400Style() throws Exception {
    PSSearch custom = mockView("Inbox", true, false);
    stubLoadedViews(List.of(custom));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.executeView("Inbox", new ViewExecuteRequest()));
    assertTrue(ex.getMessage().toLowerCase().contains("custom"));
    assertTrue(ex.getMessage().contains("#3118"));
  }

  @Test
  void executeView_happyPathPagesAndMaps() throws Exception {
    PSSearch design = mockView("All Content", false, true);
    when(design.getDisplayFormatId()).thenReturn("0-1-1");
    when(design.getMaximumResultSize()).thenReturn(100);
    when(design.clone()).thenReturn(design);
    stubLoadedViews(List.of(design));

    ViewResultItem a = item("id-a", "A");
    ViewResultItem b = item("id-b", "B");
    ViewResultItem c = item("id-c", "C");
    doReturn(List.of(a, b, c)).when(adaptor).runDesignView(any(PSSearch.class));

    ViewExecuteRequest req = new ViewExecuteRequest();
    req.setStartIndex(2);
    req.setMaxResults(1);
    ViewExecuteResult result = adaptor.executeView("All Content", req);

    assertNotNull(result);
    assertEquals("All Content", result.getViewName());
    assertEquals("0-1-1", result.getDisplayFormatId());
    assertEquals(3, result.getTotalCount());
    assertEquals(2, result.getStartIndex());
    assertEquals(1, result.getChildren().size());
    assertEquals("B", result.getChildren().get(0).getTitle());
  }

  @Test
  void executeView_emptyResults() throws Exception {
    PSSearch design = mockView("Empty", false, true);
    when(design.getDisplayFormatId()).thenReturn("0-1-1");
    when(design.getMaximumResultSize()).thenReturn(25);
    when(design.clone()).thenReturn(design);
    stubLoadedViews(List.of(design));
    doReturn(List.of()).when(adaptor).runDesignView(any(PSSearch.class));

    ViewExecuteResult result = adaptor.executeView("Empty", null);
    assertNotNull(result);
    assertEquals(0, result.getTotalCount());
    assertTrue(result.getChildren().isEmpty());
    assertEquals(1, result.getStartIndex());
  }

  @Test
  void executeView_loadsViaFindViewsNotFindSearches() throws Exception {
    PSSearch design = mockView("Community Content", false, true);
    when(design.getDisplayFormatId()).thenReturn("0-1-1");
    when(design.getMaximumResultSize()).thenReturn(25);
    when(design.clone()).thenReturn(design);
    stubLoadedViews(List.of(design));
    doReturn(List.of()).when(adaptor).runDesignView(any(PSSearch.class));

    assertNotNull(adaptor.executeView("Community Content", new ViewExecuteRequest()));
    verify(designWs).findViews(isNull(), isNull());
    verify(designWs).loadViews(anyList(), eq(false), eq(false), isNull(), isNull());
  }

  @Test
  void mapSearchResponse_skipsNullMappedRowsAndKeepsItems() {
    IPSSearchResultRow blank = mock(IPSSearchResultRow.class);
    IPSSearchResultRow ok = mock(IPSSearchResultRow.class);

    doReturn(null).when(adaptor).mapResultRow(blank);
    ViewResultItem kept = item("guid-42", "Title");
    kept.setFolderPath("/Sites/Demo");
    kept.setType("Page");
    doReturn(kept).when(adaptor).mapResultRow(ok);

    PSWSSearchResponse response = mock(PSWSSearchResponse.class);
    when(response.getRows()).thenReturn(List.of(blank, ok).iterator());

    List<ViewResultItem> mapped = adaptor.mapSearchResponse(response);
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
  void isSafeViewKey_stillHoldsForExecutePath() {
    assertTrue(ViewAdaptor.isSafeViewKey("All Content"));
    assertFalse(ViewAdaptor.isSafeViewKey("a\\b"));
  }

  @Test
  void findPsViewByKey_matchesGuidStringAndUntyped() throws Exception {
    PSSearch s = mockView("ByGuid", false, true);
    stubLoadedViews(List.of(s));
    IPSGuid g = s.getGUID();
    when(g.toStringUntyped()).thenReturn("42");

    assertEquals(s, adaptor.findPsViewByKey("0-301-0"));
    assertEquals(s, adaptor.findPsViewByKey("42"));
  }

  @Test
  void findPsViewByKey_skipsBlankGuidString() throws Exception {
    PSSearch s = mockView("BlankGuid", false, true);
    stubLoadedViews(List.of(s));
    IPSGuid g = s.getGUID();
    when(g.toString()).thenReturn("   ");
    when(g.toStringUntyped()).thenReturn("");
    when(s.getId()).thenReturn(99);

    assertNull(adaptor.findPsViewByKey("   "));
    assertEquals(s, adaptor.findPsViewByKey("99"));
  }

  private static ViewResultItem item(String id, String title) {
    ViewResultItem i = new ViewResultItem();
    i.setId(id);
    i.setName(title);
    i.setTitle(title);
    return i;
  }

  private PSSearch mockView(String name, boolean custom, boolean standard) {
    PSSearch s = mock(PSSearch.class);
    when(s.getName()).thenReturn(name);
    when(s.isCustomView()).thenReturn(custom);
    when(s.isStandardView()).thenReturn(standard);
    when(s.isView()).thenReturn(true);
    when(s.getId()).thenReturn(10);
    when(s.getGUID()).thenReturn(null);
    return s;
  }

  private void stubLoadedViews(List<PSSearch> views) throws Exception {
    List<IPSCatalogSummary> summaries = new ArrayList<>();
    List<IPSGuid> guids = new ArrayList<>();
    for (int i = 0; i < views.size(); i++) {
      IPSGuid g = mock(IPSGuid.class);
      when(g.toString()).thenReturn("0-301-" + i);
      IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
      when(sum.getGUID()).thenReturn(g);
      summaries.add(sum);
      guids.add(g);
      PSSearch s = views.get(i);
      when(s.getGUID()).thenReturn(g);
    }
    when(designWs.findViews(isNull(), isNull())).thenReturn(summaries);
    when(designWs.loadViews(anyList(), eq(false), eq(false), isNull(), isNull())).thenReturn(views);
  }
}
