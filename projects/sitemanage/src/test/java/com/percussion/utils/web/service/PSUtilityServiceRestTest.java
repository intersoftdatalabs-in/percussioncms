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

package com.percussion.utils.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.share.data.PSMapWrapper;
import com.percussion.share.test.PSRestTestCase;
import com.percussion.utils.service.impl.PSUtilityRestService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the utility REST service.
 * Refactored for Java 11 and JUnit5.
 */
public class PSUtilityServiceRestTest extends PSRestTestCase<PSUtilityRestClient> {

    private static PSUtilityRestClient utilityTestClient;

    @Override
    protected PSUtilityRestClient getRestClient(String baseUrl) {
        return utilityTestClient = new PSUtilityRestClient(baseUrl);
    }

    @BeforeAll
    public static void setUp() {
        // No-op, placeholder for future setup if needed.
    }

    @Test
    public void encryptDecryptUrlTest() {
        var defaultKey = "D6ZX#23GGS$";
        var stringToBeEncrypted = "http://yahoo.com";

        var map = new HashMap<String, String>();
        map.put(PSUtilityRestService.KEY_KEY, defaultKey);
        map.put(PSUtilityRestService.STRING_KEY, stringToBeEncrypted);
        var mapWrapper = new PSMapWrapper();
        mapWrapper.setEntries(map);

        var mw = utilityTestClient.encryptString(mapWrapper);

        map.clear();
        map.put(PSUtilityRestService.KEY_KEY, defaultKey);
        map.put(PSUtilityRestService.STRING_KEY, mw.getEntries().get(PSUtilityRestService.STRING_KEY));
        mapWrapper = new PSMapWrapper();
        mapWrapper.setEntries(map);

        mw = utilityTestClient.decryptString(mapWrapper);

        assertEquals(stringToBeEncrypted, mw.getEntries().get(PSUtilityRestService.STRING_KEY));
    }
}
