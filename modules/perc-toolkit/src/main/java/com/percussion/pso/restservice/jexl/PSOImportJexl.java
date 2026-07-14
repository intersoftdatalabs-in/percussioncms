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
package com.percussion.pso.restservice.jexl;

import com.percussion.extension.IPSJexlExpression;
import com.percussion.extension.IPSJexlMethod;
import com.percussion.extension.IPSJexlParam;
import com.percussion.extension.PSJexlUtilBase;
import com.percussion.pso.restservice.IItemRestService;
import com.percussion.pso.restservice.ItemRestServiceLocator;
import com.percussion.pso.restservice.impl.ItemRestServiceImpl;
import com.percussion.pso.restservice.model.Field;
import com.percussion.pso.restservice.model.HttpDOMResponse;
import com.percussion.pso.restservice.model.HttpHtmlResponse;
import com.percussion.pso.restservice.model.Item;
import com.percussion.pso.restservice.utils.HtmlLinkHelper;
import com.percussion.pso.restservice.utils.ItemServiceHelper;
import com.percussion.pso.utils.HTTPProxyClientConfig;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSRequest;
import com.percussion.utils.request.PSRequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;

/** */
public class PSOImportJexl extends PSJexlUtilBase implements IPSJexlExpression {

  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(PSOImportJexl.class);

  private static final String HTTP_IFMODIFIED = "If-Modified-Since";
  private static final String HTTP_IFNONEMATCH = "If-None-Match";
  private static final int HTTP_NOT_MODIFIED = 304;

  /**
   * Method getPostBodyAsDom.
   *
   * @return the document
   * @throws IOException if an I/O error occurs
   */
  @IPSJexlMethod(
      description =
          "Gets html posted to the template and converts it to tidied xml compliant output ",
      params = {})
  public Document getPostBodyAsDom() throws IOException {
    PSRequest req = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
    HttpServletRequest sreq = req.getServletRequest();
    return parseHtmlToDom(sreq.getInputStream());
  }

  /**
   * Convert an InputStream containing HTML into a dom4j Document using jsoup. This replaces the
   * legacy TagSoup-based cleaning logic.
   *
   * @param in html input stream (UTF-8 assumed)
   * @return a dom4j Document
   * @throws IOException if the stream cannot be read
   */
  private Document parseHtmlToDom(InputStream in) throws IOException {
    org.jsoup.nodes.Document jdoc = Jsoup.parse(in, "UTF-8", "");
    org.w3c.dom.Document w3c = new W3CDom().fromJsoup(jdoc);
    return new org.dom4j.io.DOMReader().read(w3c);
  }

  /**
   * Method getHttpAsDom.
   *
   * @param url String
   * @return Document
   */
  @IPSJexlMethod(
      description = "Extracts html from a url and convert to dom",
      params = {@IPSJexlParam(name = "url", description = "the url to connect to")})
  public Document getHttpAsDom(String url) {
    Document doc = null;
    HttpClient client = createHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofMillis(2000)).build();

    try {
      HttpResponse<InputStream> response =
          client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      try (InputStream responseBody = response.body()) {
        doc = parseHtmlToDom(responseBody);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error(e.getMessage());
      log.debug(e.getMessage(), e);
    } catch (Exception ex) {
      log.error(ex.getMessage());
      log.debug(ex.getMessage(), ex);
    }
    return doc;
  }

