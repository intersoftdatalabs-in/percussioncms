// REFACTORED: CP-JAVA11

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
package com.percussion.delivery.metadata.impl;


import com.percussion.delivery.metadata.IPSBlogPostVisitService;
import com.percussion.delivery.metadata.IPSCookieConsent;
import com.percussion.delivery.metadata.IPSCookieConsentService;
import com.percussion.delivery.metadata.IPSMetadataDao;
import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataIndexerService;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.IPSMetadataQueryService;
import com.percussion.delivery.metadata.IPSMetadataRestService;
import com.percussion.delivery.metadata.data.PSCookieConsentQuery;
import com.percussion.delivery.metadata.data.PSMetadataBlogResult;
import com.percussion.delivery.metadata.data.PSMetadataDatedEntries;
import com.percussion.delivery.metadata.data.PSMetadataQuery;
import com.percussion.delivery.metadata.data.PSMetadataRestBlogList;
import com.percussion.delivery.metadata.data.PSMetadataRestCategory;
import com.percussion.delivery.metadata.data.PSMetadataRestEntry;
import com.percussion.delivery.metadata.data.PSMetadataRestTag;
import com.percussion.delivery.metadata.data.PSMetadataRestTagList;
import com.percussion.delivery.metadata.data.PSSearchResults;
import com.percussion.delivery.metadata.data.PSVisitQuery;
import com.percussion.delivery.metadata.data.PSVisitRestEntry;
import com.percussion.delivery.metadata.impl.utils.PSPair;
import com.percussion.delivery.services.PSAbstractRestService;
import com.percussion.error.PSExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * REST/Webservice layer for metadata services.
 * Sunny Sal here: Java 11, Google style, and maintainability upgrades!
 */
@Path("/metadata")
@Component
public class PSMetadataRestService extends PSAbstractRestService implements IPSMetadataRestService {
    /**
     * The metadata query service reference. Injected in the ctor. Never
     * <code>null</code>.
     */

    @Autowired
    private IPSMetadataQueryService queryService;

    @Autowired
    private IPSMetadataIndexerService indexer;

    @Autowired
    private IPSMetadataDao dao;

    @Autowired
    private IPSBlogPostVisitService visitService;

    @Autowired
    private IPSCookieConsentService cookieService;

    private static final Logger log = LogManager.getLogger(PSMetadataRestService.class);

    public PSMetadataRestService() {}

    @Autowired
    public PSMetadataRestService(
            IPSMetadataQueryService service,
            IPSMetadataIndexerService indexer,
            IPSMetadataDao dao,
            IPSBlogPostVisitService visitService,
            IPSCookieConsentService cookieService) {
        this.queryService = service;
        this.indexer = indexer;
        this.dao = dao;
        this.visitService = visitService;
        this.cookieService = cookieService;
    }

