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
package com.percussion.delivery.forms.data;

/**
 * Class to hold form summary info.
 *
 * @author leonardohildt
 */
public class PSFormSummary {
  /**
   * No-arg constructor required by the JAXB binding provider. Application code should use the
   * setters to populate the bean before serializing it.
   */
  public PSFormSummary() {}

  private String name;

  private Long totalForms;

  private Long exportedForms;

  /**
   * Returns the form name represented by this summary.
   *
   * @return the form name, may be <code>null</code>.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the form name represented by this summary.
   *
   * @param name the form name, may be <code>null</code>.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the total number of submissions currently stored for the form.
   *
   * @return the total form count, may be <code>null</code> when uninitialized.
   */
  public Long getTotalForms() {
    return totalForms;
  }

  /**
   * Sets the total number of submissions currently stored for the form.
   *
   * @param totalForms the total form count, may be <code>null</code> when uninitialized.
   */
  public void setTotalForms(Long totalForms) {
    this.totalForms = totalForms;
  }

  /**
   * Returns the number of submissions that have been marked as exported.
   *
   * @return the exported form count, may be <code>null</code> when uninitialized.
   */
  public Long getExportedForms() {
    return exportedForms;
  }

  /**
   * Sets the number of submissions that have been marked as exported.
   *
   * @param exportedForms the exported form count, may be <code>null</code> when uninitialized.
   */
  public void setExportedforms(Long exportedForms) {
    this.exportedForms = exportedForms;
  }
}
