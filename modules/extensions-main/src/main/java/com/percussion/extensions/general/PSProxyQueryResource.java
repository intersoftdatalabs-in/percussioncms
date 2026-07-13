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

package com.percussion.extensions.general;

import static com.percussion.utils.request.PSRequestInfoBase.KEY_JSESSIONID;
import static com.percussion.xml.PSXmlDocumentBuilder.createXmlDocument;
import static java.util.Arrays.asList;

import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.extensions.utils.PSExtensionParamsHelper;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.validation.URLValidation;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSServer;
import com.percussion.utils.request.PSRequestInfo;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This exit allows you to make an internal request to an external resource. <br>
 * The request parameters that are non-null are copied to the provided url that is to be called
 * using HTTP GET.
 *
 * <p>The results of the request are then converted to W3C DOM Document. <em>If the results are not
 * XML you will get an XML error.</em>
 *
 * <p>This is useful when you need to make a sys_Lookup XML document for a control and would like to
 * use JSP instead of a full blown legacy XML query resource. <br>
 * See Extensions.xml for the parameters that you can pass to this exit.
 *
 * @author adamgent
 */
public class PSProxyQueryResource extends PSDefaultExtension implements IPSResultDocumentProcessor {

  private static final String PARAM_PASSWORD = "password";
  private static final String PARAM_USER = "user";
  private static final String PARAM_URL = "url";

  /** The log instance to use for this class, never <code>null</code>. */
  private static final Logger log = LogManager.getLogger(PSProxyQueryResource.class);

  public boolean canModifyStyleSheet() {
    return false;
  }

