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
// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.importer.helpers;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pagemanagement.service.PSSiteDataServletTestCaseFixture;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.sitemanage.dao.impl.PSSiteContentDao;
import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.PSSiteImportLogger;
import com.percussion.sitemanage.importer.helpers.impl.PSThemeTemplateHelper;
import com.percussion.theme.service.IPSThemeService;
import org.junit.jupiter.api.*;

@IntegrationTest
class PSThemeTemplateHelperTest {

  private IPSSiteImportLogger logger = new PSSiteImportLogger(PSLogObjectType.SITE);
  private PSSiteDataServletTestCaseFixture fixture;
  private String themeName = "";
  private String renamedLandingPage = "renamedIndex";
  private IPSThemeService themeService;
  private IPSTemplateService templateService;
  private PSThemeTemplateHelper themeTemplateHelper;
  private IPSSiteManager siteMgr;
  private IPSPageService pageService;
  private IPSItemWorkflowService itemWorkflowService;

  @BeforeEach
  void setUp() throws Exception {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
    fixture = new PSSiteDataServletTestCaseFixture(request, response);
    fixture.setUp();
    fixture.pageCleaner.add(fixture.site1.getFolderPath() + "/Page1");

    var templateObj = templateService.load(fixture.baseTemplate.getId());
    themeName = templateObj.getTheme();
  }

  @AfterEach
  void tearDown() throws Exception {
    fixture.tearDown();
  }

  @Test
  void testProcess() throws Exception {
    var pageContent = new PSPageContent();
    var importContext = new PSSiteImportCtx();

    var site = new PSSite();
    site.setName(fixture.site1.getName());
    site.setSiteId(fixture.site1.getSiteId());

    importContext.setSite(site);
    importContext.setLogger(logger);

    themeTemplateHelper.process(pageContent, importContext);
    assertEquals(importContext.getThemeSummary().getName(), themeName);

    var landingPage =
        pageService.findPage(PSSiteContentDao.HOME_PAGE_NAME, fixture.site1.getFolderPath());

    if (landingPage != null) {
      itemWorkflowService.checkOut(landingPage.getId());
      landingPage.setName(renamedLandingPage);
      pageService.save(landingPage);
    }

    landingPage = pageService.findPage(renamedLandingPage, fixture.site1.getFolderPath());
    if (landingPage != null) {
      assertEquals(landingPage.getName(), renamedLandingPage);
    }

    themeTemplateHelper.process(pageContent, importContext);
    assertEquals(importContext.getThemeSummary().getName(), themeName);
  }

  // Dependency injection setters for test context
  public void setThemeService(IPSThemeService themeService) {
    this.themeService = themeService;
  }

  public void setTemplateService(IPSTemplateService templateService) {
    this.templateService = templateService;
  }

  public void setThemeTemplateHelper(PSThemeTemplateHelper themeTemplateHelper) {
    this.themeTemplateHelper = themeTemplateHelper;
  }

  public void setSiteMgr(IPSSiteManager siteMgr) {
    this.siteMgr = siteMgr;
  }

  public void setPageService(IPSPageService pageService) {
    this.pageService = pageService;
  }

  public void setItemWorkflowService(IPSItemWorkflowService itemWorkflowService) {
    this.itemWorkflowService = itemWorkflowService;
  }
}
