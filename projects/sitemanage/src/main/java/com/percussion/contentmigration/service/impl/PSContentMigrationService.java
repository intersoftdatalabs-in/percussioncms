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

package com.percussion.contentmigration.service.impl;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSAssetDropCriteria;
import com.percussion.assetmanagement.data.PSAssetWidgetRelationship;
import com.percussion.assetmanagement.data.PSOrphanedAssetSummary;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.assetmanagement.service.impl.PSPreviewPageUtils;
import com.percussion.contentmigration.converters.IPSContentMigrationConverter;
import com.percussion.contentmigration.rules.IPSContentMigrationRule;
import com.percussion.contentmigration.service.IPSContentMigrationService;
import com.percussion.contentmigration.service.PSContentMigrationException;
import com.percussion.error.PSExceptionUtils;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSPageTemplateService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.queue.IPSPageImportQueue;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.IPSNameGenerator;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.utils.service.impl.PSJsoupUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PSContentMigrationService implements IPSContentMigrationService {
    //Other services
    private IPSRenderAssemblyBridge renderAssemblyBridge;
    private IPSPageService pageService;
    private IPSTemplateService templateService;
    private IPSAssetService assetService;
    private IPSItemWorkflowService itemWorkflowService;
    private IPSNameGenerator nameGenerator;
    private IPSPageImportQueue pageImportQueue;
    private IPSGuidManager guidMgr;
    //Memebers
    private List<IPSContentMigrationRule> migrationRules;
    private List<IPSContentMigrationConverter> migrationConverters;
    private Map<String, IPSContentMigrationConverter> converterMap = new HashMap<>();
    private IPSPageTemplateService pageTemplateService;


    public PSContentMigrationService(
            IPSPageService pageService,
            IPSRenderAssemblyBridge renderAssemblyBridge,
            IPSTemplateService templateService,
            IPSAssetService assetService,
            IPSItemWorkflowService itemWorkflowService,
            IPSNameGenerator nameGenerator,
            IPSPageImportQueue pageImportQueue,
            IPSPageTemplateService pageTemplateService) {
        this.pageService = pageService;
        this.renderAssemblyBridge = renderAssemblyBridge;
        this.templateService = templateService;
        this.assetService = assetService;
        this.itemWorkflowService = itemWorkflowService;
        this.nameGenerator = nameGenerator;
        this.pageImportQueue = pageImportQueue;
        this.guidMgr = PSGuidManagerLocator.getGuidMgr();
        this.pageTemplateService = pageTemplateService;
    }

    @Override
    public void migrateContent(String siteName, String templateId, String refPageId, List<String> pageIds)
            throws PSContentMigrationException, PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException {
        Validate.notEmpty(templateId, "templateId must not be empty for content migration");
        Validate.notEmpty(pageIds, "newPageIds must not be empty for content migration");
        migrateContentOnTemplateChange(templateId, refPageId, pageIds);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(siteName)) {
            for (var pageId : pageIds) {
                pageImportQueue.removeImportPage(siteName, pageId);
            }
        }
    }

    @Override
    public void migrateContentOnTemplateChange(String templateId, String referencePageId, List<String> newPageIds)
            throws PSContentMigrationException, PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException {
        var failedItems = new HashMap<String, String>();
        var refDoc = getReferenceDocument(templateId, referencePageId);
        var template = templateService.load(templateId);
        for (var pageId : newPageIds) {
            if (itemWorkflowService.isCheckedOutToSomeoneElse(pageId)) {
                failedItems.put(pageId, "Failed to process, the page is being edited by someone else.");
                continue;
            }
            boolean checkedOut = false;
            if (!itemWorkflowService.isCheckedOutToCurrentUser(pageId)) {
                try {
                    itemWorkflowService.checkOut(pageId);
                    checkedOut = true;
                } catch (IPSItemWorkflowService.PSItemWorkflowServiceException e) {
                    log.warn(PSExceptionUtils.getMessageForLog(e));
                }
            }
            pageTemplateService.changeTemplate(pageId, templateId);
            var applicableWidgets = findEmptyWidgets(templateId, pageId);
            if (applicableWidgets.isEmpty()) {
                log.debug("Could not find any applicable widgets, skipping migration process.");
            } else {
                var page = pageService.load(pageId);
                findMatchingContent(page, template, refDoc, applicableWidgets);
                updatePage(pageId, templateId, applicableWidgets);
            }
            if (checkedOut) {
                try {
                    itemWorkflowService.checkIn(pageId);
                } catch (IPSItemWorkflowService.PSItemWorkflowServiceException e) {
                    log.warn(PSExceptionUtils.getMessageForLog(e));
                }
            }
        }
        if (!failedItems.isEmpty()) {
            var cme = new PSContentMigrationException();
            cme.setFailedItems(failedItems);
            throw cme;
        }
    }

    @Override
    public void migrateSameTemplateChanges(String templateId, List<String> pageIds)
            throws PSContentMigrationException, PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException {
        var failedItems = new HashMap<String, String>();
        var refDoc = getReferenceDocument(templateId, null);
        if (pageIds == null) {
            pageIds = getTemplatePages(templateId);
        }
        var template = templateService.load(templateId);
        for (var pageId : pageIds) {
            var page = pageService.load(pageId);
            if (template.getContentMigrationVersion().equals(page.getTemplateContentMigrationVersion())) {
                log.info("Both template {} and page {} have same version, skipping content migration.", templateId, pageId);
            }
            if (itemWorkflowService.isCheckedOutToSomeoneElse(pageId)) {
                failedItems.put(pageId, "Failed to process, the page is being edited by someone else.");
                continue;
            }
            boolean checkedOut = false;
            if (!itemWorkflowService.isCheckedOutToCurrentUser(pageId)) {
                try {
                    itemWorkflowService.checkOut(pageId);
                    checkedOut = true;
                } catch (IPSItemWorkflowService.PSItemWorkflowServiceException e) {
                    log.warn(PSExceptionUtils.getMessageForLog(e));
                }
            }
            var applicableWidgets = findEmptyWidgets(templateId, pageId);
            if (!applicableWidgets.isEmpty()) {
                findMatchingContent(page, template, refDoc, applicableWidgets);
                updatePage(pageId, templateId, applicableWidgets);
            } else {
                log.debug("Could not find any applicable widgets, skipping migration process for page {}", pageId);
                pageService.updateMigrationEmptyWidgetFlag(pageId, false);
            }
            pageService.updateTemplateMigrationVersion(pageId);
            if (checkedOut) {
                try {
                    itemWorkflowService.checkIn(pageId);
                } catch (IPSItemWorkflowService.PSItemWorkflowServiceException e) {
                    log.warn(PSExceptionUtils.getMessageForLog(e));
                }
            }
        }
        if (!failedItems.isEmpty()) {
            var cme = new PSContentMigrationException();
            cme.setFailedItems(failedItems);
            throw cme;
        }
    }

    @Override
    public List<String> getTemplatePages(String templateId) throws IPSPageService.PSPageException {
        var pageIds = new ArrayList<String>();
        var pgIds = pageTemplateService.findPageIdsByTemplate(templateId);
        for (var pgId : pgIds) {
            pageIds.add(guidMgr.makeGuid(pgId, PSTypeEnum.LEGACY_CONTENT).toString());
        }
        return pageIds;
    }

    private String getUnUsedContent(PSOrphanedAssetSummary unusedAsset) throws PSDataServiceException {
        var asset = assetService.load(unusedAsset.getId());
        String content = null;
        if ("percRawHtmlAsset".equalsIgnoreCase(asset.getType())) {
            content = (String) asset.getFields().get("html");
        } else if ("percRichTextAsset".equalsIgnoreCase(asset.getType())) {
            content = (String) asset.getFields().get("text");
        }
        return content;
    }

    private void findMatchingContent(PSPage page, PSTemplate template, org.jsoup.nodes.Document refDoc, List<ApplicableWidget> applicableWidgets)
            throws IPSDataService.DataServiceLoadException, IPSDataService.DataServiceNotFoundException {
        var unusedDocuments = new ArrayList<org.jsoup.nodes.Document>();
        var unusedAssets = PSPreviewPageUtils.getOrphanedAssetsSummaries(page, template);
        for (var unusedAsset : unusedAssets) {
            String content = null;
            try {
                content = getUnUsedContent(unusedAsset);
            } catch (PSDataServiceException e) {
                log.error(PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
            if (org.apache.commons.lang3.StringUtils.isNotBlank(content)) {
                var doc = org.jsoup.Jsoup.parseBodyFragment(content);
                unusedDocuments.add(doc);
            }
        }
        for (var unusedDocument : unusedDocuments) {
            findMatchingContent(refDoc, unusedDocument, applicableWidgets);
        }
    }

    private org.jsoup.nodes.Document getReferenceDocument(String templateId, String refPageId) throws PSContentMigrationException {
        org.jsoup.nodes.Document refDoc;
        try {
            if (org.apache.commons.lang3.StringUtils.isNotBlank(refPageId)) {
                var refPageHtml = renderAssemblyBridge.renderPage(refPageId, true, false);
                refDoc = org.jsoup.Jsoup.parseBodyFragment(refPageHtml);
            } else {
                var refTemplateHtml = renderAssemblyBridge.renderTemplate(templateId, true);
                refDoc = org.jsoup.Jsoup.parseBodyFragment(refTemplateHtml);
            }
        } catch (Exception e) {
            log.error(e);
            throw new PSContentMigrationException("Failed to migrate content, see log for more details.");
        }
        return refDoc;
    }

    private List<ApplicableWidget> findEmptyWidgets(String templateId, String pageId) throws PSDataServiceException {
        var tplAssetDropCriteria = assetService.getWidgetAssetCriteria(templateId, false);
        var applicableWidgets = new ArrayList<ApplicableWidget>();
        var tplWidgetIds = new ArrayList<String>();
        var tplContentWidgetIds = new ArrayList<String>();
        for (var adc : tplAssetDropCriteria) {
            tplWidgetIds.add(adc.getWidgetId());
            if (adc.getExistingAsset()) {
                tplContentWidgetIds.add(adc.getWidgetId());
            }
        }
        var pageAssetDropCriteria = assetService.getWidgetAssetCriteria(pageId, true);
        for (var adc : pageAssetDropCriteria) {
            if (tplContentWidgetIds.contains(adc.getWidgetId())) {
                continue;
            }
            var wc = new ArrayList<>(converterMap.keySet());
            wc.retainAll(adc.getSupportedCtypes());
            if (!adc.getExistingAsset() && converterMap.size() == wc.size() + adc.getSupportedCtypes().size()) {
                applicableWidgets.add(createApplicableWidget(adc, null, !tplWidgetIds.contains(adc.getWidgetId())));
            }
        }
        return applicableWidgets;
    }

    private ApplicableWidget createApplicableWidget(PSAssetDropCriteria adc, org.jsoup.nodes.Document refDoc, boolean isPageWidget) {
        var applicableWidget = new ApplicableWidget();
        applicableWidget.widgetId = adc.getWidgetId();
        if (refDoc != null) {
            var regElem = PSJsoupUtils.closestParentByClass(
                    refDoc,
                    PSJsoupUtils.generateAttributeSelector(IPSContentMigrationRule.ATTR_WIDGET_ID, adc.getWidgetId()),
                    IPSContentMigrationRule.CLASS_PERC_REGION);
            applicableWidget.regionId = regElem.id();
        }
        applicableWidget.widgetDefId = adc.getSupportedCtypes().get(0);
        applicableWidget.isPageWidget = isPageWidget;
        return applicableWidget;
    }

    private void findMatchingContent(org.jsoup.nodes.Document refDoc, org.jsoup.nodes.Document targetPageDoc, List<ApplicableWidget> applicableWidgets) {
        for (var widget : applicableWidgets) {
            if (widget.fields == null) {
                for (var rule : migrationRules) {
                    var content = rule.findMatchingContent(widget.widgetId, refDoc, targetPageDoc);
                    if (content != null) {
                        var converter = converterMap.get(widget.widgetDefId);
                        var fields = converter.convert(content);
                        widget.fields = fields;
                        break;
                    }
                }
            }
        }
    }

    private void updatePage(String pageId, String templateId, List<ApplicableWidget> applicableWidgets)
            throws PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException {
        boolean hasEmptyWidgets = false;
        for (var widget : applicableWidgets) {
            if (widget.isPageWidget) {
                // TODO: Create a widget on page in the specified region
            }
            if (widget.fields != null && !widget.fields.isEmpty()) {
                createAndAssociateAsset(pageId, widget);
            } else {
                hasEmptyWidgets = true;
            }
        }
        pageService.updateMigrationEmptyWidgetFlag(pageId, hasEmptyWidgets);
    }

    @Override
    public void createAndAssociateAsset(String pageId, ApplicableWidget applicableWidget)
            throws PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException {
        var asset = new PSAsset();
        asset.setType(converterMap.get(applicableWidget.widgetDefId).getWidgetContentType());
        var newName = nameGenerator.generateLocalContentName();
        asset.setName(newName);
        var fields = asset.getFields();
        fields.putAll(applicableWidget.fields);
        fields.put(IPSHtmlParameters.SYS_WORKFLOWID, "" + itemWorkflowService.getLocalContentWorkflowId());
        fields.put(IPSHtmlParameters.SYS_TITLE, newName);
        var newAsset = assetService.save(asset);
        var awRel = new PSAssetWidgetRelationship(
                pageId,
                Long.parseLong(applicableWidget.widgetId),
                applicableWidget.widgetDefId,
                newAsset.getId(),
                0
        );
        assetService.createAssetWidgetRelationship(awRel);
    }

    private static final Logger log = LogManager.getLogger(PSContentMigrationService.class);

    private class ApplicableWidget {
        String widgetId;
        String regionId;
        String widgetDefId;
        boolean isPageWidget;
        Map<String, Object> fields;
    }

    public List<IPSContentMigrationRule> getMigrationRules() {
        return migrationRules;
    }

    public void setMigrationRules(List<IPSContentMigrationRule> migrationRules) {
        this.migrationRules = migrationRules;
    }

    public List<IPSContentMigrationConverter> getMigrationConverters() {
        return migrationConverters;
    }

    public void setMigrationConverters(List<IPSContentMigrationConverter> migrationConverters) {
        this.migrationConverters = migrationConverters;
        for (var converter : migrationConverters) {
            converterMap.put(converter.getWidgetContentType(), converter);
        }
    }
}
