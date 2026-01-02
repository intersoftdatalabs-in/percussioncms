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
package com.percussion.pso.syndication;

import com.percussion.pso.utils.HTTPProxyClientConfig;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndPerson;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

<<<<<<< HEAD
/***
 * Serves as a Velocity friendly proxy class for the underlying syndication
 * libraries.
 *
 *
 */
=======
/** * Serves as a Velocity friendly proxy class for the underlying syndication libraries. */
>>>>>>> development-8.1.x
public class PSSynFeedProxy {

  private static final Logger log = LogManager.getLogger(PSSynFeedProxy.class);
  private static final String HTTP_IFMODIFIED = "If-Modified-Since";
  private static final String HTTP_IFNONEMATCH = "If-None-Match";

  private SyndFeed feed;

<<<<<<< HEAD
  /***
   * Returns the name of the first feed author in the collection of authors.
=======
  /**
   * * Returns the name of the first feed author in the collection of authors.
>>>>>>> development-8.1.x
   *
   * @return
   */
  public String getAuthor() {
    return feed.getAuthor();
  }

<<<<<<< HEAD
  /***
   *  Returns the feed authors.
=======
  /**
   * * Returns the feed authors.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public List<SyndPerson> getAuthorList() {
    return feed.getAuthors();
  }

<<<<<<< HEAD
  /***
   * Convenience method that returns the list of Authors as a comma separated string.
=======
  /**
   * * Convenience method that returns the list of Authors as a comma separated string.
   *
>>>>>>> development-8.1.x
   * @return
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

<<<<<<< HEAD
  /***
   * Returns the feed categories.
=======
  /**
   * * Returns the feed categories.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public List getCategoriesList() {
    return feed.getCategories();
  }

<<<<<<< HEAD
  /***
   * Returns the feed categories as a comma separated string.
=======
  /**
   * * Returns the feed categories as a comma separated string.
   *
>>>>>>> development-8.1.x
   * @return
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

<<<<<<< HEAD
  /***
   * the feed author.
=======
  /**
   * * the feed author.
   *
>>>>>>> development-8.1.x
   * @return
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

  public List getContributorsList() {
    return feed.getContributors();
  }

<<<<<<< HEAD
  /***
   * Returns the feed copyright.
=======
  /**
   * * Returns the feed copyright.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getCopyright() {
    return feed.getCopyright();
  }

<<<<<<< HEAD
  /***
   * Returns the feed description.
=======
  /**
   * * Returns the feed description.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getDescription() {
    // @TODO: Add Ext Description support.
    return feed.getDescription();
  }

<<<<<<< HEAD
  /***
   * Returns the charset encoding of a the feed.
=======
  /**
   * * Returns the charset encoding of a the feed.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getEncoding() {
    return feed.getEncoding();
  }

<<<<<<< HEAD
  /***
   * Returns the feed entries.
=======
  /**
   * * Returns the feed entries.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public List<PSSynFeedEntry> getEntries() {

    ArrayList<PSSynFeedEntry> ret = new ArrayList<PSSynFeedEntry>();

    for (int i = 0; i < feed.getEntries().size(); i++) {
      ret.add(new PSSynFeedEntry((SyndEntry) feed.getEntries().get(i)));
    }
    return ret;
  }

<<<<<<< HEAD
  /***
   * Returns the wire feed type the feed had/will-have when coverted from/to a WireFeed.
=======
  /**
   * * Returns the wire feed type the feed had/will-have when coverted from/to a WireFeed.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getFeedType() {
    return feed.getFeedType();
  }

<<<<<<< HEAD
  /***
   * Returns the feed image.
=======
  /**
   * * Returns the feed image.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public PSSynFeedImage getImage() {
    return new PSSynFeedImage(feed.getImage());
  }

<<<<<<< HEAD
  /***
   * Returns the feed language.
   * w   * @return
   */
=======
  /** * Returns the feed language. w * @return */
>>>>>>> development-8.1.x
  public String getLanguage() {
    return feed.getLanguage();
  }

<<<<<<< HEAD
  /***
   * Returns the feed link.
=======
  /**
   * * Returns the feed link.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getLink() {
    return feed.getLink();
  }

<<<<<<< HEAD
  /***
   * Returns the entry links
=======
  /**
   * * Returns the entry links
   *
>>>>>>> development-8.1.x
   * @return
   */
  public List<String> getLinks() {
    ArrayList<String> ret = new ArrayList<String>();

    for (int i = 0; i < feed.getLinks().size(); i++) {
      ret.add(feed.getLinks().get(i).getHref());
    }
    return ret;
  }

<<<<<<< HEAD
  /***
   * Returns the feed published date.
=======
  /**
   * * Returns the feed published date.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public Date getPublishedDate() {
    return feed.getPublishedDate();
  }

<<<<<<< HEAD
  /***
   * Returns the feed title.
=======
  /**
   * * Returns the feed title.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getTitle() {
    // @TODO: Add support for title EX.
    return feed.getTitle();
  }

<<<<<<< HEAD
  /***
   * Returns the feed URI.
=======
  /**
   * * Returns the feed URI.
   *
>>>>>>> development-8.1.x
   * @return
   */
  public String getUri() {
    return feed.getUri();
  }

<<<<<<< HEAD
  /***
   * Initializes this instances of the proxy with the specified feed url.
=======
  /**
   * * Initializes this instances of the proxy with the specified feed url.
>>>>>>> development-8.1.x
   *
   * @param urlString
   * @throws HttpException
   * @throws IOException
   * @throws IllegalArgumentException
   */
  public PSSynFeedProxy(String urlString)
      throws HttpException, IOException, IllegalArgumentException, FeedException, FeedException {
    HttpClient client = new HttpClient();

    // Set up the proxy server if there is one.
    HTTPProxyClientConfig proxy = new HTTPProxyClientConfig();

    if (!proxy.getProxyServer().equals("")) {
      client
          .getHostConfiguration()
          .setProxy(proxy.getProxyServer(), Integer.parseInt(proxy.getProxyPort()));
    }

    HttpMethod get = new GetMethod(urlString);

    try {
      int code = client.executeMethod(get);

      SyndFeedInput input = new SyndFeedInput();
      log.debug("Requesting feed from " + urlString);
      SyndFeed f = input.build(new XmlReader(get.getResponseBodyAsStream()));
      this.feed = f;
    } finally {
      get.releaseConnection();
    }
  }

