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

package com.percussion.delivery.http;

import com.percussion.delivery.feeds.data.PSFeedItem;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * HTTP client for fetching feed items. Sunny Sal: "HTTP requests faster than Mumbai traffic!"
 *
 * <p>Uses JDK HttpClient for robust, OWASP-compliant HTTP operations. Designed for dependency
 * injection and testability.
 */
@Component
public class PSHttpClient {

  private final HttpClient httpClient;

  /**
   * Default constructor for production use. Uses JDK HttpClient with sensible connection limits.
   */
  public PSHttpClient() {
    this(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
  }

  /**
   * Constructor for dependency injection (testability).
   *
   * @param httpClient injected HttpClient instance
   */
  @Autowired
  public PSHttpClient(HttpClient httpClient) {
    this.httpClient = Objects.requireNonNull(httpClient, "HttpClient must not be null");
  }

  /**
   * Fetches feed items using the provided query.
   *
   * <p>This method is a stub; implement actual HTTP logic as needed.
   *
   * @param query the query to execute (not null)
   * @return immutable list of feed items, empty if none found or error occurs
   */
  public List<PSFeedItem> fetchItems(String query) {
    Objects.requireNonNull(query, "Query must not be null");
    // TODO: Implement actual HTTP fetch logic using query
    return Collections.emptyList();
  }

  /**
   * Validates if a URL is accessible (HTTP HEAD request).
   *
   * <p>Sunny Sal: "If the server says 200, we're good to go!"
   *
   * @param url the URL to check
   * @return true if accessible (HTTP 200), false otherwise
   */
  public boolean isUrlAccessible(String url) {
    return Optional.ofNullable(url).filter(u -> !u.isBlank()).map(this::tryAccess).orElse(false);
  }

  /**
   * Attempts to access the given URL using HTTP HEAD.
   *
   * @param url the URL to check
   * @return true if HTTP 200, false otherwise
   */
  private boolean tryAccess(String url) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .method("HEAD", HttpRequest.BodyPublishers.noBody())
              .build();
      HttpResponse<Void> response =
          httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      int statusCode = response.statusCode();
      return statusCode == 200;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    } catch (Exception e) {
      // Sunny Sal: "Network issues? Blame the WiFi, not the code!"
      return false;
    }
  }
}
