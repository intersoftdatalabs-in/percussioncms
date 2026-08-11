/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.percussion.pso.jexl;

import com.percussion.extension.IPSJexlExpression;
import com.percussion.extension.IPSJexlMethod;
import com.percussion.extension.IPSJexlParam;
import com.percussion.extension.PSJexlUtilBase;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Provide tools to work with remote XML/HTML/JSON content
 *
 * @author justinraines
 */
@SuppressWarnings("unused")
public class PSORemoteContentTools extends PSJexlUtilBase implements IPSJexlExpression {

  private static final Logger log = LogManager.getLogger(PSORemoteContentTools.class);
  private static final int SC_OK = 200;
  private static final int SC_NOT_MODIFIED = 304;
  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

  /**
   * Creates a new PSORemoteContentTools.
   */
  public PSORemoteContentTools() {
    super();
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString the url string
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  // TODO: Remove me @SuppressFBWarnings("HTTP_PARAMETER_POLLUTION") //Is an api specifically for
  // pulling remote content
  @IPSJexlMethod(
      description = "Returns a status code for a url",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired")
      },
      returns = "Returns an integer status code")
  /**
   * Returns the httpstatus code.
   *
   * @param urlString the url string
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public int getHTTPStatusCode(String urlString) throws IllegalArgumentException, IOException {
    return executeGet(urlString, null, null, null).statusCode();
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString the url string
   * @param username the username
   * @param password the password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  @IPSJexlMethod(
      description = "Returns a status code for a url",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a integer status code")
  /**
   * Returns the httpstatus code.
   *
   * @param urlString the url string
   * @param username the username
   * @param password the password
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public int getHTTPStatusCode(String urlString, String username, String password)
      throws IllegalArgumentException, IOException {
    return executeGet(urlString, null, username, password).statusCode();
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString the url string
   * @param headers the headers
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  @IPSJexlMethod(
      description = "Returns status code based on url",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "map of headers to set")
      },
      returns = "Returns a integer status code")
  /**
   * Returns the httpstatus code.
   *
   * @param urlString the url string
   * @param headers the headers
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public int getHTTPStatusCode(String urlString, Map<String, String> headers)
      throws IllegalArgumentException, IOException {
    return executeGet(urlString, headers, null, null).statusCode();
  }

  /**
   * This gets a status code for a url.
   *
   * @param urlString the url string
   * @param headers the headers
   * @param username the username
   * @param password the password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  @IPSJexlMethod(
      description = "Returns status code based on a URL.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "map of headers to set"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a integer status code")
  /**
   * Returns the httpstatus code.
   *
   * @param urlString the url string
   * @param headers the headers
   * @param username the username
   * @param password the password
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public int getHTTPStatusCode(
      String urlString, Map<String, String> headers, String username, String password)
      throws IllegalArgumentException, IOException {
    return executeGet(urlString, headers, username, password).statusCode();
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString the url string
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  // TODO: Remove me @SuppressFBWarnings("HTTP_PARAMETER_POLLUTION") //Is an API method for
  // returning remote JSON content in a template.
  @IPSJexlMethod(
      description = "Returns parsed JSON content (object or array) from a URL.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired")
      },
      returns = "Returns parsed JSON as Object (Map for objects, List for arrays)")
  /**
   * Returns the remote jsoncontent.
   *
   * @param urlString the url string
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public Object getRemoteJSONContent(String urlString)
      throws IllegalArgumentException, IOException {
    HttpResponse<String> response = executeGet(urlString, null, null, null);
    int statusCode = response.statusCode();
    if (statusCode != SC_OK && statusCode != SC_NOT_MODIFIED) {
      log.warn(
          "JEXL: getRemoteJSONContent request was not 200/304: URL: {} Status Code: {}",
          urlString,
          statusCode);
    }
    return OBJECT_MAPPER.readValue(response.body(), Object.class);
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString the url string
   * @param username the username
   * @param password the password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  // TODO: Remove me @SuppressFBWarnings("HTTP_PARAMETER_POLLUTION") //Is an API
  @IPSJexlMethod(
      description = "Returns parsed JSON content (object or array) from a URL with basic auth.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns parsed JSON as Object (Map for objects, List for arrays)")
  /**
   * Returns the remote jsoncontent.
   *
   * @param urlString the url string
   * @param username the username
   * @param password the password
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public Object getRemoteJSONContent(String urlString, String username, String password)
      throws IllegalArgumentException, IOException {
    HttpResponse<String> response = executeGet(urlString, null, username, password);
    int statusCode = response.statusCode();
    if (statusCode != SC_OK && statusCode != SC_NOT_MODIFIED) {
      log.warn(
          "JEXL: getRemoteJSONContent response was not 200/304. URL: {} Status Code: {}",
          urlString,
          statusCode);
    }
    return OBJECT_MAPPER.readValue(response.body(), Object.class);
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString the url string
   * @param headers the headers
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  @IPSJexlMethod(
      description = "Returns parsed JSON content (object or array) from a URL with headers.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "map of headers to set")
      },
      returns = "Returns parsed JSON as Object (Map for objects, List for arrays)")
  /**
   * Returns the remote jsoncontent.
   *
   * @param urlString the url string
   * @param headers the headers
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public Object getRemoteJSONContent(String urlString, Map<String, String> headers)
      throws IllegalArgumentException, IOException {
    HttpResponse<String> response = executeGet(urlString, headers, null, null);
    if (response.statusCode() != SC_OK) {
      log.debug("Get failed for URL {}. Status code: {}", urlString, response.statusCode());
    }
    return OBJECT_MAPPER.readValue(response.body(), Object.class);
  }

  /**
   * This gets remote JSON content and returns a JSONobject.
   *
   * @param urlString the url string
   * @param headers the headers
   * @param username the username
   * @param password the password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  // TODO: Remove me @SuppressFBWarnings("HTTP_PARAMETER_POLLUTION") // Is an api method for getting
  // remote data by url
  @IPSJexlMethod(
      description =
          "Returns parsed JSON content (object or array) from a URL with headers and basic auth.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "map of headers to set"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns parsed JSON as Object (Map for objects, List for arrays)")
  /**
   * Returns the remote jsoncontent.
   *
   * @param urlString the url string
   * @param headers the headers
   * @param username the username
   * @param password the password
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public Object getRemoteJSONContent(
      String urlString, Map<String, String> headers, String username, String password)
      throws IllegalArgumentException, IOException {
    HttpResponse<String> response = executeGet(urlString, headers, username, password);
    int statusCode = response.statusCode();
    if (statusCode != SC_OK && statusCode != SC_NOT_MODIFIED) {
      log.warn(
          "JEXL: getRemoteJSONContent was not 200/304. URL: {} Status Code:{}",
          urlString,
          statusCode);
    }
    return OBJECT_MAPPER.readValue(response.body(), Object.class);
  }

  private HttpResponse<String> executeGet(
      String urlString, Map<String, String> headers, String username, String password)
      throws IOException {
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(urlString)).GET();

    Map<String, String> requestHeaders = headers == null ? new HashMap<>() : headers;
    for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
      if (entry.getKey() != null && entry.getValue() != null) {
        requestBuilder.header(entry.getKey(), entry.getValue());
      }
    }

    if (username != null && password != null) {
      String login = username + ":" + password;
      String base64Login =
          new String(
              Base64.encodeBase64(login.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
      requestBuilder.header("Authorization", "Basic " + base64Login);
    }

    try {
      return HTTP_CLIENT.send(
          requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while requesting " + urlString, e);
    }
  }

  /**
   * This gets remote XML content and returns a JSOUP Document object.
   *
   * @param urlString the url string
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired")
      },
      returns = "Returns a JSOUP document")
  /**
   * Returns the remote xmlcontent.
   *
   * @param urlString the url string
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public Document getRemoteXMLContent(String urlString)
      throws IllegalArgumentException, IOException {

    return Jsoup.connect(urlString).get();
  }

  /**
   * This gets remote XML content with basic authentication and returns a JSOUP Document object.
   *
   * @param urlString the url string
   * @param username the username
   * @param password the password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a JSOUP document")
  /**
   * Returns the remote xmlcontent.
   *
   * @param urlString the url string
   * @param username the username
   * @param password the password
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public org.jsoup.nodes.Document getRemoteXMLContent(
      String urlString, String username, String password)
      throws IllegalArgumentException, IOException {
    String login = username + ":" + password;
    String base64login = new String(Base64.encodeBase64(login.getBytes(StandardCharsets.UTF_8)));

    return Jsoup.connect(urlString).header("Authorization", "Basic " + base64login).get();
  }

  /**
   * This gets remote XML content with map of headers and returns a JSOUP Document object.
   *
   * @param urlString the url string
   * @param headers the headers
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "Map of headers")
      },
      returns = "Returns a JSOUP document")
  /**
   * Returns the remote xmlcontent.
   *
   * @param urlString the url string
   * @param headers the headers
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public Document getRemoteXMLContent(String urlString, Map<String, String> headers)
      throws IllegalArgumentException, IOException {
    Connection connection = Jsoup.connect(urlString);
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      connection = connection.header(entry.getKey(), entry.getValue());
    }
    return connection.get();
  }

  /**
   * This gets remote XML content with map of headers, username, and password then returns a JSOUP
   * Document object.
   *
   * @param urlString the url string
   * @param headers the headers
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "Map of headers"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a JSOUP document")
  /**
   * Returns the remote xmlcontent.
   *
   * @param urlString the url string
   * @param headers the headers
   * @param username the username
   * @param password the password
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public Document getRemoteXMLContent(
      String urlString, Map<String, String> headers, String username, String password)
      throws IllegalArgumentException, IOException {
    String login = username + ":" + password;
    String base64login = new String(Base64.encodeBase64(login.getBytes(StandardCharsets.UTF_8)));
    Connection connection =
        Jsoup.connect(urlString).header("Authorization", "Basic " + base64login);
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      connection = connection.header(entry.getKey(), entry.getValue());
    }
    return connection.get();
  }

  /* ALIAS METHODS FOR HTML CONTENT, THESE SIMPLY CALL THEIR XML COUNTERPARTS */
  /**
   * This is an aliased method for getRemoteXMLContent
   *
   * @param urlString the url string
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  @IPSJexlMethod(
      description =
          "Returns JSOUP document with xml content, returns a JSoup Document element. Alias for"
              + " getRemoteXMLContent",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired")
      },
      returns = "Returns a JSOUP document")
  /**
   * Returns the remote htmlcontent.
   *
   * @param urlString the url string
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public Document getRemoteHTMLContent(String urlString)
      throws IllegalArgumentException, IOException {
    return getRemoteXMLContent(urlString);
  }

  /**
   * This is an aliased method for getRemoteXMLContent, with basic authorization
   *
   * @param urlString the url string
   * @param username the username
   * @param password the password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a JSOUP document")
  /**
   * Returns the remote htmlcontent.
   *
   * @param urlString the url string
   * @param username the username
   * @param password the password
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public Document getRemoteHTMLContent(String urlString, String username, String password)
      throws IllegalArgumentException, IOException {
    return getRemoteXMLContent(urlString, username, password);
  }

  /**
   * This is an aliased method for getRemoteXMLContent, with headers
   *
   * @param urlString the url string
   * @param headers the headers
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "Map of headers")
      },
      returns = "Returns a JSOUP document")
  /**
   * Returns the remote htmlcontent.
   *
   * @param urlString the url string
   * @param headers the headers
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public Document getRemoteHTMLContent(String urlString, Map<String, String> headers)
      throws IllegalArgumentException, IOException {
    return getRemoteXMLContent(urlString, headers);
  }

  /**
   * This is an aliased method for getRemoteXMLContent, with headers and basic authorization
   *
   * @param urlString the URL string
   * @param headers the headers map
   * @param username the username
   * @param password the password
   * @return org.jsoup.nodes.Document
   * @throws IllegalArgumentException if arguments are invalid
   * @throws IOException if I/O error occurs
   */
  @IPSJexlMethod(
      description = "Returns JSOUP document with xml content, returns a JSoup Document element.",
      params = {
        @IPSJexlParam(
            name = "urlString",
            description = "url to pull content from, include query params if desired"),
        @IPSJexlParam(name = "headers", description = "Map of headers"),
        @IPSJexlParam(name = "username", description = "username"),
        @IPSJexlParam(name = "password", description = "password")
      },
      returns = "Returns a JSOUP document")
  /**
   * Returns the remote htmlcontent.
   *
   * @param urlString the url string
   * @param headers the headers
   * @param username the username
   * @param password the password
   * @return the result
   * @throws IllegalArgumentException if an error occurs
   * @throws IOException if an error occurs
   */
  public Document getRemoteHTMLContent(
      String urlString, Map<String, String> headers, String username, String password)
      throws IllegalArgumentException, IOException {
    return getRemoteXMLContent(urlString, headers, username, password);
  }
}
