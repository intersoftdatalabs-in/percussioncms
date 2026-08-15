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

import com.percussion.rest.assets.Asset;
import com.percussion.rest.assets.AssetField;
import com.percussion.rest.assets.AssetFieldList;
import com.percussion.rest.assets.BinaryFile;
import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeList;
import com.percussion.rest.templates.TemplateSummary;
import com.percussion.rest.templates.TemplateSummaryList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Catalog DTOs must serialize names (and related fields) for Developer SPA tables. ContentType list
 * historically collapsed to hideFromMenu-only when Optional getters were not unwrapped (issue
 * #1693). TemplateSummary similarly collapsed to templateId-only (issue #2189). Both now use plain
 * getters under {@code @JsonInclude(NON_NULL)}.
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

  private static void assertNoOptionalBeanKeys(String json) {
    assertFalse(
        json.contains("\"empty\"") || json.contains("\"present\""),
        "JSON must not contain Optional-bean empty/present keys: " + json);
  }
}
