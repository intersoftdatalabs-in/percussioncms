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
import com.percussion.rest.locationscheme.LocationScheme;
import com.percussion.rest.locationscheme.LocationSchemeParameter;
import com.percussion.rest.locationscheme.LocationSchemeParameterList;
import com.percussion.rest.templates.TemplateSummary;
import com.percussion.rest.templates.TemplateSummaryList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Catalog DTOs must serialize names (and related fields) for Developer SPA tables. ContentType list
 * historically collapsed to hideFromMenu-only when Optional getters were not unwrapped (issue
 * #1693). TemplateSummary similarly collapsed to templateId-only (issue #2189). LocationScheme /
 * Context / DeliveryType follow the same plain-getter contract (issue #3412). All use plain getters
 * under {@code @JsonInclude(NON_NULL)}.
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
