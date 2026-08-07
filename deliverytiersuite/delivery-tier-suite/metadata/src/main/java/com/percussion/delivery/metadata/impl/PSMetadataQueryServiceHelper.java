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
package com.percussion.delivery.metadata.impl;

import com.percussion.delivery.metadata.IPSMetadataProperty.VALUETYPE;
import com.percussion.delivery.metadata.IPSMetadataQueryService;
import com.percussion.delivery.metadata.data.impl.PSCriteriaElement;
import com.percussion.delivery.metadata.utils.PSHashCalculator;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Static utility helpers shared by the metadata query services and friends. The class centralises
 * the parsing of criteria values, the column-name lookup for the various {@link
 * com.percussion.delivery.metadata.IPSMetadataProperty.VALUETYPE} branches and the HQL sort-order /
 * sort-property coercion.
 *
 * @author erikserating
 */
public abstract class PSMetadataQueryServiceHelper {

  /**
   * Suppresses the default public constructor. This class only exposes static methods and is not
   * meant to be instantiated.
   */
  private PSMetadataQueryServiceHelper() {}

  /**
   * A set of property keys that are not stored as properties but are instead columns in the
   * metadata entry table.
   */
  public static final Set<String> ENTRY_PROPERTY_KEYS = new HashSet<String>();

  static {
    ENTRY_PROPERTY_KEYS.add("folder");
    ENTRY_PROPERTY_KEYS.add("name");
    ENTRY_PROPERTY_KEYS.add("type");
    ENTRY_PROPERTY_KEYS.add("linktext");
    ENTRY_PROPERTY_KEYS.add("linktext_lower");
    ENTRY_PROPERTY_KEYS.add("pagepath");
    ENTRY_PROPERTY_KEYS.add("site");
  }

  /** ISO-8601 formatter for dates such as {@code 2011-01-21T09:36:05} used in metadata queries. */
  public static final DateTimeFormatter dateFormat =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  /**
   * Resolves the declared {@link VALUETYPE} for the supplied property name, stripping any namespace
   * prefix first.
   *
   * @param name the property name (optionally namespace-prefixed with {@code prefix:localName});
   *     may not be {@code null}.
   * @param datatypeMappings the property datatype mappings used to resolve the value type; may not
   *     be {@code null}.
   * @return the resolved {@link VALUETYPE}, never {@code null}.
   */
  public static VALUETYPE getDatatype(String name, PSPropertyDatatypeMappings datatypeMappings) {
    String nameWithOutNamespace;

    if (name.contains(":")) nameWithOutNamespace = name.split(":")[1];
    else nameWithOutNamespace = name;

    return datatypeMappings.getDatatype(nameWithOutNamespace);
  }

  /**
   * Parses the supplied comma / quoted value into the typed list expected by an HQL {@code IN}
   * expression. Returns a list of {@link Double} for {@link VALUETYPE#NUMBER} entries, {@link
   * LocalDateTime} for {@link VALUETYPE#DATE} entries, and string hashes for text entries.
   *
   * @param key the property key; may not be {@code null}.
   * @param val the raw value list string; may not be {@code null}.
   * @param datatypeMappings the datatype mappings used to resolve the value type; may not be {@code
   *     null}.
   * @param hashCalc the hash calculator used to derive string hashes; may not be {@code null}.
   * @return the parsed typed list, never <code>null</code>, may be empty.
   * @throws ParseException if a date in the supplied value fails to parse.
   */
  public static List<Object> parseToList(
      String key,
      String val,
      PSPropertyDatatypeMappings datatypeMappings,
      PSHashCalculator hashCalc)
      throws ParseException {
    VALUETYPE type = datatypeMappings.getDatatype(key);
    List<Object> results = new ArrayList<>();

    if (type == VALUETYPE.NUMBER) {
      for (String s : val.split(",")) {
        results.add(Double.valueOf(s));
      }
    } else if (type == VALUETYPE.DATE) {
      for (String s : val.split("'")) {
        if (s.trim().equals(",") || s.trim().equals("")) continue;
        try {
          results.add(LocalDateTime.parse(s, dateFormat));
        } catch (Exception e) {
          throw new ParseException("Unparseable date: \"" + s + "\"", 0);
        }
      }
    } else { // For text / string use value hash if it is a property
      boolean calcHash = true;
      if (!key.contains("propValue")
          && datatypeMappings.getDatatypeMappings().getProperty(key, "").equals("")) {
        calcHash = false;
      }
      for (String s : val.split("'")) {
        if (s.trim().equals(",") || s.trim().equals("")) continue;

        if (calcHash) results.add(hashCalc.calculateHash(s));
        else results.add(s);
      }
    }
    return results;
  }

