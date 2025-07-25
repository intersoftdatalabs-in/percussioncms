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
package com.percussion.delivery.forms.data;

/**
 * Class to hold form summary info.
 * <p>
 * Uses Java 11 features and Google Java Style. Immutable fields, builder pattern, and Optional for null safety.
 * </p>
 *
 * @author leonardohildt
 */
public class PSFormSummary {
  private final String name;
  private final Long totalForms;
  private final Long exportedForms;

  private PSFormSummary(Builder builder) {
    this.name = builder.name;
    this.totalForms = builder.totalForms;
    this.exportedForms = builder.exportedForms;
  }

  public String getName() {
    return name;
  }

  public Long getTotalForms() {
    return totalForms;
  }

  public Long getExportedForms() {
    return exportedForms;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private Long totalForms;
    private Long exportedForms;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder totalForms(Long totalForms) {
      this.totalForms = totalForms;
      return this;
    }

    public Builder exportedForms(Long exportedForms) {
      this.exportedForms = exportedForms;
      return this;
    }

    public PSFormSummary build() {
      return new PSFormSummary(this);
    }
  }
}
