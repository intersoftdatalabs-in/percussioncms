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

package com.percussion.pagemanagement.service.impl;

import static com.percussion.share.service.exception.PSParameterValidationUtils.rejectIfBlank;
import static com.percussion.share.service.exception.PSParameterValidationUtils.validateParameters;
import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.getWebApplicationContext;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.noNullElements;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.apache.commons.lang3.math.NumberUtils.toInt;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.cms.IPSConstants;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.dao.IPSTemplateDao;
import com.percussion.pagemanagement.dao.IPSWidgetDao;
import com.percussion.pagemanagement.dao.impl.PSHtmlMetadataUtils;
import com.percussion.pagemanagement.data.PSHtmlMetadata;
import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSRegionTree;
import com.percussion.pagemanagement.data.PSRegionTreeUtils;
import com.percussion.pagemanagement.data.PSRegionWidgetAssociations;
import com.percussion.pagemanagement.data.PSRegionWidgets;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplate.PSTemplateTypeEnum;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.pagemanagement.parser.PSTemplateRegionParser;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pagemanagement.service.IPSWidgetService;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.dao.IPSGenericDao.LoadException;
import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSBeanValidationUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSSpringValidationException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrors;
import com.percussion.sitemanage.service.IPSSiteSectionService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import com.percussion.webservices.content.IPSContentDesignWs;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Will CRUD templates using the {@link IPSTemplateDao}.
 *
 * @author YuBingChen
 * @author adamgent
 */
@Component("sys_templateService")
@Lazy
@Transactional(noRollbackFor = Exception.class)
public class PSTemplateService implements IPSTemplateService {

  private IPSTemplateDao templateDao;
  private IPSWidgetAssetRelationshipService widgetAssetRelationshipService;
  private IPSPageDao pageDao;
  private IPSPageDaoHelper pageDaoHelper;
  private IPSWorkflowHelper workflowHelper;

  @Value("${templateService.validatingDeleteTemplate:true}")
  private boolean validatingDeleteTemplate = true;

  private PSAbstractTemplateSorter templateSorter = new PSTemplateSorter();
  private IPSWidgetDao widgetDao;
  private IPSAssemblyService assemblyService;
  private IPSIdMapper idMapper;
  private final IPSContentDesignWs contentDesignWs;

  /**
   * Instance of site section service, this service is not autowired by Spring during the
   * constructor. Avoid using it directly, instead use {@link #getSiteSectionService()}.
   */
  private IPSSiteSectionService siteSectionService;

  public IPSSiteSectionService getSiteSectionService() {
    if (siteSectionService == null) {
      siteSectionService =
          (IPSSiteSectionService) getWebApplicationContext().getBean("siteSectionService");
    }
    return siteSectionService;
  }

  @Autowired
  public PSTemplateService(
      @Lazy IPSTemplateDao templateDao,
      @Lazy IPSWidgetAssetRelationshipService widgetAssetRelationshipService,
      @Lazy IPSPageDao pageDao,
      @Lazy IPSPageDaoHelper pageDaoHelper,
      IPSWidgetService widgetService,
      IPSWorkflowHelper workflowHelper,
      IPSWidgetDao widgetDao,
      IPSAssemblyService assemblyService,
      IPSIdMapper idMapper,
      IPSContentDesignWs contentDesignWs) {
    super();
    this.templateDao = templateDao;
    this.widgetAssetRelationshipService = widgetAssetRelationshipService;
    this.pageDao = pageDao;
    this.pageDaoHelper = pageDaoHelper;
    this.workflowHelper = workflowHelper;
    this.regionWidgetAssocationsValidator = new RegionWidgetValidator(widgetService);
    this.widgetDao = widgetDao;
    this.assemblyService = assemblyService;
    this.idMapper = idMapper;
    this.contentDesignWs = contentDesignWs;
  }

  @Deprecated
  public PSTemplateSummary createTemplate(String name, String srcId) throws PSDataServiceException {
    return createTemplate(name, srcId, null);
  }

  public PSTemplateSummary createTemplate(String name, String srcId, String siteId)
      throws PSDataServiceException {
    return createTemplate(name, srcId, siteId, null);
  }

