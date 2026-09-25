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
import com.percussion.share.dao.impl.PSContentItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;

/**
 * Maps {@link PSContentItem} field maps to the React editor payload. Binary / object values are
 * omitted. System fields except {@code sys_title} are omitted from both read and write.
 */
public final class PSItemEditorFieldsMapper {

  private PSItemEditorFieldsMapper() {}

  public static final String COMMUNITY_FIELD = "sys_communityid";

  public static boolean isEditableFieldName(String name) {
    if (StringUtils.isBlank(name)) {
      return false;
    }
    if ("sys_title".equals(name) || COMMUNITY_FIELD.equals(name)) {
      return true;
    }
    return !name.startsWith("sys_");
  }

  /**
   * Copy the content-status community onto the editor payload when the item field map omitted
   * {@code sys_communityid}. A present value, including blank, is left alone so an explicit clear
   * is not replaced by the previous id. {@code communityId} &lt;= 0 is ignored.
   */
  public static void fillCommunityWhenAbsent(PSItemEditorFields out, int communityId) {
    if (out == null || communityId <= 0) {
      return;
    }
    List<PSItemEditorField> fields = out.getFields();
    if (fields == null) {
      fields = new ArrayList<>();
      out.setFields(fields);
    }
    for (PSItemEditorField field : fields) {
      if (field != null && COMMUNITY_FIELD.equals(field.getName())) {
        return;
      }
    }
    fields.add(new PSItemEditorField(COMMUNITY_FIELD, Integer.toString(communityId)));
  }

  public static String stringifyFieldValue(Object raw) {
    if (raw == null) {
      return "";
    }
    if (raw instanceof byte[]) {
      return null;
    }
    if (raw instanceof Collection<?> col) {
      if (col.isEmpty()) {
        return "";
      }
      Object first = col.iterator().next();
      return stringifyFieldValue(first);
    }
    if (raw instanceof Date date) {
      return Instant.ofEpochMilli(date.getTime()).toString();
    }
    if (raw instanceof Number || raw instanceof Boolean || raw instanceof CharSequence) {
      return String.valueOf(raw);
    }
    return null;
  }

  public static PSItemEditorFields fromContentItem(PSContentItem item, String checkoutUser) {
    PSItemEditorFields out = new PSItemEditorFields();
    if (item == null) {
      return out;
    }
    out.setContentId(item.getId());
    out.setContentType(item.getType());
    out.setName(item.getName());
    out.setCheckoutUser(checkoutUser);
    Map<String, Object> fields = item.getFields();
    if (fields == null || fields.isEmpty()) {
      return out;
    }
    List<PSItemEditorField> rows = new ArrayList<>();
    for (Map.Entry<String, Object> entry : new TreeMap<>(fields).entrySet()) {
      String name = entry.getKey();
      if (!isEditableFieldName(name)) {
        continue;
      }
      String value = stringifyFieldValue(entry.getValue());
      if (value == null) {
        continue;
      }
      rows.add(new PSItemEditorField(name, value));
    }
    out.setFields(rows);
    return out;
  }

  /**
   * NUL cannot be stored in item field text. Callers map this to HTTP 400 before writing.
   *
   * @param updates field values from the editor PUT; may be {@code null}
   */
  public static void rejectEmbeddedNul(List<PSItemEditorField> updates) {
    if (updates == null || updates.isEmpty()) {
      return;
    }
    for (PSItemEditorField update : updates) {
      if (update == null || update.getValue() == null) {
        continue;
      }
      if (update.getValue().indexOf('\0') >= 0) {
        String name = update.getName() == null ? "" : update.getName();
        throw new IllegalArgumentException(
            "Field \"" + name + "\" contains a character that cannot be saved.");
      }
    }
  }

  /**
   * Numeric catalog types must be a number inside any inclusive bounds. Callers map this to HTTP
   * 400 before writing.
   */
  public static void rejectInvalidNumeric(List<PSItemEditorField> updates) {
    if (updates == null || updates.isEmpty()) {
      return;
    }
    for (PSItemEditorField update : updates) {
      if (update == null) {
        continue;
      }
      String message =
          PSItemEditorNumeric.rejectionMessage(
              update.getName(),
              update.getValue(),
              update.getDataType(),
              update.getMinimum(),
              update.getMaximum());
      if (message != null) {
        throw new IllegalArgumentException(message);
      }
    }
  }

  public static void applyUpdates(PSContentItem item, List<PSItemEditorField> updates) {
    if (item == null || updates == null || updates.isEmpty()) {
      return;
    }
    Map<String, Object> fields = item.getFields();
    for (PSItemEditorField update : updates) {
      if (update == null || !isEditableFieldName(update.getName())) {
        continue;
      }
      fields.put(update.getName(), update.getValue() == null ? "" : update.getValue());
    }
    item.setFields(fields);
  }
}
