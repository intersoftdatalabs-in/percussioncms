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
package com.percussion.delivery.metadata.impl;

import com.percussion.delivery.metadata.IPSMetadataProperty.VALUETYPE;
import com.percussion.delivery.metadata.IPSMetadataQueryService;
import com.percussion.delivery.metadata.data.impl.PSCriteriaElement;
import com.percussion.delivery.metadata.utils.PSHashCalculator;
import org.apache.commons.lang3.time.FastDateFormat;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class for metadata query service.
 * @author erikserating
 */
public abstract class PSMetadataQueryServiceHelper {

    private PSMetadataQueryServiceHelper() {}

    /**
     * Property keys that are columns in the metadata entry table.
     */
    public static final Set<String> ENTRY_PROPERTY_KEYS = Set.of(
            "folder", "name", "type", "linktext", "linktext_lower", "pagepath", "site"
    );

    /**
     * Date format: yyyy-MM-dd'T'HH:mm:ss
     */
    public static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");

    public static VALUETYPE getDatatype(String name, PSPropertyDatatypeMappings datatypeMappings) {
        var nameWithOutNamespace = name.contains(":") ? name.split(":")[1] : name;
        return datatypeMappings.getDatatype(nameWithOutNamespace);
    }

    public static List<Object> parseToList(String key, String val, PSPropertyDatatypeMappings datatypeMappings, PSHashCalculator hashCalc) throws ParseException {
        var type = datatypeMappings.getDatatype(key);
        var results = new ArrayList<Object>();

        if (type == VALUETYPE.NUMBER) {
            Arrays.stream(val.split(","))
                    .map(Double::valueOf)
                    .forEach(results::add);
        } else if (type == VALUETYPE.DATE) {
            Arrays.stream(val.split("'"))
                    .map(String::trim)
                    .filter(s -> !s.equals(",") && !s.isEmpty())
                    .map(s -> {
                        try {
                            return dateFormat.parse(s);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .forEach(results::add);
        } else {
            boolean calcHash = !key.contains("propValue") && !datatypeMappings.getDatatypeMappings().getProperty(key, "").isEmpty();
            Arrays.stream(val.split("'"))
                    .map(String::trim)
                    .filter(s -> !s.equals(",") && !s.isEmpty())
                    .forEach(s -> results.add(calcHash ? hashCalc.calculateHash(s) : s));
        }
        return results;
    }

    /**
     * Returns the DB column name for the property.
     */
    public static String getValueColumnName(PSCriteriaElement ce, PSPropertyDatatypeMappings datatypeMappings) {
        var dt = getDatatype(ce.getName(), datatypeMappings);
        return switch (dt) {
            case DATE -> IPSMetadataQueryService.PROP_DATEVALUE_COLUMN_NAME;
            case NUMBER -> IPSMetadataQueryService.PROP_NUMBERVALUE_COLUMN_NAME;
            case TEXT -> ce.getOperationType() == PSCriteriaElement.OPERATION_TYPE.LIKE
                    ? IPSMetadataQueryService.PROP_TEXTVALUE_COLUMN_NAME
                    : IPSMetadataQueryService.PROP_VALUEHASH_COLUMN_NAME;
            default -> ce.getOperationType() == PSCriteriaElement.OPERATION_TYPE.LIKE
                    ? IPSMetadataQueryService.PROP_STRINGVALUE_COLUMN_NAME
                    : IPSMetadataQueryService.PROP_VALUEHASH_COLUMN_NAME;
        };
    }

    public static String getValueColumnName(String name, PSPropertyDatatypeMappings datatypeMappings) {
        var dt = getDatatype(name, datatypeMappings);
        return switch (dt) {
            case DATE -> IPSMetadataQueryService.PROP_DATEVALUE_COLUMN_NAME;
            case NUMBER -> IPSMetadataQueryService.PROP_NUMBERVALUE_COLUMN_NAME;
            case TEXT -> IPSMetadataQueryService.PROP_TEXTVALUE_COLUMN_NAME;
            default -> IPSMetadataQueryService.PROP_STRINGVALUE_COLUMN_NAME;
        };
    }

    /**
     * Returns the sorting order based on the passed in orderBy string.
     */
    public static String getSortingOrder(String orderBy) {
        return orderBy.toLowerCase().endsWith(IPSMetadataQueryService.SORT_ORDER_DESCEND)
                ? IPSMetadataQueryService.SORT_ORDER_DESCEND
                : IPSMetadataQueryService.SORT_ORDER_ASCEND;
    }

    /**
     * Removes asc/desc suffix from orderBy and returns the property name.
     */
    public static String getSortPropertyName(String orderBy) {
        var sortPropertyLowerCase = orderBy.toLowerCase();
        if (sortPropertyLowerCase.endsWith(IPSMetadataQueryService.SORT_ORDER_ASCEND) ||
                sortPropertyLowerCase.endsWith(IPSMetadataQueryService.SORT_ORDER_DESCEND)) {
            return orderBy.substring(0, orderBy.indexOf(' ')).trim();
        }
        return orderBy.trim();
    }

    public static String getSortingOrderForProperty(String name, String orderBy) {
        var sortOrder = IPSMetadataQueryService.SORT_ORDER_ASCEND;
        var props = orderBy.split(",");
        for (var sortProperty : props) {
            var pair = sortProperty.trim().split(" ");
            var propName = pair[0].trim();
            if (propName.equals(name) && pair.length > 1) {
                var order = pair[1].trim();
                if (order.equals(IPSMetadataQueryService.SORT_ORDER_ASCEND) ||
                        order.equals(IPSMetadataQueryService.SORT_ORDER_DESCEND)) {
                    return order;
                }
            }
        }
        return sortOrder;
    }
}
