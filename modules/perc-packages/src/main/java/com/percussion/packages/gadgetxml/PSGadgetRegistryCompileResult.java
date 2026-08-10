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

import com.percussion.packages.manifest.PSComponentPackageManifest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Output of compiling a gadget registry: aggregate modern catalog + per-gadget component package
 * manifests keyed by gadget id.
 */
public final class PSGadgetRegistryCompileResult {

  private final PSGadgetRegistryModel source;
  private final PSGadgetCatalog catalog;
  /** gadget id → validated component package manifest ({@code catalog.kind = "gadget"}). */
  private final Map<String, PSComponentPackageManifest> gadgetPackages;

  public PSGadgetRegistryCompileResult(
      PSGadgetRegistryModel source,
      PSGadgetCatalog catalog,
      Map<String, PSComponentPackageManifest> gadgetPackages) {
    this.source = Objects.requireNonNull(source, "source");
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.gadgetPackages =
        gadgetPackages == null
            ? Map.of()
            : Collections.unmodifiableMap(new LinkedHashMap<>(gadgetPackages));
  }

  public PSGadgetRegistryModel getSource() {
    return source;
  }

  public PSGadgetCatalog getCatalog() {
    return catalog;
  }

  public Map<String, PSComponentPackageManifest> getGadgetPackages() {
    return gadgetPackages;
  }

  /** Convenience: package for a single gadget id, or null. */
  public PSComponentPackageManifest getGadgetPackage(String gadgetId) {
    return gadgetPackages.get(gadgetId);
  }
}