  public PSTemplateSummary createTemplate(
      String name, String srcId, String siteId, PSTemplateTypeEnum type)
      throws PSDataServiceException {
    validateParameters("createTemplate")
        .rejectIfBlank("name", name)
        .rejectIfBlank("sourceTemplateId", srcId)
        .throwIfInvalid();
    var template = templateDao.createTemplate(name, srcId);
    validate(template);
    updateBodyMarkupOrRegionTree(template);
    if (type != null) {
      template.setType(type.getLabel());
    }
    template = templateDao.save(template, siteId);

    // Base (assembly) templates are read-only; skip relationship copy without a full reload.
    // A find() here can fail with a null Hibernate connection after contentWs.saveItems runs
    // workflow JDBC on a separate connection (site create path).
    try {
      var srcTemplate = templateDao.find(srcId);
      if (srcTemplate != null && !srcTemplate.isReadOnly()) {
        widgetAssetRelationshipService.copyAssetWidgetRelationships(srcId, template.getId());
      }
    } catch (Exception e) {
      log.debug(
          "Skip copyAssetWidgetRelationships for srcId={} after createTemplate: {}",
          srcId,
          e.toString());
    }
    return template;
  }

  public void delete(String id) throws PSDataServiceException, PSNotFoundException {
    validateParameters("delete").rejectIfBlank("id", id).throwIfInvalid();
    delete(id, false);
  }

  public void delete(String id, boolean force) throws PSDataServiceException, PSNotFoundException {
    var builder = validateParameters("delete").rejectIfBlank("id", id).throwIfInvalid();
    var template = load(id);
    if (!force) {
      var errorMsg =
          "Template '" + template.getName() + "' cannot be deleted because it is being used by ";
      if (isAssociatedToPages(id)) {
        errorMsg += "one or more pages.";
        log.debug("{} Template id: {}", errorMsg, id);
        if (isValidatingDeleteTemplate()) {
          builder.reject("template.inUse", errorMsg);
          builder.throwIfInvalid();
        }
      } else if (isAssociatedToBlogs(id)) {
        errorMsg += "a blog.";
        log.debug("{} Template id: {}", errorMsg, id);
        if (isValidatingDeleteTemplate()) {
          builder.reject("template.inUse", errorMsg);
          builder.throwIfInvalid();
        }
      }
    }
    pageDaoHelper.replaceTemplateForPageInOlderRevisions(id);
    templateDao.remove(id);
  }

  private boolean isAssociatedToBlogs(String templateId)
      throws PSDataServiceException, PSNotFoundException {
    var blogTemplates = getSiteSectionService().findAllTemplatesUsedByBlogs(null);
    return blogTemplates.stream().anyMatch(templateId::equals);
  }

  public PSTemplateSummary find(String id) throws PSDataServiceException {
    rejectIfBlank("find", "id", id);
    var t = templateDao.find(id);
    return fullToSum(t);
  }

  /**
   * @deprecated This is used by unit test only. It cannot be used by production code
   */
  @Deprecated
  public PSTemplateSummary findUserTemplateByName_UsedByUnitTestOnly(String name)
      throws PSDataServiceException {
    rejectIfBlank("findUserTemplateByName", "name", name);
    var t = templateDao.findUserTemplateByName_UsedByUnitTestOnly(name);
    if (t == null) throw new DataServiceLoadException("Failed to find template with name: " + name);
    return fullToSum(t);
  }

  public IPSGuid findUserTemplateIdByName(String templateName, String siteName)
      throws PSValidationException, DataServiceLoadException {
    rejectIfBlank("findUserTemplateByNameAndSite", "templateName", templateName);
    rejectIfBlank("findUserTemplateByNameAndSite", "siteName", siteName);
    var templateGuid = templateDao.findUserTemplateIdByName(templateName, siteName);
    if (templateGuid == null)
      throw new DataServiceLoadException("Failed to find template with name: " + templateName);
    return templateGuid;
  }

  private PSTemplateSummary fullToSum(PSTemplate t) {
    if (t == null) return null;
    var ts = new PSTemplateSummary();
    PSSerializerUtils.copyFullToSummary(t, ts);
    return ts;
  }

  public List<PSTemplateSummary> findAll() throws LoadException, PSTemplateException {
    return sort(templateDao.findAllSummaries());
  }

  public List<PSTemplateSummary> findAll(String siteName)
      throws LoadException, PSTemplateException {
    return sort(templateDao.findAllSummaries(siteName));
  }

  public List<PSTemplateSummary> findAllUserTemplates() throws PSTemplateException {
    return sort(templateDao.findAllUserTemplateSummariesByType(PSTemplateTypeEnum.NORMAL));
  }

