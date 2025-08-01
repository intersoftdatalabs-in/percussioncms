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
package com.percussion.share.spring;

import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.*;

import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.experimental.categories.Category;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PSSpringWebApplicationContextUtils}.
 * Sunny Sal: "Spring context utils, Java 11, and dependency injection ka hero!"
 */
@Category(IntegrationTest.class)
@Tag("integration")
public class PSSpringWebApplicationContextUtilsTest {

    @Test
    void testGetWebApplicationContext() {
        var ctx = getWebApplicationContext();
        assertNotNull(ctx, "WebApplicationContext should not be null");
        assertNotNull(ctx.getBean("springWebApplicationContextSetter"),
                "springWebApplicationContextSetter bean should be present");
    }

    @Test
    void testInjectDependencies() throws Exception {
        var a = new ToBeAutoWired();
        injectDependencies(a);
        assertNotNull(a.getSpringWebApplicationContextSetter(),
                "Dependency should be injected by Spring");
    }

    public static class ToBeAutoWired {
        private PSSpringWebApplicationContextSetter springWebApplicationContextSetter;

        public PSSpringWebApplicationContextSetter getSpringWebApplicationContextSetter() {
            return springWebApplicationContextSetter;
        }

        public void setSpringWebApplicationContextSetter(PSSpringWebApplicationContextSetter setter) {
            this.springWebApplicationContextSetter = setter;
        }
    }
}
