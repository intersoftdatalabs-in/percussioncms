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

package com.percussion.soln.listbuilder;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.removeEnd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builds a JCR query from fields on a node.
 *
 * @author adamgent
 */
public class JCRQueryBuilder {
  /**
   * Creates a new JCRQueryBuilder.
   */
  public JCRQueryBuilder() {
    // default
  }

  // REFACTORED: CP-JAVA11
  private String startDate = null;
  private String endDate = null;
  private String titleContains = null;
  private String queryStartDateField = "rx:sys_contentstartdate";
  private String queryTitleField = "rx:displaytitle";
  private List<String> selectFields = asList("rx:sys_contentid", "rx:sys_folderid", "jcr:path");
  private Collection<String> fromTypes = asList("nt:base");
  private Collection<String> folderPaths = emptyList();
  private String query;

  /**
   * validate operation.
   */
  public void validate() {
    if (isEmpty(selectFields) || isEmpty(fromTypes)) {
      throw new IllegalStateException("Configuration of ListBuilder invalid");
    }
  }

  /**
   * buildTextField operation.
   *
   * @param field the field
   * @param value the value
   * @return the result
   */
  protected String buildTextField(String field, String value) {
    return format("( {0} like ''%{1}%'' )", field, value);
  }

  /**
   * buildQuery operation.
   *
   * @param fields the fields
   * @param types the types
   * @param cond the cond
   * @return the result
   */
  public String buildQuery(List<String> fields, Collection<String> types, String cond) {
    String f = join(fields.iterator(), ", ");
    String t = join(types.iterator(), ", ");
    if (isNotBlank(cond)) return format("select {0} from {1} where {2}", f, t, cond);
    return format("select {0} from {1}", f, t);
  }

  /**
   * buildDateRange operation.
   *
   * @param field the field
   * @param startDate the start date
   * @param endDate the end date
   * @return the result
   */
  public String buildDateRange(String field, String startDate, String endDate) {
    if (isNotBlank(field) && isNotBlank(startDate) && isNotBlank(endDate))
      return format("(''{0}'' < {1} and {1} < ''{2}'')", startDate, field, endDate);
    else if (isNotBlank(field) && isNotBlank(startDate))
      return format("(''{0}''  < {1} )", startDate, field);
    else return "";
  }

  /**
   * buildDateCond operation.
   *
   * @return the result
   */
  protected String buildDateCond() {
    return buildDateRange(queryStartDateField, startDate, endDate);
  }

  /**
   * buildTitleCond operation.
   *
   * @return the result
   */
  protected String buildTitleCond() {
    if (isBlank(queryTitleField)) return "";
    if (isBlank(titleContains)) return "";
    return format(" {0} like ''%{1}%'' ", queryTitleField, titleContains);
  }

  /**
   * buildCond operation.
   *
   * @param sep the sep
   * @param cond the cond
   * @return the result
   */
  public String buildCond(String sep, List<String> cond) {
    cond = removeBlank(cond);
    if (isEmpty(cond)) return "";
    String conds = join(cond.iterator(), " " + sep + " ");
    return "(" + conds + ")";
  }

  private boolean isEmpty(Collection<?> c) {
    return (c == null || c.isEmpty());
  }

  private List<String> removeBlank(List<String> ss) {
    List<String> rvalue = new ArrayList<String>();
    if (ss == null) return rvalue;

    for (String s : ss) {
      if (isNotBlank(s)) {
        rvalue.add(s);
      }
    }
    return rvalue;
  }

  /**
   * buildPathsLikeCond operation.
   *
   * @param paths the paths
   * @return the result
   */
  public String buildPathsLikeCond(Collection<String> paths) {
    if (isEmpty(paths)) return "";
    List<String> conds = new ArrayList<String>();
    for (String p : paths) {
      if (isNotBlank(p)) {
        String n = removeEnd(p, "/") + "/";
        conds.add(" jcr:path like '" + n + "%' ");
      }
    }
    if (isEmpty(conds)) return "";
    return "(" + join(conds.iterator(), " or ") + ")";
  }

