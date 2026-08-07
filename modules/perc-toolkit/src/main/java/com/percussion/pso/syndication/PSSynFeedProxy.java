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
package com.percussion.pso.syndication;

import com.percussion.pso.utils.HTTPProxyClientConfig;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndPerson;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/***
 * Serves as a Velocity friendly proxy class for the underlying syndication
 * libraries.
 *
 *
 */
public class PSSynFeedProxy {

  private static final Logger log = LogManager.getLogger(PSSynFeedProxy.class);
  private static final String HTTP_IFMODIFIED = "If-Modified-Since";
  private static final String HTTP_IFNONEMATCH = "If-None-Match";

  private SyndFeed feed;

  /***
   * Returns the name of the first feed author in the collection of authors.
   *
   *
   */
  public String getAuthor() {
    return feed.getAuthor();
  }

  /***
   *  Returns the feed authors.
   *
   */
  public List<SyndPerson> getAuthorList() {
    return feed.getAuthors();
  }

  /***
   * Convenience method that returns the list of Authors as a comma separated string.
   *
   */
  public String getAuthors() {
    String ret = "";
    Object a;

    // @TODO: Add Atom Support
    for (int i = 0; i < feed.getAuthors().size(); i++) {
      a = feed.getAuthors().get(i);
      if (!(a instanceof SyndPerson)) {
        if (ret == "") ret = a.toString();
        else ret.concat("," + a.toString());
      }
    }
    return ret;
  }

  /***
   * Returns the feed categories.
   *
   */
  public List<?> getCategoriesList() {
    return feed.getCategories();
  }

  /***
   * Returns the feed categories as a comma separated string.
   *
   */
  public String getCategories() {
    String ret = "";
    Object a;

    for (int i = 0; i < feed.getCategories().size(); i++) {
      a = feed.getCategories().get(i);
      if (ret == "") ret = (String) a;
      else ret.concat("," + (String) a);
    }

    return ret;
  }

  /***
   * the feed author.
   *
   */
  public String getContributors() {
    String ret = "";
    Object a;

    // @TODO: Add Atom Support
    for (int i = 0; i < feed.getContributors().size(); i++) {
      a = feed.getContributors().get(i);
      if (!(a instanceof SyndPerson)) {
        if (ret == "") ret = a.toString();
        else ret.concat("," + a.toString());
      }
    }
    return ret;
  }

  public List<SyndPerson> getContributorsList() {
    return feed.getContributors();
  }

  /***
   * Returns the feed copyright.
   *
   */
  public String getCopyright() {
    return feed.getCopyright();
  }

  /***
   * Returns the feed description.
   *
   */
  public String getDescription() {
    // @TODO: Add Ext Description support.
    return feed.getDescription();
  }

  /***
   * Returns the charset encoding of a the feed.
   *
   */
  public String getEncoding() {
    return feed.getEncoding();
  }

  /***
   * Returns the feed entries.
   *
   */
  public List<PSSynFeedEntry> getEntries() {

    ArrayList<PSSynFeedEntry> ret = new ArrayList<PSSynFeedEntry>();

    for (int i = 0; i < feed.getEntries().size(); i++) {
      ret.add(new PSSynFeedEntry(feed.getEntries().get(i)));
    }
    return ret;
  }

  /***
   * Returns the wire feed type the feed had/will-have when coverted from/to a WireFeed.
   *
   */
  public String getFeedType() {
    return feed.getFeedType();
  }

  /***
   * Returns the feed image.
   *
   */
  public PSSynFeedImage getImage() {
    return new PSSynFeedImage(feed.getImage());
  }

  /***
   * Returns the feed language.
   * w   * @return
   */
  public String getLanguage() {
    return feed.getLanguage();
  }

  /***
   * Returns the feed link.
   *
   */
  public String getLink() {
    return feed.getLink();
  }

  /***
   * Returns the entry links
   *
   */
  public List<String> getLinks() {
    ArrayList<String> ret = new ArrayList<String>();

    for (int i = 0; i < feed.getLinks().size(); i++) {
      ret.add(feed.getLinks().get(i).getHref());
    }
    return ret;
  }

  /***
   * Returns the feed published date.
   *
   */
  public Date getPublishedDate() {
    return feed.getPublishedDate();
  }

  /***
   * Returns the feed title.
   *
   */
  public String getTitle() {
    // @TODO: Add support for title EX.
    return feed.getTitle();
  }

  /***
   * Returns the feed URI.
   *
   */
  public String getUri() {
    return feed.getUri();
  }

  /***
   * Initializes this instances of the proxy with the specified feed url.
   *
   * @param urlString
   * @throws IOException
   * @throws IllegalArgumentException
   */
  public PSSynFeedProxy(String urlString)
      throws IOException, IllegalArgumentException, FeedException {

    // Set up the proxy server if there is one.
    HTTPProxyClientConfig proxy = new HTTPProxyClientConfig();
    HttpClient client = buildHttpClient(proxy, Duration.ofSeconds(30));

    HttpRequest request = HttpRequest.newBuilder(URI.create(urlString)).GET().build();

    try {
      HttpResponse<InputStream> response =
          client.send(request, HttpResponse.BodyHandlers.ofInputStream());

      SyndFeedInput input = new SyndFeedInput();
      log.debug("Requesting feed from {} with status {}", urlString, response.statusCode());
      try (InputStream responseBody = response.body()) {
        this.feed = input.build(new XmlReader(responseBody));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while requesting feed from " + urlString, e);
    }
  }

  public PSSynFeedProxy(String urlString, String eTag, String lastModified)
      throws IllegalArgumentException, FeedException, IOException {
    // Set up the proxy server if there is one.
    HTTPProxyClientConfig proxy = new HTTPProxyClientConfig();
    HttpClient client = buildHttpClient(proxy, Duration.ofMillis(2000));

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder(URI.create(urlString)).timeout(Duration.ofMillis(2000)).GET();

    // Add the modification check headers if we have valid params.
    if (eTag != null && !eTag.trim().equals("")) {
      requestBuilder.header(HTTP_IFNONEMATCH, eTag);
    }

    if (lastModified != null && !lastModified.trim().equals("")) {
      requestBuilder.header(HTTP_IFMODIFIED, lastModified);
    }

    try {
      HttpResponse<InputStream> response =
          client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
      int code = response.statusCode();

      if (code == 304) {
        log.debug("Feed URL not modified.");
      } else if (code == 200) {
        SyndFeedInput input = new SyndFeedInput();
        log.debug("Requesting feed from {}", urlString);
        try (InputStream responseBody = response.body()) {
          this.feed = input.build(new XmlReader(responseBody));
        }

        // @TODO: Add logic into this section to persist the lastmodified header.

      } else {
        log.debug("Unexpected response from server for url" + urlString + " Response Code:" + code);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while requesting feed from " + urlString, e);
    }
  }

  private HttpClient buildHttpClient(HTTPProxyClientConfig proxy, Duration connectTimeout) {
    HttpClient.Builder builder =
        HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .followRedirects(HttpClient.Redirect.NORMAL);

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
}
