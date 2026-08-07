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
package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.pagemanagement.assembler.IPSRenderLinkContextFactory;
import com.percussion.pagemanagement.assembler.impl.PSLegacyLinkGenerator;
import com.percussion.pagemanagement.assembler.impl.PSResourceInstanceHelper;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSResourceDefinitionService;
import com.percussion.services.linkmanagement.IPSManagedLinkDao;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.theme.service.impl.PSThemeService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Behavioral unit coverage for page/asset inline link title resolve wiring on {@link
 * PSRenderLinkService} (#2242 / parent #946). Pure resolver chain is covered by {@link
 * PSInlineLinkTitleResolverTest}; Playwright residual is #2243.
 */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class PSRenderLinkServiceInlineTitleTest {

  @Mock private IPSIdMapper idMapper;
  @Mock private PSLegacyLinkGenerator legacyLinkGenerator;
  @Mock private IPSPageService pageService;
  @Mock private IPSRenderLinkContextFactory renderLinkContextFactory;
  @Mock private IPSResourceDefinitionService resourceDefinitionService;
  @Mock private PSResourceInstanceHelper resourceInstanceHelper;
  @Mock private PSThemeService themeService;
  @Mock private IPSManagedLinkDao managedLinkDao;

  private PSRenderLinkService service;

  @BeforeEach
  void setUp() {
    service =
        new PSRenderLinkService(
            idMapper,
            legacyLinkGenerator,
            pageService,
            renderLinkContextFactory,
            resourceDefinitionService,
            resourceInstanceHelper,
            themeService,
            managedLinkDao);
  }

  @Test
  @DisplayName("buildPageTitleFieldMap maps DTO getters to PSPageDao field names")
  void buildPageTitleFieldMap_usesSharedFieldNameConstants() {
    PSPage page = new PSPage();
    page.setLinkTitle("Nav Title");
    page.setTitle("Browser Title");
    page.setName("sys-name");
    page.setDescription("desc");
    page.setSummary("sum");
    page.setAuthor("author");

    Map<String, Object> fields = PSRenderLinkService.buildPageTitleFieldMap(page);

    assertEquals("Nav Title", fields.get(PSInlineLinkTitleResolver.PAGE_DEFAULT_TITLE_FIELD));
    assertEquals("Browser Title", fields.get(PSInlineLinkTitleResolver.PAGE_TITLE_FIELD));
    assertEquals("sys-name", fields.get(PSInlineLinkTitleResolver.SYS_TITLE_FIELD));
    assertEquals("desc", fields.get(PSInlineLinkTitleResolver.PAGE_DESCRIPTION_FIELD));
    assertEquals("sum", fields.get(PSInlineLinkTitleResolver.PAGE_SUMMARY_FIELD));
    assertEquals("author", fields.get(PSInlineLinkTitleResolver.PAGE_AUTHOR_FIELD));
    assertFalse(fields.containsKey(PSInlineLinkTitleResolver.DISPLAYTITLE_FIELD));
  }

  @Test
  @DisplayName("resolveAssetInlineLinkTitle uses configured field then displaytitle")
  void resolveAssetInlineLinkTitle_configuredThenDisplaytitle() {
    PSAsset asset = new PSAsset();
    Map<String, Object> fields = new HashMap<>();
    fields.put("displaytitle", "Asset Display");
    fields.put("pagetitle", "Custom Page Title");
    asset.setFields(fields);

    assertEquals(
        "Custom Page Title",
        PSRenderLinkService.resolveAssetInlineLinkTitle(asset, "pagetitle"));
    assertEquals(
        "Asset Display",
        PSRenderLinkService.resolveAssetInlineLinkTitle(asset, "missing"));
    assertEquals(
        "Asset Display",
        PSRenderLinkService.resolveAssetInlineLinkTitle(asset, null));
  }

  @Test
  @DisplayName("page titleField from DTO skips partial asset load")
  void resolvePageInlineLinkTitle_dtoHit_skipsPartialLoad() throws Exception {
    PSPage page = new PSPage();
    page.setLinkTitle("Link Default");
    page.setTitle("From DTO page_title");

    String title =
        service.resolvePageInlineLinkTitle(page, "page-1", PSInlineLinkTitleResolver.PAGE_TITLE_FIELD);

    assertEquals("From DTO page_title", title);
    verify(resourceInstanceHelper, never()).loadPartialAsset(anyString());
  }

  @Test
  @DisplayName("page custom titleField loads partial and uses content field")
  void resolvePageInlineLinkTitle_customField_loadsPartial() throws Exception {
    PSPage page = new PSPage();
    page.setLinkTitle("Link Default");
    page.setTitle("Browser");

    PSAsset partial = new PSAsset();
    Map<String, Object> contentFields = new HashMap<>();
    contentFields.put("my_custom_title", "From Content");
    partial.setFields(contentFields);
    when(resourceInstanceHelper.loadPartialAsset("page-2")).thenReturn(partial);

    String title = service.resolvePageInlineLinkTitle(page, "page-2", "my_custom_title");

    assertEquals("From Content", title);
    verify(resourceInstanceHelper).loadPartialAsset("page-2");
  }

  @Test
  @DisplayName("page blank titleField returns link title without load")
  void resolvePageInlineLinkTitle_blankConfig_usesTypeDefault() throws Exception {
    PSPage page = new PSPage();
    page.setLinkTitle("BC Link Title");

    assertEquals("BC Link Title", service.resolvePageInlineLinkTitle(page, "page-3", null));
    assertEquals("BC Link Title", service.resolvePageInlineLinkTitle(page, "page-3", "  "));
    verify(resourceInstanceHelper, never()).loadPartialAsset(anyString());
  }

  @Test
  @DisplayName("page custom missing falls back to type default when displaytitle absent")
  void resolvePageInlineLinkTitle_missingCustom_usesTypeDefault() throws Exception {
    PSPage page = new PSPage();
    page.setLinkTitle("Link Default");

    PSAsset partial = new PSAsset();
    partial.setFields(new HashMap<>());
    when(resourceInstanceHelper.loadPartialAsset("page-4")).thenReturn(partial);

    String title = service.resolvePageInlineLinkTitle(page, "page-4", "nonexistent");
    assertEquals("Link Default", title);
  }

  @Test
  @DisplayName("page constants stay aligned with literal PSPageDao field names")
  void pageFieldConstants_matchDaoLiterals() {
    // Guard against silent drift from PSPageDao field put/get strings.
    assertEquals("resource_link_title", PSInlineLinkTitleResolver.PAGE_DEFAULT_TITLE_FIELD);
    assertEquals("page_title", PSInlineLinkTitleResolver.PAGE_TITLE_FIELD);
    assertEquals("sys_title", PSInlineLinkTitleResolver.SYS_TITLE_FIELD);
    assertTrue(PSRenderLinkService.buildPageTitleFieldMap(new PSPage()).keySet()
        .containsAll(
            java.util.Set.of(
                PSInlineLinkTitleResolver.PAGE_DEFAULT_TITLE_FIELD,
                PSInlineLinkTitleResolver.PAGE_TITLE_FIELD,
                PSInlineLinkTitleResolver.SYS_TITLE_FIELD)));
  }
}
