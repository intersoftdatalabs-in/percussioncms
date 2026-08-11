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
package com.percussion.pagemanagement.dao.impl;

import static com.percussion.services.utils.orm.PSDataCollectionHelper.MAX_IDS;
import static com.percussion.util.PSSqlHelper.qualifyTableName;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.searchmanagement.data.PSSearchCriteria;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.error.PSRuntimeException;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.webservices.content.IPSContentWs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper for page DAO operations, including workflow and template management.
 *
 * @author miltonpividori
 */
@Component("pageDaoHelper")
@Lazy
@Transactional(propagation = Propagation.SUPPORTS, noRollbackFor = Exception.class)
public class PSPageDaoHelper implements IPSPageDaoHelper {

  @PersistenceContext private EntityManager entityManager;

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }

  private IPSContentWs contentWs;
  private IPSFolderHelper folderHelper;
  private IPSIdMapper idMapper;

  @Autowired
  public PSPageDaoHelper(
      IPSContentWs contentWs,
      // @Lazy breaks reverse edge: folderHelper creation → … → pageDaoHelper → folderHelper.
      // Class-level @Lazy on this bean only defers first request; ctor deps still resolve eagerly.
      // Observed on Docker CMS after contentItemDao @Lazy (#2435): #2423 / #2437.
      @Lazy IPSFolderHelper folderHelper,
      IPSIdMapper idMapper) {
    this.contentWs = contentWs;
    this.folderHelper = folderHelper;
    this.idMapper = idMapper;
  }

  @Override
  public void setWorkflowAccordingToParentFolder(PSPage page) throws PSValidationException {
    notNull(page, "page cannot be null");
    notEmpty(page.getFolderPaths(), "page.folderpaths cannot be null");
    // Get the parent folder and set the correct workflow id for the new page
    page.setWorkflowId(getWorkflowIdForPath(page.getFolderPaths().get(0)));
  }

  @Override
  public int getWorkflowIdForPath(String folderPath) throws PSValidationException {
    notEmpty(folderPath);
    int workflowId;
    var parentFolderGuid = contentWs.getIdByPath(folderPath);
    if (parentFolderGuid != null) {
      var parentFolder = folderHelper.findFolderProperties(idMapper.getString(parentFolderGuid));
      workflowId = folderHelper.getValidWorkflowId(parentFolder);
    } else {
      var workflowService = PSWorkflowServiceLocator.getWorkflowService();
      workflowId = workflowService.getDefaultWorkflowId().getUUID();
    }
    return workflowId;
  }

  @Override
  @Transactional
  public Collection<Integer> findPageIdsByTemplate(String templateId) {
    var sess = getSession();
    try {
      var sql =
          "select distinct CONTENTID from "
              + qualifyTableName(PAGE_TABLE)
              + " where TEMPLATEID = :template";
      // Hibernate createNativeQuery+addScalar is not fully generic; assign via List<?> then copy.
      org.hibernate.query.NativeQuery<?> query =
          sess.createNativeQuery(sql)
              .addScalar("CONTENTID", org.hibernate.type.StandardBasicTypes.INTEGER);
      query.setParameter(TEMPLATE_PARAM, templateId);
      return castIntegerList(query.list());
    } catch (SQLException e) {
      log.error("Failed to get the fully qualified table name for '{}'", PAGE_TABLE);
      throw new PSRuntimeException(e);
    }
  }

  @Override
  @Transactional
  public Collection<Integer> findPageIdsByTemplateInRecentRevision(String deletedTemplate) {
    notEmpty(deletedTemplate);
    var sess = getSession();
    try {
      var sql =
          "select distinct P.CONTENTID "
              + "from "
              + qualifyTableName(PAGE_TABLE)
              + " as P "
              + "inner join "
              + qualifyTableName(CONTENT_TABLE)
              + " as CS ON P.CONTENTID = CS.CONTENTID "
              + "where TEMPLATEID = :template "
              + "    and (CS.CURRENTREVISION = P.REVISIONID "
              + "         OR CS.TIPREVISION = P.REVISIONID) ";
      org.hibernate.query.NativeQuery<?> query =
          sess.createNativeQuery(sql)
              .addScalar("CONTENTID", org.hibernate.type.StandardBasicTypes.INTEGER);
      query.setParameter(TEMPLATE_PARAM, deletedTemplate);
      var results = query.list();
      if (results == null) {
        return new ArrayList<>();
      }
      return castIntegerList(results);
    } catch (SQLException e) {
      log.error(
          "Failed to get the fully qualified table name for {}. Error: {}",
          PAGE_TABLE,
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      // This method should not be failing upstream transactions with a runtime exception.
      return new ArrayList<>();
    }
  }

  @Override
  public void replaceTemplateForPageInOlderRevisions(String deletedTemplate) {
    notEmpty(deletedTemplate);
    // get the pages that we need to update
    var pages = findPageIdsByTemplate(deletedTemplate);
    // get the new template to use for each page
    var mapPageToTemplate = findTemplateUsedByCurrentRevisionOfPages(new ArrayList<>(pages));
    // make the update of each page revision
    replaceTemplateForPages(mapPageToTemplate, deletedTemplate);
  }

  /**
   * Update old page revisions that used the deleted template to use the one that is being used in
   * the current revision of the page.
   */
  @Transactional
  public void replaceTemplateForPages(
      Map<String, String> mapPageToTemplate, String deletedTemplate) {
    var sess = getSession();
    try {
      var tableName = qualifyTableName(PAGE_TABLE);
      for (var entry : mapPageToTemplate.entrySet()) {
        var sql =
            "UPDATE "
                + tableName
                + " "
                + "SET TEMPLATEID = :template "
                + "WHERE CONTENTID = :contentid"
                + "    AND TEMPLATEID = :deletedtemplate";
        org.hibernate.query.NativeQuery<?> query = sess.createNativeQuery(sql);
        query.setParameter(TEMPLATE_PARAM, entry.getValue());
        query.setParameter("contentid", Integer.parseInt(entry.getKey()));
        query.setParameter("deletedtemplate", deletedTemplate);
        query.executeUpdate();
      }
    } catch (SQLException e) {
      log.error(ERROR_QUALIFY, PAGE_TABLE, CONTENT_TABLE);
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new PSRuntimeException(e);
    }
  }

  @Override
  public Map<String, String> findTemplateUsedByCurrentRevisionOfPages(List<Integer> pages) {
    if (isEmpty(pages)) {
      return new HashMap<>();
    }
    if (pages.size() < MAX_IDS) {
      return findTemplateUsedByCurrentRevision(pages);
    } else {
      // we need to paginate the query to avoid oracle problems
      var mapPageToTemplate = new HashMap<String, String>();
      for (int i = 0; i < pages.size(); i += MAX_IDS) {
        int end = Math.min(i + MAX_IDS, pages.size());
        // make the query
        mapPageToTemplate.putAll(findTemplateUsedByCurrentRevision(pages.subList(i, end)));
      }
      return mapPageToTemplate;
    }
  }

  @Override
  @Transactional(noRollbackFor = Exception.class)
  public Collection<Integer> findImportedPageIdsByTemplate(String templateId, List<Integer> pages) {
    if (isEmpty(pages)) {
      return new ArrayList<>();
    }
    if (pages.size() < MAX_IDS) {
      return findPageIdsByTemplateAndImportedPageIds(templateId, pages);
    } else {
      // we need to paginate the query to avoid oracle problems
      var results = new ArrayList<Integer>();
      for (int i = 0; i < pages.size(); i += MAX_IDS) {
        int end = Math.min(i + MAX_IDS, pages.size());
        // make the query
        results.addAll(findPageIdsByTemplateAndImportedPageIds(templateId, pages.subList(i, end)));
      }
      return results;
    }
  }

  /** Makes the query to find the template used by pages in the current revision. */
  @Transactional
  public Map<String, String> findTemplateUsedByCurrentRevision(List<Integer> pages) {
    var mapPageToTemplate = new HashMap<String, String>();
    var sess = getSession();
    try {
      var sql =
          "SELECT P.CONTENTID, P.TEMPLATEID "
              + "FROM "
              + qualifyTableName(PAGE_TABLE)
              + " AS P INNER JOIN "
              + qualifyTableName(CONTENT_TABLE)
              + " AS CS ON P.CONTENTID = CS.CONTENTID "
              + "WHERE P.CONTENTID IN ("
              + join(pages, ",")
              + ") "
              + "    AND CS.CURRENTREVISION = P.REVISIONID ";
      org.hibernate.query.NativeQuery<?> query = sess.createNativeQuery(sql);
      for (Object rowObj : query.list()) {
        Object[] row = (Object[]) rowObj;
        mapPageToTemplate.put(row[0].toString(), row[1].toString());
      }
      return mapPageToTemplate;
    } catch (SQLException e) {
      log.error(ERROR_QUALIFY, PAGE_TABLE, CONTENT_TABLE);
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new PSRuntimeException(e);
    }
  }

  /** Makes the query to find the imported page ids that are using the unassigned template Id. */
  @Transactional(noRollbackFor = Exception.class)
  public Collection<Integer> findPageIdsByTemplateAndImportedPageIds(
      String templateId, List<Integer> pages) {
    var sess = getSession();
    try {
      var sql =
          "select distinct CONTENTID from "
              + qualifyTableName(PAGE_TABLE)
              + " where TEMPLATEID ='"
              + templateId
              + "' AND CONTENTID in ("
              + join(pages, ",")
              + ") ";
      org.hibernate.query.NativeQuery<?> query =
          sess.createNativeQuery(sql)
              .addScalar("CONTENTID", org.hibernate.type.StandardBasicTypes.INTEGER);
      return castIntegerList(query.list());
    } catch (SQLException e) {
      log.error("Failed to get the fully qualified table name for '{}'", PAGE_TABLE);
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new PSRuntimeException(e);
    }
  }

  @Override
  public Map<String, String> findLinkTextForCurrentRevisionOfPages(List<Integer> pages) {
    if (isEmpty(pages)) {
      return new HashMap<>();
    }
    if (pages.size() < MAX_IDS) {
      return findLinkTextForCurrentRevision(pages);
    } else {
      // we need to paginate the query to avoid oracle problems
      var mapPageToLinkText = new HashMap<String, String>();
      for (int i = 0; i < pages.size(); i += MAX_IDS) {
        int end = Math.min(i + MAX_IDS, pages.size());
        // make the query
        mapPageToLinkText.putAll(findLinkTextForCurrentRevision(pages.subList(i, end)));
      }
      return mapPageToLinkText;
    }
  }

  @Transactional
  public Map<String, String> findLinkTextForCurrentRevision(List<Integer> pages) {
    var mapPageToLinkText = new HashMap<String, String>();
    var sess = getSession();
    try {
      var sql =
          "SELECT P.CONTENTID, P.RESOURCE_LINK_TITLE "
              + "FROM "
              + qualifyTableName(PAGE_TABLE)
              + " AS P INNER JOIN "
              + qualifyTableName(CONTENT_TABLE)
              + " AS CS ON P.CONTENTID = CS.CONTENTID "
              + "WHERE P.CONTENTID IN ("
              + join(pages, ",")
              + ") "
              + "    AND CS.CURRENTREVISION = P.REVISIONID ";
      org.hibernate.query.NativeQuery<?> query = sess.createNativeQuery(sql);
      for (Object rowObj : query.list()) {
        Object[] row = (Object[]) rowObj;
        mapPageToLinkText.put(row[0].toString(), row[1].toString());
      }
      return mapPageToLinkText;
    } catch (SQLException e) {
      log.error(ERROR_QUALIFY, PAGE_TABLE, CONTENT_TABLE);
      throw new PSRuntimeException(e);
    }
  }

  /** Copy Hibernate scalar list into a typed {@link Integer} collection without unchecked cast. */
  private static Collection<Integer> castIntegerList(List<?> results) {
    if (results == null || results.isEmpty()) {
      return new ArrayList<>();
    }
    var out = new ArrayList<Integer>(results.size());
    for (Object o : results) {
      if (o instanceof Integer i) {
        out.add(i);
      } else if (o instanceof Number n) {
        out.add(n.intValue());
      } else if (o != null) {
        out.add(Integer.valueOf(o.toString()));
      }
    }
    return out;
  }

  private static final String ERROR_QUALIFY =
      "Failed to get the fully qualified table name for '{}' or '{}'";
  private static final String PAGE_TABLE = "CT_PAGE";
  private static final String CONTENT_TABLE = "CONTENTSTATUS";
  private static final String TEMPLATE_PARAM = "template";
  private static final Logger log = LogManager.getLogger(PSPageDaoHelper.class);

  @Override
  @Transactional(noRollbackFor = Exception.class)
  public Collection<Integer> getContentIdsForFetchingByStatus(
      PSSearchCriteria criteria, List<Integer> contentIDs) {
    var sess = getSession();
    try {
      String sql;
      if (contentIDs.isEmpty()) {
        contentIDs.add(0);
      }
      if (criteria.getFolderPath().contains("Assets")) {
        sql =
            "select CS.CONTENTID from "
                + qualifyTableName("CONTENTSTATUS")
                + " AS CS WHERE CS.CONTENTID IN ("
                + join(contentIDs, ",")
                + ") AND CS.CONTENTTYPEID != "
                + PSFolder.FOLDER_CONTENT_TYPE_ID;
      } else {
        sql =
            "SELECT DISTINCT P.CONTENTID "
                + "FROM "
                + qualifyTableName("CT_PAGE")
                + " AS P INNER JOIN "
                + qualifyTableName("CONTENTSTATUS")
                + " AS CS ON P.CONTENTID = CS.CONTENTID "
                + " WHERE P.CONTENTID IN ("
                + join(contentIDs, ",")
                + ") ";
      }
      // formGetByStatusSQLQuery now returns the SQL with named-parameter
      // placeholders and a parameter name -> value map that the caller
      // binds via setParameter. The user-supplied search-field values
      // never reach the SQL string as concatenated text.
      var params = new HashMap<String, Object>();
      sql = formGetByStatusSQLQuery(criteria, sql, params);
      org.hibernate.query.NativeQuery<?> query =
          sess.createNativeQuery(sql)
              .addScalar("CONTENTID", org.hibernate.type.StandardBasicTypes.INTEGER);
      for (var e : params.entrySet()) {
        query.setParameter(e.getKey(), e.getValue());
      }
      return castIntegerList(query.list());
    } catch (NumberFormatException e) {
      // Per the review on PR #1202: a non-numeric ID value in
      // sys_contenttypeid / sys_contentstateid / sys_workflowid
      // causes parseLongId() to throw. We catch it here and return
      // an empty result (consistent with the Javadoc on parseLongId
      // and with the prior wrapped-DB-error behavior for invalid
      // inputs - both result in "no rows match"). Per the review on
      // PR #1202.
      log.error(
          "Invalid search field value in getContentIdsForFetchingByStatus: {}", e.getMessage());
      return java.util.Collections.emptyList();
    } catch (SQLException e) {
      var error = "Failed to get the fully qualified table name for 'CT_PAGE'";
      log.error(error, e);
      throw new PSRuntimeException(error, e);
    }
  }

  /**
   * Appends the search-field WHERE clauses for the supplied criteria, using named-parameter
   * placeholders. Each user-supplied value is recorded in {@code params} and bound by the caller
   * via {@code setParameter}. The values are NEVER concatenated into the SQL string.
   *
   * <p>Field types:
   *
   * <ul>
   *   <li>{@code templateid} — varchar (template name)
   *   <li>{@code sys_contenttypeid}, {@code sys_contentstateid}, {@code sys_workflowid} — integer
   *       IDs
   *   <li>{@code sys_contentlastmodifier} — varchar (partial match via LIKE)
   * </ul>
   *
   * <p>CodeQL {@code java/sql-injection} closure: the user-supplied values are bound as parameters,
   * not concatenated. See {@code specs/004-zero-code-scanning-alerts/tasks.md} T042 and {@code
   * contracts/C2}.
   */
  String formGetByStatusSQLQuery(
      PSSearchCriteria criteria, String sql, Map<String, Object> params) {
    Object templateid = criteria.getSearchFields().get("templateid");
    if (templateid != null) {
      sql = sql + " AND P.TEMPLATEID = :templateid";
      params.put("templateid", templateid);
    }
    Long contenttypeid = parseLongIdOrNull(criteria.getSearchFields().get("sys_contenttypeid"));
    if (contenttypeid != null) {
      sql = sql + " AND CS.CONTENTTYPEID = :contenttypeid";
      params.put("contenttypeid", contenttypeid);
    }
    Long contentstateid = parseLongIdOrNull(criteria.getSearchFields().get("sys_contentstateid"));
    if (contentstateid != null) {
      sql = sql + " AND CS.CONTENTSTATEID = :contentstateid";
      params.put("contentstateid", contentstateid);
    }
    Long workflowappid = parseLongIdOrNull(criteria.getSearchFields().get("sys_workflowid"));
    if (workflowappid != null) {
      sql = sql + " AND CS.WORKFLOWAPPID = :workflowappid";
      params.put("workflowappid", workflowappid);
    }
    Object contentlastmodifier = criteria.getSearchFields().get("sys_contentlastmodifier");
    if (contentlastmodifier != null) {
      sql = sql + " AND CS.CONTENTLASTMODIFIER LIKE :contentlastmodifier";
      // Wrap with %...% so the LIKE pattern is parameterized, not the full
      // pattern. The user-supplied value cannot break out of the pattern
      // because it's bound as a parameter.
      params.put("contentlastmodifier", "%" + contentlastmodifier + "%");
    }
    return sql;
  }

  /**
   * Parses a user-supplied search-field value as a {@code Long}, or returns {@code null} if the
   * value is null.
   *
   * @param value The value to parse, which may be a {@link Number} or a string representation of a
   *     long.
   * @return The parsed {@code Long} value, or {@code null} if the input is null.
   * @throws NumberFormatException if the value is non-numeric and cannot be parsed. The caller
   *     (getContentIdsForFetchingByStatus) catches this exception to return an empty result,
   *     preventing invalid numeric IDs from matching or throwing uncaught errors.
   */
  private static Long parseLongIdOrNull(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return Long.parseLong(value.toString().trim());
  }
}
