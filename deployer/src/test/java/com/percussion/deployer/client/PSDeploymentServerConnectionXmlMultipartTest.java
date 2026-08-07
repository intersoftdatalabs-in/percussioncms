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
package com.percussion.deployer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.HTTPClient.Codecs;
import com.percussion.HTTPClient.NVPair;
import com.percussion.HTTPClient.PSBinaryFileData;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Unit tests for Package Installer / deployer client multipart encoding of XML request documents.
 *
 * <p>Regression coverage for #955 / #2265: without an explicit XML Content-Type on the file part,
 * {@code PSFormContentParser} may leave {@code getInputDocument()} null and handlers throw {@code
 * IPSDeploymentErrors.NULL_INPUT_DOC}.
 */
public class PSDeploymentServerConnectionXmlMultipartTest {

  @Test
  public void createXmlRequestFileDataForcesApplicationXmlContentType() {
    byte[] xml = "<root/>".getBytes(StandardCharsets.UTF_8);
    PSBinaryFileData[] parts =
        PSDeploymentServerConnection.createXmlRequestFileData("dpl_abc123.xml", xml);
    assertEquals(1, parts.length);
    assertEquals(PSDeploymentServerConnection.XML_REQUEST_CONTENT_TYPE, parts[0].getContentType());
    assertEquals("dpl_abc123.xml", parts[0].getFileName());
    assertEquals("dpl_abc123.xml", parts[0].getFieldName());
  }

  @Test
  public void createXmlRequestFileDataStripsPathAndEnsuresXmlSuffix() {
    byte[] xml = "<root/>".getBytes(StandardCharsets.UTF_8);
    // Windows-style path (backslashes)
    PSBinaryFileData[] winParts =
        PSDeploymentServerConnection.createXmlRequestFileData("C:\\temp\\dpl_req", xml);
    assertEquals(1, winParts.length);
    String winName = winParts[0].getFileName();
    assertFalse(winName.contains("\\") || winName.contains("/"), "basename only: " + winName);
    assertTrue(winName.toLowerCase(Locale.ROOT).endsWith(".xml"), winName);
    assertEquals(
        PSDeploymentServerConnection.XML_REQUEST_CONTENT_TYPE, winParts[0].getContentType());

    // Unix-style path (forward slashes) — same stripping on both separator families
    PSBinaryFileData[] unixParts =
        PSDeploymentServerConnection.createXmlRequestFileData("/tmp/dpl_req", xml);
    assertEquals(1, unixParts.length);
    String unixName = unixParts[0].getFileName();
    assertFalse(unixName.contains("\\") || unixName.contains("/"), "basename only: " + unixName);
    assertTrue(unixName.toLowerCase(Locale.ROOT).endsWith(".xml"), unixName);
    assertEquals("dpl_req.xml", unixName);
    assertEquals(
        PSDeploymentServerConnection.XML_REQUEST_CONTENT_TYPE, unixParts[0].getContentType());
  }

  @Test
  public void createXmlRequestFileDataRejectsNullBytes() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSDeploymentServerConnection.createXmlRequestFileData("x.xml", null));
  }

  @Test
  public void encodeXmlDocumentMultipartIncludesXmlContentTypeHeader() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(doc, "PSXDeployConnectRequest");
    root.setAttribute("userId", "admin");
    // Placeholder only — not a real credential (REVIEW.md: no secrets in fixtures)
    root.setAttribute("password", "test-password");

    NVPair[] opts = new NVPair[] {new NVPair("sessionProbe", "1")};
    NVPair[] hdrs = new NVPair[2];
    hdrs[1] = new NVPair("PS-Request-Type", "deploy-connect");

    byte[] body = PSDeploymentServerConnection.encodeXmlDocumentMultipart(opts, doc, hdrs);

    assertTrue(
        PSDeploymentServerConnection.multipartContainsXmlContentType(body),
        "multipart body must declare application/xml (or text/xml) on the XML part");
    String asText = new String(body, StandardCharsets.ISO_8859_1);
    assertTrue(
        asText.toLowerCase(Locale.ROOT).contains("content-type: application/xml"),
        "expected explicit application/xml, body headers were:\n" + extractHeaders(asText));
    assertTrue(asText.contains("PSXDeployConnectRequest"), "XML body must be present");
    assertTrue(asText.contains("sessionProbe"), "form fields must be present");
    assertEquals("Content-Type", hdrs[0].getName());
    assertTrue(
        hdrs[0].getValue().toLowerCase(Locale.ROOT).startsWith("multipart/form-data"),
        hdrs[0].getValue());
  }

  @Test
  public void encodeWithExplicitContentTypeWinsOverFilenameGuess() throws Exception {
    // Even if the filename map is empty / wrong, explicit content type on PSBinaryFileData
    // is what Codecs writes into the part headers.
    byte[] xml = "<PSXDeployValidateArchiveRequest/>".getBytes(StandardCharsets.UTF_8);
    PSBinaryFileData[] files =
        new PSBinaryFileData[] {
          new PSBinaryFileData(
              xml,
              "req",
              "dpl_no_map.bin", // non-.xml name would not guess as XML
              PSDeploymentServerConnection.XML_REQUEST_CONTENT_TYPE)
        };
    NVPair[] hdrs = new NVPair[1];
    byte[] body = Codecs.mpFormDataEncode(null, files, hdrs);
    assertTrue(PSDeploymentServerConnection.multipartContainsXmlContentType(body));
  }

  private static String extractHeaders(String multipartBody) {
    StringBuilder sb = new StringBuilder();
    for (String line : multipartBody.split("\r\n")) {
      if (line.toLowerCase(Locale.ROOT).startsWith("content-")) {
        sb.append(line).append('\n');
      }
    }
    return sb.toString();
  }
}
