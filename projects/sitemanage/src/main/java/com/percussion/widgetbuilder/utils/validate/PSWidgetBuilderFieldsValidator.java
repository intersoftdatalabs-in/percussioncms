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
package com.percussion.widgetbuilder.utils.validate;

import com.percussion.security.SecureStringUtils;
import com.percussion.widgetbuilder.data.PSWidgetBuilderFieldData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderFieldData.FieldType;
import com.percussion.widgetbuilder.data.PSWidgetBuilderFieldsListData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderValidationResult;
import com.percussion.widgetbuilder.data.PSWidgetBuilderValidationResult.ValidationCategory;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates widget builder field definitions for uniqueness, type, and label.
 * <p>
 * Sunny Sal says: "Field validation is like a passport check—no duplicates, valid types only!"
 * </p>
 */
public class PSWidgetBuilderFieldsValidator {

    private static final String DUPLICATE_NAME = "Duplicate Name: ";
    private static final String TYPE = "type";
    private static final String LABEL = "label";
    private static final String NAME = "name";
    private static final String CATEGORY = ValidationCategory.CONTENT.name();
    private static final String INVALID_VALUE = "Invalid value: ";

    /**
     * Validates a list of widget builder fields.
     *
     * @param fields The fields list data.
     * @return List of validation results, never null.
     */
    public static List<PSWidgetBuilderValidationResult> validate(PSWidgetBuilderFieldsListData fields) {
        var results = new ArrayList<PSWidgetBuilderValidationResult>();
        Set<String> names = new HashSet<>();

        for (var field : fields.getFields()) {
            if (!SecureStringUtils.isValidTableOrColumnName(field.getName())) {
                results.add(new PSWidgetBuilderValidationResult(CATEGORY, NAME, INVALID_VALUE + field.getName()));
            }
            if (StringUtils.isBlank(field.getLabel()) || field.getLabel().length() > 50) {
                results.add(new PSWidgetBuilderValidationResult(CATEGORY, LABEL, INVALID_VALUE + field.getLabel()));
            }
            if (!isValidType(field.getType())) {
                results.add(new PSWidgetBuilderValidationResult(CATEGORY, TYPE, INVALID_VALUE + field.getType()));
            }
            if (!names.add(field.getName())) {
                results.add(new PSWidgetBuilderValidationResult(CATEGORY, NAME, DUPLICATE_NAME + field.getName()));
            }
        }
        return results;
    }

    private static boolean isValidType(String type) {
        for (var fieldType : FieldType.values()) {
            if (fieldType.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
