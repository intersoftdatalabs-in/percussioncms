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
package com.percussion.search.ui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.*;

/**
 * Model that may be used for a combo box with the data representing all existing display formats.
 */
public class ApplicationDataComboModel extends DefaultComboBoxModel<String> {
  private static final long serialVersionUID = 1L;

  /** No-op default constructor. */
  public ApplicationDataComboModel() {
    super();
  }

  /**
   * Static call to populate the model with data and return the model
   *
   * @param map Map of id and names. If <code>null</code> an empty map will be created.
   * @return ApplicationDataComboModel object.
   */
  public static ApplicationDataComboModel createApplicationDataComboModel(Map<String, String> map) {
    if (map == null) map = new HashMap<>();
    try {
      return new ApplicationDataComboModel(map);
    } catch (Exception e) {
      Map<String, String> m = new HashMap<>();
      m.put(e.getLocalizedMessage(), e.getClass().getName());
      return new ApplicationDataComboModel(m);
    }
  }

  /**
   * Creates an ApplicationDataComboModel object.
   *
   * @param map of ids and display names. Assumed not <code>null</code>.
   */
  private ApplicationDataComboModel(Map<String, String> map) {
    super(sort(map.values()));
    m_map = map;
  }

  /**
   * Access method to get the selected id from the combo box.
   *
   * @return String the id of the selected item. Returns <code>null
   *    </code> if the nothing is selected.
   */
  public String getSelectedId() {
    String strVal = (String) super.getSelectedItem();

    Iterator<String> keys = m_map.keySet().iterator();
    while (keys.hasNext()) {
      String strId = keys.next();

      if (m_map.get(strId).equalsIgnoreCase(strVal)) return strId;
    }

    return null;
  }

  /**
   * Sets the display name of the supplied id as selected item in the combo box.
   *
   * @param strId the id of the item that need to be set. If <code>null</code> or empty then nothing
   *     is set.
   */
  public void setSelectedId(String strId) {
    if (strId == null || strId.trim().length() < 1) return;
    String strVal = m_map.get(strId);

    if (strVal == null) {
      Iterator<String> iter = m_map.values().iterator();

      if (iter.hasNext()) strVal = iter.next();
    }

    super.setSelectedItem(strVal);
  }

  /**
   * Re-orders the supplied collection into dictionary order.
   *
   * @param values May be <code>null</code>.
   * @return A sorted array, or <code>null</code> if {@code values} is <code>null</code>.
   */
  private static String[] sort(java.util.Collection<String> values) {
    if (values == null) return null;

    String[] arr = values.toArray(new String[0]);
    Arrays.sort(arr, String::compareToIgnoreCase);
    return arr;
  }

  /** Initialized in ctor. Never <code>null</code>, may be empty. */
  private transient Map<String, String> m_map = null;
}
