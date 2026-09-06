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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Runtime configuration for a Virtual Site build: filesystem root plus parsed {@code _config.yaml}.
 */
public final class VirtualSiteConfig {

  private final Path root;
  private final String siteTitle;
  private final String siteUrl;
  private final String layoutFile;
  private final List<VersionSpec> versions;
  private final List<NavSpec> nav;
  private final String siteKey;
  private final SqlSpec sql;
  private final HttpSpec http;
  private final ObjectsSpec objects;
  private final RssSpec rss;
  private final IcalendarSpec icalendar;
  private final SitemapSpec sitemap;
  private final RobotsSpec robots;
  private final LlmsSpec llms;

  public VirtualSiteConfig(
      Path root,
      String siteTitle,
      String siteUrl,
      String layoutFile,
      List<VersionSpec> versions,
      List<NavSpec> nav,
      String siteKey) {
    this(root, siteTitle, siteUrl, layoutFile, versions, nav, siteKey, null, null);
  }

  public VirtualSiteConfig(
      Path root,
      String siteTitle,
      String siteUrl,
      String layoutFile,
      List<VersionSpec> versions,
      List<NavSpec> nav,
      String siteKey,
      SqlSpec sql) {
    this(root, siteTitle, siteUrl, layoutFile, versions, nav, siteKey, sql, null);
  }

  public VirtualSiteConfig(
      Path root,
      String siteTitle,
      String siteUrl,
      String layoutFile,
      List<VersionSpec> versions,
      List<NavSpec> nav,
      String siteKey,
      SqlSpec sql,
      HttpSpec http) {
    this(root, siteTitle, siteUrl, layoutFile, versions, nav, siteKey, sql, http, null);
  }

  public VirtualSiteConfig(
      Path root,
      String siteTitle,
      String siteUrl,
      String layoutFile,
      List<VersionSpec> versions,
      List<NavSpec> nav,
      String siteKey,
      SqlSpec sql,
      HttpSpec http,
      ObjectsSpec objects) {
    this(root, siteTitle, siteUrl, layoutFile, versions, nav, siteKey, sql, http, objects, null);
  }

  public VirtualSiteConfig(
      Path root,
      String siteTitle,
      String siteUrl,
      String layoutFile,
      List<VersionSpec> versions,
      List<NavSpec> nav,
      String siteKey,
      SqlSpec sql,
      HttpSpec http,
      ObjectsSpec objects,
      RssSpec rss) {
    this(root, siteTitle, siteUrl, layoutFile, versions, nav, siteKey, sql, http, objects, rss, null);
  }

  public VirtualSiteConfig(
      Path root,
      String siteTitle,
      String siteUrl,
      String layoutFile,
      List<VersionSpec> versions,
      List<NavSpec> nav,
      String siteKey,
      SqlSpec sql,
      HttpSpec http,
      ObjectsSpec objects,
      RssSpec rss,
      IcalendarSpec icalendar) {
    this(root, siteTitle, siteUrl, layoutFile, versions, nav, siteKey, sql, http, objects, rss, icalendar, null);
  }

  public VirtualSiteConfig(
      Path root,
      String siteTitle,
      String siteUrl,
      String layoutFile,
      List<VersionSpec> versions,
      List<NavSpec> nav,
      String siteKey,
      SqlSpec sql,
      HttpSpec http,
      ObjectsSpec objects,
      RssSpec rss,
      IcalendarSpec icalendar,
      SitemapSpec sitemap) {
    this(
        root,
        siteTitle,
        siteUrl,
        layoutFile,
        versions,
        nav,
        siteKey,
        sql,
        http,
        objects,
        rss,
        icalendar,
        sitemap,
        null);
  }

  public VirtualSiteConfig(
      Path root,
      String siteTitle,
      String siteUrl,
      String layoutFile,
      List<VersionSpec> versions,
      List<NavSpec> nav,
      String siteKey,
      SqlSpec sql,
      HttpSpec http,
      ObjectsSpec objects,
      RssSpec rss,
      IcalendarSpec icalendar,
      SitemapSpec sitemap,
      RobotsSpec robots) {
    this(
        root,
        siteTitle,
        siteUrl,
        layoutFile,
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
        null);
  }

