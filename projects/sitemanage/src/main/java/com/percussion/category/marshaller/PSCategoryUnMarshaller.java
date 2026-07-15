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

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.jaxb.XmlJaxbAnnotationIntrospector;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.percussion.category.data.PSCategory;
import com.percussion.category.data.PSCategoryNode;
import com.percussion.category.transformer.PSCategoryXmlTransform;
import com.percussion.server.PSServer;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("categoryUnmarshaller")
@Lazy
public class PSCategoryUnMarshaller {

  private static final String LEGACY_ADD_TOP_LEVEL_CATEGORIES = "Add Top Level Categories";
  private static final Logger log = LogManager.getLogger(PSCategoryUnMarshaller.class);

  public PSCategory unMarshal() {
    PSCategory category = null;
    var file = createCategoryFileIfNotExisting();

    if (file == null) category = getEmptyCategory();

    if (category == null) {
      try {
        var jaxbContext = JAXBContext.newInstance(PSCategory.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        category = (PSCategory) jaxbUnmarshaller.unmarshal(file);
        removeTopLevelNode(category);
      } catch (JAXBException e) {
        throw new RuntimeException("Invalid category.xml file " + file.getPath(), e);
      }
    }
    return category;
  }

  private void removeTopLevelNode(PSCategory category) {
    var nodes = new ArrayList<PSCategoryNode>();
    for (var node : category.getTopLevelNodes()) {
      if (!StringUtils.equals(node.getTitle(), LEGACY_ADD_TOP_LEVEL_CATEGORIES)) nodes.add(node);
      else if (log.isDebugEnabled())
        log.debug("Removing old " + LEGACY_ADD_TOP_LEVEL_CATEGORIES + " category ");
    }
    category.setTopLevelNodes(nodes);
  }

  public static File createCategoryFileIfNotExisting() {
    var file = new File(PSServer.getRxDir(), "rx_resources/category/category.xml");

    if (!file.exists()) {
      var fromFile = new File(PSServer.getRxDir(), "/web_resources/categories/tree.xml");

      if (!fromFile.exists()) {
        var marshaller = new PSCategoryMarshaller();
        marshaller.setCategory(PSCategoryUnMarshaller.getEmptyCategory());
        marshaller.marshal();
      } else {
        log.info("Transforming old categories tree.xml to new category.xml");
        var transformer = new PSCategoryXmlTransform();
        transformer.transformXml(fromFile, file);
      }
    }
    return file;
  }

  public static PSCategory getEmptyCategory() {
    var category = new PSCategory();
    category.setTopLevelNodes(new ArrayList<>());
    return category;
  }

  public static PSCategory unMarshalFromString(String categoryJson) {
    if (StringUtils.isBlank(categoryJson)) {
      return null;
    }
    try (Reader reader = new StringReader(categoryJson)) {
      var mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());
      mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
      AnnotationIntrospector introspector =
          new XmlJaxbAnnotationIntrospector(mapper.getTypeFactory());
      mapper.getDeserializationConfig().withAppendedAnnotationIntrospector(introspector);
      return mapper.readValue(categoryJson, PSCategory.class);
    } catch (IOException e) {
      log.error("Error parsing category JSON: " + categoryJson, e);
      throw new RuntimeException("Unexpected error processing categories", e);
    }
  }
}
