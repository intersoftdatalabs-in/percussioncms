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
package com.percussion.services.touchitem;

import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case for the {@link PSTouchItemConfiguration} class.
 */
@Tag("IntegrationTest")
public class PSTouchItemConfigurationTest {

    private PSTouchItemConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new PSTouchItemConfiguration();

        var bean1 = new PSTouchItemConfigBean();
        var srcTypes = new HashSet<String>();
        srcTypes.add("rffFile");
        var tgtTypes = new HashSet<String>();
        tgtTypes.add("rffGeneric");
        bean1.setSourceTypes(srcTypes);
        bean1.setTargetTypes(tgtTypes);
        bean1.setLevel(0);
        bean1.setTouchAAParents(false);

        var bean2 = new PSTouchItemConfigBean();
        srcTypes = new HashSet<>();
        srcTypes.add("rffFile");
        srcTypes.add("rffBrief");
        tgtTypes = new HashSet<>();
        tgtTypes.add("rffGeneric");
        tgtTypes.add("rffImage");
        bean2.setSourceTypes(srcTypes);
        bean2.setTargetTypes(tgtTypes);
        bean2.setLevel(-1);
        bean2.setTouchAAParents(true);

        var bean3 = new PSTouchItemConfigBean();
        srcTypes = new HashSet<>();
        srcTypes.add("rffFile");
        srcTypes.add("rffBrief");
        srcTypes.add("rffContacts");

        tgtTypes = new HashSet<>();
        tgtTypes.add("rffGeneric");
        tgtTypes.add("rffImage");
        tgtTypes.add("rffNavTree");
        tgtTypes.add("rffCalendar");
        bean3.setSourceTypes(srcTypes);
        bean3.setTargetTypes(tgtTypes);
        bean3.setLevel(-2);
        bean3.setTouchAAParents(false);

        var configBeans = new HashSet<PSTouchItemConfigBean>();
        configBeans.add(bean1);
        configBeans.add(bean2);
        configBeans.add(bean3);

        configuration.setTouchItemConfig(configBeans);
    }

    @Test
    void testGetTouchItemConfigMap() {
        var itemConfig = new PSTouchItemConfiguration();
        assertTrue(itemConfig.getTouchItemConfig().isEmpty());
        assertTrue(itemConfig.getTouchItemConfigMap().isEmpty());

        var configMap = configuration.getTouchItemConfigMap();
        assertEquals(3, configMap.size());

        var configBeans = configMap.get(309L);
        assertEquals(3, configBeans.size());

        configBeans = configMap.get(302L);
        assertEquals(2, configBeans.size());

        configBeans = configMap.get(305L);
        assertEquals(1, configBeans.size());
    }

    @Test
    void testGetLevelTargetTypes() {
        Map<Integer, Set<String>> levelTargetTypes = configuration.getLevelTargetTypes(309L);

        var targetTypes = levelTargetTypes.get(0);
        assertEquals(1, targetTypes.size());
        assertTrue(targetTypes.contains("rffGeneric"));

        targetTypes = levelTargetTypes.get(-1);
        assertEquals(2, targetTypes.size());
        assertTrue(targetTypes.contains("rffGeneric"));
        assertTrue(targetTypes.contains("rffImage"));

        targetTypes = levelTargetTypes.get(-2);
        assertEquals(4, targetTypes.size());
        assertTrue(targetTypes.contains("rffGeneric"));
        assertTrue(targetTypes.contains("rffImage"));
        assertTrue(targetTypes.contains("rffNavTree"));
        assertTrue(targetTypes.contains("rffCalendar"));

        levelTargetTypes = configuration.getLevelTargetTypes(302L);

        targetTypes = levelTargetTypes.get(0);
        assertNull(targetTypes);

        targetTypes = levelTargetTypes.get(-1);
        assertEquals(2, targetTypes.size());
        assertTrue(targetTypes.contains("rffGeneric"));
        assertTrue(targetTypes.contains("rffImage"));

        targetTypes = levelTargetTypes.get(-2);
        assertEquals(4, targetTypes.size());
        assertTrue(targetTypes.contains("rffGeneric"));
        assertTrue(targetTypes.contains("rffImage"));
        assertTrue(targetTypes.contains("rffNavTree"));
        assertTrue(targetTypes.contains("rffCalendar"));

        levelTargetTypes = configuration.getLevelTargetTypes(305L);

        targetTypes = levelTargetTypes.get(0);
        assertNull(targetTypes);

        targetTypes = levelTargetTypes.get(-1);
        assertNull(targetTypes);

        targetTypes = levelTargetTypes.get(-2);
        assertEquals(4, targetTypes.size());
        assertTrue(targetTypes.contains("rffGeneric"));
        assertTrue(targetTypes.contains("rffImage"));
        assertTrue(targetTypes.contains("rffNavTree"));
        assertTrue(targetTypes.contains("rffCalendar"));
    }

    @Test
    void testShouldTouchAAParents() {
        var tgtTypes = new HashSet<String>();
        tgtTypes.add("rffGeneric");
        assertFalse(configuration.shouldTouchAAParents(309L, 0, tgtTypes));
        assertFalse(configuration.shouldTouchAAParents(309L, -1, tgtTypes));

        tgtTypes.add("rffImage");
        assertTrue(configuration.shouldTouchAAParents(309L, -1, tgtTypes));
        assertTrue(configuration.shouldTouchAAParents(302L, -1, tgtTypes));

        tgtTypes.add("rffNavTree");
        tgtTypes.add("rffCalendar");
        assertFalse(configuration.shouldTouchAAParents(309L, -2, tgtTypes));
        assertFalse(configuration.shouldTouchAAParents(302L, -2, tgtTypes));
        assertFalse(configuration.shouldTouchAAParents(305L, -2, tgtTypes));
    }

    @Test
    void testGetMinimumLevel() {
        var itemConfig = new PSTouchItemConfiguration();
        assertNull(itemConfig.getMinimumLevel());

        assertEquals(-2, configuration.getMinimumLevel());
    }
}
