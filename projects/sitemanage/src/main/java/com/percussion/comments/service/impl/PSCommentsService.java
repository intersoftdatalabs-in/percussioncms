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
package com.percussion.comments.service.impl;

import com.percussion.comments.data.PSComment;
import com.percussion.comments.data.PSCommentIds;
import com.percussion.comments.data.PSCommentList;
import com.percussion.comments.data.PSCommentModeration;
import com.percussion.comments.data.PSCommentsDefaultModerationState;
import com.percussion.comments.data.PSCommentsSummary;
import com.percussion.comments.data.PSCommentsSummaryList;
import com.percussion.comments.data.PSSiteComments;
import com.percussion.comments.service.IPSCommentsService;
import com.percussion.delivery.client.IPSDeliveryClient.HttpMethodType;
import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions;
import com.percussion.delivery.client.PSDeliveryClient;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.error.PSExceptionUtils;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSPageSummary;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSSiteSummary;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.Validate.notNull;

/**
 * @author davidpardini
 *
 */
@Path("/comment")
@Component("commentsService")
@Lazy
public class PSCommentsService implements IPSCommentsService {
    /**
     * The delivery service initialized by constructor, never <code>null</code>.
     */
    private IPSDeliveryInfoService deliveryService;

    private IPSPageService pageService;

    private IPSFolderHelper folderHelper;

    private IPSiteDao siteDao;

    /***
     * The license Override if any
     */
    private String licenseId = "";

    @Autowired
    @Lazy
    private IPSPubServerService pubServerService;

    /**
     * Create an instance of the service.
     *
     * @param deliveryService the delivery service, not <code>null</code>.
     * @param pageService the page service, not <code>null</code>.
     */
    @Autowired
    public PSCommentsService(IPSDeliveryInfoService deliveryService, IPSPageService pageService,
                             IPSFolderHelper folderHelper, IPSiteDao siteDao) {
        notNull(deliveryService);
        notNull(pageService);
        notNull(folderHelper);
        this.deliveryService = deliveryService;
        this.pageService = pageService;
        this.folderHelper = folderHelper;
        this.siteDao = siteDao;

    }

