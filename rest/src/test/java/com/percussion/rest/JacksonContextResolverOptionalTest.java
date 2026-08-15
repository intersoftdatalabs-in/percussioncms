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

import com.percussion.rest.assets.Asset;
import com.percussion.rest.assets.AssetField;
import com.percussion.rest.assets.AssetFieldList;
import com.percussion.rest.assets.BinaryFile;
import com.percussion.rest.communities.Community;
import com.percussion.rest.communities.CommunityRole;
import com.percussion.rest.communities.CommunityVisibility;
import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeList;
import com.percussion.rest.contexts.Context;
import com.percussion.rest.deliverytypes.DeliveryType;
import com.percussion.rest.displayformat.DisplayFormatProperty;
import com.percussion.rest.folders.CopyFolderItemRequest;
import com.percussion.rest.folders.Folder;
import com.percussion.rest.folders.SectionInfo;
import com.percussion.rest.folders.SectionLinkRef;
import com.percussion.rest.LinkRef;
import com.percussion.rest.MoveFolderItem;
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
 * plain-getter contract (issue #3412). Folder / Section / path-request DTOs follow the same
 * rule (issue #3413). All use production {@link JacksonContextResolver} under {@code
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

  @Test
  void folder_serializesNamePathSiteNotOptionalBeans() {
    Folder folder = new Folder();
    folder.setId("guid-1");
    folder.setName("News");
    folder.setSiteName("Corporate");
    folder.setPath("sections");
    folder.setWorkflow("default");
    folder.setAccessLevel(Folder.ACCESS_LEVEL_WRITE);
    folder.setCommunityName("Default");
    folder.setDefaultDisplayFormatName("Related Content");
    folder.setLocale("en-us");
    folder.setEditUsers(List.of("Admin"));

    SectionInfo info = new SectionInfo();
    info.setType("section");
    info.setDisplayTitle("News Section");
    info.setTargetWindow("_self");
    info.setNavClass("nav-news");
    info.setTemplateName("perc.page");
    info.setLandingPage(new LinkRef("index.html", "http://example.com/index.html"));
    folder.setSectionInfo(info);

    folder.setPages(List.of(new LinkRef("index.html", "http://example.com/index.html")));
    folder.setSubfolders(List.of(new LinkRef("archive", "http://example.com/archive")));
    folder.setSubsections(
        List.of(new SectionLinkRef("Press", "http://example.com/press", SectionLinkRef.TYPE_INTERNAL)));

    ObjectMapper folderMapper = new JacksonContextResolver().getContext(Folder.class);
    String json = folderMapper.writeValueAsString(folder);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("News"), json);
    assertTrue(json.contains("\"siteName\""), json);
    assertTrue(json.contains("Corporate"), json);
    assertTrue(json.contains("\"path\""), json);
    assertTrue(json.contains("sections"), json);
    assertTrue(json.contains("\"sectionInfo\""), json);
    assertTrue(json.contains("News Section"), json);
    assertTrue(json.contains("\"subsections\""), json);
    assertNoOptionalBeanKeys(json);

    Folder roundTrip = folderMapper.readValue(json, Folder.class);
    assertEquals("News", roundTrip.getName(), json);
    assertEquals("Corporate", roundTrip.getSiteName(), json);
    assertEquals("sections", roundTrip.getPath(), json);
    assertNotNull(roundTrip.getSectionInfo(), json);
    assertEquals("News Section", roundTrip.getSectionInfo().getDisplayTitle(), json);
    assertEquals("perc.page", roundTrip.getSectionInfo().getTemplateName(), json);
  }

  @Test
  void sectionInfo_serializesScalarsNotOptionalBeans() {
    SectionInfo info = new SectionInfo();
    info.setType("externallink");
    info.setDisplayTitle("Partner");
    info.setTargetWindow("_blank");
    info.setNavClass("nav-ext");
    info.setTemplateName("perc.page");
    info.setExternalLinkUrl("https://partner.example");
    info.setLandingPage(new LinkRef("home", "https://partner.example/"));

    ObjectMapper infoMapper = new JacksonContextResolver().getContext(SectionInfo.class);
    String json = infoMapper.writeValueAsString(info);
    assertTrue(json.contains("\"displayTitle\""), json);
    assertTrue(json.contains("Partner"), json);
    assertTrue(json.contains("\"externalLinkUrl\""), json);
    assertTrue(json.contains("https://partner.example"), json);
    assertTrue(json.contains("\"type\""), json);
    assertNoOptionalBeanKeys(json);

    SectionInfo roundTrip = infoMapper.readValue(json, SectionInfo.class);
    assertEquals("externallink", roundTrip.getType(), json);
    assertEquals("Partner", roundTrip.getDisplayTitle(), json);
    assertEquals("https://partner.example", roundTrip.getExternalLinkUrl(), json);
    assertNotNull(roundTrip.getLandingPage(), json);
  }

  @Test
  void sectionLinkRef_serializesTypeNotOptionalBeans() {
    SectionLinkRef ref =
        new SectionLinkRef("Press", "http://example.com/press", SectionLinkRef.TYPE_EXTERNAL);

    ObjectMapper refMapper = new JacksonContextResolver().getContext(SectionLinkRef.class);
    String json = refMapper.writeValueAsString(ref);
    assertTrue(json.contains("\"type\""), json);
    assertTrue(json.contains(SectionLinkRef.TYPE_EXTERNAL), json);
    assertTrue(json.contains("Press"), json);
    assertNoOptionalBeanKeys(json);

    SectionLinkRef roundTrip = refMapper.readValue(json, SectionLinkRef.class);
    assertEquals(SectionLinkRef.TYPE_EXTERNAL, roundTrip.getType(), json);
    assertEquals("Press", roundTrip.getName().orElse(null), json);
  }

  @Test
  void copyFolderItemRequest_serializesPathsNotOptionalBeans() {
    CopyFolderItemRequest req =
        new CopyFolderItemRequest("/Sites/A/dest", "/Sites/A/src/page.html");

    ObjectMapper reqMapper = new JacksonContextResolver().getContext(CopyFolderItemRequest.class);
    String json = reqMapper.writeValueAsString(req);
    assertTrue(json.contains("\"targetFolderPath\""), json);
    assertTrue(json.contains("/Sites/A/dest"), json);
    assertTrue(json.contains("\"itemPath\""), json);
    assertTrue(json.contains("/Sites/A/src/page.html"), json);
    assertNoOptionalBeanKeys(json);

    CopyFolderItemRequest roundTrip = reqMapper.readValue(json, CopyFolderItemRequest.class);
    assertEquals("/Sites/A/dest", roundTrip.getTargetFolderPath(), json);
    assertEquals("/Sites/A/src/page.html", roundTrip.getItemPath(), json);
  }

  @Test
  void moveFolderItem_serializesPathsNotOptionalBeans() {
    MoveFolderItem req = new MoveFolderItem("/Sites/A/src/page.html", "/Sites/A/dest");

    ObjectMapper reqMapper = new JacksonContextResolver().getContext(MoveFolderItem.class);
    String json = reqMapper.writeValueAsString(req);
    assertTrue(json.contains("\"targetFolderPath\""), json);
    assertTrue(json.contains("/Sites/A/dest"), json);
    assertTrue(json.contains("\"itemPath\""), json);
    assertTrue(json.contains("/Sites/A/src/page.html"), json);
    assertNoOptionalBeanKeys(json);

    MoveFolderItem roundTrip = reqMapper.readValue(json, MoveFolderItem.class);
    assertEquals("/Sites/A/dest", roundTrip.getTargetFolderPath(), json);
    assertEquals("/Sites/A/src/page.html", roundTrip.getItemPath(), json);
  }

  @Test
  void asset_serializesPlainScalarsNotOptionalBeans() {
    Asset asset = new Asset();
    asset.setId("guid-1");
    asset.setName("banner.png");
    asset.setType("percImageAsset");
    asset.setFolderPath("/Assets/uploads");
    asset.setRemove(false);
    asset.setFields(new AssetFieldList());
    asset.getFields().add(new AssetField("alttext", "Hero banner"));

    ObjectMapper assetMapper = new JacksonContextResolver().getContext(Asset.class);
    String json = assetMapper.writeValueAsString(asset);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("banner.png"), json);
    assertTrue(json.contains("\"type\""), json);
    assertTrue(json.contains("percImageAsset"), json);
    assertTrue(json.contains("\"folderPath\""), json);
    assertTrue(json.contains("/Assets/uploads"), json);
    assertTrue(json.contains("\"id\""), json);
    assertTrue(json.contains("alttext"), json);
    assertTrue(json.contains("Hero banner"), json);
    assertNoOptionalBeanKeys(json);

    Asset roundTrip = assetMapper.readValue(json, Asset.class);
    assertEquals("banner.png", roundTrip.getName(), json);
    assertEquals("percImageAsset", roundTrip.getType(), json);
    assertEquals("/Assets/uploads", roundTrip.getFolderPath(), json);
    assertEquals("guid-1", roundTrip.getId(), json);
    assertFalse(roundTrip.getRemove(), json);
  }

  @Test
  void assetField_serializesNameValueNotOptionalBeans() {
    AssetField field = new AssetField("displaytitle", "Home banner");

    ObjectMapper fieldMapper = new JacksonContextResolver().getContext(AssetField.class);
    String json = fieldMapper.writeValueAsString(field);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("displaytitle"), json);
    assertTrue(json.contains("\"value\""), json);
    assertTrue(json.contains("Home banner"), json);
    assertNoOptionalBeanKeys(json);

    AssetField roundTrip = fieldMapper.readValue(json, AssetField.class);
    assertEquals("displaytitle", roundTrip.getName(), json);
    assertEquals("Home banner", roundTrip.getValue(), json);
  }

  @Test
  void assetFamily_omitsNullWireFieldsFromJson() {
    Asset asset = new Asset();
    asset.setId("guid-nulls");
    asset.setName(null);
    asset.setType(null);
    asset.setFolderPath(null);

    ObjectMapper assetMapper = new JacksonContextResolver().getContext(Asset.class);
    String assetJson = assetMapper.writeValueAsString(asset);
    assertTrue(assetJson.contains("\"id\""), assetJson);
    assertTrue(assetJson.contains("guid-nulls"), assetJson);
    assertFalse(assetJson.contains("\"name\""), assetJson);
    assertFalse(assetJson.contains("\"type\""), assetJson);
    assertFalse(assetJson.contains("\"folderPath\""), assetJson);
    assertNoOptionalBeanKeys(assetJson);

    AssetField field = new AssetField();
    field.setName(null);
    field.setValue(null);
    ObjectMapper fieldMapper = new JacksonContextResolver().getContext(AssetField.class);
    String fieldJson = fieldMapper.writeValueAsString(field);
    assertFalse(fieldJson.contains("\"name\""), fieldJson);
    assertFalse(fieldJson.contains("\"value\""), fieldJson);
    assertNoOptionalBeanKeys(fieldJson);

    BinaryFile file = new BinaryFile();
    file.setFilename(null);
    file.setExtension(null);
    file.setType(null);
    ObjectMapper fileMapper = new JacksonContextResolver().getContext(BinaryFile.class);
    String fileJson = fileMapper.writeValueAsString(file);
    assertFalse(fileJson.contains("\"filename\""), fileJson);
    assertFalse(fileJson.contains("\"extension\""), fileJson);
    assertFalse(fileJson.contains("\"type\""), fileJson);
    assertNoOptionalBeanKeys(fileJson);
  }

  @Test
  void binaryFile_serializesFilenameTypeNotOptionalBeans() {
    BinaryFile file = new BinaryFile();
    file.setFilename("report.pdf");
    file.setExtension("pdf");
    file.setType("application/pdf");
    file.setSize(2048L);

    ObjectMapper fileMapper = new JacksonContextResolver().getContext(BinaryFile.class);
    String json = fileMapper.writeValueAsString(file);
    assertTrue(json.contains("\"filename\""), json);
    assertTrue(json.contains("report.pdf"), json);
    assertTrue(json.contains("\"extension\""), json);
    assertTrue(json.contains("pdf"), json);
    assertTrue(json.contains("\"type\""), json);
    assertTrue(json.contains("application/pdf"), json);
    assertTrue(json.contains("2048"), json);
    assertNoOptionalBeanKeys(json);

    BinaryFile roundTrip = fileMapper.readValue(json, BinaryFile.class);
    assertEquals("report.pdf", roundTrip.getFilename(), json);
    assertEquals("pdf", roundTrip.getExtension(), json);
    assertEquals("application/pdf", roundTrip.getType(), json);
    assertEquals(2048L, roundTrip.getSize(), json);
  }

  @Test
  void community_serializesPlainScalarsNotOptionalBeans() {
    Community community = new Community();
    community.setId(10L);
    community.setName("Default");
    community.setLabel("Default Community");
    community.setDescription("The default community");
    community.setGuid(new Guid("0-13-10"));

    ObjectMapper communityMapper = new JacksonContextResolver().getContext(Community.class);
    String json = communityMapper.writeValueAsString(community);
    assertTrue(json.contains("\"name\""), json);
    assertTrue(json.contains("Default"), json);
    assertTrue(json.contains("\"label\""), json);
    assertTrue(json.contains("Default Community"), json);
    assertTrue(json.contains("\"description\""), json);
    assertTrue(json.contains("\"guid\""), json);
    assertTrue(json.contains("0-13-10") || json.contains("10"), json);
    assertNoOptionalBeanKeys(json);

    Community communityRoundTrip = communityMapper.readValue(json, Community.class);
    assertEquals("Default", communityRoundTrip.getName(), json);
    assertEquals("Default Community", communityRoundTrip.getLabel(), json);
    assertEquals("The default community", communityRoundTrip.getDescription(), json);
    assertEquals(10L, communityRoundTrip.getId(), json);
  }

  @Test
  void communityRole_serializesRoleNameNotOptionalBeans() {
    CommunityRole role = new CommunityRole();
    role.setCommunityId(10L);
    role.setRoleId(2L);
    role.setRoleName("Admin");
    role.setCommunityGuid(new Guid("0-13-10"));
    Guid roleGuid = new Guid();
    roleGuid.setStringValue("0-8-2");
    roleGuid.setType((short) 8);
    roleGuid.setUuid(2);
    role.setRoleGuid(roleGuid);

    ObjectMapper roleMapper = new JacksonContextResolver().getContext(CommunityRole.class);
    String json = roleMapper.writeValueAsString(role);
    assertTrue(json.contains("\"roleName\""), json);
    assertTrue(json.contains("Admin"), json);
    assertTrue(json.contains("\"communityId\""), json);
    assertTrue(json.contains("\"roleId\""), json);
    assertTrue(json.contains("\"communityGuid\""), json);
    assertTrue(json.contains("\"roleGuid\""), json);
    assertNoOptionalBeanKeys(json);

    CommunityRole roleRoundTrip = roleMapper.readValue(json, CommunityRole.class);
    assertEquals("Admin", roleRoundTrip.getRoleName(), json);
    assertEquals(10L, roleRoundTrip.getCommunityId(), json);
    assertEquals(2L, roleRoundTrip.getRoleId(), json);
  }

  @Test
  void communityVisibility_serializesGuidNotOptionalBeans() {
    CommunityVisibility visibility = new CommunityVisibility();
    visibility.setId(10L);
    visibility.setGuid(new Guid("0-13-10"));

    ObjectMapper visibilityMapper =
        new JacksonContextResolver().getContext(CommunityVisibility.class);
    String json = visibilityMapper.writeValueAsString(visibility);
    assertTrue(json.contains("\"id\""), json);
    assertTrue(json.contains("10"), json);
    assertTrue(json.contains("\"guid\""), json);
    assertTrue(json.contains("0-13-10") || json.contains("13"), json);
    assertNoOptionalBeanKeys(json);

    CommunityVisibility visibilityRoundTrip =
        visibilityMapper.readValue(json, CommunityVisibility.class);
    assertEquals(10L, visibilityRoundTrip.getId(), json);
    assertNotNull(visibilityRoundTrip.getGuid(), json);
    assertEquals("0-13-10", visibilityRoundTrip.getGuid().getStringValue().orElse(null), json);
  }

  private static void assertNoOptionalBeanKeys(String json) {
    assertFalse(
        json.contains("\"empty\"") || json.contains("\"present\""),
        "JSON must not contain Optional-bean empty/present keys: " + json);
  }
}
