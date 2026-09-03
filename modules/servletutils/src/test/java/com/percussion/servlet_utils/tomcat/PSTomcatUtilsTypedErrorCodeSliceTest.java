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
package com.percussion.servlet_utils.tomcat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.utils.container.IPSConnector;
import com.percussion.utils.tomcat.PSTomcatConnector;
import com.percussion.utils.xml.IPSXmlErrors;
import com.percussion.utils.xml.PSInvalidXmlException;
import com.percussion.utils.xml.XmlErrorCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Issue #4195 (parent #2616): leftover {@code PSTomcatUtils} production sites throw typed {@link
 * XmlErrorCode} peers. Catalog codes are non-auditable (dual-write skip). {@link IPSXmlErrors}
 * remains the numeric bridge.
 */
@Tag("UnitTest")
class PSTomcatUtilsTypedErrorCodeSliceTest {

  @TempDir Path tempDir;

  @Test
  void xmlErrorCodePeersMatchLegacyIntsAndSkipDualWrite() {
    assertEquals(IPSXmlErrors.XML_ELEMENT_MISSING, XmlErrorCode.XML_ELEMENT_MISSING.numericCode());
    assertEquals(
        IPSXmlErrors.XML_ELEMENT_INVALID_ATTR, XmlErrorCode.XML_ELEMENT_INVALID_ATTR.numericCode());
    assertFalse(XmlErrorCode.XML_ELEMENT_MISSING.isAuditable());
    assertFalse(XmlErrorCode.XML_ELEMENT_INVALID_ATTR.isAuditable());
  }

  @Test
  void loadHttpConnectorsMissingServerUsesTypedCode() throws Exception {
    Path serverFile = writeXml("<NotServer/>");
    PSInvalidXmlException ex =
        assertThrows(
            PSInvalidXmlException.class,
            () -> PSTomcatUtils.loadHttpConnectors(serverFile.toFile()));
    assertSame(XmlErrorCode.XML_ELEMENT_MISSING, ex.getTypedErrorCode());
    assertEquals(XmlErrorCode.XML_ELEMENT_MISSING.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void loadHttpConnectorsMissingServiceUsesTypedCode() throws Exception {
    Path serverFile = writeXml("<Server/>");
    PSInvalidXmlException ex =
        assertThrows(
            PSInvalidXmlException.class,
            () -> PSTomcatUtils.loadHttpConnectors(serverFile.toFile()));
    assertSame(XmlErrorCode.XML_ELEMENT_MISSING, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void saveHttpConnectorsMissingServerUsesTypedCode() throws Exception {
    Path serverFile = writeXml("<NotServer/>");
    List<IPSConnector> connectors =
        List.of(PSTomcatConnector.getBuilder().setPort(8080).build());
    PSInvalidXmlException ex =
        assertThrows(
            PSInvalidXmlException.class,
            () -> PSTomcatUtils.saveHttpConnectors(serverFile.toFile(), connectors));
    assertSame(XmlErrorCode.XML_ELEMENT_MISSING, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void saveHttpConnectorsMissingServiceUsesTypedCode() throws Exception {
    Path serverFile = writeXml("<Server/>");
    List<IPSConnector> connectors =
        List.of(PSTomcatConnector.getBuilder().setPort(8080).build());
    PSInvalidXmlException ex =
        assertThrows(
            PSInvalidXmlException.class,
            () -> PSTomcatUtils.saveHttpConnectors(serverFile.toFile(), connectors));
    assertSame(XmlErrorCode.XML_ELEMENT_MISSING, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void getAjpConnectorPortInvalidAttrUsesTypedCode() throws Exception {
    Path serverFile =
        writeXml(
            "<Server><Service>"
                + "<Connector protocol=\"AJP/1.3\" port=\"not-a-port\"/>"
                + "</Service></Server>");
    PSInvalidXmlException ex =
        assertThrows(
            PSInvalidXmlException.class,
            () -> PSTomcatUtils.getAJPConnectorPort(serverFile.toFile()));
    assertSame(XmlErrorCode.XML_ELEMENT_INVALID_ATTR, ex.getTypedErrorCode());
    assertEquals(XmlErrorCode.XML_ELEMENT_INVALID_ATTR.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void setAjpConnectorPortMissingConnectorUsesTypedCode() throws Exception {
    Path serverFile =
        writeXml(
            "<Server><Service>"
                + "<Connector protocol=\"HTTP/1.1\" port=\"8080\"/>"
                + "</Service></Server>");
    PSInvalidXmlException ex =
        assertThrows(
            PSInvalidXmlException.class,
            () -> PSTomcatUtils.setAJPConnectorPort(serverFile.toFile(), 8009));
    assertSame(XmlErrorCode.XML_ELEMENT_MISSING, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  private Path writeXml(String xml) throws Exception {
    Path file = tempDir.resolve("server.xml");
    Files.writeString(file, xml);
    return file;
  }
}
