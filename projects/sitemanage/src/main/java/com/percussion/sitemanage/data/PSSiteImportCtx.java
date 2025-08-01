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
package com.percussion.sitemanage.data;

import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitesummaryservice.service.IPSSiteImportSummaryService;
import com.percussion.theme.data.PSThemeSummary;

import java.util.Map;
import java.util.Optional;

/**
 * Context for site import operations.
 */
public class PSSiteImportCtx {

    private String siteUrl;
    private PSSite site;
    private IPSSiteImportLogger logger;
    private IPSSiteImportSummaryService summaryService;
    private Map<IPSSiteImportSummaryService.SiteImportSummaryTypeEnum, Integer> summaryStats;
    private PSThemeSummary themeSummary;
    private String themesRootDirectory;
    private String templateId;
    private String pageName;
    private String catalogedPageId;
    private String templateName;
    private String statusMessagePrefix;
    private String userAgent;
    private boolean isCanceled = false;
    private PSSiteImportConfiguration importConfiguration;

    public Optional<String> getSiteUrl() {
        return Optional.ofNullable(siteUrl);
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public Optional<PSSite> getSite() {
        return Optional.ofNullable(site);
    }

    public void setSite(PSSite site) {
        this.site = site;
    }

    /**
     * Set logger on the context.
     *
     * @param logger The logger, never null.
     */
    public void setLogger(IPSSiteImportLogger logger) {
        this.logger = logger;
    }

    /**
     * Get the current logger.
     *
     * @return The logger, never null.
     * @throws IllegalStateException if no logger has been set.
     */
    public IPSSiteImportLogger getLogger() {
        if (logger == null) {
            throw new IllegalStateException("logger has not been set");
        }
        return logger;
    }

    public Optional<PSSiteImportConfiguration> getImportConfiguration() {
        return Optional.ofNullable(importConfiguration);
    }

    public void setImportConfiguration(PSSiteImportConfiguration importConfiguration) {
        this.importConfiguration = importConfiguration;
    }

    public Optional<IPSSiteImportSummaryService> getSummaryService() {
        return Optional.ofNullable(summaryService);
    }

    public void setSummaryService(IPSSiteImportSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    public Optional<PSThemeSummary> getThemeSummary() {
        return Optional.ofNullable(themeSummary);
    }

    public void setThemeSummary(PSThemeSummary themeSummary) {
        this.themeSummary = themeSummary;
    }

    public Optional<String> getThemesRootDirectory() {
        return Optional.ofNullable(themesRootDirectory);
    }

    public void setThemesRootDirectory(String themesRootDirectory) {
        this.themesRootDirectory = themesRootDirectory;
    }

    /**
     * Get the id of the template if one was created during the import process.
     *
     * @return The id, or empty if a template was not created.
     */
    public Optional<String> getTemplateId() {
        return Optional.ofNullable(templateId);
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public Optional<String> getPageName() {
        return Optional.ofNullable(pageName);
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public Optional<String> getTemplateName() {
        return Optional.ofNullable(templateName);
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Optional<String> getStatusMessagePrefix() {
        return Optional.ofNullable(statusMessagePrefix);
    }

    public void setStatusMessagePrefix(String statusMessagePrefix) {
        this.statusMessagePrefix = statusMessagePrefix;
    }

    public Optional<String> getUserAgent() {
        return Optional.ofNullable(userAgent);
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Used when importing cataloged pages. Is the id of the page being imported.
     *
     * @return Optional page id.
     */
    public Optional<String> getCatalogedPageId() {
        return Optional.ofNullable(catalogedPageId);
    }

    public void setCatalogedPageId(String catalogedPageId) {
        this.catalogedPageId = catalogedPageId;
    }

    public void setCanceled(boolean cancelFlag) {
        isCanceled = cancelFlag;
    }

    /**
     * Determines if the current import process has been canceled.
     *
     * @return true if the import process has been canceled.
     */
    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     * @return May be null if not set.
     */
    public Optional<Map<IPSSiteImportSummaryService.SiteImportSummaryTypeEnum, Integer>> getSummaryStats() {
        return Optional.ofNullable(summaryStats);
    }

    public void setSummaryStats(Map<IPSSiteImportSummaryService.SiteImportSummaryTypeEnum, Integer> summaryStats) {
        this.summaryStats = summaryStats;
    }
}
