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
// REFACTORED: CP-JAVA11
package com.percussion.feeds.service.impl;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.error.PSException;
import com.percussion.feeds.data.PSFeedInfo;
import com.percussion.feeds.error.PSFeedInfoServiceException;
import com.percussion.feeds.service.IPSFeedsInfoService;
import com.percussion.pagemanagement.service.IPSRenderService;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.IPSSiteItem;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.relationship.IPSRelationshipService;
import com.percussion.services.relationship.PSRelationshipServiceLocator;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.webservices.PSWebserviceUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service for managing feed information. Sunny Sal says: "FeedsInfoService, now Java 11 and
 * Google-styled!"
 */
public class PSFeedsInfoService implements IPSFeedsInfoService {

  private final IPSContentMgr contentMgr = PSContentMgrLocator.getContentMgr();
  private final IPSRelationshipService relService =
      PSRelationshipServiceLocator.getRelationshipService();
  private final PSItemDefManager iDefMgr = PSItemDefManager.getInstance();
  private final IPSPublisherService pubService = PSPublisherServiceLocator.getPublisherService();
  private final IPSRenderService renderService;
  private final PSFeedsInfoQueue queue;
  private final IPSDeliveryInfoService deliveryInfoService;
  private int contentTypePage = -1;
  private int contentTypeTemplate = -1;

  /**
   * Used to hold flags for each site that an empty descriptor list was already queued. We want to
   * avoid sending empty lists for no reason, but we need to do it at least once so that the feed
   * service removes feeds that no longer exist.
   */
  private final Set<Long> emptyFeedSetSent = new HashSet<>();

  public static final Logger log = LogManager.getLogger(PSFeedsInfoService.class);

  @Autowired
  public PSFeedsInfoService(
      IPSRenderService renderService,
      PSFeedsInfoQueue queue,
      IPSDeliveryInfoService deliveryInfoService) {
    this.renderService = renderService;
    this.queue = queue;
    this.deliveryInfoService = deliveryInfoService;
  }

  /** Initialize members for use by the service. */
  public void setContentTypeIds() {
    if (contentTypePage != -1) {
      return;
    }
    try {
      contentTypePage = (int) iDefMgr.contentTypeNameToId(CONTENT_TYPE_PAGE);
      contentTypeTemplate = (int) iDefMgr.contentTypeNameToId(CONTENT_TYPE_TEMPLATE);
    } catch (PSInvalidContentTypeException e) {
      log.error(e.getLocalizedMessage());
    }
  }

  @Override
  public Collection<PSFeedInfo> getFeeds(long serverId) throws PSFeedInfoServiceException {
    setContentTypeIds();
    var feedContentTypes = getFeedContentTypes();
    Collection<PSFeedInfo> feeds;
    try {
      feeds = getFeedEnabledContentItems(feedContentTypes);
      addParentItems(feeds);
      filterFeeds(feeds, serverId);
      addQueries(feeds);
    } catch (Exception e) {
      throw new PSFeedInfoServiceException(e);
    }
    return feeds;
  }

  @Override
  public void pushFeeds(IPSSite site, PSPubServer server) throws PSFeedInfoServiceException {
    Objects.requireNonNull(site, "site cannot be null");
    var feeds = getFeeds(server.getServerId());
    if (feeds.isEmpty() && emptyFeedSetSent.contains(site.getSiteId())) {
      log.info(
          "No feeds found to push to feeds service for site or server is selected none {}",
          site.getName());
      return;
    }
    if (server.getPublishServer() != null
        && server.getPublishServer().equalsIgnoreCase(IPSPubServerService.DEFAULT_DTS)) {
      log.info("server is selected none {}", site.getName());
      return;
    }
    try {
      var descriptors =
          createDescriptorsJson(site, feeds, server.getServerType(), server.getPublishServer());
      log.info("Queuing {} feeds for site {}", feeds.size(), site.getName());
      queue.queueDescriptors(site.getName(), descriptors, server.getServerType());
      if (feeds.isEmpty()) {
        emptyFeedSetSent.add(site.getSiteId());
      } else {
        emptyFeedSetSent.remove(site.getSiteId());
      }
    } catch (JSONException | IPSGenericDao.LoadException | IPSGenericDao.SaveException e) {
      throw new PSFeedInfoServiceException("Error occurred while trying to create descriptors.", e);
    }
  }

