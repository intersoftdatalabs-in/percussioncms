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

package com.percussion.packages.gadgetxml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed product gadget registry (upgrade-input {@code GadgetRegistry.xml}).
 *
 * <p>Order of entries matches document order within each group; groups themselves are document
 * order. Used only as compiler input — modern ship format is {@link PSGadgetCatalog} + per-gadget
 * {@code component-package.json}.
 */
public final class PSGadgetRegistryModel {

  private String sourceFileName;
  private final List<PSGadgetRegistryEntry> gadgets = new ArrayList<>();

  public String getSourceFileName() {
    return sourceFileName;
  }

  public void setSourceFileName(String sourceFileName) {
    this.sourceFileName = sourceFileName;
  }

  public List<PSGadgetRegistryEntry> getGadgets() {
    return gadgets;
  }

  public void addGadget(PSGadgetRegistryEntry entry) {
    if (entry != null) {
      gadgets.add(entry);
    }
  }

  /** Display name → group (first wins). */
  public Map<String, String> toNameGroupMap() {
    Map<String, String> map = new LinkedHashMap<>();
    for (PSGadgetRegistryEntry g : gadgets) {
      if (g == null || g.getName() == null || g.getName().isBlank()) {
        continue;
      }
      map.putIfAbsent(g.getName(), g.getGroup() != null ? g.getGroup() : "Custom");
    }
    return Collections.unmodifiableMap(map);
  }

  public PSGadgetRegistryEntry findById(String gadgetId) {
    if (gadgetId == null || gadgetId.isBlank()) {
      return null;
    }
    for (PSGadgetRegistryEntry g : gadgets) {
      if (g != null && gadgetId.equals(g.gadgetId())) {
        return g;
      }
    }
    return null;
  }

  public PSGadgetRegistryEntry findByName(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    for (PSGadgetRegistryEntry g : gadgets) {
      if (g != null && name.equals(g.getName())) {
        return g;
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PSGadgetRegistryModel that)) {
      return false;
    }
    return Objects.equals(sourceFileName, that.sourceFileName)
        && Objects.equals(gadgets, that.gadgets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceFileName, gadgets);
  }
}
