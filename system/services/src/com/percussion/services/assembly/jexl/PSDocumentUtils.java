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
package com.percussion.services.assembly.jexl;

import com.percussion.extension.IPSJexlMethod;
import com.percussion.extension.IPSJexlParam;
import com.percussion.extension.PSJexlUtilBase;
import com.percussion.security.PSThreadRequestUtils;
import com.percussion.security.validation.URLValidation;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.services.security.PSServletRequestWrapper;
import com.percussion.servlet_utils.servlet.PSServletUtils;
import com.percussion.system.utils.PSHtmlBodyInputStream;
import com.percussion.util.PSCharSets;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.string.PSStringUtils;
import com.percussion.utils.timing.PSStopwatchStack;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Utilities to make document requests from a velocity macro
 *
 * @author dougrand
 */
public class PSDocumentUtils extends PSJexlUtilBase
{
   /**
    * Calls the specified URL and returns the result document data or an empty
    * string on error.
    *
    * @param url The url, must not be <code>null</code> or empty
    * @return the result document data
    * @throws IOException
    * @throws ServletException
    */
   @IPSJexlMethod(description = "Calls the specified URL and returns the "
         + "result document data or an empty string on error.", params =
   {@IPSJexlParam(name = "url", description = "The url, must not be null or empty.")})
   public String getDocument(String url) throws IOException, ServletException
   {
      return getDocument(url, null, null);
   }

   /**
    * Calls the specified URL and returns the result document data or an empty
    * string on error.
    *
    * @param url The url, must not be <code>null</code> or empty
    * @param user The username, may be <code>null</code> or empty
    * @param password The password, may be <code>null</code> or empty
    * @return the result document
    * @throws IOException
    * @throws ServletException
    */
   @IPSJexlMethod(description = "Calls the specified URL and returns the "
         + "result document data or an empty string on error.", params =
   {
         @IPSJexlParam(name = "url", description = "The url, must not be null or empty."),
         @IPSJexlParam(name = "user", description = "The user name, may be null or empty."),
         @IPSJexlParam(name = "password", description = "The password, may be null or empty.")})
   public String getDocument(String url, String user, String password)
         throws IOException, ServletException
   {
      PSStopwatchStack sws = PSStopwatchStack.getStack();
      sws.start(getClass().getCanonicalName() + "#getDocument");
      try
      {
         if (url.startsWith("../"))
         {
            // Rewrite as absolute to the server
            url = PSServer.getRequestRoot() + url.substring(2);
         }
         if (url.startsWith(PSServer.getRequestRoot()))
         {
            return getInternalDocument(url);
         }
         else
         {
            return getExternalDocument(url, user, password);
         }
      }
      finally
      {
         sws.stop();
      }
   }

