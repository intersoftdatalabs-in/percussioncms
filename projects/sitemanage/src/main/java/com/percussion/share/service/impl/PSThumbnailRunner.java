// REFACTORED: CP-JAVA11
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

package com.percussion.share.service.impl;

import com.percussion.error.PSExceptionUtils;
import com.percussion.monitor.process.PSThumbnailProcessMonitor;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestContext;
import com.percussion.share.data.PSPagedItemList;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import com.percussion.system.utils.PSUrlUtils;
import com.percussion.thumbnail.PSScreenCapture;
import com.percussion.thumbnail.PSThumbnailImageUtils;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.service.impl.PSSiteConfigUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Handles thumbnail generation for templates and pages.
 * Sunny Sal says: "Thumbnails so sharp, even your boss will be impressed!"
 */
public class PSThumbnailRunner implements Runnable {

    private static final AtomicInteger activeWorkers = new AtomicInteger(0);
    private static AtomicInteger activeWorkerLimit = new AtomicInteger(1);
    private static final AtomicBoolean shutdownFlag = new AtomicBoolean();
    private static final ConcurrentHashMap<String, Function> inProcess = new ConcurrentHashMap<>();
    private static final String PAGE_STRING = "-page.jpg";
    private static final String TEMPLATE_STRING = "-template.jpg";
    private static final String TPL_IMAGES_DIR = "rx_resources/images/TemplateImages";
    private static final Logger log = LogManager.getLogger(PSThumbnailRunner.class);

    private final IPSSiteTemplateService siteTemplateService;
    private final IPSTemplateService templateService;
    private final IPSPageService pageService;
    private final boolean waitForCompletion;
    private final Map<String, Object> requestInfoMap;
    private Set<Map.Entry<String, String>> sessionParameterMap = null;
    private PSRequestContext requestContext = null;

    public enum Function {
        GENERATE_TEMPLATE_THUMBNAIL,
        DELETE_TEMPLATE_THUMBNAIL,
        GENERATE_PAGE_THUMBNAIL,
        DELETE_PAGE_THUMBNAIL,
        CHECK_FOR_PAGE_THUMBNAIL,
        CHECK_FOR_TEMPLATE_THUMBNAIL
    }

    public PSThumbnailRunner(IPSSiteTemplateService siteTemplateService,
                            IPSTemplateService templateService,
                            IPSPageService pageService,
                            boolean waitForCompletion,
                            Map<String, Object> requestInfoMap) {
        this.siteTemplateService = siteTemplateService;
        this.templateService = templateService;
        this.pageService = pageService;
        this.waitForCompletion = waitForCompletion;
        this.requestInfoMap = requestInfoMap;
    }

    public static void setActiveWorkerLimit(Integer limit) {
        if (limit != null && limit > -1) {
            activeWorkerLimit = new AtomicInteger(limit);
        }
    }

    public static synchronized boolean scheduleThumbnailJob(String id, Function function) {
        if (!shutdownFlag.get() && id != null && !inProcess.containsKey(id)) {
            inProcess.putIfAbsent(id, function);
            PSThumbnailProcessMonitor.incrementCount();
            return true;
        }
        return false;
    }

    private static void completeJob(String id) {
        inProcess.remove(id);
    }

    private static synchronized boolean isWorkAvailable() {
        if (activeWorkers.get() < activeWorkerLimit.get()) {
            activeWorkers.incrementAndGet();
            return true;
        }
        return false;
    }

    private static synchronized void leaveWork() {
        activeWorkers.decrementAndGet();
    }

    /**
     * Clear the process queue and set shutdown flag.
     */
    public static synchronized void shutdown() {
        shutdownFlag.set(true);
        inProcess.clear();
    }

    @Override
    public void run() {
        if (isWorkAvailable()) {
            goToWork();
        }
    }

    private static PSWorkPackage getNextPSWorkPackage() {
        if (inProcess.isEmpty()) return null;
        var idForWork = inProcess.keySet().iterator().next();
        var functionForWork = inProcess.get(idForWork);
        completeJob(idForWork);
        return new PSWorkPackage(idForWork, functionForWork);
    }

    public void generateThumbnailNow(String id, Function function) {
        var workPackage = new PSWorkPackage(id, function);
        this.init();
        performWork(workPackage);
    }

    private void goToWork() {
        this.init();
        var workPackage = getNextPSWorkPackage();
        while (workPackage != null) {
            try {
                if (!shutdownFlag.get())
                    performWork(workPackage);
            } catch (Exception e) {
                completeJob(workPackage.getId());
            } finally {
                PSThumbnailProcessMonitor.decrementCount();
            }
            workPackage = getNextPSWorkPackage();
        }
        leaveWork();
    }

