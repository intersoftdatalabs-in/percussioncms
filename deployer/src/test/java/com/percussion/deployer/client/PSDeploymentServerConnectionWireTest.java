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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;
import com.percussion.deployer.objectstore.PSDbmsInfo;
import com.percussion.error.PSDeployException;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Wire-level tests for {@link PSDeploymentServerConnection} after the vendored {@code
 * com.percussion.HTTPClient} migration (#6056) to {@code java.net.http}.
 *
 * <p>A loopback {@link HttpServer} plays the deployment handler: it records the request line,
 * headers, and multipart body of every call and answers connect / echo / upload / download /
 * error probes. The tests pin the request/response behavior the old transport provided —
 * preemptive Basic auth, per-connection cookies, explicit {@code application/xml} multipart parts,
 * session-id round-trip, upload progress callbacks, and response streaming.
 */
@DisplayName("PSDeploymentServerConnection java.net.http wire tests (#6056)")
public class PSDeploymentServerConnectionWireTest {

  /** Placeholder credentials for the loopback server (no real secrets in fixtures). */
  private static final String USER = "wire-user";

  private static final String PASSWORD = "wire-password";
  private static final String SESSION_ID = "wire-session-1";

  private HttpServer server;
  private WireHandler handler;
  private PSDeploymentServerConnection conn;

  /** One recorded request: method, path, headers, and raw body. */
  private static final class RecordedRequest {
    String method;
    String path;
    String requestType;
    String authorization;
    String contentType;
    String cookie;
    String body;
  }

  private static final class WireHandler implements com.sun.net.httpserver.HttpHandler {
    final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();
    volatile byte[] downloadBytes = new byte[0];

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      RecordedRequest recorded = new RecordedRequest();
      recorded.method = exchange.getRequestMethod();
      recorded.path = exchange.getRequestURI().getPath();
      recorded.requestType = exchange.getRequestHeaders().getFirst("PS-Request-Type");
      recorded.authorization = exchange.getRequestHeaders().getFirst("Authorization");
      recorded.contentType = exchange.getRequestHeaders().getFirst("Content-Type");
      recorded.cookie = exchange.getRequestHeaders().getFirst("Cookie");
      byte[] raw;
      try (var in = exchange.getRequestBody()) {
        raw = in.readAllBytes();
      }
      recorded.body =
          new String(raw, StandardCharsets.ISO_8859_1).replace("\r\n", "\n");
      requests.add(recorded);

      String type = recorded.requestType == null ? "" : recorded.requestType;
      byte[] response;
      int status = 200;
      switch (type) {
        case "deploy-connect" -> {
          exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=wire-1; Path=/");
          try {
            response = connectResponse().getBytes(StandardCharsets.UTF_8);
          } catch (Exception e) {
            throw new IOException("cannot build connect fixture", e);
          }
        }
        case "deploy-disconnect", "deploy-extendlock" ->
            response = "<PSXDeployOkResponse/>".getBytes(StandardCharsets.UTF_8);
        case "deploy-probe503" -> {
          status = 503;
          response = "starting".getBytes(StandardCharsets.UTF_8);
        }
        case "deploy-probe500" -> {
          status = 500;
          response = "boom".getBytes(StandardCharsets.UTF_8);
        }
        case "deploy-uploadPackage" ->
            response = "<PSXDeployUploadResponse/>".getBytes(StandardCharsets.UTF_8);
        case "deploy-exportPackage" -> {
          response = downloadBytes;
          exchange.getResponseHeaders().add("Content-Length", String.valueOf(response.length));
        }
        default -> {
          // mkd-code-review[paths.hardcoded_sep]: XML wire fixture, not a filesystem path —
          // quotes are XML attribute delimiters in the canned handler response.
          boolean hadCookie = recorded.cookie != null && recorded.cookie.contains("JSESSIONID=wire-1");
          response = String.format("<PSXDeployEchoResponse receivedSession=\"%s\" hadCookie=\"%s\"/>", extractSessionId(recorded.body), hadCookie).getBytes(StandardCharsets.UTF_8);
        }
      }
      exchange.sendResponseHeaders(status, response.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(response);
      }
    }

    private static String extractSessionId(String body) {
      Matcher m = Pattern.compile("sessionId=\"([^\"]*)\"").matcher(body);
      return m.find() ? m.group(1) : "";
    }
  }

  @BeforeEach
  public void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    handler = new WireHandler();
    server.createContext("/Rhythmyx/sys_deployerHandler", handler);
    server.start();
    conn =
        new PSDeploymentServerConnection(
            "http",
            "127.0.0.1",
            server.getAddress().getPort(),
            USER,
            PASSWORD,
            false,
            true,
            IPSDeployConstants.OperatingMode.Packager);
  }

  @AfterEach
  public void stopServer() throws Exception {
    try {
      if (conn != null && conn.isConnected()) {
        conn.disconnect();
      }
    } finally {
      server.stop(0);
    }
  }

  @Test
  @DisplayName("connect posts multipart XML with preemptive Basic auth")
  public void connect_postsMultipartXmlWithBasicAuth() {
    assertTrue(conn.isConnected());
    assertTrue(
        conn.getServerVersion().contains("8.2"),
        "server version round-trips the fixture version, was: " + conn.getServerVersion());
    assertEquals(1, handler.requests.size());
    RecordedRequest connect = handler.requests.get(0);

    assertEquals("POST", connect.method);
    assertEquals("/Rhythmyx/sys_deployerHandler", connect.path);
    assertEquals("deploy-connect", connect.requestType);
    assertEquals(
        "Basic " + Base64.getEncoder().encodeToString((USER + ":" + PASSWORD).getBytes(StandardCharsets.ISO_8859_1)),
        connect.authorization);
    assertTrue(
        connect.contentType.startsWith("multipart/form-data; boundary="), connect.contentType);
    assertTrue(
        connect.body.contains("Content-Type: application/xml"),
        "XML part must declare application/xml");
    // mkd-code-review[paths.hardcoded_sep]: XML attribute wire matcher, not a filesystem
    // path — asserts the client echoes userId in the multipart XML part.
    assertTrue(connect.body.contains("userId=\"" + USER + "\""));
    assertTrue(connect.body.contains("overrideLock=\"yes\""));
  }

  @Test
  @DisplayName("execute echoes session id and replays server cookies")
  public void execute_echoesSessionIdAndReplaysCookies() throws Exception {
    Document req = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.createRoot(req, "PSXDeployGetDeployableElementsRequest");

    // Placeholder only — not a real credential (no secrets in fixtures)
    Document resp =
        conn.execute(
            "deploy-getDeployableElements",
            req,
            Map.of("sessionProbe", "1", "empty", ""));

    Element root = resp.getDocumentElement();
    assertEquals("PSXDeployEchoResponse", root.getNodeName());
    assertEquals(SESSION_ID, root.getAttribute("receivedSession"));
    assertEquals("true", root.getAttribute("hadCookie"));

    RecordedRequest execute = handler.requests.get(handler.requests.size() - 1);
    assertEquals("deploy-getDeployableElements", execute.requestType);
    assertTrue(execute.body.contains("sessionProbe"));
    // mkd-code-review[paths.hardcoded_sep]: XML attribute wire matcher, not a filesystem
    // path — asserts the session id round-trips in the multipart XML part.
    assertTrue(execute.body.contains("sessionId=\"" + SESSION_ID + "\""));
    assertTrue(execute.body.contains("Content-Type: application/xml"));
  }

  @Test
  @DisplayName("disconnect posts disconnect and marks the connection unusable")
  public void disconnect_postsDisconnectRequest() throws Exception {
    conn.disconnect();

    assertFalse(conn.isConnected());
    RecordedRequest last = handler.requests.get(handler.requests.size() - 1);
    assertEquals("deploy-disconnect", last.requestType);
    // mkd-code-review[paths.hardcoded_sep]: XML attribute wire matcher, not a filesystem
    // path — asserts the disconnect request carries userId in the multipart XML part.
    assertTrue(last.body.contains("userId=\"" + USER + "\""));

    Document req = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.createRoot(req, "PSXDeployGetDeployableElementsRequest");
    assertThrows(
        PSDeployException.class, () -> conn.execute("deploy-getDeployableElements", req));
  }

  @Test
  @DisplayName("execute with params and file body uploads bytes with progress callbacks")
  public void execute_uploadsFileBodyWithProgress() throws Exception {
    Path tmp = Files.createTempFile("wire_upload_", ".ppkg");
    byte[] fileBytes = "wire-package-bytes".getBytes(StandardCharsets.UTF_8);
    Files.write(tmp, fileBytes);
    try {
      PSDeployFileJobControl controller = mock(PSDeployFileJobControl.class);
      when(controller.getCancelledStatus()).thenReturn(-1);

      Document resp =
          conn.execute(
              "deploy-uploadPackage", Map.of("archive", "wire"), tmp.toFile(), controller);

      assertEquals("PSXDeployUploadResponse", resp.getDocumentElement().getNodeName());

      RecordedRequest upload = handler.requests.get(handler.requests.size() - 1);
      assertEquals("deploy-uploadPackage", upload.requestType);
      assertTrue(upload.contentType.startsWith("multipart/form-data; boundary="));
      // mkd-code-review[paths.hardcoded_sep]: multipart wire matcher, not a filesystem
      // path — asserts the upload part carries the temp file's basename.
      assertTrue(upload.body.contains("filename=\"" + tmp.getFileName() + "\""));
      assertTrue(upload.body.contains("wire-package-bytes"));
      assertTrue(upload.body.contains("archive"));

      ArgumentCaptor<IPSStreamCounter> counterCaptor =
          ArgumentCaptor.forClass(IPSStreamCounter.class);
      verify(controller).setStream(counterCaptor.capture(), anyInt());
      assertTrue(
          counterCaptor.getValue().getByteCount() > 0,
          "upload must flow through the progress counter");
    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  @Test
  @DisplayName("cancelled upload fails the request instead of sending")
  public void execute_cancelledUploadFails() throws Exception {
    Path tmp = Files.createTempFile("wire_cancel_", ".ppkg");
    Files.write(tmp, "cancel-me".getBytes(StandardCharsets.UTF_8));
    try {
      PSDeployFileJobControl controller = mock(PSDeployFileJobControl.class);
      when(controller.getCancelledStatus()).thenReturn(1);

      assertThrows(
          PSDeployException.class,
          () -> conn.execute("deploy-uploadPackage", null, tmp.toFile(), controller));
    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  @Test
  @DisplayName("execute to output stream downloads exact response bytes")
  public void execute_downloadsResponseBytesToStream() throws Exception {
    byte[] payload = "wire-download-bytes".getBytes(StandardCharsets.UTF_8);
    handler.downloadBytes = payload;

    Document req = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.createRoot(req, "PSXDeployValidateArchiveRequest");

    PSDeployFileJobControl controller = mock(PSDeployFileJobControl.class);
    when(controller.getCancelledStatus()).thenReturn(-1);
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    conn.execute("deploy-exportPackage", req, out, controller);

    assertArrayEquals(payload, out.toByteArray());
    ArgumentCaptor<IPSStreamCounter> counterCaptor =
        ArgumentCaptor.forClass(IPSStreamCounter.class);
    ArgumentCaptor<Integer> lengthCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(controller).setStream(counterCaptor.capture(), lengthCaptor.capture());
    assertEquals(payload.length, lengthCaptor.getValue());
    assertEquals((long) payload.length, counterCaptor.getValue().getByteCount());
  }

  @Test
  @DisplayName("service unavailable maps to SERVER_NOT_AVAILABLE")
  public void execute_serviceUnavailableMaps() throws Exception {
    Document req = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.createRoot(req, "PSXDeployValidateArchiveRequest");

    PSDeployException ex =
        assertThrows(PSDeployException.class, () -> conn.execute("deploy-probe503", req));
    assertEquals(DeploymentErrorCodes.SERVER_NOT_AVAILABLE.numericCode(), ex.getErrorCode());
  }

  @Test
  @DisplayName("non-XML error body maps to SERVER_ERROR_RESPONSE")
  public void execute_nonXmlErrorMaps() throws Exception {
    Document req = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.createRoot(req, "PSXDeployValidateArchiveRequest");

    PSDeployException ex =
        assertThrows(PSDeployException.class, () -> conn.execute("deploy-probe500", req));
    assertEquals(DeploymentErrorCodes.SERVER_ERROR_RESPONSE.numericCode(), ex.getErrorCode());
  }

  private static String connectResponse() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(doc, "PSXDeployConnectResponse");
    root.setAttribute("deployVersion", "8");
    root.setAttribute("licensed", "yes");
    root.setAttribute("sessionId", SESSION_ID);

    Element version = doc.createElement("PSXFormatVersion");
    version.setAttribute("buildNumber", "8.2.0 test");
    version.setAttribute("buildId", "1");
    version.setAttribute("majorVersion", "8");
    version.setAttribute("minorVersion", "2");
    version.setAttribute("microVersion", "0");
    version.setAttribute("versionString", "8.2");
    root.appendChild(version);

    // Placeholder only — not a real credential (no secrets in fixtures)
    PSDbmsInfo dbms =
        new PSDbmsInfo("derby", "wire-server", "wire-db", "wire-origin", USER, "test-password",
            false);
    root.appendChild(dbms.toXml(doc));
    return PSXmlDocumentBuilder.toString(doc);
  }
}
