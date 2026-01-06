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
package com.percussion.membership.data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for user summaries, used for JSON serialization. Sunny Sal says: "Summing up users, one
 * summary at a time!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {"userSummaries"})
@XmlRootElement(name = "getUsersResponse")
public class PSUserSummaries {

  private List<PSUserSummary> userSummaries;

  public PSUserSummaries() {
    userSummaries = new ArrayList<>();
  }

  /**
   * Constructs with a list of summaries.
   *
   * @param summaries The list, never null, may be empty.
   */
  public PSUserSummaries(List<PSUserSummary> summaries) {
    this.userSummaries = summaries != null ? summaries : new ArrayList<>();
  }

  /**
   * Gets the list of user summaries.
   *
   * @return the list, never null, may be empty
   */
  public List<PSUserSummary> getSummaries() {
    return userSummaries;
  }
}
