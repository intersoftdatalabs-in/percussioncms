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

import com.percussion.services.virtualsite.VirtualSiteConfig.HttpSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.NavSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.ObjectsSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.IcalendarSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.LlmsSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.RobotsSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.RssSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.SitemapSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.SqlSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.VersionSpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Loads {@code _config.yaml} from a Virtual Site root (optional fallback for CSV trees).
 *
 * <p>Stateless: each {@link #load} / {@link #loadOrDefault} reads the current file (or current
 * child directories). No process-lifetime YAML cache — a second build after a config edit sees
 * the new title/versions (and current {@code sql:} / {@code http:} / {@code objects:} /
 * {@code rss:} / {@code icalendar:} / {@code sitemap:} / {@code robots:} / {@code llms:} mapping)
 * without a JVM restart.
 */
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

  /**
   * Load {@code _config.yaml} when present; otherwise synthesize a CSV-friendly default from
   * immediate child version directories (no Markdown tree required).
   *
   * <p>Missing config is allowed only for this fallback. A present but invalid YAML still fails
   * closed. Child folders named {@code assets}, or whose names start with {@code _} or {@code .},
   * are skipped. Empty trees (no version folder) fail closed.
   *
   * @param root Virtual Site root directory
   * @param configFileName config file name, may be null for default
   * @param siteKey participant/site key
   * @return config
   */
  public static VirtualSiteConfig loadOrDefault(Path root, String configFileName, String siteKey)
      throws IOException, VirtualSiteException {
    if (root == null) {
      throw new VirtualSiteException("Virtual Site root is null");
    }
    String name =
        configFileName == null || configFileName.isBlank() ? DEFAULT_CONFIG_FILE : configFileName;
    Path configPath = root.resolve(name);
    if (Files.isRegularFile(configPath)) {
      return load(root, name, siteKey);
    }
    return defaultFromVersionDirectories(root, siteKey);
  }

  /**
   * Infer versions from immediate child directories of {@code root} when {@code _config.yaml} is
   * omitted (CSV trees).
   *
   * @param root site root, not null
   * @param siteKey participant key
   * @return config with at least one version
   */
  static VirtualSiteConfig defaultFromVersionDirectories(Path root, String siteKey)
      throws IOException, VirtualSiteException {
    if (!Files.isDirectory(root)) {
      throw new VirtualSiteException("CSV site root is not a directory: " + root);
    }
    if (!PSVirtualSiteHelper.isSafeRootPath(root)) {
      throw new VirtualSiteException("CSV site root is missing or unsafe");
    }
    Path safeRoot = root.normalize();
    List<String> folders = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(safeRoot)) {
      for (Path child : stream) {
        if (!Files.isDirectory(child)) {
          continue;
        }
        Path normalized = child.normalize();
        if (!normalized.startsWith(safeRoot)) {
          throw new VirtualSiteException("Version path escapes site root: " + child);
        }
        String folder = child.getFileName().toString();
        if (folder.startsWith("_")
            || folder.startsWith(".")
            || "assets".equalsIgnoreCase(folder)) {
          continue;
        }
        folders.add(folder);
      }
    }
    folders.sort(Comparator.comparing(s -> s.toLowerCase(Locale.ROOT)));
    if (folders.isEmpty()) {
      throw new VirtualSiteException(
          "CSV source has no _config.yaml and no version folder (add _config.yaml or a version"
              + " directory such as 8.2)");
    }
    boolean prefer82 = folders.contains("8.2");
    List<VersionSpec> versions = new ArrayList<>();
    boolean first = true;
    for (String folder : folders) {
      boolean def = prefer82 ? "8.2".equals(folder) : first;
      versions.add(new VersionSpec(folder, folder, folder, def));
      first = false;
    }
    String title = siteKey != null && !siteKey.isBlank() ? siteKey : "CSV Site";
    return new VirtualSiteConfig(safeRoot, title, "", "page.html", versions, List.of(), siteKey);
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

      Object sqlObj = map.get("sql");
      if (sqlObj != null && !(sqlObj instanceof Map<?, ?>)) {
        throw new VirtualSiteException("sql: must be a mapping in " + sourceLabel);
      }
      SqlSpec sql = parseSqlSpec(asMap(sqlObj));
      Object httpObj = map.get("http");
      if (httpObj != null && !(httpObj instanceof Map<?, ?>)) {
        throw new VirtualSiteException("http: must be a mapping in " + sourceLabel);
      }
      HttpSpec http = parseHttpSpec(asMap(httpObj));
      Object objectsObj = map.get("objects");
      if (objectsObj != null && !(objectsObj instanceof Map<?, ?>)) {
        throw new VirtualSiteException("objects: must be a mapping in " + sourceLabel);
      }
      ObjectsSpec objects = parseObjectsSpec(asMap(objectsObj), sourceLabel);
      Object rssObj = map.get("rss");
      if (rssObj != null && !(rssObj instanceof Map<?, ?>)) {
        throw new VirtualSiteException("rss: must be a mapping in " + sourceLabel);
      }
      RssSpec rss = parseRssSpec(asMap(rssObj));
      Object icalendarObj = map.get("icalendar");
      if (icalendarObj != null && !(icalendarObj instanceof Map<?, ?>)) {
        throw new VirtualSiteException("icalendar: must be a mapping in " + sourceLabel);
      }
      IcalendarSpec icalendar = parseIcalendarSpec(asMap(icalendarObj));
      Object sitemapObj = map.get("sitemap");
      if (sitemapObj != null && !(sitemapObj instanceof Map<?, ?>)) {
        throw new VirtualSiteException("sitemap: must be a mapping in " + sourceLabel);
      }
      SitemapSpec sitemap = parseSitemapSpec(asMap(sitemapObj));
      Object robotsObj = map.get("robots");
      if (robotsObj != null && !(robotsObj instanceof Map<?, ?>)) {
        throw new VirtualSiteException("robots: must be a mapping in " + sourceLabel);
      }
      RobotsSpec robots = parseRobotsSpec(asMap(robotsObj));
      Object llmsObj = map.get("llms");
      if (llmsObj != null && !(llmsObj instanceof Map<?, ?>)) {
        throw new VirtualSiteException("llms: must be a mapping in " + sourceLabel);
      }
      LlmsSpec llms = parseLlmsSpec(asMap(llmsObj));
      return new VirtualSiteConfig(
          root,
          title,
          url,
          layout,
          versions,
          nav,
          siteKey,
          sql,
          http,
          objects,
          rss,
          icalendar,
          sitemap,
          robots,
          llms);
    } catch (VirtualSiteException e) {
      throw e;
    } catch (Exception e) {
      throw new VirtualSiteException("Failed to parse config: " + sourceLabel, e);
    }
  }

  private static HttpSpec parseHttpSpec(Map<String, Object> http) {
    if (http == null || http.isEmpty()) {
      return null;
    }
    return new HttpSpec(stringVal(http.get("url")), stringVal(http.get("file")));
  }

  private static RssSpec parseRssSpec(Map<String, Object> rss) {
    if (rss == null || rss.isEmpty()) {
      return null;
    }
    return new RssSpec(stringVal(rss.get("url")), stringVal(rss.get("file")));
  }

  private static IcalendarSpec parseIcalendarSpec(Map<String, Object> icalendar) {
    if (icalendar == null || icalendar.isEmpty()) {
      return null;
    }
    return new IcalendarSpec(stringVal(icalendar.get("url")), stringVal(icalendar.get("file")));
  }

  private static SitemapSpec parseSitemapSpec(Map<String, Object> sitemap) {
    if (sitemap == null || sitemap.isEmpty()) {
      return null;
    }
    return new SitemapSpec(stringVal(sitemap.get("url")), stringVal(sitemap.get("file")));
  }

  private static RobotsSpec parseRobotsSpec(Map<String, Object> robots) {
    if (robots == null || robots.isEmpty()) {
      return null;
    }
    return new RobotsSpec(stringVal(robots.get("url")), stringVal(robots.get("file")));
  }

  private static LlmsSpec parseLlmsSpec(Map<String, Object> llms) {
    if (llms == null || llms.isEmpty()) {
      return null;
    }
    return new LlmsSpec(stringVal(llms.get("url")), stringVal(llms.get("file")));
  }

  private static ObjectsSpec parseObjectsSpec(Map<String, Object> objects, String sourceLabel)
      throws VirtualSiteException {
    if (objects == null || objects.isEmpty()) {
      return null;
    }
    Object keysObj = objects.get("keys");
    if (keysObj == null) {
      return new ObjectsSpec(List.of());
    }
    if (!(keysObj instanceof List<?> list)) {
      throw new VirtualSiteException("objects.keys must be a list in " + sourceLabel);
    }
    List<String> keys = new ArrayList<>();
    for (Object item : list) {
      if (item == null) {
        continue;
      }
      String key = String.valueOf(item).trim();
      if (!key.isBlank()) {
        keys.add(key);
      }
    }
    return new ObjectsSpec(keys);
  }

  private static SqlSpec parseSqlSpec(Map<String, Object> sql) {
    if (sql == null || sql.isEmpty()) {
      return null;
    }
    Map<String, Object> columns = asMap(sql.get("columns"));
    return new SqlSpec(
        stringVal(sql.get("jdbcUrl")),
        stringVal(sql.get("user")),
        stringVal(sql.get("password")),
        stringVal(sql.get("query")),
        stringVal(sql.get("queryFile")),
        stringVal(columns.get("id")),
        stringVal(columns.get("title")),
        stringVal(columns.get("body")),
        stringVal(columns.get("path")),
        stringVal(columns.get("order")),
        stringVal(columns.get("version")));
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
