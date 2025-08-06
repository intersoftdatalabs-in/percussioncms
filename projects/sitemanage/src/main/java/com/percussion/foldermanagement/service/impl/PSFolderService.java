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
package com.percussion.foldermanagement.service.impl;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.error.PSExceptionUtils;
import com.percussion.foldermanagement.data.PSFolderItem;
import com.percussion.foldermanagement.data.PSGetAssignedFoldersJobStatus;
import com.percussion.foldermanagement.data.PSWorkflowAssignment;
import com.percussion.foldermanagement.service.IPSFolderService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pathmanagement.data.PSFolderProperties;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.server.PSRequest;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.content.data.PSItemSummary.ObjectTypeEnum;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.share.async.IPSAsyncJobService;
import com.percussion.share.async.PSAsyncJobStatus;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.PSLightWeightObject;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.service.IPSSiteSectionMetaDataService;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.thread.PSThreadUtils;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.workflow.service.IPSSteppedWorkflowMetadata;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service class to handle the associations of sites or assets folders and
 * workflows. It provides methods to query and to modify the workflow associated
 * with sites and folders.
 * 
 * @author miltonpividori
 * 
 */
@Component("folderService")
public class PSFolderService implements IPSFolderService {
    private static final String ASSIGNMENT_IN_PROGRESS = "We are unable to process your request at this time.  A similar request has already been submitted and is still processing in the background.  Please try again later.";
    
    private static final Logger log = LogManager.getLogger(PSFolderService.class);
    
    private static AtomicInteger assignmentOperationCount = new AtomicInteger();

    private IPSFolderHelper folderHelper;

    private IPSPathService pathService;

    private IPSSiteManager siteMgr;

    private IPSWorkflowService workflowService;
    
    private IPSCmsObjectMgr cmsObjectManager;
    
    private IPSContentWs contentWs;
    
    private IPSSteppedWorkflowMetadata steppedWfMetadata;
    
    private IPSIdMapper idMapper;
    
    private IPSAsyncJobService asyncJobService;
    
    /**
     * The item definition manager, initialized by constructor.
     */
    private PSItemDefManager itemDefManager;
    
