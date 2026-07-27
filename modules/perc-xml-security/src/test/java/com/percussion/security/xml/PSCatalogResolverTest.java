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

package com.percussion.security.xml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.xml.resolver.CatalogManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression: OASIS XML catalogs must load under secured SAX without TR9401 fallback AIOOBE, and
 * {@link PSCatalogResolver} must construct without failing startup.
 */
class PSCatalogResolverTest {

  @TempDir Path tempDir;

  @Test
  void defaultConstructorDoesNotThrow() {
    assertDoesNotThrow(
        () -> {
          new PSCatalogResolver();
        });
  }

  @Test
  void createDefaultCatalogManagerIsConfigured() {
    CatalogManager manager = PSCatalogResolver.createDefaultCatalogManager();
    assertNotNull(manager);
    assertTrue(manager.getIgnoreMissingProperties());
    // Private (non-static) catalog by default — avoids process-wide shared catalog corruption
    assertFalse(manager.getUseStaticCatalog());
    assertFalse(manager.getPreferPublic());
  }

  @Test
  void privateCatalogFlagControlsStaticCatalogSetting() {
    assertFalse(PSCatalogResolver.createCatalogManager(true).getUseStaticCatalog());
    assertTrue(PSCatalogResolver.createCatalogManager(false).getUseStaticCatalog());
  }

  @Test
  void oasisCatalogWithoutDoctypeLoadsWithoutAioobe() throws Exception {
    // Minimal OASIS catalog (no DOCTYPE) — same shape as shipping PercussionXMLCatalog.xml
    String catalogXml =
        """
        <?xml version="1.0"?>
        <catalog xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog" prefer="system">
          <public publicId="-//Test//DTD Example//EN" uri="example.dtd"/>
        </catalog>
        """;
    Path catalogFile = tempDir.resolve("test-catalog.xml");
    Files.writeString(catalogFile, catalogXml, StandardCharsets.UTF_8);

    CatalogManager manager = PSCatalogResolver.createDefaultCatalogManager();
    // file URI form that CatalogManager accepts on all platforms
    String catalogUri = catalogFile.toUri().toString();
    manager.setCatalogFiles(catalogUri);

    PSCatalogResolver resolver = new PSCatalogResolver(manager);
    assertNotNull(resolver);
    assertNotNull(resolver.getCatalog());
  }

  @Test
  void packagedCatalogsDoNotDeclareDoctype() throws Exception {
    for (String resource : new String[] {"PercussionXMLCatalog.xml", "CustomXMLCatalog.xml"}) {
      URL url = PSCatalogResolver.class.getClassLoader().getResource(resource);
      assertNotNull(url, "classpath resource missing: " + resource);
      try (InputStream in = url.openStream()) {
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        // Normalize for assertion; packaged catalogs must not force external catalog.dtd
        assertFalse(
            text.toUpperCase().contains("<!DOCTYPE"),
            resource + " must not declare DOCTYPE (breaks OASIS parse under secured SAX)");
      }
    }
  }

  @Test
  void apacheFeatureUrisAreRealFeatureIdsNotDocPages() {
    assertTrue(
        PSSecureXMLUtils.X1_GENERAL_EXTERNAL_ENTITIES_FEATURE.startsWith(
            "http://apache.org/xml/features/"),
        "external-general-entities must use Apache feature URI");
    assertTrue(
        PSSecureXMLUtils.X1_EXTERNAL_PARAMETER_ENTITIES_FEATURE.startsWith(
            "http://apache.org/xml/features/"),
        "external-parameter-entities must use Apache feature URI");
    assertFalse(
        PSSecureXMLUtils.X1_GENERAL_EXTERNAL_ENTITIES_FEATURE.contains("xerces.apache.org"),
        "must not use documentation-page URLs as feature IDs");
  }
}
