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
package com.percussion.pagemanagement.service.impl;

import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.pagemanagement.dao.IPSWidgetDao;
import com.percussion.pagemanagement.data.PSWidgetDefinition;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.pagemanagement.data.PSWidgetPackageInfo;
import com.percussion.pagemanagement.data.PSWidgetPackageInfoRequest;
import com.percussion.pagemanagement.data.PSWidgetPackageInfoResult;
import com.percussion.pagemanagement.data.PSWidgetSummary;
import com.percussion.pagemanagement.service.IPSWidgetService;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.pkginfo.IPSPkgInfoService;
import com.percussion.services.pkginfo.data.PSPkgInfo;
import com.percussion.services.pkginfo.utils.PSIdNameHelper;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSSpringValidationException;
import com.percussion.share.validation.PSAbstractPropertiesValidator;
import com.percussion.share.validation.PSValidationErrors;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Widget catalog / validation service.
 *
 * <p>Property validators are initialized with {@code this} at field init (intentional). Justified
 * {@code this-escape} suppress on the type for that register-style wiring; class left non-final for
 * Spring friendliness.
 */
@SuppressWarnings("this-escape")
@Component("widgetService")
public class PSWidgetService implements IPSWidgetService {

  private final IPSWidgetDao widgetDao;
  private final IPSPkgInfoService pkgInfoSvc;
  private final IPSMetadataService mdService;

  @Value("${widgetService.baseTemplate:perc.widget}")
  private String baseTemplate;

  private final PSAbstractPropertiesValidator<PSWidgetItem> widgetUserPropertiesValidator =
      new PSWidgetUserPropertiesValidator(this);
  private final PSAbstractPropertiesValidator<PSWidgetItem> widgetCssPropertiesValidator =
      new PSWidgetCssPropertiesValidator(this);

  // Private data variable initialized in getWidgetType method.
  private Map<String, String> widgetTypeMap = null;

  @Autowired
  public PSWidgetService(
      IPSWidgetDao widgetDao, IPSPkgInfoService pkgInfoSvc, IPSMetadataService mdService) {
    super();
    this.widgetDao = widgetDao;
    this.pkgInfoSvc = pkgInfoSvc;
    this.mdService = mdService;
  }

  public PSSpringValidationException validateWidgetItem(PSWidgetItem widgetItem) {
    var e = widgetUserPropertiesValidator.validate(widgetItem);
    widgetCssPropertiesValidator.validate(widgetItem, e);
    return e;
  }

  /**
   * Prepares the widget item for assembly. Sets default values.
   *
   * @param item never <code>null</code>.
   */
  public void normalizeWidgetItem(PSWidgetItem item) throws PSDataServiceException {
    var def = load(item.getDefinitionId());
    PSWidgetUtils.setDefaultValuesFromDefinition(item, def);
  }

  public PSWidgetSummary find(String id) throws PSDataServiceException {
    var full = load(id);
    if (full == null) throw new DataServiceLoadException("Cannot find widget for id: " + id);
    var summary = createWidgetSummary();
    convertFullToSummary(full, summary);
    return summary;
  }

  public List<PSWidgetSummary> findAll() throws PSDataServiceException {
    return findByType("All");
  }

  @Override
  public List<PSWidgetSummary> findByType(String type) throws PSDataServiceException {
    return findByType(type, null);
  }

  public List<PSWidgetSummary> findByType(String type, String filterDisabledWidgets)
      throws PSDataServiceException {
    if (StringUtils.isBlank(type)) type = "All";
    var disabledWidgets = new ArrayList<String>();
    var filter =
        StringUtils.isNotBlank(filterDisabledWidgets)
            && filterDisabledWidgets.equalsIgnoreCase("yes");
    // If filter get the disabled widgets from metadata service
    if (filter) {
      var md = mdService.find("percwidgetconfiguration");
      if (md != null) {
        var data = md.getData();
        if (StringUtils.isNotBlank(data)) {
          var mapper = JsonMapper.builder().build();
          try {
            var jsonArray = mapper.readValue(data, java.util.ArrayList.class);
            for (var item : jsonArray) {
              disabledWidgets.add((String) item);
            }
          } catch (Exception e) {
            log.warn("Error parsing disabled widgets configuration", e);
          }
        }
      }
    }

    var summaries = new ArrayList<PSWidgetSummary>();
    var fulls = widgetDao.findAll();
    for (var full : fulls) {
      var sum = createWidgetSummary();
      convertFullToSummary(full, sum);
      if (type.equalsIgnoreCase("All") || type.equalsIgnoreCase(sum.getType())) {
        if (!filter || !disabledWidgets.contains(sum.getId())) summaries.add(sum);
      }
    }
    summaries.sort(summaryComp);
    return summaries;
  }

  // TODO: A PSWidgetDefinition should be a subclass of PSWidgetSummary
  private void convertFullToSummary(PSWidgetDefinition full, PSWidgetSummary summary) {
    full.getWidgetPrefs()
        .ifPresentOrElse(
            prefs -> {
              summary.setId(full.getId());
              summary.setLabel(prefs.getTitle());
              summary.setName(prefs.getContenttypeName());
              summary.setIcon(prefs.getThumbnail());
              summary.setHasUserPrefs(!full.getUserPref().isEmpty());
              summary.setHasCssPrefs(!full.getCssPref().isEmpty());
              summary.setType(getWidgetType(prefs.getTitle()));
              summary.setCategory(prefs.getCategory());
              summary.setDescription(prefs.getDescription());
              summary.setResponsive(prefs.isResponsive());
            },
            () ->
                log.error(
                    "Widget definition does not have user prefs, definitionId: " + full.getId()));
  }

