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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * @author stephenbolton
 *     <p>This is picked up by Jackson automatically by the Provider annotation It will modify the
 *     serialization behavior of the objects passed in we test that the class has the same ancestor
 *     package as this class to ensure we do not modify behavior for other parts of the system
 *
 *     <p><strong>Jdk8Module is required:</strong> many rest DTOs expose {@code Optional} getters
 *     (e.g. {@link com.percussion.rest.contenttypes.ContentType#getName()}). Without the module,
 *     Jackson treats {@code Optional} as a bean and, with {@code NON_NULL}, drops name/label/guid —
 *     leaving only primitives like {@code hideFromMenu}. Live symptom: Developer content-types table
 *     full of em-dashes while adaptors correctly call design webservices ({@code IPSContentDesignWs}).
 */
// REFACTORED: CP-JAVA11
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JacksonContextResolver implements ContextResolver<ObjectMapper> {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(SerializationFeature.WRAP_ROOT_VALUE, true)
        .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
  }

  @Override
  public ObjectMapper getContext(Class<?> objectType) {
    // Only use this configuration for classes in same package and subpackages
    var pkgName = objectType.getPackage().getName();
    return (pkgName.startsWith(JacksonContextResolver.class.getPackage().getName()))
        ? objectMapper
        : null;
  }
}
