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
package com.percussion.share.dao.impl;

import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.pathmanagement.data.PSFolderDisplayFormatCatalog;
import com.percussion.pathmanagement.data.PSFolderDisplayFormatChoice;
import com.percussion.pathmanagement.data.PSFolderProperties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Folder display-format catalog and id rules (#5131). The persisted folder property is {@code
 * sys_displayformat} (the numeric id). The display-format name on {@code PSFolder} is transient and
 * is never written by these rules. A name that is not a positive id is not a successful assignment.
 *
 * <p>When any loaded format is valid for folders, only those are listed. Otherwise the catalog
 * falls back to every positive id, matching the Explorer list selector when folder-usage flags are
 * empty.
 */
public final class FolderDisplayFormatCatalogRules {
  private FolderDisplayFormatCatalogRules() {}

  /** One display format before catalog filtering. */
  public static final class SourceRow {
    private final int id;
    private final String name;
    private final boolean validForFolder;

    public SourceRow(int id, String name, boolean validForFolder) {
      this.id = id;
      this.name = name;
      this.validForFolder = validForFolder;
    }

    public int id() {
      return id;
    }

    public String name() {
      return name;
    }

    public boolean validForFolder() {
      return validForFolder;
    }
  }

  /** How a wire or property string maps onto {@code sys_displayformat}. */
  public enum IdKind {
    /** Blank, zero, or negative — leave the stored id alone. */
    ABSENT,
    /** Non-numeric text, including a display-format name. Not a successful id. */
    INVALID,
    /** Positive numeric id. */
    POSITIVE
  }

  /** Parsed display-format id. {@link #canonical()} is empty unless {@link IdKind#POSITIVE}. */
  public static final class ParsedId {
    private final IdKind kind;
    private final String canonical;

    private ParsedId(IdKind kind, String canonical) {
      this.kind = kind;
      this.canonical = canonical == null ? "" : canonical;
    }

    public IdKind kind() {
      return kind;
    }

    public String canonical() {
      return canonical;
    }
  }

  public static List<SourceRow> rowsFromFormats(List<PSDisplayFormat> formats) {
    List<SourceRow> rows = new ArrayList<>();
    if (formats == null) {
      return rows;
    }
    for (PSDisplayFormat format : formats) {
      if (format == null) {
        continue;
      }
      String name = trim(format.getDisplayName());
      if (name.isEmpty()) {
        name = trim(format.getInternalName());
      }
      boolean valid = false;
      try {
        valid = format.isValidForFolder();
      } catch (RuntimeException ex) {
        valid = false;
      }
      rows.add(new SourceRow(format.getDisplayId(), name, valid));
    }
    return rows;
  }

  public static PSFolderDisplayFormatCatalog fromFormats(List<PSDisplayFormat> formats) {
    return fromRows(rowsFromFormats(formats));
  }

  public static PSFolderDisplayFormatCatalog fromRows(List<SourceRow> raw) {
    Map<String, PSFolderDisplayFormatChoice> byId = new LinkedHashMap<>();
    Map<String, Boolean> validById = new LinkedHashMap<>();
    boolean anyFolder = false;
    if (raw != null) {
      for (SourceRow row : raw) {
        if (row == null || row.id() <= 0) {
          continue;
        }
        String id = Integer.toString(row.id());
        String name = trim(row.name());
        if (name.isEmpty()) {
          name = id;
        }
        boolean valid = row.validForFolder();
        Boolean previous = validById.get(id);
        if (previous == null || (!previous && valid)) {
          PSFolderDisplayFormatChoice choice = new PSFolderDisplayFormatChoice();
          choice.setId(id);
          choice.setName(name);
          byId.put(id, choice);
          validById.put(id, valid);
        }
        if (valid) {
          anyFolder = true;
        }
      }
    }
    List<PSFolderDisplayFormatChoice> choices = new ArrayList<>();
    for (Map.Entry<String, PSFolderDisplayFormatChoice> entry : byId.entrySet()) {
      if (!anyFolder || Boolean.TRUE.equals(validById.get(entry.getKey()))) {
        choices.add(entry.getValue());
      }
    }
    choices.sort(
        Comparator.comparing(PSFolderDisplayFormatChoice::getName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(PSFolderDisplayFormatChoice::getId));
    PSFolderDisplayFormatCatalog catalog = new PSFolderDisplayFormatCatalog();
    catalog.setChoices(choices);
    return catalog;
  }

  public static ParsedId parseId(String raw) {
    if (raw == null || raw.trim().isEmpty()) {
      return new ParsedId(IdKind.ABSENT, "");
    }
    try {
      int id = Integer.parseInt(raw.trim());
      if (id <= 0) {
        return new ParsedId(IdKind.ABSENT, "");
      }
      return new ParsedId(IdKind.POSITIVE, Integer.toString(id));
    } catch (NumberFormatException ex) {
      return new ParsedId(IdKind.INVALID, "");
    }
  }

  public static boolean sameId(String left, String right) {
    ParsedId a = parseId(left);
    ParsedId b = parseId(right);
    return a.kind() == IdKind.POSITIVE
        && b.kind() == IdKind.POSITIVE
        && a.canonical().equals(b.canonical());
  }

  public static boolean contains(PSFolderDisplayFormatCatalog catalog, String id) {
    return nameOf(catalog, id) != null;
  }

  /** Catalog name for a positive id, or {@code null} when the id is not listed. */
  public static String nameOf(PSFolderDisplayFormatCatalog catalog, String id) {
    ParsedId parsed = parseId(id);
    if (parsed.kind() != IdKind.POSITIVE || catalog == null || catalog.getChoices() == null) {
      return null;
    }
    for (PSFolderDisplayFormatChoice choice : catalog.getChoices()) {
      if (choice != null && parsed.canonical().equals(parseId(choice.getId()).canonical())) {
        String name = trim(choice.getName());
        return name.isEmpty() ? parsed.canonical() : name;
      }
    }
    return null;
  }

  /**
   * Copy the stored id onto the properties DTO and resolve a blank transient name from the catalog.
   * A transient name is shown as-is and is not treated as the persisted value. A name with no
   * positive id does not invent an id.
   */
  public static void copyOntoProperties(
      PSFolderProperties props,
      String storedId,
      String transientName,
      PSFolderDisplayFormatCatalog catalog) {
    if (props == null) {
      return;
    }
    ParsedId parsed = parseId(storedId);
    if (parsed.kind() == IdKind.POSITIVE) {
      props.setDisplayFormatId(parsed.canonical());
    }
    String shown = trim(transientName);
    if (!shown.isEmpty()) {
      props.setDisplayFormatName(shown);
      return;
    }
    if (parsed.kind() == IdKind.POSITIVE) {
      String resolved = nameOf(catalog, parsed.canonical());
      if (resolved != null) {
        props.setDisplayFormatName(resolved);
      }
    }
  }

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }
}
