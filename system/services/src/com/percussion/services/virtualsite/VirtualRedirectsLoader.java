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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Loads optional {@code _redirects.yaml} from a Virtual Site root.
 *
 * <p>Missing file is a no-op (empty list). Present file is parsed and validated: open redirects
 * (off-site, protocol-relative, or non-http(s) schemes) are rejected.
 */
public final class VirtualRedirectsLoader {

  public static final String DEFAULT_REDIRECTS_FILE = "_redirects.yaml";

  public static final int DEFAULT_STATUS = 301;

  private static final Set<Integer> ALLOWED_STATUS = Set.of(301, 302, 307, 308);

  private VirtualRedirectsLoader() {}

  /**
   * Load {@code root/_redirects.yaml} when present.
   *
   * @param root Virtual Site root (same directory as {@code _config.yaml})
   * @param siteUrl {@code site.url} from config; used to allow same-site absolute targets
   * @return parsed redirects, or an empty list when the file is missing
   * @throws VirtualSiteException when YAML is invalid or a target is unsafe
   * @throws IOException when the file exists but cannot be read
   */
  public static List<VirtualRedirect> loadOptional(Path root, String siteUrl)
      throws IOException, VirtualSiteException {
    if (root == null) {
      return List.of();
    }
    Path file = root.resolve(DEFAULT_REDIRECTS_FILE);
    if (!Files.isRegularFile(file)) {
      return List.of();
    }
    try (InputStream in = Files.newInputStream(file)) {
      return parseAndValidate(in, siteUrl, file.toString());
    }
  }

  static List<VirtualRedirect> parseAndValidate(InputStream in, String siteUrl, String sourceLabel)
      throws VirtualSiteException {
    List<VirtualRedirect> parsed = parse(in, sourceLabel);
    validateAll(parsed, siteUrl);
    return parsed;
  }