  public VirtualSiteConfig(
      Path root,
      String siteTitle,
      String siteUrl,
      String layoutFile,
      List<VersionSpec> versions,
      List<NavSpec> nav,
      String siteKey,
      SqlSpec sql,
      HttpSpec http,
      ObjectsSpec objects,
      RssSpec rss,
      IcalendarSpec icalendar,
      SitemapSpec sitemap,
      RobotsSpec robots,
      LlmsSpec llms) {
    this.root = Objects.requireNonNull(root, "root");
    this.siteTitle = siteTitle != null ? siteTitle : "Documentation";
    this.siteUrl = siteUrl != null ? siteUrl : "";
    this.layoutFile = layoutFile != null && !layoutFile.isBlank() ? layoutFile : "page.html";
    this.versions =
        versions == null
            ? List.of()
            : Collections.unmodifiableList(new ArrayList<>(versions));
    this.nav =
        nav == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(nav));
    this.siteKey = siteKey != null && !siteKey.isBlank() ? siteKey : "default";
    this.sql = sql;
    this.http = http;
    this.objects = objects;
    this.rss = rss;
    this.icalendar = icalendar;
    this.sitemap = sitemap;
    this.robots = robots;
    this.llms = llms;
  }

  public Path root() {
    return root;
  }

  public String siteTitle() {
    return siteTitle;
  }

  public String siteUrl() {
    return siteUrl;
  }

  public String layoutFile() {
    return layoutFile;
  }

  public List<VersionSpec> versions() {
    return versions;
  }

  public List<NavSpec> nav() {
    return nav;
  }

  public String siteKey() {
    return siteKey;
  }

  /**
   * Optional JDBC settings for {@code sql-database} sources ({@code sql:} in {@code _config.yaml}).
   *
   * @return spec, or null when the mapping is omitted
   */
  public SqlSpec sql() {
    return sql;
  }

  /**
   * Optional HTTP JSON catalog settings for {@code http-json} sources ({@code http:} in {@code
   * _config.yaml}).
   *
   * @return spec, or null when the mapping is omitted (adapter then uses {@code pages.json} under
   *     the site root)
   */
  public HttpSpec http() {
    return http;
  }

  /**
   * Optional object-key listing for {@code object-storage} sources ({@code objects:} in {@code
   * _config.yaml}). When omitted, the adapter walks version folders for Markdown / HTML / JSON
   * objects.
   *
   * @return spec, or null when the mapping is omitted
   */
  public ObjectsSpec objects() {
    return objects;
  }

  /**
   * Optional RSS / Atom feed settings for {@code rss-atom} sources ({@code rss:} in {@code
   * _config.yaml}).
   *
   * @return spec, or null when the mapping is omitted (adapter then uses {@code feed.xml} or {@code
   *     atom.xml} under the site root)
   */
  public RssSpec rss() {
    return rss;
  }

  /**
   * Optional iCalendar settings for {@code icalendar} sources ({@code icalendar:} in {@code
   * _config.yaml}).
   *
   * @return spec, or null when the mapping is omitted (adapter then uses {@code calendar.ics}
   *     under the site root)
   */
  public IcalendarSpec icalendar() {
    return icalendar;
  }

  /**
   * Optional sitemap settings for {@code sitemap-xml} sources ({@code sitemap:} in {@code
   * _config.yaml}).
   *
   * @return spec, or null when the mapping is omitted (adapter then uses {@code sitemap.xml} under
   *     the site root). Legacy constructors (12-arg and earlier) always pass {@code null} here; a
   *     {@code SITEMAP_XML} site wired that way also falls back to {@code sitemap.xml} and does
   *     not fail.
   */
  public SitemapSpec sitemap() {
    return sitemap;
  }

  /**
   * Optional robots.txt settings for {@code robots-txt} sources ({@code robots:} in {@code
   * _config.yaml}).
   *
   * @return spec, or null when the mapping is omitted (adapter then uses {@code robots.txt} under
   *     the site root). Legacy constructors always pass {@code null} here; a {@code ROBOTS_TXT}
   *     site wired that way also falls back to {@code robots.txt} and does not fail.
   */
  public RobotsSpec robots() {
    return robots;
  }

  /**
   * Optional llms.txt settings for {@code llms-txt} sources ({@code llms:} in {@code
   * _config.yaml}).
   *
   * @return spec, or null when the mapping is omitted (adapter then uses {@code llms.txt} under
   *     the site root). Legacy constructors always pass {@code null} here; a {@code LLMS_TXT}
   *     site wired that way also falls back to {@code llms.txt} and does not fail.
   */
  public LlmsSpec llms() {
    return llms;
  }

  public Path themeDir() {
    return root.resolve("_theme");
  }

  public Path assetsDir() {
    return root.resolve("assets");
  }

  /** Version entry from {@code _config.yaml}. */
  public static final class VersionSpec {
    private final String id;
    private final String label;
    private final String path;
    private final boolean defaultVersion;

    public VersionSpec(String id, String label, String path, boolean defaultVersion) {
      this.id = Objects.requireNonNull(id, "id");
      this.label = label != null ? label : id;
      this.path = path != null ? path : id;
      this.defaultVersion = defaultVersion;
    }

    public String id() {
      return id;
    }

    public String label() {
      return label;
    }

    public String path() {
      return path;
    }

    public boolean defaultVersion() {
      return defaultVersion;
    }
  }

  /**
   * JDBC query settings for {@code sql-database}. {@link #toString()} omits the password field so
   * logs cannot leak credentials.
   */
  public static final class SqlSpec {
    private final String jdbcUrl;
    private final String user;
    private final String password;
    private final String query;
    private final String queryFile;
    private final String idColumn;
    private final String titleColumn;
    private final String bodyColumn;
    private final String pathColumn;
    private final String orderColumn;
    private final String versionColumn;

    public SqlSpec(
        String jdbcUrl,
        String user,
        String password,
        String query,
        String queryFile,
        String idColumn,
        String titleColumn,
        String bodyColumn,
        String pathColumn,
        String orderColumn,
        String versionColumn) {
      this.jdbcUrl = jdbcUrl != null ? jdbcUrl.trim() : "";
      this.user = user != null ? user.trim() : "";
      this.password = password != null ? password : "";
      this.query = query != null ? query.trim() : "";
      this.queryFile = queryFile != null ? queryFile.trim() : "";
      this.idColumn = columnOrDefault(idColumn, "id");
      this.titleColumn = columnOrDefault(titleColumn, "title");
      this.bodyColumn = columnOrDefault(bodyColumn, "body");
      this.pathColumn = columnOrDefault(pathColumn, "path");
      this.orderColumn = columnOrDefault(orderColumn, "order");
      this.versionColumn = columnOrDefault(versionColumn, "version");
    }

    private static String columnOrDefault(String raw, String fallback) {
      if (raw == null || raw.isBlank()) {
        return fallback;
      }
      return raw.trim();
    }

    public String jdbcUrl() {
      return jdbcUrl;
    }

    public String user() {
      return user;
    }

    public String password() {
      return password;
    }

    public String query() {
      return query;
    }

    public String queryFile() {
      return queryFile;
    }

    public String idColumn() {
      return idColumn;
    }

    public String titleColumn() {
      return titleColumn;
    }

    public String bodyColumn() {
      return bodyColumn;
    }

    public String pathColumn() {
      return pathColumn;
    }

    public String orderColumn() {
      return orderColumn;
    }

    public String versionColumn() {
      return versionColumn;
    }

    @Override
    public String toString() {
      return "SqlSpec{jdbcUrl='"
          + jdbcUrl
          + "', user='"
          + user
          + "', query='"
          + query
          + "', queryFile='"
          + queryFile
          + "', idColumn='"
          + idColumn
          + "', titleColumn='"
          + titleColumn
          + "', bodyColumn='"
          + bodyColumn
          + "', pathColumn='"
          + pathColumn
          + "', orderColumn='"
          + orderColumn
          + "', versionColumn='"
          + versionColumn
          + "'}";
    }
  }

  /**
   * HTTP JSON catalog settings for {@code http-json}. Either {@link #url()} (http/https GET) or
   * {@link #file()} (portable path under the site root). Both blank means default {@code
   * pages.json}.
   */
  public static final class HttpSpec {
    private final String url;
    private final String file;

    public HttpSpec(String url, String file) {
      this.url = url != null ? url.trim() : "";
      this.file = file != null ? file.trim() : "";
    }

    public String url() {
      return url;
    }

    public String file() {
      return file;
    }

    public boolean hasUrl() {
      return !url.isBlank();
    }

    public boolean hasFile() {
      return !file.isBlank();
    }

    @Override
    public String toString() {
      return "HttpSpec{url='" + url + "', file='" + file + "'}";
    }
  }

  /**
   * RSS / Atom feed settings for {@code rss-atom}. Either {@link #url()} (loopback http/https GET)
   * or {@link #file()} (portable path under the site root). Both blank means default {@code
   * feed.xml} then {@code atom.xml}.
   */
  public static final class RssSpec {
    private final String url;
    private final String file;

    public RssSpec(String url, String file) {
      this.url = url != null ? url.trim() : "";
      this.file = file != null ? file.trim() : "";
    }

    public String url() {
      return url;
    }

    public String file() {
      return file;
    }

    public boolean hasUrl() {
      return !url.isBlank();
    }

    public boolean hasFile() {
      return !file.isBlank();
    }

    @Override
    public String toString() {
      return "RssSpec{url='" + url + "', file='" + file + "'}";
    }
  }

  /**
   * iCalendar fixture settings for {@code icalendar}. {@link #file()} is a portable path under the
   * site root. {@link #url()} is parsed so the adapter can reject live CalDAV / remote {@code
   * .ics} URLs (local fixture only). Both blank means default {@code calendar.ics}.
   */
  public static final class IcalendarSpec {
    private final String url;
    private final String file;

    public IcalendarSpec(String url, String file) {
      this.url = url != null ? url.trim() : "";
      this.file = file != null ? file.trim() : "";
    }

    public String url() {
      return url;
    }

    public String file() {
      return file;
    }

    public boolean hasUrl() {
      return !url.isBlank();
    }

    public boolean hasFile() {
      return !file.isBlank();
    }

    @Override
    public String toString() {
      return "IcalendarSpec{url='" + url + "', file='" + file + "'}";
    }
  }

  /**
   * Sitemap fixture settings for {@code sitemap-xml}. {@link #file()} is a portable path under the
   * site root. {@link #url()} is parsed so the adapter can reject live remote sitemap crawls (local
   * fixture only). Both blank means default {@code sitemap.xml}.
   */
  public static final class SitemapSpec {
    private final String url;
    private final String file;

    public SitemapSpec(String url, String file) {
      this.url = url != null ? url.trim() : "";
      this.file = file != null ? file.trim() : "";
    }

    public String url() {
      return url;
    }

    public String file() {
      return file;
    }

    public boolean hasUrl() {
      return !url.isBlank();
    }

    public boolean hasFile() {
      return !file.isBlank();
    }

    @Override
    public String toString() {
      return "SitemapSpec{url='" + url + "', file='" + file + "'}";
    }
  }

  /**
   * Robots.txt fixture settings for {@code robots-txt}. {@link #file()} is a portable path under
   * the site root. {@link #url()} is parsed so the adapter can reject live remote robots crawls
   * (local fixture only). Both blank means default {@code robots.txt}.
   */
  public static final class RobotsSpec {
    private final String url;
    private final String file;

    public RobotsSpec(String url, String file) {
      this.url = url != null ? url.trim() : "";
      this.file = file != null ? file.trim() : "";
    }

    public String url() {
      return url;
    }

    public String file() {
      return file;
    }

    public boolean hasUrl() {
      return !url.isBlank();
    }

    public boolean hasFile() {
      return !file.isBlank();
    }

    @Override
    public String toString() {
      return "RobotsSpec{url='" + url + "', file='" + file + "'}";
    }
  }

  /**
   * llms.txt fixture settings for {@code llms-txt}. {@link #file()} is a portable path under the
   * site root. {@link #url()} is parsed so the adapter can reject live remote llms.txt fetches
   * (local fixture only). Both blank means default {@code llms.txt}.
   */
  public static final class LlmsSpec {
    private final String url;
    private final String file;

    public LlmsSpec(String url, String file) {
      this.url = url != null ? url.trim() : "";
      this.file = file != null ? file.trim() : "";
    }

    public String url() {
      return url;
    }

    public String file() {
      return file;
    }

    public boolean hasUrl() {
      return !url.isBlank();
    }

    public boolean hasFile() {
      return !file.isBlank();
    }

    @Override
    public String toString() {
      return "LlmsSpec{url='" + url + "', file='" + file + "'}";
    }
  }

  /**
   * Local object-key listing for {@code object-storage}. Keys are portable relative paths under the
   * site root (logical {@code /}, no remaining {@code ..}).
   */
  public static final class ObjectsSpec {
    private final List<String> keys;

    public ObjectsSpec(List<String> keys) {
      this.keys =
          keys == null
              ? List.of()
              : Collections.unmodifiableList(new ArrayList<>(keys));
    }

    public List<String> keys() {
      return keys;
    }

    public boolean hasKeys() {
      return !keys.isEmpty();
    }

    @Override
    public String toString() {
      return "ObjectsSpec{keys=" + keys.size() + "}";
    }
  }

  /** Optional top-level nav override. */
  public static final class NavSpec {
    private final String title;
    private final String id;

    public NavSpec(String title, String id) {
      this.title = title != null ? title : id;
      this.id = Objects.requireNonNull(id, "id");
    }

    public String title() {
      return title;
    }

    public String id() {
      return id;
    }
  }
}