  /**
   * Method getHttpAsDom.
   *
   * <p>This routine will try to download the specified URL. If the server says the file hasn't
   * changed, it will not be downloaded.
   *
   * <p>The resulting document will have a block of XML in the headers: namespace that represent the
   * headers the server returns. This XML block can then be cached with the resulting item for
   * subsequant calls to this routine.
   *
   * <p>This is to avoid the expensive process of downloading content unecessarily in Feed
   * scenarios, when the Feed is updated but many of the items in the feed are not.
   *
   * @param url String HTTP Resource to download
   * @param keyfield Indicates the field in the system that stores the hash code for the specified
   *     URL
   * @param contextRoot Indicates the path in the system where this item may already be stored.
   * @return Document
   */
  @IPSJexlMethod(
      description =
          "Extracts html from a url and convert to dom. Given the specified keyfield and"
              + " contextroot, will attempt to find any items that match the url's has code in the"
              + " keyfield and path.  If found any previous cached etag and/or last modified header"
              + " data will be used whn checking to see if the item needs to be downloaded.",
      params = {
        @IPSJexlParam(name = "url", description = "the url to connect to"),
        @IPSJexlParam(
            name = "keyfield",
            description = "The field containing the hash code for the specified URL."),
        @IPSJexlParam(
            name = "contextroot",
            description = "The root path within the system where this item may already be stored.")
      })
  public HttpDOMResponse getHttpAsDom(String url, String keyfield, String contextRoot) {
    HttpDOMResponse ret = null;
    Document doc = null;
    String etag = null;
    String lastModified = null;

    /**
     * Before we do anything we need to check to see if this item exists. If it does we want to get
     * the cached ETag and Last Modified headers to we don't download it unecessarily.
     */
    // IItemRestService svc = ItemRestServiceLocator.getItemServiceBase();
    ItemRestServiceImpl svc = new ItemRestServiceImpl();

    Item item = svc.findByKeyField(Integer.toString(url.hashCode()), keyfield, contextRoot);

    if (item != null && item.getContentId() != null) {
      log.debug("Located existing item for URL {}", url);
      Field etag_field = item.getField("cached_etag");
      if (etag_field != null) {
        etag = etag_field.getStringValue();
        log.debug("ETag Header set to {}", etag);
      }

      Field lm_field = item.getField("cached_lastmodified");
      if (lm_field != null) {
        lastModified = lm_field.getStringValue();
        log.debug("Last-Modified Header set to {}", lastModified);
      }
    }

    HttpClient client = createHttpClient();
    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofMillis(2000));

    if (etag != null && !etag.trim().isEmpty()) {
      requestBuilder.header(HTTP_IFNONEMATCH, etag);
    }

    if (lastModified != null && !lastModified.trim().isEmpty()) {
      requestBuilder.header(HTTP_IFMODIFIED, lastModified);
    }

    try {
      HttpResponse<InputStream> response =
          client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
      int code = response.statusCode();

      if (code != HTTP_NOT_MODIFIED) {
        try (InputStream responseBody = response.body()) {
          doc = parseHtmlToDom(responseBody);
        }
        ret = new HttpDOMResponse(doc, response.headers());
        ret.setExistingItem(item);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error(e.getMessage());
      log.debug(e.getMessage(), e);
    } catch (Exception ex) {
      log.error(ex.getMessage());
      log.debug(ex.getMessage(), ex);
    }

    return ret;
  }

  /**
   * Method getPostBody.
   *
   * @return String
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Gets posted body as string",
      params = {})
  public String getPostBody() throws IOException {
    PSRequest req = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
    HttpServletRequest sreq = req.getServletRequest();

    BufferedReader br = new BufferedReader(new InputStreamReader(sreq.getInputStream()));

    StringBuilder sb = new StringBuilder();

    String str;
    while ((str = br.readLine()) != null) {
      sb.append(str);
    }
    return sb.toString();
  }

  /**
   * Method getPostDom.
   *
   * @return Document
   * @throws IOException
   */
  private SAXReader createSecureSAXReader() {
    SAXReader reader = new SAXReader();
    try {
      reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
      reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    } catch (Exception e) {
      log.warn("Failed to set secure features on SAXReader: {}", e.getMessage());
    }
    return reader;
  }

