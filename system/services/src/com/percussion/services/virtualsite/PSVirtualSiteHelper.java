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
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Site property contract for Virtual Sites (Phase 1 — no new {@code RXSITES} columns).
 *
 * <p>Property keys:
 *
 * <ul>
 *   <li>{@code virtual.sourceKind} — e.g. {@code git-filesystem}; blank ⇒ repository
 *   <li>{@code virtual.rootPath} — filesystem path to Virtual Site root
 *   <li>{@code virtual.configFile} — optional; default {@code _config.yaml}
 *   <li>{@code virtual.siteKey} — optional participant key; default site name
 * </ul>
 */
public final class PSVirtualSiteHelper {

  public static final String PROP_SOURCE_KIND = "virtual.sourceKind";
  public static final String PROP_ROOT_PATH = "virtual.rootPath";
  public static final String PROP_CONFIG_FILE = "virtual.configFile";
  public static final String PROP_SITE_KEY = "virtual.siteKey";

  private PSVirtualSiteHelper() {}

  public static SourceKind sourceKind(IPSSite site) {
    String kind = findProperty(site, PROP_SOURCE_KIND).orElse("");
    if (StringUtils.isBlank(kind) || "repository".equalsIgnoreCase(kind.trim())) {
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

  public static Optional<Path> rootPath(IPSSite site) {
    return findProperty(site, PROP_ROOT_PATH).filter(StringUtils::isNotBlank).map(Path::of);
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

  private static Set<PSSiteProperty> propertiesOf(IPSSite site) {
    if (site instanceof PSSite ps) {
      Set<PSSiteProperty> props = ps.getProperties();
      return props != null ? props : Collections.emptySet();
    }
    return Collections.emptySet();
  }
}
