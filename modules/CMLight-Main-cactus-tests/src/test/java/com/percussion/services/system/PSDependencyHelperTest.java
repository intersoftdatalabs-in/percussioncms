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
package com.percussion.services.system;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.system.impl.PSDependencyHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case for the {@link PSDependencyHelper}. This test depends on the
 * FastForward implementation and sample content.
 */
@Tag("IntegrationTest")
public class PSDependencyHelperTest {

    /**
     * Tests finding various dependencies.
     */
    @Test
    void testFindDependents() {
        var dh = new PSDependencyHelper();
        assertFalse(dh.findDependents(new PSGuid(PSTypeEnum.WORKFLOW, 4)).isEmpty());
        assertFalse(dh.findDependents(new PSGuid(PSTypeEnum.WORKFLOW, 5)).isEmpty());
        assertFalse(dh.findDependents(new PSGuid(PSTypeEnum.LOCALE, 1)).isEmpty());
        assertFalse(dh.findDependents(new PSGuid(PSTypeEnum.COMMUNITY_DEF, 1001)).isEmpty());
        assertFalse(dh.findDependents(new PSGuid(PSTypeEnum.DISPLAY_FORMAT, 0)).isEmpty());
        assertFalse(dh.findDependents(new PSGuid(PSTypeEnum.SLOT, 516)).isEmpty());
        assertFalse(dh.findDependents(new PSGuid(PSTypeEnum.SITE, 303)).isEmpty());
        assertFalse(dh.findDependents(new PSGuid(PSTypeEnum.TEMPLATE, 521)).isEmpty());
        IPSGuid[] guids = new IPSGuid[2];
        guids[0] = new PSGuid(PSTypeEnum.TEMPLATE, 521);
        guids[1] = new PSGuid(PSTypeEnum.NODEDEF, 307);
        assertFalse(dh.findDependents(guids).isEmpty());
    }
}
