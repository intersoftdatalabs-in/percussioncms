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

// REFACTORED: CP-JAVA11
package com.percussion.integritymanagement.service.impl;

import com.percussion.assetmanagement.data.PSAbstractAssetRequest;
import com.percussion.assetmanagement.data.PSAbstractAssetRequest.AssetType;
import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSBinaryAssetRequest;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.error.PSExceptionUtils;
import com.percussion.integritymanagement.data.PSIntegrityStatus;
import com.percussion.integritymanagement.data.PSIntegrityStatus.Status;
import com.percussion.integritymanagement.data.PSIntegrityTask;
import com.percussion.integritymanagement.data.PSIntegrityTask.TaskStatus;
import com.percussion.integritymanagement.data.PSIntegrityTaskProperty;
import com.percussion.integritymanagement.service.IPSIntegrityCheckerDao;
import com.percussion.integritymanagement.service.IPSIntegrityCheckerService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pathmanagement.service.impl.PSAssetPathItemService;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.rx.delivery.impl.PSAmazonS3DeliveryHandler;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.service.IPSSiteDataService.PublishType;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.IPSDTSStatusProvider;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.service.IPSUtilityService;
import com.percussion.utils.types.PSPair;
import com.percussion.webservices.PSWebserviceUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

/**
 * Service implementation for integrity checker operations.
 */
@Service("integrityCheckerService")
public class PSIntegrityCheckerService implements IPSIntegrityCheckerService {

    private static final Logger ms_log = LogManager.getLogger(PSIntegrityCheckerService.class);

    private static final String IMAGE_CREATE_TASK = "Image-Create";
    private static final String IMAGE_DELETE_TASK = "Image-Delete";
    private static final String IMAGE_APPROVE_TASK = "Image-Approve";
    private static final String IMAGE_PUBLISH_TASK = "Image-Publish";

    @Autowired
    private IPSIntegrityCheckerDao integrityDao;

    @Autowired
    private IPSDTSStatusProvider dtsStatusProvider;

    @Autowired
    private IPSAssetService assetService;

    @Autowired
    private IPSItemWorkflowService itemWorkflowService;

    @Autowired
    private IPSUserService userService;

    @Autowired
    private IPSUtilityService utilityService;

    @Autowired
    private IPSSiteManager siteMgr;

    @Autowired
    private IPSPubServerService pubServerService;

    private PSRequest request;

