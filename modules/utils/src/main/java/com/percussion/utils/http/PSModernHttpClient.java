/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.utils.http;

import com.percussion.security.error.PSExceptionUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Modern HTTP client wrapper that uses java.net.http.HttpClient. This class provides a
 * migration-friendly interface for replacing Apache Commons HttpClient usage.
 */
public class PSModernHttpClient {

  private static final Logger log = LogManager.getLogger(PSModernHttpClient.class);

  private static final int HTTP_OK_RANGE_START = 200;
  private static final int HTTP_OK_RANGE_END = 300;

  private final HttpClient httpClient;
  private final Map<String, String> defaultHeaders;
  private final String baseUrl;
  private Duration timeout = Duration.ofSeconds(30);

  /**
   * Constructor with base URL
   *
   * @param baseUrl the base URL for requests
   */
  public PSModernHttpClient(String baseUrl) {
    this.baseUrl = baseUrl;
    this.defaultHeaders = new HashMap<>();
    this.defaultHeaders.put("Content-Type", "application/json");
    this.defaultHeaders.put("Accept", "application/json");
    this.httpClient = createHttpClient();
  }

  /**
   * Constructor with base URL and custom headers
   *
   * @param baseUrl the base URL for requests
   * @param headers default headers to include with requests
   */
  public PSModernHttpClient(String baseUrl, Map<String, String> headers) {
    this.baseUrl = baseUrl;
    this.defaultHeaders = new HashMap<>(headers);
    this.httpClient = createHttpClient();
  }

  /**
   * Performs HTTP GET request
   *
   * @param path the relative path
   * @return response body as String
   * @throws IOException if request fails
   */
  public String get(String path) throws IOException {
    return get(path, new HashMap<>());
  }

  /**
   * Performs HTTP GET request with query parameters
   *
   * @param path the relative path
   * @param params query parameters
   * @return response body as String
   * @throws IOException if request fails
   */
  public String get(String path, Map<String, String> params) throws IOException {
    String queryString = buildQueryString(params);
    String fullUrl = buildUrl(path) + (queryString.isEmpty() ? "" : "?" + queryString);

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder().uri(URI.create(fullUrl)).timeout(timeout).GET();

    // Add default headers
    for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
      requestBuilder.header(header.getKey(), header.getValue());
    }

