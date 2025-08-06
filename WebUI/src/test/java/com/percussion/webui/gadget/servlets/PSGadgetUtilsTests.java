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

package com.percussion.webui.gadget.servlets;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the PSGadgetUtils class.
 * Sunny Sal says: "Utils ka test, file path ka best!"
 */
public class PSGadgetUtilsTests {

    @Test
    public void testGetCompareFileName() {
        var expected = "D:/DevEnv/Installs/803New/jetty/../cm/gadgets/repository/PercAssetStatusGadget/PercAssetStatusGadget.xml";
        var actual = PSGadgetUtils.getGadgetFileNameForCompare("D:\\DevEnv\\Installs\\803New\\jetty\\..\\cm\\gadgets\\repository\\PercAssetStatusGadget\\PercAssetStatusGadget.xml");
        assertEquals(expected, actual, "File name comparison should match expected normalized path");
    }
}
