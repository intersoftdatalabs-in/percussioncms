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
package com.percussion.xml;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.junit.jupiter.api.Test;

/**
 * Ensures the product SAX factory (JVM default via system property) never hands Digester a null
 * underlying factory / parser.
 */
class PSSaxParserFactoryImplTest {

  @Test
  void createUnderlyingFactoryNeverReturnsNull() {
    SAXParserFactory factory = PSSaxParserFactoryImpl.createUnderlyingFactory();
    assertNotNull(factory, "underlying factory must never be null (Digester caches poison)");
  }

  @Test
  void newSAXParserSucceeds() throws Exception {
    PSSaxParserFactoryImpl impl = new PSSaxParserFactoryImpl();
    SAXParser parser = impl.newSAXParser();
    assertNotNull(parser);
    assertNotNull(parser.getXMLReader());
  }

  @Test
  void digesterStyleFactoryLifecycleDoesNotNullParser() throws Exception {
    // Mirrors Commons Digester3 Digester.getFactory/getParser sequence
    SAXParserFactory factory = new PSSaxParserFactoryImpl();
    factory.setNamespaceAware(true);
    factory.setXIncludeAware(false);
    factory.setValidating(false);
    SAXParser parser = factory.newSAXParser();
    assertNotNull(parser);
    assertNotNull(parser.getXMLReader());
  }

  @Test
  void createFallbackFactoryNeverReturnsNull() {
    SAXParserFactory factory = PSSaxParserFactoryImpl.createFallbackFactory();
    assertNotNull(factory);
    assertTrue(factory.isNamespaceAware() || !factory.isNamespaceAware());
  }
}
