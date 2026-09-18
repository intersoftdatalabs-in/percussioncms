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
package com.percussion.itemmanagement.service.impl;

import com.percussion.itemmanagement.data.PSItemEditorField;
import com.percussion.itemmanagement.data.PSItemEditorFields;
import com.percussion.itemmanagement.data.PSItemRevisionCompareResult;
import com.percussion.itemmanagement.data.PSItemRevisionFieldDiff;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;

/**
 * Field-level compare of two item-revision payloads. Builds revision GUIDs the same way Explorer
 * restore does ({@code {rev}-101-{id}}).
 */
public final class PSItemRevisionCompare {

  private PSItemRevisionCompare() {}

  /**
   * Encode a revision GUID: first GUID segment is the revision. Numeric content ids become {@code
   * {rev}-101-{id}}.
   */
  public static String revisionGuid(String itemId, int revId) {
    String trimmed = itemId == null ? "" : itemId.trim();
    if (trimmed.isEmpty() || revId <= 0) {
      throw new IllegalArgumentException("item id and revision are required");
    }
    if (trimmed.contains("-")) {
      String[] parts = trimmed.split("-", -1);
      parts[0] = String.valueOf(revId);
      return String.join("-", parts);
    }
    return revId + "-101-" + trimmed;
  }

  public static PSItemRevisionCompareResult diff(
      String itemId, int rev1, int rev2, PSItemEditorFields left, PSItemEditorFields right) {
    PSItemRevisionCompareResult out = new PSItemRevisionCompareResult();
    out.setItemId(itemId);
    out.setRev1(rev1);
    out.setRev2(rev2);
    Map<String, String> leftMap = fieldMap(left);
    Map<String, String> rightMap = fieldMap(right);
    TreeSet<String> names = new TreeSet<>();
    names.addAll(leftMap.keySet());
    names.addAll(rightMap.keySet());
    List<PSItemRevisionFieldDiff> rows = new ArrayList<>();
    for (String name : names) {
      String lv = leftMap.getOrDefault(name, "");
      String rv = rightMap.getOrDefault(name, "");
      rows.add(new PSItemRevisionFieldDiff(name, lv, rv, !StringUtils.equals(lv, rv)));
    }
    out.setFields(rows);
    return out;
  }

  private static Map<String, String> fieldMap(PSItemEditorFields fields) {
    Map<String, String> map = new LinkedHashMap<>();
    if (fields == null || fields.getFields() == null) {
      return map;
    }
    for (PSItemEditorField field : fields.getFields()) {
      if (field == null || StringUtils.isBlank(field.getName())) {
        continue;
      }
      map.put(field.getName(), field.getValue() == null ? "" : field.getValue());
    }
    return map;
  }
}
