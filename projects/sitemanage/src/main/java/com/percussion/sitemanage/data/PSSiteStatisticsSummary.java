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
package com.percussion.sitemanage.data;

import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;

/** Summary of site statistics and issues. */
@XmlRootElement(name = "SiteSummaryData")
public class PSSiteStatisticsSummary extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  @NotBlank @NotNull private String name;

  private long id;

  private PSSiteStatistics statistics;

  private ArrayList<PSSiteIssueSummary> issues = new ArrayList<>();

  private String abridgedErrorMessage;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getSiteId() {
    return this.id;
  }

  public void setSiteId(long id) {
    this.id = id;
  }

  public Optional<PSSiteStatistics> getStatistics() {
    return Optional.ofNullable(statistics);
  }

  public void setStatistics(PSSiteStatistics statistics) {
    this.statistics = statistics;
  }

  public List<PSSiteIssueSummary> getIssues() {
    return issues;
  }

  @SuppressWarnings("unchecked")
  public void setIssues(List<PSSiteIssueSummary> issues) {
    if (issues == null) {
      this.issues = new ArrayList<>();
    } else if (issues instanceof ArrayList) {
      this.issues = (ArrayList<PSSiteIssueSummary>) issues;
    } else {
      this.issues = new ArrayList<>(issues);
    }
  }

  public void setAbridgedErrorMessage(String message) {
    this.abridgedErrorMessage = message;
  }

  public Optional<String> getAbridgedErrorMessage() {
    return Optional.ofNullable(abridgedErrorMessage);
  }
}