  public PSSynFeedProxy(String urlString, String eTag, String lastModified)
      throws IllegalArgumentException, FeedException, IOException {
    HttpClient client = new HttpClient();

    // Set up the proxy server if there is one.
    HTTPProxyClientConfig proxy = new HTTPProxyClientConfig();

    if (!proxy.getProxyServer().equals("")) {
      log.debug("Setting Proxy server to " + proxy.getProxyServer() + ":" + proxy.getProxyPort());
      client
          .getHostConfiguration()
          .setProxy(proxy.getProxyServer(), Integer.parseInt(proxy.getProxyPort()));
    }
    client.getParams().setConnectionManagerTimeout(2000);

    GetMethod get = new GetMethod(urlString);

    try {
      // Add the modification check headers if we have valid params.
      if (eTag != null && !eTag.trim().equals("")) {
        get.addRequestHeader(HTTP_IFNONEMATCH, eTag);
      }

      if (lastModified != null && !lastModified.trim().equals("")) {
        get.addRequestHeader(HTTP_IFMODIFIED, lastModified);
      }

      get.setFollowRedirects(true);
      int code = client.executeMethod(get);

      if (code == HttpStatus.SC_NOT_MODIFIED) {
        log.debug("Feed URL not modified.");
      } else if (code == HttpStatus.SC_OK) {
        SyndFeedInput input = new SyndFeedInput();
        log.debug("Requesting feed from " + urlString);
        this.feed = input.build(new XmlReader(get.getResponseBodyAsStream()));

        // @TODO: Add logic into this section to persist the lastmodified header.

      } else {
        log.debug("Unexpected response from server for url" + urlString + " Response Code:" + code);
      }
    } finally {
      get.releaseConnection();
    }
  }
}