  public List<PSTemplateSummary> loadUserTemplateSummaries(List<String> ids, String siteName)
      throws PSTemplateException {
    return sort(templateDao.loadUserTemplateSummaries(ids, siteName));
  }

  public List<PSTemplateSummary> findBaseTemplates(String type) {
    return sort(templateDao.findBaseTemplates(type));
  }

  public List<PSTemplateSummary> sort(List<PSTemplateSummary> items) {
    return templateSorter.sort(items);
  }

  public PSTemplate load(String id) throws PSDataServiceException {
    rejectIfBlank("load", "id", id);
    return templateDao.find(id);
  }

  public String getTemplateThumbPath(PSTemplateSummary summary, String siteName) {
    return templateDao.getTemplateThumbPath(summary, siteName);
  }

  public PSTemplate save(PSTemplate object) throws PSDataServiceException {
    return save(object, null);
  }

  public PSTemplate save(PSTemplate object, String siteId) throws PSDataServiceException {
    return save(object, siteId, null);
  }

  @Override
  public PSTemplate save(PSTemplate object, String siteId, String pageId)
      throws PSDataServiceException {
    log.debug("Saving template");
    validate(object);

    // If valid page id is supplied, then we'll bump the template's content migration revision
    var incrementRevision = false;
    if (pageId != null) {
      if (!isValidPageId(pageId))
        throw new DataServiceSaveException(
            "Page must exist and be checked out to the current user");
      incrementRevision = true;
    }

    updateBodyMarkupOrRegionTree(object);
    updateMetaData(object);

    var template = templateDao.find(object.getId());
    updateRevision(template, object, incrementRevision);

    var savedTemplate = templateDao.save(object, siteId);

    var id = savedTemplate.getId();

    // Remove assets for the deleted widgets
    widgetAssetRelationshipService.removeAssetWidgetRelationships(id, savedTemplate.getWidgets());

    // Transition shared assets to Pending
    workflowHelper.transitionToPending(widgetAssetRelationshipService.getSharedAssets(id));

    // Update widget names (only if there was a change)
    var changedWidgets = getWidgetNamesChanged(template, savedTemplate);
    if (!changedWidgets.isEmpty()) {
      widgetAssetRelationshipService.updateWidgetsNames(id, changedWidgets);
    }

    return savedTemplate;
  }

  private void updateRevision(PSTemplate current, PSTemplate toSave, boolean increment) {
    // No version or bad version interpreted as 0
    var version = toInt(current.getContentMigrationVersion(), 0);
    if (increment) version++;
    toSave.setContentMigrationVersion(String.valueOf(version));
  }

  private boolean isValidPageId(String pageId) {
    try {
      var page = pageDao.find(pageId);
      return page != null && workflowHelper.isCheckedOutToCurrentUser(pageId);
    } catch (Exception e) {
      // allow method to return false
      return false;
    }
  }

  private Map<String, PSPair<String, String>> getWidgetNamesChanged(
      PSTemplate template, PSTemplate savedTemplate) {
    var changedWidgets = new HashMap<String, PSPair<String, String>>();
    if (template.getWidgets() == null) {
      return changedWidgets;
    }
    var oldIdsToWidgetName = getWidgetIdsToNameMap(template.getWidgets());
    var idsToWidgetName = getWidgetIdsToNameMap(savedTemplate.getWidgets());
    for (var oldId : oldIdsToWidgetName.keySet()) {
      if (!idsToWidgetName.containsKey(oldId)) {
        continue;
      }
      var oldName = oldIdsToWidgetName.get(oldId);
      var newName = idsToWidgetName.get(oldId);
      if (!(isBlank(newName) && isBlank(oldName)) && !equalsIgnoreCase(oldName, newName)) {
        changedWidgets.put(oldId, new PSPair<>(oldName, newName));
      }
    }
    return changedWidgets;
  }

  private Map<String, String> getWidgetIdsToNameMap(List<PSWidgetItem> list) {
    return list.stream()
        .collect(Collectors.toMap(PSWidgetItem::getId, w -> w.getName().orElse("")));
  }

  private void checkDuplicatedNames(PSRegionTree region) throws DataServiceSaveException {
    var widgetNames = new HashSet<String>();
    for (var regionWidget : region.getRegionWidgetAssociations()) {
      for (var widgetItem : regionWidget.getWidgetItems()) {
        var name = widgetItem.getName().orElse("");
        if (!isBlank(name) && !widgetNames.add(name)) {
          throw new DataServiceSaveException(
              "Widget name '" + name + "' is already in use. Please use another name.");
        }
      }
    }
  }

