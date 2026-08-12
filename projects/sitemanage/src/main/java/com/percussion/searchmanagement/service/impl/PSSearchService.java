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
package com.percussion.searchmanagement.service.impl;

import static com.percussion.webservices.PSWebserviceUtils.getWorkflow;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import com.percussion.design.objectstore.PSField;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.itemmanagement.service.impl.PSWorkflowHelper;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.recycle.service.IPSRecycleService;
import com.percussion.search.objectstore.PSWSSearchField;
import com.percussion.search.objectstore.PSWSSearchParams;
import com.percussion.search.objectstore.PSWSSearchRequest;
import com.percussion.searchmanagement.data.PSSearchCriteria;
import com.percussion.searchmanagement.error.PSSearchServiceException;
import com.percussion.searchmanagement.service.IPSSearchService;
import com.percussion.security.SecureStringUtils;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSServer;
import com.percussion.server.webservices.PSSearchHandler;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.IPSItemEntry;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.system.PSSystemServiceLocator;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.data.PSPagedItemList;
import com.percussion.share.data.PSPagedItemPropertiesList;
import com.percussion.share.data.PSPagedObjectList;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.ui.data.PSDisplayPropertiesCriteria;
import com.percussion.ui.service.IPSListViewHelper;
import com.percussion.ui.service.IPSUiService;
import com.percussion.webservices.PSWebserviceUtils;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Provides services to search {@link PSPathItem} objects through Lucene. Sunny Sal says: "Search
 * like a pro, but don't search for bugs!"
 */
@Component("searchService")
public class PSSearchService implements IPSSearchService {
  /**
   * TODO: This service needs refactored to not use the Web Services API for calling the backend
   * search it should just be calling the spring service instead. The way it is coded now it is
   * interacting with the backend search over the wire which is not smart as it is running in the
   * same web app.
   */
  @Autowired IPSPageDaoHelper ipsPageDaoHelper;

  @Autowired
  public PSSearchService(
      IPSFolderHelper folderHelper,
      IPSIdMapper idMapper,
      IPSItemWorkflowService itemWorkflowService,
      @Qualifier("cm1SearchListViewHelper") IPSListViewHelper listViewHelper,
      IPSUiService uiService,
      IPSRecycleService recycleService,
      IPSSystemService systemService) {
    this.folderHelper = folderHelper;
    this.idMapper = idMapper;
    this.itemWorkflowService = itemWorkflowService;
    this.listViewHelper = listViewHelper;
    this.workflowService = PSWorkflowServiceLocator.getWorkflowService();
    PSSearchService.uiService = uiService;
    this.recycleService = recycleService;
    this.systemService =
        Optional.ofNullable(systemService).orElseGet(PSSystemServiceLocator::getSystemService);
  }

  @Override
  public PSPagedItemList search(PSSearchCriteria criteria)
      throws PSSearchServiceException,
          PSValidationException,
          PSNotFoundException,
          IPSDataService.DataServiceLoadException {
    var contentIdList = searchForIds(criteria);
    return search(criteria, contentIdList);
  }

  @Override
  public List<Integer> getContentIdsForFetchingByStatus(PSSearchCriteria criteria) {
    return getContentIdsForSearchByStatus(criteria);
  }

