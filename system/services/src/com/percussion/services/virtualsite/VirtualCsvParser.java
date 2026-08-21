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
package com.percussion.services.virtualsite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * RFC 4180-style CSV reader for Virtual Site {@code csv-filesystem} sources.
 *
 * <p>Required header columns (case-insensitive): {@code id}, {@code title}, {@code body}. Optional:
 * {@code path}, {@code order}. Missing required columns fail closed.
 */
public final class VirtualCsvParser {

  public static final String COL_ID = "id";
  public static final String COL_TITLE = "title";
  public static final String COL_BODY = "body";
  public static final String COL_PATH = "path";
  public static final String COL_ORDER = "order";

  private VirtualCsvParser() {}

  /**
   * Parse UTF-8 CSV text into rows keyed by lowercase header names.
   *
   * @param text full file content, never null
   * @param sourceLabel path label for error messages
   * @return row list (empty when only a header is present)
   * @throws VirtualSiteException when the header or row contract is violated
   */
  public static List<Map<String, String>> parse(String text, String sourceLabel)
      throws VirtualSiteException {
    Objects.requireNonNull(text, "text");
    String label = sourceLabel != null ? sourceLabel : "csv";
    String normalized = stripBom(text).replace("\r\n", "\n").replace('\r', '\n');
    List<List<String>> records = parseRecords(normalized, label);
    if (records.isEmpty()) {
      throw new VirtualSiteException("CSV has no header row: " + label);
    }
    List<String> header = records.get(0);
    Map<String, Integer> indexByName = headerIndex(header, label);
    requireColumn(indexByName, COL_ID, label);
    requireColumn(indexByName, COL_TITLE, label);
    requireColumn(indexByName, COL_BODY, label);

    List<Map<String, String>> rows = new ArrayList<>();
    for (int r = 1; r < records.size(); r++) {
      List<String> fields = records.get(r);
      if (isEmptyRecord(fields)) {
        continue;
      }
      if (fields.size() != header.size()) {
        throw new VirtualSiteException(
            "CSV row "
                + (r + 1)
                + " has "
                + fields.size()
                + " field(s) but header has "
                + header.size()
                + " in "
                + label);
      }
      Map<String, String> row = new LinkedHashMap<>();
      for (Map.Entry<String, Integer> e : indexByName.entrySet()) {
        int idx = e.getValue();
        String value = idx < fields.size() ? fields.get(idx) : "";
        row.put(e.getKey(), value);
      }
      rows.add(Collections.unmodifiableMap(row));
    }
    return rows;
  }

  static String cell(Map<String, String> row, String column) {
    if (row == null || column == null) {
      return "";
    }
    String v = row.get(column.toLowerCase(Locale.ROOT));
    return v != null ? v : "";
  }

  private static Map<String, Integer> headerIndex(List<String> header, String label)
      throws VirtualSiteException {
    Map<String, Integer> index = new LinkedHashMap<>();
    for (int i = 0; i < header.size(); i++) {
      String raw = header.get(i);
      String name = raw != null ? raw.trim().toLowerCase(Locale.ROOT) : "";
      if (name.isEmpty()) {
        throw new VirtualSiteException("CSV header column " + (i + 1) + " is blank in " + label);
      }
      if (index.containsKey(name)) {
        throw new VirtualSiteException("Duplicate CSV header '" + name + "' in " + label);
      }
      index.put(name, i);
    }
    return index;
  }

  private static void requireColumn(Map<String, Integer> index, String name, String label)
      throws VirtualSiteException {
    if (!index.containsKey(name)) {
      throw new VirtualSiteException(
          "CSV missing required column '" + name + "' in " + label);
    }
  }

  private static boolean isEmptyRecord(List<String> fields) {
    for (String f : fields) {
      if (f != null && !f.isBlank()) {
        return false;
      }
    }
    return true;
  }

  private static String stripBom(String text) {
    if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
      return text.substring(1);
    }
    return text;
  }

  private static List<List<String>> parseRecords(String text, String label)
      throws VirtualSiteException {
    List<List<String>> records = new ArrayList<>();
    List<String> fields = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean inQuotes = false;
    int line = 1;
    int i = 0;
    int n = text.length();
    while (i < n) {
      char c = text.charAt(i);
      if (inQuotes) {
        if (c == '"') {
          if (i + 1 < n && text.charAt(i + 1) == '"') {
            field.append('"');
            i += 2;
            continue;
          }
          inQuotes = false;
          i++;
          continue;
        }
        if (c == '\n') {
          line++;
        }
        field.append(c);
        i++;
        continue;
      }
      if (c == '"') {
        inQuotes = true;
        i++;
        continue;
      }
      if (c == ',') {
        fields.add(field.toString());
        field.setLength(0);
        i++;
        continue;
      }
      if (c == '\n') {
        fields.add(field.toString());
        field.setLength(0);
        records.add(fields);
        fields = new ArrayList<>();
        line++;
        i++;
        continue;
      }
      field.append(c);
      i++;
    }
    if (inQuotes) {
      throw new VirtualSiteException("Unclosed quoted CSV field starting near line " + line + " in " + label);
    }
    // Trailing content or a last record without a terminating newline.
    if (field.length() > 0 || !fields.isEmpty()) {
      fields.add(field.toString());
      records.add(fields);
    }
    return records;
  }
}
