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

package com.percussion.rest.actions;

import com.percussion.webservices.PSErrorResultsException;
import java.util.List;

/** Adaptor interface for Action Menu operations. */
public interface IActionMenuAdaptor {

  /** Finds menus matching the given criteria. */
  List<ActionMenu> findMenus(
      String name, String label, Boolean item, Boolean dynamic, Boolean cascading)
      throws PSErrorResultsException;

  /** Finds allowed transitions for the given content and assignment type IDs. */
  List<ActionMenu> findAllowedTransitions(Integer[] contentIds, Integer[] assignmentTypeIds);

  /** Finds allowed content types for the given content IDs. */
  List<ActionMenu> findAllowedContentTypes(Integer[] contentIds);

  /** Finds allowed templates for the given content ID. */
  List<ActionMenu> findAllowedTemplates(Integer contentId, boolean isAA);

  /** Resolve one menu by name or numeric id string. Returns null if missing/unsafe. */
  ActionMenu findMenuByKey(String idOrName);
}
