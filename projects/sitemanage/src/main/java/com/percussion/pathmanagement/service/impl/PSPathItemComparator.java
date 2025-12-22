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
package com.percussion.pathmanagement.service.impl;

import com.percussion.pathmanagement.data.PSPathItem;
import java.util.Comparator;

/**
 * Comparator for {@link PSPathItem} that sorts by name, case-insensitive, ascending. Thread-safe
 * singleton.
 */
public final class PSPathItemComparator implements Comparator<PSPathItem> {

  private static final PSPathItemComparator INSTANCE = new PSPathItemComparator();

  private PSPathItemComparator() {
    // Singleton: prevent instantiation
  }

  /** Returns the singleton instance of this comparator. */
  public static Comparator<PSPathItem> getInstance() {
    return INSTANCE;
  }

  @Override
  public int compare(PSPathItem a, PSPathItem b) {
    // Null-safe comparison, but names should not be null in practice
    var nameA = a != null && a.getName() != null ? a.getName() : "";
    var nameB = b != null && b.getName() != null ? b.getName() : "";
    return nameA.compareToIgnoreCase(nameB);
  }
}
