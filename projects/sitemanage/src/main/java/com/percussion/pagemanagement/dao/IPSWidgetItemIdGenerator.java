// REFACTORED: CP-JAVA11
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
package com.percussion.pagemanagement.dao;

import com.percussion.pagemanagement.data.PSRegionWidgetAssociations;
import com.percussion.pagemanagement.data.PSWidgetItem;

/** Generates IDs for widget items. IDs only need to be unique within the page. */
public interface IPSWidgetItemIdGenerator {

  /**
   * Generates an ID that should be unique over the widget associations.
   *
   * @param widgets never {@code null}.
   * @param item never {@code null}.
   * @return generated ID.
   */
  Long generateId(PSRegionWidgetAssociations widgets, PSWidgetItem item);

  /**
   * Generates and sets IDs for widgets that do not have an ID. Widgets where {@link
   * PSWidgetItem#getId()} is {@code null} will get a new ID. <strong>Note: this method mutates the
   * widgets.</strong>
   *
   * @param widgets never {@code null}.
   */
  void generateIds(PSRegionWidgetAssociations widgets);

  /**
   * Sets IDs to null for widgets that have an ID.
   *
   * @param widgets never {@code null}.
   */
  void deleteIds(PSRegionWidgetAssociations widgets);
}
