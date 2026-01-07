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
package com.percussion.sitemanage.service;

import com.percussion.share.data.PSAbstractDataObject;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

/** Represents a mapping between a page and a template for a specific site. */
@XmlRootElement(name = "PageToTemplatePair")
public class PSPageToTemplatePair extends PSAbstractDataObject
    implements Serializable, Comparable<PSPageToTemplatePair> {

  private static final long serialVersionUID = 1L;

  private String siteId;
  private String pageId;

  public String getSiteId() {
    return siteId;
  }

  public void setSiteId(String siteId) {
    this.siteId = siteId;
  }

  public String getPageId() {
    return pageId;
  }

  public void setPageId(String pageId) {
    this.pageId = pageId;
  }

  @Override
  public int compareTo(PSPageToTemplatePair o) {
    // Compare by siteId then pageId for ordering
    int siteCompare = siteId != null && o.siteId != null ? siteId.compareTo(o.siteId) : 0;
    if (siteCompare != 0) {
      return siteCompare;
    }
    return pageId != null && o.pageId != null ? pageId.compareTo(o.pageId) : 0;
  }
}
