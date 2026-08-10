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

import com.percussion.services.virtualsite.VirtualSiteConfig.NavSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.VersionSpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Loads {@code _config.yaml} from a Virtual Site root. */
public final class VirtualSiteConfigLoader {

  public static final String DEFAULT_CONFIG_FILE = "_config.yaml";

  private VirtualSiteConfigLoader() {}

  /**
   * Load config from {@code root/_config.yaml} (or custom name).
   *
   * @param root Virtual Site root directory
   * @param configFileName config file name, may be null for default
   * @param siteKey participant/site key
   * @return config
   * @throws IOException if file missing/unreadable
   * @throws VirtualSiteException if YAML invalid
   */
  public static VirtualSiteConfig load(Path root, String configFileName, String siteKey)
      throws IOException, VirtualSiteException {
    if (root == null) {
      throw new VirtualSiteException("Virtual Site root is null");
    }
    String name =
        configFileName == null || configFileName.isBlank() ? DEFAULT_CONFIG_FILE : configFileName;
    Path configPath = root.resolve(name);
    if (!Files.isRegularFile(configPath)) {
      throw new VirtualSiteException("Config file not found: " + configPath);
    }
    try (InputStream in = Files.newInputStream(configPath)) {
      return parse(root, in, siteKey, configPath.toString());
    }
  }

  @SuppressWarnings("unchecked")
  static VirtualSiteConfig parse(Path root, InputStream in, String siteKey, String sourceLabel)
      throws VirtualSiteException {
    try {
      Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
      Object loaded = yaml.load(in);
      if (!(loaded instanceof Map)) {
        throw new VirtualSiteException("Config root must be a YAML mapping: " + sourceLabel);
      }
      Map<String, Object> map = (Map<String, Object>) loaded;

      Map<String, Object> site = asMap(map.get("site"));
      String title = stringVal(site.get("title"));
      String url = stringVal(site.get("url"));

      Map<String, Object> theme = asMap(map.get("theme"));
      String layout = stringVal(theme.get("layout"));

      List<VersionSpec> versions = new ArrayList<>();
      Object versionsObj = map.get("versions");
      if (versionsObj instanceof List<?> list) {
        for (Object item : list) {
          if (item instanceof Map<?, ?> m) {
            Map<String, Object> vm = (Map<String, Object>) m;
            String id = stringVal(vm.get("id"));
            if (id == null || id.isBlank()) {
              continue;
            }
            String label = stringVal(vm.get("label"));
            String path = stringVal(vm.get("path"));
            boolean def = booleanVal(vm.get("default"), false);
            versions.add(new VersionSpec(id, label, path, def));
          }
        }
      }
      if (versions.isEmpty()) {
        throw new VirtualSiteException("Config must declare at least one version: " + sourceLabel);
      }

      List<NavSpec> nav = new ArrayList<>();
      Object navObj = map.get("nav");
      if (navObj instanceof List<?> list) {
        for (Object item : list) {
          if (item instanceof Map<?, ?> m) {
            Map<String, Object> nm = (Map<String, Object>) m;
            String id = stringVal(nm.get("id"));
            if (id == null || id.isBlank()) {
              continue;
            }
            String ntitle = stringVal(nm.get("title"));
            nav.add(new NavSpec(ntitle, id));
          }
        }
      }

      return new VirtualSiteConfig(root, title, url, layout, versions, nav, siteKey);
    } catch (VirtualSiteException e) {
      throw e;
    } catch (Exception e) {
      throw new VirtualSiteException("Failed to parse config: " + sourceLabel, e);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object o) {
    if (o instanceof Map) {
      return (Map<String, Object>) o;
    }
    return Map.of();
  }

  private static String stringVal(Object o) {
    return o == null ? null : String.valueOf(o).trim();
  }

  private static boolean booleanVal(Object o, boolean defaultValue) {
    if (o == null) {
      return defaultValue;
    }
    if (o instanceof Boolean b) {
      return b;
    }
    return Boolean.parseBoolean(String.valueOf(o));
  }
}