    HttpRequest request = requestBuilder.build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      validateResponse(response, fullUrl);
      return response.body();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Request interrupted", e);
    }
  }

  /**
   * Performs HTTP GET request returning binary data
   *
   * @param path the relative path
   * @return response body as InputStream
   * @throws IOException if request fails
   */
  public InputStream getBinary(String path) throws IOException {
    String fullUrl = buildUrl(path);

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder().uri(URI.create(fullUrl)).timeout(timeout).GET();

    // Add default headers (excluding content-type for binary)
    for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
      if (!"Content-Type".equals(header.getKey())) {
        requestBuilder.header(header.getKey(), header.getValue());
      }
    }

    HttpRequest request = requestBuilder.build();

    try {
      HttpResponse<InputStream> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
      validateResponse(response, fullUrl);
      return response.body();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Request interrupted", e);
    }
  }

  /**
   * Performs HTTP POST request
   *
   * @param path the relative path
   * @param body request body
   * @return response body as String
   * @throws IOException if request fails
   */
  public String post(String path, String body) throws IOException {
    return post(path, body, "application/json");
  }

  /**
   * Performs HTTP POST request with custom content type
   *
   * @param path the relative path
   * @param body request body
   * @param contentType content type header
   * @return response body as String
   * @throws IOException if request fails
   */
  public String post(String path, String body, String contentType) throws IOException {
    String fullUrl = buildUrl(path);

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create(fullUrl))
            .timeout(timeout)
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

    // Set content type
    requestBuilder.header("Content-Type", contentType + "; charset=UTF-8");

    // Add other default headers (excluding content-type)
    for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
      if (!"Content-Type".equals(header.getKey())) {
        requestBuilder.header(header.getKey(), header.getValue());
      }
    }

    HttpRequest request = requestBuilder.build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      validateResponse(response, fullUrl);
      return response.body();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Request interrupted", e);
    }
  }

  /**
   * Performs HTTP POST request with form parameters
   *
   * @param path the relative path
   * @param params form parameters
   * @return response body as String
   * @throws IOException if request fails
   */
  public String postForm(String path, Map<String, String> params) throws IOException {
    String formData = buildFormData(params);
    return post(path, formData, "application/x-www-form-urlencoded");
  }

  /**
   * Performs HTTP PUT request
   *
   * @param path the relative path
   * @param body request body
   * @return response body as String
   * @throws IOException if request fails
   */
  public String put(String path, String body) throws IOException {
    return put(path, body, "application/json");
  }

  /**
   * Performs HTTP PUT request with custom content type
   *
   * @param path the relative path
   * @param body request body
   * @param contentType content type header
   * @return response body as String
   * @throws IOException if request fails
   */
  public String put(String path, String body, String contentType) throws IOException {
    String fullUrl = buildUrl(path);

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create(fullUrl))
            .timeout(timeout)
            .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

    // Set content type
    requestBuilder.header("Content-Type", contentType + "; charset=UTF-8");

    // Add other default headers (excluding content-type)
    for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
      if (!"Content-Type".equals(header.getKey())) {
        requestBuilder.header(header.getKey(), header.getValue());
      }
    }

    HttpRequest request = requestBuilder.build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      validateResponse(response, fullUrl);
      return response.body();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Request interrupted", e);
    }
  }

  /**
   * Performs HTTP DELETE request
   *
   * @param path the relative path
   * @return response body as String
   * @throws IOException if request fails
   */
  public String delete(String path) throws IOException {
    String fullUrl = buildUrl(path);

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder().uri(URI.create(fullUrl)).timeout(timeout).DELETE();

    // Add default headers
    for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
      requestBuilder.header(header.getKey(), header.getValue());
    }

    HttpRequest request = requestBuilder.build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      validateResponse(response, fullUrl);
      return response.body();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Request interrupted", e);
    }
  }

  /**
   * Sets the request timeout
   *
   * @param timeout timeout duration
   */
  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

  /**
   * Adds a default header for all requests
   *
   * @param name header name
   * @param value header value
   */
  public void addHeader(String name, String value) {
    defaultHeaders.put(name, value);
  }

  private HttpClient createHttpClient() {
    SSLContext sslContext = null;
    try {
      // Use the default SSL context for secure connections
      sslContext = SSLContext.getDefault();

      return HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(10))
          .sslContext(sslContext)
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();
    } catch (Exception e) {

      log.warn(
          "Failed to create SSL context, using the default SSL Context {}: {}",
          sslContext,
          PSExceptionUtils.getMessageForLog(e));
      return HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(10))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();
    }
  }

  private String buildUrl(String path) {
    if (path.startsWith("http://") || path.startsWith("https://")) {
      return path;
    }
    String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    String p = path.startsWith("/") ? path : "/" + path;
    return base + p;
  }

  private String buildQueryString(Map<String, String> params) {
    if (params.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : params.entrySet()) {
      if (!sb.isEmpty()) {
        sb.append("&");
      }
      sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
          .append("=")
          .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
    }
    return sb.toString();
  }

  private String buildFormData(Map<String, String> params) {
    return buildQueryString(params);
  }

  private void validateResponse(HttpResponse<?> response, String url) throws IOException {
    int statusCode = response.statusCode();
    if (log.isDebugEnabled()) {
      log.debug("HTTP {} {} Status: {}", response.request().method(), url, statusCode);
    }

    if (statusCode < HTTP_OK_RANGE_START || statusCode >= HTTP_OK_RANGE_END) {
      String errorMessage = String.format("HTTP Error %d for %s", statusCode, url);
      log.error(errorMessage);
      throw new IOException(errorMessage);
    }
  }
}
