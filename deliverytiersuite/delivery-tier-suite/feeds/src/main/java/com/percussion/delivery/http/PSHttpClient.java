// REFACTORED: CP-JAVA11
package com.percussion.delivery.http;

import com.percussion.delivery.feeds.data.PSFeedItem;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * HTTP client for fetching feed items.
 */
@Component
public class PSHttpClient {
    private final HttpClient httpClient;

    public PSHttpClient() {
        this.httpClient = HttpClients.custom()
            .setMaxConnTotal(50)
            .setMaxConnPerRoute(10)
            .build();
    }

    /**
     * Fetches feed items using the provided query.
     *
     * @param query the query to execute
     * @return list of feed items, empty list if none found or error occurs
     */
    public List<PSFeedItem> fetchItems(String query) {
        Objects.requireNonNull(query, "Query must not be null");
        // Implementation details would go here
        return Collections.emptyList();
    }

    /**
     * Validates if a URL is accessible.
     *
     * @param url the URL to check
     * @return true if accessible, false otherwise
     */
    public boolean isUrlAccessible(String url) {
        return Optional.ofNullable(url)
            .map(this::tryAccess)
            .orElse(false);
    }

    private boolean tryAccess(String url) {
        try {
            var response = httpClient.execute(org.apache.http.client.methods.RequestBuilder
                .head()
                .setUri(url)
                .build());
            return response.getStatusLine().getStatusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
