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

package com.percussion.widgetbuilder.dao;

import com.percussion.services.widgetbuilder.IPSWidgetBuilderDefinitionDao;
import com.percussion.services.widgetbuilder.PSWidgetBuilderDefinition;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PSWidgetBuilderDefinitionDao.
 */
@Tag("IntegrationTest")
public class TestPSWidgetBuilderDefinitionDao {

    private IPSWidgetBuilderDefinitionDao dao;

    public void setDao(IPSWidgetBuilderDefinitionDao dao) {
        this.dao = dao;
    }

    @BeforeEach
    public void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
    }

    @Test
    public void testDao() throws IPSGenericDao.SaveException {
        var previousDefinitions = dao.getAll();

        var definition = new PSWidgetBuilderDefinition();
        assertEquals(-1, definition.getWidgetBuilderDefinitionId(), "Instantiated id is not equal to -1");

        dao.save(definition);

        assertNotEquals(-1, definition.getWidgetBuilderDefinitionId(), "No Id assigned during persist");

        definition.setDescription("a description");
        definition.setLabel("a label");
        definition.setPrefix("perc");
        definition.setPublisherUrl("http://www.percussion.com");
        definition.setVersion("42");
        definition.setFields("this is some field data");
        definition.setWidgetHtml("<p>here is some html with a <b>$field</b> in it</p>");
        definition.setResponsive(true);
        dao.save(definition);

        var comparisonDefinition = dao.find(definition.getWidgetBuilderDefinitionId());
        assertEquals(definition.getDescription(), comparisonDefinition.getDescription());
        assertEquals(definition.getLabel(), comparisonDefinition.getLabel());
        assertEquals(definition.getPrefix(), comparisonDefinition.getPrefix());
        assertEquals(definition.getPublisherUrl(), comparisonDefinition.getPublisherUrl());
        assertEquals(definition.getVersion(), comparisonDefinition.getVersion());
        assertEquals(definition.isResponsive(), comparisonDefinition.isResponsive());
        dao.delete(definition.getWidgetBuilderDefinitionId());

        assertNull(dao.find(definition.getWidgetBuilderDefinitionId()));

        dao.save(definition);
        var definition2 = new PSWidgetBuilderDefinition();
        definition2.setDescription("a description");
        definition2.setLabel("a label");
        definition2.setPrefix("perc");
        definition2.setPublisherUrl("http://www.percussion.com");
        definition2.setVersion("42");
        dao.save(definition2);

        var definitions = dao.getAll();
        assertEquals(2 + previousDefinitions.size(), definitions.size());

        dao.delete(definition.getWidgetBuilderDefinitionId());
        dao.delete(definition2.getWidgetBuilderDefinitionId());

        definitions = dao.getAll();
        assertEquals(previousDefinitions.size(), definitions.size());
    }
}
