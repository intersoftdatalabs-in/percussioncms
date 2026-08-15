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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeList;
import com.percussion.rest.sites.SiteMapDateFormat;
import com.percussion.rest.sites.SiteMapOptions;
import com.percussion.rest.sites.SiteMapType;
import com.percussion.rest.sites.VirtualSiteBuildRequest;
import com.percussion.rest.sites.VirtualSiteBuildResult;
import com.percussion.rest.sites.VirtualSitePreviewStatus;
import com.percussion.rest.sites.VirtualSitePublishResult;
import com.percussion.rest.templates.TemplateSummary;
import com.percussion.rest.templates.TemplateSummaryList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Catalog DTOs must serialize names (and related fields) for Developer SPA tables. ContentType list
 * historically collapsed to hideFromMenu-only when Optional getters were not unwrapped (issue
 * #1693). TemplateSummary similarly collapsed to templateId-only (issue #2189). Virtual Site +
 * SiteMap wire DTOs follow the same plain-getter rule (issue #3411 / #3388). All use production
 * {@link JacksonContextResolver} under {@code @JsonInclude(NON_NULL)}.
 */
@Tag("UnitTest")
class JacksonContextResolverOptionalTest {

  private final ObjectMapper mapper = new JacksonContextResolver().getContext(ContentType.class);

  @Test
  void contentType_serializesNameLabelGuidNotOnlyHideFromMenu() {
    ContentType ct = new ContentType();
    ct.setName("percPage");
    ct.setLabel("Page");
    ct.setDescription("Page content type");
    ct.setHideFromMenu(false);
    ct.setGuid(new Guid("0-2-311"));

    String json = mapper.writeValueAsString(ct);
    assertTrue(json.contains("percPage"), json);
    assertTrue(json.contains("Page"), json);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("\"label\""), json);
    assertTrue(json.contains("\"guid\""), json);
    assertTrue(json.contains("0-2-311") || json.contains("311"), json);
  }

  @Test
  void contentTypeList_serializesNamesNotHideFromMenuOnly() {
    ContentType ct = new ContentType();
    ct.setName("percPage");
    ct.setLabel("Page");
    ct.setGuid(new Guid("0-2-311"));
    ct.setHideFromMenu(false);

    ContentTypeList list = new ContentTypeList(List.of(ct));
    String json = mapper.writeValueAsString(list);
    assertTrue(json.contains("percPage"), json);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("\"label\""), json);
    // List root wrap uses ContentType (XmlRootElement on ContentTypeList) or plain array
    assertTrue(json.contains("ContentType") || json.startsWith("["), json);
  }

  @Test
  void templateSummary_serializesNameNotOnlyId() {
    TemplateSummary t = new TemplateSummary();
    t.setTemplateId(1018);
    t.setTemplateName("perc.page");
    t.setTemplateLabel("Page");
    t.setTemplateDescription("Page template");

    String json = mapper.writeValueAsString(t);
    assertTrue(json.contains("perc.page"), json);
    assertTrue(json.contains("Page"), json);
    assertTrue(json.contains("1018"), json);
    // Property names (not only values) — live H2 list was templateId-only (issue #2189)
    assertTrue(json.contains("\"templateName\""), json);
    assertTrue(json.contains("\"templateLabel\""), json);
    assertTrue(json.contains("\"templateDescription\""), json);
    assertTrue(json.contains("\"templateId\""), json);
    assertFalse(
        json.replaceAll("\\s", "").matches(".*\\{\"templateId\":1018\\}.*")
            && !json.contains("\"templateName\""),
        "Summary JSON must not be templateId-only: " + json);
  }

  @Test
  void templateSummaryList_serializesNamesNotIdOnly() {
    TemplateSummary t = new TemplateSummary();
    t.setTemplateId(1037);
    t.setTemplateName("perc.page");
    t.setTemplateLabel("Page");

    TemplateSummaryList list = new TemplateSummaryList(List.of(t));
    String json = mapper.writeValueAsString(list);
    assertTrue(json.contains("perc.page"), json);
    assertTrue(json.contains("\"templateName\""), json);
    assertTrue(json.contains("\"templateLabel\""), json);
    assertTrue(json.contains("1037"), json);
    assertTrue(json.contains("TemplateSummary") || json.startsWith("["), json);
  }

  @Test
  void virtualSiteBuildRequest_serializesPlainScalarsNotOptionalBeans() {
    VirtualSiteBuildRequest request = new VirtualSiteBuildRequest();
    request.setOutputRoot("C:/tmp/product-docs-site");

    ObjectMapper requestMapper =
        new JacksonContextResolver().getContext(VirtualSiteBuildRequest.class);
    String json = requestMapper.writeValueAsString(request);
    assertTrue(json.contains("\"outputRoot\""), json);
    assertTrue(json.contains("C:/tmp/product-docs-site"), json);
    assertNoOptionalBeanKeys(json);

    VirtualSiteBuildRequest roundTrip =
        requestMapper.readValue(json, VirtualSiteBuildRequest.class);
    assertEquals("C:/tmp/product-docs-site", roundTrip.getOutputRoot(), json);
  }

  @Test
  void virtualSiteBuildResult_serializesPlainScalarsNotOptionalBeans() {
    VirtualSiteBuildResult result = new VirtualSiteBuildResult();
    result.setSiteName("Help");
    result.setSiteKey("product-docs");
    result.setOutputPath("C:/Rhythmyx/tmp/virtual-sites/product-docs");
    result.setPagesWritten(42);
    result.setLinkProblemCount(0);
    result.setHasLinkProblems(false);

    ObjectMapper resultMapper =
        new JacksonContextResolver().getContext(VirtualSiteBuildResult.class);
    String json = resultMapper.writeValueAsString(result);
    assertTrue(json.contains("\"siteName\""), json);
    assertTrue(json.contains("Help"), json);
    assertTrue(json.contains("\"siteKey\""), json);
    assertTrue(json.contains("product-docs"), json);
    assertTrue(json.contains("\"outputPath\""), json);
    assertTrue(json.contains("\"pagesWritten\""), json);
    assertNoOptionalBeanKeys(json);

    VirtualSiteBuildResult roundTrip = resultMapper.readValue(json, VirtualSiteBuildResult.class);
    assertEquals("Help", roundTrip.getSiteName(), json);
    assertEquals("product-docs", roundTrip.getSiteKey(), json);
    assertEquals("C:/Rhythmyx/tmp/virtual-sites/product-docs", roundTrip.getOutputPath(), json);
    assertEquals(42, roundTrip.getPagesWritten(), json);
  }

  @Test
  void virtualSitePreviewStatus_serializesPlainScalarsNotOptionalBeans() {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(true);
    status.setHomePath("8.2/index.html");
    status.setOutputPath("C:/Rhythmyx/tmp/virtual-sites/product-docs");
    status.setMessage("ready");

    ObjectMapper statusMapper =
        new JacksonContextResolver().getContext(VirtualSitePreviewStatus.class);
    String json = statusMapper.writeValueAsString(status);
    assertTrue(json.contains("\"homePath\""), json);
    assertTrue(json.contains("8.2/index.html"), json);
    assertTrue(json.contains("\"outputPath\""), json);
    assertTrue(json.contains("\"message\""), json);
    assertNoOptionalBeanKeys(json);

    VirtualSitePreviewStatus roundTrip =
        statusMapper.readValue(json, VirtualSitePreviewStatus.class);
    assertEquals(Boolean.TRUE, roundTrip.getAvailable(), json);
    assertEquals("8.2/index.html", roundTrip.getHomePath(), json);
    assertEquals("ready", roundTrip.getMessage(), json);
  }

  @Test
  void virtualSitePublishResult_serializesPlainScalarsNotOptionalBeans() {
    VirtualSitePublishResult result = new VirtualSitePublishResult();
    result.setSiteName("Help");
    result.setSiteKey("product-docs");
    result.setPublishPath("C:/inetpub/wwwroot/help");
    result.setBuildOutputPath("C:/Rhythmyx/tmp/virtual-sites/product-docs");
    result.setPagesWritten(42);
    result.setFilesCopied(50);
    result.setLinkProblemCount(0);
    result.setHasLinkProblems(false);

    ObjectMapper resultMapper =
        new JacksonContextResolver().getContext(VirtualSitePublishResult.class);
    String json = resultMapper.writeValueAsString(result);
    assertTrue(json.contains("\"siteName\""), json);
    assertTrue(json.contains("Help"), json);
    assertTrue(json.contains("\"publishPath\""), json);
    assertTrue(json.contains("\"buildOutputPath\""), json);
    assertNoOptionalBeanKeys(json);

    VirtualSitePublishResult roundTrip =
        resultMapper.readValue(json, VirtualSitePublishResult.class);
    assertEquals("Help", roundTrip.getSiteName(), json);
    assertEquals("C:/inetpub/wwwroot/help", roundTrip.getPublishPath(), json);
    assertEquals(
        "C:/Rhythmyx/tmp/virtual-sites/product-docs", roundTrip.getBuildOutputPath(), json);
    assertEquals(50, roundTrip.getFilesCopied(), json);
  }

  @Test
  void siteMapOptions_serializesPlainScalarsNotOptionalBeans() {
    SiteMapOptions options = new SiteMapOptions();
    options.setNavigationBased(true);
    options.setIncludeFolder(false);
    options.setTimeZone("UTC");
    options.setDateFormat(SiteMapDateFormat.DAY);
    options.setFileName("sitemap.xml");
    options.setUseSiteMapIndex("true");
    options.setSiteMapType(SiteMapType.STANDARD);
    options.setDefaultFrequency(0.5);

    ObjectMapper optionsMapper = new JacksonContextResolver().getContext(SiteMapOptions.class);
    String json = optionsMapper.writeValueAsString(options);
    assertTrue(json.contains("\"timeZone\""), json);
    assertTrue(json.contains("UTC"), json);
    assertTrue(json.contains("\"dateFormat\""), json);
    assertTrue(json.contains("DAY"), json);
    assertTrue(json.contains("\"fileName\""), json);
    assertTrue(json.contains("sitemap.xml"), json);
    assertTrue(json.contains("\"useSiteMapIndex\""), json);
    assertTrue(json.contains("\"siteMapType\""), json);
    assertNoOptionalBeanKeys(json);

    SiteMapOptions roundTrip = optionsMapper.readValue(json, SiteMapOptions.class);
    assertEquals("UTC", roundTrip.getTimeZone(), json);
    assertEquals(SiteMapDateFormat.DAY, roundTrip.getDateFormat(), json);
    assertEquals("sitemap.xml", roundTrip.getFileName(), json);
    assertEquals("true", roundTrip.getUseSiteMapIndex(), json);
    assertEquals(SiteMapType.STANDARD, roundTrip.getSiteMapType(), json);
  }

  private static void assertNoOptionalBeanKeys(String json) {
    assertFalse(
        json.contains("\"empty\"") || json.contains("\"present\""),
        "JSON must not contain Optional-bean empty/present keys: " + json);
  }
}
