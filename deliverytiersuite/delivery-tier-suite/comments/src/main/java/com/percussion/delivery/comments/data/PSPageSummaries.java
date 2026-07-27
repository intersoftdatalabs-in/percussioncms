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
package com.percussion.delivery.comments.data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple container. Its use is just to add a root element name for Jersey to spit out when
 * serializing to JSON.
 *
 * @author erikserating
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {"summaries"})
public class PSPageSummaries {
  /** List of {@link PSPageSummary} entries. */
  protected List<PSPageSummary> summaries;

  /** Default no-arg constructor required by JAXB. */
  public PSPageSummaries() {}

  /**
   * Creates a new page summaries container.
   *
   * @param summaries the initial list of summaries, may be {@code null}.
   */
  public PSPageSummaries(List<PSPageSummary> summaries) {
    this.summaries = summaries;
  }

  /**
   * Gets the list of page summaries. Lazily initializes an empty list if needed.
   *
   * @return the list of summaries, never {@code null}.
   */
  public List<PSPageSummary> getSummaries() {
    if (summaries == null) summaries = new ArrayList<>();
    return summaries;
  }
}
