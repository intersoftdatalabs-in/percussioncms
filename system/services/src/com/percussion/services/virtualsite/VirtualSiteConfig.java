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

  public VirtualSiteConfig(
      Path root,
      String siteTitle,
      String siteUrl,
      String layoutFile,
      List<VersionSpec> versions,
      List<NavSpec> nav,
      String siteKey) {
    this(root, siteTitle, siteUrl, layoutFile, versions, nav, siteKey, null);
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
   * JDBC query settings for {@code sql-database}. Password is never included in {@link #toString()}
   * (this type does not override it).
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