  private List<Integer> getContentIdsForSearchByStatus(PSSearchCriteria criteria) {
    if (criteria.getFormatId() == null) {
      criteria.setFormatId(DEFAULT_SEARCH_FORMAT_ID);
    }
    try {
      var searchHandler = new PSSearchHandler();
      var searchParams = new PSWSSearchParams();

      var searchFields = criteria.getSearchFields();
      if (searchFields != null && !searchFields.isEmpty()) {
        var wsSearchFields =
            searchFields.entrySet().stream()
                .map(
                    entry ->
                        new PSWSSearchField(
                            entry.getKey(), "=", entry.getValue(), PSWSSearchField.CONN_ATTR_AND))
                .collect(Collectors.toList());
        searchParams.setSearchFields(wsSearchFields);
      }
      var folderPath = criteria.getFolderPath();
      if (!StringUtils.isBlank(folderPath)) {
        searchParams.setFolderPathFilter(folderPath, true);
      }

      var search = new PSWSSearchRequest(searchParams);
      var request = PSWebserviceUtils.getRequest();

      var contentIdList = searchHandler.searchAndGetContentIdsForSearchByStatus(request, search);
      return contentIdList.stream()
          .filter(contentID -> !recycleService.isInRecycler(contentID.toString()))
          .collect(Collectors.toList());
    } catch (NumberFormatException nfe) {
      log.error("Error occurred while trying to parse the sys_contentid: {}", nfe.getMessage());
      log.debug(nfe);
      throw new PSSearchServiceException(nfe);
    } catch (Exception e) {
      log.error(
          "Error occurred while trying to perform a full text search: {}",
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new PSSearchServiceException(e);
    }
  }

  @Override
  public PSPagedItemPropertiesList getExtendedSearchResults(PSSearchCriteria criteria)
      throws PSSearchServiceException {
    var contentIds = searchForIds(criteria);
    var allItemEntries = getSortedEntries(criteria, contentIds);
    var startIndex = Optional.ofNullable(criteria.getStartIndex()).orElse(1);

    var result = PSPagedObjectList.getPage(allItemEntries, startIndex, criteria.getMaxResults());
    var pagedItemEntries = result.getChildrenInPage();
    var resultingStartIndex = result.getStartIndex();

    var itemsInPage = new ArrayList<PSItemProperties>();
    for (var itemEntry : pagedItemEntries) {
      try {
        var myGuid = PSGuidUtils.makeGuid(itemEntry.getContentId(), PSTypeEnum.LEGACY_CONTENT);
        if (folderHelper.getParentFolderId(myGuid, false) == null) {
          log.debug(
              "Item (id = {}) is not in a folder. It will not be included in the search results.",
              itemEntry.getContentId());
          continue;
        }
        var itemProps = folderHelper.findItemPropertiesById(idMapper.getString(myGuid));
        itemsInPage.add(itemProps);
      } catch (Exception e) {
        log.warn(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
    }
    return new PSPagedItemPropertiesList(itemsInPage, allItemEntries.size(), resultingStartIndex);
  }

  @Override
  public PSPagedItemList search(PSSearchCriteria criteria, List<Integer> contentIdList)
      throws PSSearchServiceException,
          PSValidationException,
          PSNotFoundException,
          IPSDataService.DataServiceLoadException {
    if (criteria.getFormatId() == null) {
      criteria.setFormatId(DEFAULT_SEARCH_FORMAT_ID);
    }
    var allItemEntries = getSortedEntries(criteria, contentIdList);
    return formatResults(criteria, allItemEntries);
  }

  @Override
  public PSPagedItemList searchByStatus(PSSearchCriteria criteria, List<Integer> contentIdList)
      throws PSSearchServiceException,
          PSValidationException,
          PSNotFoundException,
          IPSDataService.DataServiceLoadException {
    if (criteria.getFormatId() == null) {
      criteria.setFormatId(DEFAULT_SEARCH_FORMAT_ID);
    }
    // ensure we have a List since helper may return a generic Collection
    List<Integer> finalContentIdList =
        new ArrayList<>(ipsPageDaoHelper.getContentIdsForFetchingByStatus(criteria, contentIdList));
    var allItemEntries = getSortedEntries(criteria, finalContentIdList);
    return formatResults(criteria, allItemEntries);
  }

  private List<Integer> searchForIds(PSSearchCriteria criteria) {
    // GH-2950: minimal free-text criteria is valid; default format instead of 400.
    if (criteria.getFormatId() == null) {
      criteria.setFormatId(DEFAULT_SEARCH_FORMAT_ID);
    }
    try {
      var searchHandler = new PSSearchHandler();
      var searchParams = new PSWSSearchParams();

      var searchFields = criteria.getSearchFields();
      if (searchFields != null && !searchFields.isEmpty()) {
        var wsSearchFields =
            searchFields.entrySet().stream()
                .map(
                    entry ->
                        new PSWSSearchField(
                            entry.getKey(), "=", entry.getValue(), PSWSSearchField.CONN_ATTR_AND))
                .collect(Collectors.toList());
        searchParams.setSearchFields(wsSearchFields);
      }

      // Decode the URL-encoded query, workaround for jQuery bug http://bugs.jquery.com/ticket/8417
      // Then escape it for Lucene. Null/blank query is not a valid FTS request.
      if (criteria.getQuery() == null) {
        throw new IllegalArgumentException("query cannot be blank.");
      }
      var urlDecodedQuery = URLDecoder.decode(criteria.getQuery(), "UTF-8");
      var query = escapeLuceneQuery(urlDecodedQuery);

      // Exclude local content from the search
      query = excludeLocalWorkflow(query, searchParams);
      searchParams.setFTSQuery(query);

      var folderPath = criteria.getFolderPath();
      if (!StringUtils.isBlank(folderPath)) {
        searchParams.setFolderPathFilter(folderPath, true);
      }

      var search = new PSWSSearchRequest(searchParams);
      search.setUseExternalSearchEngine(true);
      var request = PSWebserviceUtils.getRequest();

      var contentIdList = searchHandler.searchAndGetContentIds(request, search);
      return contentIdList.stream()
          .filter(contentID -> !recycleService.isInRecycler(contentID.toString()))
          .collect(Collectors.toList());
    } catch (NumberFormatException nfe) {
      log.error("Error occurred while trying to parse the sys_contentid: {}", nfe.getMessage());
      log.debug(nfe);
      throw new PSSearchServiceException(nfe);
    } catch (Exception e) {
      log.error(
          "Error occurred while trying to perform a full text search: {}",
          PSExceptionUtils.getMessageForLog(e));
      throw new PSSearchServiceException(e);
    }
  }

  /** Creates the paged item list with the expected display properties. */
  private PSPagedItemList formatResults(
      PSSearchCriteria criteria, List<IPSItemEntry> allItemEntries)
      throws PSValidationException, IPSDataService.DataServiceLoadException, PSNotFoundException {
    var startIndex = Optional.ofNullable(criteria.getStartIndex()).orElse(1);

    var result = PSPagedObjectList.getPage(allItemEntries, startIndex, criteria.getMaxResults());
    var pagedItemEntries = result.getChildrenInPage();
    var resultingStartIndex = result.getStartIndex();

    var itemsInPage = new ArrayList<PSPathItem>();
    for (var itemEntry : pagedItemEntries) {
      var myGuid = PSGuidUtils.makeGuid(itemEntry.getContentId(), PSTypeEnum.LEGACY_CONTENT);
      if (folderHelper.getParentFolderId(myGuid, false) == null) {
        log.debug(
            "Item (id = {}) is not in a folder. It will not be included in the search results.",
            itemEntry.getContentId());
        continue;
      }
      var pathItem = folderHelper.findItemById(idMapper.getString(myGuid));
      pathItem.setRelatedObject(itemEntry);
      itemsInPage.add(pathItem);
    }

    var format = uiService.getDisplayFormat(criteria.getFormatId());
    listViewHelper.fillDisplayProperties(new PSDisplayPropertiesCriteria(itemsInPage, format));

    return new PSPagedItemList(itemsInPage, allItemEntries.size(), resultingStartIndex);
  }

  /**
   * Gets the item entries for all of the supplied content ids sorted based on the supplied
   * criteria.
   */
  private List<IPSItemEntry> getSortedEntries(
      PSSearchCriteria criteria, List<Integer> contentIdList) {
    CompareItemEntry compare = null;
    if (criteria.getSortColumn() != null && criteria.getSortOrder() != null) {
      compare = new CompareItemEntry(criteria.getSortColumn(), criteria.getSortOrder());
    }
    // Use reflection to avoid compile‑time dependency on methods not present
    // in the published IPSCmsObjectMgr interface version that sitemanage
    // currently depends on.  The implementation (PSCmsObjectMgr) does provide
    // the method.
    try {
      var method =
          cmsObjectMgr.getClass().getMethod("findItemEntries", List.class, Comparator.class);

      Object invoked = method.invoke(cmsObjectMgr, contentIdList, compare);
      if (!(invoked instanceof List<?> raw)) {
        return List.of();
      }
      var result = new ArrayList<IPSItemEntry>(raw.size());
      for (Object o : raw) {
        if (o instanceof IPSItemEntry entry) {
          result.add(entry);
        }
      }
      return result;
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to invoke findItemEntries via reflection", e);
    }
  }

  private class CompareItemEntry implements Comparator<IPSItemEntry> {
    String sortColumn;
    int sortOrderNumber;

    private CompareItemEntry(String sortColumn, String sortOrder) {
      this.sortColumn = sortColumn;
      this.sortOrderNumber = equalsIgnoreCase(sortOrder, "desc") ? -1 : 1;
    }

    @Override
    public int compare(IPSItemEntry o1, IPSItemEntry o2) {
      Object prop1 = null, prop2 = null;
      if (StringUtils.equals(sortColumn, CONTENT_CREATEDBY_NAME)) {
        prop1 = o1.getCreatedBy();
        prop2 = o2.getCreatedBy();
      } else if (StringUtils.equals(sortColumn, CONTENT_CREATEDDATE_NAME)) {
        prop1 = o1.getCreatedDate();
        prop2 = o2.getCreatedDate();
      } else if (StringUtils.equals(sortColumn, POSTDATE_NAME)) {
        prop1 = o1.getPostDate();
        prop2 = o2.getPostDate();
      } else if (StringUtils.equals(sortColumn, CONTENT_LAST_MODIFIED_DATE_NAME)) {
        prop1 = o1.getLastModifiedDate();
        prop2 = o2.getLastModifiedDate();
      } else if (StringUtils.equals(sortColumn, STATE_NAME)) {
        prop1 = o1.getStateName();
        prop2 = o2.getStateName();
      } else if (StringUtils.equals(sortColumn, TITLE_NAME)) {
        prop1 = o1.getName();
        prop2 = o2.getName();
      } else if (StringUtils.equals(sortColumn, CONTENTTYPE_NAME)) {
        prop1 = o1.getContentTypeLabel();
        prop2 = o2.getContentTypeLabel();
      } else if (StringUtils.equals(sortColumn, WORKFLOW_NAME)) {
        var wf1 = getWorkflow(o1.getWorkflowAppId());
        var wf2 = getWorkflow(o2.getWorkflowAppId());
        prop1 = wf1.getName();
        prop2 = wf2.getName();
      } else {
        throw new IllegalArgumentException("The specified sort column is not supported");
      }
      var compareResult = new CompareToBuilder().append(prop1, prop2).toComparison();
      return sortOrderNumber * compareResult;
    }
  }

  /** Delegates to {@link PSLuceneQueryEscaper#escape(String)} for free-text Lucene queries. */
  private String escapeLuceneQuery(String query) {
    return PSLuceneQueryEscaper.escape(query);
  }

  private String excludeLocalWorkflow(String query, PSWSSearchParams searchParams)
      throws IPSItemWorkflowService.PSItemWorkflowServiceException, PSValidationException {
    var localId = getLocalContentWfId();
    if (!StringUtils.isBlank(query)) {
      query += EXCLUDE_WORKFLOW + localId;
    } else {
      if (!hasWorkflowParam(searchParams)) {
        query += "(";
        var wfIds = getSearchableWorkflowIds();
        var iter = wfIds.iterator();
        while (iter.hasNext()) {
          var wfId = iter.next();
          query += WORKFLOW_ID + ":" + wfId;
          if (iter.hasNext()) {
            query += " OR ";
          }
        }
        query += ")";
      }
    }
    return query;
  }

  private boolean hasWorkflowParam(PSWSSearchParams searchParams) {
    for (var field : searchParams.getSearchFields()) {
      if (field.getName().equalsIgnoreCase(WORKFLOW_ID)) {
        return true;
      }
    }
    return false;
  }

  private int getLocalContentWfId()
      throws PSValidationException, IPSItemWorkflowService.PSItemWorkflowServiceException {
    if (localContentWfId == -1) {
      localContentWfId = itemWorkflowService.getWorkflowId(PSWorkflowHelper.LOCAL_WORKFLOW_NAME);
    }
    return localContentWfId;
  }

  private List<Integer> getSearchableWorkflowIds()
      throws IPSItemWorkflowService.PSItemWorkflowServiceException, PSValidationException {
    var wfIds = new ArrayList<Integer>();
    var localWorkflowId = getLocalContentWfId();
    var workflowSums = workflowService.findWorkflowSummariesByName(null);
    for (var summary : workflowSums) {
      var wfId = summary.getGUID().getUUID();
      if (wfId == localWorkflowId) {
        continue;
      }
      wfIds.add(wfId);
    }
    return wfIds;
  }

  private IPSFolderHelper folderHelper;
  private IPSIdMapper idMapper;
  private IPSItemWorkflowService itemWorkflowService;
  private IPSListViewHelper listViewHelper;
  private static IPSUiService uiService;
  // reference via interface, which contains findItemEntries
  private static IPSCmsObjectMgr cmsObjectMgr = PSCmsObjectMgrLocator.getObjectManager();
  private IPSWorkflowService workflowService;
  private IPSRecycleService recycleService;
  private IPSSystemService systemService = PSSystemServiceLocator.getSystemService();
  private int localContentWfId = -1;

  private static final Logger log = LogManager.getLogger(PSSearchService.class);

  private static final String CONTENT_CREATEDBY_NAME = "sys_contentcreatedby";
  private static final String CONTENT_CREATEDDATE_NAME = "sys_contentcreateddate";
  private static final String POSTDATE_NAME = "sys_postdate";
  private static final String CONTENT_LAST_MODIFIED_DATE_NAME = "sys_contentlastmodifieddate";
  private static final String STATE_NAME = "sys_statename";
  private static final String TITLE_NAME = "sys_title";
  private static final String CONTENTTYPE_NAME = "sys_contenttypename";
  private static final String WORKFLOW_NAME = "sys_workflow";
  private static final String WORKFLOW_ID = "sys_workflowid";
  private static final String EXCLUDE_WORKFLOW = " NOT " + WORKFLOW_ID + ":";

  /**
   * Classic system list display format id (same as finder Home default / GH-2950). Used when
   * callers omit formatId; extended-results path does not use the format for row projection but
   * historically required a non-null id.
   */
  static final int DEFAULT_SEARCH_FORMAT_ID = 9;

  @Override
  public PSSearchCriteria validateSearchCriteria(PSSearchCriteria criteria) {
    SecureStringUtils.DatabaseType type = null;
    if (systemService.isMySQL()) {
      type = SecureStringUtils.DatabaseType.MYSQL;
    } else if (systemService.isOracle()) {
      type = SecureStringUtils.DatabaseType.ORACLE;
    } else if (systemService.isDB2()) {
      type = SecureStringUtils.DatabaseType.DB2;
    } else if (systemService.isMsSQL()) {
      type = SecureStringUtils.DatabaseType.MSSQL;
    } else if (systemService.isDerby()) {
      type = SecureStringUtils.DatabaseType.DERBY;
    } else if (systemService.isH2()) {
      type = SecureStringUtils.DatabaseType.H2;
    }

    if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
      criteria.setQuery(SecureStringUtils.sanitizeStringForSQLStatement(criteria.getQuery(), type));
    }
    if (criteria.getSearchType() != null && !criteria.getSearchType().trim().isEmpty()) {
      criteria.setSearchType(
          SecureStringUtils.sanitizeStringForSQLStatement(criteria.getSearchType(), type));
    }
    if (criteria.getStartIndex() != null) {
      if (!org.apache.commons.lang3.StringUtils.isNumeric(criteria.getStartIndex().toString())) {
        throw new IllegalArgumentException(
            criteria.getStartIndex() + " must have a numeric value for search");
      }
    }
    if (criteria.getMaxResults() != null) {
      if (!org.apache.commons.lang3.StringUtils.isNumeric(criteria.getMaxResults().toString())) {
        throw new IllegalArgumentException(
            criteria.getMaxResults() + " must have a numeric value for search");
      }
    }
    if (criteria.getSortColumn() != null && !criteria.getSortColumn().trim().isEmpty()) {
      criteria.setSortColumn(
          SecureStringUtils.sanitizeStringForSQLStatement(criteria.getSortColumn(), type));
    }
    if (criteria.getSortOrder() != null && !criteria.getSortOrder().trim().isEmpty()) {
      criteria.setSortOrder(
          SecureStringUtils.sanitizeStringForSQLStatement(criteria.getSortOrder(), type));
    }
    if (criteria.getFolderPath() != null && !criteria.getFolderPath().trim().isEmpty()) {
      criteria.setFolderPath(
          SecureStringUtils.sanitizeStringForSQLStatement(criteria.getFolderPath(), type));
    }
    if (criteria.getFormatId() != null && -1 != criteria.getFormatId()) {
      if (!org.apache.commons.lang3.StringUtils.isNumeric(criteria.getFormatId().toString())) {
        throw new IllegalArgumentException(
            criteria.getFormatId() + " must have a numeric value for search");
      }
    }
    // getSearchFields() is unmodifiable — copy before validating / rewriting values.
    var fields = criteria.getSearchFields();
    if (fields != null && !fields.isEmpty()) {
      var mutableFields = new HashMap<String, String>(fields);
      var systemFieldSet = PSServer.getContentEditorSystemDef().getFieldSet();
      for (var field : mutableFields.entrySet()) {
        var f = systemFieldSet.findFieldByName(field.getKey(), false);
        if (f != null) {
          if (f.getDataType().equalsIgnoreCase(PSField.DT_INTEGER)
              || f.getDataType().equalsIgnoreCase(PSField.DT_FLOAT)) {
            if (!org.apache.commons.lang3.StringUtils.isNumeric(field.getValue())) {
              throw new IllegalArgumentException(
                  field.getKey() + " must have a numeric value for search");
            }
          } else if (f.getDataType().equalsIgnoreCase(PSField.DT_BOOLEAN)) {
            Boolean b = BooleanUtils.toBoolean(field.getValue());
            if (b == null) {
              throw new IllegalArgumentException(field.getKey() + " requires a boolean value.");
            }
          } else if (f.getDataType().equalsIgnoreCase(PSField.DT_DATE)) {
            if (!SecureStringUtils.isValidDate(field.getValue())) {
              throw new IllegalArgumentException(field.getKey() + " must be a valid date.");
            }
          } else if (f.getDataType().equalsIgnoreCase(PSField.DT_TIME)) {
            if (!SecureStringUtils.isValidTime(field.getValue())) {
              throw new IllegalArgumentException(field.getKey() + " must be a valid time.");
            }
          } else if (f.getDataType().equalsIgnoreCase(PSField.DT_BINARY)
              || f.getDataType().equalsIgnoreCase(PSField.DT_IMAGE)) {
            throw new IllegalArgumentException("Can't use Binary fields in Search criteria.");
          } else {
            field.setValue(SecureStringUtils.sanitizeStringForSQLStatement(field.getValue(), type));
          }
        } else {
          field.setValue(SecureStringUtils.sanitizeStringForSQLStatement(field.getValue(), type));
        }
      }
      criteria.setSearchFields(mutableFields);
    }
    return criteria;
  }
}
