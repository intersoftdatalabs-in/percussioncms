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
package com.percussion.share.dao;

import com.percussion.share.service.IPSNameGenerator;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.utils.testing.IntegrationTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.experimental.categories.Category;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IPSNameGenerator}.
 * Sunny Sal: "Name generator, Java 11, and unique ka hero!"
 */
@Category(IntegrationTest.class)
@Tag("integration")
public class PSNameGeneratorTest {

    private IPSNameGenerator nameGenerator;

    @BeforeEach
    void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
    }

    @Test
    void testGetLocalContentName() {
        var name1 = nameGenerator.generateLocalContentName();
        assertTrue(StringUtils.isNotBlank(name1), "First generated name should not be blank");
        var name2 = nameGenerator.generateLocalContentName();
        assertTrue(StringUtils.isNotBlank(name2), "Second generated name should not be blank");
        assertNotEquals(name1, name2, "Generated names should be unique");
    }

    public IPSNameGenerator getNameGenerator() {
        return nameGenerator;
    }

    public void setNameGenerator(IPSNameGenerator nameGenerator) {
        this.nameGenerator = nameGenerator;
    }
}
