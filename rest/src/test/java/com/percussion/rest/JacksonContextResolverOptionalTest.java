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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeList;
import com.percussion.rest.contexts.Context;
import com.percussion.rest.deliverytypes.DeliveryType;
import com.percussion.rest.displayformat.DisplayFormatProperty;
import com.percussion.rest.locationscheme.LocationScheme;
import com.percussion.rest.locationscheme.LocationSchemeParameter;
import com.percussion.rest.locationscheme.LocationSchemeParameterList;
import com.percussion.rest.pages.CalendarInfo;
import com.percussion.rest.pages.CodeInfo;
import com.percussion.rest.pages.Page;
import com.percussion.rest.pages.Region;
import com.percussion.rest.pages.SeoInfo;
import com.percussion.rest.pages.Widget;
import com.percussion.rest.pages.WorkflowInfo;
import com.percussion.rest.roles.Role;
import com.percussion.rest.sites.SiteMapDateFormat;
import com.percussion.rest.sites.SiteMapOptions;
import com.percussion.rest.sites.SiteMapType;
import com.percussion.rest.sites.VirtualSiteBuildRequest;
import com.percussion.rest.sites.VirtualSiteBuildResult;
import com.percussion.rest.sites.VirtualSitePreviewStatus;
import com.percussion.rest.sites.VirtualSitePublishResult;
import com.percussion.rest.templates.Template;
import com.percussion.rest.templates.TemplateBinding;
import com.percussion.rest.templates.TemplateSummary;
import com.percussion.rest.templates.TemplateSummaryList;
import com.percussion.rest.users.User;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Catalog DTOs must serialize names (and related fields) for Developer SPA tables. ContentType list
 * historically collapsed to hideFromMenu-only when Optional getters were not unwrapped (issue
 * #1693). TemplateSummary similarly collapsed to templateId-only (issue #2189). User / Role /
 * ObjectSummary follow the same plain-getter rule (issue #3388). DisplayFormatProperty / Template /
 * Page family follow the same rule (issue #3407). Virtual Site + SiteMap wire DTOs follow the same
 * rule (issue #3411 / #3388). LocationScheme / Context / DeliveryType follow the same
 * plain-getter contract (issue #3412). All use production {@link JacksonContextResolver} under {@code
 * @JsonInclude(NON_NULL)}.
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
  void user_serializesPlainScalarsNotOptionalBeans() {
    User user = new User();
    user.setUserName("admin");
    user.setFirstName("Ada");
    user.setLastName("Lovelace");
    user.setEmailAddress("ada@example.com");
    user.setUserType("INTERNAL");

    ObjectMapper userMapper = new JacksonContextResolver().getContext(User.class);
    String json = userMapper.writeValueAsString(user);
    assertTrue(json.contains("\"userName\""), json);
    assertTrue(json.contains("admin"), json);
    assertTrue(json.contains("\"firstName\""), json);
    assertTrue(json.contains("Ada"), json);
    assertTrue(json.contains("\"lastName\""), json);
    assertTrue(json.contains("\"emailAddress\""), json);
    assertTrue(json.contains("\"userType\""), json);
    assertNoOptionalBeanKeys(json);

    User roundTrip = userMapper.readValue(json, User.class);
    assertEquals("admin", roundTrip.getUserName(), json);
    assertEquals("Ada", roundTrip.getFirstName(), json);
    assertEquals("INTERNAL", roundTrip.getUserType(), json);
  }

  @Test
  void role_serializesPlainScalarsNotOptionalBeans() {
    Role role = new Role();
    role.setName("Editor");
    role.setDescription("Edit content");
    role.setHomePage("Dashboard");

    ObjectMapper roleMapper = new JacksonContextResolver().getContext(Role.class);
    String json = roleMapper.writeValueAsString(role);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("Editor"), json);
    assertTrue(json.contains("\"description\""), json);
    assertTrue(json.contains("\"homePage\""), json);
    assertTrue(json.contains("Dashboard"), json);
    assertNoOptionalBeanKeys(json);

    Role roundTrip = roleMapper.readValue(json, Role.class);
    assertEquals("Editor", roundTrip.getName(), json);
    assertEquals("Dashboard", roundTrip.getHomePage(), json);
  }

  @Test
  void objectSummary_serializesNameLabelGuidNotOptionalBeans() {
    ObjectSummary summary = new ObjectSummary();
    summary.setName("Default");
    summary.setLabel("Default ACL");
    summary.setDescription("Site ACL");
    summary.setGuid(new Guid("0-13-10"));

    ObjectMapper summaryMapper = new JacksonContextResolver().getContext(ObjectSummary.class);
    String json = summaryMapper.writeValueAsString(summary);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("Default"), json);
    assertTrue(json.contains("\"label\""), json);
    assertTrue(json.contains("\"description\""), json);
    assertTrue(json.contains("\"guid\""), json);
    assertTrue(json.contains("0-13-10") || json.contains("10"), json);
    assertNoOptionalBeanKeys(json);

    ObjectSummary roundTrip = summaryMapper.readValue(json, ObjectSummary.class);
    assertEquals("Default", roundTrip.getName(), json);
    assertEquals("Default ACL", roundTrip.getLabel(), json);
    assertNotNull(roundTrip.getGuid(), json);
  }

  @Test
  void displayFormatProperty_serializesPlainScalarsNotOptionalBeans() {
    DisplayFormatProperty prop = new DisplayFormatProperty();
    prop.setPropertyId("1");
    prop.setPropertyName("sortColumn");
    prop.setPropertyValue("sys_title");
    prop.setDescription("Default sort");

    ObjectMapper propMapper = new JacksonContextResolver().getContext(DisplayFormatProperty.class);
    String json = propMapper.writeValueAsString(prop);
    assertTrue(json.contains("\"propertyId\""), json);
    assertTrue(json.contains("\"propertyName\""), json);
    assertTrue(json.contains("sortColumn"), json);
    assertTrue(json.contains("\"propertyValue\""), json);
    assertTrue(json.contains("sys_title"), json);
    assertTrue(json.contains("\"description\""), json);
    assertNoOptionalBeanKeys(json);

    DisplayFormatProperty roundTrip = propMapper.readValue(json, DisplayFormatProperty.class);
    assertEquals("1", roundTrip.getPropertyId(), json);
    assertEquals("sortColumn", roundTrip.getPropertyName(), json);
    assertEquals("sys_title", roundTrip.getPropertyValue(), json);
  }

  @Test
  void template_serializesNameLabelNotOptionalBeans() {
    Template template = new Template();
    template.setName("perc.page");
    template.setLabel("Page");
    template.setDescription("Page assembly");
    template.setMimeType("text/html");

    ObjectMapper templateMapper = new JacksonContextResolver().getContext(Template.class);
    String json = templateMapper.writeValueAsString(template);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("perc.page"), json);
    assertTrue(json.contains("\"label\""), json);
    assertTrue(json.contains("\"description\""), json);
    assertTrue(json.contains("\"mimeType\""), json);
    assertNoOptionalBeanKeys(json);

    Template roundTrip = templateMapper.readValue(json, Template.class);
    assertEquals("perc.page", roundTrip.getName(), json);
    assertEquals("Page", roundTrip.getLabel(), json);
    assertEquals("text/html", roundTrip.getMimeType(), json);
  }

  @Test
  void templateBinding_serializesVariableExpressionNotOptionalBeans() {
    TemplateBinding binding = new TemplateBinding();
    binding.setVariable("$rx");
    binding.setExpression("$sys.template");
    binding.setExecutionOrder(1);

    ObjectMapper bindingMapper = new JacksonContextResolver().getContext(TemplateBinding.class);
    String json = bindingMapper.writeValueAsString(binding);
    assertTrue(json.contains("\"variable\""), json);
    assertTrue(json.contains("$rx"), json);
    assertTrue(json.contains("\"expression\""), json);
    assertTrue(json.contains("$sys.template"), json);
    assertNoOptionalBeanKeys(json);

    TemplateBinding roundTrip = bindingMapper.readValue(json, TemplateBinding.class);
    assertEquals("$rx", roundTrip.getVariable(), json);
    assertEquals("$sys.template", roundTrip.getExpression(), json);
  }

  @Test
  void page_serializesNamePathNotOptionalBeans() {
    Page page = new Page();
    page.setId("1234");
    page.setName("home.html");
    page.setDisplayName("Home");
    page.setSiteName("Help");
    page.setFolderPath("docs");
    page.setTemplateName("perc.page");
    page.setSummary("Welcome");

    SeoInfo seo = new SeoInfo();
    seo.setBrowserTitle("Help Home");
    seo.setMetaDescription("Landing page");
    seo.setHideSearch(Boolean.FALSE);
    page.setSeo(seo);

    ObjectMapper pageMapper = new JacksonContextResolver().getContext(Page.class);
    String json = pageMapper.writeValueAsString(page);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("home.html"), json);
    assertTrue(json.contains("\"displayName\""), json);
    assertTrue(json.contains("\"siteName\""), json);
    assertTrue(json.contains("\"folderPath\""), json);
    assertTrue(json.contains("\"templateName\""), json);
    assertTrue(json.contains("\"browserTitle\""), json);
    assertTrue(json.contains("Help Home"), json);
    assertNoOptionalBeanKeys(json);

    Page roundTrip = pageMapper.readValue(json, Page.class);
    assertEquals("1234", roundTrip.getId(), json);
    assertEquals("home.html", roundTrip.getName(), json);
    assertEquals("Help", roundTrip.getSiteName(), json);
    assertNotNull(roundTrip.getSeo(), json);
    assertEquals("Help Home", roundTrip.getSeo().getBrowserTitle(), json);
  }

  @Test
  void widgetAndPageFamily_serializePlainScalarsNotOptionalBeans() {
    Widget widget = new Widget();
    widget.setId("w1");
    widget.setName("richText");
    widget.setType("percRichText");
    widget.setScope(Widget.SCOPE_LOCAL);
    widget.setEditable(Boolean.TRUE);

    ObjectMapper widgetMapper = new JacksonContextResolver().getContext(Widget.class);
    String widgetJson = widgetMapper.writeValueAsString(widget);
    assertTrue(widgetJson.contains("\"name\""), widgetJson);
    assertTrue(widgetJson.contains("richText"), widgetJson);
    assertTrue(widgetJson.contains("\"type\""), widgetJson);
    assertNoOptionalBeanKeys(widgetJson);
    Widget widgetRoundTrip = widgetMapper.readValue(widgetJson, Widget.class);
    assertEquals("w1", widgetRoundTrip.getId(), widgetJson);
    assertEquals("percRichText", widgetRoundTrip.getType(), widgetJson);

    Region region = new Region();
    region.setName("content");
    region.setType("TEMPLATE");
    region.setWidgets(List.of(widget));
    ObjectMapper regionMapper = new JacksonContextResolver().getContext(Region.class);
    String regionJson = regionMapper.writeValueAsString(region);
    assertTrue(regionJson.contains("\"name\""), regionJson);
    assertTrue(regionJson.contains("content"), regionJson);
    assertNoOptionalBeanKeys(regionJson);

    WorkflowInfo workflow = new WorkflowInfo();
    workflow.setName("Default Workflow");
    workflow.setState("Draft");
    workflow.setCheckedOut(Boolean.FALSE);
    ObjectMapper wfMapper = new JacksonContextResolver().getContext(WorkflowInfo.class);
    String wfJson = wfMapper.writeValueAsString(workflow);
    assertTrue(wfJson.contains("\"name\""), wfJson);
    assertTrue(wfJson.contains("Draft"), wfJson);
    assertNoOptionalBeanKeys(wfJson);
    WorkflowInfo wfRoundTrip = wfMapper.readValue(wfJson, WorkflowInfo.class);
    assertEquals("Default Workflow", wfRoundTrip.getName(), wfJson);
    assertEquals("Draft", wfRoundTrip.getState(), wfJson);

    CodeInfo code = new CodeInfo();
    code.setHead("<script></script>");
    ObjectMapper codeMapper = new JacksonContextResolver().getContext(CodeInfo.class);
    String codeJson = codeMapper.writeValueAsString(code);
    assertTrue(codeJson.contains("\"head\""), codeJson);
    assertNoOptionalBeanKeys(codeJson);

    CalendarInfo calendar = new CalendarInfo();
    calendar.setCalendars(List.of("Default"));
    ObjectMapper calMapper = new JacksonContextResolver().getContext(CalendarInfo.class);
    String calJson = calMapper.writeValueAsString(calendar);
    assertTrue(calJson.contains("\"calendars\""), calJson);
    assertTrue(calJson.contains("Default"), calJson);
    assertNoOptionalBeanKeys(calJson);
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

    VirtualSiteBuildRequest roundTrip = requestMapper.readValue(json, VirtualSiteBuildRequest.class);
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

    ObjectMapper resultMapper = new JacksonContextResolver().getContext(VirtualSiteBuildResult.class);
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
    assertEquals("C:/Rhythmyx/tmp/virtual-sites/product-docs", roundTrip.getBuildOutputPath(), json);
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


  @Test
  void locationScheme_serializesNameNotOptionalBeans() {
    LocationScheme scheme = new LocationScheme();
    scheme.setSchemeId(new Guid("0-113-42"));
    scheme.setName("generic");
    scheme.setDescription("Generic location scheme");
    scheme.setLocationSchemeGenerator("sys_GenericLocationSchemeGenerator");
    scheme.setTemplateId(1018);
    scheme.setContentTypeId(311);

    ObjectMapper schemeMapper = new JacksonContextResolver().getContext(LocationScheme.class);
    String json = schemeMapper.writeValueAsString(scheme);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("generic"), json);
    assertTrue(json.contains("\"description\""), json);
    assertTrue(json.contains("\"locationSchemeGenerator\""), json);
    assertTrue(json.contains("sys_GenericLocationSchemeGenerator"), json);
    assertNoOptionalBeanKeys(json);

    LocationScheme roundTrip = schemeMapper.readValue(json, LocationScheme.class);
    assertEquals("generic", roundTrip.getName(), json);
    assertEquals("Generic location scheme", roundTrip.getDescription(), json);
    assertNotNull(roundTrip.getSchemeId(), json);
  }

  @Test
  void locationSchemeParameter_serializesNameValueNotOptionalBeans() {
    LocationSchemeParameter param = new LocationSchemeParameter();
    param.setName("ext");
    param.setSequence(1);
    param.setType("String");
    param.setValue(".html");

    ObjectMapper paramMapper =
        new JacksonContextResolver().getContext(LocationSchemeParameter.class);
    String json = paramMapper.writeValueAsString(param);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("ext"), json);
    assertTrue(json.contains("\"value\""), json);
    assertTrue(json.contains(".html"), json);
    assertTrue(json.contains("\"sequence\""), json);
    assertNoOptionalBeanKeys(json);

    LocationSchemeParameter roundTrip = paramMapper.readValue(json, LocationSchemeParameter.class);
    assertEquals("ext", roundTrip.getName(), json);
    assertEquals(".html", roundTrip.getValue(), json);
    assertEquals(1, roundTrip.getSequence(), json);
  }

  @Test
  void context_serializesNameAndNestedSchemeNotOptionalBeans() {
    LocationScheme defaultScheme = new LocationScheme();
    defaultScheme.setName("generic");
    defaultScheme.setSchemeId(new Guid("0-113-42"));

    LocationSchemeParameter param = new LocationSchemeParameter();
    param.setName("ext");
    param.setValue(".html");
    LocationSchemeParameterList params = new LocationSchemeParameterList(List.of(param));
    defaultScheme.setParameters(params);

    Context context = new Context();
    context.setId(new Guid("0-101-1"));
    context.setName("Publish");
    context.setDescription("Publish context");
    context.setDefaultScheme(defaultScheme);
    context.setLocationSchemes(List.of(defaultScheme));

    ObjectMapper contextMapper = new JacksonContextResolver().getContext(Context.class);
    String json = contextMapper.writeValueAsString(context);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("Publish"), json);
    assertTrue(json.contains("\"defaultScheme\""), json);
    assertTrue(json.contains("generic"), json);
    assertTrue(json.contains("\"locationSchemes\""), json);
    assertTrue(json.contains("ext"), json);
    assertNoOptionalBeanKeys(json);

    Context roundTrip = contextMapper.readValue(json, Context.class);
    assertEquals("Publish", roundTrip.getName(), json);
    assertNotNull(roundTrip.getDefaultScheme(), json);
    assertEquals("generic", roundTrip.getDefaultScheme().getName(), json);
    assertNotNull(roundTrip.getLocationSchemes(), json);
    assertEquals(1, roundTrip.getLocationSchemes().size(), json);
  }

  @Test
  void deliveryType_serializesNameBeanNotOptionalBeans() {
    DeliveryType type = new DeliveryType();
    type.setId(new Guid("0-115-7"));
    type.setName("filesystem");
    type.setDescription("File system delivery");
    type.setBeanName("sys_fileDeliveryType");
    type.setUnpublishingRequiresAssembly(false);

    ObjectMapper typeMapper = new JacksonContextResolver().getContext(DeliveryType.class);
    String json = typeMapper.writeValueAsString(type);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("filesystem"), json);
    assertTrue(json.contains("\"beanName\""), json);
    assertTrue(json.contains("sys_fileDeliveryType"), json);
    assertTrue(json.contains("\"description\""), json);
    assertNoOptionalBeanKeys(json);

    DeliveryType roundTrip = typeMapper.readValue(json, DeliveryType.class);
    assertEquals("filesystem", roundTrip.getName(), json);
    assertEquals("sys_fileDeliveryType", roundTrip.getBeanName(), json);
    assertEquals("File system delivery", roundTrip.getDescription(), json);
    assertFalse(roundTrip.isUnpublishingRequiresAssembly(), json);
  }
  private static void assertNoOptionalBeanKeys(String json) {
    assertFalse(
        json.contains("\"empty\"") || json.contains("\"present\""),
        "JSON must not contain Optional-bean empty/present keys: " + json);
  }
}