  private void updateMetaData(PSTemplate object) throws PSDataServiceException {
    var metadata = loadHtmlMetadata(object.getId());
    if (object.getAdditionalHeadContent() == null) {
      object.setAdditionalHeadContent(metadata.getAdditionalHeadContent());
    }
    if (object.getAfterBodyStartContent() == null) {
      object.setAfterBodyStartContent(metadata.getAfterBodyStartContent());
    }
    if (object.getBeforeBodyCloseContent() == null) {
      object.setBeforeBodyCloseContent(metadata.getBeforeBodyCloseContent());
    }
    if (object.getProtectedRegion() == null) {
      object.setProtectedRegion(metadata.getProtectedRegion());
    }
    if (object.getProtectedRegionText() == null) {
      object.setProtectedRegionText(metadata.getProtectedRegionText());
    }
    if (object.getDocType() == null) {
      object.setDocType(metadata.getDocType());
    }
  }

  public boolean isAssociatedToPages(String templateId) throws PSValidationException {
    rejectIfBlank("isAssociatedToPages", "templateId", templateId);
    return !pageDaoHelper.findPageIdsByTemplateInRecentRevision(templateId).isEmpty();
  }

  public Collection<Integer> getPageIdsForTemplate(String templateId) {
    return pageDaoHelper.findPageIdsByTemplateInRecentRevision(templateId);
  }

  private void updateBodyMarkupOrRegionTree(PSTemplate object) {
    // If we have the body markup but not the tree, create the tree from the markup.
    if (isNotBlank(object.getBodyMarkup())
        && (object.getRegionTree() == null || object.getRegionTree().getRootRegion() == null)) {
      log.debug("Creating the region tree from markup.");
      var regions = new HashMap<String, PSRegion>();
      var parser = new PSTemplateRegionParser(regions);
      var pt = parser.parse(object.getBodyMarkup());
      var tree = Optional.ofNullable(object.getRegionTree()).orElseGet(PSRegionTree::new);
      tree.setRootRegion(pt.getRootNode());
      object.setRegionTree(tree);
    }
    // If we have the tree, create the markup from the tree.
    else if (object.getRegionTree() != null && object.getRegionTree().getRootRegion() != null) {
      log.debug("Creating markup from tree");
      var markup = PSRegionTreeUtils.treeToString(object.getRegionTree().getRootRegion());
      object.setBodyMarkup(markup);
    }
  }

  public PSHtmlMetadata loadHtmlMetadata(String id) throws PSDataServiceException {
    var metadata = new PSHtmlMetadata();
    var t = load(id);
    metadata.setId(id);
    PSHtmlMetadataUtils.copy(t, metadata);
    return metadata;
  }

  public String getTemplateEditUrl(String id) {
    isTrue(isNotBlank(id), "id may not be blank");
    var url =
        contentDesignWs.getItemEditUrl(
            idMapper.getGuid(id), TPL_CONTENT_TYPE, IPSConstants.SYS_HIDDEN_FIELDS_VIEW_NAME);
    return fixUrl(url);
  }

  private String fixUrl(String url) {
    isTrue(isNotBlank(url), "url may not be blank");
    if (url.startsWith("../")) url = "/Rhythmyx/" + url.substring(3);
    return url;
  }

  public void saveHtmlMetadata(PSHtmlMetadata metadata) throws PSDataServiceException {
    var t = load(metadata.getId());
    PSHtmlMetadataUtils.copy(metadata, t);
    save(t);
  }

  public PSValidationErrors validate(PSTemplate object)
      throws PSSpringValidationException, DataServiceSaveException {
    var e = PSBeanValidationUtils.validate(object);
    regionWidgetAssocationsValidator.validate(object, e);
    e.throwIfInvalid();
    var errors = e.getValidationErrors();
    if (object.getRegionTree() != null
        && object.getRegionTree().getRegionWidgetAssociations() != null) {
      checkDuplicatedNames(object.getRegionTree());
    }
    return errors;
  }

  private PSRegionWidgetAssociationsValidator<PSTemplate> regionWidgetAssocationsValidator;

  public static class RegionWidgetValidator
      extends PSRegionWidgetAssociationsValidator<PSTemplate> {
    public RegionWidgetValidator(IPSWidgetService widgetService) {
      super(widgetService);
    }

    @Override
    public String getField() {
      return "regionTree";
    }

    @Override
    public PSRegionWidgetAssociations getWidgetAssociations(
        PSTemplate wa, PSBeanValidationException e) {
      return wa.getRegionTree();
    }
  }

