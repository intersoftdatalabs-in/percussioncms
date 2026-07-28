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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple container. Its use is just to add a root element name for Jersey to spit out when
 * serializing to JSON.
 *
 * @author leonardohildt
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {"formsInfo"})
public class PSFormSummaries {
  /**
   * No-arg constructor required by the JAXB binding provider. Application code can populate
   * the container via the setters or by adding entries to the result of
   * {@link #getSummaries()}.
   */
  public PSFormSummaries() {}

  private List<PSFormSummary> formsInfo = new ArrayList<>();

  /**
   * Returns the list of form summaries backing this container, lazily initializing it when
   * accessed for the first time after deserialization.
   *
   * @return the live list of summaries, never <code>null</code>.
   */
  public List<PSFormSummary> getSummaries() {
    if (formsInfo == null) formsInfo = new ArrayList<>();
    return formsInfo;
  }

  /**
   * Replaces the list of summaries backing this container.
   *
   * @param formSummaries the new summaries, may be <code>null</code> in which case the
   *     container will lazily create an empty list on the next read.
   */
  public void setSummaries(List<PSFormSummary> formSummaries) {
    this.formsInfo = formSummaries;
  }
}