  /** Helper method to create the descriptors json object string to be sent to the feed service. */
  private String createDescriptorsJson(
      IPSSite site, Collection<PSFeedInfo> feeds, String serverType, String adminURL)
      throws JSONException, PSFeedInfoServiceException {
    var deliveryInfo =
        deliveryInfoService.findByService(PSDeliveryInfo.SERVICE_FEEDS, serverType, adminURL);
    if (deliveryInfo == null) {
      var error = "Failed to find delivery server info";
      log.error(error);
      throw new PSFeedInfoServiceException(error);
    }
    var deliveryUrl = deliveryInfo.getUrl();
    String host;
    try {
      var uri = new URI(deliveryUrl);
      host = uri.getScheme() + "://" + uri.getHost();
      var port = uri.getPort();
      if (port != -1) {
        host += ":" + port;
      }
    } catch (URISyntaxException e) {
      var error = "Failed to parse host from feed service url: " + deliveryUrl;
      log.error(error);
      throw new RuntimeException(error);
    }
    var obj = new JSONObject();
    var descriptors = new JSONArray();
    obj.put("site", site.getName());
    for (var feed : feeds) {
      var d = new JSONObject();
      d.put("name", feed.getName());
      d.put("site", site.getName());
      d.put("description", feed.getDesc());
      d.put("link", host + feed.getOwnerPageLocation());
      d.put("title", feed.getTitle());
      d.put("query", feed.getQuery());
      d.put("type", feed.getType());
      descriptors.put(d);
    }
    obj.put("descriptors", descriptors);
    return obj.toString();
  }

  /** Retrieves a collection of content types that use the feeds shared field group. */
  private Collection<String> getFeedContentTypes() {
    var cts = new ArrayList<String>();
    try {
      var results = iDefMgr.getContentTypesUsingSharedFieldGroup("rssfeeds");
      Collections.addAll(cts, results);
    } catch (PSInvalidContentTypeException e) {
      log.error(e.getLocalizedMessage());
    }
    return cts;
  }

  /** Retrieves all feed enabled content items as feed info objects. */
  private Collection<PSFeedInfo> getFeedEnabledContentItems(Collection<String> contentTypes)
      throws InvalidQueryException, RepositoryException {
    var feeds = new ArrayList<PSFeedInfo>();
    for (var ct : contentTypes) {
      var queryString =
          "select rx:sys_contentid, rx:feed_name, rx:feed_title, rx:feed_description from rx:"
              + ct
              + " where rx:enable_rss_feed='Enable Rss feed'";
      var query = contentMgr.createQuery(queryString, Query.SQL);
      var qresults = contentMgr.executeQuery(query, -1, null, null);
      var rows = qresults.getRows();
      while (rows.hasNext()) {
        var nrow = rows.nextRow();
        var vals = nrow.getValues();
        var feed =
            new PSFeedInfo(
                Integer.parseInt(vals[0].getString()),
                vals[1].getString(),
                vals[2].getString(),
                vals[3].getString());
        feeds.add(feed);
      }
    }
    return feeds;
  }

