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
package com.percussion.pathmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.impl.PSSitePathItemService;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteDataService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Regression for #3457: sample-site {@code /Pages} (and missing {@code /Files})
 * must list children instead of an empty PagedItemList.
 */
class PSSitePathItemServiceVirtualPagesTest {

  @Mock private IPSSiteDataService siteDataService;
  @Mock private IPSFolderHelper folderHelper;

  private TestableSitePathItemService service;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    when(folderHelper.concatPath(anyString(), anyString()))
        .thenAnswer(
            inv -> {
              String start = inv.getArgument(0);
              String end = inv.getArgument(1);
              if (end == null || end.isEmpty() || "/".equals(end)) {
                return start;
              }
              String rel = end.startsWith("/") ? end.substring(1) : end;
              if (rel.endsWith("/")) {
                rel = rel.substring(0, rel.length() - 1);
              }
              if (start.endsWith("/")) {
                return start + rel;
              }
              return start + "/" + rel;
            });
    service = new TestableSitePathItemService(siteDataService, folderHelper);
  }

  @Test
  void pagesSegmentHelpersRecognizeChromeNames() {
    assertTrue(PSSitePathItemService.isPagesSegment("Pages"));
    assertTrue(PSSitePathItemService.isPagesSegment("pages"));
    assertTrue(PSSitePathItemService.isFilesSegment("Files"));
    assertFalse(PSSitePathItemService.isPagesSegment("About"));
    assertEquals("Pages", PSSitePathItemService.trimFolderSegments("/Pages/"));
    assertEquals("", PSSitePathItemService.trimFolderSegments("/"));
  }

  @Test
  void listingSitenamePagesReturnsSiteRootPageChild() throws Exception {
    stubSampleSite();
    var items = service.exposeFindItems("/Corporate_Investments/Pages/");
    assertFalse(items.isEmpty(), "Pages listing must not be empty");
    assertTrue(
        items.stream().anyMatch(i -> "Home".equals(i.getName()) && !i.isFolder()),
        "Pages listing must include a page-type child; got "
            + items.stream().map(PSPathItem::getName).toList());
  }

  @Test
  void listingSitenameFilesReturnsFilesFolderChild() throws Exception {
    stubSampleSite();
    var items = service.exposeFindItems("/Corporate_Investments/Files/");
    assertFalse(items.isEmpty(), "Files listing must not be empty");
    assertTrue(
        items.stream().anyMatch(i -> "annual-report.pdf".equals(i.getName())),
        "Files listing must include a file child; got "
            + items.stream().map(PSPathItem::getName).toList());
  }

  @Test
  void listingSiteRootInjectsPagesChrome() throws Exception {
    stubSampleSite();
    var items = service.exposeFindItems("/Corporate_Investments/");
    assertTrue(
        items.stream().anyMatch(i -> PSSitePathItemService.isPagesSegment(i.getName())),
        "Site listing must expose Pages chrome; got "
            + items.stream().map(PSPathItem::getName).toList());
    assertTrue(
        items.stream().anyMatch(i -> "AboutCorporateInvestments".equals(i.getName())),
        "Site listing must still include real FastForward folders");
  }

  @Test
  void emptyPhysicalPagesFolderStillListsSitePages() throws Exception {
    stubSiteSummary();
    var siteFolder = folder("523", "CorporateInvestments");
    var emptyPages = folder("350", "Pages");
    var home = page("700", "Home", "rffHome");
    when(folderHelper.findItems(anyString(), anyBoolean())).thenReturn(List.of());
    when(folderHelper.findItems("//Sites")).thenReturn(List.of(siteFolder));
    when(folderHelper.findChildItems("523")).thenReturn(List.of(emptyPages, home));
    when(folderHelper.findChildItems("350")).thenReturn(List.of());

    var items = service.exposeFindItems("/Corporate_Investments/Pages/");
    assertTrue(
        items.stream().anyMatch(i -> "Home".equals(i.getName()) && !i.isFolder()),
        "Empty physical Pages folder must fall back to site pages; got "
            + items.stream().map(PSPathItem::getName).toList());
  }

  @Test
  void pagesListingFallsBackToSectionFolderPages() throws Exception {
    stubSampleSiteWithoutRootPage();
    var items = service.exposeFindItems("/Corporate_Investments/Pages/");
    assertTrue(
        items.stream().anyMatch(i -> "AboutPage".equals(i.getName()) && !i.isFolder()),
        "Empty site-root Pages must flatten a page from About…; got "
            + items.stream().map(PSPathItem::getName).toList());
  }

  private void stubSampleSite() throws Exception {
    stubSiteSummary();
    var siteFolder = folder("523", "CorporateInvestments");
    var about = folder("600", "AboutCorporateInvestments");
    var files = folder("601", "Files");
    var home = page("700", "Home", "rffHome");
    var pdf = item("701", "annual-report.pdf", "rffFile", false);

    when(folderHelper.findItems(anyString(), anyBoolean())).thenReturn(List.of());
    when(folderHelper.findItems("//Sites")).thenReturn(List.of(siteFolder));
    when(folderHelper.findChildItems("523")).thenReturn(List.of(about, files, home));
    when(folderHelper.findChildItems("600")).thenReturn(List.of(page("702", "AboutPage", "rffGeneric")));
    when(folderHelper.findChildItems("601")).thenReturn(List.of(pdf));
  }

  private void stubSampleSiteWithoutRootPage() throws Exception {
    stubSiteSummary();
    var siteFolder = folder("523", "CorporateInvestments");
    var about = folder("600", "AboutCorporateInvestments");
    var files = folder("601", "Files");

    when(folderHelper.findItems(anyString(), anyBoolean())).thenReturn(List.of());
    when(folderHelper.findItems("//Sites")).thenReturn(List.of(siteFolder));
    when(folderHelper.findChildItems("523")).thenReturn(List.of(about, files));
    when(folderHelper.findChildItems("600")).thenReturn(List.of(page("702", "AboutPage", "rffGeneric")));
    when(folderHelper.findChildItems("601")).thenReturn(List.of());
  }

  private void stubSiteSummary() throws Exception {
    var site = new PSSiteSummary();
    site.setName("Corporate_Investments");
    site.setId("site-ci");
    site.setFolderPath("//Sites/CorporateInvestments");
    when(siteDataService.findByPath(anyString())).thenReturn(site);
  }

  private static PSDataItemSummary folder(String id, String name) {
    return item(id, name, "Folder", true);
  }

  private static PSDataItemSummary page(String id, String name, String type) {
    var sum = item(id, name, type, false);
    sum.setCategory(IPSItemSummary.Category.PAGE);
    return sum;
  }

  private static PSDataItemSummary item(String id, String name, String type, boolean folder) {
    var sum = new PSDataItemSummary();
    sum.setId(id);
    sum.setName(name);
    sum.setType(type);
    sum.setCategory(folder ? IPSItemSummary.Category.FOLDER : IPSItemSummary.Category.RESOURCE);
    return sum;
  }

  static final class TestableSitePathItemService extends PSSitePathItemService {
    TestableSitePathItemService(IPSSiteDataService sites, IPSFolderHelper folders) {
      super(sites, folders, null, null, null, null, null, null, null, null, null, null);
    }

    List<PSPathItem> exposeFindItems(String path) throws Exception {
      return findItems(path);
    }
  }
}
