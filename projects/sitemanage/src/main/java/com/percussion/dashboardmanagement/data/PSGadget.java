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
package com.percussion.dashboardmanagement.data;

import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;

@XmlRootElement(name = "gadget")
@XmlAccessorType(XmlAccessType.FIELD)
public class PSGadget extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;
  private Integer instanceId;
  private String url;
  private Integer col, row;

  @XmlTransient private boolean expanded = true;

  private HashMap<String, String> settings = new HashMap<>();

  public Integer getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(Integer instanceId) {
    this.instanceId = instanceId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Integer getCol() {
    return col;
  }

  public void setCol(Integer col) {
    this.col = col;
  }

  public Integer getRow() {
    return row;
  }

  public void setRow(Integer row) {
    this.row = row;
  }

  public boolean isExpanded() {
    return expanded;
  }

  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
  }

  public Map<String, String> getSettings() {
    return settings;
  }

  @SuppressWarnings("unchecked")
  public void setSettings(Map<String, String> settings) {
    if (settings == null) {
      this.settings = null;
    } else if (settings instanceof HashMap) {
      this.settings = (HashMap<String, String>) settings;
    } else {
      this.settings = new HashMap<>(settings);
    }
  }

  @Override
  public String toString() {
    var fileName = url != null ? FilenameUtils.getName(url) : "N/A";
    return String.format(
        "File: %s - Column: %s - Row: %s - Settings: %s", fileName, col, row, settings);
  }
}
