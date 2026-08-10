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
import java.util.List;
import java.util.Objects;

/**
 * Modern <strong>gadget catalog</strong> ship format (JSON) — product replacement for {@code
 * GadgetRegistry.xml} grouping / listing (ADR-004 / issue #2771).
 *
 * <p>Default file name: {@value #DEFAULT_CATALOG_FILE_NAME}. Per-gadget install/authoring still uses
 * {@code component-package.json} with {@code catalog.kind = "gadget"}; this aggregate catalog is the
 * registry equivalent consumed by packaging tools and (eventually) SPA Home/Dashboard loaders.
 */
public final class PSGadgetCatalog {

  /** Default ship-format file name for the aggregate gadget catalog. */
  public static final String DEFAULT_CATALOG_FILE_NAME = "gadget-catalog.json";

  /** Schema version supported by this model (semver major.minor). */
  public static final String SUPPORTED_SCHEMA_VERSION = "1.0";

  private String schemaVersion = SUPPORTED_SCHEMA_VERSION;
  private String id = "perc.gadgetCatalog";
  private String name = "Product Gadget Catalog";
  private String version = "0.0.0";
  private String description;
  private List<Entry> gadgets = new ArrayList<>();

  public String getSchemaVersion() {
    return schemaVersion;
  }

  public void setSchemaVersion(String schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Entry> getGadgets() {
    return gadgets;
  }

  public void setGadgets(List<Entry> gadgets) {
    this.gadgets = gadgets != null ? gadgets : new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PSGadgetCatalog that)) {
      return false;
    }
    return Objects.equals(schemaVersion, that.schemaVersion)
        && Objects.equals(id, that.id)
        && Objects.equals(name, that.name)
        && Objects.equals(version, that.version)
        && Objects.equals(description, that.description)
        && Objects.equals(gadgets, that.gadgets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(schemaVersion, id, name, version, description, gadgets);
  }

  /** One catalog row (modern equivalent of a registry {@code <gadget/>}). */
  public static final class Entry {
    private String id;
    private String name;
    private String group;
    private String baseUri;
    /** Legacy OpenSocial definition file name (upgrade-input only; product no longer ships XML). */
    private String legacyDefinitionFile;
    private boolean deprecated;
    /** Package-relative path to this gadget's {@code component-package.json} when packaged alone. */
    private String componentPackageRef;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getGroup() {
      return group;
    }

    public void setGroup(String group) {
      this.group = group;
    }

    public String getBaseUri() {
      return baseUri;
    }

    public void setBaseUri(String baseUri) {
      this.baseUri = baseUri;
    }

    public String getLegacyDefinitionFile() {
      return legacyDefinitionFile;
    }

    public void setLegacyDefinitionFile(String legacyDefinitionFile) {
      this.legacyDefinitionFile = legacyDefinitionFile;
    }

    public boolean isDeprecated() {
      return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
      this.deprecated = deprecated;
    }

    public String getComponentPackageRef() {
      return componentPackageRef;
    }

    public void setComponentPackageRef(String componentPackageRef) {
      this.componentPackageRef = componentPackageRef;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Entry that)) {
        return false;
      }
      return deprecated == that.deprecated
          && Objects.equals(id, that.id)
          && Objects.equals(name, that.name)
          && Objects.equals(group, that.group)
          && Objects.equals(baseUri, that.baseUri)
          && Objects.equals(legacyDefinitionFile, that.legacyDefinitionFile)
          && Objects.equals(componentPackageRef, that.componentPackageRef);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          id, name, group, baseUri, legacyDefinitionFile, deprecated, componentPackageRef);
    }
  }
}
