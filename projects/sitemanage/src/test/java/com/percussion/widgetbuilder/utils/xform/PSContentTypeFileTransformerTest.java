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
package com.percussion.widgetbuilder.utils.xform;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.design.objectstore.PSControlMeta;
import com.percussion.widgetbuilder.data.PSWidgetBuilderFieldData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderFieldData.FieldType;
import com.percussion.widgetbuilder.utils.PSWidgetPackageSpec;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PSContentTypeFileTransformer.
 */
public class PSContentTypeFileTransformerTest {

    private static PSWidgetPackageSpec packageSpec;

    @BeforeEach All
    public static void beforeAll() {
        packageSpec = setupPackageSpec();
    }

    @Test
    public void testHandleFile() throws Exception {
        var xform = new PSContentTypeFileTransformer(new PSControlMgr());
        assertTrue(xform.handleFile(new File("/test/myWidget.schemaDef.contentType")));
        assertTrue(xform.handleFile(new File("/test/myWidget.itemDef.contentType")));
        assertFalse(xform.handleFile(new File("/test/myWidget.nodeDef.contentType")));
        assertFalse(xform.handleFile(new File("/test/myWidget.xml")));
    }

    @Test
    public void testTransformSchemaFile() throws Exception {
        var xform = new PSContentTypeFileTransformer(new PSControlMgr());
        var file = new File("/test/myWidget.schemaDef.contentType");
        try (
            Reader resultReader = new InputStreamReader(this.getClass().getResourceAsStream("transformSchema.xml"));
            Reader expectedReader = new InputStreamReader(this.getClass().getResourceAsStream("expectedSchema.xml"))
        ) {
            var expected = PSXmlDocumentBuilder.toString(xform.getSchema(expectedReader).toXml(PSXmlDocumentBuilder.createXmlDocument()));
            var result = IOUtils.toString(xform.transformFile(file, resultReader, packageSpec));
            assertEquals(expected, result);
        }
    }

    @Test
    public void testTransformItemDefFile() throws Exception {
        var xform = new PSContentTypeFileTransformer(new PSControlMgr());
        var file = new File("/test/myWidget.itemDef.contentType");
        try (Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("transformItemDef.xml"))) {
            var expectedItemDef = new PSItemDefinition(PSXmlDocumentBuilder.createXmlDocument(this.getClass().getResourceAsStream("expectedItemDef.xml"), false).getDocumentElement());
            var expected = PSXmlDocumentBuilder.toString(expectedItemDef.toXml(PSXmlDocumentBuilder.createXmlDocument()));
            var result = IOUtils.toString(xform.transformFile(file, reader, packageSpec));
            assertEquals(expected, result);
        }
    }

    public static PSWidgetPackageSpec setupPackageSpec() {
        var spec = new PSWidgetPackageSpec("pre", "url", "MyWidget", "", "1.0.0", "3.2.1");
        var fields = new ArrayList<PSWidgetBuilderFieldData>();
        var field = new PSWidgetBuilderFieldData();
        field.setName("Author");
        field.setLabel(field.getName());
        field.setType(FieldType.TEXT.name());
        fields.add(field);

        field = new PSWidgetBuilderFieldData();
        field.setName("ContentDate");
        field.setLabel("Content Date");
        field.setType(FieldType.DATE.name());
        fields.add(field);

        field = new PSWidgetBuilderFieldData();
        field.setName("ContentAbstract");
        field.setLabel("Content Abstract");
        field.setType(FieldType.TEXT_AREA.name());
        fields.add(field);

        field = new PSWidgetBuilderFieldData();
        field.setName("Article");
        field.setLabel(field.getName());
        field.setType(FieldType.RICH_TEXT.name());
        fields.add(field);

        field = new PSWidgetBuilderFieldData();
        field.setName("ImageField");
        field.setLabel(field.getName());
        field.setType(FieldType.IMAGE.name());
        fields.add(field);

        spec.setFields(fields);

        return spec;
    }

    private static class PSControlMgr implements IPSControlManager {
        PSControlMeta ctrlMeta;

        public PSControlMgr() throws Exception {
            ctrlMeta = new PSControlMeta((Element) PSXmlDocumentBuilder.createXmlDocument(this.getClass().getResourceAsStream("controlMeta.xml"), false).getDocumentElement().getElementsByTagName("psxctl:ControlMeta").item(0));
        }

        @Override
        public PSControlMeta getControl(String name) {
            return ctrlMeta;
        }
    }
}
