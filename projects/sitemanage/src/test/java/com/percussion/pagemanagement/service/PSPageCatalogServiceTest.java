/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.pagemanagement.service;

import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.service.impl.PSPageCatalogService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.PSFolderPathUtils;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.test.PSServletTestCase;
import java.util.ArrayList;
import java.util.Properties;
import org.junit.jupiter.api.Tag;

/**
 * @author JaySeletz
 */
@Tag("IntegrationTest")
public class PSPageCatalogServiceTest extends PSServletTestCase {
  private PSSiteDataServletTestCaseFixture fixture;
  private IPSPageCatalogService pageCatalogService;
  private IPSFolderHelper folderHelper;
  private IPSPageService pageService;
  private IPSSystemProperties serviceProps;
  private PSMockSystemProps testProps;

  @Override
  public void setUp() throws Exception {
    PSSpringWebApplicationContextUtils.injectDependencies(this);

    fixture = new PSSiteDataServletTestCaseFixture(request, response);
    fixture.setUp();

    var svcImpl = (PSPageCatalogService) pageCatalogService;
    serviceProps = svcImpl.getSystemProps();

    testProps = new PSMockSystemProps();
    svcImpl.setSystemProps(testProps);
    testProps.setMax("-1");
    // FB:IJU_SETUP_NO_SUPER NC 1-16-16
    super.setUp();
  }

  @Override
  public void tearDown() throws Exception {
    fixture.tearDown();
    var svcImpl = (PSPageCatalogService) pageCatalogService;
    svcImpl.setSystemProps(serviceProps);
  }

  public void testPageCatalog() throws Exception {
    testProps.setMax("-1");

    var siteName = fixture.site1.getName();
    var ids = pageCatalogService.findCatalogPages(siteName);
    assertNotNull(ids);
    assertTrue(ids.isEmpty());

    var href1 = "http://www.test.com/folder1/page1.htm";
    var pageName1 = "page1.htm";
    var folderPath1 = "/folder1";
    var linkText1 = "page1";

    var page1 = catalogPage(siteName, href1, pageName1, folderPath1, linkText1);
    var dupe = addCatalogPage(siteName, href1, pageName1, folderPath1, linkText1);

    assertNull(dupe);

    ids = pageCatalogService.findCatalogPages(siteName);
    assertNotNull(ids);
    assertEquals(1, ids.size());
    assertEquals(page1.getId(), ids.get(0));
    checkSummary(page1.getId(), folderPath1 + "/" + pageName1, linkText1);

    var href2 = "http://www.test.com/folder1/subfolder1/page2.htm";
    var pageName2 = "page2.htm";
    var folderPath2 = "/folder1/subfolder1";
    var linkText2 = "page2";

    var page2 = catalogPage(siteName, href2, pageName2, folderPath2, linkText2);

    var pageNull =
        pageCatalogService.addCatalogPage(
            siteName, "pageNull", linkText2, folderPath2 + "/" + pageName2, href2);
    assertNull(pageNull);

    ids = pageCatalogService.findCatalogPages(siteName);
    assertNotNull(ids);
    assertEquals(2, ids.size());
    assertTrue(ids.contains(page1.getId()));
    assertTrue(ids.contains(page2.getId()));
    checkSummary(page1.getId(), folderPath1 + "/" + pageName1, linkText1);
    checkSummary(page2.getId(), folderPath2 + "/" + pageName2, linkText2);
    checkTemplate(siteName);
    checkCreateImportedPage(page1);

    var realpageName = "realPage.htm";
    var realPath = "/folder2/subfolder2";
    var realhref = "http://www.test.com" + realPath + "/" + realpageName;
    createAndSavePage(realpageName, realPath);
    var dupeRealPage = addCatalogPage(siteName, realhref, realpageName, realPath, realpageName);
    assertNull(dupeRealPage);
  }

  public void testMaxCatalogSetting() throws Exception {
    var siteName = fixture.site1.getName();

    var ids = pageCatalogService.findCatalogPages(siteName);
    assertNotNull(ids);
    assertTrue(ids.isEmpty());

    var svcImpl = (PSPageCatalogService) pageCatalogService;
    var props = new PSMockSystemProps();
    svcImpl.setSystemProps(props);

    var href1 = "http://www.test.com/folder1/page1.htm";
    var pageName1 = "page1.htm";
    var folderPath1 = "/folder1";
    var linkText1 = "page1";

    props.setMax("0");
    svcImpl.setSystemProps(props);
    var page1 = addCatalogPage(siteName, href1, pageName1, folderPath1, linkText1);
    assertNull(page1);

    props.setMax("1");
    svcImpl.setSystemProps(props);
    page1 = addCatalogPage(siteName, href1, pageName1, folderPath1, linkText1);
    assertNotNull(page1);

    var href2 = "http://www.test.com/folder1/subfolder1/page2.htm";
    var pageName2 = "page2.htm";
    var folderPath2 = "/folder1/subfolder1";
    var linkText2 = "page2";
    var page2 = addCatalogPage(siteName, href2, pageName2, folderPath2, linkText2);
    assertNull(page2);

    int max = 5;
    props.setMax(String.valueOf(max));
    svcImpl.setSystemProps(props);
    for (int i = 2; i <= (max + 1); i++) {
      var pageName = "page" + i;
      var folderPath = "/folder" + i + "/subfolder" + i;
      var href = "http://www.test.com" + folderPath + "/" + pageName;
      var linkText = pageName;

      var page = addCatalogPage(siteName, href, pageName, folderPath, linkText);
      if (i <= max) assertNotNull(page);
      else assertNull(page);
    }

    props.setMax("-1");
    svcImpl.setSystemProps(props);
    for (int i = max; i < (max + 5); i++) {
      var pageName = "page" + i;
      var folderPath = "/folder" + i + "/subfolder-" + i;
      var href = "http://www.test.com" + folderPath + "/" + pageName;
      var linkText = pageName;

      var page = addCatalogPage(siteName, href, pageName, folderPath, linkText);
      if (i <= max) assertNotNull(page);
    }
  }

