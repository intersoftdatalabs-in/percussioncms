/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.rest.keywords;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "Keyword")
@JsonRootName("Keyword")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Keyword definition with optional choices")
public class KeywordSummary {

  private Guid guid;
  private String label;
  private String value;
  private String description;
  private Integer sequence;

  /**
   * Live Jackson uses the getter as the collection setter and adds elements here without calling
   * {@link #setChoices(List)}. The list must be this field, not a throwaway from {@link
   * #getChoices()}. An omitted property leaves it empty and does not replace stored choices.
   */
  private List<KeywordChoiceSummary> choices = new ArrayList<>();

  /**
   * True when {@link #setChoices(List)} was called with a list, including an empty array. Getter
   * mutation of a non-empty {@link #choices} does not set this; {@link #isChoicesSpecified()}
   * checks both.
   */
  @JsonIgnore @XmlTransient private boolean choicesSpecified;

  public KeywordSummary() {}

  public Guid getGuid() {
    return guid;
  }

  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getSequence() {
    return sequence;
  }

  public void setSequence(Integer sequence) {
    this.sequence = sequence;
  }

  public List<KeywordChoiceSummary> getChoices() {
    if (choices == null) {
      choices = new ArrayList<>();
    }
    return choices;
  }

  /**
   * @param choices the choice list when the request included {@code choices}; {@code null} means
   *     the property was not a list and does not replace stored choices
   */
  public void setChoices(List<KeywordChoiceSummary> choices) {
    if (choices == null) {
      this.choicesSpecified = false;
      this.choices = new ArrayList<>();
      return;
    }
    this.choicesSpecified = true;
    this.choices = choices;
  }

  /**
   * Whether the request included {@code choices}, including an empty list. True when {@link
   * #setChoices(List)} stored a list, or when Jackson added choices through {@link #getChoices()}
   * without that setter.
   */
  @JsonIgnore
  @XmlTransient
  public boolean isChoicesSpecified() {
    return choicesSpecified || (choices != null && !choices.isEmpty());
  }
}
