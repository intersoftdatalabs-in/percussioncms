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
import java.util.Locale;
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
 *   <li>{@code virtual.sourceKind} — allow-listed adapter wire name ({@code git-filesystem},
 *       {@code csv-filesystem}, {@code sql-database}, {@code http-json}, {@code object-storage},
 *       {@code rss-atom}, {@code icalendar}, {@code sitemap-xml}, {@code robots-txt}); blank or
 *       {@code repository} ⇒
 *       traditional repository Site
 *   <li>{@code virtual.rootPath} — filesystem path to Virtual Site root when no remote is set
 *       (required when virtual and {@code virtual.remoteUrl} is blank); when a remote is set,
 *       an optional relative path inside the checkout
 *   <li>{@code virtual.remoteUrl} — optional Git remote (https / ssh / file / {@code git@host:path});
 *       blank keeps local-path {@code git-filesystem} behavior
 *   <li>{@code virtual.branch} — optional ref to checkout; default {@code main}
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
  public static final String PROP_REMOTE_URL = "virtual.remoteUrl";
  public static final String PROP_BRANCH = "virtual.branch";

  /** Wire name for traditional repository-backed Sites. */
  public static final String SOURCE_KIND_REPOSITORY = "repository";

  /** Default Git branch when {@link #PROP_BRANCH} is blank. */
  public static final String DEFAULT_BRANCH = "main";

  private PSVirtualSiteHelper() {}

  /**
   * Allow-listed {@link #PROP_SOURCE_KIND} wire names for Virtual adapters ({@code git-filesystem},
   * {@code csv-filesystem}, {@code sql-database}, {@code http-json}, {@code object-storage}, {@code
   * rss-atom}, {@code icalendar}, {@code sitemap-xml}, {@code robots-txt}). Does not include {@link
   * #SOURCE_KIND_REPOSITORY}.
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
   * Configured Git remote when {@link #PROP_REMOTE_URL} is non-blank.
   *
   * @param site may be null
   * @return trimmed remote URL
   */
  public static Optional<String> remoteUrl(IPSSite site) {
    return findProperty(site, PROP_REMOTE_URL).filter(StringUtils::isNotBlank);
  }

  /**
   * Whether a Virtual Site is configured to fetch from a Git remote before discover.
   *
   * @param site may be null
   * @return true when {@link #PROP_REMOTE_URL} is non-blank
   */
  public static boolean hasRemote(IPSSite site) {
    return remoteUrl(site).isPresent();
  }

  /**
   * Branch to checkout. Blank / missing {@link #PROP_BRANCH} ⇒ {@link #DEFAULT_BRANCH}.
   *
   * @param site may be null
   * @return non-blank branch name
   */
  public static String branch(IPSSite site) {
    return findProperty(site, PROP_BRANCH)
        .filter(StringUtils::isNotBlank)
        .orElse(DEFAULT_BRANCH);
  }

  /**
   * Validates the Virtual Site property contract.
   *
   * <p>Traditional repository Sites (missing/blank {@code virtual.sourceKind} or value {@code
   * repository}) always pass. Virtual Sites must:
   *
   * <ul>
   *   <li>use an allow-listed {@code virtual.sourceKind} (see {@link #allowedSourceKindWireNames()};
   *       {@code csv-filesystem}, {@code sql-database}, {@code http-json}, {@code object-storage},
   *       {@code rss-atom}, {@code icalendar}, {@code sitemap-xml}, and {@code robots-txt} do not
   *       accept {@code virtual.remoteUrl})
   *   <li>{@code object-storage}, {@code rss-atom}, {@code icalendar}, {@code sitemap-xml}, and
   *       {@code robots-txt} require a local filesystem {@code virtual.rootPath} (NIO {@link Path};
   *       no remaining {@code ..}); cloud URLs and credential properties are rejected
   *   <li>when {@code virtual.remoteUrl} is blank: provide a non-blank safe {@code virtual.rootPath}
   *   <li>when {@code virtual.remoteUrl} is set: a safe Git URL (https / ssh / file / {@code
   *       git@host:path}); optional {@code virtual.branch}; optional relative {@code
   *       virtual.rootPath} inside the checkout (no {@code ..}, not absolute)
   *   <li>use a safe root path after NIO {@link Path#normalize()} (no empty path; no remaining {@code
   *       ..} segments) when a local root is required
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

    if (requiresLocalOnlyRoot(type)) {
      rejectCredentialProperties(site, type);
    }

    Optional<String> remoteRaw = remoteUrl(site);
    if (type != VirtualSiteSourceType.GIT_FILESYSTEM && remoteRaw.isPresent()) {
      throw new VirtualSiteException(
          PROP_REMOTE_URL
              + " is not supported for "
              + type.wireName()
              + " (Git remotes apply to "
              + VirtualSiteSourceType.GIT_FILESYSTEM.wireName()
              + " only).");
    }
    if (remoteRaw.isPresent()) {
      PSGitRemoteCheckout.requireSafeRemoteUrl(remoteRaw.get());
      String branchRaw = findProperty(site, PROP_BRANCH).orElse("");
      if (StringUtils.isNotBlank(branchRaw)) {
        PSGitRemoteCheckout.requireSafeBranch(branchRaw);
      }
      Optional<String> rootRaw = findProperty(site, PROP_ROOT_PATH);
      if (rootRaw.isPresent()) {
        validateRemoteSubPath(rootRaw.get());
      }
    } else {
      Optional<String> rootRaw = findProperty(site, PROP_ROOT_PATH);
      if (rootRaw.isEmpty()) {
        throw new VirtualSiteException(
            PROP_ROOT_PATH
                + " is required when "
                + PROP_SOURCE_KIND
                + " is '"
                + kind
                + "' and "
                + PROP_REMOTE_URL
                + " is blank.");
      }

      if (requiresLocalOnlyRoot(type)) {
        rejectCloudOrRemoteRootPath(rootRaw.get(), type);
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
    }

    Optional<String> configRaw = findProperty(site, PROP_CONFIG_FILE);
    if (configRaw.isPresent()) {
      validateConfigFileName(configRaw.get());
    }
  }

  /**
   * When a remote is configured, {@link #PROP_ROOT_PATH} is an optional relative path inside the
   * checkout (for example {@code product-docs}). Absolute paths and remaining {@code ..} are
   * rejected.
   *
   * @param raw property value, not blank
   * @throws VirtualSiteException when the sub-path is unsafe
   */
  public static void validateRemoteSubPath(String raw) throws VirtualSiteException {
    if (StringUtils.isBlank(raw)) {
      throw new VirtualSiteException(
          PROP_ROOT_PATH + " must not be blank when set (omit it to use the checkout root).");
    }
    Path sub;
    try {
      sub = Path.of(raw.trim()).normalize();
    } catch (InvalidPathException e) {
      throw new VirtualSiteException(
          PROP_ROOT_PATH + " is not a valid filesystem path: '" + raw + "'.", e);
    }
    if (sub.isAbsolute() || raw.indexOf(':') >= 0) {
      throw new VirtualSiteException(
          PROP_ROOT_PATH
              + " must be a relative path inside the Git checkout when "
              + PROP_REMOTE_URL
              + " is set. Rejected absolute path.");
    }
    if (!isSafeRootPath(sub)) {
      throw new VirtualSiteException(
          PROP_ROOT_PATH
              + " must be a non-empty relative path with no '..' segments after normalize when "
              + PROP_REMOTE_URL
              + " is set. Rejected: '"
              + raw
              + "'.");
    }
  }

  /**
   * Discover root after an optional Git checkout.
   *
   * <p>When {@code checkoutRoot} is null, returns the local {@link #rootPath(IPSSite)}. When a
   * checkout is supplied, returns that directory or a validated relative sub-path under it.
   *
   * @param site configured site
   * @param checkoutRoot checkout work directory, or null for local-path mode
   * @return discover root
   * @throws VirtualSiteException when the path is missing or escapes the checkout
   */
  public static Path resolveDiscoverRoot(IPSSite site, Path checkoutRoot)
      throws VirtualSiteException {
    if (checkoutRoot == null) {
      return rootPath(site)
          .orElseThrow(
              () ->
                  new VirtualSiteException(
                      PROP_ROOT_PATH + " is required when " + PROP_REMOTE_URL + " is blank."));
    }
    Path safeCheckout = checkoutRoot.normalize();
    if (!isSafeRootPath(safeCheckout)) {
      throw new VirtualSiteException("Git checkout work directory is not a safe path.");
    }
    Optional<String> raw = findProperty(site, PROP_ROOT_PATH);
    if (raw.isEmpty()) {
      return safeCheckout;
    }
    validateRemoteSubPath(raw.get());
    Path resolved = safeCheckout.resolve(Path.of(raw.get().trim())).normalize();
    if (!resolved.startsWith(safeCheckout)) {
      throw new VirtualSiteException(
          PROP_ROOT_PATH + " escapes the Git checkout work directory.");
    }
    return resolved;
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
    // Path.of("") / Path.of(".").normalize() yield an empty relative path — not a usable root.
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

  /**
   * {@code object-storage} / {@code rss-atom} / {@code icalendar} / {@code sitemap-xml} / {@code
   * robots-txt} roots must be local filesystem paths. Cloud / remote URI schemes are fail-closed
   * (no S3/GCS/Azure/HTTP object buckets, live feeds, CalDAV URLs, live sitemap crawls, or live
   * robots.txt crawls, no credentials in the path).
   *
   * <p>Windows drive letters ({@code C:\…}) are not treated as URI schemes.
   *
   * @param raw {@link #PROP_ROOT_PATH} value, not blank
   * @param type adapter kind used in the error message, not null
   * @throws VirtualSiteException when the value looks like a remote/cloud URL
   */
  static void rejectCloudOrRemoteRootPath(String raw, VirtualSiteSourceType type)
      throws VirtualSiteException {
    if (StringUtils.isBlank(raw)) {
      return;
    }
    String kind = Objects.requireNonNull(type, "type").wireName();
    String trimmed = raw.trim();
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (lower.contains("://")) {
      throw new VirtualSiteException(
          PROP_ROOT_PATH
              + " for "
              + kind
              + " must be a local filesystem path (NIO Path). Cloud URLs are rejected.");
    }
    int colon = trimmed.indexOf(':');
    // Drive letter "C:\" / "C:/" is index 1; schemes such as s3:bucket have a longer prefix.
    if (colon > 1) {
      String scheme = trimmed.substring(0, colon);
      boolean uriScheme =
          !scheme.isEmpty()
              && scheme
                  .chars()
                  .allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '+' || ch == '.' || ch == '-');
      if (uriScheme) {
        throw new VirtualSiteException(
            PROP_ROOT_PATH
                + " for "
                + kind
                + " must be a local filesystem path (NIO Path). Cloud URLs are rejected.");
      }
    }
  }

  /**
   * Fail closed when extra Site properties look like cloud credentials (AWS/IAM, Azure keys,
   * connection strings). Standard {@code virtual.*} keys are never treated as credentials.
   *
   * @param site may be null
   * @param type adapter kind used in the error message, not null
   * @throws VirtualSiteException when a credential-like property name is present
   */
  static void rejectCredentialProperties(IPSSite site, VirtualSiteSourceType type)
      throws VirtualSiteException {
    String kind = Objects.requireNonNull(type, "type").wireName();
    for (PSSiteProperty p : propertiesOf(site)) {
      if (p == null || StringUtils.isBlank(p.getName())) {
        continue;
      }
      String name = p.getName().trim();
      if (isVirtualContractProperty(name)) {
        continue;
      }
      if (isCredentialPropertyName(name)) {
        throw new VirtualSiteException(
            "Credential property is not allowed for "
                + kind
                + " (no AWS/IAM/secrets on this envelope).");
      }
    }
  }

  private static boolean requiresLocalOnlyRoot(VirtualSiteSourceType type) {
    return type == VirtualSiteSourceType.OBJECT_STORAGE
        || type == VirtualSiteSourceType.RSS_ATOM
        || type == VirtualSiteSourceType.ICALENDAR
        || type == VirtualSiteSourceType.SITEMAP_XML
        || type == VirtualSiteSourceType.ROBOTS_TXT;
  }

  private static boolean isVirtualContractProperty(String name) {
    return PROP_SOURCE_KIND.equals(name)
        || PROP_ROOT_PATH.equals(name)
        || PROP_CONFIG_FILE.equals(name)
        || PROP_SITE_KEY.equals(name)
        || PROP_REMOTE_URL.equals(name)
        || PROP_BRANCH.equals(name);
  }

  private static boolean isCredentialPropertyName(String name) {
    String n =
        name.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(".", "");
    return n.contains("accesskey")
        || n.contains("secretkey")
        || n.contains("secretaccess")
        || n.contains("sessiontoken")
        || n.contains("accountkey")
        || n.contains("connectionstring")
        || n.contains("awssecret")
        || n.contains("iamrole")
        || n.equals("password");
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