  @SuppressWarnings("unchecked")
  static List<VirtualRedirect> parse(InputStream in, String sourceLabel)
      throws VirtualSiteException {
    try {
      Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
      Object loaded = yaml.load(in);
      if (loaded == null) {
        return List.of();
      }
      if (!(loaded instanceof Map)) {
        throw new VirtualSiteException(
            "Redirects file root must be a YAML mapping: " + sourceLabel);
      }
      Map<String, Object> map = (Map<String, Object>) loaded;
      Object redirectsObj = map.get("redirects");
      if (redirectsObj == null) {
        return List.of();
      }
      if (!(redirectsObj instanceof List<?> list)) {
        throw new VirtualSiteException(
            "redirects must be a YAML list: " + sourceLabel);
      }
      List<VirtualRedirect> out = new ArrayList<>();
      int index = 0;
      for (Object item : list) {
        index++;
        if (!(item instanceof Map<?, ?> raw)) {
          throw new VirtualSiteException(
              "redirects[" + index + "] must be a mapping with from/to: " + sourceLabel);
        }
        Map<String, Object> entry = (Map<String, Object>) raw;
        String from = stringVal(entry.get("from"));
        String to = stringVal(entry.get("to"));
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
          throw new VirtualSiteException(
              "redirects[" + index + "] requires non-blank from and to: " + sourceLabel);
        }
        int status = statusVal(entry.get("status"), sourceLabel, index);
        out.add(new VirtualRedirect(normalizeFrom(from), to.trim(), status));
      }
      return List.copyOf(out);
    } catch (VirtualSiteException e) {
      throw e;
    } catch (Exception e) {
      throw new VirtualSiteException("Failed to parse redirects: " + sourceLabel, e);
    }
  }

  static void validateAll(List<VirtualRedirect> redirects, String siteUrl)
      throws VirtualSiteException {
    if (redirects == null || redirects.isEmpty()) {
      return;
    }
    Set<String> seen = new LinkedHashSet<>();
    for (VirtualRedirect redirect : redirects) {
      requireSafeFrom(redirect.from());
      requireSafeTarget(redirect.to(), siteUrl);
      if (!seen.add(redirect.from())) {
        throw new VirtualSiteException("Duplicate redirect from: " + redirect.from());
      }
    }
  }

  /**
   * Reject open redirects. Relative site paths and same-site http(s) URLs are allowed.
   *
   * @param to raw target
   * @param siteUrl configured site URL (host used for same-site absolute targets)
   */
  static void requireSafeTarget(String to, String siteUrl) throws VirtualSiteException {
    if (to == null || to.isBlank()) {
      throw new VirtualSiteException("Redirect target is blank");
    }
    String trimmed = to.trim();
    if (trimmed.indexOf('\0') >= 0 || trimmed.indexOf('\\') >= 0) {
      throw new VirtualSiteException("Unsafe redirect target: " + trimmed);
    }
    if (trimmed.startsWith("//")) {
      throw new VirtualSiteException(
          "Open redirect rejected (protocol-relative target): " + trimmed);
    }
    URI uri;
    try {
      uri = new URI(trimmed);
    } catch (URISyntaxException e) {
      throw new VirtualSiteException("Invalid redirect target: " + trimmed, e);
    }
    if (uri.getScheme() != null) {
      requireSameSiteAbsolute(uri, trimmed, siteUrl);
      requireSafePathSegments(uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath());
      return;
    }
    if (uri.getHost() != null) {
      throw new VirtualSiteException("Open redirect rejected (host without scheme): " + trimmed);
    }
    String path = pathOnly(trimmed);
    requireSafePathSegments(path);
  }

  static void requireSafeFrom(String from) throws VirtualSiteException {
    if (from == null || from.isBlank()) {
      throw new VirtualSiteException("Redirect from is blank");
    }
    if (from.indexOf('\0') >= 0 || from.indexOf('\\') >= 0 || from.indexOf(':') >= 0) {
      throw new VirtualSiteException("Unsafe redirect from: " + from);
    }
    if (from.startsWith("//")) {
      throw new VirtualSiteException("Redirect from must be a site-relative path: " + from);
    }
    requireSafePathSegments(from);
  }

  /**
   * Site-relative href used as the output file (no leading slash; directory froms become {@code
   * index.html}).
   */
  static String toOutputHref(String from) {
    String p = from == null ? "" : from.trim();
    if (p.startsWith("/")) {
      p = p.substring(1);
    }
    if (p.isEmpty() || p.endsWith("/")) {
      p = p + "index.html";
    }
    return p;
  }

  static String normalizeFrom(String from) {
    String p = from.trim().replace('\\', '/');
    if (!p.startsWith("/")) {
      p = "/" + p;
    }
    return p;
  }

  private static void requireSameSiteAbsolute(URI uri, String trimmed, String siteUrl)
      throws VirtualSiteException {
    String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
    if (!"http".equals(scheme) && !"https".equals(scheme)) {
      throw new VirtualSiteException(
          "Open redirect rejected (scheme '" + scheme + "'): " + trimmed);
    }
    if (uri.getRawUserInfo() != null) {
      throw new VirtualSiteException("Open redirect rejected (userinfo in target): " + trimmed);
    }
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new VirtualSiteException("Open redirect rejected (missing host): " + trimmed);
    }
    String siteHost = hostOf(siteUrl);
    if (siteHost == null || !siteHost.equalsIgnoreCase(host)) {
      throw new VirtualSiteException(
          "Open redirect rejected (target host '" + host + "' is not this site): " + trimmed);
    }
  }

  static String hostOf(String siteUrl) {
    if (siteUrl == null || siteUrl.isBlank()) {
      return null;
    }
    try {
      URI site = new URI(siteUrl.trim());
      return site.getHost();
    } catch (URISyntaxException e) {
      return null;
    }
  }

  private static String pathOnly(String target) {
    int q = target.indexOf('?');
    int h = target.indexOf('#');
    int cut = target.length();
    if (q >= 0) {
      cut = Math.min(cut, q);
    }
    if (h >= 0) {
      cut = Math.min(cut, h);
    }
    return target.substring(0, cut);
  }

  private static void requireSafePathSegments(String path) throws VirtualSiteException {
    if (path == null || path.isBlank()) {
      throw new VirtualSiteException("Redirect path is blank");
    }
    if (path.indexOf('\0') >= 0 || path.indexOf('\\') >= 0) {
      throw new VirtualSiteException("Unsafe redirect path: " + path);
    }
    for (String seg : path.split("/")) {
      if (seg.isEmpty() || ".".equals(seg)) {
        continue;
      }
      if ("..".equals(seg) || seg.indexOf(':') >= 0) {
        throw new VirtualSiteException(
            "Redirect path must not contain '..' or absolute segments: " + path);
      }
    }
  }

  private static int statusVal(Object o, String sourceLabel, int index)
      throws VirtualSiteException {
    if (o == null) {
      return DEFAULT_STATUS;
    }
    int status;
    if (o instanceof Number n) {
      status = n.intValue();
    } else {
      try {
        status = Integer.parseInt(String.valueOf(o).trim());
      } catch (NumberFormatException e) {
        throw new VirtualSiteException(
            "redirects[" + index + "] status is not a number: " + sourceLabel);
      }
    }
    if (!ALLOWED_STATUS.contains(status)) {
      throw new VirtualSiteException(
          "redirects["
              + index
              + "] status must be 301, 302, 307, or 308: "
              + sourceLabel);
    }
    return status;
  }

  private static String stringVal(Object o) {
    return o == null ? null : String.valueOf(o).trim();
  }
}
