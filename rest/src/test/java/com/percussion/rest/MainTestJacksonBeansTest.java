/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.rest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for v8.1.7 PR #574 residue: Jackson provider beans used by {@link MainTest}
 * must be constructible as static {@code @Bean} factories (Spring Framework 6+ / 7-safe).
 *
 * <p>Does not start the full CXF REST server — only the Jackson bean wiring from #574.
 */
class MainTestJacksonBeansTest {

  @Test
  void staticJacksonJsonProviderBeanIsConstructible() {
    JacksonJsonProvider provider = MainTest.ContextConfiguration.getJacksonJsonProvider();
    assertNotNull(provider);
  }

  @Test
  void staticJacksonContextResolverBeanProvidesConfiguredMapper() {
    JacksonContextResolver resolver = MainTest.ContextConfiguration.getContextResolver();
    assertNotNull(resolver);

    // Resolver only applies to com.percussion.rest.* types
    ObjectMapper mapper = resolver.getContext(JacksonContextResolver.class);
    assertNotNull(mapper);
    // Production resolver static block: unknown properties fail; empty string → null object
    assertTrue(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    assertTrue(mapper.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT));
    // Same singleton for rest-package types; non-rest packages get null
    assertSame(mapper, resolver.getContext(MainTest.class));
    // java.lang.String is outside com.percussion.rest
    org.junit.jupiter.api.Assertions.assertNull(resolver.getContext(String.class));
  }
}
