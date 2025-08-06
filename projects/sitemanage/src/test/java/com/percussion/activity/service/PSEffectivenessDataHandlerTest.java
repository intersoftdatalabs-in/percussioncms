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
package com.percussion.activity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.activity.data.PSContentActivity;
import com.percussion.activity.data.PSEffectiveness;
import com.percussion.activity.data.PSEffectivenessRequest;
import com.percussion.activity.service.IPSContentActivityService.PSUsageEnum;
import com.percussion.activity.service.impl.PSEffectivenessDataHandler;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PSEffectivenessDataHandler}.
 */
class PSEffectivenessDataHandlerTest {

    @Test
    void testGetEffectiveness() throws Exception {
        var handler = new PSEffectivenessDataHandler();
        handler.setFile("src/test/resources/activity/Effectiveness.xml");

        var request = new PSEffectivenessRequest();
        request.setDurationType("days");
        request.setDuration("5");
        request.setPath("/Sites/");
        request.setUsage(PSUsageEnum.pageviews);
        request.setThreshold(10);

        var emptyList = new ArrayList<PSContentActivity>();
        var eList = handler.getEffectiveness(request, emptyList);
        assertEquals(2, eList.size());

        request.setPath("/Sites/MySite.com");
        eList = handler.getEffectiveness(request, emptyList);
        assertEquals(4, eList.size());
    }
}
