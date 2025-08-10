// REFACTORED: CP-JAVA11
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

package com.percussion.assetmanagement.service;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.assetmanagement.service.impl.PSAssetNewFolderPathResolver;
import com.percussion.assetmanagement.service.impl.PSAssetNewFolderPathResolver.PSResolvedFolderPath;
import com.percussion.assetmanagement.service.impl.PSAssetNewFolderPathResolver.PSResolvedFolderPath.PSResolvedFolderPathType;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import java.util.List;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Scenario description: Tests for PSAssetNewFolderPathResolver. */
public class PSAssetNewFolderPathResolverTest {

  private Mockery context = new JUnit4Mockery();

  private PSAssetNewFolderPathResolver resolver;

  private IPSSiteTemplateService siteTemplateService;
  private IPSPageService pageService;

  private PSResolvedFolderPath resolved;
  private IPSItemSummary owner;
  private IPSItemSummary asset;
  private IPSItemSummary pageItem;
  private PSPage page;
  private IPSItemSummary template;
  private PSSiteSummary siteSummary;

  @BeforeEach
  public void setUp() throws Exception {
    siteTemplateService = context.mock(IPSSiteTemplateService.class);
    pageService = context.mock(IPSPageService.class);
    resolver = new PSAssetNewFolderPathResolver(pageService, siteTemplateService);
    pageItem = createItemSummary("p", IPSPageService.PAGE_CONTENT_TYPE, asList("//a/b"));
    template = createItemSummary("t", IPSTemplateService.TPL_CONTENT_TYPE, asList("//a/b"));
    // Asset in site A and site B
    asset = createItemSummary("a", "asset", asList("//a/b", "//c/b"));
    page = new PSPage();
    page.setId("p");
    page.setFolderPath("//a/d");
    page.setTemplateId("t");

    siteSummary = new PSSiteSummary();
    siteSummary.setId("s");
    siteSummary.setFolderPath("//a");
  }

  @Test
  public void shouldResolveForPageUsingAssetsPath() throws Exception {
    owner = pageItem;
    context.checking(
        new Expectations() {
          {
            oneOf(pageService).find("p");
            will(returnValue(page));
            oneOf(siteTemplateService).findSitesByTemplate("t");
            will(returnValue(asList(siteSummary)));
          }
        });

    resolved = resolver.resolveFolderPath(owner, asset);

    assertNotNull(resolved);
    assertEquals("//a/b", resolved.getFolderPath());
    assertEquals(PSResolvedFolderPathType.PAGE, resolved.getType());
    assertTrue(resolved.isAlreadyInFolder());
  }

  @Test
  public void shouldResolveForPageUsingSitePath() throws Exception {
    pageItem = createItemSummary("blah", IPSPageService.PAGE_CONTENT_TYPE, asList("//SITE/blah"));
    owner = pageItem;
    siteSummary.setFolderPath("//SITE");
    context.checking(
        new Expectations() {
          {
            oneOf(pageService).find("blah");
            will(returnValue(page));
            oneOf(siteTemplateService).findSitesByTemplate("t");
            will(returnValue(asList(siteSummary)));
          }
        });

    resolved = resolver.resolveFolderPath(owner, asset);

    assertNotNull(resolved);
    assertEquals("//a/d", resolved.getFolderPath());
    assertEquals(PSResolvedFolderPathType.PAGE, resolved.getType());
    assertFalse(resolved.isAlreadyInFolder());
  }

  @Test
  public void shouldResolveForTemplateUsingAssetsPath() throws Exception {
    owner = template;
    context.checking(
        new Expectations() {
          {
            oneOf(siteTemplateService).findSitesByTemplate("t");
            will(returnValue(asList(siteSummary)));
          }
        });

    resolved = resolver.resolveFolderPath(owner, asset);

    assertNotNull(resolved);
    assertEquals("//a/b", resolved.getFolderPath());
    assertEquals(PSResolvedFolderPathType.TEMPLATE, resolved.getType());
    assertTrue(resolved.isAlreadyInFolder());
  }

  @Test
  public void shouldResolveForTemplateUsingSitePath() throws Exception {
    owner = template;
    siteSummary.setFolderPath("//SITE");
    context.checking(
        new Expectations() {
          {
            oneOf(siteTemplateService).findSitesByTemplate("t");
            will(returnValue(asList(siteSummary)));
          }
        });

    resolved = resolver.resolveFolderPath(owner, asset);

    assertNotNull(resolved);
    assertEquals("//SITE", resolved.getFolderPath());
    assertEquals(PSResolvedFolderPathType.TEMPLATE, resolved.getType());
    assertFalse(resolved.isAlreadyInFolder());
  }

  private IPSItemSummary createItemSummary(
      final String id, final String type, final List<String> paths) {
    final IPSItemSummary item = context.mock(IPSItemSummary.class, id);
    context.checking(
        new Expectations() {
          {
            allowing(item).getId();
            will(returnValue(id));
            allowing(item).getType();
            will(returnValue(type));
            allowing(item).getFolderPaths();
            will(returnValue(paths));
          }
        });
    return item;
  }
}
