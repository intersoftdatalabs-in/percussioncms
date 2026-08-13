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

package com.percussion.rest.views;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Paged result envelope for {@code POST /rest/views/{idOrName}/execute}.
 *
 * <p>Peer of {@code SearchExecuteResult}; uses {@link ViewResultItem} so Views and Searches do
 * not share a catalog identity on the wire.
 */
@XmlRootElement(name = "ViewExecuteResult")
@JsonRootName("ViewExecuteResult")
@Schema(description = "Paged Explorer-ready results from executing a CX design view")
public class ViewExecuteResult {

  private List<ViewResultItem> children = new ArrayList<>();
  private int totalCount;
  private int startIndex = 1;
  private String viewName;
  private String displayFormatId;

  public List<ViewResultItem> getChildren() {
    return children;
  }

  public void setChildren(List<ViewResultItem> children) {
    this.children = children != null ? children : new ArrayList<>();
  }

  public int getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
  }

  public int getStartIndex() {
    return startIndex;
  }

  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  public String getViewName() {
    return viewName;
  }

  public void setViewName(String viewName) {
    this.viewName = viewName;
  }

  public String getDisplayFormatId() {
    return displayFormatId;
  }

  public void setDisplayFormatId(String displayFormatId) {
    this.displayFormatId = displayFormatId;
  }
}
