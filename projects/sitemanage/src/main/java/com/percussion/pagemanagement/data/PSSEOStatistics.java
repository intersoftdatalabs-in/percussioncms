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
package com.percussion.pagemanagement.data;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serial;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;
import org.apache.commons.lang3.StringUtils;

/**
 * Encapsulates search engine optimization statistics for a Page. Includes detected issues and
 * overall severity.
 *
 * @author Sunny Sal
 */
@XmlRootElement(name = "SEOStatistics")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PSSEOStatistics {

  @Serial private static final long serialVersionUID = 3197862964060713693L;

  @NotNull private PSPageSummary pageSummary;

  @NotNull @NotBlank private String path;

  @NotNull private Set<SEO_ISSUE> issues = EnumSet.noneOf(SEO_ISSUE.class);

  private int severity;
  private String keyword;
  private boolean keywordPresent;
  private String summary;

  // Static sets for issue grouping
  private static final Set<SEO_ISSUE> TITLE_ISSUES =
      Collections.unmodifiableSet(EnumSet.of(SEO_ISSUE.DEFAULT_TITLE, SEO_ISSUE.TITLE_TOO_LONG));
  private static final Set<SEO_ISSUE> DESCRIPTION_ISSUES =
      Collections.unmodifiableSet(
          EnumSet.of(SEO_ISSUE.MISSING_DESCRIPTION, SEO_ISSUE.DESCRIPTION_TOO_LONG));
  private static final Set<SEO_ISSUE> KEYWORD_ISSUES =
      Collections.unmodifiableSet(
          EnumSet.of(
              SEO_ISSUE.MISSING_KEYWORD_TITLE,
              SEO_ISSUE.MISSING_KEYWORD_DESCRIPTION,
              SEO_ISSUE.MISSING_KEYWORD_LINK));
  private static final Set<Set<SEO_ISSUE>> SINGLE_WEIGHT_ISSUES =
      Collections.unmodifiableSet(Set.of(DESCRIPTION_ISSUES));
  private static double totalIssueCount = -1;

  public PSSEOStatistics() {
    // Default constructor for serialization
  }

  /**
   * Constructs a statistics object from a page summary.
   *
   * @param pageSummary the summary for which SEO statistics will be generated, not null.
   * @param path the finder path of the page, not blank.
   * @param keyword the keyword to use in analysis.
   */
  public PSSEOStatistics(PSPageSummary pageSummary, String path, String keyword) {
    requireNonNull(pageSummary, "pageSummary must not be null");
    if (StringUtils.isBlank(path)) {
      throw new IllegalArgumentException("path must not be blank");
    }
    this.pageSummary = pageSummary;
    this.path = path;
    this.keyword = keyword;
    analyze();
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Set<SEO_ISSUE> getIssues() {
    return issues;
  }

  public void setIssues(Set<SEO_ISSUE> issues) {
    this.issues = (issues == null) ? EnumSet.noneOf(SEO_ISSUE.class) : EnumSet.copyOf(issues);
  }

  public int getSeverity() {
    return severity;
  }

  public void setSeverity(int severity) {
    this.severity = severity;
  }

  public PSPageSummary getPageSummary() {
    return pageSummary;
  }

  public void setPageSummary(PSPageSummary pageSummary) {
    this.pageSummary = pageSummary;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public boolean isKeywordPresent() {
    return keywordPresent;
  }

  public void setKeywordPresent(boolean keywordPresent) {
    this.keywordPresent = keywordPresent;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  /**
   * Returns the SEO severity level of the page.
   *
   * @return never null.
   */
  public SEO_SEVERITY getSeverityLevel() {
    if (severity == 100) {
      return SEO_SEVERITY.SEVERE;
    } else if (severity >= 75) {
      return SEO_SEVERITY.HIGH;
    } else if (severity >= 50) {
      return SEO_SEVERITY.MEDIUM;
    } else if (severity >= 25) {
      return SEO_SEVERITY.MODERATE;
    } else {
      return SEO_SEVERITY.ALL;
    }
  }

  /**
   * Performs an analysis of the encapsulated Page to determine its SEO rating. Issues are stored
   * and severity is computed based on the number of issues found.
   */
  private void analyze() {
    if (pageSummary != null) {
      analyzeKeyword();
      var sDescr = pageSummary.getDescription();
      var sTitle = pageSummary.getTitle();

      if (sTitle.equalsIgnoreCase(pageSummary.getLinkTitle())) {
        issues.add(SEO_ISSUE.DEFAULT_TITLE);
      }
      if (sTitle.length() > 70) {
        issues.add(SEO_ISSUE.TITLE_TOO_LONG);
      }
      if (StringUtils.isBlank(sDescr)) {
        issues.add(SEO_ISSUE.MISSING_DESCRIPTION);
      } else if (sDescr.length() > 150) {
        issues.add(SEO_ISSUE.DESCRIPTION_TOO_LONG);
      }
    }
    double issuesLength = issues.size();
    double total = getTotalIssueCount();
    if (keywordPresent) {
      total--;
    } else {
      total -= KEYWORD_ISSUES.size();
    }
    severity = (int) Math.round((issuesLength / total) * 100);
  }

  /**
   * Calculates the number of possible issues for a Page. Used in {@link #analyze()} to compute the
   * severity level.
   *
   * @return the total number of issues which could be encountered for the Page.
   */
  private double getTotalIssueCount() {
    if (totalIssueCount == -1) {
      int singleWeightIssueCount = SINGLE_WEIGHT_ISSUES.stream().mapToInt(Set::size).sum();
      totalIssueCount =
          SEO_ISSUE.values().length - singleWeightIssueCount + SINGLE_WEIGHT_ISSUES.size();
    }
    return totalIssueCount;
  }

  /**
   * Analyzes the page summary for correct usage of the keyword. Checks description, link, and title
   * for keyword presence.
   */
  private void analyzeKeyword() {
    if (pageSummary == null) {
      return;
    }
    if (StringUtils.isNotBlank(keyword)) {
      var sDescr = pageSummary.getDescription();
      var sLink = pageSummary.getLinkTitle();
      var sTitle = pageSummary.getTitle();

      boolean inDescr = containsKeyword(sDescr);
      boolean inLink =
          containsKeyword(sLink)
              || containsKeyword(sLink, keyword.replace(" ", "_"), '_')
              || containsKeyword(sLink, keyword.replace(" ", "-"), '-');
      boolean inTitle = containsKeyword(sTitle);

      if (inDescr || inLink || inTitle) {
        keywordPresent = true;
        if (inDescr && !inLink) {
          issues.add(SEO_ISSUE.MISSING_KEYWORD_LINK);
        }
        if (inDescr && !inTitle) {
          issues.add(SEO_ISSUE.MISSING_KEYWORD_TITLE);
        }
        if (inLink && !inDescr) {
          issues.add(SEO_ISSUE.MISSING_KEYWORD_DESCRIPTION);
        }
        if (inLink && !inTitle) {
          issues.add(SEO_ISSUE.MISSING_KEYWORD_TITLE);
        }
        if (inTitle && !inDescr) {
          issues.add(SEO_ISSUE.MISSING_KEYWORD_DESCRIPTION);
        }
        if (inTitle && !inLink) {
          issues.add(SEO_ISSUE.MISSING_KEYWORD_LINK);
        }
      } else {
        keywordPresent = false;
      }
    }
  }

  /**
   * Convenience method that calls {@link #containsKeyword(String, String, char)} as
   * containsKeyword(str, keyword, ' ').
   */
  private boolean containsKeyword(String str) {
    return containsKeyword(str, keyword, ' ');
  }

  /**
   * Determines if the specified string contains the keyword. Whole word comparison,
   * case-insensitive.
   */
  private boolean containsKeyword(String str, String key, char separator) {
    if (StringUtils.isBlank(str)) {
      return false;
    }
    var lowerStr = str.toLowerCase();
    var lowerKey = key.toLowerCase();
    return lowerStr.equals(lowerKey)
        || lowerStr.startsWith(lowerKey + String.valueOf(separator))
        || lowerStr.endsWith(separator + lowerKey)
        || lowerStr.contains(separator + lowerKey + separator);
  }

  /** Defines the possible SEO issues which could be discovered for a Page. */
  public enum SEO_ISSUE {
    DEFAULT_TITLE,
    MISSING_DESCRIPTION,
    TITLE_TOO_LONG,
    DESCRIPTION_TOO_LONG,
    MISSING_KEYWORD_DESCRIPTION,
    MISSING_KEYWORD_TITLE,
    MISSING_KEYWORD_LINK
  }

  /** Defines the levels of severity of SEO non-compliance. */
  public enum SEO_SEVERITY {
    ALL,
    MODERATE,
    MEDIUM,
    HIGH,
    SEVERE
  }
}
