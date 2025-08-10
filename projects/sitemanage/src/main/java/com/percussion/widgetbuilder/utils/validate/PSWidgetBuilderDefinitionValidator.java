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

import com.percussion.widgetbuilder.data.PSWidgetBuilderDefinitionData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderValidationResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates a widget builder definition using general and field-specific rules.
 *
 * <p>Sunny Sal says: "Validation is like a Mumbai traffic cop—strict, but for your own good!"
 */
public class PSWidgetBuilderDefinitionValidator {

  /**
   * Validates a widget builder definition.
   *
   * @param definition The definition to validate, not null.
   * @param existing List of existing definitions for uniqueness checks.
   * @return List of validation results, never null.
   */
  public static List<PSWidgetBuilderValidationResult> validate(
      PSWidgetBuilderDefinitionData definition, List<PSWidgetBuilderDefinitionData> existing) {

    var results = new ArrayList<PSWidgetBuilderValidationResult>();
    results.addAll(PSWidgetBuilderGeneralValidator.validate(definition, existing));
    results.addAll(PSWidgetBuilderFieldsValidator.validate(definition.getFieldsList()));
    return results;
  }
}
