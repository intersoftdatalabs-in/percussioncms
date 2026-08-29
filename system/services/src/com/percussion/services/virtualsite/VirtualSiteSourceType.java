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

/**
 * Registered Virtual Site adapter kinds. {@link #GIT_FILESYSTEM}, {@link #CSV_FILESYSTEM}, {@link
 * #SQL_DATABASE}, {@link #HTTP_JSON}, {@link #OBJECT_STORAGE}, {@link #RSS_ATOM}, and {@link
 * #ICALENDAR} are wired through {@link PSVirtualSiteSourceFactory} and allow-listed for Site
 * property validation. REST GET/PUT persist round-trips {@link #RSS_ATOM} with a portable-safe
 * local {@code rootPath}. {@link #ICALENDAR} is SPI/CLI assemble only in this slice (local {@code
 * calendar.ics}); REST persist and Developer Sites chrome stay later slices.
 */
public enum VirtualSiteSourceType {
  GIT_FILESYSTEM("git-filesystem"),
  /** Local CSV / directory of CSVs (stable {@code id} column; Markdown in {@code body}). */
  CSV_FILESYSTEM("csv-filesystem"),
  /**
   * JDBC query adapter. This slice is in-memory H2 only ({@code jdbc:h2:mem:}); Oracle / MySQL /
   * SQL Server URLs are rejected.
   */
  SQL_DATABASE("sql-database"),
  /**
   * HTTP GET of a JSON page catalog, or a local JSON file under the site root ({@code http-json}).
   * REST GET/PUT persist this kind. Open JSON only (no Authorization / API keys). SSRF fail-closed:
   * {@code http}/{@code https}, no userinfo, {@code URLValidation}, redirects off-loopback rejected.
   * Git {@code virtual.remoteUrl} is not accepted.
   */
  HTTP_JSON("http-json"),
  /**
   * Local directory treated as an object-key bucket ({@code object-storage}). Object keys are
   * portable relative paths under {@code virtual.rootPath}. No cloud SDK, access keys, or network.
   * REST GET/PUT persist this kind with a portable-safe {@code virtual.rootPath} (NIO {@link
   * java.nio.file.Path}; no remaining {@code ..}). Cloud URLs ({@code s3://}, {@code gs://}, {@code
   * azure://}, {@code http(s)://}) and credential properties (access keys, secrets, connection
   * strings) are rejected. Git {@code virtual.remoteUrl} is not accepted. No AWS/IAM/secrets on
   * this envelope.
   */
  OBJECT_STORAGE("object-storage"),
  /**
   * Local RSS 2.0 / Atom XML syndication ({@code rss-atom}). Discovers pages from {@code feed.xml}
   * / {@code atom.xml} (or {@code _config.yaml} {@code rss.file}) under a portable-safe {@code
   * virtual.rootPath}, or from a loopback HTTP GET ({@code rss.url}) in tests. REST GET/PUT persist
   * this kind with a portable-safe {@code virtual.rootPath} (NIO {@link java.nio.file.Path}; no
   * remaining {@code ..}). Cloud URLs and credential properties are rejected. Git {@code
   * virtual.remoteUrl} is not accepted. No live feed credentials on this envelope. Developer Sites
   * chrome stays a later slice.
   */
  RSS_ATOM("rss-atom"),
  /**
   * Local RFC 5545 iCalendar ({@code icalendar}). Discovers pages from {@code calendar.ics} (or
   * {@code _config.yaml} {@code icalendar.file}) under a portable-safe {@code virtual.rootPath}.
   * Each {@code VEVENT} maps {@code UID}/{@code SUMMARY}/{@code DTSTART}/{@code DESCRIPTION} into
   * assemble {@code id}/{@code title}/{@code body}. Cloud / CalDAV URLs, {@code icalendar.url},
   * Git {@code virtual.remoteUrl}, and credential properties are rejected. REST GET/PUT persist
   * and Developer Sites chrome stay later slices. No live CalDAV, API keys, or authenticated
   * remotes on this envelope.
   */
  ICALENDAR("icalendar");

  private final String wireName;

  VirtualSiteSourceType(String wireName) {
    this.wireName = wireName;
  }

  public String wireName() {
    return wireName;
  }

  /**
   * Parse a property / config wire name.
   *
   * @param value may be null
   * @return matching type or null if unknown/blank
   */
  public static VirtualSiteSourceType fromWireName(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String v = value.trim();
    for (VirtualSiteSourceType t : values()) {
      if (t.wireName.equalsIgnoreCase(v)) {
        return t;
      }
    }
    return null;
  }
}