  public void testImportedPages() throws Exception {
    var siteName = fixture.site1.getName();

    var ids = pageCatalogService.findImportedPageIds(siteName);
    assertNotNull(ids);
    assertTrue(ids.isEmpty());

    int max = 5;
    var createdPageIds = new ArrayList<String>();

    for (int i = 1; i <= max; i++) {
      var pageName = "page" + i;
      var folderPath = "/importedPages" + "/subfolder" + i;
      var href = "http://www.test.com" + folderPath + "/" + pageName;
      var linkText = pageName;

      var page = addCatalogPage(siteName, href, pageName, folderPath, linkText);
      assertNotNull(page);
      createdPageIds.add(page.getId());
      assertFalse(pageCatalogService.doesImportedPageExist(page));

      pageCatalogService.createImportedPage(page.getId());
      assertTrue(pageCatalogService.doesImportedPageExist(page));

      assertNull(addCatalogPage(siteName, href, pageName, folderPath, linkText));
    }

    ids = pageCatalogService.findImportedPageIds(siteName);
    assertNotNull(ids);

    for (int i = 0; i < max; i++) {
      assertTrue(ids.contains(createdPageIds.get(i)));
    }
  }

  private PSPage addCatalogPage(
      String siteName, String href, String pageName, String folderPath, String linkText)
      throws Exception {
    var page = pageCatalogService.addCatalogPage(siteName, pageName, linkText, folderPath, href);
    if (page != null) fixture.pageCatalogCleaner.add(page.getId());

    return page;
  }

  private PSPage catalogPage(
      String siteName, String href, String pageName, String folderPath, String linkText)
      throws Exception {
    var page = addCatalogPage(siteName, href, pageName, folderPath, linkText);
    assertNotNull(page);
    assertEquals(pageName, page.getName());
    assertTrue(page.getFolderPath().endsWith(folderPath));
    assertEquals(linkText, page.getLinkTitle());
    assertEquals(href, page.getDescription());
    return page;
  }

  private void checkSummary(String id, String folderPath, String linkText) throws Exception {
    var sum = pageCatalogService.getCatalogPageSummary(id);
    assertNotNull(sum);
    assertEquals(id, sum.getId());
    assertEquals(linkText, sum.getName());
    assertEquals(folderPath, sum.getPath());
  }

  private void checkTemplate(String siteName) throws PSDataServiceException, PSSiteImportException {
    var templateId = pageCatalogService.getCatalogTemplateIdBySite(siteName);
    assertNotNull(templateId);
  }

  private void checkCreateImportedPage(PSPage page) throws Exception {
    var pageId = page.getId();
    pageCatalogService.createImportedPage(pageId);
    var expectedFolderPath = "//Sites/PSSiteDataServletTestCaseFixtureSite/folder1";
    var expectedPath = PSFolderPathUtils.concatPath(expectedFolderPath, page.getName());

    var item = folderHelper.findItemById(pageId);

    assertNotNull(item);

    var newPageFolderPath = item.getFolderPaths().get(0);
    var expectedFullPath = PSFolderPathUtils.concatPath(newPageFolderPath, page.getName());
    assertEquals(expectedPath, expectedFullPath);
    assertTrue(PSPathUtils.doesItemExist(expectedFullPath));
  }

  private PSPage createAndSavePage(String pageName, String folderPath)
      throws PSDataServiceException {
    var templateId = fixture.template1.getId();
    var siteFolderPath = fixture.site1.getFolderPath() + folderPath;
    var linkTitle = "TestLink";
    var noindex = "true";
    var description = "This is a page";

    var pageId =
        createPage(pageName, pageName, templateId, siteFolderPath, linkTitle, noindex, description);
    assertNotNull(pageId);

    var page = pageService.findPage(pageName, siteFolderPath);
    assertNotNull(page);

    return page;
  }

  private String createPage(
      String name,
      String title,
      String templateId,
      String folderPath,
      String linkTitle,
      String noindex,
      String description)
      throws PSDataServiceException {
    var page = new PSPage();
    page.setFolderPath(folderPath);
    page.setName(name);
    page.setTitle(title);
    page.setTemplateId(templateId);
    page.setFolderPath(folderPath);
    page.setLinkTitle(linkTitle);
    page.setNoindex(noindex);
    page.setDescription(description);

    return fixture.createPage(page).getId();
  }

  public void setPageCatalogService(IPSPageCatalogService pageCatalogService) {
    this.pageCatalogService = pageCatalogService;
  }

  public IPSFolderHelper getFolderHelper() {
    return folderHelper;
  }

  public void setFolderHelper(IPSFolderHelper folderHelper) {
    this.folderHelper = folderHelper;
  }

  public void setPageService(IPSPageService pageService) {
    this.pageService = pageService;
  }

  private class PSMockSystemProps extends Properties implements IPSSystemProperties {
    public void setMax(String value) {
      setProperty(CATALOG_PAGE_MAX, value);
    }
  }
}
