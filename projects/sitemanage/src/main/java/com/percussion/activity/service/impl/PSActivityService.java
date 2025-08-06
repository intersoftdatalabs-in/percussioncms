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

package com.percussion.activity.service.impl;

import com.percussion.activity.data.PSActivityNode;
import com.percussion.activity.data.PSContentActivity;
import com.percussion.activity.service.IPSActivityService;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService.PSItemWorkflowServiceException;
import com.percussion.pagemanagement.dao.IPSResourceDefinitionGroupDao;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSAssetResource;
import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import com.percussion.pathmanagement.service.IPSPathService.PSPathServiceException;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSContentPropertyConstants;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.date.PSDateRange;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static com.percussion.itemmanagement.service.impl.PSWorkflowHelper.WF_STATE_ARCHIVE;
import static com.percussion.itemmanagement.service.impl.PSWorkflowHelper.WF_STATE_LIVE;
import static com.percussion.itemmanagement.service.impl.PSWorkflowHelper.WF_STATE_PENDING;
import static com.percussion.itemmanagement.service.impl.PSWorkflowHelper.WF_TAKE_DOWN_TRANSITION;
import static com.percussion.pagemanagement.service.IPSPageService.PAGE_CONTENT_TYPE;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

/**
 * Utilities for content activity service.
 * Sunny Sal says: "May the Streams be with you!"
 */
@PSSiteManageBean("activityService")
public class PSActivityService implements IPSActivityService {
    private static final Logger ms_log = LogManager.getLogger(PSActivityService.class);

    private final IPSSiteManager siteMgr;
    private final IPSResourceDefinitionGroupDao resDao;
    private final PSItemDefManager itemDefMgr;
    private final IPSPublisherService pub;
    private final IPSContentMgr contentMgr;
    private final IPSSystemService sysSrv;
    private final IPSContentWs contentWs;
    private final IPSItemWorkflowService itemWfSrvc;
    private final IPSGuidManager guidMgr;
    private final IPSIdMapper idMapper;

    @Autowired
    public PSActivityService(
            IPSSiteManager siteMgr,
            IPSResourceDefinitionGroupDao resDao,
            PSItemDefManager itemDefMgr,
            IPSPublisherService pub,
            IPSContentMgr contentMgr,
            IPSSystemService sysSrv,
            IPSContentWs contentWs,
            IPSItemWorkflowService itemWfSrvc,
            IPSGuidManager guidMgr,
            IPSIdMapper idMapper) {
        this.siteMgr = siteMgr;
        this.resDao = resDao;
        this.itemDefMgr = itemDefMgr;
        this.pub = pub;
        this.contentMgr = contentMgr;
        this.contentWs = contentWs;
        this.itemWfSrvc = itemWfSrvc;
        this.sysSrv = sysSrv;
        this.guidMgr = guidMgr;
        this.idMapper = idMapper;
    }

    @Override
    public PSContentActivity createActivity(PSActivityNode node, Date beginDate, long timeout)
            throws PSActivityServiceException, PSPathServiceException {
        notNull(node);
        notNull(beginDate);

        var sw = new StopWatch();
        sw.start();

        checkTimeout(sw.getTime(), timeout);

        var path = node.getPath();
        var siteName = node.getSiteName();
        var ids = findPageIdsByPath(path);
        checkTimeout(sw.getTime(), timeout);

        var endDate = new Date();

        int publishedItems = 0;
        if (!ids.isEmpty()) {
            var site = siteMgr.findSite(siteName);
            if (site != null) {
                publishedItems = pub.findLastPublishedItemsBySite(site.getGUID(), ids);
            }
        }
        checkTimeout(sw.getTime(), timeout);

        int newItems = ids.isEmpty() ? 0 : sysSrv.findNewContentActivities(ids, beginDate, endDate, WF_STATE_LIVE);
        checkTimeout(sw.getTime(), timeout);

        int updatedItems = ids.isEmpty() ? 0 : sysSrv.findNumberContentActivities(ids, beginDate, endDate, WF_STATE_LIVE, null);
        updatedItems -= newItems;
        checkTimeout(sw.getTime(), timeout);

        int archivedItems = ids.isEmpty() ? 0 : sysSrv.findNumberContentActivities(
                ids, beginDate, endDate, WF_STATE_ARCHIVE, WF_TAKE_DOWN_TRANSITION);
        checkTimeout(sw.getTime(), timeout);

        int pendingItems = (int) getPendingPageCount(path);

        return new PSContentActivity(
                siteName, node.getPath(), node.getName(),
                publishedItems, pendingItems, newItems, updatedItems, archivedItems);
    }

    private void checkTimeout(long time, long timeout) throws PSActivityServiceException {
        if (time > timeout) {
            throw new PSActivityServiceException("The requested data is taking too long to retrieve, sorry!");
        }
    }

    @Override
    public Collection<Integer> findPageIdsByPath(String path) {
        notEmpty(path);
        return getContentIdsByPath(path, Collections.singletonList(PAGE_CONTENT_TYPE));
    }

