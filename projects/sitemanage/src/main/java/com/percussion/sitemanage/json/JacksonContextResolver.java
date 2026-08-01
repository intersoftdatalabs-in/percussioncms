// REFACTORED: CP-JAVA11
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

package com.percussion.sitemanage.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.system.utils.PSSiteManageBean;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;

/**
 * JacksonContextResolver is picked up by Jackson automatically via the Provider annotation. It
 * modifies the serialization behavior of objects passed in.
 *
 * <p>Configured via Jackson 3 immutable {@link JsonMapper} builder (Optional / java.time built-in).
 */
@Provider
@PSSiteManageBean("jacksonContextResolver")
@Consumes({MediaType.APPLICATION_JSON, "text/json"})
@Produces({MediaType.APPLICATION_JSON, "text/json"})
public class JacksonContextResolver implements ContextResolver<ObjectMapper> {

  private static final ObjectMapper MAPPER =
      JsonMapper.builder()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(SerializationFeature.INDENT_OUTPUT)
          .changeDefaultPropertyInclusion(
              incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
          .enable(SerializationFeature.WRAP_ROOT_VALUE)
          .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
          .disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
          .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
          .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
          .changeDefaultVisibility(
              vc -> vc.withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY))
          .annotationIntrospector(
              AnnotationIntrospector.pair(
                  new JakartaXmlBindAnnotationIntrospector(), new JacksonAnnotationIntrospector()))
          .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
          .build();

  public JacksonContextResolver() {
    // Default constructor
  }

  @Override
  public ObjectMapper getContext(Class<?> objectType) {
    return MAPPER;
  }
}
