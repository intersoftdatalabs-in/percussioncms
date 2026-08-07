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

package com.percussion.services.pipeline.model;

import java.util.Objects;

/** Result pager stage (classic {@code PSResultPager}). */
public class PagerStageIr {

  private boolean present;
  private int maxRowsPerPage;
  private int maxPages;
  private int maxPageLinks;

  public boolean isPresent() {
    return present;
  }

  public void setPresent(boolean present) {
    this.present = present;
  }

  public int getMaxRowsPerPage() {
    return maxRowsPerPage;
  }

  public void setMaxRowsPerPage(int maxRowsPerPage) {
    this.maxRowsPerPage = maxRowsPerPage;
  }

  public int getMaxPages() {
    return maxPages;
  }

  public void setMaxPages(int maxPages) {
    this.maxPages = maxPages;
  }

  public int getMaxPageLinks() {
    return maxPageLinks;
  }

  public void setMaxPageLinks(int maxPageLinks) {
    this.maxPageLinks = maxPageLinks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PagerStageIr that)) {
      return false;
    }
    return present == that.present
        && maxRowsPerPage == that.maxRowsPerPage
        && maxPages == that.maxPages
        && maxPageLinks == that.maxPageLinks;
  }

  @Override
  public int hashCode() {
    return Objects.hash(present, maxRowsPerPage, maxPages, maxPageLinks);
  }
}