    @Override
    public synchronized String start(final IntegrityTaskType type) throws PSDataServiceException {
        ms_log.info("Started integrity checker.");
        validateUsage();
        var status = getRunningStatus();
        request = PSWebserviceUtils.getRequest();
        request = request.cloneRequest();
        if (status == null) {
            status = new PSIntegrityStatus();
            final var token = UUID.randomUUID().toString();
            status.setToken(token);
            status.setStartTime(new Date());
            status.setStatus(Status.RUNNING);
            integrityDao.save(status);
            Runnable r = () -> {
                try {
                    if (PSRequestInfo.isInited()) {
                        PSRequestInfo.resetRequestInfo();
                    }
                    PSRequestInfo.initRequestInfo(request.getServletRequest());
                    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_PSREQUEST, request);
                    runTasks(type);
                } catch (Exception e) {
                    ms_log.info("Error starting the integration tasks", e);
                }
            };
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(r);
        }
        return status.getToken();
    }

    private void runTasks(IntegrityTaskType type) throws IPSGenericDao.SaveException {
        ms_log.info("Started running the tasks.");
        var status = getRunningStatus();
        if (status != null) {
            var tasks = new HashSet<PSIntegrityTask>();
            var topSt = Status.SUCCESS;
            if (type == IntegrityTaskType.all || type == IntegrityTaskType.cm1) {
                try {
                    tasks.addAll(getCmsTasks(status.getToken()));
                } catch (Exception e) {
                    topSt = Status.FAILED;
                    ms_log.error("Error occurred running cms tasks", e);
                }
            }
            if (type == IntegrityTaskType.all || type == IntegrityTaskType.dts) {
                try {
                    tasks.addAll(getDtsTasks(status.getToken()));
                } catch (Exception e) {
                    topSt = Status.FAILED;
                    ms_log.error("Error occurred running dts tasks", e);
                }
            }
            // Roll up the failure status, if any of the tasks fail then we should mark the top status as failed.
            if (topSt == Status.SUCCESS) {
                if (tasks.stream().anyMatch(task -> task.getStatus() == TaskStatus.FAILED)) {
                    topSt = Status.FAILED;
                }
            }
            status.setStatus(topSt);
            status.setTasks(tasks);
            status.setEndTime(new Date());
            integrityDao.save(status);
        }
    }

    private Set<PSIntegrityTask> getDtsTasks(String token) {
        var dtsTasks = new HashSet<PSIntegrityTask>();
        var dtsStatus = dtsStatusProvider.getDTSStatusReport();
        dtsStatus.forEach((name, pair) -> {
            var task = new PSIntegrityTask();
            task.setType(IntegrityTaskType.dts.toString());
            task.setName(name);
            task.setStatus(pair.getFirst());
            task.setMessage(pair.getSecond());
            task.setToken(token);
            dtsTasks.add(task);
        });
        return dtsTasks;
    }

    private Set<PSIntegrityTask> getCmsTasks(String token) {
        var cmsTasks = new HashSet<PSIntegrityTask>();
        var cm1Status = runImageTasks(token);
        cm1Status.forEach((name, pair) -> {
            var task = new PSIntegrityTask();
            task.setType(IntegrityTaskType.cm1.toString());
            task.setName(name);
            task.setStatus(pair.getFirst());
            task.setMessage(pair.getSecond());
            task.setToken(token);
            cmsTasks.add(task);
        });
        cmsTasks.addAll(runImagePublishTasks(token));
        return cmsTasks;
    }

    @Override
    public void stop() throws PSDataServiceException {
        validateUsage();
        var status = getRunningStatus();
        if (status != null) {
            status.setStatus(Status.CANCELLED);
            status.setEndTime(new Date());
            integrityDao.save(status);
        }
    }

    private PSIntegrityStatus getRunningStatus() throws IPSGenericDao.SaveException {
        PSIntegrityStatus result = null;
        var statuses = integrityDao.find(Status.RUNNING);
        if (statuses.size() == 1) {
            result = statuses.get(0);
        } else if (statuses.size() > 1) {
            for (int i = 1; i < statuses.size(); i++) {
                var status = statuses.get(i);
                status.setStatus(Status.CANCELLED);
                integrityDao.save(status);
            }
        }
        return result;
    }

    @Override
    public PSIntegrityStatus getStatus(String token) throws PSDataServiceException {
        validateUsage();
        return integrityDao.find(token);
    }

    @Override
    public List<PSIntegrityStatus> getHistory() throws PSDataServiceException {
        validateUsage();
        return getHistory(null);
    }

    @Override
    public List<PSIntegrityStatus> getHistory(Status status) throws PSDataServiceException {
        validateUsage();
        return integrityDao.find(status);
    }

    @Override
    public void delete(String token) throws PSDataServiceException {
        validateUsage();
        var status = integrityDao.find(token);
        if (status != null) {
            integrityDao.delete(status);
        }
    }

    /**
     * Helper method to run the image tasks.
     *
     * @param token assumed not null
     * @return map of task name and pair of status and message
     */
    private Map<String, PSPair<TaskStatus, String>> runImageTasks(String token) {
        var result = new HashMap<String, PSPair<TaskStatus, String>>();
        PSAsset percussionImage = null;
        try (InputStream in = new FileInputStream(PSServer.getRxDir().getAbsolutePath()
                + PSAmazonS3DeliveryHandler.PERC_TEST_IMG_DIR
                + PSAmazonS3DeliveryHandler.PERC_TEST_IMG)) {
            PSAbstractAssetRequest ar = new PSBinaryAssetRequest(
                    PSAssetPathItemService.ASSET_ROOT + "/uploads",
                    AssetType.IMAGE,
                    PSAmazonS3DeliveryHandler.generateTestImageKey(token),
                    "image/jpeg",
                    in
            );
            percussionImage = assetService.createAsset(ar);
            result.put(IMAGE_CREATE_TASK, new PSPair<>(TaskStatus.SUCCESS, ""));
        } catch (Exception e) {
            result.put(IMAGE_CREATE_TASK, new PSPair<>(TaskStatus.FAILED, PSExceptionUtils.getMessageForLog(e)));
            return result;
        }

        // Approve Image Asset
        try {
            itemWorkflowService.performApproveTransition(percussionImage.getId(), false, "");
            result.put(IMAGE_APPROVE_TASK, new PSPair<>(TaskStatus.SUCCESS, ""));
        } catch (Exception e) {
            result.put(IMAGE_APPROVE_TASK, new PSPair<>(TaskStatus.FAILED, PSExceptionUtils.getMessageForLog(e)));
        }

        // Delete Image from CM1
        try {
            assetService.delete(percussionImage.getId());
            result.put(IMAGE_DELETE_TASK, new PSPair<>(TaskStatus.SUCCESS, ""));
        } catch (Exception e) {
            result.put(IMAGE_DELETE_TASK, new PSPair<>(TaskStatus.FAILED, PSExceptionUtils.getMessageForLog(e)));
        }

        return result;
    }

    private void validateUsage() throws PSDataServiceException {
        if (!userService.isAdminUser(userService.getCurrentUser().getName())) {
            throw new RuntimeException("You are not authorized to use " + PSIntegrityCheckerService.class.getName() + " API.");
        }
        if (!utilityService.isSaaSEnvironment()) {
            throw new RuntimeException("The " + PSIntegrityCheckerService.class.getName() + " API is not supported in your environment.");
        }
    }

    /**
     * Helper method to run the image publish tasks.
     *
     * @param token assumed not null
     * @return set of integrity tasks
     */
    private Set<PSIntegrityTask> runImagePublishTasks(String token) {
        var result = new HashSet<PSIntegrityTask>();
        try {
            var delHandler = new PSAmazonS3DeliveryHandler();
            var sites = siteMgr.findAllSites();
            for (var site : sites) {
                var pubServer = pubServerService.getDefaultPubServer(site.getGUID());
                if (pubServer == null) {
                    continue;
                }
                var pubType = pubServer.getPublishType();
                if (!(equalsIgnoreCase(pubType, PublishType.amazon_s3.toString())
                        || equalsIgnoreCase(pubType, PublishType.amazon_s3_only.toString()))) {
                    continue;
                }
                var taskProps = new HashSet<PSIntegrityTaskProperty>();
                taskProps.add(new PSIntegrityTaskProperty("sitename", site.getName()));
                try {
                    var pub = delHandler.publishTestImage(pubServer, site, token);
                    var task = new PSIntegrityTask();
                    task.setName(IMAGE_PUBLISH_TASK + ":" + site.getName());
                    task.setToken(token);
                    task.setStatus(pub.getFirst() ? TaskStatus.SUCCESS : TaskStatus.FAILED);
                    task.setMessage(pub.getSecond());
                    task.setType(IntegrityTaskType.cm1.toString());
                    task.setTaskProperties(taskProps);
                    result.add(task);
                } catch (Exception e) {
                    var task = createErrorTask(IMAGE_PUBLISH_TASK + ":" + site.getName(), token, IntegrityTaskType.cm1.toString(), e);
                    task.setTaskProperties(taskProps);
                    result.add(task);
                }
            }
        } catch (Exception e) {
            var task = createErrorTask(IMAGE_PUBLISH_TASK, token, IntegrityTaskType.cm1.toString(), e);
            result.add(task);
        }
        return result;
    }

    private PSIntegrityTask createErrorTask(String name, String token, String type, Exception e) {
        var task = new PSIntegrityTask();
        task.setName(name);
        task.setToken(token);
        task.setStatus(TaskStatus.FAILED);
        task.setMessage(e.getLocalizedMessage());
        task.setType(type);
        return task;
    }
}