    @Autowired
    public PSFolderService(IPSFolderHelper folderHelper, @Qualifier("pathService") IPSPathService pathService, IPSSiteManager siteMgr,
            IPSWorkflowService workflowService, IPSCmsObjectMgr cmsObjectManager, IPSContentWs contentWs, IPSSteppedWorkflowMetadata steppedWfMetadata, IPSIdMapper idMapper, 
            IPSAsyncJobService asyncJobService, PSItemDefManager itemDefManager)
    {
        this.folderHelper = folderHelper;
        this.pathService = pathService;
        this.siteMgr = siteMgr;
        this.workflowService = workflowService;
        this.cmsObjectManager = cmsObjectManager;
        this.contentWs = contentWs;
        this.steppedWfMetadata = steppedWfMetadata;
        this.idMapper = idMapper;
        this.asyncJobService = asyncJobService;
        this.itemDefManager = itemDefManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.percussion.foldermanagement.service.IPSFolderService#getAssignedFolders
     * (java.lang.String, java.lang.String, boolean)
     */
    @Override
    public List<PSFolderItem> getAssignedFolders(String workflowName, String path,
            boolean includeFoldersWithDifferentWorkflow) throws Exception {
        Validate.notEmpty(workflowName, "workflowName cannot be empty");
        Validate.notEmpty(path, "path cannot be empty");
        validateWorkflow(workflowName);

        var folderItems = new ArrayList<PSFolderItem>();
        var foldersProperties = getSubfolders(path);

        for (var folderProperties : foldersProperties) {
            PSThreadUtils.checkForInterrupt();
            var fullFolderTreeItem = getFullFolderTree(getFolderItem(folderProperties), workflowName, includeFoldersWithDifferentWorkflow);

            if (!includeFoldersWithDifferentWorkflow) {
                if (fullFolderTreeItem.branchAssociatedWithWorkflow) {
                    folderItems.add(fullFolderTreeItem.folderItem);
                }
            } else {
                folderItems.add(fullFolderTreeItem.folderItem);
            }
        }
        return folderItems;
    }

    @Override
    public String startGetAssignedFoldersJob(String workflowName, String path, boolean includeFoldersWithDifferentWorkflow) throws PSWorkflowNotFoundException {
        var jobId = asyncJobService.startJob("getAssignedFoldersJob", new Object[] {workflowName, path, includeFoldersWithDifferentWorkflow});
        return String.valueOf(jobId);
    }

    @Override
    public PSGetAssignedFoldersJobStatus getAssignedFoldersJobStatus(String jobId) {
        if (!StringUtils.isNumeric(jobId))
            throw new IllegalArgumentException("jobId must be a number.");
        var lJob = Long.parseLong(jobId);

        var status = asyncJobService.getJobStatus(lJob);
        var result = asyncJobService.getJobResult(lJob);

        if (result instanceof PSGetAssignedFoldersJobStatus) {
            var jobStatus = (PSGetAssignedFoldersJobStatus) result;
            jobStatus.setJobId(lJob);
            if ("-1".equals(jobStatus.getStatus())) {
                asyncJobService.cancelJob(lJob);
                jobStatus.setMessage("terminated");
            } else {
                jobStatus.setMessage(status.getMessage());
            }
            return jobStatus;
        }

        var jobStatus = new PSGetAssignedFoldersJobStatus();
        jobStatus.setJobId(lJob);
        jobStatus.setStatus(String.valueOf(status.getStatus()));
        jobStatus.setMessage(status.getMessage());
        return jobStatus;
    }

    @Override
    public PSGetAssignedFoldersJobStatus cancelAssignedFoldersJob(String jobId) {
        if (!StringUtils.isNumeric(jobId))
            throw new IllegalArgumentException("jobId must be a number.");
        var lJob = Long.parseLong(jobId);

        asyncJobService.cancelJob(lJob);
        return getAssignedFoldersJobStatus(jobId);
    }

    @Override
    public void assignFoldersToWorkflow(PSWorkflowAssignment workflowAssignment) throws PSWorkflowNotFoundException, PSWorkflowAssignmentInProgressException {
        Validate.notNull(workflowAssignment, "workflowAssignment cannot be null");
        Validate.notEmpty(workflowAssignment.getWorkflowName(), "workflowAssignment.workflowName cannot be null");

        var workflow = validateWorkflow(workflowAssignment.getWorkflowName());

        boolean didSetInProgress = false;
        boolean didStartThread = false;
        try {
            if (!incrementAssignmentOperationCount(false)) {
                throw new PSWorkflowAssignmentInProgressException(ASSIGNMENT_IN_PROGRESS);
            }
            didSetInProgress = true;

            assignFoldersToWorkflow(workflowAssignment.getAssignedFolders(), workflowAssignment.getUnassignedFolders(), workflow);
            didStartThread = applyWorkflowToContent(workflowAssignment.getAppliedFolders());
        } finally {
            if (didSetInProgress && !didStartThread) {
                decrementAssignmentOperationCount();
            }
        }
    }

    private void decrementAssignmentOperationCount() {
        var count = assignmentOperationCount.decrementAndGet();
        if (count < 0)
            assignmentOperationCount.compareAndSet(count, 0);
    }

    private boolean incrementAssignmentOperationCount(boolean allowMultiple) {
        if (assignmentOperationCount.getAndIncrement() > 0 && !allowMultiple) {
            assignmentOperationCount.decrementAndGet();
            return false;
        }
        return true;
    }

    private void assignFoldersToWorkflow(String[] assignedFolderIds, String[] unassignedFolderIds, PSWorkflow workflow) {
        for (var id : assignedFolderIds) {
            try {
                var folderProperties = folderHelper.findFolderProperties(id);
                folderProperties.setWorkflowId(workflow.getGUID().getUUID());
                folderHelper.saveFolderProperties(folderProperties);
            } catch (Exception e) {
                log.error("There was an error assigning the workflow '{}' to the folder with id '{}'. The underlying error was: {}",
                        workflow.getName(), id, e.getMessage());
            }
        }
        for (var id : unassignedFolderIds) {
            try {
                var folderProperties = folderHelper.findFolderProperties(id);
                folderProperties.setWorkflowId(Integer.MIN_VALUE);
                folderHelper.saveFolderProperties(folderProperties);
            } catch (Exception e) {
                log.error("There was an error unassigning the workflow '{}' to the folder '{}'. The underlying error was: {}",
                        workflow.getName(), id, e.getMessage());
            }
        }
    }

    @Override
    public boolean isContentWorkflowAssignmentInProgress() {
        return assignmentOperationCount.get() > 0;
    }

    @Override
    public boolean applyWorkflowToContent(String[] folderIds) {
        boolean didStartThread = false;
        boolean didIncrementCount = false;

        if (folderIds != null && folderIds.length == 0)
            return didStartThread;

        final Map<Integer, List<Integer>> workflowsMap = new HashMap<>();

        if (folderIds == null) {
            var defaultFolderIds = getDefaultWorkflowFolderIds();
            if (defaultFolderIds.isEmpty()) {
                return didStartThread;
            }
            didIncrementCount = incrementAssignmentOperationCount(true);
            folderIds = defaultFolderIds.toArray(new String[0]);
        }

        try {
            for (var folderId : folderIds) {
                try {
                    var folderProperties = folderHelper.findFolderProperties(folderId);
                    var folderContentId = idMapper.getContentId(folderId);
                    var folderWorkflowId = folderHelper.getValidWorkflowId(folderProperties);

                    var workflowsList = workflowsMap.computeIfAbsent(folderWorkflowId, k -> new ArrayList<>());
                    workflowsList.add(folderContentId);
                } catch (Exception e) {
                    log.error("There was an error applying the assigned workflow to content in the folder '{}'. The underlying error was: {}",
                            folderId, e.getMessage());
                }
            }

            final var systemStateNames = steppedWfMetadata.getSystemStatesList();
            final var requestInfoMap = PSRequestInfo.copyRequestInfoMap();
            var request = (PSRequest) requestInfoMap.get(PSRequestInfo.KEY_PSREQUEST);
            requestInfoMap.put(PSRequestInfo.KEY_PSREQUEST, request.cloneRequest());
            Runnable worker = () -> {
                try {
                    if (PSRequestInfo.isInited()) {
                        PSRequestInfo.resetRequestInfo();
                    }
                    PSRequestInfo.initRequestInfo(requestInfoMap);

                    for (var pairs : workflowsMap.entrySet()) {
                        cmsObjectManager.changeWorfklowForItems(pairs.getValue(), pairs.getKey(), systemStateNames);
                    }
                } catch (Exception e) {
                    log.error("There was an error applying the assigned workflow to content in the specified folders. The underlying error was: {}",
                            PSExceptionUtils.getMessageForLog(e));
                } finally {
                    assignmentOperationCount.decrementAndGet();
                }
            };

            var thread = new Thread(worker, "AssignContentToWorkflow");
            thread.setDaemon(true);
            thread.start();
            didStartThread = true;
            return didStartThread;
        } finally {
            if (!didStartThread && didIncrementCount)
                decrementAssignmentOperationCount();
        }
    }

    private List<String> getDefaultWorkflowFolderIds() {
        var idList = new ArrayList<String>();
        try {
            idList.addAll(getDefaultWorkflowIdsFromPath("/Sites"));
            idList.addAll(getDefaultWorkflowIdsFromPath("/Assets"));
            return idList;
        } catch (Exception e) {
            log.error("Error getting all paths using the default workflow. The underlying error was: {}", e.getMessage());
            idList.clear();
            return idList;
        }
    }

    private List<String> getDefaultWorkflowIdsFromPath(String path) throws Exception {
        var ids = new ArrayList<String>();
        var subPaths = getSubfolders(path);
        for (var pathItem : subPaths) {
            var folderItem = getFolderItem(pathItem);
            if (StringUtils.isEmpty(folderItem.getWorkflowName()))
                ids.add(folderItem.getId());
            ids.addAll(getDefaultWorkflowIds(folderItem.getId()));
        }
        return ids;
    }

    private List<String> getDefaultWorkflowIds(String id) throws Exception {
        var paths = new ArrayList<String>();
        var subPaths = getSubFoldersWithoutPath(id);
        for (var pathItem : subPaths) {
            var folderItem = getFolderItem(pathItem);
            if (StringUtils.isEmpty(folderItem.getWorkflowName()))
                paths.add(pathItem.getId());
            paths.addAll(getDefaultWorkflowIds(pathItem.getId()));
        }
        return paths;
    }

    private List<PSPathItem> getSubfolders(String path) throws IPSPathService.PSPathServiceException, PSDataServiceException {
        var children = pathService.findChildren(path);
        var subfolders = new ArrayList<PSPathItem>();
        for (var item : children) {
            if (item.isFolder() || PSPathItem.TYPE_SITE.equals(item.getType()))
                subfolders.add(item);
        }
        return subfolders;
    }

    private List<PSPathItem> getSubFoldersWithoutPath(String id) {
        var subfolders = new ArrayList<PSPathItem>();
        var itemSums = contentWs.findFolderChildren(idMapper.getGuid(id), false);
        for (var sum : itemSums) {
            if (sum.getObjectType().equals(ObjectTypeEnum.ITEM))
                continue;
            if (sum.getName().equals(IPSSiteSectionMetaDataService.SECTION_SYSTEM_FOLDER_NAME))
                continue;
            var childItem = new PSPathItem();
            childItem.setId(sum.getGUID().toString());
            childItem.setName(sum.getName());
            childItem.setType(sum.getContentTypeName());
            subfolders.add(childItem);
        }
        subfolders.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
        return subfolders;
    }

    private PSFolderTreeInfo getFullFolderTree(PSFolderItem folderItem, String workflowName,
            Boolean includeFoldersWithDifferentWorkflow) throws Exception {
        PSThreadUtils.checkForInterrupt();

        boolean branchAssociatedWithWorkflow;
        var foldersProperties = getSubFoldersWithoutPath(folderItem.getId());

        if (foldersProperties == null || foldersProperties.isEmpty()) {
            branchAssociatedWithWorkflow = StringUtils.equalsIgnoreCase(folderItem.getWorkflowName(), workflowName);
            folderItem.setChildren(null);
            folderItem.setAllChildrenAssociatedWithWorkflow(branchAssociatedWithWorkflow);
            return new PSFolderTreeInfo(folderItem, branchAssociatedWithWorkflow);
        }

        var children = new ArrayList<PSFolderTreeInfo>();
        boolean allChildrenAssociatedWithWorkflow = true;

        for (var folderProperties : foldersProperties) {
            var childFolderItemWithChildren = getFullFolderTree(getFolderItem(folderProperties), workflowName, includeFoldersWithDifferentWorkflow);

            if (!includeFoldersWithDifferentWorkflow) {
                if (childFolderItemWithChildren.branchAssociatedWithWorkflow) {
                    children.add(childFolderItemWithChildren);
                }
            } else {
                children.add(childFolderItemWithChildren);
            }

            if (!StringUtils.equalsIgnoreCase(childFolderItemWithChildren.folderItem.getWorkflowName(), workflowName)
                    || (childFolderItemWithChildren.folderItem.getAllChildrenAssociatedWithWorkflow() != null
                        && Boolean.FALSE.equals(childFolderItemWithChildren.folderItem.getAllChildrenAssociatedWithWorkflow())))
                allChildrenAssociatedWithWorkflow = false;
        }

        folderItem.setChildren(PSFolderTreeInfo.getChildrenElements(children));
        folderItem.setAllChildrenAssociatedWithWorkflow(allChildrenAssociatedWithWorkflow);

        branchAssociatedWithWorkflow = StringUtils.equalsIgnoreCase(folderItem.getWorkflowName(), workflowName)
                || (!includeFoldersWithDifferentWorkflow && !children.isEmpty());

        return new PSFolderTreeInfo(folderItem, branchAssociatedWithWorkflow);
    }

    private PSFolderItem getFolderItem(PSPathItem pathItem) throws Exception {
        var id = pathItem.getId();
        PSFolderProperties folderProperties = null;

        if (PSPathItem.TYPE_SITE.equals(pathItem.getType())) {
            var site = siteMgr.loadSite(pathItem.getName());
            id = folderHelper.findFolder(site.getFolderRoot()).getId();
        }

        var folder = new PSFolderItem();
        folder.setName(pathItem.getName());
        folder.setId(id);

        folderProperties = folderHelper.findFolderProperties(id);
        if (folderProperties.getWorkflowId() > 0) {
            var workflow = workflowService.loadWorkflow(PSGuidUtils.makeGuid(folderProperties.getWorkflowId(), PSTypeEnum.WORKFLOW));
            if (workflow != null)
                folder.setWorkflowName(workflow.getName());
            else
                log.debug("The workflow ID associated with the folder '{}' is invalid and will be ignored", pathItem.getPath());
        }
        return folder;
    }

    @Override
    public PSWorkflow validateWorkflow(String workflowName) throws PSWorkflowNotFoundException {
        PSWorkflow workflow = null;
        if (!StringUtils.isBlank(workflowName)) {
            var wfs = workflowService.findWorkflowsByName(workflowName);
            if (!wfs.isEmpty()) {
                workflow = wfs.get(0);
            }
        }
        if (workflow == null) {
            throw new PSWorkflowNotFoundException("The workflow '" + workflowName + "' could not be found.");
        }
        return workflow;
    }

    private static class PSFolderTreeInfo {
        private PSFolderItem folderItem;
        private boolean branchAssociatedWithWorkflow;

        public PSFolderTreeInfo(PSFolderItem folderItem, boolean branchAssociatedWithWorkflow) {
            this.folderItem = folderItem;
            this.branchAssociatedWithWorkflow = branchAssociatedWithWorkflow;
        }

        public static List<PSFolderItem> getChildrenElements(List<PSFolderTreeInfo> children) {
            var items = new ArrayList<PSFolderItem>();
            if (children != null) {
                for (var item : children) {
                    items.add(item.folderItem);
                }
            }
            return items;
        }
    }

    @Override
    public List<PSLightWeightObject> getPagesFromFolder(String folderId)
            throws PSFolderNotFoundException, PSPagesNotFoundException, PSValidationException {
        if (StringUtils.isBlank(folderId))
            throw new PSFolderNotFoundException("The supplied folder id is blank");
        var folderProperties = folderHelper.findFolderProperties(folderId);
        var results = new ArrayList<PSLightWeightObject>();
        long pageCtypeId;
        try {
            pageCtypeId = itemDefManager.contentTypeNameToId(IPSPageService.PAGE_CONTENT_TYPE);
        } catch (PSInvalidContentTypeException e) {
            log.error(e);
            throw new PSPagesNotFoundException("Error occurred retrieving pages for the supplied folder, please see log for more details.");
        }
        var itemSums = contentWs.findFolderChildren(idMapper.getGuid(folderProperties.getId()), false);
        for (var sum : itemSums) {
            if (sum.getContentTypeId() == pageCtypeId)
                results.add(new PSLightWeightObject(sum.getName(), sum.getGUID().toString()));
        }
        return results;
    }

}
