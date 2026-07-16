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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Coverage for v8.1.7 PR #574 residue: Jackson provider beans used by {@link MainTest} must be
 * constructible as static {@code @Bean} factories and resolvable through Spring Framework 6+/7
 * configuration-class processing.
 *
 * <p>Does not start the full CXF REST server (that path is covered by {@link MainTest}). Boots a
 * minimal Spring context whose static {@code @Bean} methods delegate to {@link
 * MainTest.ContextConfiguration} factories so a regression that only appears during Spring
 * bootstrap would fail here.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MainTestJacksonBeansTest.StaticJacksonSpringConfig.class)
class MainTestJacksonBeansTest {

  @Autowired private ApplicationContext applicationContext;
  @Autowired private JacksonJsonProvider jacksonJsonProvider;
  @Autowired private JacksonContextResolver jacksonContextResolver;

  /**
   * Minimal config that exercises Spring's static {@code @Bean} processing using the same factory
   * methods as {@link MainTest.ContextConfiguration}.
   */
  @Configuration
  static class StaticJacksonSpringConfig {
    @Bean
    public static JacksonJsonProvider jacksonJsonProvider() {
      return MainTest.ContextConfiguration.getJacksonJsonProvider();
    }

    @Bean
    public static JacksonContextResolver jacksonContextResolver() {
      return MainTest.ContextConfiguration.getContextResolver();
    }
  }

  @Test
  void springContextResolvesStaticJacksonBeans() {
    assertNotNull(applicationContext);
    assertNotNull(jacksonJsonProvider);
    assertNotNull(jacksonContextResolver);
    // Also resolvable by type from the context (Spring-managed, not plain static calls)
    assertNotNull(applicationContext.getBean(JacksonJsonProvider.class));
    assertNotNull(applicationContext.getBean(JacksonContextResolver.class));
  }

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
    assertNull(resolver.getContext(String.class));
  }

  @Test
  void springInjectedResolverMatchesPackageScopedMapperBehavior() {
    ObjectMapper mapper = jacksonContextResolver.getContext(JacksonContextResolver.class);
    assertNotNull(mapper);
    assertTrue(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    assertNull(jacksonContextResolver.getContext(String.class));
  }
}
