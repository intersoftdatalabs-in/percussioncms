// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.widgetbuilder.service;

import com.percussion.error.PSExceptionUtils;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.widgetbuilder.data.PSWidgetBuilderDefinitionData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderFieldData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderFieldData.FieldType;
import com.percussion.widgetbuilder.data.PSWidgetBuilderResourceListData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderSummaryData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderValidationResults;
import com.percussion.widgetbuilder.utils.xform.PSContentTypeFileTransformerTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WidgetBuilderService.
 */
@Tag("IntegrationTest")
public class TestWidgetBuilderService {

    private static final Logger log = LogManager.getLogger(TestWidgetBuilderService.class);

    IPSWidgetBuilderService service;

    public void setService(IPSWidgetBuilderService service) {
        this.service = service;
    }

    @BeforeEach
    public void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
        // PSSecurityFilter.authenticate(request, response, "Admin", "demo"); // Not needed in JUnit5 context
    }

    @Test
    public void testServiceCRUD() {
        var currentSize = service.loadAll().size();

        Set<String> ids = new HashSet<>();
        long badId = 0;

        try {
            var definition = new PSWidgetBuilderDefinitionData();
            assertEquals(0, Long.parseLong(definition.getId()), "Instantiated id is not equal to 0");
            assertNotNull(definition.getFieldsList().getFields(), "Instantiated fields is null");
            assertTrue(definition.getFieldsList().getFields().isEmpty(), "Instantiated fields is not empty");

            var results = service.saveWidgetBuilderDefinition(definition);
            badId = results.getDefinitionId();
            assertEquals(0, badId, "No Id assigned during persist");
            assertFalse(results.getResults().isEmpty());

            definition.setDescription("a description");
            definition.setLabel("alabel");
            definition.setPrefix("perc");
            definition.setPublisherUrl("http://www.percussion.com");
            definition.setVersion("1.0.0");
            definition.setAuthor("Dr. Caligari");
            definition.setResponsive(true);
            definition = roundTrip(ids, definition);

            var comparisonDefinition = service.loadWidgetDefinition(Long.parseLong(definition.getId()));
            assertEquals(definition.getDescription(), comparisonDefinition.getDescription());
            assertEquals(definition.getLabel(), comparisonDefinition.getLabel());
            assertEquals(definition.getPrefix(), comparisonDefinition.getPrefix());
            assertEquals(definition.getPublisherUrl(), comparisonDefinition.getPublisherUrl());
            assertEquals(definition.getVersion(), comparisonDefinition.getVersion());
            assertEquals(definition.getAuthor(), comparisonDefinition.getAuthor());
            service.deleteWidgetBuilderDefinition(Long.parseLong(definition.getId()));
            ids.remove(definition.getId());

            assertNull(service.loadWidgetDefinition(Long.parseLong(definition.getId())));

            definition = roundTrip(ids, definition);
            var definition2 = new PSWidgetBuilderDefinitionData();
            definition2.setDescription("a description");
            definition2.setLabel("alabel2");
            definition2.setPrefix("perc");
            definition2.setPublisherUrl("http://www.percussion.com");
            definition2.setVersion("1.0.0");
            definition2.setAuthor("Dr. Strangelove");
            definition2 = roundTrip(ids, definition2);

            var definitions = service.loadAll();
            assertEquals(ids.size() + currentSize, definitions.size());

            var sums = service.loadAllSummaries();
            assertEquals(definitions.size(), sums.size());
            var expectedSums = new ArrayList<PSWidgetBuilderSummaryData>();
            for (var data : definitions) {
                expectedSums.add(new PSWidgetBuilderSummaryData(data));
            }
            assertEquals(expectedSums, sums);

            // test fields & html
            var fields = definition.getFieldsList().getFields();

            var textField = new PSWidgetBuilderFieldData();
            textField.setName("textField");
            textField.setLabel("Text Field");
            textField.setType(FieldType.TEXT.toString());
            fields.add(textField);

            var areaField = new PSWidgetBuilderFieldData();
            areaField.setName("textArea");
            areaField.setLabel("Text Area");
            areaField.setType(FieldType.TEXT_AREA.toString());
            fields.add(areaField);

            var dateField = new PSWidgetBuilderFieldData();
            dateField.setName("dateField");
            dateField.setLabel("Date Field");
            dateField.setType(FieldType.DATE.toString());
            fields.add(dateField);

            var richField = new PSWidgetBuilderFieldData();
            richField.setName("richText");
            richField.setLabel("Rich Text");
            richField.setType(FieldType.RICH_TEXT.toString());
            fields.add(richField);

            var imgField = new PSWidgetBuilderFieldData();
            imgField.setName("imgField");
            imgField.setLabel("Image Field");
            imgField.setType(FieldType.IMAGE.toString());
            fields.add(imgField);

            var html = new StringBuilder("<ul>");
            for (var field : fields) {
                if (field.getType().equals(FieldType.IMAGE.name())) {
                    html.append("<li><img src=\"$").append(field.getName()).append("_path\"/></li>");
                } else {
                    html.append("<li>$").append(field.getName()).append("</li>");
                }
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

            var savedDef = roundTrip(ids, definition);
            assertEquals(definition, savedDef);

            service.deleteWidgetBuilderDefinition(Long.parseLong(definition.getId()));
            ids.remove(definition.getId());
            service.deleteWidgetBuilderDefinition(Long.parseLong(definition2.getId()));
            ids.remove(definition2.getId());

            definitions = service.loadAll();
            assertEquals(currentSize, definitions.size());
        } catch (Exception e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            fail("Exception: " + e.getLocalizedMessage());
        } finally {
            if (badId > 0)
                deleteQuietly(badId);

            for (var defId : ids) {
                deleteQuietly(Long.parseLong(defId));
            }
        }
    }

    private void deleteQuietly(long id) {
        try {
            service.deleteWidgetBuilderDefinition(id);
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    private PSWidgetBuilderDefinitionData roundTrip(Set<String> ids, PSWidgetBuilderDefinitionData definition) {
        var results = service.saveWidgetBuilderDefinition(definition);
        assertTrue(results.getResults().isEmpty());
        ids.add(String.valueOf(results.getDefinitionId()));
        definition = service.loadWidgetDefinition(results.getDefinitionId());
        assertNotNull(definition);
        return definition;
    }

    @Test
    public void testPackagingandDeploy() {
        long id = 0;
        try {
            var definition = new PSWidgetBuilderDefinitionData();
            definition.setDescription("a description");
            definition.setLabel("alabel");
            definition.setPrefix("perc");
            definition.setPublisherUrl("http://www.percussion.com");
            definition.setVersion("3.2.1");
            definition.setAuthor("Dr. Caligari");
            definition.setResponsive(true);
            definition.getFieldsList().setFields(PSContentTypeFileTransformerTest.setupPackageSpec().getFields());

            var html = new StringBuilder("<ul>");
            for (var field : definition.getFieldsList().getFields()) {
                if (field.getType().equals(FieldType.IMAGE.name())) {
                    html.append("<li><img src=\"$").append(field.getName()).append("_path\" title=\"$")
                            .append(field.getName()).append("_title\" alt=\"$")
                            .append(field.getName()).append("_alt_text\" /></li>");
                } else {
                    html.append("<li>$").append(field.getName()).append("</li>");
                }
            }
            html.append("</ul>");
            definition.setWidgetHtml(html.toString());

            definition = service.loadWidgetDefinition(service.saveWidgetBuilderDefinition(definition).getDefinitionId());

            id = Long.parseLong(definition.getId());
            service.deployWidget(id);
        } finally {
            if (id > 0)
                service.deleteWidgetBuilderDefinition(id);
        }
    }
}