  private PSWidgetSummary createWidgetSummary() {
    return new PSWidgetSummary();
  }

  /**
   * Helper method to get the widget type for the supplied widget name. If the widgetTypeMap is
   * <code>null</code>, then initializes it by loading WidgetRegistry.xml. If the supplied widget is
   * not a registered widget then returns the type as "Custom".
   *
   * @param widgetName The name of the widget for which the type needs to be found, assumed not
   *     blank.
   * @return The widget type, never <code>null</code>, will be "Custom" if the widget is not found
   *     in the registry.
   */
  private String getWidgetType(String widgetName) {
    // Load the map if needed
    if (widgetTypeMap == null) {
      widgetTypeMap = loadWidgetTypeMap();
    }

    var widgetType = widgetTypeMap.get(widgetName);
    if (widgetType == null) widgetType = "Custom";
    return widgetType;
  }

  /**
   * Helper method that loads the WidgetRegistry.xml and creates a map of widget name as key and
   * widget type as value.
   *
   * @return Map of widget name and type, never <code>null</code> may be empty.
   */
  private Map<String, String> loadWidgetTypeMap() {
    var widgetTypeMap = new HashMap<String, String>();
    try (var in =
        this.getClass()
            .getClassLoader()
            .getResourceAsStream("com/percussion/pagemanagement/service/impl/WidgetRegistry.xml")) {
      var doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
      var groupElems = doc.getElementsByTagName("group");
      for (int i = 0; i < groupElems.getLength(); i++) {
        var groupElem = (Element) groupElems.item(i);
        var groupName = groupElem.getAttribute("name");
        var widgetElems = groupElem.getElementsByTagName("widget");
        for (int j = 0; j < widgetElems.getLength(); j++) {
          var widgetElem = (Element) widgetElems.item(j);
          var wdgName = widgetElem.getAttribute("name");
          widgetTypeMap.put(wdgName, groupName);
        }
      }
    } catch (IOException | SAXException e) {
      // This should not happen as we are reading the file from JAR
      // in case if it happens logging it and returning empty widget
      // map.
      log.error("Failed to load or parse WidgetRegistry.xml file:", e);
    }
    return widgetTypeMap;
  }

  public void delete(String id)
      throws com.percussion.share.service.IPSDataService.DataServiceDeleteException {
    throw new UnsupportedOperationException("delete is not yet supported");
  }

  public PSWidgetDefinition load(String id) throws PSDataServiceException {
    var wd = widgetDao.find(id);
    if (wd == null) throw new DataServiceLoadException("No widget found for id: " + id);
    return wd;
  }

  @Override
  public PSWidgetPackageInfoResult findWidgetPackageInfo(PSWidgetPackageInfoRequest request) {
    var results = new PSWidgetPackageInfoResult();

    for (var widgetName : request.getWidgetNames()) {
      var info = findPackageInfo(widgetName);
      if (info == null) continue;

      var result = new PSWidgetPackageInfo();
      result.setWidgetName(widgetName);
      result.setProviderUrl(info.getPublisherUrl());
      result.setVersion(info.getPackageVersion());
      results.getPackageInfoList().add(result);
    }

    return results;
  }

  /**
   * Find the package info for the specified widget
   *
   * @param widgetName The name of the widget, not <code>null</code>.
   * @return The info, or null if not found.
   */
  private PSPkgInfo findPackageInfo(String widgetName) {
    PSPkgInfo pkgInfo = null;

    var filepath = widgetDao.getBaseConfigDir() + "/" + widgetName + ".xml";
    var pkgElem =
        pkgInfoSvc.findPkgElementByObject(
            PSIdNameHelper.getGuid(filepath, PSTypeEnum.USER_DEPENDENCY));

    if (pkgElem != null) {
      var pkgGuid = pkgElem.getPackageGuid();
      try {
        pkgInfo = pkgInfoSvc.loadPkgInfo(pkgGuid);
      } catch (Exception e) {
        // noop, fall thru
      }
    }

    return pkgInfo;
  }

  public PSWidgetDefinition save(PSWidgetDefinition object)
      throws PSBeanValidationException,
          com.percussion.share.service.IPSDataService.DataServiceSaveException {
    throw new UnsupportedOperationException("save is not yet supported");
  }

  public PSValidationErrors validate(PSWidgetDefinition object) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("validate is not yet supported");
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.pagemanagement.service.IPSWidgetService#getBaseTemplate()
   */
  public String getBaseTemplate() {
    return baseTemplate;
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.pagemanagement.service.IPSWidgetService#setBaseTemplate(java.lang.String)
   */
  public void setBaseTemplate(String baseTemplate) {
    this.baseTemplate = baseTemplate;
  }

  /**
   * Used for sorting of {@link PSWidgetSummary} objects. Sorts alphabetically by label
   * (case-sensitive).
   */
  private static class SummaryComparator implements Comparator<PSWidgetSummary> {
    public int compare(PSWidgetSummary s1, PSWidgetSummary s2) {
      if (s1 == null && s2 == null) return 0;
      else if (s1 == null) return -1;
      else if (s2 == null) return 1;

      if (s1.getLabel() == null && s2.getLabel() == null) return 0;
      if (s1.getLabel() != null && s2.getLabel() != null) {
        String l1 = s1.getLabel();
        String l2 = s2.getLabel();
        return l1.compareTo(l2);
      }
      if (s1.getLabel() != null) return 1;
      else return -1;
    }
  }

  /** Used for widget summary sorting. Never <code>null</code>. */
  private final SummaryComparator summaryComp = new SummaryComparator();

  /** The log instance to use for this class, never <code>null</code>. */
  private static final Logger log = LogManager.getLogger(PSWidgetService.class);
}