    @Override
    public Collection<Integer> findItemIdsByPath(String path, Collection<String> contentTypes) {
        notEmpty(path);
        return getContentIdsByPath(path, contentTypes);
    }

    @Override
    public List<Integer> findNewContentActivities(Collection<Integer> contentIds, List<Date> dates) {
        notNull(contentIds);
        if (dates == null || dates.size() < 2) {
            throw new IllegalArgumentException("dates must contain more than 1 Date elements.");
        }
        if (contentIds.isEmpty()) {
            return Collections.nCopies(dates.size() - 1, 0);
        }
        var counts = new ArrayList<Integer>(dates.size() - 1);
        for (int i = 1; i < dates.size(); i++) {
            var beginDate = dates.get(i - 1);
            var endDate = dates.get(i);
            int count = sysSrv.findNewContentActivities(contentIds, beginDate, endDate, WF_STATE_LIVE);
            counts.add(count);
        }
        return counts;
    }

    @Override
    public List<Integer> findNumberContentActivities(Collection<Integer> contentIds, List<Date> dates, String stateName, String transitionName) {
        if (dates == null || dates.size() < 2) {
            throw new IllegalArgumentException("dates must contain more than 1 Date elements.");
        }
        if (contentIds.isEmpty()) {
            return Collections.nCopies(dates.size() - 1, 0);
        }
        var counts = new ArrayList<Integer>();
        for (int i = 1; i < dates.size(); i++) {
            var beginDate = dates.get(i - 1);
            var endDate = dates.get(i);
            int count = sysSrv.findNumberContentActivities(contentIds, beginDate, endDate, stateName, transitionName);
            counts.add(count);
        }
        return counts;
    }