  /**
   * Method getPostDom.
   *
   * @return Document
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Gets posted body as DOM",
      params = {})
  public Document getPostDom() throws IOException {
    PSRequest req = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
    HttpServletRequest sreq = req.getServletRequest();

    Document document = null;
    SAXReader reader = createSecureSAXReader();
    try {
      document = reader.read(sreq.getInputStream());
    } catch (DocumentException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    return document;
  }

  /**
   * Method getDomFromString.
   *
   * @param string String
   * @return Document
   * @throws IOException
   */
  @IPSJexlMethod(
      description = "Gets Dom from String",
      params = {@IPSJexlParam(name = "string", description = "the xml string ")})
  public Document getDomFromString(String string) throws IOException {

    Document document = null;
    SAXReader reader = createSecureSAXReader();

    try {
      document = reader.read(new StringReader(string));
    } catch (DocumentException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    return document;
  }

  /**
   * Method xpathSelectSingleNode.
   *
   * @param doc the document object
   * @param xpathString the xpath string
   * @param namespaces map of namespace prefixes
   * @return the selected node
   */
  @IPSJexlMethod(
      description = "Uses xpath to search a document containing namespaces",
      params = {
        @IPSJexlParam(name = "node", description = "the node or document"),
        @IPSJexlParam(name = "xpath", description = "the xpath string"),
        @IPSJexlParam(name = "namespaces", description = "a map of namespace prefixes")
      })
  public Node xpathSelectSingleNode(
      Object doc, String xpathString, Map<String, String> namespaces) {
    Node nd = (Node) doc;

    XPath xpath = nd.createXPath(xpathString);
    xpath.setNamespaceURIs(namespaces);

    return xpath.selectSingleNode(nd);
  }

  /**
   * Method xpathSelectNodes.
   *
   * @param doc the document object
   * @param xpathString the xpath string
   * @param namespaces map of namespace prefixes
   * @return list of nodes
   */
  @IPSJexlMethod(
      description = "Uses xpath to search a document containing namespaces",
      params = {
        @IPSJexlParam(name = "node", description = "the node or document"),
        @IPSJexlParam(name = "xpath", description = "the xpath string"),
        @IPSJexlParam(name = "namespaces", description = "a map of namespace prefixes")
      })
  public List<?> xpathSelectNodes(Object doc, String xpathString, Map<String, String> namespaces) {
    XPath xpath = DocumentHelper.createXPath(xpathString);
    xpath.setNamespaceURIs(namespaces);

    return xpath.selectNodes(doc);
  }

  /**
   * Method getItemXml.
   *
   * @param contentId int
   * @return Document
   */
  @IPSJexlMethod(
      description = "Gets gets the xml for an existing content item by id",
      params = {@IPSJexlParam(name = "contentId", description = "the id for the Item")})
  public Document getItemXml(int contentId) {
    IItemRestService itemservice = ItemRestServiceLocator.getItemServiceBase();
    /*	ItemRestService itemservice = JAXRSClientFactory.create("http://localhost:9992/Rhythmyx/services",ItemRestService.class,"admin1","demo",null);
    WebClient.client(itemservice).accept("text/xml");
    WebClient.client(itemservice).header("RX_USEBASICAUTH","true");
     */
    Item item = itemservice.getItem(contentId);

    return ItemServiceHelper.getItemDOM(item);
  }

  /**
   * Method getHttpAsDom.
   *
   * <p>This routine will try to download the specified URL. If the server says the file hasn't
   * changed, it will not be downloaded.
   *
   * <p>The resulting document will have a block of XML in the headers: namespace that represent the
   * headers the server returns. This XML block can then be cached with the resulting item for
   * subsequant calls to this routine.
   *
   * <p>This is to avoid the expensive process of downloading content unecessarily in Feed
   * scenarios, when the Feed is updated but many of the items in the feed are not.
   *
   * @param url String HTTP Resource to download
   * @param keyfield Indicates the field in the system that stores the hash code for the specified
   *     URL
   * @param contextRoot Indicates the path in the system where this item may already be stored.
   * @return Document
   */
  @IPSJexlMethod(
      description =
          "Extracts html from a url and convert to dom. Given the specified keyfield and"
              + " contextroot, will attempt to find any items that match the url's has code in the"
              + " keyfield and path.  If found any previous cached etag and/or last modified header"
              + " data will be used whn checking to see if the item needs to be downloaded.",
      params = {
        @IPSJexlParam(name = "url", description = "the url to connect to"),
        @IPSJexlParam(
            name = "keyfield",
            description = "The field containing the hash code for the specified URL."),
        @IPSJexlParam(
            name = "contextroot",
            description = "The root path within the system where this item may already be stored.")
      })
  public HttpHtmlResponse getWildHtmlAsDom(String url, String keyfield, String contextRoot) {
    HttpHtmlResponse ret = null;
    org.jsoup.nodes.Document doc = null;
    String etag = null;
    String lastModified = null;

    /**
     * Before we do anything we need to check to see if this item exists. If it does we want to get
     * the cached ETag and Last Modified headers to we don't download it unnecessarily.
     */
    // IItemRestService svc = ItemRestServiceLocator.getItemServiceBase();
    ItemRestServiceImpl svc = new ItemRestServiceImpl();

    Item item = svc.findByKeyField(Integer.toString(url.hashCode()), keyfield, contextRoot);

    if (item != null && item.getContentId() != null) {
      log.debug("Located existing item for URL " + url);
      Field etag_field = item.getField("cached_etag");
      if (etag_field != null) {
        etag = etag_field.getStringValue();
        log.debug("ETag Header set to " + etag);
      }

      Field lm_field = item.getField("cached_lastmodified");
      if (lm_field != null) {
        lastModified = lm_field.getStringValue();
        log.debug("Last-Modified Header set to " + lastModified);
      }
    }

    HttpClient client = createHttpClient();
    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofMillis(2000));

    if (etag != null && !etag.trim().isEmpty()) {
      requestBuilder.header(HTTP_IFNONEMATCH, etag);
    }

    if (lastModified != null && !lastModified.trim().isEmpty()) {
      requestBuilder.header(HTTP_IFMODIFIED, lastModified);
    }

    requestBuilder.header("Content-Type", "text/xhtml; charset=UTF-8");

    try {
      HttpResponse<InputStream> response =
          client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
      int code = response.statusCode();

      if (code != HTTP_NOT_MODIFIED) {
        try (InputStream responseBody = response.body()) {
          log.debug(
              "Response Content-Type: {}",
              response.headers().firstValue("Content-Type").orElse("Unknown"));

          doc =
              HtmlLinkHelper.convertLinksToAbsolute(
                  url, Jsoup.parse(responseBody, "UTF-8", HtmlLinkHelper.getBaseLink(url)));
        }

        ret = new HttpHtmlResponse(doc, response.headers());

        ret.setExistingItem(item);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error(e.getMessage());
      log.debug(e.getMessage(), e);
    } catch (Exception ex) {
      log.error(ex.getMessage());
      log.debug(ex.getMessage(), ex);
    }

    return ret;
  }

  private HttpClient createHttpClient() {
    HTTPProxyClientConfig proxy = new HTTPProxyClientConfig();

    HttpClient.Builder builder =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofMillis(2000));

    if (proxy.getProxyServer() != null
        && !proxy.getProxyServer().isBlank()
        && proxy.getProxyPort() != null
        && !proxy.getProxyPort().isBlank()) {
      log.debug("Setting Proxy server to {}:{}", proxy.getProxyServer(), proxy.getProxyPort());
      builder.proxy(
          ProxySelector.of(
              new InetSocketAddress(
                  proxy.getProxyServer(), Integer.parseInt(proxy.getProxyPort()))));
    }

    return builder.build();
  }

