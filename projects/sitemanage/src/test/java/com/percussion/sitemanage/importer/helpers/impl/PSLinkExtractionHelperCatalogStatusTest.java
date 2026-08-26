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
package com.percussion.sitemanage.importer.helpers.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.service.IPSPageCatalogService;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.PSLink;
import com.percussion.sitemanage.importer.PSSiteImportLogger;
import com.percussion.theme.service.IPSThemeService;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * {@link PSLinkExtractionHelper#catalogPage} catalogs only HTTP 200 responses using the JDK
 * protocol constant (issue #3846).
 */
@Tag("UnitTest")
class PSLinkExtractionHelperCatalogStatusTest {

  @Test
  void isSuccessfulCatalogHttpStatusIsHttpOkOnly() {
    assertTrue(
        PSLinkExtractionHelper.isSuccessfulCatalogHttpStatus(HttpURLConnection.HTTP_OK));
    assertFalse(
        PSLinkExtractionHelper.isSuccessfulCatalogHttpStatus(
            HttpURLConnection.HTTP_MOVED_PERM));
    assertFalse(
        PSLinkExtractionHelper.isSuccessfulCatalogHttpStatus(HttpURLConnection.HTTP_NOT_FOUND));
    assertEqualsHttpOk();
  }

  @Test
  void catalogPageSkipsNonOkStatusWithoutCataloging() {
    PSLinkExtractionHelper helper = newHelper();
    PSSiteImportCtx ctx = newCtx(false);
    PSLink link = PSLink.createLinkWithoutElementReference("/a", "A", "http://example.test/a", "a");
    PSSiteImportLogger logger = new PSSiteImportLogger(PSLogObjectType.SITE);

    assertFalse(
        helper.catalogPage(ctx, logger, link, HttpURLConnection.HTTP_NOT_FOUND, "http://example.test/a"));
    assertFalse(
        helper.catalogPage(
            ctx, logger, link, HttpURLConnection.HTTP_MOVED_PERM, "http://example.test/a"));
  }

  @Test
  void catalogPageOkBranchHonorsCanceledImport() {
    PSLinkExtractionHelper helper = newHelper();
    PSSiteImportCtx ctx = newCtx(true);
    PSLink link = PSLink.createLinkWithoutElementReference("/a", "A", "http://example.test/a", "a");
    PSSiteImportLogger logger = new PSSiteImportLogger(PSLogObjectType.SITE);

    assertFalse(
        helper.catalogPage(ctx, logger, link, HttpURLConnection.HTTP_OK, "http://example.test/a"));
  }

  private static PSLinkExtractionHelper newHelper() {
    return new PSLinkExtractionHelper(
        mock(IPSPageCatalogService.class),
        mock(IPSThemeService.class),
        mock(IPSPageDao.class),
        mock(IPSItemWorkflowService.class),
        mock(IPSFolderHelper.class));
  }

  private static void assertEqualsHttpOk() {
    org.junit.jupiter.api.Assertions.assertEquals(200, HttpURLConnection.HTTP_OK);
  }

  private static PSSiteImportCtx newCtx(boolean canceled) {
    PSSite site = new PSSite();
    site.setName("example");
    PSSiteImportCtx ctx = new PSSiteImportCtx();
    ctx.setSite(site);
    ctx.setCanceled(canceled);
    return ctx;
  }
}