    @Override
    public List<String> findPageIdsContentActivities(Collection<Integer> contentIds, Date beginDate, Date endDate, String stateName, String transitionName) {
        if (beginDate == null || endDate == null) {
            throw new IllegalArgumentException("date must not be empty");
        }
        if (contentIds.isEmpty()) {
            return Collections.emptyList();
        }
        var pageIds = sysSrv.findPageIdsContentActivities(contentIds, beginDate, endDate, stateName, transitionName);
        return pageIds.stream()
                .map(pageId -> {
                    IPSGuid guid = guidMgr.makeGuid(pageId, PSTypeEnum.LEGACY_CONTENT);
                    return idMapper.getString(guid);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> findPublishedItems(Collection<Integer> contentIds, List<Date> dates) {
        if (dates == null || dates.size() < 2) {
            throw new IllegalArgumentException("dates must contain more than 1 Date elements.");
        }
        if (contentIds.isEmpty()) {
            return Collections.nCopies(dates.size() - 1, 0);
        }
        var counts = new ArrayList<Integer>();
        for (int i = 1; i < dates.size(); i++) {
            var beginDate = dates.get(i - 1);
            var endDate = dates.get(i);
            int count = sysSrv.findPublishedItems(contentIds, beginDate, endDate, WF_STATE_LIVE, WF_STATE_ARCHIVE);
            counts.add(count);
        }
        return counts;
    }

    @Override
    public Collection<Long> findPublishedItems(Collection<Integer> contentIds) {
        if (contentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sysSrv.findPublishedItems(contentIds, WF_STATE_LIVE, WF_STATE_ARCHIVE);
    }

    /**
     * Returns the number of pages that are in pending state under a given path.
     */
    private long getPendingPageCount(String path) throws PSPathServiceException {
        IPSWorkflowService workflowService = PSWorkflowServiceLocator.getWorkflowService();
        return getItemCount(path, Collections.singletonList(PAGE_CONTENT_TYPE),
                workflowService.getDefaultWorkflowName(), WF_STATE_PENDING);
    }

    /**
     * Finds the number of items under the given path that are in given workflow and state.
     */
    private long getItemCount(String path, List<String> contentTypes, String workflowName, String stateName)
            throws PSPathNotFoundServiceException, PSPathServiceException {
        notEmpty(path);
        notEmpty(contentTypes);
        notEmpty(workflowName);
        notEmpty(stateName);

        int workflowId;
        int stateId;
        try {
            workflowId = itemWfSrvc.getWorkflowId(workflowName);
            stateId = itemWfSrvc.getStateId(workflowName, stateName);
        } catch (PSItemWorkflowServiceException | PSValidationException e) {
            throw new PSPathServiceException(e);
        }
        var jcrCtypes = contentTypes.stream()
                .map(ct -> "rx:" + ct)
                .collect(Collectors.joining(", "));
        var jcrQuery = "select rx:sys_title from " + jcrCtypes
                + " where jcr:path like '" + path + "/%' and rx:sys_workflowid = " + workflowId;
        if (stateId != -1) {
            jcrQuery += " and rx:sys_contentstateid = " + stateId;
        }
        try {
            Query query = contentMgr.createQuery(jcrQuery, Query.SQL);
            QueryResult queryResult = contentMgr.executeQuery(query, -1, new HashMap<>(), null);
            return queryResult.getRows().getSize();
        } catch (Exception e) {
            ms_log.error("Error querying item count for path: '{}'", path, e);
            return 0;
        }
    }

    @Override
    public List<PSActivityNode> createActivityNodesByPaths(String path, boolean includeSite) {
        notEmpty(path);

        var cm1Sites = siteMgr.findAllSites().stream()
                .filter(site -> contentWs.getIdByPath(PSPathUtils.getFolderPath(site.getFolderRoot()) + "/.system") != null)
                .collect(Collectors.toList());

        if (!(PSPathUtils.SITES_FINDER_ROOT + "/").equals(path)) {
            for (var site : cm1Sites) {
                var folderRoot = site.getFolderRoot();
                if (path.equals(folderRoot) || ("/" + path).equals(folderRoot)) {
                    return createActivityNodesBySite(site, includeSite);
                } else if ((path + "/").startsWith(folderRoot + "/") || ("/" + path + "/").startsWith(folderRoot + "/")) {
                    return createActivityChildNodes(path, site.getName());
                }
            }
            throw new RuntimeException("Cannot find a site for path: " + path);
        }

        // get all sites
        return cm1Sites.stream()
                .map(site -> new PSActivityNode(site.getName(), site.getName(), site.getFolderRoot(), PAGE_CONTENT_TYPE))
                .collect(Collectors.toList());
    }

    private List<PSActivityNode> createActivityNodesBySite(IPSSite site, boolean includeSite) {
        var result = new ArrayList<PSActivityNode>();
        var siteName = site.getName();
        if (includeSite) {
            result.add(new PSActivityNode(siteName, siteName, site.getFolderRoot(), PAGE_CONTENT_TYPE));
        }
        result.addAll(createActivityChildNodes(site.getFolderRoot(), siteName));
        return result;
    }

    private List<PSActivityNode> createActivityChildNodes(String path, String siteName) {
        var result = new ArrayList<PSActivityNode>();
        var folderPath = (!path.startsWith("//") ? '/' + path : path);
        var sums = contentWs.findFolderChildren(folderPath, false);
        for (var sum : sums) {
            if (sum.getObjectType().equals(PSItemSummary.ObjectTypeEnum.FOLDER) && !".system".equals(sum.getName())) {
                var paths = contentWs.findItemPaths(sum.getGUID());
                result.add(new PSActivityNode(siteName, sum.getName(), paths[0], PAGE_CONTENT_TYPE));
            }
        }
        return result;
    }

    @Override
    public List<String> getResourceAssets() throws PSDataServiceException {
        return resDao.findAll().stream()
                .flatMap(resGrp -> resGrp.getAssetResources().stream())
                .filter(res -> res.isPrimary() && !PAGE_CONTENT_TYPE.equals(res.getContentType()))
                .map(PSAssetResource::getContentType)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getNonResourceAssets(Collection<String> resAssets) {
        notNull(resAssets);
        var result = new ArrayList<>(asList(itemDefMgr.getContentTypeNames(-1)));
        result.removeAll(resAssets);
        result.remove(PAGE_CONTENT_TYPE);
        return result;
    }

    @Override
    public PSDateRange createDateRange(String start, String end, String granularity) {
        var formatter = FastDateFormat.getInstance("yyyy-MM-dd");
        Date startDate = new Date();
        Date endDate = new Date();
        try {
            startDate = formatter.parse(start);
        } catch (ParseException e) {
            ms_log.error("Invalid start date: {}", start, e);
        }
        try {
            endDate = formatter.parse(end);
        } catch (ParseException e) {
            ms_log.error("Invalid end date: {}", end, e);
        }
        return new PSDateRange(startDate, endDate, PSDateRange.Granularity.valueOf(granularity));
    }

    @SuppressWarnings("unchecked")
    private Collection<Integer> getContentIdsByPath(String path, Collection<String> contentTypes) {
        var result = new ArrayList<Integer>();
        var query = createJCRQuery(path, contentTypes);
        try {
            Query q = contentMgr.createQuery(query, Query.SQL);
            QueryResult qresult = contentMgr.executeQuery(q, -1, null, null);
            RowIterator riter = qresult.getRows();
            while (riter.hasNext()) {
                Row r = riter.nextRow();
                Value cid = r.getValue(IPSContentPropertyConstants.RX_SYS_CONTENTID);
                result.add((int) cid.getLong());
            }
            return result;
        } catch (Exception e) {
            ms_log.error("Caught error while querying content IDs by path: '{}'", path, e);
            return Collections.emptyList();
        }
    }

    private String createJCRQuery(String path, Collection<String> contentTypes) {
        if (contentTypes == null || contentTypes.isEmpty()) {
            return "select rx:sys_contentid from nt:base where jcr:path like '" + path + "/%'";
        }
        var joined = contentTypes.stream()
                .map(name -> "rx:" + name)
                .collect(Collectors.joining(", "));
        return "select rx:sys_contentid from " + joined + " where jcr:path like '" + path + "/%'";
    }
}
