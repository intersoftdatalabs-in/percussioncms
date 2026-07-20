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
package test.percussion.pso.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.IPSReplacementValue;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.pso.legacy.PSOProxyQueryResource;
import com.percussion.server.IPSRequestContext;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.w3c.dom.Document;

/**
 * Unit tests for {@link PSOProxyQueryResource}.
 *
 * <p>Uses a local {@link HttpServer} so tests do not depend on external network resources (the
 * previous Google News Atom feed is no longer a reliable fixture).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PSOProxyQueryResourceTest {

  private static final String SAMPLE_ATOM_FEED =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<feed xmlns=\"http://www.w3.org/2005/Atom\">\n"
          + "  <title>Test Feed</title>\n"
          + "  <id>urn:test:feed</id>\n"
          + "  <entry>\n"
          + "    <title>Sample Entry</title>\n"
          + "    <id>urn:test:entry:1</id>\n"
          + "  </entry>\n"
          + "</feed>\n";

  private static HttpServer server;
  private static String baseUrl;

  private final PSOProxyQueryResource proxy = new PSOProxyQueryResource();

  @BeforeAll
  static void startLocalServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    // Serve a fixed Atom feed for any path so tests cover extension URL, request URL,
    // and query-string append without calling the public network.
    server.createContext(
        "/",
        exchange -> {
          byte[] body = SAMPLE_ATOM_FEED.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/atom+xml; charset=UTF-8");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    baseUrl = "http://localhost:" + server.getAddress().getPort();
  }

  @AfterAll
  static void stopLocalServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @BeforeEach
  public void setUp() throws Exception {
    proxy.init(makeExtensionDef("url"), null);
  }

  @Test
  public void shouldFailIfUrlIsNotProvided() throws Exception {
    IPSRequestContext request = makeRequest(makeRequestParams(null, null, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> proxy.processResultDocument(new Object[] {}, request, null));
  }

  @Test
  public void shouldReturnAnXmlDocumentFromTheUrlProvidedAsAnExtensionParameter() throws Exception {
    IPSRequestContext request = makeRequest(makeRequestParams(null, null, null));
    Object[] eParams = makeParams(baseUrl + "/feed?output=atom");
    Document doc = proxy.processResultDocument(eParams, request, null);
    assertNotNull(doc);
    assertEquals("feed", doc.getDocumentElement().getTagName());
  }

  @Test
  public void shouldReturnAnXmlDocumentFromTheUrlProvidedAsARequestParameter() throws Exception {
    IPSRequestContext request =
        makeRequest(makeRequestParams(baseUrl + "/feed?output=atom", null, null));
    Object[] eParams = new Object[] {};
    Document doc = proxy.processResultDocument(eParams, request, null);
    assertNotNull(doc);
    assertEquals("feed", doc.getDocumentElement().getTagName());
  }

  @Test
  public void shouldReturnAnXmlDocumentFromTheGivenUrlWithTheRequestParametersAppendedToTheUrl()
      throws Exception {
    Map<String, String> params = makeRequestParams(null, null, null);
    params.put("output", "atom");
    IPSRequestContext request = makeRequest(params);
    // Trailing slash so appended query uses '?' + output=atom against local root context
    Object[] eParams = makeParams(baseUrl + "/");
    Document doc = proxy.processResultDocument(eParams, request, null);
    assertNotNull(doc);
    assertEquals("feed", doc.getDocumentElement().getTagName());
  }

  public IPSExtensionDef makeExtensionDef(final String... names) {
    IPSExtensionDef extensionDef = mock(IPSExtensionDef.class);
    when(extensionDef.getRuntimeParameterNames())
        .thenAnswer(invocation -> java.util.Arrays.asList(names).iterator());
    return extensionDef;
  }

  public IPSRequestContext makeRequest(final Map<String, String> parameters) {
    IPSRequestContext request = mock(IPSRequestContext.class);
    for (Entry<String, String> entry : parameters.entrySet()) {
      when(request.getParameter(entry.getKey())).thenReturn(entry.getValue());
    }
    when(request.getParametersIterator())
        .thenAnswer(invocation -> parameters.entrySet().iterator());
    return request;
  }

  public Map<String, String> makeRequestParams(String url, String user, String password) {
    Map<String, String> ps = new HashMap<>();
    ps.put("url", url);
    ps.put("user", user);
    ps.put("password", password);
    return ps;
  }

  public Object[] makeParams(final String... params) throws Exception {
    Object[] rvalue = new Object[params.length];
    for (int i = 0; i < params.length; i++) {
      IPSReplacementValue irv = mock(IPSReplacementValue.class);
      when(irv.getValueText()).thenReturn(params[i]);
      rvalue[i] = irv;
    }
    return rvalue;
  }
}