  /**
   * For the provided {@link PSCriteriaElement} it returns the column name in the database that
   * matches its declared value type. Defaults to the {@code stringvalue} column when the property
   * has no explicit type.
   *
   * @param ce the criteria element whose value column is to be resolved; may not be <code>null
   *     </code>.
   * @param datatypeMappings the property datatype mappings used to map names to value types; may
   *     not be <code>null</code>.
   * @return the name of the column backing the property's value type.
   */
  public static String getValueColumnName(
      PSCriteriaElement ce, PSPropertyDatatypeMappings datatypeMappings) {
    String valueColumn = "";
    VALUETYPE dt = getDatatype(ce.getName(), datatypeMappings);

    switch (dt) {
      case DATE:
        valueColumn = IPSMetadataQueryService.PROP_DATEVALUE_COLUMN_NAME;
        break;
      case NUMBER:
        valueColumn = IPSMetadataQueryService.PROP_NUMBERVALUE_COLUMN_NAME;
        break;
      case TEXT:
        if (ce.getOperationType() == PSCriteriaElement.OPERATION_TYPE.LIKE)
          valueColumn = IPSMetadataQueryService.PROP_TEXTVALUE_COLUMN_NAME;
        else valueColumn = IPSMetadataQueryService.PROP_VALUEHASH_COLUMN_NAME;
        break;
      default:
        if (ce.getOperationType() == PSCriteriaElement.OPERATION_TYPE.LIKE)
          valueColumn = IPSMetadataQueryService.PROP_STRINGVALUE_COLUMN_NAME;
        else valueColumn = IPSMetadataQueryService.PROP_VALUEHASH_COLUMN_NAME;
    }
    return valueColumn;
  }

  /**
   * For the supplied property name it returns the column name in the database that matches its
   * declared value type. Defaults to the {@code stringvalue} column when the property has no
   * explicit type.
   *
   * @param name the property name to resolve; may not be <code>null</code>.
   * @param datatypeMappings the property datatype mappings used to map names to value types; may
   *     not be <code>null</code>.
   * @return the name of the column backing the property's value type.
   */
  public static String getValueColumnName(
      String name, PSPropertyDatatypeMappings datatypeMappings) {
    String valueColumn = "";
    VALUETYPE dt = getDatatype(name, datatypeMappings);

    switch (dt) {
      case DATE:
        valueColumn = IPSMetadataQueryService.PROP_DATEVALUE_COLUMN_NAME;
        break;
      case NUMBER:
        valueColumn = IPSMetadataQueryService.PROP_NUMBERVALUE_COLUMN_NAME;
        break;
      case TEXT:
        valueColumn = IPSMetadataQueryService.PROP_TEXTVALUE_COLUMN_NAME;
        break;
      default:
        valueColumn = IPSMetadataQueryService.PROP_STRINGVALUE_COLUMN_NAME;
    }
    return valueColumn;
  }

  /**
   * Returns the sorting order based on the passed in orderby string, if nothing is there in the
   * orderby then the default would be asc
   *
   * @param orderBy the textual ordering suffix to inspect; may be <code>null</code>.
   * @return {@link com.percussion.delivery.metadata.IPSMetadataQueryService#SORT_ORDER_ASCEND} or
   *     {@link com.percussion.delivery.metadata.IPSMetadataQueryService#SORT_ORDER_DESCEND}.
   */
  public static String getSortingOrder(String orderBy) {
    return orderBy.toLowerCase().endsWith(IPSMetadataQueryService.SORT_ORDER_DESCEND)
        ? IPSMetadataQueryService.SORT_ORDER_DESCEND
        : IPSMetadataQueryService.SORT_ORDER_ASCEND;
  }

  /**
   * if the orderBy ends with either asc or desc then remove the suffix, trim it and return the
   * string, other wise just trim the passed in ordey by and return it. Ex: orderBy =
   * "dcterms:created asc" and the method returns dcterms:created Ex: orderBy = "dcterms:created
   * asc" and the method returns dcterms:created
   *
   * @param orderBy the {@code orderBy} string to strip the asc/desc suffix from; cannot be <code>
   *     null</code> or empty.
   * @return the sort property name with the asc/desc suffix removed; never <code>null</code>.
   */
  public static String getSortPropertyName(String orderBy) {
    String sortProperty = orderBy;
    String sortPropertyLowerCase = orderBy.toLowerCase();
    if (sortPropertyLowerCase.endsWith(IPSMetadataQueryService.SORT_ORDER_ASCEND)
        || sortPropertyLowerCase.endsWith(IPSMetadataQueryService.SORT_ORDER_DESCEND)) {
      sortProperty = sortProperty.substring(0, sortProperty.indexOf(' ')).trim();
    } else {
      sortProperty = sortProperty.trim();
    }
    return sortProperty;
  }

  /**
   * Determines the {@code asc}/{@code desc} ordering for a single property within a comma-separated
   * {@code orderBy} expression.
   *
   * @param name the property whose ordering should be returned; may be <code>null</code>.
   * @param orderBy the {@code orderBy} clause to scan; may not be <code>null</code>.
   * @return the resolved sort order, or {@link IPSMetadataQueryService#SORT_ORDER_ASCEND} when the
   *     property is not found in the clause.
   */
  public static String getSortingOrderForProperty(String name, String orderBy) {

    String sortOrder = IPSMetadataQueryService.SORT_ORDER_ASCEND;

    String props[] = orderBy.split(",");

    for (int i = 0; i < props.length; i++) {

      String sortProperty = props[i].trim();
      String[] pair = sortProperty.split(" ");

      if (pair.length > 0) sortProperty = pair[0].trim();

      if (sortProperty.equals(name)) {
        if (pair.length > 1) {
          sortOrder = pair[1].trim();
        }

        if (sortOrder.equals(IPSMetadataQueryService.SORT_ORDER_ASCEND)
            | sortOrder.equals(IPSMetadataQueryService.SORT_ORDER_DESCEND)) {
          return sortOrder;
        }
      }
    }

    return sortOrder;
  }
}
