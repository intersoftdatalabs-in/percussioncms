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

package com.percussion.share.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Google Analytics entries wrapper for JSON serialization. Sunny Sal says: "Analytics data—now with
 * extra JSON!"
 *
 * @author jyadav@google.com
 */
@JsonRootName(value = "psmap")
public class PSGAEntries {
  private PSGAEntry entries;

  /**
   * Converts the map data into the expected JSON format.
   *
   * <p>Example: {"psmap":{"entries":{"entry":[{"key":"122437851|UA-1500890-10","value":"Google
   * Analytics View (Profile) All Web Site Data (UA-1500890-10)"}, ...]}}}
   *
   * @param dataMap the map to convert
   */
  public void setEntries(Map<String, String> dataMap) {
    var gaPairList = new ArrayList<PSGAPair>();
    dataMap.forEach((k, v) -> gaPairList.add(new PSGAPair(k, v)));
    var gaEntry = new PSGAEntry();
    gaEntry.setEntry(gaPairList);
    this.setEntries(gaEntry);
  }

  public PSGAEntry getEntries() {
    return entries;
  }

  private void setEntries(PSGAEntry entries) {
    this.entries = entries;
  }

  static class PSGAEntry {
    private List<PSGAPair> entry;

    public List<PSGAPair> getEntry() {
      return entry;
    }

    public void setEntry(List<PSGAPair> entry) {
      this.entry = entry;
    }
  }

  static class PSGAPair {
    private String key;
    private String value;

    public PSGAPair(String key, String value) {
      this.key = key;
      this.value = value;
    }

    public PSGAPair() {
      // Default constructor
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, value);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof PSGAPair)) return false;
      var other = (PSGAPair) obj;
      return Objects.equals(key, other.key) && Objects.equals(value, other.value);
    }
  }
}