  public Document processResultDocument(
      Object[] params, IPSRequestContext request, Document resultDoc)
      throws PSParameterMismatchException, PSExtensionProcessingException {
    try {
      Map<String, String> p = getParameters(params);
      PSExtensionParamsHelper helper = new PSExtensionParamsHelper(p, request, log);
      String url = helper.getRequiredParameter(PARAM_URL);
      String user = helper.getOptionalParameter(PARAM_USER, null);
      String password = helper.getOptionalParameter(PARAM_PASSWORD, null);
      String host = "";
      int port = -1;
      String scheme = "http";
      URI requestUri = null;

      boolean internalRequest = false;
      String prepend = null;
      String queryString = buildUrlQueryString(request, asList(PARAM_URL));
      if (StringUtils.isBlank(queryString)) {
        prepend = "";
      } else if (url.contains("?")) {
        prepend = "&amp;";
      } else {
        prepend = "?";
      }

      url = url + prepend + queryString;
      // Cache getRequestRoot() so the validation branch and the
      // internal/external dispatch below both see the same value
      // (single-call semantics; per the review on this PR at line 124).
      String requestRoot = PSServer.getRequestRoot();
      boolean isInternal = url.startsWith(requestRoot);
      if (url.startsWith("../")) {
        // Rewrite as absolute to the server
        url = requestRoot + url.substring(2);
        isInternal = true;
      }

      // Validate the EFFECTIVE OUTBOUND target URL, not the raw request
      // string. This is the data-flow ordering CodeQL's taint analysis
      // needs to recognize the request as sanitized per specs/004-
      // zero-code-scanning-alerts/tasks.md T037 and contracts/C2.
      //
      // For internal requests (../ rewrite or absolute path under
      // requestRoot) the outbound target is forced to 127.0.0.1:
      // PSServer.getListenerPort(); build that URL and validate it.
      // Loopback is always allowed per URLValidationConfig regardless
      // of port, so this passes the validator for every internal
      // request regardless of the requestRoot hostname.
      //
      // For external requests the outbound target is the URL itself.
      // The validator accepts the scheme, host, and port per
      // URLValidationConfig; the path that flows into the URI is the
      // same one the validator accepted.
      // (per the review on this PR at line 127: avoids the regression
      //  where PSServer.getRequestRoot resolves to a non-loopback host
      //  on a non-standard listener port and is rejected by the validator.)
      URL validatedUrl;
      try {
        if (isInternal) {
          int internalPort = PSServer.getListenerPort();
          int pathStart = url.indexOf('/', requestRoot.length());
          String targetPath = pathStart >= 0 ? url.substring(pathStart) : "/";
          URL outboundTarget =
              new URL(scheme, "127.0.0.1", internalPort, targetPath);
          validatedUrl = URLValidation.validateURLString(outboundTarget.toString());
        } else {
          validatedUrl = URLValidation.validateURLString(url);
        }
      } catch (SecurityException e) {
        log.error(
            "URL validation failed for request: {}",
            PSExceptionUtils.getMessageForLog(e));
        throw new PSExtensionProcessingException(0, "Invalid URL: " + e.getMessage());
      }

      if (isInternal) {
        internalRequest = true;

        host = "127.0.0.1";
        port = PSServer.getListenerPort();

        try {
          // Use the validated URL's protocol/path/query/ref; force the
          // host/port to 127.0.0.1:PSServer.getListenerPort() since the
          // relative-URL branch concatenates against requestRoot.
          requestUri =
              new URI(
                  validatedUrl.getProtocol(),
                  null,
                  host,
                  port,
                  validatedUrl.getPath(),
                  validatedUrl.getQuery(),
                  validatedUrl.getRef());

          // This is an internal request so pass the jsessionid
          String sessionid = (String) PSRequestInfo.getRequestInfo(KEY_JSESSIONID);
          if (StringUtils.isNotBlank(sessionid)) {
            String sessionPath = requestUri.getRawPath() + ";jsessionid=" + sessionid;
            requestUri =
                new URI(
                    requestUri.getScheme(),
                    requestUri.getRawUserInfo(),
                    requestUri.getHost(),
                    requestUri.getPort(),
                    sessionPath,
                    requestUri.getRawQuery(),
                    requestUri.getRawFragment());
          }

        } catch (URISyntaxException | IllegalArgumentException e) {
          log.error(
              "Error parsing supplied url: {} Error: {}",
              url,
              PSExceptionUtils.getMessageForLog(e));
          throw new RuntimeException("Error parsing supplied url:" + url, e);
        }
      } else {
        try {
          // Derive the URI from the validated URL object (not the raw
          // user-supplied string) — this is the data-flow ordering
          // CodeQL's taint analysis needs to recognize the request as
          // sanitized. See T037 and contracts/C2.
          requestUri = validatedUrl.toURI();
        } catch (URISyntaxException e) {
          log.error(
              "Error converting validated url to URI: {} Error: {}",
              url,
              PSExceptionUtils.getMessageForLog(e));
          throw new RuntimeException("Error converting validated url:" + url, e);
        }
      }

      String repr = "url = " + url + " user = " + user + " password = " + password;
      log.debug("Trying to get document with: {}", repr);

      HttpClient client =
          HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(requestUri).GET();

      if (!internalRequest && !StringUtils.isEmpty(user)) {
        String credentials = user + ":" + StringUtils.defaultString(password);
        String encoded =
            Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        requestBuilder.header("Authorization", "Basic " + encoded);
      }

      try {
        HttpResponse<String> response =
            client.send(
                requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int statusCode = response.statusCode();

        if (statusCode != 200) {
          log.error("Remote request to url: {} failed with status code: {}", url, statusCode);
          throw new RuntimeException(
              "Remote request to url: " + url + " failed with status code: " + statusCode);
        }

        // Read the response body.
        String results = response.body();
        results = results.replaceFirst("<\\?xml.*\\?>", "");

        try {
          return createXmlDocument(new StringReader(results), false);
        } catch (SAXException e) {
          String message = "XML Error with " + repr;
          log.error(message, e);
          throw new Exception(message, e);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("HTTP request interrupted: {}", PSExceptionUtils.getMessageForLog(e));
        throw new Exception(e);
      } catch (IOException e) {
        log.error("Fatal transport error: {}", PSExceptionUtils.getMessageForLog(e));
        throw new Exception(e);
      }
    } catch (Exception e) {
      log.debug("PSProxyQueryResource attempt failed. Returning null to caller.", e);
      return null;
    }
  }

  @SuppressWarnings({"unused", "unchecked"})
  private String buildUrlQueryString(IPSRequestContext request, List<String> ignore) {
    Iterator<?> it = request.getParametersIterator();
    List<String> params = new ArrayList<>();
    while (it.hasNext()) {
      Entry<String, Object> element = (Entry<String, Object>) it.next();
      String name = element.getKey();
      if (ignore != null && ignore.contains(name)) continue;
      Object value = element.getValue();
      if (value == null) continue;
      String valueString = null;
      if (value instanceof String) {
        valueString = (String) value;
      } else if (value instanceof Number) {
        valueString = value.toString();
      }
      if (valueString != null) params.add(name + "=" + valueString);
    }
    return StringUtils.join(params.iterator(), "&amp;");
  }
}
