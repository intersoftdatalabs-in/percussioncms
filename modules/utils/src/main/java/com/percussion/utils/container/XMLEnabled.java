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

package com.percussion.utils.container;

import com.percussion.utils.xml.PSInvalidXmlException;
import java.io.IOException;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * XML-enabled interface.
 */
public interface XMLEnabled {
  /**
   * Convert to XML.
   *
   * @param properties the properties, may be <code>null</code>.
   * @return the document, may be <code>null</code>.
   * @throws IOException if an I/O error occurs.
   * @throws SAXException if a SAX error occurs.
   * @throws PSInvalidXmlException if the XML is invalid.
   */
  Document toXml(Map<String, String> properties)
      throws IOException, SAXException, PSInvalidXmlException;

  /**
   * Convert to XML.
   *
   * @return the document, may be <code>null</code>.
   * @throws IOException if an I/O error occurs.
   * @throws SAXException if a SAX error occurs.
   * @throws PSInvalidXmlException if the XML is invalid.
   */
  Document toXml() throws IOException, SAXException, PSInvalidXmlException;

  /**
   * Load from XML.
   *
   * @param el the element, may not be <code>null</code>.
   * @param properties the properties, may be <code>null</code>.
   * @throws IOException if an I/O error occurs.
   * @throws SAXException if a SAX error occurs.
   * @throws PSInvalidXmlException if the XML is invalid.
   */
  void fromXml(Element el, Map<String, String> properties)
      throws IOException, SAXException, PSInvalidXmlException;

  /**
   * Load from XML.
   *
   * @param el the element, may not be <code>null</code>.
   * @throws SAXException if a SAX error occurs.
   * @throws IOException if an I/O error occurs.
   * @throws PSInvalidXmlException if the XML is invalid.
   */
  void fromXml(Element el) throws SAXException, IOException, PSInvalidXmlException;
}