    private void performWork(PSWorkPackage workPackage) {
        try {
            workPackage.setPage(getPage(workPackage));
            workPackage.setTemplate(getTemplate(workPackage));
            workPackage.setSite(getSite(workPackage.getId(), workPackage.getFunction()));
            workPackage.setSiteFolderPath(getSiteFolder(workPackage));
            workPackage.setFileSuffix(getFileSuffix(workPackage.getFunction()));
            switch (workPackage.getFunction()) {
                case GENERATE_PAGE_THUMBNAIL:
                case GENERATE_TEMPLATE_THUMBNAIL:
                    generateThumbnail(workPackage);
                    break;
                case CHECK_FOR_PAGE_THUMBNAIL:
                    checkForPageThumbnail(workPackage);
                    break;
                case CHECK_FOR_TEMPLATE_THUMBNAIL:
                    checkForTemplateThumbnail(workPackage);
                    break;
                case DELETE_PAGE_THUMBNAIL:
                    delete(workPackage);
                    break;
                case DELETE_TEMPLATE_THUMBNAIL:
                    deleteTemplateThumbnail(workPackage);
                    break;
            }
        } catch (PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    private void deleteTemplateThumbnail(PSWorkPackage workPackage) {
        // Not implemented yet
    }

    private void delete(PSWorkPackage workPackage) {
        try {
            var root = new File((PSSiteConfigUtils.getRootDirectory() + "/" + TPL_IMAGES_DIR).replace("\\", "/"));
            var files = FileUtils.listFiles(root, null, true);
            for (var file : files) {
                if (file.getName().contains(workPackage.getId() + TEMPLATE_STRING)
                        || file.getName().contains(workPackage.getId() + PAGE_STRING)) {
                    Files.delete(file.toPath());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to delete thumbnail for Page ID: {} {}", workPackage.getId(), e.getLocalizedMessage(), e);
        }
    }

    private void checkForPageThumbnail(PSWorkPackage workPackage) {
        var imgPath = (PSSiteConfigUtils.getRootDirectory() + "/" + TPL_IMAGES_DIR + '/'
                + workPackage.getSite().getName() + '/' + workPackage.getId() + PAGE_STRING)
                .replace("\\", "/").replace("//", "/");
        var f = new File(imgPath);
        if (!f.exists()) {
            workPackage.setFunction(Function.GENERATE_PAGE_THUMBNAIL);
            scheduleThumbnailJob(workPackage.getId(), workPackage.getFunction());
        }
    }

    private void checkForTemplateThumbnail(PSWorkPackage workPackage) {
        // Not implemented yet
    }

    private void generateThumbnail(PSWorkPackage workPackage) {
        if (workPackage.getPage() == null) return;
        try {
            if (isTemplateFunction(workPackage.getFunction())) {
                if (!"Unassigned".equalsIgnoreCase(workPackage.getTemplate().getName())) {
                    buildThumbnailsForTemplatesPages(workPackage);
                    handleThumbnailGeneration(workPackage.getId(),
                            workPackage.getFunction(),
                            workPackage.getFileSuffix(),
                            workPackage.getSiteFolderPath(), workPackage.getPage());
                }
            } else {
                handleThumbnailGeneration(workPackage.getId(),
                        workPackage.getFunction(),
                        workPackage.getFileSuffix(),
                        workPackage.getSiteFolderPath(), workPackage.getPage());
            }
        } catch (Exception e) {
            log.warn("Failed to generate thumbnail for id: {} {} failure occurred in generateThumbnail", workPackage.getId(), e.getLocalizedMessage(), e);
        }
    }

    // ===================================================================================================
    // Thumbnail Specific Methods
    // ===================================================================================================
    private void handleThumbnailGeneration(String id, Function function,
                                           String fileSuffix, String siteFolder, PSPage page) throws MalformedURLException {
        if (page == null) return;
        var thumbnailFilePath = new File(siteFolder + id + fileSuffix).getAbsolutePath();
        var path = (page.getFolderPath() + "/" + page.getName()).replace("//", "/");
        var url = PSUrlUtils.createUrl("127.0.0.1", null, path,
                sessionParameterMap.iterator(), null, requestContext, true);
        try {
            PSScreenCapture.takeCapture(url.toString(), thumbnailFilePath);
            PSThumbnailImageUtils.resizeThumbnail(thumbnailFilePath);
        } catch (Exception e) {
            log.error("Thumbnail Exception: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        var thumbNail = new File(thumbnailFilePath);
        if (!thumbNail.exists()) {
            try {
                PSScreenCapture.generateEmptyThumb(thumbnailFilePath);
            } catch (Exception e1) {
                log.error("Thumbnail Exception for empty thumb: {}", e1.getMessage());
                log.debug(e1);
            }
        }
    }

    private void buildThumbnailsForTemplatesPages(PSWorkPackage workPackage) throws PSDataServiceException {
        if (!"Unassigned".equals(workPackage.getTemplate().getName())
                && workPackage.getFunction() == Function.GENERATE_TEMPLATE_THUMBNAIL) {
            var pages = pageService.findPagesByTemplate(workPackage.getTemplate().getId(), 1, 10000, null, null, null);
            var pagePaths = pages.getChildrenInPage();
            int numPages = pagePaths.size();
            if (numPages > 0)
                PSThumbnailProcessMonitor.incrementCount(numPages);

            for (var pagePath : pagePaths) {
                try {
                    var thisPage = pageService.find(pagePath.getId());
                    scheduleThumbnailJob(thisPage.getId(), Function.GENERATE_PAGE_THUMBNAIL);
                } catch (Exception e) {
                    log.debug("Unable to generate thumbnail for a template's child page with id: {} {}", pagePath.getId(), e.getLocalizedMessage(), e);
                    var imagePath = workPackage.getSiteFolderPath() + pagePath.getId() + PAGE_STRING;
                    var thumbNail = new File(imagePath);
                    if (!thumbNail.exists()) {
                        try {
                            PSScreenCapture.generateEmptyThumb(imagePath);
                        } catch (Exception e1) {
                            log.error(ExceptionUtils.getStackFrames(e1));
                        }
                    }
                } finally {
                    if (numPages > 0)
                        PSThumbnailProcessMonitor.decrementCount(numPages);
                }
            }
        }
    }

    // ===================================================================================================
    // INITIALIZATION METHODS
    // ===================================================================================================

    private void init() {
        try {
            initializeRequest();
            initSessionVariablesForUrlAssembly();
        } catch (Exception e) {
            log.debug("Thumbnail Service Runner initialization failure for ID: successful thumbnail generation unlikely");
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    private void initSessionVariablesForUrlAssembly() {
        var request = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
        var sessionId = request.getUserSessionId();
        requestContext = new PSRequestContext(request);
        var paramMap = new HashMap<String, String>();
        paramMap.put(IPSHtmlParameters.SYS_SESSIONID, sessionId);
        sessionParameterMap = paramMap.entrySet();
    }

    private String getSiteFolder(PSWorkPackage work) throws IPSDataService.DataServiceLoadException, IPSDataService.DataServiceNotFoundException, PSValidationException {
        if (work.getSite() == null)
            work.setSite(getSite(work.getId(), work.getFunction()));
        var siteFolder = PSSiteConfigUtils.getRootDirectory()
                + "/rx_resources/images/TemplateImages/"
                + work.getSite().getName() + "/";
        var f = new File(siteFolder);
        if (!f.exists()) {
            f.mkdir();
        }
        return siteFolder;
    }

    private void initializeRequest() {
        if (PSRequestInfo.isInited()) {
            PSRequestInfo.resetRequestInfo();
        }
        PSRequestInfo.initRequestInfo(requestInfoMap);
    }

    private boolean isTemplateFunction(Function function) {
        return function == Function.GENERATE_TEMPLATE_THUMBNAIL
                || function == Function.CHECK_FOR_TEMPLATE_THUMBNAIL
                || function == Function.DELETE_TEMPLATE_THUMBNAIL;
    }

    private boolean isPageFunction(Function function) {
        return function == Function.GENERATE_PAGE_THUMBNAIL
                || function == Function.CHECK_FOR_PAGE_THUMBNAIL
                || function == Function.DELETE_PAGE_THUMBNAIL;
    }

    private PSTemplateSummary getTemplate(PSWorkPackage workPackage) throws PSDataServiceException {
        if (workPackage.getTemplate() != null) return workPackage.getTemplate();
        if (isTemplateFunction(workPackage.getFunction())) {
            return templateService.find(workPackage.getId());
        } else if (isPageFunction(workPackage.getFunction())) {
            if (workPackage.getPage() == null)
                workPackage.setPage(pageService.find(workPackage.getId()));
            return templateService.find(workPackage.getPage().getTemplateId());
        }
        return null;
    }

    private PSPage getPage(PSWorkPackage workPackage) throws PSDataServiceException {
        if (workPackage.getPage() != null) return workPackage.getPage();
        if (isPageFunction(workPackage.getFunction())) {
            return pageService.find(workPackage.getId());
        } else if (isTemplateFunction(workPackage.getFunction())) {
            var template = templateService.find(workPackage.getId());
            var pages = pageService.findPagesByTemplate(template.getId(), 1, 2, null, null, null);
            var pagePaths = pages.getChildrenInPage();
            if (pages.getChildrenCount() > 0) {
                return pageService.find(pagePaths.get(0).getId());
            }
        }
        return null;
    }

    private String getFileSuffix(Function function) {
        if (isPageFunction(function)) {
            return PAGE_STRING;
        } else if (isTemplateFunction(function)) {
            return TEMPLATE_STRING;
        }
        return null;
    }

    private PSSiteSummary getSite(String id, Function function) throws IPSDataService.DataServiceLoadException, IPSDataService.DataServiceNotFoundException, PSValidationException {
        var templateId = id;
        if (isPageFunction(function)) {
            templateId = pageService.find(id).getTemplateId();
        }
        var sites = siteTemplateService.findSitesByTemplate(templateId);
        return sites.isEmpty() ? null : sites.get(0);
    }
}
