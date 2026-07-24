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

package com.percussion.deployer.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.File;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Unit tests for {@link PSDependencyFile}, especially XML round-trip of the {@code fileType}
 * attribute. Regression: modernization dropped {@code fileType} deserialization, defaulting all
 * archive entries to {@link PSDependencyFile#TYPE_APPLICATION_XML} and breaking package install.
 */
public class PSDependencyFileTest {

  /** Every type in {@link PSDependencyFile#TYPE_ENUM} must round-trip through toXml/fromXml. */
  static Stream<Integer> allFileTypes() {
    return Stream.iterate(0, i -> i + 1).limit(PSDependencyFile.TYPE_ENUM.length);
  }

  @ParameterizedTest
  @MethodSource("allFileTypes")
  public void testXmlRoundTripPreservesFileType(int fileType) throws Exception {
    File rxFile = new File("ObjectStore", "demo.application");
    File archiveLoc = new File("Application-demo", "demo.application");
    File original = new File("sys_Lookup.dtd");

    PSDependencyFile source = new PSDependencyFile(fileType, rxFile, original);
    source.setArchiveLocation(archiveLoc);

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = source.toXml(doc);
    assertEquals(
        PSDependencyFile.TYPE_ENUM[fileType],
        el.getAttribute("fileType"),
        "toXml must write fileType attribute");

    PSDependencyFile restored = new PSDependencyFile(el);
    assertEquals(fileType, restored.getType(), "fromXml must restore fileType for " + PSDependencyFile.TYPE_ENUM[fileType]);
    assertEquals(source.getFile(), restored.getFile());
    assertEquals(source.getArchiveLocation(), restored.getArchiveLocation());
    assertNotNull(restored.getOriginalFile());
    // original path is normalized to forward slashes on restore
    assertEquals(
        PSDeployComponentUtils.getNormalizedPath(original.getPath()),
        restored.getOriginalFile().getPath().replace('\\', '/'));
    assertEquals(source, restored);
  }

  @Test
  public void testXmlRoundTripWithoutOriginalFile() throws Exception {
    PSDependencyFile source =
        new PSDependencyFile(
            PSDependencyFile.TYPE_SUPPORT_FILE, new File("rx_resources/widgets/x.css"));
    source.setArchiveLocation(new File("sys__UserDependency--x", "x.css"));

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSDependencyFile restored = new PSDependencyFile(source.toXml(doc));

    assertEquals(PSDependencyFile.TYPE_SUPPORT_FILE, restored.getType());
    assertNull(restored.getOriginalFile());
    assertEquals(source, restored);
  }

  @Test
  public void testApplicationFileNotDefaultedToApplicationXml() throws Exception {
    // Simulates companion DTD entry from package manifests (e.g. percEventAsset(1).application)
    PSDependencyFile dtdCompanion =
        new PSDependencyFile(
            PSDependencyFile.TYPE_APPLICATION_FILE,
            new File("percEventAsset(1).application"),
            new File("sys_Lookup.dtd"));
    dtdCompanion.setArchiveLocation(
        new File("Application-percEventAsset", "percEventAsset(1).application"));

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSDependencyFile restored = new PSDependencyFile(dtdCompanion.toXml(doc));

    assertEquals(
        PSDependencyFile.TYPE_APPLICATION_FILE,
        restored.getType(),
        "APPLICATION_FILE must not collapse to default APPLICATION_XML (0)");
    assertTrue(restored.getType() != PSDependencyFile.TYPE_APPLICATION_XML);
  }

  @Test
  public void testFromXmlRejectsMissingFileType() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = doc.createElement(PSDependencyFile.XML_NODE_NAME);
    // deliberately omit fileType
    PSXmlDocumentBuilder.addElement(doc, el, "RxFile", "foo.application");
    PSXmlDocumentBuilder.addElement(doc, el, "ArchiveFile", "Application-foo/foo.application");

    assertThrows(PSUnknownNodeTypeException.class, () -> new PSDependencyFile(el));
  }

  @Test
  public void testFromXmlRejectsUnknownFileType() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = doc.createElement(PSDependencyFile.XML_NODE_NAME);
    el.setAttribute("fileType", "NOT_A_REAL_TYPE");
    PSXmlDocumentBuilder.addElement(doc, el, "RxFile", "foo.application");
    PSXmlDocumentBuilder.addElement(doc, el, "ArchiveFile", "Application-foo/foo.application");

    assertThrows(PSUnknownNodeTypeException.class, () -> new PSDependencyFile(el));
  }

  @Test
  public void testFromXmlRejectsEmptyArchiveFile() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = doc.createElement(PSDependencyFile.XML_NODE_NAME);
    el.setAttribute("fileType", "APPLICATION_XML");
    PSXmlDocumentBuilder.addElement(doc, el, "RxFile", "foo.application");
    PSXmlDocumentBuilder.addElement(doc, el, "ArchiveFile", "   ");

    assertThrows(PSUnknownNodeTypeException.class, () -> new PSDependencyFile(el));
  }

  @Test
  public void testConstructorRejectsInvalidType() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSDependencyFile(-1, new File("x")));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSDependencyFile(PSDependencyFile.TYPE_ENUM.length, new File("x")));
  }
}
