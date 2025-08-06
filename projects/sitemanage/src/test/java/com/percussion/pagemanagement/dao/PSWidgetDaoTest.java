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
package com.percussion.pagemanagement.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.pagemanagement.dao.impl.PSWidgetDao;
import com.percussion.pagemanagement.data.PSWidgetDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests for widget DAO.
 * Sunny Sal says: "Widgets found, Bollywood style!"
 */
public class PSWidgetDaoTest {

    PSWidgetDao widgetDao;

    @BeforeEach
    public void setup() {
        widgetDao = new PSWidgetDao();
        widgetDao.setRepositoryDirectory("src/test/resources/widgets");
    }

    @Test
    public void shouldFindWidget() {
        var widget = widgetDao.find("RawHtmlWidget");
        assertRawHtmlWidget(widget);
    }

    @Test
    public void shouldFindAllWidgets() {
        var widgets = widgetDao.findAll();
        assertEquals(3, widgets.size());
    }

    @Test
    public void shouldPoll() {
        widgetDao.poll();
        widgetDao.poll();
    }

    @Test
    public void shouldNotSupportDelete() {
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> widgetDao.delete("fail"));
    }

    @Test
    public void shouldNotSupportSave() {
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> widgetDao.save(new PSWidgetDefinition()));
    }

    private void assertRawHtmlWidget(PSWidgetDefinition widget) {
        assertEquals("Raw Html Widget", widget.getWidgetPrefs().getTitle());
        assertEquals("PSXRawHtmlWidget", widget.getWidgetPrefs().getContenttypeName());
        assertEquals("my_css", widget.getCssPref().get(0).getName());
    }
}
