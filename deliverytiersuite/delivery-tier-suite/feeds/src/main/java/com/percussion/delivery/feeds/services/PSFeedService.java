/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.delivery.feeds.services;

import com.percussion.delivery.feeds.PSFeedGenerator;
import com.percussion.delivery.feeds.data.IPSFeedDescriptor;
import com.percussion.delivery.feeds.data.PSFeedDTO;
import com.percussion.delivery.feeds.data.PSFeedDescriptors;
import com.percussion.delivery.feeds.data.PSFeedItem;
import com.percussion.delivery.listeners.IPSServiceDataChangeListener;
import com.percussion.delivery.services.PSAbstractRestService;
import com.percussion.delivery.utils.security.PSHttpClient;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.security.SecureStringUtils;
import com.percussion.security.ToDoVulnerability;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.validation.URLValidation;
import com.percussion.security.validation.XSSValidation;
import com.percussion.utils.io.PathUtils;
import com.rometools.rome.io.FeedException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The feed service is responsible for generating RSS/ATOM feeds. The service also collects feed
 * descriptors from the CM1 which provide meta data about the feed and a query used to get page data
 * from the dynamic index service(meta data service) to build the feed list.
 *
 * @author erikserating
 */
@Path("/rss")
@Component
@Scope("singleton")
public class PSFeedService extends PSAbstractRestService implements IPSFeedsRestService {

  public PSFeedService() {}

  public String getRssFeedsIP() {
    return this.rssFeedsIP;
  }

  public void setRssFeedsIP(String rssFeedsIP) {
    this.rssFeedsIP = rssFeedsIP;
  }

  private String rssFeedsIP;