    @Override
    @GET
    @Path("/pageswithcomments/{site}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<PSCommentsSummary> getPagesWithComments(
            @PathParam("site") String site,
            @QueryParam("max") Integer max,
            @QueryParam("start") Integer start) {
        // Prettify called url (legacy, but let's keep it for now)
        var maxParamString = max != null ? "max=" + max : "";
        var startParamString = start != null ? "start=" + start : "";
        var queryParamQMark = (!maxParamString.isBlank() || !startParamString.isBlank()) ? "?" : "";
        var queryParamAmper = (!maxParamString.isBlank() && !startParamString.isBlank()) ? "&" : "";

        var postJson = new JSONObject();
        postJson.element("maxResults", max);
        postJson.elementOpt("startIndex", start);
        var actionUrl = COMMENT_GET_PAGES_WITH_COMMENTS + site;

        return new PSCommentsSummaryList(getCommentsSummaries(site, actionUrl, false, postJson));
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.comments.service.IPSCommentsService#getCommentsSummary(java.lang.String)
     */
    @Override
    public PSCommentsSummary getCommentsSummary(String id)
            throws IPSDataService.DataServiceLoadException, IPSDataService.DataServiceNotFoundException, PSValidationException {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        var summary = new PSCommentsSummary();
        var pageSum = pageService.find(id);
        var siteSum = siteDao.findByPath(pageSum.getFolderPath());
        var siteName = siteSum.getName();
        var actionUrl = COMMENT_GET_PAGES_WITH_COMMENTS + siteName;

        for (var sum : getCommentsSummaries(siteName, actionUrl, false, null)) {
            if (id.equals(sum.getId())) {
                if (summary.getId() == null) {
                    summary = sum;
                } else {
                    summary.setApprovedCount(summary.getApprovedCount() + sum.getApprovedCount());
                    summary.setCommentCount(summary.getCommentCount() + sum.getCommentCount());
                    summary.setNewCount(summary.getNewCount() + sum.getNewCount());
                }
            }
        }
        return summary;
    }

    @Override
    public List<PSCommentsSummary> getCommentCountsForSite(String siteName) {
        Validate.notEmpty(siteName);
        var actionUrl = COMMENT_GET_PAGES_WITH_COMMENTS + siteName;
        return getCommentsSummaries(siteName, actionUrl, false, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.percussion.comments.service.IPSCommentsService#getCommentsOnPage(
     * java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer)
     */
    @Override
    @GET
    @Path("/commentsonpage/{site}/{pagePath:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<PSComment> getCommentsOnPage(
            @PathParam("site") String site,
            @PathParam("pagePath") String pagePath,
            @QueryParam("max") Integer max,
            @QueryParam("start") Integer start) {
        var aggregatedComments = new ArrayList<PSComment>();
        try {
            if (isBlank(pagePath)) {
                pagePath = "";
            }
            pagePath = "/" + pagePath;
            var adminUrl = pubServerService.getDefaultAdminURL(site);
            var server = deliveryService.findByService(PSDeliveryInfo.SERVICE_COMMENTS, null, adminUrl);
            if (server == null) {
                throw new RuntimeException("Cannot find server with service of: " + PSDeliveryInfo.SERVICE_COMMENTS);
            }
            var postJson = new JSONObject();
            postJson.element("site", site);
            postJson.elementOpt("pagepath", pagePath);

            try {
                var deliveryClient = new PSDeliveryClient();
                deliveryClient.setLicenseOverride(licenseId);
                var commentsOnPage = deliveryClient.getJsonObject(
                        new PSDeliveryActionOptions(server, COMMENT_GET_COMMENTS_ON_PAGE, HttpMethodType.POST, true),
                        postJson.toString()).getJSONArray("comments");

                for (var i = 0; i < commentsOnPage.size(); i++) {
                    var jsonComment = commentsOnPage.getJSONObject(i);
                    var currentComment = new PSComment();
                    currentComment.setPagePath(jsonComment.getString("pagePath"));
                    currentComment.setSiteName(jsonComment.getString("site"));
                    if (!(jsonComment.get("username") instanceof JSONNull)) {
                        currentComment.setUserName(jsonComment.getString("username"));
                    }
                    currentComment.setCommentCreateDate(jsonComment.getString("createdDate"));
                    currentComment.setCommentTitle(jsonComment.getString("title"));
                    currentComment.setCommentText(jsonComment.getString("text"));
                    currentComment.setUserEmail(jsonComment.getString("email"));
                    currentComment.setUserLinkUrl(jsonComment.getString("url"));
                    currentComment.setCommentApprovalState(jsonComment.getString("approvalState"));
                    currentComment.setCommentModerated(jsonComment.getBoolean("moderated"));
                    currentComment.setCommentViewed(jsonComment.getBoolean("viewed"));
                    currentComment.setCommentId(jsonComment.getString("id"));
                    aggregatedComments.add(currentComment);
                }
            } catch (Exception e) {
                var serviceUrl = server.getUrl() + COMMENT_GET_COMMENTS_ON_PAGE;
                log.warn("Error getting all comments data from processor at : {}. Error: {}",
                        serviceUrl, PSExceptionUtils.getMessageForLog(e));
            }
        } catch (IPSPubServerService.PSPubServerServiceException | PSNotFoundException e) {
            log.warn("Error getting all comments data from processor. Error: {}",
                    PSExceptionUtils.getMessageForLog(e));
        }
        return new PSCommentList(aggregatedComments);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.percussion.comments.service.IPSCommentsService#moderate(com.percussion
     * .comments.data.PSCommentModeration)
     */
    @Override
    @PUT
    @Path("/moderate/{site}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public void moderate(@PathParam("site") String site, PSCommentModeration commentModeration) {
        try {
            var adminUrl = pubServerService.getDefaultAdminURL(site);
            var server = deliveryService.findByService(PSDeliveryInfo.SERVICE_COMMENTS, null, adminUrl);
            if (server == null) {
                throw new RuntimeException("Cannot find service of: " + PSDeliveryInfo.SERVICE_COMMENTS);
            }
            if (commentModeration.getDeletes() != null && !commentModeration.getDeletes().isEmpty()) {
                log.info("Deleting comments in the delivery server: {}", server.getUrl());
                moderateCommentsOnDeliveryServer(server, COMMENT_DELETE_PATH, commentModeration.getDeletes());
            }
            if (commentModeration.getApproves() != null && !commentModeration.getApproves().isEmpty()) {
                log.info("Approving comments in the delivery server: {}", server.getUrl());
                moderateCommentsOnDeliveryServer(server, COMMENT_APPROVE_PATH, commentModeration.getApproves());
            }
            if (commentModeration.getRejects() != null && !commentModeration.getRejects().isEmpty()) {
                log.info("Rejecting comments in the delivery server: {}", server.getUrl());
                moderateCommentsOnDeliveryServer(server, COMMENT_REJECT_PATH, commentModeration.getRejects());
            }
        } catch (Exception ex) {
            log.error("There was an error in moderating comments in the delivery server. Error: {}", ex.getMessage());
            log.debug(ex.getMessage(), ex);
            throw new WebApplicationException(ex, Response.serverError().build());
        }
    }

    @Override
    @PUT
    @Path("/defaultModerationState")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public void setDefaultModerationState(PSCommentsDefaultModerationState data) {
        try {
            var adminUrl = pubServerService.getDefaultAdminURL(data.getSite());
            var server = deliveryService.findByService(PSDeliveryInfo.SERVICE_COMMENTS, null, adminUrl);
            var deliveryClient = new PSDeliveryClient();
            deliveryClient.setLicenseOverride(licenseId);

            var setState = new JSONObject();
            setState.put("site", data.getSite());
            setState.put("state", data.getState());
            deliveryClient.push(new PSDeliveryActionOptions(server, COMMENT_DEFAULT_MODERATION_STATE_PATH,
                    HttpMethodType.PUT, true), setState.toString());
        } catch (Exception e) {
            log.error("An unknown error occurred while retrieving the default moderation setting. Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new RuntimeException("An unknown error occurred while retrieving the default moderation setting");
        }
    }

    @Override
    @GET
    @Path("/defaultModerationState/{site}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getDefaultModerationState(@PathParam("site") String site) {
        try {
            var adminUrl = pubServerService.getDefaultAdminURL(site);
            var server = deliveryService.findByService(PSDeliveryInfo.SERVICE_COMMENTS, null, adminUrl);
            var deliveryClient = new PSDeliveryClient();
            deliveryClient.setLicenseOverride(licenseId);

            var moderationState = deliveryClient.getString(new PSDeliveryActionOptions(server,
                    COMMENT_GET_DEFAULT_MODERATION_STATE_PATH + "/" + site, HttpMethodType.GET, true));

            if ("APPROVED".equals(moderationState) || "REJECTED".equals(moderationState)) {
                var returnObject = new JSONObject();
                returnObject.put("defaultModerationState", moderationState);
                return returnObject.toString();
            } else {
                log.error("Unknown default moderation state: {}", moderationState);
                throw new WebApplicationException(Response.serverError().build());
            }
        } catch (Exception e) {
            var msg = "An error occurred while retrieving the default moderation setting.  "
                    + e.getLocalizedMessage();
            log.error("{}, Error: {}", msg, PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new RuntimeException(msg);
        }
    }

    /**
     * Moderates a list of comments in the given delivery server, according to
     * the url action.
     *
     * @param server The delivery server where moderation will take place. Must
     *            not be <code>null</code>.
     * @param urlAction The URL action part. Must not be <code>null</code>.
     * @param commentsToModerate A list of comments to moderate. Must not be
     *            <code>null</code>, maybe empty.
     * @throws IOException
     */
    private void moderateCommentsOnDeliveryServer(
            PSDeliveryInfo server, String urlAction, Collection<PSSiteComments> commentsToModerate) throws IOException {
        var commentIds = new PSCommentIds();
        commentsToModerate.stream()
                .filter(siteComments -> siteComments != null && siteComments.getComments() != null)
                .forEach(siteComments -> commentIds.getComments().addAll(siteComments.getComments()));

        var deliveryClient = new PSDeliveryClient();
        deliveryClient.setLicenseOverride(licenseId);

        deliveryClient.push(new PSDeliveryActionOptions(server, urlAction, HttpMethodType.PUT, true),
                PSSerializerUtils.getJsonFromObject(commentIds));
    }

    /**
     * Populates the PSCommentsSummary object with page and comments
     * information. Page link title will be different based on the availability
     * of the given pagepath(part of the pageObj) on the CM1 system
     *
     * @param siteName
     * @param pageObj comes from the delivery tier server
     * @param countsOnly <code>true</code> to load only the comment counts, <code>false</code> to include page data as well (much more expensive)
     *
     * @return PSCommentsSummary for the page
     */
    private PSCommentsSummary getCommentSummary(String siteName, JSONObject pageObj, boolean countsOnly) {
        var sum = new PSCommentsSummary();
        PSPage page = null;

        sum.setCommentCount(pageObj.getInt("commentCount"));
        sum.setApprovedCount(pageObj.getInt("approvedCount"));
        sum.setNewCount(pageObj.getInt("newCommentCount"));
        sum.setPagePath(pageObj.getString("pagePath"));

        var fullPagePath = SITES + siteName + sum.getPagePath();

        if (countsOnly) {
            sum.setPath(fullPagePath);
            return sum;
        }

        // set default date in case if error or page not find
        sum.setDatePosted("");
        try {
            page = pageService.findPageByPath(fullPagePath);
            if (page != null) {
                sum.setPath(fullPagePath);
                sum.setSummary(page.getSummary());
                var itemProperties = folderHelper.findItemProperties(PSPathUtils.getFolderPath(fullPagePath));
                sum.setDatePosted(itemProperties.getLastPublishedDate());
            }
        } catch (Exception e) {
            // If something goes wrong while getting the page by path, just move
            // on writing
            // the error to the log.
            log.warn("Error occurred while finding the page by path : {}", fullPagePath, e);
        }

        if (page == null) {
            sum.setId(null);
            return sum;
        } else if (page.getLinkTitle() == null || page.getLinkTitle().isEmpty()) {
            sum.setPageLinkTitle(page.getName());
        } else {
            sum.setPageLinkTitle(page.getLinkTitle());
        }
        sum.setId(page.getId());
        return sum;
    }

    /**
     * Finds the comments summaries for the specified site.
     *
     * @param name of the site.
     * @param url action url used to request the comment information from the delivery tier.
     *
     * @return list of comment summaries for the site.
     */
    private List<PSCommentsSummary> getCommentsSummaries(
            String name, String url, boolean countsOnly, JSONObject postJson) {
        if (postJson == null) {
            postJson = new JSONObject();
            postJson.element("maxResults", "");
            postJson.elementOpt("startIndex", "");
        }
        var summaries = new ArrayList<PSCommentsSummary>();
        String adminUrl;
        try {
            adminUrl = pubServerService.getDefaultAdminURL(name);
        } catch (IPSPubServerService.PSPubServerServiceException | PSNotFoundException e) {
            throw new WebApplicationException(e);
        }
        var server = deliveryService.findByService(PSDeliveryInfo.SERVICE_COMMENTS, null, adminUrl);
        if (server == null) {
            throw new RuntimeException("Cannot find service of: " + PSDeliveryInfo.SERVICE_COMMENTS);
        }
        try {
            var deliveryClient = new PSDeliveryClient();
            deliveryClient.setLicenseOverride(licenseId);
            var siteInfo = deliveryClient.getJsonObject(
                    new PSDeliveryActionOptions(server, url, HttpMethodType.POST, true), postJson.toString())
                    .getJSONArray("summaries");
            for (var i = 0; i < siteInfo.size(); i++) {
                var pageObj = siteInfo.getJSONObject(i);
                summaries.add(getCommentSummary(name, pageObj, countsOnly));
            }
        } catch (Exception e) {
            var urlStr = server.getUrl() + url;
            log.warn("Error getting all comments data from processor at : {}. Error: {}",
                    urlStr, PSExceptionUtils.getMessageForLog(e));
        }
        return summaries;
    }

    /**
     * Logger for this service.
     */
    private static final Logger log = LogManager.getLogger(PSCommentsService.class);

    private static final String COMMENT_GET_COMMENTS_ON_PAGE = "/perc-comments-services/comment/moderation/asmoderator";

    private static final String COMMENT_GET_PAGES_WITH_COMMENTS = "/perc-comments-services/comment/pageswithcomments/";

    private static final String COMMENT_GET_DEFAULT_MODERATION_STATE_PATH = "/perc-comments-services/comment/defaultModerationState";

    private static final String COMMENT_DELETE_PATH = "/perc-comments-services/comment/moderation/delete";

    private static final String COMMENT_APPROVE_PATH = "/perc-comments-services/comment/moderation/approve";

    private static final String COMMENT_REJECT_PATH = "/perc-comments-services/comment/moderation/reject";

    private static final String COMMENT_DEFAULT_MODERATION_STATE_PATH = "/perc-comments-services/comment/moderation/defaultModerationState";

    private static final String SITES = "/Sites/";

    /***
     * @see IPSCommentsService#setLicenseOverride(String licenseId)
     */
    @Override
    public void setLicenseOverride(String licenseId) {
        this.licenseId = licenseId;
    }

    /***
     * @see IPSCommentsService#getLicenseOverride()
     */
    @Override
    public String getLicenseOverride() {
        return this.licenseId;
    }

}