  /**
   * Method getHttpAsDom.
   *
   * @param url String
   * @return Document
   */
  @IPSJexlMethod(
      description = "Extracts html from a url and convert to dom",
      params = {
        @IPSJexlParam(name = "url", description = "The base url"),
        @IPSJexlParam(name = "html", description = "The body content")
      })
  public String cleanRelativeLinks(String url, String html) {

    String ret = html;

    try {
      ret = HtmlLinkHelper.convertLinksToAbsolute(url, html);
    } catch (MalformedURLException e) {
      log.debug(e, e);
      ret = html;
      log.warn(
          "An error occurred while cleaning relative links, content may still contain relative"
              + " links.");
    } catch (URISyntaxException e) {
      log.debug(e, e);
      ret = html;
      log.warn(
          "An error occurred while cleaning relative links, content may still contain relative"
              + " links.");
    }

    return ret;
  }

  @IPSJexlMethod(
      description = "Returns a SHA-1 hash for the specified string.",
      params = {@IPSJexlParam(name = "data", description = "The data to hash")})
  public String getHash(String data) {
    String ret = String.valueOf(data.hashCode());

    try {
      MessageDigest checksum = MessageDigest.getInstance("SHA-256");
      checksum.reset();
      checksum.update(data.getBytes());

      ret = asHex(checksum.digest());
    } catch (NoSuchAlgorithmException e) {
      log.debug(e, e);
    }
    return ret;
  }

  private static String asHex(byte[] b) {
    String result = "";
    for (int i = 0; i < b.length; i++) {
      result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
    }
    return result;
  }
}
