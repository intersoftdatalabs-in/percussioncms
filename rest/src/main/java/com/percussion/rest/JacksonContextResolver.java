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

package com.percussion.rest;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author stephenbolton
 *     <p>This is picked up by Jackson automatically by the Provider annotation It will modify the
 *     serialization behavior of the objects passed in we test that the class has the same ancestor
 *     package as this class to ensure we do not modify behavior for other parts of the system
 *     <p>Jackson 3 embeds Optional / java.time support in databind — no Jdk8Module or
 *     JavaTimeModule registration required. Many rest DTOs expose {@code Optional} getters (e.g.
 *     ContentType name); without that support, catalog tables serialize empty (hideFromMenu-only
 *     payloads).
 */
// REFACTORED: CP-JAVA11
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JacksonContextResolver implements ContextResolver<ObjectMapper> {
  private static final ObjectMapper objectMapper =
      JsonMapper.builder()
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
          .enable(SerializationFeature.WRAP_ROOT_VALUE)
          .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
          .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
          .build();

  @Override
  public ObjectMapper getContext(Class<?> objectType) {
    // Only use this configuration for classes in same package and subpackages
    var pkgName = objectType.getPackage().getName();
    return (pkgName.startsWith(JacksonContextResolver.class.getPackage().getName()))
        ? objectMapper
        : null;
  }
}
