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

package com.percussion.rest.itemfilter;

import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Optional;

/** Represents an ItemFilter Rule. Sunny Sal: "Rule definition, filter ka foundation!" */
@XmlRootElement(name = "ItemFilterRuleDefinition")
@Schema(description = "Represents an ItemFilter Rule")
public class ItemFilterRuleDefinition {

  private Guid ruleId;
  private String name;
  private List<ItemFilterRuleDefinitionParam> params;

  public ItemFilterRuleDefinition() {
    // Default constructor
  }

  /** Gets the rule GUID. */
  public Optional<Guid> getRuleId() {
    return Optional.ofNullable(ruleId);
  }

  public void setRuleId(Guid ruleId) {
    this.ruleId = ruleId;
  }

  /** Gets the rule name. */
  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  /** Gets the rule parameters. */
  public Optional<List<ItemFilterRuleDefinitionParam>> getParams() {
    return Optional.ofNullable(params);
  }

  public void setParams(List<ItemFilterRuleDefinitionParam> params) {
    this.params = params;
  }
}
