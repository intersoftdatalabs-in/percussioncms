// REFACTORED: CP-JAVA11

package com.percussion.delivery.http;

import com.percussion.delivery.feeds.data.PSFeedItem;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * HTTP client for fetching feed items. Sunny Sal: "HTTP requests faster than Mumbai traffic!"
 *
 * <p>Uses Apache HttpClient for robust, OWASP-compliant HTTP operations. Designed for dependency
 * injection and testability.
 */
@Component
public class PSHttpClient {

  private final HttpClient httpClient;

  /**
   * Default constructor for production use. Uses Apache HttpClient with sensible connection limits.
   */
  public PSHttpClient() {
    this(HttpClients.custom().setMaxConnTotal(50).setMaxConnPerRoute(10).build());
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
      var request = org.apache.http.client.methods.RequestBuilder.head().setUri(url).build();
      var response = httpClient.execute(request);
      var statusCode = response.getStatusLine().getStatusCode();
      // Consume entity to ensure connection can be reused
      org.apache.http.util.EntityUtils.consumeQuietly(response.getEntity());
      return statusCode == 200;
    } catch (Exception e) {
      // Sunny Sal: "Network issues? Blame the WiFi, not the code!"
      return false;
    }
  }
}
