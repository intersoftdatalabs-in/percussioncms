/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSDtdGenerator;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Behavioral tests for extension-manager / DTD residual generics after #2944
 * (parent #2877 residual of #2935).
 */
@Tag("UnitTest")
@DisplayName("extension manager / DTD merge residual generics")
class PSExtensionManagerDtdResidualTypedTest {

  @Test
  @DisplayName("IPSExtensionManager public iterator methods are generic")
  void extensionManagerIteratorSignatures() throws Exception {
    Method handlers = IPSExtensionManager.class.getMethod("getExtensionHandlerNames");
    assertEquals(Iterator.class, handlers.getReturnType());
    assertTrue(handlers.getGenericReturnType().getTypeName().contains("PSExtensionRef"));

    Method names =
        IPSExtensionManager.class.getMethod(
            "getExtensionNames", String.class, String.class, String.class, String.class);
    assertTrue(names.getGenericReturnType().getTypeName().contains("PSExtensionRef"));

    Method files = IPSExtensionManager.class.getMethod("getExtensionFiles", PSExtensionRef.class);
    assertTrue(files.getGenericReturnType().getTypeName().contains("URL"));

    Method install =
        IPSExtensionManager.class.getMethod(
            "installExtension", IPSExtensionDef.class, Iterator.class);
    assertTrue(install.getGenericParameterTypes()[1].getTypeName().contains("?"));
  }

  @Test
  @DisplayName("IPSExtensionHandler public iterator methods are generic")
  void extensionHandlerIteratorSignatures() throws Exception {
    Method names = IPSExtensionHandler.class.getMethod("getExtensionNames");
    assertTrue(names.getGenericReturnType().getTypeName().contains("PSExtensionRef"));

    Method resources =
        IPSExtensionHandler.class.getMethod("getResources", IPSExtensionDef.class);
    assertTrue(resources.getGenericReturnType().getTypeName().contains("URL"));
  }

  @Test
  @DisplayName("PSExtensionRefNameIterator yields typed extension names")
  void refNameIteratorTyped() {
    PSExtensionRef a = new PSExtensionRef("Java", "global/percussion/", "a");
    PSExtensionRef b = new PSExtensionRef("Java", "global/percussion/", "b");
    Iterator<String> names =
        new PSExtensionManager.PSExtensionRefNameIterator(List.of(a, b).iterator());
    assertTrue(names.hasNext());
    assertEquals("a", names.next());
    assertEquals("b", names.next());
    assertFalse(names.hasNext());
  }

  @Test
  @DisplayName("PSDtdGenerator produces ELEMENT from typed maps")
  void dtdGeneratorTypedMaps() throws Exception {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Root attr=\"x\"><Child>text</Child></Root>";
    Document doc =
        PSXmlDocumentBuilder.createXmlDocument(
            new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), false);
    PSDtdGenerator gen = new PSDtdGenerator();
    gen.generateDtd(doc);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    gen.writeDtd(out, "UTF-8");
    String dtd = out.toString(StandardCharsets.UTF_8);
    assertNotNull(dtd);
    assertTrue(dtd.contains("<!ELEMENT Root"), dtd);
    assertTrue(dtd.contains("Child"), dtd);
    assertTrue(dtd.contains("attr"), dtd);
  }
}
