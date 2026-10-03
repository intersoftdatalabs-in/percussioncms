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

import com.percussion.pathmanagement.data.PSFolderCommunityCatalog;
import com.percussion.pathmanagement.data.PSFolderCommunityChoice;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the folder community catalog. Ids are the community id stored on the folder. A
 * non-positive id, or an id that does not fit in the folder's {@code int} field, is omitted
 * (#5105).
 */
public final class FolderCommunityCatalogRules {
  private FolderCommunityCatalogRules() {}

  public static PSFolderCommunityCatalog fromChoices(List<PSFolderCommunityChoice> raw) {
    Map<String, PSFolderCommunityChoice> byId = new LinkedHashMap<>();
    if (raw != null) {
      for (PSFolderCommunityChoice choice : raw) {
        PSFolderCommunityChoice cleaned = clean(choice);
        if (cleaned != null) {
          byId.putIfAbsent(cleaned.getId(), cleaned);
        }
      }
    }
    List<PSFolderCommunityChoice> choices = new ArrayList<>(byId.values());
    choices.sort(
        Comparator.comparing(PSFolderCommunityChoice::getName, String.CASE_INSENSITIVE_ORDER));
    PSFolderCommunityCatalog catalog = new PSFolderCommunityCatalog();
    catalog.setChoices(choices);
    return catalog;
  }

  public static boolean contains(PSFolderCommunityCatalog catalog, int communityId) {
    return nameOf(catalog, communityId) != null;
  }

  /** Display name for a positive catalog id, or {@code null} when the id is not listed. */
  public static String nameOf(PSFolderCommunityCatalog catalog, int communityId) {
    if (catalog == null || communityId <= 0 || catalog.getChoices() == null) {
      return null;
    }
    String id = Integer.toString(communityId);
    for (PSFolderCommunityChoice choice : catalog.getChoices()) {
      if (choice != null && id.equals(trim(choice.getId()))) {
        String name = trim(choice.getName());
        return name.isEmpty() ? id : name;
      }
    }
    return null;
  }

  private static PSFolderCommunityChoice clean(PSFolderCommunityChoice choice) {
    if (choice == null) {
      return null;
    }
    String idText = trim(choice.getId());
    int id;
    try {
      long parsed = Long.parseLong(idText);
      if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
        return null;
      }
      id = (int) parsed;
    } catch (NumberFormatException ex) {
      return null;
    }
    String name = trim(choice.getName());
    if (name.isEmpty()) {
      name = Integer.toString(id);
    }
    PSFolderCommunityChoice out = new PSFolderCommunityChoice();
    out.setId(Integer.toString(id));
    out.setName(name);
    return out;
  }

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }
}
