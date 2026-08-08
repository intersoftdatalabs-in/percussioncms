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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.HTTPClient.NVPair;
import com.percussion.deployer.catalog.server.PSCatalogHandler;
import com.percussion.deployer.server.PSDeploymentHandler;
import com.percussion.error.IPSDeploymentErrors;
import com.percussion.error.PSDeployException;
import com.percussion.server.PSRequest;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Agent-safe residual smoke for Package Installer {@code NULL_INPUT_DOC} after the Slice 2
 * multipart Content-Type fix (#2271 / #2265).
 *
 * <p>Parent epic: #955. This slice (#2266) does <strong>not</strong> run a live CMS Package
 * Installer install; it locks the encode → MIME-gate contract for install-critical request types
 * and verifies handlers do not report error 8 when an input document is present.
 *
 * <p>Live residual steps (when a CMS + sample {@code .ppkg} are available) are documented on #2266
 * / #955 and in {@code docs/ai-generated/tasks/955-package-installer-null-input-doc/2266-residual-smoke.md}.
 */
@DisplayName("Package Installer NULL_INPUT_DOC residual smoke (#2266)")
public class PSPackageInstallerNullInputDocResidualSmokeTest {

  /**
   * Install-critical deploy request roots that Package Installer exercises over HTTP multipart
   * before / during package install (connect, catalog, validate).
   */
  static Stream<Arguments> installCriticalRequestDocs() {
    return Stream.of(
        Arguments.of("deploy-connect", "PSXDeployConnectRequest"),
        Arguments.of("deploy-validateArchive", "PSXDeployValidateArchiveRequest"),
        Arguments.of("deploy-getDeployableElements", "PSXDeployGetDeployableElementsRequest"),
        Arguments.of("catalog-types", "PSXDeployCatalogTypesRequest"));
  }

  @ParameterizedTest(name = "{0} multipart forces application/xml")
  @MethodSource("installCriticalRequestDocs")
  public void residualSmoke_installRequestTypes_encodeWithApplicationXml(
      String requestType, String rootElement) throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(doc, rootElement);
    // Placeholder only — not a real credential (no secrets in fixtures)
    if ("PSXDeployConnectRequest".equals(rootElement)) {
      root.setAttribute("userId", "smoke-user");
      root.setAttribute("password", "smoke-password");
      root.setAttribute("overrideLock", "no");
      root.setAttribute("enforceLicense", "no");
    } else if ("PSXDeployValidateArchiveRequest".equals(rootElement)) {
      root.setAttribute("checkArchiveRef", "yes");
    } else if ("PSXDeployGetDeployableElementsRequest".equals(rootElement)) {
      root.setAttribute("type", "Package");
    }

    NVPair[] opts = new NVPair[] {new NVPair("sessionProbe", "1")};
    NVPair[] hdrs = new NVPair[2];
    hdrs[1] = new NVPair("PS-Request-Type", requestType);

    byte[] body = PSDeploymentServerConnection.encodeXmlDocumentMultipart(opts, doc, hdrs);

    assertTrue(
        PSDeploymentServerConnection.multipartContainsXmlContentType(body),
        () -> "missing XML Content-Type for " + requestType);
    String asText = new String(body, StandardCharsets.ISO_8859_1);
    assertTrue(
        asText.toLowerCase(Locale.ROOT).contains("content-type: application/xml"),
        () -> "expected application/xml for " + requestType + ", headers:\n" + extractHeaders(asText));
    assertTrue(asText.contains(rootElement), () -> "XML body must include " + rootElement);
    assertEquals("Content-Type", hdrs[0].getName());
    assertTrue(
        hdrs[0].getValue().toLowerCase(Locale.ROOT).startsWith("multipart/form-data"),
        hdrs[0].getValue());
  }

  @Test
  @DisplayName("production File encode path (execute) forces application/xml")
  public void residualSmoke_fileBasedEncodeMatchesProductionExecutePath() throws Exception {
    // Production execute() writes the request Document to a temp .xml file then encodes it.
    java.nio.file.Path tmp = Files.createTempFile("dpl_smoke_", ".xml");
    try {
      String xml =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
              + "<PSXDeployValidateArchiveRequest checkArchiveRef=\"yes\"/>";
      Files.writeString(tmp, xml, StandardCharsets.UTF_8);

      NVPair[] opts = null;
      NVPair[] hdrs = new NVPair[2];
      hdrs[1] = new NVPair("PS-Request-Type", "deploy-validateArchive");

      byte[] body =
          PSDeploymentServerConnection.encodeXmlDocumentMultipart(opts, tmp.toFile(), hdrs);

      assertTrue(PSDeploymentServerConnection.multipartContainsXmlContentType(body));
      String asText = new String(body, StandardCharsets.ISO_8859_1);
      assertTrue(asText.toLowerCase(Locale.ROOT).contains("content-type: application/xml"));
      assertTrue(asText.contains("PSXDeployValidateArchiveRequest"));
    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  @Test
  @DisplayName("server MIME gate: application/xml and text/xml are XML; other types are not")
  public void residualSmoke_serverMimeGateMatchesFormParserXmlClassification() {
    // Mirrors PSFormContentParser: only text/xml and application/xml set isXml → setInputDocument.
    assertTrue(isXmlMimeType("application/xml"));
    assertTrue(isXmlMimeType("application/xml; charset=utf-8"));
    assertTrue(isXmlMimeType("text/xml"));
    assertTrue(isXmlMimeType("text/xml; charset=utf-8"));
    assertFalse(isXmlMimeType("application/octet-stream"));
    assertFalse(isXmlMimeType("text/plain"));
    assertFalse(isXmlMimeType(""));
    assertFalse(isXmlMimeType(null));
  }

  @Test
  @DisplayName("handlers skip NULL_INPUT_DOC when input document is present")
  public void residualSmoke_handlersDoNotThrowNullInputDocWhenDocumentPresent() throws Exception {
    // Catalog: intentional non-catalog root → INVALID_REQUEST_TYPE, not error 8.
    Document catalogDoc = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.createRoot(catalogDoc, "PSXDeployConnectRequest");
    PSRequest catalogReq = mock(PSRequest.class);
    when(catalogReq.getInputDocument()).thenReturn(catalogDoc);

    PSDeployException catalogEx =
        assertThrows(PSDeployException.class, () -> PSCatalogHandler.processRequest(catalogReq));
    assertNotEquals(
        IPSDeploymentErrors.NULL_INPUT_DOC,
        catalogEx.getErrorCode(),
        "present document must not surface as NULL_INPUT_DOC");
    assertEquals(IPSDeploymentErrors.INVALID_REQUEST_TYPE, catalogEx.getErrorCode());

    // validateArchive: null-doc guard must not fire when document is present.
    // Missing archive child fails later — residual gate is only error 8.
    Document valDoc = PSXmlDocumentBuilder.createXmlDocument();
    Element valRoot =
        PSXmlDocumentBuilder.createRoot(valDoc, "PSXDeployValidateArchiveRequest");
    valRoot.setAttribute("checkArchiveRef", "yes");
    valRoot.setAttribute("warnOnBuidMismatch", "no");
    valRoot.setAttribute("warnMissingPackageDep", "no");
    PSRequest valReq = mock(PSRequest.class);
    when(valReq.getInputDocument()).thenReturn(valDoc);

    PSDeploymentHandler handler = new PSDeploymentHandler();
    PSDeployException valEx =
        assertThrows(PSDeployException.class, () -> handler.validateArchive(valReq));
    assertNotEquals(
        IPSDeploymentErrors.NULL_INPUT_DOC,
        valEx.getErrorCode(),
        "present document must not surface as NULL_INPUT_DOC from validateArchive: "
            + valEx.getLocalizedMessage());
  }

  @Test
  @DisplayName("null document still maps to NULL_INPUT_DOC (guards retained)")
  public void residualSmoke_nullDocumentStillMapsToError8() {
    PSRequest req = mock(PSRequest.class);
    when(req.getInputDocument()).thenReturn(null);

    PSDeployException ex =
        assertThrows(PSDeployException.class, () -> PSCatalogHandler.processRequest(req));
    assertEquals(IPSDeploymentErrors.NULL_INPUT_DOC, ex.getErrorCode());
  }

  /**
   * Same classification rule as {@code PSFormContentParser} for multipart parts (startsWith
   * text/xml or application/xml). Kept local so residual smoke does not require spinning the full
   * request parser.
   */
  private static boolean isXmlMimeType(String mimeType) {
    if (mimeType == null || mimeType.isEmpty()) {
      return false;
    }
    String lower = mimeType.toLowerCase(Locale.ROOT).trim();
    return lower.startsWith("text/xml") || lower.startsWith("application/xml");
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
