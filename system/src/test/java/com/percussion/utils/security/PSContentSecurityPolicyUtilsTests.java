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

package com.percussion.utils.security;

import com.percussion.delivery.data.PSDeliveryInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PSContentSecurityPolicyUtilsTests {


    @BeforeEach
    public void setup(){

    }

    @AfterEach
    public void teardown(){

    }

    @Test
    @Disabled //FIXME Test is failing
    public void testEditCSP(){
        List<PSDeliveryInfo> psDeliveryInfoList = new ArrayList<>();
        String contentSecurityString = PSSecurityUtility.CONTENT_SECURITY_POLICY_DEFAULT;

        String edited = PSContentSecurityPolicyUtils.editContentSecurityPolicy(psDeliveryInfoList,contentSecurityString);

        assertNotNull(edited);

        assertEquals(contentSecurityString, edited);

    }
}