    @HEAD
    @Path("/csrf")
    public void csrf(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        var cookies = request.getCookies();
        if (cookies == null) {
            return;
        }
        for (var cookie : cookies) {
            if ("XSRF-TOKEN".equals(cookie.getName())) {
                response.setHeader("X-CSRF-HEADER", "X-XSRF-TOKEN");
                response.setHeader("X-CSRF-TOKEN", cookie.getValue());
            }
        }
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.metadata.impl.IPSMetadataRestService#get(com.percussion.delivery.metadata.data.PSMetadataQuery)
     */
    @Override
    @POST
    @Path("/get")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public PSSearchResults get(PSMetadataQuery metadataQuery) {
        var resultArr = new ArrayList<PSMetadataRestEntry>();
        var searchResults = new PSSearchResults();

        if (metadataQuery == null) {
            log.error("Illegal argument passed. MetadataQuery cannot be null.");
            return searchResults;
        }
        //Check FOR CMS-6530 Vulnerability ISSUE
        if (metadataQuery.getOrderBy() != null && metadataQuery.getOrderBy().toUpperCase().contains(" OR ")) {
            log.error("Blind SQL Injection Vulnerability found.");
            return searchResults;
        }
        if (log.isDebugEnabled()) {
            log.debug("Metadata query criteria in the service is : {}", metadataQuery.getCriteria());
        }

        try {
            var results = queryService.executeQuery(metadataQuery);
            results.getFirst().stream()
                    .map(this::toRestMetadataEntry)
                    .forEach(resultArr::add);
            searchResults.setResults(resultArr);
            searchResults.setTotalEntries(results.getSecond());
            return searchResults;
        } catch (Exception e) {
            log.error("Exception during searching metadata : {}", PSExceptionUtils.getMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.metadata.impl.IPSMetadataRestService#getTags(com.percussion.delivery.metadata.data.PSMetadataQuery, java.lang.String)
     */
    @Override
    @POST
    @Path("/tags/get")
    @Produces(MediaType.APPLICATION_JSON)
    public PSMetadataRestTagList getTags(PSMetadataQuery metadataQuery) {
        if (metadataQuery == null) {
            log.error("Illegal argument passed. MetadataQuery cannot be null.");
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Metadata query criteria in the service is : {}", metadataQuery.getCriteria());
        }
        try {
            var sortTagsBy = metadataQuery.getSortTagsBy();
            var metadataResults = queryService.executeQuery(metadataQuery);
            var psMetadataTagsHelper = new PSMetadataTagsHelper();
            var results = metadataResults.getFirst();
            var tags = psMetadataTagsHelper.processTags(results, sortTagsBy);
            return toRestMetadataTagList(tags);
        } catch (Exception e) {
            log.error("Exception during getting tag list : {}", PSExceptionUtils.getMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.metadata.impl.IPSMetadataRestService#getBlog(com.percussion.delivery.metadata.data.PSMetadataQuery, java.lang.String)
     */
    @Override
    @POST
    @Path("/blog/getCurrent")
    @Produces(MediaType.APPLICATION_JSON)
    public PSMetadataBlogResult getBlog(PSMetadataQuery metadataQuery) {
        if (metadataQuery == null) {
            log.error("Illegal argument passed. MetadataQuery cannot be null.");
            return null;
        }
        var currentPageId = metadataQuery.getCurrentPageId();
        if (StringUtils.isEmpty(currentPageId)) {
            log.error("Illegal argument passed to getBlog. Current Page Id was missing from request.");
            return null;
        }
        log.debug("Metadata query criteria in the service is : {}", metadataQuery.getCriteria());
        log.debug("Current page id in the service is : {}", currentPageId);

        try {
            var metadataResults = queryService.executeQuery(metadataQuery);
            var results = metadataResults.getFirst();
            var resultArr = results.stream()
                    .map(this::toRestMetadataEntry)
                    .collect(java.util.stream.Collectors.toList());

            var metadataBlogResults = new PSMetadataBlogResult();
            for (int i = 0; i < resultArr.size(); i++) {
                var entry = resultArr.get(i);
                if (entry.getPagepath().equalsIgnoreCase(currentPageId)) {
                    if (i > 0) {
                        metadataBlogResults.setNext(resultArr.get(i - 1));
                    }
                    metadataBlogResults.setCurrent(resultArr.get(i));
                    if ((i + 1) < resultArr.size()) {
                        metadataBlogResults.setPrevious(resultArr.get(i + 1));
                    }
                    break;
                }
            }
            return metadataBlogResults;
        } catch (Exception e) {
            log.error("Exception during getting current blog : {}", PSExceptionUtils.getMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.metadata.impl.IPSMetadataRestService#getCategories(com.percussion.delivery.metadata.data.PSMetadataQuery)
     */
    @Override
    @POST
    @Path("/categories/get")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PSMetadataRestCategory> getCategories(PSMetadataQuery metadataQuery) {
        if (metadataQuery == null) {
            log.error("Illegal argument passed. MetadataQuery cannot be null.");
            return null;
        }
        log.debug("Metadata query criteria in the service is : {}", metadataQuery.getCriteria());
        try {
            var cats = queryService.executeCategoryQuery(metadataQuery);
            var psMetadataCategoriesHelper = new PSMetadataCategoriesHelper();
            return psMetadataCategoriesHelper.processCategorySummary(cats);
        } catch (Exception e) {
            log.error("Exception during getting categories : {}", e.getLocalizedMessage());
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }


    /* (non-Javadoc)
     * @see com.percussion.delivery.metadata.impl.IPSMetadataRestService#getBlogs(com.percussion.delivery.metadata.data.PSMetadataQuery)
     */
    @Override
    @POST
    @Path("/blogs/get")
    @Produces(MediaType.APPLICATION_JSON)
    public PSMetadataRestBlogList getBlogs(PSMetadataQuery metadataQuery) {
        if (metadataQuery == null) {
            log.error("Illegal argument passed. MetadataQuery cannot be null.");
            return null;
        }
        log.debug("Metadata query criteria in the service is: {}", metadataQuery.getCriteria());
        try {
            var metadataResults = queryService.executeQuery(metadataQuery);
            var results = metadataResults.getFirst();
            var psBlogsHelper = new PSBlogsHelper();
            return psBlogsHelper.getProcessedBlogs(results);
        } catch (Exception e) {
            log.error("Exception during getting blogs: {}", PSExceptionUtils.getMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.metadata.impl.IPSMetadataRestService#getDatedEntries(com.percussion.delivery.metadata.data.PSMetadataQuery)
     */
    @Override
    @POST
    @Path("/dated/get")
    @Produces(MediaType.APPLICATION_JSON)
    public PSMetadataDatedEntries getDatedEntries(PSMetadataQuery metadataQuery) {
        if (metadataQuery == null) {
            log.error("Illegal argument passed. MetadataQuery cannot be null.");
            return null;
        }
        log.debug("Metadata query criteria in the service is: {}", metadataQuery.getCriteria());
        try {
            var metadataResults = queryService.executeQuery(metadataQuery);
            var results = metadataResults.getFirst();
            var psDatedHelper = new PSDatedEntriesHelper();
            return psDatedHelper.getDatedEntries(results);
        } catch (Exception e) {
            log.error("Exception during getting dated entries : {}", PSExceptionUtils.getMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }
    /* (non-Javadoc)
     * @see com.percussion.delivery.metadata.impl.IPSMetadataRestService#delete(java.util.Collection)
     */
    @Override
    @POST
    @Path("/delete")
    @RolesAllowed("deliverymanager")
    public void delete(Collection<String> pagepaths) {
        try {
            if (!pagepaths.isEmpty()) {
                indexer.delete(pagepaths);
                visitService.delete(pagepaths);
            }
        } catch (Exception e) {
            log.error("Exception during delete : {}", PSExceptionUtils.getMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }


    /* (non-Javadoc)
     * @see com.percussion.delivery.metadata.impl.IPSMetadataRestService#getAllIndexedDirectories()
     */
    @Override
    @GET
    @Path("/indexedDirectories")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getAllIndexedDirectories() {
        try {
            return indexer.getAllIndexedDirectories();
        } catch (Exception e) {
            log.error("Exception during getting all indexed directories: {}", PSExceptionUtils.getMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }

    }

    /**
     * Converts a PSMetadataEntry to a PSMetadataRestEntry.
     * Both classes represent the same thing, but the latter is used in the REST layer to return the desired fields.
     */
    private PSMetadataRestEntry toRestMetadataEntry(IPSMetadataEntry entry) {
        var metadataEntry = new PSMetadataRestEntry();
        metadataEntry.setName(entry.getName());
        metadataEntry.setFolder(entry.getFolder());
        metadataEntry.setLinktext(entry.getLinktext());
        metadataEntry.setPagepath(entry.getPagepath());
        metadataEntry.setType(entry.getType());
        metadataEntry.setSite(entry.getSite());
        entry.getProperties().forEach(metadataEntry::addMetadataProperty);
        return metadataEntry;
    }

    /**
     * Converts tag pairs to a PSMetadataRestTagList.
     */
    private PSMetadataRestTagList toRestMetadataTagList(List<PSPair<String, Integer>> tags) {
        var tagListResults = new PSMetadataRestTagList();
        tags.forEach(tag -> {
            var metadataTag = new PSMetadataRestTag();
            metadataTag.setTagName(tag.getFirst());
            metadataTag.setTagCount(tag.getSecond());
            tagListResults.getProperties().add(metadataTag);
        });
        return tagListResults;
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.metadata.impl.IPSMetadataRestService#getIndexerService()
     */
    @Override
    public IPSMetadataIndexerService getIndexerService() {
        return indexer;
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.metadata.impl.IPSMetadataRestService#setIndexerService(com.percussion.delivery.metadata.IPSMetadataIndexerService)
     */
    @Override
    public void setIndexerService(IPSMetadataIndexerService indexerService) {
        this.indexer = indexerService;
    }

    @Override
    @POST
    @Path("/categories/update/{sitename}/{deliveryserver}")
    @Consumes(MediaType.APPLICATION_JSON)
    public String updateCategoryInDTS(String category, @PathParam("sitename") String sitename, @PathParam("deliveryserver") String deliveryserver) {
        JSONObject categoryJson = null;
        JSONObject returnJson = new JSONObject();
        JSONArray categoryArray = null;
        try {
            categoryArray = new JSONArray(category);
            if (categoryArray.length() != 0) {
                returnJson = categoryArray.getJSONObject(0);
                for (int i = 0; i < categoryArray.length(); i++) {
                    categoryJson = categoryArray.getJSONObject(i);
                    dao.updateByCategoryProperty(
                            categoryJson.get("previousCategoryName").toString(),
                            categoryJson.get("title").toString());
                }
            } else {
                returnJson = new JSONObject();
                returnJson.put("empty", true);
                log.info("Category for update seems to be empty!");
            }
        } catch (JSONException e) {
            log.error("JSON Exception during updating the categories : {}", e.getMessage());
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return returnJson.toString();
    }

    @Override
    @POST
    @Path("/trackblogpost")
    @Consumes(MediaType.APPLICATION_JSON)
    public void trackBlogPost(PSVisitRestEntry visitEntry) {
        if (visitEntry == null || StringUtils.isBlank(visitEntry.getPagePath())) {
            log.error("Blank pagePath passed for tracking");
            return;
        }
        visitService.trackBlogPost(visitEntry.getPagePath());
    }

    @Override
    @POST
    @Path("/topblogposts")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PSMetadataRestEntry> getTopVisitedBlogPosts(PSVisitQuery visitQuery) {
        var results = new ArrayList<PSMetadataRestEntry>();
        try {
            var promotedPagePaths = new ArrayList<>(Arrays.asList(StringUtils.defaultString(visitQuery.getPromotedPagePaths(), "").split(";")));
            for (var path : promotedPagePaths) {
                if (StringUtils.isBlank(path)) {
                    continue;
                }
                var entry = dao.findEntry(path);
                if (entry != null) {
                    results.add(toRestMetadataEntry(entry));
                }
            }
            var limit = visitService.convertToLimit(visitQuery.getLimit());
            if (results.size() > limit) {
                results = new ArrayList<>(results.subList(0, limit));
            } else if (results.size() < limit) {
                var pagePaths = visitService.getTopVisitedBlogPosts(visitQuery);
                pagePaths.removeAll(promotedPagePaths);
                if (pagePaths.size() > limit - results.size()) {
                    pagePaths = new ArrayList<>(pagePaths.subList(0, limit - results.size()));
                }
                for (var path : pagePaths) {
                    var entry = dao.findEntry(path);
                    if (entry != null) {
                        results.add(toRestMetadataEntry(entry));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception during getting top read blog posts: {}", PSExceptionUtils.getMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
        return results;
    }

    @Override
    @POST
    @Path("/consent/log")
    @Consumes(MediaType.APPLICATION_JSON)
    public void saveCookieConsent(PSCookieConsentQuery consentQuery, @Context HttpServletRequest req) {
        if (consentQuery == null || StringUtils.isBlank(consentQuery.getServices().get(0))) {
            log.error("Cookie consent query was null or no services were approved to use cookies.");
            return;
        }
        log.debug("Cookie consent query object to save is: {}", consentQuery);
        log.debug("IP to save is: {}", req.getRemoteAddr());
        consentQuery.setIP(req.getRemoteAddr());
        visitService.logCookieConsentEntry(consentQuery);
    }

    @Override
    @GET
    @Path("/consent/log/{csvFileName}")
    @Produces({"text/csv"})
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("deliverymanager")
    public Response exportAllSiteCookieConsentStats(@PathParam("csvFileName") String csvFileName) {
        if (StringUtils.isBlank(csvFileName) || !StringUtils.contains(csvFileName.toLowerCase(), ".csv")) {
            log.error("CSV filename may not be blank and must contain .CSV as file extension.");
            return Response.serverError().build();
        }
        log.debug("Exporting all site stats.  CSV file name is: {}", csvFileName);
        var consents = cookieService.getAllConsentStats();
        var writer = new PSCookieConsentCSVWriter(consents);
        try {
            var response = Response.ok(writer.writeCSVFile());
            response.header("Content-Disposition", "attachment; filename=" + csvFileName);
            return response.build();
        } catch (Exception e) {
            log.error("Error getting cookie consent entries. Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    @Override
    @GET
    @Path("/consent/log/{siteName}/{csvFileName}")
    @Produces({"text/csv"})
    @RolesAllowed("deliverymanager")
    public Response exportSiteCookieConsentStats(@PathParam("siteName") String siteName,
                                                 @PathParam("csvFileName") String csvFileName) {
        if (StringUtils.isBlank(siteName) || StringUtils.isBlank(csvFileName) || !StringUtils.contains(csvFileName.toLowerCase(), ".csv")) {
            log.error("Site name or CSV file name may not be blank and file name must contain .csv.");
            return Response.serverError().build();
        }
        log.debug("Exporting CSV entries for site: {} with CSV name: {}", siteName, csvFileName);
        var consents = cookieService.getAllConsentStatsForSite(siteName);
        var writer = new PSCookieConsentCSVWriter(consents);
        try {
            var response = Response.ok(writer.writeCSVFile());
            response.header("Content-Disposition", "attachment; filename=" + siteName + "_" + csvFileName);
            return response.build();
        } catch (Exception e) {
            log.error("Error getting cookie consent entries.", e);
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    @Override
    @GET
    @Path("/consent/log/totals")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("deliverymanager")
    public Map<String, Integer> getAllCookieConsentTotals() {
        try {
            return cookieService.getAllConsentEntryTotals();
        } catch (Exception e) {
            log.error("Error getting total cookie consents for all sites.", e);
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    @Override
    @GET
    @Path("/consent/log/totals/{siteName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("deliverymanager")
    public Map<String, Integer> getCookieConsentEntriesPerSite(@PathParam("siteName") String siteName) {
        if (StringUtils.isBlank(siteName)) {
            log.error("Error retrieving cookie consent entries for site. Site name must not be blank");
            return new ConcurrentHashMap<>();
        }
        log.debug("Getting cookie consent entries for site: {}", siteName);
        try {
            return cookieService.getCookieConsentEntryTotalsPerSite(siteName);
        } catch (Exception e) {
            log.error("Error getting total cookie consents per site with name: {} Error: {}", siteName, PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    @Override
    @DELETE
    @Path("/consent/log")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("deliverymanager")
    public Response deleteAllCookieConsentEntries() {
        log.debug("Deleting all cookie consent entries.");
        try {
            cookieService.deleteAllCookieConsentEntries();
            return Response.ok().build();
        } catch (Exception e) {
            log.error("Error deleting all cookie consent entries. Error: {}", PSExceptionUtils.getMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    @Override
    @DELETE
    @Path("/consent/log/{siteName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("deliverymanager")
    public Response deleteCookieConsentEntriesForSite(@PathParam("siteName") String siteName) {
        if (StringUtils.isBlank(siteName)) {
            log.error("Site name may not be empty when deleting site's cookie consent entries.");
            return Response.serverError().build();
        }
        log.debug("Deleting all cookie consent entries for site: {}", siteName);
        try {
            cookieService.deleteCookieConsentEntriesForSite(siteName);
            return Response.ok().build();
        } catch (Exception e) {
            log.error("Error deleting all cookie consent entries. Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    @Override
    public String getVersion() {
        var version = super.getVersion();
        log.info("getVersion() from PSMetadataRestService ... {}", version);
        return version;
    }

    @Override
    @GET
    @Path("/visits/status")
    @Produces(MediaType.APPLICATION_JSON)
    public String getVisitServiceStatus() {
        if (!visitService.visitSchedulerStatus()) {
            throw new WebApplicationException(Response.serverError().build());
        }
        return "Running";
    }

    @Override
    public Response updateOldSiteEntries(String prevSiteName, String newSiteName) {
        if (StringUtils.isBlank(prevSiteName)) {
            log.error("prevSiteName may not be null or empty.");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        log.info("Logging with: {}", prevSiteName);
        dao.deleteBySite(prevSiteName, newSiteName);

        var cookies = cookieService.getAllConsentStatsForSite(prevSiteName);
        cookies.forEach(cookie -> cookie.setSiteName(newSiteName));
        cookieService.updateOldSiteName(prevSiteName, newSiteName);

        visitService.updatePostsAfterSiteRename(prevSiteName, newSiteName);

        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
