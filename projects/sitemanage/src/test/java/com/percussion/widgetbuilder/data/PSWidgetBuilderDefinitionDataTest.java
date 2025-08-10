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
package com.percussion.widgetbuilder.data;

import static com.percussion.share.test.PSDataObjectTestUtils.assertXmlSerialization;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.data.PSDataObjectTestCase;
import org.junit.jupiter.api.Test;

/** Tests for PSWidgetBuilderDefinitionData. */
public class PSWidgetBuilderDefinitionDataTest
    extends PSDataObjectTestCase<PSWidgetBuilderDefinitionData> {

  @Override
  public PSWidgetBuilderDefinitionData getObject() throws Exception {
    var definition = new PSWidgetBuilderDefinitionData();
    definition.setDescription("a description");
    definition.setLabel("a label");
    definition.setPrefix("perc");
    definition.setPublisherUrl("http://www.percussion.com");
    definition.setVersion("42");
    definition.setAuthor("Dr. Caligari");
    definition.setId("1");
    definition.setResponsive(true);

    var fields = definition.getFieldsList().getFields();

    var textField = new PSWidgetBuilderFieldData();
    textField.setName("textField");
    textField.setLabel("Text Field");
    textField.setType(PSWidgetBuilderFieldData.FieldType.TEXT.toString());
    fields.add(textField);

    var areaField = new PSWidgetBuilderFieldData();
    areaField.setName("textArea");
    areaField.setLabel("Text Area");
    areaField.setType(PSWidgetBuilderFieldData.FieldType.TEXT_AREA.toString());
    fields.add(areaField);

    var dateField = new PSWidgetBuilderFieldData();
    dateField.setName("dateField");
    dateField.setLabel("Date Field");
    dateField.setType(PSWidgetBuilderFieldData.FieldType.DATE.toString());
    fields.add(dateField);

    var richField = new PSWidgetBuilderFieldData();
    richField.setName("richText");
    richField.setLabel("Rich Text");
    richField.setType(PSWidgetBuilderFieldData.FieldType.RICH_TEXT.toString());
    fields.add(richField);

    var imgField = new PSWidgetBuilderFieldData();
    imgField.setName("imgField");
    imgField.setLabel("Image Field");
    imgField.setType(PSWidgetBuilderFieldData.FieldType.IMAGE.toString());
    fields.add(imgField);

    var html = new StringBuilder("<ul>");
    for (var field : fields) {
      html.append("<li>$").append(field.getName()).append("</li>");
    }
    html.append("</ul>");
    definition.setWidgetHtml(html.toString());

    var jsFiles = new PSWidgetBuilderResourceListData();
    var files = jsFiles.getResourceList();
    files.add("/foo/bar.js");
    files.add("/foo/bar2.js");
    definition.setJsFileList(jsFiles);

    var cssFiles = new PSWidgetBuilderResourceListData();
    files = cssFiles.getResourceList();
    files.add("/foo/bar.js");
    files.add("/foo/bar2.js");
    definition.setCssFileList(cssFiles);

    return definition;
  }

  @Test
  public void testJsonSerialization() throws Exception {
    var json = PSSerializerUtils.getJsonFromObject(object);
    // Optionally assert JSON structure here
  }

  @Test
  public void testToFromDao() throws Exception {
    var data =
        new PSWidgetBuilderDefinitionData(PSWidgetBuilderDefinitionData.createDaoObject(object));
    assertEquals(object, data);
  }

  @Test
  public void testSummaryData() throws Exception {
    var sum = new PSWidgetBuilderSummaryData(object);
    assertXmlSerialization(sum);
  }
}