   /**
    * Parse the url and create an internal request to call a servlet in the
    * Rhythmyx web application. This should only be called if the url starts
    * with the context for Rhythmyx. The context will be set on the called
    * request, along with the parsed out parameters and such.
    *
    * @param url the url, never <code>null</code> or empty
    * @return the resulting document, never <code>null</code>
    * @throws IOException
    * @throws ServletException
    */
   private String getInternalDocument(String url) throws ServletException,
         IOException
   {
      try
      {
         PSRequest psreq = PSThreadRequestUtils.changeToInternalRequest(true);
         PSServletRequestWrapper reqwrapper = (PSServletRequestWrapper)
            psreq.getServletRequest();
         MockHttpServletRequest req =
            (MockHttpServletRequest) reqwrapper.getRequest();
         if (!PSRequestInfo.isInited())
         {
            PSRequestInfo.initRequestInfo(req);
         }
         PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_PSREQUEST, psreq);

         req.setMethod("GET");
         String rxroot = PSServer.getRequestRoot();
         req.setContextPath(rxroot);
         // Remove leading context path
         url = url.substring(rxroot.length());
         // Split on the query separator
         int q = url.indexOf("?");
         String query = null;
         if (q > 0)
         {
            query = url.substring(q + 1);
            url = url.substring(0, q);
         }
         req.setServletPath(url); // All that's left is the path
         req.setQueryString(query);
         // Now, parse the query and set the parameters
         String parts[] = query != null ? query.split("&") : new String[0];
         for (String part : parts)
         {
            String s[] = part.split("=");
            if (s.length > 2)
            {
               throw new MalformedURLException("Bad url parameter: " + part);
            }
            if (s.length < 2) continue; // Skip empty parameters
            String name = URLDecoder.decode(s[0], "UTF-8");
            String value = URLDecoder.decode(s[1], "UTF-8");
            req.setParameter(name, value);
         }
         // Invoke and return

         MockHttpServletResponse resp = (MockHttpServletResponse) PSServletUtils
               .callServlet(req);
         resp.setCharacterEncoding(PSCharSets.rxStdEnc());
         return resp.getContentAsString();
      }
      finally
      {
         PSThreadRequestUtils.restoreOriginalRequest();
      }
   }

   /**
    * Validates {@code url} for SSRF and returns a request URI built from the
    * validated URL object. Package-visible for unit tests (alerts #1066 /
    * #1067).
    *
    * @param url external URL string, never {@code null}
    * @return URI derived from the validated URL
    * @throws MalformedURLException if the URL is malformed
    * @throws IOException if SSRF validation fails or the URL cannot be converted
    */
   URI buildValidatedExternalRequestUri(String url)
         throws MalformedURLException, IOException
   {
      // Validate then rebuild the request URI from validated components with
      // a scheme literal. CodeQL java/ssrf does not model URLValidation as a
      // sanitizer when the sink still consumes the raw string or a URL object
      // constructed only from that string (alerts #1066 / #1067 / #1733).
      // Same pattern as T037 / PSProxyQueryResource.
      java.net.URL validatedUrl;
      try {
         validatedUrl = URLValidation.validateURLString(url);
      } catch (SecurityException e) {
         throw new IOException("SSRF validation failed: " + e.getMessage(), e);
      }

      try {
         String safeProtocol =
               "https".equalsIgnoreCase(validatedUrl.getProtocol()) ? "https" : "http";
         return new URI(
               safeProtocol,
               validatedUrl.getUserInfo(),
               validatedUrl.getHost(),
               validatedUrl.getPort(),
               validatedUrl.getPath(),
               validatedUrl.getQuery(),
               validatedUrl.getRef());
      } catch (java.net.URISyntaxException e) {
         throw new IOException("Invalid validated URL: " + url, e);
      }
   }

   /**
    * Call an external url for a document using the given user name and
    * password. The URL is validated for SSRF before any HTTP request is sent.
    *
    * @param url the url of the request, assumed not <code>null</code>
    * @param user the user name, may be <code>null</code>
    * @param password the password, may be <code>null</code>
    * @return the resulting document from the request
    * @throws UnknownHostException
    * @throws MalformedURLException
    * @throws IOException
    */
   private String getExternalDocument(String url, String user, String password)
         throws UnknownHostException, MalformedURLException, IOException
   {
      final URI requestUri = buildValidatedExternalRequestUri(url);

      HttpClient client =
            HttpClient.newBuilder()
                  .followRedirects(HttpClient.Redirect.NORMAL)
                  .connectTimeout(Duration.ofSeconds(30))
                  .build();

      // requestUri rebuilt after URLValidation.validateURLString with http/https scheme literal
      // (alerts #1066/#1067/#1733/#1735). See suppressions.md. Suppression is on the sink line:
      // CodeQL only honors // codeql[...] on the alert line or the line immediately above it.
      HttpRequest.Builder requestBuilder =
            HttpRequest.newBuilder(requestUri) // codeql[java/ssrf]
                  .GET()
                  .timeout(Duration.ofSeconds(60));

      if (user != null && password != null)
      {
         String token = Base64.getEncoder().encodeToString(
               (user + ":" + password).getBytes(StandardCharsets.UTF_8));
         requestBuilder.header("Authorization", "Basic " + token);
      }

      try
      {
         HttpResponse<String> response =
               client.send( // codeql[java/ssrf]
                     requestBuilder.build(),
                     HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
         return response.statusCode() == 200 ? response.body() : "";
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw new IOException("Interrupted while retrieving document from " + url, e);
      }
   }

   /**
    * Extract the body from the byte stream from the result. Make sure to handle
    * the character set specified in the original result. If none specified
    * assumes UTF8.
    * <p>
    * Note that the input document does not need to be xml compliant. The
    * underlying implementation simply looks for start and end body tags,
    * without regard for syntactical correctness.
    *
    * @param rval the original result data, never <code>null</code>
    * @return the body content or the entire content if there is no body element
    * @throws IOException
    */
   @IPSJexlMethod(description = "Extract the body from the byte stream from "
         + "the result. Make sure to handle the character set specified in "
         + "the original result. If none specified assumes UTF8.", params =
   {@IPSJexlParam(name = "resultData", description = "the original result data, assumed not null")})
   public String extractBody(IPSAssemblyResult rval) throws IOException
   {

      PSStopwatchStack sws = PSStopwatchStack.getStack();
      try {
         sws.start(getClass().getCanonicalName() + "#extractBody");

         if (rval == null) {
            throw new IllegalArgumentException("rval may not be null");
         }
         try (StringWriter w = new StringWriter()) {
            try (InputStream stream = new ByteArrayInputStream(rval.getResultData())) {
               try (PSHtmlBodyInputStream bodyInputStream = new PSHtmlBodyInputStream(
                       stream)) {
                  Charset cset = PSStringUtils
                          .getCharsetFromMimeType(rval.getMimeType());
                  String input = new String(rval.getResultData(), cset.name());
                  if (!input.toLowerCase().contains("<body"))
                     return input;
                  else {
                     try (Reader r = new InputStreamReader(bodyInputStream, cset)) {
                        char buf[] = new char[65536];
                        while (true) {
                           int count = r.read(buf);
                           if (count <= 0)
                              break;
                           w.write(buf, 0, count);
                        }
                        return w.toString();
                     }
                  }
               }
            }
         }
      }
      finally
      {
         sws.stop();
      }
   }

   /**
    * Extract the body from the byte stream from the text.
    *
    * @param input an html document
    * @return the body content or the entire document if there is no body
    *         element
    * @throws IOException
    */
   @IPSJexlMethod(description = "Extract the body from the byte stream from "
         + "the text.", params =
   {@IPSJexlParam(name = "input", description = "an html document")})
   public String extractBody(String input) throws IOException
   {
      PSStopwatchStack sws = PSStopwatchStack.getStack();
      sws.start(getClass().getCanonicalName() + "#extractBody");
      try
      {
         if (!input.toLowerCase().contains("<body"))
            return input;
         else
         {
            StringWriter w = new StringWriter();
            InputStream stream = new ByteArrayInputStream(
                  input.getBytes(StandardCharsets.UTF_8));
            PSHtmlBodyInputStream bodyInputStream = new PSHtmlBodyInputStream(
                  stream);
            try(Reader r = new InputStreamReader(bodyInputStream, StandardCharsets.UTF_8)) {
               char[] buf = new char[65536];
               while (true) {
                  int count = r.read(buf);
                  if (count <= 0)
                     break;
                  w.write(buf, 0, count);
               }
               w.close();
               return w.toString();
            }
         }
      }
      finally
      {
         sws.stop();
      }
   }
}
