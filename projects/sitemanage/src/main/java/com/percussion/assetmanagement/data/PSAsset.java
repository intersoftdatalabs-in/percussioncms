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

package com.percussion.assetmanagement.data;

import com.percussion.share.data.IPSContentItem;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;

/** Represents a CMS Asset with arbitrary fields. */
@XmlRootElement(name = "Asset")
public class PSAsset extends PSAssetSummary implements IPSContentItem {

  private static final long serialVersionUID = 8252999104256582955L;

  private HashMap<String, Object> fields = new HashMap<>();

  /**
   * Gets the asset fields.
   *
   * @return a map of field names to values; never {@code null}.
   */
  public Map<String, Object> getFields() {
    return fields;
  }

  /**
   * Sets the asset fields.
   *
   * @param fields the fields map; must not be {@code null}.
   */
  @SuppressWarnings("unchecked")
  public void setFields(Map<String, Object> fields) {
    if (fields == null) {
      this.fields = null;
    } else if (fields instanceof HashMap) {
      this.fields = (HashMap<String, Object>) fields;
    } else {
      this.fields = new HashMap<>(fields);
    }
  }
}
