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
package com.percussion.services.virtualsite;

import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.data.PSSite;
import com.percussion.services.sitemgr.data.PSSiteProperty;
import com.percussion.utils.guid.IPSGuid;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 * Site property contract for Virtual Sites (Phase 1 — no new {@code RXSITES} columns).
 *
 * <p>Property keys:
 *
 * <ul>
 *   <li>{@code virtual.sourceKind} — allow-listed adapter wire name (e.g. {@code git-filesystem});
 *       blank or {@code repository} ⇒ traditional repository Site
 *   <li>{@code virtual.rootPath} — filesystem path to Virtual Site root (required when virtual)
 *   <li>{@code virtual.configFile} — optional; default {@code _config.yaml}; simple file name only
 *   <li>{@code virtual.siteKey} — optional participant key; default site name
 * </ul>
 *
 * <p>Use {@link #validate(IPSSite)} before treating a Site as a safe Virtual Site source. Path
 * handling uses {@link Path} (NIO) for cross-platform Windows / Linux / macOS behavior.
 */
public final class PSVirtualSiteHelper {

  public static final String PROP_SOURCE_KIND = "virtual.sourceKind";
  public static final String PROP_ROOT_PATH = "virtual.rootPath";
  public static final String PROP_CONFIG_FILE = "virtual.configFile";
  public static final String PROP_SITE_KEY = "virtual.siteKey";

  /** Wire name for traditional repository-backed Sites. */
  public static final String SOURCE_KIND_REPOSITORY = "repository";

  private PSVirtualSiteHelper() {}

  /**
   * Allow-listed {@link #PROP_SOURCE_KIND} wire names for Virtual adapters (Phase 1:
   * {@code git-filesystem} only). Does not include {@link #SOURCE_KIND_REPOSITORY}.
   *
   * @return unmodifiable list of wire names in enum declaration order
   */
  public static List<String> allowedSourceKindWireNames() {
    List<String> names = new ArrayList<>();
    for (VirtualSiteSourceType t : VirtualSiteSourceType.values()) {
      names.add(t.wireName());
    }
    return Collections.unmodifiableList(names);
  }

  public static SourceKind sourceKind(IPSSite site) {
    String kind = findProperty(site, PROP_SOURCE_KIND).orElse("");
    if (StringUtils.isBlank(kind) || SOURCE_KIND_REPOSITORY.equalsIgnoreCase(kind.trim())) {
      return SourceKind.REPOSITORY;
    }
    return SourceKind.VIRTUAL;
  }

  public static boolean isVirtual(IPSSite site) {
    return sourceKind(site) == SourceKind.VIRTUAL;
  }

  public static Optional<VirtualSiteSourceType> virtualSourceType(IPSSite site) {
    return findProperty(site, PROP_SOURCE_KIND).map(VirtualSiteSourceType::fromWireName);
  }

  /**
   * Resolved Virtual Site root as a normalized NIO path when the property is non-blank.
   *
   * <p>Does not validate safety; call {@link #validate(IPSSite)} for contract checks.
   *
   * @param site may be null
   * @return normalized path if property present and non-blank
   */
  public static Optional<Path> rootPath(IPSSite site) {
    return findProperty(site, PROP_ROOT_PATH)
        .filter(StringUtils::isNotBlank)
        .map(raw -> Path.of(raw).normalize());
  }

  public static String configFile(IPSSite site) {
    return findProperty(site, PROP_CONFIG_FILE)
        .filter(StringUtils::isNotBlank)
        .orElse(VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE);
  }

  public static String siteKey(IPSSite site) {
    Optional<String> key = findProperty(site, PROP_SITE_KEY).filter(StringUtils::isNotBlank);
    if (key.isPresent()) {
      return key.get();
    }
    if (site != null && StringUtils.isNotBlank(site.getName())) {
      return site.getName();
    }
    return "default";
  }

  /**
   * Validates the Virtual Site property contract.
   *
   * <p>Traditional repository Sites (missing/blank {@code virtual.sourceKind} or value {@code
   * repository}) always pass. Virtual Sites must:
   *
   * <ul>
   *   <li>use an allow-listed {@code virtual.sourceKind} (see {@link #allowedSourceKindWireNames()})
   *   <li>provide a non-blank {@code virtual.rootPath}
   *   <li>use a safe root path after NIO {@link Path#normalize()} (no empty path; no remaining {@code
   *       ..} segments)
   *   <li>when set, use a simple {@code virtual.configFile} name (no directory separators or {@code
   *       ..})
   * </ul>
   *
   * @param site may be null (treated as a valid repository Site)
   * @throws VirtualSiteException when the virtual property contract is violated
   */
  public static void validate(IPSSite site) throws VirtualSiteException {
    String kindRaw = findProperty(site, PROP_SOURCE_KIND).orElse("");
    if (StringUtils.isBlank(kindRaw)
        || SOURCE_KIND_REPOSITORY.equalsIgnoreCase(kindRaw.trim())) {
      return;
    }

    String kind = kindRaw.trim();
    VirtualSiteSourceType type = VirtualSiteSourceType.fromWireName(kind);
    if (type == null) {
      throw new VirtualSiteException(
          "Unsupported "
              + PROP_SOURCE_KIND
              + " value '"
              + kind
              + "'. Allowed: "
              + allowedSourceKindsDescription()
              + " (or blank/"
              + SOURCE_KIND_REPOSITORY
              + " for traditional Sites).");
    }

    Optional<String> rootRaw = findProperty(site, PROP_ROOT_PATH);
    if (rootRaw.isEmpty()) {
      throw new VirtualSiteException(
          PROP_ROOT_PATH + " is required when " + PROP_SOURCE_KIND + " is '" + kind + "'.");
    }

    Path root;
    try {
      root = Path.of(rootRaw.get()).normalize();
    } catch (InvalidPathException e) {
      throw new VirtualSiteException(
          PROP_ROOT_PATH + " is not a valid filesystem path: '" + rootRaw.get() + "'.", e);
    }

    if (!isSafeRootPath(root)) {
      throw new VirtualSiteException(
          PROP_ROOT_PATH
              + " must be a non-empty path with no '..' segments after normalize (cross-platform NIO"
              + " Path). Rejected: '"
              + rootRaw.get()
              + "'.");
    }

    Optional<String> configRaw = findProperty(site, PROP_CONFIG_FILE);
    if (configRaw.isPresent()) {
      validateConfigFileName(configRaw.get());
    }
  }

  /**
   * Whether {@code path} is acceptable as a Virtual Site root after normalize.
   *
   * <ul>
   *   <li>Rejects empty / relative-current-only paths ({@code ""}, {@code .})
   *   <li>Rejects any remaining {@code ..} name element (path traversal after normalize)
   *   <li>Allows absolute roots ({@code /}, {@code C:\} on Windows) and normal absolute/relative
   *       trees
   * </ul>
   *
   * @param path may be null
   * @return true if safe for use as virtual root
   */
  public static boolean isSafeRootPath(Path path) {
    if (path == null) {
      return false;
    }
    Path normalized = path.normalize();
    // Path.of("") / Path.of(".").normalize() yield an empty relative path ΓÇö not a usable root.
    // Absolute filesystem roots ("/", "C:\") are non-empty as strings and remain absolute.
    String text = normalized.toString();
    if (text.isEmpty() || ".".equals(text)) {
      return false;
    }
    for (Path part : normalized) {
      String name = part.toString();
      if (name.isEmpty() || "..".equals(name)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Find first property value by name across all contexts.
   *
   * <p>{@link IPSSite} does not expose properties; the concrete {@link PSSite} entity does.
   *
   * @param site may be null
   * @param name property name
   * @return value if present
   */
  public static Optional<String> findProperty(IPSSite site, String name) {
    Objects.requireNonNull(name, "name");
    if (site == null) {
      return Optional.empty();
    }
    Set<PSSiteProperty> props = propertiesOf(site);
    if (props.isEmpty()) {
      return Optional.empty();
    }
    for (PSSiteProperty p : props) {
      if (p != null && name.equals(p.getName()) && StringUtils.isNotBlank(p.getValue())) {
        return Optional.of(p.getValue().trim());
      }
    }
    return Optional.empty();
  }

  /**
   * Context id of the first property matching {@code name}, if any.
   *
   * @param site may be null
   * @param name property name
   * @return context guid when the property exists
   */
  public static Optional<IPSGuid> findPropertyContext(IPSSite site, String name) {
    Objects.requireNonNull(name, "name");
    if (!(site instanceof PSSite ps)) {
      return Optional.empty();
    }
    Set<PSSiteProperty> props = ps.getProperties();
    if (props == null || props.isEmpty()) {
      return Optional.empty();
    }
    for (PSSiteProperty p : props) {
      if (p != null && name.equals(p.getName()) && p.getContextId() != null) {
        return Optional.of(p.getContextId());
      }
    }
    return Optional.empty();
  }

  /**
   * Set or clear a named property on a concrete {@link PSSite}.
   *
   * <p>Blank {@code value} removes the first property with that name (any context). Non-blank
   * values update an existing property in place when present; otherwise a new {@link
   * PSSiteProperty} is added for {@code contextId}. Does not call {@link
   * PSSite#setProperty(String, IPSGuid, String)} (avoids GuidManager for unit-testability).
   *
   * @param site concrete site entity, not null
   * @param contextId publishing context used when creating a new property, not null when creating
   * @param name property name, not blank
   * @param value value to store; blank clears
   */
  public static void putProperty(PSSite site, IPSGuid contextId, String name, String value) {
    Objects.requireNonNull(site, "site");
    Objects.requireNonNull(name, "name");
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name may not be blank");
    }
    if (StringUtils.isBlank(value)) {
      // getProperties() returns emptySet when field is null (unmodifiable) — only mutate live set
      Set<PSSiteProperty> existing = site.getProperties();
      if (!existing.isEmpty()) {
        existing.removeIf(p -> p != null && name.equals(p.getName()));
      }
      return;
    }
    Set<PSSiteProperty> props = site.getProperties();
    if (!props.isEmpty()) {
      for (PSSiteProperty p : props) {
        if (p != null && name.equals(p.getName())) {
          p.setValue(value.trim());
          return;
        }
      }
    }
    Objects.requireNonNull(contextId, "contextId");
    PSSiteProperty prop = new PSSiteProperty();
    // Stable-enough id without GuidManager (persistence layer reassigns if needed on save)
    prop.setPropertyId(
        Math.floorMod((long) name.hashCode() * 31L + contextId.longValue(), Integer.MAX_VALUE - 1L)
            + 1L);
    prop.setContextId(contextId);
    prop.setName(name);
    prop.setValue(value.trim());
    prop.setSite(site);
    site.addProperty(prop);
  }

  private static void validateConfigFileName(String configFile) throws VirtualSiteException {
    String name = configFile.trim();
    if (name.isEmpty()) {
      throw new VirtualSiteException(PROP_CONFIG_FILE + " must not be blank when set.");
    }
    // Config is resolved under rootPath; reject directory traversal and separators.
    if (name.contains("..")
        || name.indexOf('/') >= 0
        || name.indexOf('\\') >= 0) {
      throw new VirtualSiteException(
          PROP_CONFIG_FILE
              + " must be a simple file name under the Virtual Site root (no path separators or"
              + " '..'). Rejected: '"
              + configFile
              + "'.");
    }
  }

  private static String allowedSourceKindsDescription() {
    return Stream.of(VirtualSiteSourceType.values())
        .map(VirtualSiteSourceType::wireName)
        .collect(Collectors.joining(", "));
  }

  private static Set<PSSiteProperty> propertiesOf(IPSSite site) {
    if (site instanceof PSSite ps) {
      Set<PSSiteProperty> props = ps.getProperties();
      return props != null ? props : Collections.emptySet();
    }
    return Collections.emptySet();
  }
}