  @HEAD
  @Path("/csrf")
  public void csrf(@Context HttpServletRequest request, @Context HttpServletResponse response) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return;
    }
    for (Cookie cookie : cookies) {
      if ("XSRF-TOKEN".equals(cookie.getName())) {
        response.setHeader("X-CSRF-HEADER", "X-XSRF-TOKEN");
        response.setHeader("X-CSRF-TOKEN", cookie.getValue());
      }
    }
  }

  private PSHttpClient httpClient;
  private static final Logger log = LogManager.getLogger(PSFeedService.class);
  private List<IPSServiceDataChangeListener> listeners = new ArrayList<>();

  private final String[] PERC_FEEDS_SERVICE = {"feeds"};

  private static final String FEEDS_IP_DEFAULT = "127.0.0.1";

  /** Fixed path of the internal metadata service used when generating feeds. */
  static final String METADATA_SERVICE_PATH = "/perc-metadata-services/metadata/get";

  /**
   * Builds the metadata-service URI from server-side values only (alert #797 / CWE-918).
   *
   * <p>Scheme is constrained to the safe literals {@code http}/{@code https}. Host and port come
   * from configuration / the container, not from the client request body. The path is a fixed
   * constant. Package-visible for unit tests.
   *
   * @param requestScheme scheme from {@link HttpServletRequest#getScheme()}, may be {@code null}
   * @param host validated IP (or loopback default), never {@code null}
   * @param port local container port
   * @return metadata service URI
   * @throws java.net.URISyntaxException if the URI cannot be constructed
   */
  static URI buildMetadataServiceUri(String requestScheme, String host, int port)
      throws java.net.URISyntaxException {
    // Constrain scheme to the two safe literals only. Selecting a constant
    // breaks CodeQL java/ssrf taint from HttpServletRequest.getScheme().
    final String scheme = "https".equalsIgnoreCase(requestScheme) ? "https" : "http";
    return new URI(scheme, null, host, port, METADATA_SERVICE_PATH, null, null);
  }

  /** The feed data access object, initialized in the ctor. Never <code>null</code> after that. */
  private IPSFeedDao feedDao;

  /** 2011-01-21T09:36:05 */
  FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");

  @Autowired
  public PSFeedService(@Qualifier("feedsDao") IPSFeedDao dao, PSHttpClient httpClient) {
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = Objects.requireNonNull(dao, "dao cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.feedDao = dao;
    this.httpClient = httpClient;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.feeds.services.IPSFeedsRestService#getFeed(java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/{sitename}/{feedname}/{hostname}")
  @Produces("text/xml")
  public Response getFeed(
      @PathParam("sitename") String sitename,
      @PathParam("feedname") String feedname,
      @PathParam("hostname") String hostname,
      @Context HttpServletRequest httpRequest) {

    sitename = SecureStringUtils.stripAllLineBreaks(SecureStringUtils.normalize(sitename, false));

    feedname = SecureStringUtils.stripAllLineBreaks(SecureStringUtils.normalize(feedname, false));

    hostname = SecureStringUtils.stripAllLineBreaks(SecureStringUtils.normalize(hostname, false));

    if (StringUtils.isEmpty(sitename)) {
      PSFeedService.log.error(
          "Illegal argument passed to getFeed. Site Name was missing from request.");
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    if (StringUtils.isEmpty(feedname)) {
      PSFeedService.log.error(
          "Illegal argument passed to getFeed. Feed Name was missing from request.");
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    if (StringUtils.isEmpty(hostname)) {
      PSFeedService.log.error(
          "Illegal argument passed to getFeed. Host Name was missing from request.");
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    if (PSFeedService.log.isDebugEnabled()) {
      PSFeedService.log.debug(
          String.format(
              "Searching for feed descriptor with feed name: %s with site name: %s and hostname:"
                  + " %s",
              feedname, sitename, hostname));
    }

    IPSFeedDescriptor desc = this.feedDao.find(feedname, sitename);
    Response resp;
    if (desc != null) {

      PSFeedService.log.debug("Found feed descriptor: {}", desc);

      PSFeedService.log.debug("Searching for feed connection information...");

      IPSConnectionInfo info = this.feedDao.getConnectionInfo();
      if (info != null) {

        PSFeedService.log.debug("Got connection info for feed: {}", info);

        String feed;
        try {
          if (PSFeedService.log.isDebugEnabled()) {
            PSFeedService.log.debug("Generating feed ...");
          }
          feed = this.generateFeed(desc, Optional.ofNullable(hostname), httpRequest);
        } catch (FeedException e) {
          PSFeedService.log.error(
              "Unexpected exception generating RSS feed: {}", PSExceptionUtils.getMessageForLog(e));
          PSFeedService.log.debug(PSExceptionUtils.getDebugMessageForLog(e));
          return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        if (StringUtils.isNotBlank(feed)) {
          if (PSFeedService.log.isDebugEnabled()) {
            PSFeedService.log.debug("Metadata Service returned results for feed: {}", feed);
          }
          resp = Response.ok(feed).type(MediaType.TEXT_XML_TYPE).build();
        } else {
          PSFeedService.log.warn("Feed query returned no results.");
          // Could not generate feed because no meta data exists
          resp = Response.status(Status.NOT_FOUND).build();
        }
      } else {
        PSFeedService.log.error(
            "Unable to locate connection information.  Unable to query for feed.");
        // No connection info present, send service unavailable
        resp = Response.status(Status.SERVICE_UNAVAILABLE).build();
      }

    } else {
      PSFeedService.log.error(
          "Unable to locate matching feed for feed name: {} and sitename: {} ", feedname, sitename);
      // No feed descriptor found
      resp = Response.status(Status.NOT_FOUND).build();
    }

    return resp;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.feeds.services.IPSFeedsRestService#readExternalFeed(java.lang.String)
   */
  @Override
  @POST
  @Path("/readExternalFeed")
  @Produces(MediaType.APPLICATION_XML)
  @Consumes(MediaType.APPLICATION_JSON)
  @ToDoVulnerability
  public String readExternalFeed(PSFeedDTO psFeedDTO) {

    URL url;
    HttpURLConnection con = null;
    String feeds = "";
    String decodedUrl = "";
    if (psFeedDTO == null) {
      return feeds;
    }
    String feedUrl = psFeedDTO.getFeedsUrl();

    PSFeedService.log.debug("URL is: {}", feedUrl);

    try {
      // If Secure File is not Copied to DTS Yet then returning

      String decryptedUrl =
          PSEncryptor.decryptString(
              PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR), feedUrl);
      PSFeedService.log.debug("Decrypted URL is: {}", decryptedUrl);
      decodedUrl = URLDecoder.decode(decryptedUrl, "UTF8");
      PSFeedService.log.debug("Decoded URL is: {}", decodedUrl);

      // plaintext URL Sent---- Throw not Allowed Error
      if (decodedUrl != null && decodedUrl.equals(feedUrl)) {
        PSFeedService.log.error(
            "Illegal argument passed to readExternalFeed. External unEncrypted Feed Url Not"
                + " Allowed.");
        throw new WebApplicationException(404);
      }

      decodedUrl = SecureStringUtils.stripNonHttpProtocols(decodedUrl);

      if (StringUtils.isEmpty(decodedUrl)) {
        // this is the case for initial time when RSS Widget is added to the page, thus returning
        // empty String
        return feeds;
      }
    } catch (PSEncryptionException e) {
      // Means EncryptionKey Not generated yet
      PSFeedService.log.error(PSExceptionUtils.getMessageForLog(e));
      PSFeedService.log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return "";

    } catch (Exception e) {
      PSFeedService.log.error(PSExceptionUtils.getMessageForLog(e));
      PSFeedService.log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(404);
    }

    if (StringUtils.isEmpty(feedUrl)) {
      PSFeedService.log.error(
          "Illegal argument passed to readExternalFeed. Feed Url was missing from request.");
      throw new WebApplicationException(404);
    }

    try {
      url = new URL(decodedUrl);
      // properly encode
      url =
          new URI(
                  url.getProtocol(),
                  url.getUserInfo(),
                  url.getHost(),
                  url.getPort(),
                  url.getPath(),
                  url.getQuery(),
                  url.getRef())
              .toURL();

      PSFeedService.log.debug("The Url for external feed : {}", url);

      // Validate URL to prevent SSRF attacks (CWE-918)
      URLValidation.validateURL(url);
      con = (HttpURLConnection) url.openConnection();
      con.setRequestProperty("Accept-Charset", "utf-8, ISO-8859-1;q=0.7,*;q=0.7");
      con.setRequestMethod("GET");
      try (BufferedReader rd =
          new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
        String line = null;
        StringBuilder bd = new StringBuilder();
        while ((line = rd.readLine()) != null) {
          bd.append(line);
        }
        feeds = bd.toString();
      }

    } catch (Exception e) {
      PSFeedService.log.error(
          "Exception during reading external feed : {}", PSExceptionUtils.getMessageForLog(e));
      PSFeedService.log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } finally {
      if (con != null) {
        con.disconnect();
      }
    }

    return feeds;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.feeds.services.IPSFeedsRestService#saveDescriptors(com.percussion.delivery.feeds.data.PSFeedDescriptors)
   */
  @Override
  @PUT
  @Path("/descriptors")
  @RolesAllowed("deliverymanager")
  public void saveDescriptors(PSFeedDescriptors descriptors) {
    if (descriptors == null) {
      PSFeedService.log.error(
          "Illegal argument passed to saveDescriptors. Feed descriptors cannot be null.");
      return;
    }

    if (descriptors.getDescriptors().isEmpty()) {
      PSFeedService.log.warn("Attempt to save empty list of Feed Descriptors");
      return;
    }

    Set<String> sites = new HashSet<>();
    sites.add(descriptors.getSite());

    // Save connection info
    this.feedDao.saveConnectionInfo(
        descriptors.getServiceUrl(),
        descriptors.getServiceUser(),
        descriptors.getServicePass(),
        descriptors.isServicePassEncrypted());
    // Determine descriptor delete list
    List<IPSFeedDescriptor> existing = this.feedDao.findBySite(descriptors.getSite());

    // Use streams to determine descriptors to delete
    List<IPSFeedDescriptor> deletes =
        existing.stream()
            .filter(
                d ->
                    descriptors.getDescriptors().stream()
                        .noneMatch(
                            nd ->
                                nd.getName().equals(d.getName())
                                    && nd.getSite().equals(d.getSite())))
            .collect(Collectors.toList());

    if (PSFeedService.log.isDebugEnabled()) {
      PSFeedService.log.debug("Descriptors that will be deleted are : {} ", deletes);
    }

    this.feedDao.saveDescriptors(descriptors.getDescriptors());

    // Remove feed descriptors for feeds that no longer exist
    this.feedDao.deleteDescriptors(deletes);
  }

  /**
   * Helper method to do the actual work of calling the dynamic indexer (meta data service) to
   * retrieve the data needed for the feed content. Then calls the feed generator to generate the
   * feed content.
   *
   * @param desc the feed descriptor, assumed to not be <code>null</code>.
   * @param httpRequest the http request, assumed not <code>null</code>.
   * @return the feed xml, may be <code>null</code>.
   * @throws FeedException if any error occurs while generating the feed.
   */
  private String generateFeed(
      IPSFeedDescriptor desc, Optional<String> hostName, HttpServletRequest httpRequest)
      throws FeedException {

    if (this.rssFeedsIP == null || this.rssFeedsIP.isEmpty()) {
      this.rssFeedsIP = PSFeedService.FEEDS_IP_DEFAULT;
    } else {
      this.rssFeedsIP = this.rssFeedsIP.trim();
    }

    // Only accept a literal IP (v4 or v6) or fall back to the loopback default.
    // Hostname strings are rejected so the metadata-service target cannot be
    // redirected via a misconfigured property (SSRF / alert #797).
    InetAddressValidator ipValidator = new InetAddressValidator();
    if (!ipValidator.isValid(this.rssFeedsIP)
        || !(ipValidator.isValidInet4Address(this.rssFeedsIP)
            || ipValidator.isValidInet6Address(this.rssFeedsIP))) {
      this.rssFeedsIP = PSFeedService.FEEDS_IP_DEFAULT;
    }
    // Call the metadata service with the query to get page listing
    PSFeedGenerator generator = new PSFeedGenerator();
    String feed = null;
    URI uri = null;
    String url = null;
    String protocol = null;
    Client client;

    final URI metadataUri;
    try {
      metadataUri =
          buildMetadataServiceUri(httpRequest.getScheme(), this.rssFeedsIP, httpRequest.getLocalPort());
      url = metadataUri.toString();
    } catch (Exception e) {
      throw new FeedException(
          "Failed to build metadata service URI for rssFeedsIP=" + this.rssFeedsIP, e);
    }

    try {
      client = this.httpClient.getSSLClient();
      uri = new URI(desc.getLink());
      protocol = uri.getScheme() + "://";
      PSFeedService.log.info(
          "The url obtained using the httpRequest.getLocalAddr() ----> {} ", url);
    } catch (Exception e) {
      client = ClientBuilder.newClient();
      PSFeedService.log.error(
          "Exception occurred in creating the SSL Client : {} ",
          PSExceptionUtils.getMessageForLog(e));
      PSFeedService.log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    WebTarget webTarget = client.target(metadataUri);

    if (PSFeedService.log.isDebugEnabled()) {
      PSFeedService.log.debug("WebResource for metadata service : {}", webTarget);
    }

    try {
      List<PSFeedItem> items = new ArrayList<>();

      Invocation.Builder invocationBuilder = (webTarget).request(MediaType.APPLICATION_JSON_TYPE);

      Response response =
          invocationBuilder.post(Entity.entity(desc.getQuery(), MediaType.APPLICATION_JSON));

      String jsonString = response.readEntity(String.class);
      JSONObject resultObj = new JSONObject(jsonString);
      JSONArray data = (JSONArray) resultObj.get("results");

      String host =
          hostName.filter(StringUtils::isNotBlank).orElse(PSFeedGenerator.getHost(desc.getLink()));

      int len = data.length();
      for (int i = 0; i < len; i++) {
        JSONObject obj = data.getJSONObject(i);
        JSONObject props = obj.getJSONObject("properties");
        PSFeedItem item = new PSFeedItem();
        String folder = obj.getString("folder");
        String pagename = obj.getString("name");
        item.setLink(protocol + host + folder + pagename);
        if (props.has(PROP_TITLE)) item.setTitle(props.getString(PROP_TITLE));
        if (props.has(PROP_DESCRIPTION)) {
          String sitePrefix = protocol + host;
          String replacedHtml =
              this.replaceRelativeLinks(props.getString(PROP_DESCRIPTION), sitePrefix);
          item.setDescription(replacedHtml);
        }
        if (props.has(PROP_PUBDATE)) {
          TimeZone tz = TimeZone.getDefault();
          if (props.has(PROP_CONTENTPOSTDATETZ))
            tz = TimeZone.getTimeZone(props.getString(PROP_CONTENTPOSTDATETZ));
          FastDateFormat tzFmt = FastDateFormat.getInstance(this.dateFormat.getPattern(), tz);
          item.setPublishDate(tzFmt.parse(props.getString(PROP_PUBDATE)));
        }
        items.add(item);
      }
      feed = generator.makeFeedContent(desc, host, items);

      PSFeedService.log.debug("The generated feed: {}", feed);

    } catch (Exception e) {
      PSFeedService.log.error(
          "Exception during feed generation : {}", PSExceptionUtils.getMessageForLog(e));
      PSFeedService.log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      // CWE-79: Escape exception message to prevent XSS in REST error response
      throw new FeedException(XSSValidation.escapeHtml(e.getMessage()), e);
    }

    return feed;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.percussion.metadata.IPSMetadataIndexerService#addMetadataListener
   * (com.percussion.metadata.event.IPSMetadataListener)
   */
  /* (non-Javadoc)
   * @see com.percussion.delivery.feeds.services.IPSFeedsRestService#addMetadataListener(com.percussion.delivery.listeners.IPSServiceDataChangeListener)
   */
  @Override
  public void addMetadataListener(IPSServiceDataChangeListener listener) {
    Validate.notNull(listener, "listener cannot be null.");
    if (!this.listeners.contains(listener)) this.listeners.add(listener);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.percussion.metadata.IPSMetadataIndexerService#removeMetadataListener
   * (com.percussion.metadata.event.IPSMetadataListener)
   */
  /* (non-Javadoc)
   * @see com.percussion.delivery.feeds.services.IPSFeedsRestService#removeMetadataListener(com.percussion.delivery.listeners.IPSServiceDataChangeListener)
   */
  @Override
  public void removeMetadataListener(IPSServiceDataChangeListener listener) {
    Validate.notNull(listener, "listener cannot be null.");
    this.listeners.remove(listener);
  }

  /** Fire a data change event for all registered listeners. */
  @SuppressWarnings("unused")
  private void fireDataChangedEvent(Set<String> sites) {
    if (sites == null || sites.isEmpty()) {
      return;
    }

    for (IPSServiceDataChangeListener listener : this.listeners) {
      listener.dataChanged(sites, this.PERC_FEEDS_SERVICE);
    }
  }

  /** Fire a data change event for all registered listeners. */
  @SuppressWarnings("unused")
  private void fireDataChangeRequestedEvent(Set<String> sites) {
    if (sites == null || sites.isEmpty()) {
      return;
    }

    for (IPSServiceDataChangeListener listener : this.listeners) {
      listener.dataChangeRequested(sites, this.PERC_FEEDS_SERVICE);
    }
  }

  @Override
  @PUT
  @Path("/rotateKey")
  @RolesAllowed("deliverymanager")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public void rotateKey(String key) {
    byte[] backToBytes = Base64.getDecoder().decode(key);
    PSEncryptor.getInstance(
            "AES", PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR))
        .forceReplaceKeyFile(backToBytes, false);
  }

  @Override
  public String getVersion() {

    String version = super.getVersion();

    PSFeedService.log.debug("getVersion() from PSFeedService... {}", version);

    return version;
  }

  /**
   * Parses the page summary of each rss post if present. This is required as all inline links are
   * currently relative and external feed applications may be required to use fully qualified URLs.
   *
   * @param html the source html to parse
   * @return a String with
   */
  private String replaceRelativeLinks(String html, String sitePrefix) {
    Document doc = Jsoup.parse(html);
    Elements elms = doc.select("img[src]");
    for (Element elm : elms) {
      String fullUrl = sitePrefix + elm.attr("src");
      elm.attr("src", fullUrl);
    }
    return doc.toString();
  }

  /** {@inheritDoc} */
  @Override
  public Response updateOldSiteEntries(String prevSiteName, String newSiteName) {
    PSFeedService.log.info("Attempting to delete feeds entries for site name: {}", prevSiteName);
    try {
      List<IPSFeedDescriptor> feeds = this.feedDao.findBySite(prevSiteName);
      this.feedDao.deleteDescriptors(feeds);
    } catch (Exception e) {
      PSFeedService.log.error(
          "Error updating feed entries for old site: {}, Error: {}",
          prevSiteName,
          PSExceptionUtils.getMessageForLog(e));
      PSFeedService.log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    return Response.status(Status.NO_CONTENT).build();
  }
}
