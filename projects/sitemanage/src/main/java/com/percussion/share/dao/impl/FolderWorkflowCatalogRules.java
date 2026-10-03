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

import com.percussion.pathmanagement.data.PSFolderWorkflowCatalog;
import com.percussion.pathmanagement.data.PSFolderWorkflowChoice;
import com.percussion.services.catalog.data.PSObjectSummary;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the folder workflow catalog from workflow summaries. Ids are the workflow UUID stored on
 * {@code sys_workflowid}. A non-positive id is omitted (#5104).
 */
public final class FolderWorkflowCatalogRules {
  private FolderWorkflowCatalogRules() {}

  public static PSFolderWorkflowCatalog fromSummaries(List<PSObjectSummary> sums) {
    Map<String, PSFolderWorkflowChoice> byId = new LinkedHashMap<>();
    if (sums != null) {
      for (PSObjectSummary sum : sums) {
        PSFolderWorkflowChoice choice = toChoice(sum);
        if (choice != null) {
          byId.putIfAbsent(choice.getId(), choice);
        }
      }
    }
    List<PSFolderWorkflowChoice> choices = new ArrayList<>(byId.values());
    choices.sort(
        Comparator.comparing(
            PSFolderWorkflowChoice::getName, String.CASE_INSENSITIVE_ORDER));
    PSFolderWorkflowCatalog catalog = new PSFolderWorkflowCatalog();
    catalog.setChoices(choices);
    return catalog;
  }

  public static boolean contains(PSFolderWorkflowCatalog catalog, int workflowId) {
    if (catalog == null || workflowId <= 0 || catalog.getChoices() == null) {
      return false;
    }
    String id = Integer.toString(workflowId);
    for (PSFolderWorkflowChoice choice : catalog.getChoices()) {
      if (choice != null && id.equals(trim(choice.getId()))) {
        return true;
      }
    }
    return false;
  }

  private static PSFolderWorkflowChoice toChoice(PSObjectSummary sum) {
    if (sum == null || sum.getGUID() == null) {
      return null;
    }
    int id = sum.getGUID().getUUID();
    if (id <= 0) {
      return null;
    }
    String idText = Integer.toString(id);
    String name = sum.getName();
    if (name == null || name.isBlank()) {
      name = idText;
    } else {
      name = name.trim();
    }
    PSFolderWorkflowChoice choice = new PSFolderWorkflowChoice();
    choice.setId(idText);
    choice.setName(name);
    return choice;
  }

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }
}
