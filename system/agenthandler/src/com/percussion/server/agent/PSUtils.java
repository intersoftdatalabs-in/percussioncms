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

package com.percussion.server.agent;

import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Utility class providing common helper methods for agent operations.
 * This class follows the utility class pattern with all static methods.
 *
 * @since Java 11
 */
public final class PSUtils {

   /**
    * Private constructor to prevent instantiation of utility class.
    */
   private PSUtils() {
      throw new UnsupportedOperationException("Utility class cannot be instantiated");
   }

   /**
    * Gets a DocumentBuilder instance configured for XML parsing.
    *
    * @return a new DocumentBuilder instance
    * @throws ParserConfigurationException if a DocumentBuilder cannot be created
    */
   public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
      var factory =
          PSSecureXMLUtils.getSecuredDocumentBuilderFactory(PSXmlSecurityOptions.secure());
      factory.setNamespaceAware(true);
      factory.setValidating(false);
      return factory.newDocumentBuilder();
   }

   /**
    * Extracts the text content from the first child element with the specified name.
    *
    * @param parent the parent element to search within, must not be {@code null}
    * @param elementName the name of the child element to find, must not be blank
    * @return the text content of the element, or {@code null} if not found
    * @throws IllegalArgumentException if parent is {@code null} or elementName is blank
    */
   public static String getElemValue(Element parent, String elementName) {
      if (parent == null) {
         throw new IllegalArgumentException("Parent element must not be null");
      }
      if (StringUtils.isBlank(elementName)) {
         throw new IllegalArgumentException("Element name must not be blank");
      }

      var nodeList = parent.getElementsByTagName(elementName);
      if (nodeList.getLength() > 0) {
         var node = nodeList.item(0);
         return getTextContent(node);
      }
      return null;
   }

   /**
    * Safely extracts text content from a DOM node.
    *
    * @param node the node to extract text from
    * @return the text content, or {@code null} if node is {@code null}
    */
   private static String getTextContent(Node node) {
      return Optional.ofNullable(node)
         .map(Node::getTextContent)
         .map(String::trim)
         .filter(text -> !text.isEmpty())
         .orElse(null);
   }

   /**
    * Gets a resource bundle for agent internationalization.
    *
    * @return the resource bundle
    * @throws MissingResourceException if the resource bundle cannot be found
    */
   public static ResourceBundle getRes() throws MissingResourceException {
      return ResourceBundle.getBundle("com.percussion.server.agent.AgentResources");
   }

   /**
    * Validates that a string parameter is not null or blank.
    *
    * @param value the value to validate
    * @param paramName the parameter name for error messages
    * @throws IllegalArgumentException if the value is null or blank
    */
   public static void validateNotBlank(String value, String paramName) {
      if (StringUtils.isBlank(value)) {
         throw new IllegalArgumentException(paramName + " must not be null or blank");
      }
   }

   /**
    * Validates that an object parameter is not null.
    *
    * @param value the value to validate
    * @param paramName the parameter name for error messages
    * @throws IllegalArgumentException if the value is null
    */
   public static void validateNotNull(Object value, String paramName) {
      if (value == null) {
         throw new IllegalArgumentException(paramName + " must not be null");
      }
   }
}
