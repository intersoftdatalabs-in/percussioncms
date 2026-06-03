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
/** */
// REFACTORED: CP-JAVA11
package com.percussion.share.dao;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSNode;
import java.util.*;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// nested exception declared at end of class

/**
 * A helper class to find our extended JCR 170 nodes.
 *
 * @author adamgent
 */
public class PSJcrNodeFinder {

  private IPSContentMgr contentMgr;
  private String contentType;
  private String uniqueIdFieldName;

  /**
   * @param contentMgr never <code>null</code>.
   * @param contentType never <code>null</code>.
   * @param uniqueIdFieldName never <code>null</code>.
   */
  public PSJcrNodeFinder(IPSContentMgr contentMgr, String contentType, String uniqueIdFieldName) {
    super();
    notNull(contentMgr);
    notNull(contentType);
    notNull(uniqueIdFieldName);
    this.contentMgr = contentMgr;
    this.contentType = contentType;
    this.uniqueIdFieldName = uniqueIdFieldName;
  }

  /**
   * @param folderPath Checks the folder path and descendents, never <code>null</code>.
   * @param id User supplied unique id, never <code>null</code>.
   * @return the generated jcr query.
   */
  public String getQuery(String folderPath, String id) {
    notNull(folderPath);
    notNull(id);
    var uniqCond = "rx:" + uniqueIdFieldName + " = '" + id + "'";
    var fp = StringUtils.removeEnd(folderPath, "/");
    var where = "jcr:path like '" + fp + "/%'" + " and " + uniqCond;
    return getQuery(where);
  }

  /**
   * @param folderPath Checks the folder path and descendents, never <code>null</code>.
   * @param id never <code>null</code>.
   * @return our extended jcr {@link javax.jcr.Node}
   * @throws PSJcrNodeFinderException If more than one node is found or the query is bad.
   */
  public IPSNode find(String folderPath, String id) throws PSJcrNodeFinderException {
    notNull(folderPath);
    notNull(id);
    var query = getQuery(folderPath, id);
    var nodes = executeQuery(query);
    if (nodes.isEmpty()) {
      return null;
    } else if (nodes.size() > 1) {
      throw new PSJcrNodeFinderException(
          "Returned multiple nodes: " + nodes.size() + " for query: " + query);
    }
    return nodes.get(0);
  }

  /** Calls {@link #find(String, Map)}. */
  public List<IPSNode> find(Map<String, String> whereFields) {
    notNull(whereFields);
    return find(null, whereFields);
  }

  /**
   * Finds all nodes which match the specified jcr path and where criteria.
   *
   * @param jcrPath set to <code>null</code> to include nodes from all paths.
   * @param whereFields map of fields which will make the where criteria. The key is the field name
   *     and the value is the field value.
   * @return list of {@link IPSNode} objects which match the criteria, never <code>null</code>, may
   *     be empty.
   */
  public List<IPSNode> find(String jcrPath, Map<String, String> whereFields) {
    notNull(whereFields);
    return executeQuery(getQuery(jcrPath, whereFields));
  }

  /**
   * Generates a basic jcr query using the specified jcr path and map of fields.
   *
   * @param jcrPath set to <code>null</code> to exclude the jcr path from the where clause.
   * @param whereFields map of fields which will make the where criteria. The key is the field name
   *     and the value is the field value.
   * @return never blank.
   */
  public String getQuery(String jcrPath, Map<String, String> whereFields) {
    notNull(whereFields);
    var where = "";
    if (jcrPath != null) {
      var fp = StringUtils.removeEnd(jcrPath, "/");
      where = "jcr:path like '" + fp + "/%'";
    }
    for (var field : whereFields.keySet()) {
      if (!StringUtils.isEmpty(where)) {
        where += " and ";
      }
      var value = whereFields.get(field);
      where += "rx:" + field + " = '" + value + "'";
    }
    return getQuery(where);
  }

  /**
   * Generates a basic jcr query using the specified where clause. The select fields are determined
   * by {@link #getSelectFields()}.
   *
   * @param where assumed not blank.
   * @return never blank.
   */
  private String getQuery(String where) {
    return "select " + getSelectFields() + " from " + contentType + " where " + where;
  }

  /**
   * Generates the select fields string used by {@link #getQuery(String)}. Current fields include
   * content id, folder id, and jcr path.
   *
   * @return never blank.
   */
  private String getSelectFields() {
    var selectFields = asList("rx:sys_contentid", "rx:sys_folderid", "jcr:path");
    return StringUtils.join(selectFields, ", ");
  }

  /**
   * Executes the given jcr query.
   *
   * @param query assumed not blank.
   * @return list of {@link IPSNode} object results, never <code>null</code>, may be empty.
   * @throws PSJcrNodeFinderException if the query is not valid or if an error occurred executing
   *     the query.
   */
  private List<IPSNode> executeQuery(String query) {
    var nodes = new ArrayList<IPSNode>();
    try {
      log.debug("Executing query: {}", query);
      var q = contentMgr.createQuery(query, Query.SQL);
      var results = contentMgr.executeQuery(q, -1, null, null);
      var it = results.getNodes();
      while (it.hasNext()) {
        var node = (IPSNode) it.nextNode();
        nodes.add(node);
      }
      return nodes;
    } catch (InvalidQueryException e) {
      throw new PSJcrNodeFinderException("Invalid query: " + query, e);
    } catch (RepositoryException e) {
      throw new PSJcrNodeFinderException("Repository error for query: " + query, e);
    }
  }

  public Map<String, String> find(List<String> selectFields, String uniqueId) {
    notEmpty(uniqueId);
    notEmpty(selectFields);

    var result = new HashMap<String, String>();
    var queryBldr = new StringBuilder("select rx:displaytitle");
    for (var fieldName : selectFields) {
      queryBldr.append(", rx:").append(fieldName);
    }
    queryBldr.append(" from rx:").append(contentType);
    queryBldr.append(" where rx:").append(uniqueIdFieldName).append("=").append(uniqueId);

    try {
      var query = contentMgr.createQuery(queryBldr.toString(), Query.SQL);
      var qresults = contentMgr.executeQuery(query, 1, null, null);
      var rowIter = qresults.getRows();
      if (rowIter.hasNext()) {
        var row = rowIter.nextRow();
        for (var fieldName : selectFields) {
          String fieldVal = "";
          Value val = row.getValue("rx:" + fieldName);
          if (val != null) {
            fieldVal = val.getString();
          }
          result.put(fieldName, fieldVal);
        }
      }
      return result;
    } catch (Exception e) {
      throw new PSJcrNodeFinderException(e);
    }
  }

  /** The log instance to use for this class, never <code>null</code>. */
  private static final Logger log = LogManager.getLogger(PSJcrNodeFinder.class);

  /**
   * Checked exception thrown by {@link PSJcrNodeFinder} when queries fail or return unexpected
   * results. Originally defined as a nested class in the 8.1.x branch; preserved here to satisfy
   * callers.
   */
  public static class PSJcrNodeFinderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PSJcrNodeFinderException(String message) {
      super(message);
    }

    public PSJcrNodeFinderException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSJcrNodeFinderException(Throwable cause) {
      super(cause);
    }
  }
}
