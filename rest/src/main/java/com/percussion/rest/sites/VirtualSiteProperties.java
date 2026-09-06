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
package com.percussion.rest.sites;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Wire DTO for Virtual Site property bag keys stored on a CMS Site.
 *
 * <p>Property names (system layer):
 *
 * <ul>
 *   <li>{@code virtual.sourceKind}
 *   <li>{@code virtual.rootPath}
 *   <li>{@code virtual.remoteUrl}
 *   <li>{@code virtual.branch}
 *   <li>{@code virtual.configFile}
 *   <li>{@code virtual.siteKey}
 * </ul>
 *
 * <p>Blank / missing {@code sourceKind} (or value {@code repository}) means a traditional repository
 * Site. Allow-listed virtual adapters: {@code git-filesystem}, {@code csv-filesystem}, {@code
 * sql-database}, {@code http-json}, {@code object-storage}, {@code rss-atom}, {@code icalendar},
 * {@code sitemap-xml}, {@code robots-txt}, {@code llms-txt}. Optional {@code remoteUrl} + {@code
 * branch} apply to {@code git-filesystem} only (fetch/clone into a contained work directory
 * before discover); blank remote keeps local {@code rootPath}. {@code csv-filesystem}, {@code
 * sql-database}, {@code http-json}, {@code object-storage}, {@code rss-atom}, {@code icalendar},
 * {@code sitemap-xml}, {@code robots-txt}, and {@code llms-txt} reject {@code remoteUrl} (no
 * secrets on this envelope). {@code sql-database} connection fields (JDBC URL, user, query) live
 * in {@code _config.yaml} under {@code rootPath} — never put passwords on this envelope or in
 * logs. {@code http-json} catalog URL/file live in {@code _config.yaml} ({@code http.url} / {@code
 * http.file}); REST persists a safe {@code rootPath} JSON fixture directory. {@code
 * object-storage}, {@code rss-atom}, {@code icalendar}, {@code sitemap-xml}, {@code robots-txt},
 * and {@code llms-txt} persist a portable-safe local {@code rootPath} (NIO Path; no remaining
 * {@code ..}); cloud URLs and credential properties are 400 ({@code rss-atom} is local/loopback
 * only; no live feed credentials; {@code icalendar} is a local RFC 5545 fixture only — no CalDAV;
 * {@code sitemap-xml} is a local sitemap.xml fixture only — no live crawl; {@code robots-txt} is
 * a local robots.txt fixture only — no live crawl; {@code llms-txt} is a local llms.txt fixture
 * only — no live HTTP fetch).
 * REST
 * {@code POST …/virtual/build} runs {@code http-json}, {@code object-storage}, {@code
 * rss-atom}, {@code icalendar}, {@code sitemap-xml}, {@code robots-txt}, and {@code llms-txt} through the existing {@code
 * IPSVirtualSiteSource} factory (local fixture / loopback JSON; local object-key bucket; local
 * RSS/Atom fixture; local RFC 5545 {@code calendar.ics}; local {@code sitemap.xml} urlset; local
 * {@code robots.txt}; local {@code llms.txt}). REST {@code GET …/virtual/preview}
 * streams last-build HTML for {@code object-storage}, {@code rss-atom}, {@code icalendar},
 * {@code sitemap-xml}, {@code robots-txt}, and {@code llms-txt} after a successful assemble (missing build is {@code available=false},
 * HTTP 200; {@code sitemap-xml}, {@code robots-txt}, and {@code llms-txt} are last-build local HTML only — no live crawl or HTTP fetch). REST {@code POST
 * …/virtual/publish} copies last-build HTML to {@code IPSSite.root} for git, CSV, SQL, {@code
 * http-json}, {@code object-storage} (local object-key fixture; leftover {@code virtual.remoteUrl}
 * is 400), {@code rss-atom} (local RSS/Atom fixture; leftover {@code virtual.remoteUrl} and
 * credentials are 400), {@code icalendar} (local RFC 5545 fixture; leftover {@code
 * virtual.remoteUrl} and credentials are 400), and {@code sitemap-xml} (local sitemap.xml
 * fixture; leftover {@code virtual.remoteUrl}, credentials, and cloud URL {@code rootPath} are
 * 400; no live crawl).
 *
 * <p>Wire getters return plain {@code String} (not {@code Optional}) so JAXB/Jettison and Jackson
 * {@code WRAP_ROOT_VALUE} emit/accept child elements {@code sourceKind}, {@code rootPath},
 * {@code remoteUrl}, {@code branch}, {@code configFile}, and {@code siteKey} under root {@code
 * VirtualSiteProperties}. Optional getters historically produced JAXB {@code unexpected element
 * sourceKind} on PUT (#3365 / QA #3030).
 */
@XmlRootElement(name = "VirtualSiteProperties")
@JsonRootName("VirtualSiteProperties")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "VirtualSiteProperties",
    description =
        "Virtual Site source configuration (virtual.sourceKind, virtual.rootPath,"
            + " virtual.remoteUrl, virtual.branch, virtual.configFile, virtual.siteKey)")