  /**
   * Locates parent pages and templates that contain the feed item as dependents. For templates we
   * find all pages that use the template. Modifies the feed info objects passed in.
   */
  private void addParentItems(Collection<PSFeedInfo> feeds)
      throws PSException, InvalidQueryException, RepositoryException {
    if (feeds.isEmpty()) {
      return;
    }
    var pFilter = new PSRelationshipFilter();
    var tFilter = new PSRelationshipFilter();
    for (var feed : feeds) {
      var loc = PSWebserviceUtils.getItemLocator(new PSLegacyGuid(feed.getId(), -1));
      pFilter.setDependent(loc);
      pFilter.setCategory(PSRelationshipFilter.FILTER_CATEGORY_ACTIVE_ASSEMBLY);
      pFilter.setOwnerContentTypeId(contentTypePage);
      for (var r : relService.findByFilter(pFilter)) {
        feed.getPages().add(r.getOwner().getId());
      }
      tFilter.setDependent(loc);
      tFilter.setCategory(PSRelationshipFilter.FILTER_CATEGORY_ACTIVE_ASSEMBLY);
      tFilter.setOwnerContentTypeId(contentTypeTemplate);
      for (var r : relService.findByFilter(tFilter)) {
        feed.getTemplates().add(r.getOwner().getId());
        // Locate pages that use templates
        var guid = PSGuidUtils.makeGuid(r.getOwner().getId(), PSTypeEnum.LEGACY_CONTENT);
        var queryString =
            "select rx:sys_contentid from rx:percPage where rx:templateid='"
                + guid.toString()
                + "'";
        var query = contentMgr.createQuery(queryString, Query.SQL);
        var qresults = contentMgr.executeQuery(query, -1, null, null);
        var rows = qresults.getRows();
        while (rows.hasNext()) {
          var nrow = rows.nextRow();
          var vals = nrow.getValues();
          feed.getPages().add(Integer.parseInt(vals[0].getString()));
        }
      }
    }
  }

  /**
   * Filters out any feed enabled items that are not within the specified site and that are not
   * currently published. Also adds site specific info to the feed info nodes for the earliest page
   * that contains the feed. Modifies the feed info objects passed in.
   */
  private void filterFeeds(Collection<PSFeedInfo> feeds, long serverId) {
    if (feeds.isEmpty()) {
      return;
    }
    var sItems = new HashMap<Integer, IPSSiteItem>();
    var sGuid = PSGuidUtils.makeGuid(serverId, PSTypeEnum.PUBLISHING_SERVER);
    var removeFeeds = new ArrayList<PSFeedInfo>();
    for (var si : pubService.findSiteItemsByPubServer(sGuid, DELIVERY_CONTEXT)) {
      sItems.put(si.getContentId(), si);
    }
    for (var feed : feeds) {
      var remove = new ArrayList<Integer>();
      Integer ownerPage = null;
      long pageDate = -1;
      for (var p : feed.getPages()) {
        if (!sItems.containsKey(p)) {
          remove.add(p);
        } else {
          var current = sItems.get(p).getDate().getTime();
          if (pageDate == -1 || current < pageDate) {
            pageDate = current;
            ownerPage = p;
          }
        }
      }
      // Remove pages not published
      for (var rmv : remove) {
        feed.getPages().remove(rmv);
      }
      // Use earliest published page as feed page parent and for site info
      if (ownerPage != null) {
        var oPage = sItems.get(ownerPage);
        feed.setOwnerPageId(oPage.getContentId());
        feed.setOwnerPageLocation(oPage.getLocation());
        feed.setOwnerFolderId(oPage.getFolderId());
      } else {
        removeFeeds.add(feed);
      }
    }
    // Remove feeds without pages
    for (var rmv : removeFeeds) {
      feeds.remove(rmv);
    }
  }

  /**
   * Gets the metadata query for the feed from the rendered page and adds it to the feed info
   * object.
   */
  private void addQueries(Collection<PSFeedInfo> feeds) {
    if (feeds.isEmpty()) {
      return;
    }
    var it = feeds.iterator();
    while (it.hasNext()) {
      String data = null;
      var feed = it.next();
      var pageId = feed.getOwnerPageId();
      var guid = PSGuidUtils.makeGuid(pageId, PSTypeEnum.LEGACY_CONTENT);
      // Render the page, the query will be created and put in an element
      var page = renderService.renderPage(guid.toString());
      // Extract the query from the page
      var doc = Jsoup.parse(page);
      var div = doc.select("div[data-name=feedQuery_" + feed.getName() + "]").first();
      if (div != null) {
        data = div.attr("data-query");
      }
      if (data != null) {
        feed.setQuery(data);
      } else {
        // Remove item
        it.remove();
      }
    }
  }

  private static final String CONTENT_TYPE_PAGE = "percPage";
  private static final String CONTENT_TYPE_TEMPLATE = "percPageTemplate";
  private static final int DELIVERY_CONTEXT = 10;
}
