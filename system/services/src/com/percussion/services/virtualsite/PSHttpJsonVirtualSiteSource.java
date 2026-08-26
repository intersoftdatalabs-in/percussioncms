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

import com.percussion.security.validation.URLValidation;
import com.percussion.services.virtualsite.VirtualSiteConfig.HttpSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.VersionSpec;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Virtual Site source: HTTP GET of a JSON page catalog, or a local JSON file under the site root.
 *
 * <p>JSON contract: a root object with {@code pages} array. Each page requires {@code id}; {@code
 * title} and {@code body} assemble like csv-filesystem / sql-database. Optional {@code path}
 * defaults to {@code {id}.html}. Optional {@code order} and {@code version}.
 *
 * <p>Open JSON only — this slice does not send Authorization headers, API keys, or other secrets.
 *
 * <p>SSRF fail-closed: remote catalogs must be {@code http} or {@code https} with no userinfo;
 * {@link URLValidation} rejects metadata hosts and non-baseline private URLs; redirects off
 * loopback are refused. Local catalogs use portable NIO {@link Path} / {@link Files} under the
 * site root.
 *
 * <p>Stateless: {@link #discover} and {@link #load} always re-read the current HTTP response or
 * local catalog file bytes via {@link Files#readString}. No path/mtime parse cache or catalog
 * cache is kept on the instance or in statics — a second build in the same JVM after a JSON
 * fixture ({@code http.file} / default {@code pages.json}) or {@code _config.yaml} edit, or
 * after a new HTTP catalog body, must see the new bytes. File watchers are not used; {@code
 * _config.yaml} is reloaded by {@link PSVirtualSiteBuildService}, not this source.
 */
public class PSHttpJsonVirtualSiteSource implements IPSVirtualSiteSource {

  static final String DEFAULT_CATALOG_FILE = "pages.json";
  static final int MAX_CATALOG_BYTES = 2_000_000;
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
  private static final Set<String> LOOPBACK_HOSTS =
      Set.of("localhost", "127.0.0.1", "::1", "[::1]");

  @Override
  public String sourceType() {
    return VirtualSiteSourceType.HTTP_JSON.wireName();
  }

  @Override
  public List<VirtualItemRef> discover(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    List<LoadedRow> rows = loadAllRows(config);
    List<VirtualItemRef> refs = new ArrayList<>(rows.size());
    for (LoadedRow row : rows) {
      refs.add(row.ref());
    }
    refs.sort(
        Comparator.comparing(VirtualItemRef::versionId)
            .thenComparingInt(VirtualItemRef::order)
            .thenComparing(r -> r.relativePath().toString().replace('\\', '/')));
    return refs;
  }

  @Override
  public VirtualItem load(VirtualSiteConfig config, VirtualItemRef ref)
      throws IOException, VirtualSiteException {
    if (ref == null) {
      throw new VirtualSiteException("http-json item ref is required");
    }
    List<LoadedRow> rows = loadAllRows(config);
    for (LoadedRow row : rows) {
      if (row.ref().versionId().equals(ref.versionId()) && row.ref().id().equals(ref.id())) {
        return new VirtualItem(row.ref(), row.frontmatter(), row.body(), row.sourcePath());
      }
    }
    throw new VirtualSiteException(
        "Unknown http-json page id '" + ref.id() + "' in version " + ref.versionId());
  }

  private static List<LoadedRow> loadAllRows(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    if (config == null) {
      throw new VirtualSiteException("Virtual Site config is required");
    }
    Path root = config.root();
    if (root == null || !PSVirtualSiteHelper.isSafeRootPath(root)) {
      throw new VirtualSiteException("http-json site root is missing or unsafe");
    }
    Path safeRoot = root.normalize();
    CatalogFetch fetch = readCatalog(config, safeRoot);
    List<JSONObject> pages = parsePages(fetch.jsonText());
    List<LoadedRow> rows = new ArrayList<>();
    Map<String, Path> seenIds = new HashMap<>();
    int index = 0;
    for (JSONObject page : pages) {
      index++;
      LoadedRow loaded = toLoadedRow(page, config, fetch.sourcePath(), index);
      String idKey = loaded.ref().versionId() + "\0" + loaded.ref().id();
      Path previous = seenIds.put(idKey, loaded.ref().relativePath());
      if (previous != null) {
        throw new VirtualSiteException(
            "Duplicate http-json id '"
                + loaded.ref().id()
                + "' in version "
                + loaded.ref().versionId()
                + ": "
                + previous
                + " and "
                + loaded.ref().relativePath());
      }
      rows.add(loaded);
    }
    return rows;
  }

  private static CatalogFetch readCatalog(VirtualSiteConfig config, Path safeRoot)
      throws IOException, VirtualSiteException {
    HttpSpec spec = config.http();
    boolean hasUrl = spec != null && spec.hasUrl();
    boolean hasFile = spec != null && spec.hasFile();
    if (hasUrl && hasFile) {
      throw new VirtualSiteException("http-json must set either http.url or http.file, not both");
    }
    if (hasUrl) {
      String json = fetchHttpCatalog(spec.url());
      Path source = safeRoot.resolve(VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE).normalize();
      if (!source.startsWith(safeRoot)) {
        source = safeRoot;
      }
      return new CatalogFetch(json, source);
    }
    String relative = hasFile ? spec.file() : DEFAULT_CATALOG_FILE;
    Path file = resolveCatalogFile(safeRoot, relative);
    if (!Files.isRegularFile(file)) {
      throw new VirtualSiteException(
          "http-json catalog file not found: " + file.toAbsolutePath().normalize());
    }
    long size = Files.size(file);
    if (size > MAX_CATALOG_BYTES) {
      throw new VirtualSiteException(
          "http-json catalog exceeds " + MAX_CATALOG_BYTES + " bytes: " + file);
    }
    return new CatalogFetch(Files.readString(file, StandardCharsets.UTF_8), file);
  }

  static String fetchHttpCatalog(String urlString) throws IOException, VirtualSiteException {
    URL current = requireSafeHttpUrl(urlString);
    HttpClient client =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    int hops = 0;
    while (true) {
      URI requestUri = toRequestUri(current);
      HttpRequest request =
          HttpRequest.newBuilder(requestUri) // codeql[java/ssrf]
              .timeout(REQUEST_TIMEOUT)
              .GET()
              .header("Accept", "application/json, text/plain;q=0.9, */*;q=0.1")
              .build();
      HttpResponse<byte[]> response;
      try {
        response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new VirtualSiteException("http-json request interrupted: " + redactUrl(current), e);
      } catch (IOException e) {
        throw new VirtualSiteException(
            "http-json request failed: " + redactUrl(current) + ": " + e.getMessage(), e);
      }
      int status = response.statusCode();
      if (status >= 300 && status < 400) {
        List<String> locations = response.headers().allValues("Location");
        if (locations.isEmpty()) {
          throw new VirtualSiteException(
              "http-json redirect refused (no Location): " + redactUrl(current) + " status " + status);
        }
        URL next;
        try {
          URI nextUri = current.toURI().resolve(locations.get(0).trim());
          next = nextUri.toURL();
        } catch (IllegalArgumentException | MalformedURLException | URISyntaxException e) {
          throw new VirtualSiteException(
              "http-json redirect Location is not a valid URL from " + redactUrl(current), e);
        }
        URL safeNext = requireSafeHttpUrl(next.toExternalForm());
        if (!isLiteralLoopback(safeNext.getHost())) {
          throw new VirtualSiteException(
              "http-json redirect is not loopback (SSRF fail-closed). Rejected host: "
                  + safeNext.getHost());
        }
        hops++;
        if (hops > 1) {
          throw new VirtualSiteException(
              "http-json redirect refused (more than one hop) from " + redactUrl(current));
        }
        current = safeNext;
        continue;
      }
      if (status != 200) {
        throw new VirtualSiteException(
            "http-json request failed: " + redactUrl(current) + " status " + status);
      }
      byte[] body = response.body() != null ? response.body() : new byte[0];
      if (body.length > MAX_CATALOG_BYTES) {
        throw new VirtualSiteException(
            "http-json catalog exceeds " + MAX_CATALOG_BYTES + " bytes from " + redactUrl(current));
      }
      return new String(body, StandardCharsets.UTF_8);
    }
  }

  /**
   * Validate an operator-configured catalog URL: http(s) only, no userinfo, {@link
   * URLValidation} SSRF baseline (loopback any port; public 80/443; metadata denied).
   */
  static URL requireSafeHttpUrl(String urlString) throws VirtualSiteException {
    if (urlString == null || urlString.isBlank()) {
      throw new VirtualSiteException("http-json url is required");
    }
    String raw = urlString.trim();
    if (raw.indexOf('\0') >= 0) {
      throw new VirtualSiteException("http-json url must not contain NUL");
    }
    URI parsed;
    try {
      parsed = new URI(raw);
    } catch (URISyntaxException e) {
      throw new VirtualSiteException("http-json url is not a valid URL: " + raw, e);
    }
    String protocol = parsed.getScheme();
    if (protocol == null
        || (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol))) {
      throw new VirtualSiteException(
          "http-json url must be http or https (SSRF fail-closed). Rejected: " + raw);
    }
    if (parsed.getUserInfo() != null && !parsed.getUserInfo().isBlank()) {
      throw new VirtualSiteException(
          "http-json url must not contain userinfo (no secrets in the URL)");
    }
    try {
      return URLValidation.validateURLString(raw);
    } catch (MalformedURLException e) {
      throw new VirtualSiteException("http-json url is not a valid URL: " + raw, e);
    } catch (IllegalArgumentException | SecurityException e) {
      throw new VirtualSiteException(
          "http-json url rejected (SSRF fail-closed): " + e.getMessage(), e);
    }
  }

  static URI toRequestUri(URL validated) throws VirtualSiteException {
    String protocol = "https".equalsIgnoreCase(validated.getProtocol()) ? "https" : "http";
    String path = validated.getPath();
    if (path == null || path.isBlank()) {
      path = "/";
    }
    try {
      return new URI(
          protocol,
          null,
          validated.getHost(),
          validated.getPort(),
          path,
          validated.getQuery(),
          null);
    } catch (URISyntaxException e) {
      throw new VirtualSiteException("http-json url could not be rebuilt as a request URI", e);
    }
  }

  static boolean isLiteralLoopback(String host) {
    if (host == null || host.isBlank()) {
      return false;
    }
    String normalized = host.trim().toLowerCase(Locale.ROOT).replace("[", "").replace("]", "");
    return LOOPBACK_HOSTS.contains(normalized)
        || LOOPBACK_HOSTS.contains(host.trim().toLowerCase(Locale.ROOT));
  }

  static String redactUrl(URL url) {
    if (url == null) {
      return "";
    }
    return redactUrl(url.toExternalForm());
  }

  static String redactUrl(String url) {
    if (url == null || url.isBlank()) {
      return "";
    }
    int schemeSep = url.indexOf("://");
    if (schemeSep < 0) {
      return url;
    }
    int userSep = url.indexOf('@', schemeSep + 3);
    int slash = url.indexOf('/', schemeSep + 3);
    if (userSep > schemeSep && (slash < 0 || userSep < slash)) {
      return url.substring(0, schemeSep + 3) + url.substring(userSep + 1);
    }
    return url;
  }

  static Path resolveCatalogFile(Path root, String catalogFile) throws VirtualSiteException {
    if (catalogFile == null || catalogFile.isBlank()) {
      throw new VirtualSiteException("http-json file is blank");
    }
    if (catalogFile.indexOf('\0') >= 0) {
      throw new VirtualSiteException("http-json file must not contain NUL");
    }
    String logical = catalogFile.trim().replace('\\', '/');
    if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
      throw new VirtualSiteException("http-json file must be relative to the site root");
    }
    Path safeRoot = root.normalize();
    Path resolved;
    try {
      Path relative = Path.of("");
      for (String seg : logical.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0) {
          throw new VirtualSiteException("http-json file must not contain '..' or NUL segments");
        }
        relative = relative.resolve(seg);
      }
      if (relative.getNameCount() == 0 || relative.toString().isEmpty()) {
        throw new VirtualSiteException("http-json file is empty after normalize");
      }
      resolved = safeRoot.resolve(relative).normalize();
    } catch (InvalidPathException e) {
      throw new VirtualSiteException("http-json file is not a valid path", e);
    }
    if (!resolved.startsWith(safeRoot)
        || !PSVirtualSiteHelper.isSafeRootPath(resolved)
        || remainingParent(resolved)) {
      throw new VirtualSiteException("http-json file escapes the site root");
    }
    return resolved;
  }

  private static List<JSONObject> parsePages(String jsonText) throws VirtualSiteException {
    if (jsonText == null || jsonText.isBlank()) {
      throw new VirtualSiteException("http-json catalog is empty");
    }
    JSONObject root;
    try {
      root = new JSONObject(jsonText);
    } catch (JSONException e) {
      throw new VirtualSiteException("http-json catalog is not a JSON object: " + e.getMessage(), e);
    }
    if (!root.has("pages") || root.isNull("pages")) {
      throw new VirtualSiteException("http-json catalog requires a pages array");
    }
    JSONArray pages;
    try {
      pages = root.getJSONArray("pages");
    } catch (JSONException e) {
      throw new VirtualSiteException("http-json catalog pages must be a JSON array", e);
    }
    List<JSONObject> out = new ArrayList<>(pages.length());
    for (int i = 0; i < pages.length(); i++) {
      Object item = pages.opt(i);
      if (!(item instanceof JSONObject page)) {
        throw new VirtualSiteException("http-json pages[" + i + "] must be a JSON object");
      }
      out.add(page);
    }
    return out;
  }

  private static LoadedRow toLoadedRow(
      JSONObject page, VirtualSiteConfig config, Path sourcePath, int index)
      throws VirtualSiteException {
    String where = "http-json pages[" + (index - 1) + "]";
    String id = stringField(page, "id").trim();
    String title = stringField(page, "title").trim();
    String body = stringField(page, "body");
    String pathRaw = stringField(page, "path").trim();
    String orderRaw = stringField(page, "order").trim();
    String versionRaw = stringField(page, "version").trim();
    if (id.isEmpty()) {
      throw new VirtualSiteException("http-json 'id' is required in " + where);
    }
    if (title.isEmpty()) {
      throw new VirtualSiteException("http-json 'title' is required in " + where);
    }
    VersionSpec version = resolveVersion(config, versionRaw, where);
    int order = parseOrder(orderRaw, where);
    Path relative = resolvePagePath(pathRaw, id, version.path(), where);
    VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, order, title);
    VirtualFrontmatter fm =
        new VirtualFrontmatter(id, title, "", version.id(), true, order, List.of(), false);
    return new LoadedRow(ref, fm, body != null ? body : "", sourcePath);
  }

  private static String stringField(JSONObject page, String key) {
    if (page == null || !page.has(key) || page.isNull(key)) {
      return "";
    }
    Object value = page.opt(key);
    if (value == null) {
      return "";
    }
    if (value instanceof Number n) {
      if (n.doubleValue() == n.longValue()) {
        return Long.toString(n.longValue());
      }
      return n.toString();
    }
    return String.valueOf(value).trim();
  }

  private static VersionSpec resolveVersion(
      VirtualSiteConfig config, String versionRaw, String where) throws VirtualSiteException {
    List<VersionSpec> versions = config.versions();
    if (versions == null || versions.isEmpty()) {
      throw new VirtualSiteException("http-json config must declare at least one version");
    }
    if (versionRaw == null || versionRaw.isBlank()) {
      for (VersionSpec v : versions) {
        if (v.defaultVersion()) {
          return v;
        }
      }
      return versions.get(0);
    }
    String wanted = versionRaw.trim();
    for (VersionSpec v : versions) {
      if (wanted.equals(v.id())) {
        return v;
      }
    }
    throw new VirtualSiteException(
        "http-json version '" + wanted + "' is not declared in _config.yaml (" + where + ")");
  }

  private static int parseOrder(String raw, String where) throws VirtualSiteException {
    if (raw == null || raw.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      throw new VirtualSiteException("http-json 'order' must be an integer in " + where, e);
    }
  }

  /**
   * Map the optional JSON {@code path} to a site-root-relative page path using NIO {@link
   * Path#resolve}. Logical {@code /} in the field is a href-style separator, not a filesystem join.
   * Omitted path defaults to {@code {id}.html} under the version folder.
   */
  static Path resolvePagePath(String pathRaw, String id, String versionPath, String where)
      throws VirtualSiteException {
    List<String> segments;
    if (pathRaw == null || pathRaw.isBlank()) {
      segments = List.of(versionPath, id + ".html");
    } else {
      if (pathRaw.indexOf('\0') >= 0) {
        throw new VirtualSiteException("http-json 'path' must not contain NUL in " + where);
      }
      String logical = pathRaw.trim().replace('\\', '/');
      if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
        throw new VirtualSiteException("http-json 'path' must be relative in " + where);
      }
      segments = new ArrayList<>();
      for (String seg : logical.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0) {
          throw new VirtualSiteException(
              "http-json 'path' must not contain '..' or NUL segments in " + where);
        }
        segments.add(seg);
      }
      if (segments.isEmpty()) {
        throw new VirtualSiteException("http-json 'path' is empty after normalize in " + where);
      }
      if (!segments.get(0).equals(versionPath)) {
        segments.add(0, versionPath);
      }
      String last = segments.get(segments.size() - 1);
      if (!last.contains(".")) {
        segments.set(segments.size() - 1, last + ".html");
      }
    }
    Path p = Path.of(segments.get(0));
    for (int i = 1; i < segments.size(); i++) {
      p = p.resolve(segments.get(i));
    }
    Path normalized = p.normalize();
    if (!PSVirtualSiteHelper.isSafeRootPath(normalized)
        || normalized.isAbsolute()
        || remainingParent(normalized)) {
      throw new VirtualSiteException("http-json 'path' is not a safe relative path in " + where);
    }
    return normalized;
  }

  private static boolean looksAbsoluteWindows(String logical) {
    return logical.length() >= 2
        && Character.isLetter(logical.charAt(0))
        && logical.charAt(1) == ':';
  }

  private static boolean remainingParent(Path path) {
    for (Path part : path) {
      if ("..".equals(part.toString())) {
        return true;
      }
    }
    return false;
  }

  private record CatalogFetch(String jsonText, Path sourcePath) {}

  private record LoadedRow(
      VirtualItemRef ref, VirtualFrontmatter frontmatter, String body, Path sourcePath) {}
}
