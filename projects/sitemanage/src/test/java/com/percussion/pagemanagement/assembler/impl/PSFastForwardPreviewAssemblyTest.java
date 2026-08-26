/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
package com.percussion.pagemanagement.assembler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSAssemblyTemplate.OutputFormat;
import com.percussion.services.assembly.IPSAssemblyTemplate.PublishWhen;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.utils.guid.IPSGuid;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for FastForward preview assembly URL/template pick (#3719). */
@Tag("UnitTest")
class PSFastForwardPreviewAssemblyTest {

  @Test
  @DisplayName("associatedTemplatesSafe does not throw when the collection is lazy")
  void associatedTemplatesSafeLazy() {
    assertTrue(PSFastForwardPreviewAssembly.associatedTemplatesSafe(null).isEmpty());
    IPSSite lazy = mock(IPSSite.class);
    when(lazy.getAssociatedTemplates())
        .thenThrow(new RuntimeException("Cannot lazily initialize collection (no session)"));
    assertTrue(PSFastForwardPreviewAssembly.associatedTemplatesSafe(lazy).isEmpty());
    IPSGuid guid = new PSGuid(PSTypeEnum.TEMPLATE, 505);
    IPSAssemblyTemplate t =
        template("rffPgCiHome", guid, PublishWhen.Default, OutputFormat.Page);
    IPSSite site = mock(IPSSite.class);
    when(site.getAssociatedTemplates()).thenReturn(java.util.Set.of(t));
    assertEquals(1, PSFastForwardPreviewAssembly.associatedTemplatesSafe(site).size());
  }

  @Test
  @DisplayName("percPage and percPageTemplate use CM1 dispatcher; rffHome does not")
  void dispatcherByContentType() {
    assertTrue(
        PSFastForwardPreviewAssembly.usesPercPageDispatcher(IPSPageService.PAGE_CONTENT_TYPE));
    assertTrue(
        PSFastForwardPreviewAssembly.usesPercPageDispatcher(IPSTemplateService.TPL_CONTENT_TYPE));
    assertFalse(PSFastForwardPreviewAssembly.usesPercPageDispatcher("rffHome"));
    assertFalse(PSFastForwardPreviewAssembly.usesPercPageDispatcher(null));
    assertFalse(PSFastForwardPreviewAssembly.usesPercPageDispatcher(""));
  }

  @Test
  @DisplayName("pickDefaultPageTemplate prefers site-associated default page templates")
  void pickPrefersSiteAssociated() {
    IPSGuid siteGuid = new PSGuid(PSTypeEnum.TEMPLATE, 505);
    IPSGuid otherGuid = new PSGuid(PSTypeEnum.TEMPLATE, 506);
    IPSAssemblyTemplate siteDefault =
        template("rffPgCiHome", siteGuid, PublishWhen.Default, OutputFormat.Page);
    IPSAssemblyTemplate otherDefault =
        template("rffPgEiHome", otherGuid, PublishWhen.Default, OutputFormat.Page);
    IPSAssemblyTemplate snippet =
        template("rffSnTitleLink", siteGuid, PublishWhen.Default, OutputFormat.Snippet);

    IPSAssemblyTemplate picked =
        PSFastForwardPreviewAssembly.pickDefaultPageTemplate(
            List.of(snippet, otherDefault, siteDefault), List.of(siteDefault));
    assertSame(siteDefault, picked);
  }

  @Test
  @DisplayName("pickDefaultPageTemplate falls back to type default when site set is empty")
  void pickFallsBackWhenSiteEmpty() {
    IPSGuid guid = new PSGuid(PSTypeEnum.TEMPLATE, 505);
    IPSAssemblyTemplate home =
        template("rffPgCiHome", guid, PublishWhen.Default, OutputFormat.Page);
    IPSAssemblyTemplate never =
        template("rffPgNever", guid, PublishWhen.Never, OutputFormat.Page);

    IPSAssemblyTemplate picked =
        PSFastForwardPreviewAssembly.pickDefaultPageTemplate(List.of(never, home), List.of());
    assertSame(home, picked);
  }

  @Test
  @DisplayName("pickDefaultPageTemplate returns null when no default page template exists")
  void pickNullWhenNone() {
    IPSGuid guid = new PSGuid(PSTypeEnum.TEMPLATE, 1);
    IPSAssemblyTemplate snippet =
        template("rffSn", guid, PublishWhen.Default, OutputFormat.Snippet);
    assertNull(
        PSFastForwardPreviewAssembly.pickDefaultPageTemplate(List.of(snippet), List.of()));
    assertNull(PSFastForwardPreviewAssembly.pickDefaultPageTemplate(List.of(), List.of()));
    assertNull(PSFastForwardPreviewAssembly.pickDefaultPageTemplate(null, null));
  }

  @Test
  @DisplayName("buildAssemblerRenderUrl is a CMS assembler path with preview filter")
  void assemblerUrl() {
    String url =
        PSFastForwardPreviewAssembly.buildAssemblerRenderUrl(551, 1, 505, 303, 523);
    assertTrue(url.startsWith("/assembler/render?"));
    assertTrue(url.contains("sys_contentid=551"));
    assertTrue(url.contains("sys_template=505"));
    assertTrue(url.contains("sys_revision=1"));
    assertTrue(url.contains("sys_context=0"));
    assertTrue(url.contains("sys_itemfilter=preview"));
    assertFalse(url.contains("sys_authtype="));
    assertTrue(url.contains("sys_siteid=303"));
    assertTrue(url.contains("sys_folderid=523"));
    assertFalse(url.contains("\\"));
  }

  @Test
  @DisplayName("siteOrAssetPathFromRequest prefers servlet path and strips context")
  void requestPath() {
    assertEquals(
        "/Sites/CorporateInvestments/Corporate Investments Home",
        PSFastForwardPreviewAssembly.siteOrAssetPathFromRequest(
            "/Rhythmyx/Sites/CorporateInvestments/Corporate Investments Home",
            "/Rhythmyx",
            "/Sites",
            "/CorporateInvestments/Corporate Investments Home"));
    assertEquals(
        "/Sites/CorporateInvestments/Home",
        PSFastForwardPreviewAssembly.siteOrAssetPathFromRequest(
            "/Rhythmyx/Sites/CorporateInvestments/Home",
            "/Rhythmyx",
            null,
            null));
  }

  @Test
  @DisplayName("parentCmsPath drops the last CMS segment")
  void parentPath() {
    assertEquals(
        "//Sites/CorporateInvestments",
        PSFastForwardPreviewAssembly.parentCmsPath(
            "//Sites/CorporateInvestments/Corporate Investments Home"));
    assertEquals("//Sites", PSFastForwardPreviewAssembly.parentCmsPath("//Sites/CI"));
    assertEquals("//Sites", PSFastForwardPreviewAssembly.parentCmsPath("//Sites"));
  }

  @Test
  @DisplayName("siteIdForRepositoryPath matches folder root without touching templates")
  void siteIdFromFolderRoot() {
    IPSSite site = mock(IPSSite.class);
    when(site.getGUID()).thenReturn(new PSGuid(PSTypeEnum.SITE, 303));
    when(site.getFolderRoot()).thenReturn("//Sites/CorporateInvestments");
    assertEquals(
        303,
        PSFastForwardPreviewAssembly.siteIdForRepositoryPath(
            "//Sites/CorporateInvestments/Corporate Investments Home", List.of(site)));
    assertNull(
        PSFastForwardPreviewAssembly.siteIdForRepositoryPath(
            "//Sites/EnterpriseInvestments/Home", List.of(site)));
  }

  private static IPSAssemblyTemplate template(
      String name, IPSGuid guid, PublishWhen when, OutputFormat format) {
    IPSAssemblyTemplate t = mock(IPSAssemblyTemplate.class);
    when(t.getName()).thenReturn(name);
    when(t.getGUID()).thenReturn(guid);
    when(t.getPublishWhen()).thenReturn(when);
    when(t.getOutputFormat()).thenReturn(format);
    return t;
  }
}
