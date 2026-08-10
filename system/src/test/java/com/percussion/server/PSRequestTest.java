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

package com.percussion.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class PSRequestTest {
  // legacy constructor removed - using @Test methods

  /**
   * Constructs <code>PSRequest</code> objects for test classes outside the server package (since
   * the <code>PSRequest</code> constructors have package access).
   *
   * @return new <code>PSRequest</code> using supplied parameters
   */
  public static PSRequest makeRequest(
      String reqFileURL,
      String reqHookURL,
      Map params,
      Map<String, String> cgiVars,
      Map cookies,
      Document inData,
      OutputStream out) {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", reqFileURL);
    if (cgiVars != null) {
      for (String cgiName : cgiVars.keySet()) {
        req.addParameter(cgiName, (String) cgiVars.get(cgiName));
      }
    }
    MockHttpServletResponse res = new MockHttpServletResponse();

    // TODO - Need to handle other data. Probably need to write more
    // complete Mock objects, or look for a better version of these

    return new PSRequest(req, res, null, null);
  }

  /** Tests the cloneRequest method to make sure the request parameter map is cloned. */
  @Test
  public void testClone() throws Exception {
    // build parameter map
    HashMap params = new HashMap();
    params.put("alpha", "beta");
    params.put("foo", "bar");

    PSRequest request = getEmptyRequest();
    request.setParameters(params);
    assertEquals("bar", request.getParameter("foo"));
    assertEquals("beta", request.getParameter("alpha"));

    PSRequest clone = request.cloneRequest();

    // make sure we start from equivalence
    assertEquals(clone.getParameters(), request.getParameters());
    // PSRequest does not override equals so can't assertEquals(clone, request);

    // test modify
    clone.setParameter("foo", "foo");
    assertEquals("foo", clone.getParameter("foo"));
    assertEquals("bar", request.getParameter("foo"));
    assertTrue(!clone.getParameters().equals(request.getParameters()));

    // test add
    clone.setParameter("bar", "bar");
    assertEquals("bar", clone.getParameter("bar"));
    assertNull(request.getParameter("bar"));
    assertTrue(!clone.getParameters().equals(request.getParameters()));
  }

  /** */
  private PSRequest getEmptyRequest() {

    MockHttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();

    return new PSRequest(req, res, null, null);
  }

  @Test
  public void testJsonPageTypeFromExtension() {
    // null servlet request avoids MockHttpServletRequest cast issues in setRequestFileURL
    PSRequest request = new PSRequest(null, null, null, null);
    request.setRequestFileURL("/Rhythmyx/MyApp/products.json");

    assertEquals(PSRequest.PAGE_TYPE_JSON, request.getRequestPageType());
    assertEquals(".json", request.getRequestPageExtension().toLowerCase());
  }

  /**
   * Extensionless URL + {@code Accept: application/json} selects JSON page type (Accept
   * negotiation).
   */
  @Test
  public void testJsonPageTypeFromAcceptHeaderWhenNoExtension() {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/Rhythmyx/MyApp/products");
    req.setContextPath("");
    req.setServletPath("/Rhythmyx/MyApp/products");
    req.addHeader("Accept", "application/json");
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSRequest request = new PSRequest(req, res, null, null);

    assertEquals(PSRequest.PAGE_TYPE_JSON, request.getRequestPageType());
    assertEquals(".json", request.getRequestPageExtension().toLowerCase());
  }

  /** Structured JSON Accept types ({@code application/*+json}) also select JSON. */
  @Test
  public void testJsonPageTypeFromStructuredJsonAccept() {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/Rhythmyx/MyApp/products");
    req.setContextPath("");
    req.setServletPath("/Rhythmyx/MyApp/products");
    req.addHeader("Accept", "application/ld+json");
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSRequest request = new PSRequest(req, res, null, null);

    assertEquals(PSRequest.PAGE_TYPE_JSON, request.getRequestPageType());
  }

  /** Extensionless with no Accept remains product default XML. */
  @Test
  public void testExtensionlessWithoutAcceptRemainsXml() {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/Rhythmyx/MyApp/products");
    req.setContextPath("");
    req.setServletPath("/Rhythmyx/MyApp/products");
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSRequest request = new PSRequest(req, res, null, null);

    assertEquals(PSRequest.PAGE_TYPE_XML, request.getRequestPageType());
  }

  /** Extensionless + Accept XML remains XML (does not force JSON). */
  @Test
  public void testExtensionlessWithAcceptXmlRemainsXml() {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/Rhythmyx/MyApp/products");
    req.setContextPath("");
    req.setServletPath("/Rhythmyx/MyApp/products");
    req.addHeader("Accept", "application/xml, text/xml");
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSRequest request = new PSRequest(req, res, null, null);

    assertEquals(PSRequest.PAGE_TYPE_XML, request.getRequestPageType());
  }

  /**
   * When JSON and XML share the same quality, default stays XML (JSON must be strictly preferred).
   */
  @Test
  public void testEqualQualityJsonAndXmlDefaultsToXml() {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/Rhythmyx/MyApp/products");
    req.setContextPath("");
    req.setServletPath("/Rhythmyx/MyApp/products");
    req.addHeader("Accept", "application/json, application/xml");
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSRequest request = new PSRequest(req, res, null, null);

    assertEquals(PSRequest.PAGE_TYPE_XML, request.getRequestPageType());
  }

  /** Higher JSON quality factor wins over lower XML quality. */
  @Test
  public void testHigherJsonQualityWinsOverXml() {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/Rhythmyx/MyApp/products");
    req.setContextPath("");
    req.setServletPath("/Rhythmyx/MyApp/products");
    req.addHeader("Accept", "application/json;q=0.9, application/xml;q=0.5");
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSRequest request = new PSRequest(req, res, null, null);

    assertEquals(PSRequest.PAGE_TYPE_JSON, request.getRequestPageType());
  }

  /** Accept star/star alone does not select JSON (product default stays XML). */
  @Test
  public void testAcceptStarStarAloneDefaultsToXml() {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/Rhythmyx/MyApp/products");
    req.setContextPath("");
    req.setServletPath("/Rhythmyx/MyApp/products");
    req.addHeader("Accept", "*/*");
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSRequest request = new PSRequest(req, res, null, null);

    assertEquals(PSRequest.PAGE_TYPE_XML, request.getRequestPageType());
  }

  /** Explicit rejection of JSON ({@code q=0}) keeps product default XML. */
  @Test
  public void testAcceptJsonQZeroDoesNotSelectJson() {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/Rhythmyx/MyApp/products");
    req.setContextPath("");
    req.setServletPath("/Rhythmyx/MyApp/products");
    req.addHeader("Accept", "application/json;q=0");
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSRequest request = new PSRequest(req, res, null, null);

    assertEquals(PSRequest.PAGE_TYPE_XML, request.getRequestPageType());
  }

  /** XML rejected via {@code q=0} while JSON has positive q → JSON selected. */
  @Test
  public void testAcceptXmlQZeroJsonSelected() {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/Rhythmyx/MyApp/products");
    req.setContextPath("");
    req.setServletPath("/Rhythmyx/MyApp/products");
    req.addHeader("Accept", "application/xml;q=0, application/json;q=0.5");
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSRequest request = new PSRequest(req, res, null, null);

    assertEquals(PSRequest.PAGE_TYPE_JSON, request.getRequestPageType());
  }

  /**
   * q-values above 1 are clamped to 1.0 (RFC 7231), so {@code application/json;q=1.5} ties
   * default-q XML and stays XML (strict preference required).
   */
  @Test
  public void testAcceptJsonQAboveOneClampedDoesNotBeatDefaultXml() {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/Rhythmyx/MyApp/products");
    req.setContextPath("");
    req.setServletPath("/Rhythmyx/MyApp/products");
    // Without clamp, q=1.5 would beat implicit XML competitors incorrectly if any were present;
    // with equal-quality application/xml (q=1), clamp keeps JSON from winning solely via q>1.
    req.addHeader("Accept", "application/json;q=1.5, application/xml");
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSRequest request = new PSRequest(req, res, null, null);

    assertEquals(PSRequest.PAGE_TYPE_XML, request.getRequestPageType());
  }

  /** Known extension always wins over Accept (XML extension + Accept JSON → XML). */
  @Test
  public void testExtensionWinsOverAcceptJson() {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/Rhythmyx/MyApp/products.xml");
    req.setContextPath("");
    req.setServletPath("/Rhythmyx/MyApp/products.xml");
    req.addHeader("Accept", "application/json");
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSRequest request = new PSRequest(req, res, null, null);

    assertEquals(PSRequest.PAGE_TYPE_XML, request.getRequestPageType());
    assertEquals(".xml", request.getRequestPageExtension().toLowerCase());
  }

  /** Known JSON extension wins even when Accept prefers XML. */
  @Test
  public void testJsonExtensionWinsOverAcceptXml() {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/Rhythmyx/MyApp/products.json");
    req.setContextPath("");
    req.setServletPath("/Rhythmyx/MyApp/products.json");
    req.addHeader("Accept", "text/xml, application/xml");
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSRequest request = new PSRequest(req, res, null, null);

    assertEquals(PSRequest.PAGE_TYPE_JSON, request.getRequestPageType());
  }

  /** Unknown extensions stay UNKNOWN; Accept does not override. */
  @Test
  public void testUnknownExtensionNotOverriddenByAccept() {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/Rhythmyx/MyApp/products.bin");
    req.setContextPath("");
    req.setServletPath("/Rhythmyx/MyApp/products.bin");
    req.addHeader("Accept", "application/json");
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSRequest request = new PSRequest(req, res, null, null);

    assertEquals(PSRequest.PAGE_TYPE_UNKNOWN, request.getRequestPageType());
  }

  @Test
  public void testIsJsonAcceptMediaTypeHelpers() {
    assertTrue(PSRequest.isJsonAcceptMediaType("application/json"));
    assertTrue(PSRequest.isJsonAcceptMediaType("application/ld+json"));
    assertTrue(PSRequest.isJsonAcceptMediaType("application/vnd.api+json"));
    assertFalse(PSRequest.isJsonAcceptMediaType("text/json"));
    assertFalse(PSRequest.isJsonAcceptMediaType("application/xml"));
    assertFalse(PSRequest.isJsonAcceptMediaType("*/*"));

    assertTrue(PSRequest.isXmlOrHtmlAcceptMediaType("application/xml"));
    assertTrue(PSRequest.isXmlOrHtmlAcceptMediaType("text/xml"));
    assertTrue(PSRequest.isXmlOrHtmlAcceptMediaType("text/html"));
    assertTrue(PSRequest.isXmlOrHtmlAcceptMediaType("application/xhtml+xml"));
    assertTrue(PSRequest.isXmlOrHtmlAcceptMediaType("application/atom+xml"));
    assertFalse(PSRequest.isXmlOrHtmlAcceptMediaType("application/json"));
  }

  /**
   * Tests the putAllParameters method to make sure it add parameters and replaces existing values.
   */
  @Test
  public void testPutAllParameters() throws Exception {
    // build parameter map
    HashMap params = new HashMap();
    params.put("alpha", "beta");
    params.put("foo", "bar");

    PSRequest request = getEmptyRequest();
    request.setParameters(params);
    assertEquals("bar", request.getParameter("foo"));
    assertEquals("beta", request.getParameter("alpha"));

    HashMap newParams = new HashMap();
    newParams.put("charlie", "delta");
    newParams.put("foo", "elephant");
    request.putAllParameters(newParams);
    assertEquals("elephant", request.getParameter("foo"));
    assertEquals("beta", request.getParameter("alpha"));
    assertEquals("delta", request.getParameter("charlie"));
  }

  /** JUnit 3 style suite removed; using JUnit 5 @Test methods instead */
}