  public boolean isValidatingDeleteTemplate() {
    return validatingDeleteTemplate;
  }

  public void setValidatingDeleteTemplate(boolean validatingDeleteTemplate) {
    this.validatingDeleteTemplate = validatingDeleteTemplate;
  }

  /** Used to sort templates by name with case-insensitive order. */
  public abstract static class PSAbstractTemplateSorter implements Comparator<PSTemplateSummary> {
    private final Collator collator = Collator.getInstance();

    public int compare(PSTemplateSummary t1, PSTemplateSummary t2) {
      var name1 = getName(t1);
      var name2 = getName(t2);
      return collator.compare(name1, name2);
    }

    /** Override to get the name from the template for sorting. */
    protected abstract String getName(PSTemplateSummary t);

    /**
     * Returns a new sorted list. <strong>Changes to the new list will NOT change the inputted
     * list</strong>
     */
    public <T extends PSTemplateSummary> List<T> sort(List<T> items) {
      notNull(items);
      noNullElements(items);
      var sorted = new ArrayList<>(items);
      Collections.sort(sorted, this);
      return sorted;
    }
  }

  /** Sorts readonly and user templates. */
  public static class PSTemplateSorter extends PSAbstractTemplateSorter {
    @Override
    protected String getName(PSTemplateSummary t) {
      var name = t.getName();
      if (t.isReadOnly()) {
        var shortName = substringAfterLast(name, ".");
        name = isBlank(shortName) ? name : shortName;
      }
      notEmpty(name);
      return name;
    }
  }

  /** Export the selected template. */
  public PSTemplate exportTemplate(String id, String name)
      throws PSValidationException, PSTemplateException {
    rejectIfBlank("exportTemplate", "id", id);
    return templateDao.generateTemplateToExport(id, name);
  }

  /** Import the selected template. */
  public PSTemplate importTemplate(PSTemplate template, String siteId)
      throws PSDataServiceException, IPSPathService.PSPathNotFoundServiceException {
    notNull(template, "template");
    rejectIfBlank("importTemplate", "siteId", siteId);
    log.debug("Importing template");
    var validRegionWidgets = cleanRegionWidgets(template);
    template.getRegionTree().setRegionWidgetAssociations(validRegionWidgets);
    validate(template);
    updateBodyMarkupOrRegionTree(template);
    var savedTemplate = templateDao.generateTemplateFromSource(template, siteId);
    var id = savedTemplate.getId();
    widgetAssetRelationshipService.removeAssetWidgetRelationships(id, savedTemplate.getWidgets());
    workflowHelper.transitionToPending(widgetAssetRelationshipService.getSharedAssets(id));
    return savedTemplate;
  }

  private Set<PSRegionWidgets> cleanRegionWidgets(PSTemplate template)
      throws PSDataServiceException {
    var tree = template.getRegionTree();
    var regionWidgetsToValidate = tree.getRegionWidgetAssociations();
    var sets = new HashSet<PSRegionWidgets>();
    if (regionWidgetsToValidate == null) return sets;
    var fulls = widgetDao.findAll();
    for (var w : regionWidgetsToValidate) {
      var widgetValidItems = new ArrayList<PSWidgetItem>();
      var widgetItems = w.getWidgetItems();
      if (widgetItems != null) {
        for (var item : widgetItems) {
          if (fulls.stream()
              .anyMatch(widgetDef -> item.getDefinitionId().equalsIgnoreCase(widgetDef.getId()))) {
            widgetValidItems.add(item);
          }
        }
      }
      if (!widgetValidItems.isEmpty()) {
        w.setWidgetItems(widgetValidItems);
        sets.add(w);
      }
    }
    return sets;
  }

  /**
   * Creates a new template with specified templateName for the site with siteId, using
   * baseTemplateName as the base template.
   */
  public PSTemplateSummary createNewTemplate(
      String baseTemplateName, String templateName, String siteId)
      throws PSAssemblyException, PSDataServiceException {
    var baseTemplate = assemblyService.findTemplateByName(baseTemplateName);
    return this.createTemplate(templateName, idMapper.getString(baseTemplate.getGUID()), siteId);
  }

  /** The log instance to use for this class, never <code>null</code>. */
  private static final Logger log = LogManager.getLogger(IPSConstants.DESIGN_LOG);
}