public class VirtualSiteProperties {

  @Schema(
      description =
          "Adapter wire name. Allow-list: git-filesystem, csv-filesystem, sql-database, http-json,"
              + " object-storage, rss-atom, icalendar, sitemap-xml, robots-txt, llms-txt. rss-atom persist/build/preview/publish is"
              + " local/loopback only (no live feed credentials). icalendar persist is a local RFC"
              + " 5545 fixture only (portable-safe rootPath; leftover remoteUrl, credentials, and"
              + " cloud URL rootPath return 400; no CalDAV). sitemap-xml persist/build/preview is a local"
              + " sitemap.xml fixture only (portable-safe rootPath; leftover remoteUrl, credentials, and"
              + " cloud URL rootPath return 400; no live crawl; preview is last-build local HTML only)."
              + " robots-txt persist/build/preview is a local robots.txt fixture only (portable-safe"
              + " rootPath; leftover remoteUrl, credentials, and cloud URL rootPath return 400; no live"
              + " crawl; preview is last-build local HTML only). llms-txt persist/build/preview is a local llms.txt"
              + " fixture only (portable-safe rootPath; leftover remoteUrl, credentials, and cloud URL"
              + " rootPath return 400; no live HTTP fetch; preview is last-build local HTML only). REST Publish"
              + " for llms-txt stays a later slice."
              + " Blank or repository = traditional Site.",
      example = "git-filesystem")
  private String sourceKind;

  @Schema(
      description =
          "Filesystem path to the Virtual Site root (absolute or install-relative) when remoteUrl"
              + " is blank. When remoteUrl is set, optional relative path inside the checkout.",
      example = "C:/workspaces/product-docs")
  private String rootPath;

  @Schema(
      description =
          "Optional Git remote (https://, ssh://, file://, or git@host:path). When set, Build"
              + " clones or fetches into a contained work directory before discover. Blank keeps"
              + " local-path git-filesystem.",
      example = "https://git.example.com/org/product-docs.git")
  private String remoteUrl;

  @Schema(
      description = "Git branch to checkout when remoteUrl is set. Optional; default main.",
      example = "main")
  private String branch;

  @Schema(
      description = "Config file name under rootPath. Optional; default _config.yaml.",
      example = "_config.yaml")
  private String configFile;

  @Schema(
      description =
          "Participant registry key. Optional; defaults to the site name when unset.",
      example = "product-docs")
  private String siteKey;

  @Schema(
      description =
          "True when sourceKind is a non-blank virtual adapter (not repository). Read-only on"
              + " responses; ignored on write.",
      accessMode = Schema.AccessMode.READ_ONLY)
  private Boolean virtual;

  public VirtualSiteProperties() {
    // Default constructor for JAX-RS / Jackson
  }

  public String getSourceKind() {
    return sourceKind;
  }

  public void setSourceKind(String sourceKind) {
    this.sourceKind = sourceKind;
  }

  public String getRootPath() {
    return rootPath;
  }

  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  public String getRemoteUrl() {
    return remoteUrl;
  }

  public void setRemoteUrl(String remoteUrl) {
    this.remoteUrl = remoteUrl;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public String getConfigFile() {
    return configFile;
  }

  public void setConfigFile(String configFile) {
    this.configFile = configFile;
  }

  public String getSiteKey() {
    return siteKey;
  }

  public void setSiteKey(String siteKey) {
    this.siteKey = siteKey;
  }

  public Boolean getVirtual() {
    return virtual;
  }

  public void setVirtual(Boolean virtual) {
    this.virtual = virtual;
  }
}
