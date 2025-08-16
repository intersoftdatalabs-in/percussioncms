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

import com.percussion.share.service.IPSDataService;
import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.PSSiteImportLogger;
import com.percussion.sitemanage.importer.helpers.impl.PSThemeHelper;
import com.percussion.theme.service.impl.PSThemeService;
import java.io.File;
import org.junit.jupiter.api.*;

class PSThemeHelperTest {

  private static final String SITE_NAME = "siteName";
  private static final String SITE_NAME_2 = "www.someDomain.com";
  private static final String SITE_NAME_2_TRANSFORMED = "www-someDomain-com";
  private static final String SITE_NAME_3 = "www.some-new-Domain.com";
  private static final String SITE_NAME_3_TRANSFORMED = "www-some-new-Domain-com";
  private static final String SITE_NAME_ROLLBACK = "siteNameRollback";
  private static final String WEB_RESOURCES_ROOT =
      "src/test/resources/importer/data/web_resources/themes";
  private static final String RX_RESOURCES_ROOT =
      "src/test/resources/importer/data/rx_resources/default_theme";

  private IPSSiteImportLogger logger = new PSSiteImportLogger(PSLogObjectType.SITE);
  private PSThemeService themeService;
  private PSThemeHelper themeHelper;

  @BeforeEach
  void setUp() {
    themeService = new PSThemeService();
    themeService.setDefaultThemeRootDirectory(RX_RESOURCES_ROOT);
    themeService.setThemesRootDirectory(WEB_RESOURCES_ROOT);
    themeService.setThemesRootRelativeUrl(WEB_RESOURCES_ROOT);
    themeHelper = new PSThemeHelper(themeService);
    themeHelper.setThemesRootDirectory(WEB_RESOURCES_ROOT);
  }

  @AfterEach
  void tearDown()
      throws IPSDataService.DataServiceDeleteException,
          IPSDataService.DataServiceNotFoundException {
    themeService.delete(SITE_NAME);
    themeService.delete(SITE_NAME_2_TRANSFORMED);
    themeService.delete(SITE_NAME_3_TRANSFORMED);
    themeService.delete(SITE_NAME_3_TRANSFORMED + "-1");
    themeService.delete(SITE_NAME_3_TRANSFORMED + "-2");
    themeService.delete(SITE_NAME_ROLLBACK);
  }

  @Test
  void testProcess() throws Exception {
    var pageContent = new PSPageContent();
    var importContext = new PSSiteImportCtx();
    var site = new PSSite();
    site.setName(SITE_NAME);
    importContext.setSite(site);
    importContext.setLogger(logger);

    themeHelper.process(pageContent, importContext);
    assertEquals(importContext.getThemeSummary().getName(), SITE_NAME);
    var imageFile =
        new File(
            WEB_RESOURCES_ROOT + "/" + importContext.getThemeSummary().getName(),
            SITE_NAME + ".png");
    var cssFile =
        new File(
            WEB_RESOURCES_ROOT + "/" + importContext.getThemeSummary().getName(),
            SITE_NAME + ".css");
    assertTrue(imageFile.exists());
    assertTrue(cssFile.exists());
  }

  @Test
  void testProcessDotName() throws Exception {
    var pageContent = new PSPageContent();
    var importContext = new PSSiteImportCtx();
    var site = new PSSite();
    site.setName(SITE_NAME_2);
    importContext.setSite(site);
    importContext.setLogger(logger);

    themeHelper.process(pageContent, importContext);
    assertEquals(importContext.getThemeSummary().getName(), SITE_NAME_2_TRANSFORMED);
    var imageFile =
        new File(
            WEB_RESOURCES_ROOT + "/" + importContext.getThemeSummary().getName(),
            SITE_NAME_2_TRANSFORMED + ".png");
    var cssFile =
        new File(
            WEB_RESOURCES_ROOT + "/" + importContext.getThemeSummary().getName(),
            SITE_NAME_2_TRANSFORMED + ".css");
    assertTrue(imageFile.exists());
    assertTrue(cssFile.exists());
  }

  @Test
  void testProcessDotNameCollision() throws Exception {
    var dirFile = new File(WEB_RESOURCES_ROOT + "/" + SITE_NAME_3_TRANSFORMED);
    var dirFile2 = new File(WEB_RESOURCES_ROOT + "/" + SITE_NAME_3_TRANSFORMED + "-2");
    assertTrue(dirFile.mkdir());
    assertTrue(dirFile2.mkdir());

    var pageContent = new PSPageContent();
    var importContext = new PSSiteImportCtx();
    var site = new PSSite();
    site.setName(SITE_NAME_3);
    importContext.setSite(site);
    importContext.setLogger(logger);

    themeHelper.process(pageContent, importContext);
    assertEquals(importContext.getThemeSummary().getName(), SITE_NAME_3_TRANSFORMED + "-1");
    var imageFile =
        new File(
            WEB_RESOURCES_ROOT + "/" + importContext.getThemeSummary().getName(),
            SITE_NAME_3_TRANSFORMED + ".png");
    var cssFile =
        new File(
            WEB_RESOURCES_ROOT + "/" + importContext.getThemeSummary().getName(),
            SITE_NAME_3_TRANSFORMED + ".css");
    assertTrue(imageFile.exists());
    assertTrue(cssFile.exists());
  }

  @Test
  void testRollback() throws Exception {
    var pageContent = new PSPageContent();
    var importContext = new PSSiteImportCtx();
    var site = new PSSite();
    site.setName(SITE_NAME_ROLLBACK);
    importContext.setSite(site);
    importContext.setLogger(logger);

    themeHelper.process(pageContent, importContext);
    assertEquals(importContext.getThemeSummary().getName(), SITE_NAME_ROLLBACK);
    var imageFile =
        new File(
            WEB_RESOURCES_ROOT + "/" + importContext.getThemeSummary().getName(),
            SITE_NAME_ROLLBACK + ".png");
    var cssFile =
        new File(
            WEB_RESOURCES_ROOT + "/" + importContext.getThemeSummary().getName(),
            SITE_NAME_ROLLBACK + ".css");
    assertTrue(imageFile.exists());
    assertTrue(cssFile.exists());

    themeHelper.rollback(pageContent, importContext);

    var dirRollbackFile =
        new File(WEB_RESOURCES_ROOT + "/" + importContext.getThemeSummary().getName());
    assertFalse(dirRollbackFile.exists());
  }
}
