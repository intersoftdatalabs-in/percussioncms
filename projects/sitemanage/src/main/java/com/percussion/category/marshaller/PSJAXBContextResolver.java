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

// REFACTORED: CP-JAVA11

package com.percussion.category.marshaller;

import com.percussion.category.data.PSCategory;
import com.percussion.category.data.PSCategoryNode;
import com.percussion.category.data.PSDateAdapter;
import com.percussion.system.utils.PSSiteManageBean;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.xml.bind.JAXBException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;

@PSSiteManageBean("categoryContextResolver")
public class PSJAXBContextResolver implements ContextResolver<ObjectMapper> {

  private final ObjectMapper objectMapper;
  private final Class<?>[] types = {PSCategory.class, PSCategoryNode.class, PSDateAdapter.class};
  private static final Logger log = LogManager.getLogger(PSJAXBContextResolver.class);

  public PSJAXBContextResolver() throws JAXBException {
    this.objectMapper =
        JsonMapper.builder()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .annotationIntrospector(
                AnnotationIntrospector.pair(
                    new JacksonAnnotationIntrospector(),
                    new JakartaXmlBindAnnotationIntrospector()))
            .build();
  }

  @Override
  public ObjectMapper getContext(Class<?> arg0) {
    for (var type : types) {
      if (type == arg0) {
        log.debug("Check changes to PSJAXBContextResolver");
        return this.objectMapper;
      }
    }
    return null;
  }
}
