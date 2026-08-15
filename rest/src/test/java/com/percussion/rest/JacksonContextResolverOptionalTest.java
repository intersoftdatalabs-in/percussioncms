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
import com.percussion.rest.displayformat.DisplayFormatProperty;
import com.percussion.rest.pages.CalendarInfo;
import com.percussion.rest.pages.CodeInfo;
import com.percussion.rest.pages.Page;
import com.percussion.rest.pages.Region;
import com.percussion.rest.pages.SeoInfo;
import com.percussion.rest.pages.Widget;
import com.percussion.rest.pages.WorkflowInfo;
import com.percussion.rest.templates.Template;
import com.percussion.rest.templates.TemplateBinding;
import com.percussion.rest.templates.TemplateSummary;
import com.percussion.rest.templates.TemplateSummaryList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Catalog DTOs must serialize names (and related fields) for Developer SPA tables. ContentType list
 * historically collapsed to hideFromMenu-only when Optional getters were not unwrapped (issue
 * #1693). TemplateSummary similarly collapsed to templateId-only (issue #2189). DisplayFormatProperty
 * / Template / Page family follow the same plain-getter rule (issue #3407). All use production
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
  void displayFormatProperty_serializesPlainScalarsNotOptionalBeans() {
    DisplayFormatProperty prop = new DisplayFormatProperty();
    prop.setPropertyId("1");
    prop.setPropertyName("sortColumn");
    prop.setPropertyValue("sys_title");
    prop.setDescription("Default sort");

    ObjectMapper propMapper =
        new JacksonContextResolver().getContext(DisplayFormatProperty.class);
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

  private static void assertNoOptionalBeanKeys(String json) {
    assertFalse(
        json.contains("\"empty\"") || json.contains("\"present\""),
        "JSON must not contain Optional-bean empty/present keys: " + json);
  }
}