  /**
   * buildCond operation.
   *
   * @return the result
   */
  public String buildCond() {
    //        String taxCond = buildTaxCond();
    //        String tagCond = buildTagCond();
    //        String categoryCond = buildCond("or", [tagCond,taxCond]);
    String dateCond = buildDateCond();
    String titleCond = buildTitleCond();
    String pathCond = buildPathsLikeCond(getFolderPaths());
    List<String> conds =
        asList(
            // categoryCond,
            dateCond, titleCond, pathCond);
    String cond = buildCond("and", conds);
    return cond;
  }

  /**
   * Returns the query.
   *
   * @return the result
   */
  public String getQuery() {
    if (isNotBlank(this.query)) return this.query;
    validate();
    String cond = buildCond();
    // String paths = this.pathsFromIds(values*.getLong());
    List<String> fields = this.selectFields;
    Collection<String> types = this.fromTypes;
    return buildQuery(fields, types, cond);
  }

  /**
   * Sets the query.
   *
   * @param query the query
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * Returns the query start date field.
   *
   * @return the result
   */
  public String getQueryStartDateField() {
    return queryStartDateField;
  }

  /**
   * Sets the query start date field.
   *
   * @param queryStartDateField the query start date field
   */
  public void setQueryStartDateField(String queryStartDateField) {
    this.queryStartDateField = queryStartDateField;
  }

  /**
   * Returns the query title field.
   *
   * @return the result
   */
  public String getQueryTitleField() {
    return queryTitleField;
  }

  /**
   * Sets the query title field.
   *
   * @param queryTitleField the query title field
   */
  public void setQueryTitleField(String queryTitleField) {
    this.queryTitleField = queryTitleField;
  }

  /**
   * Returns the select fields.
   *
   * @return the result
   */
  public List<String> getSelectFields() {
    return selectFields;
  }

  /**
   * Sets the select fields.
   *
   * @param selectFields the select fields
   */
  public void setSelectFields(List<String> selectFields) {
    this.selectFields = selectFields;
  }

  /**
   * Returns the from types.
   *
   * @return the result
   */
  public Collection<String> getFromTypes() {
    return fromTypes;
  }

  /**
   * Sets the from types.
   *
   * @param fromTypes the from types
   */
  public void setFromTypes(Collection<String> fromTypes) {
    this.fromTypes = fromTypes;
  }

  /**
   * Returns the start date.
   *
   * @return the result
   */
  public String getStartDate() {
    return startDate;
  }

  /**
   * Sets the start date.
   *
   * @param startDate the start date
   */
  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  /**
   * Returns the end date.
   *
   * @return the result
   */
  public String getEndDate() {
    return endDate;
  }

  /**
   * Sets the end date.
   *
   * @param endDate the end date
   */
  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

  /**
   * Returns the title contains.
   *
   * @return the result
   */
  public String getTitleContains() {
    return titleContains;
  }

  /**
   * Sets the title contains.
   *
   * @param titleContains the title contains
   */
  public void setTitleContains(String titleContains) {
    this.titleContains = titleContains;
  }

  /**
   * Returns the folder paths.
   *
   * @return the result
   */
  public Collection<String> getFolderPaths() {
    return folderPaths;
  }

  /**
   * Sets the folder paths.
   *
   * @param folderPaths the folder paths
   */
  public void setFolderPaths(Collection<String> folderPaths) {
    this.folderPaths = folderPaths;
  }

  //  public Collection<String> pathsFromIds(ids) {
  //  def locators = ids.collect { return new PSLocator((int)it, -1) };
  //  def guids = locators.collect { return guidManager.makeGuid(it) };
  //  def folders = contentService.loadFolders(guids);
  //  return folders*.getFolderPath()
  // }

  //  public String buildPathsEqual(List paths) {
  //      def p = paths.collect { return " jcr:path = '$it' " }.join(' or ');
  //      return p ? "($p)" : "";
  //  }
  //
  //  public String buildPathsLike(List paths) {
  //      def p = paths.collect { return " jcr:path like '$it%' " }.join(' or ');
  //      return p ? "($p)" : "";
  //  }

  //  public List convertSimpleChildField(String field) {
  //  if ( ! field ) return [];
  //  def values = node.getProperty(field).getValues().toList().string;
  //  return values ? values : [];
  // }

  // public String buildTagCond() {
  //  def values = convertSimpleChildField(tagField);
  //  return buildPathsEqual(values);
  // }

  // public String buildTaxCond() {
  //  def values = convertSimpleChildField(taxonomyField);
  //  return buildPathsLike(values);
  // }

}
