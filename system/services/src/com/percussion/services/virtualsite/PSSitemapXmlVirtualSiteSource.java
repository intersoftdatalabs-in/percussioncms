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
import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import com.percussion.services.virtualsite.VirtualSiteConfig.SitemapSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.VersionSpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Virtual Site source: local {@code sitemap.xml} (urlset / sitemapindex of local file URLs).
 *
 * <p>Default file under the site root: {@code sitemap.xml}. {@code _config.yaml} {@code
 * sitemap.file} overrides the filename. {@code sitemap.url}, Git {@code virtual.remoteUrl}, and
 * credential properties are rejected — this slice is a local fixture (loopback {@code http(s)}
 * {@code <loc>} values are allowed for tests only). Non-loopback {@code http(s)} locs fail closed.
 *
 * <p>Each {@code <url><loc>} maps the last path segment into assemble {@code id}/{@code title}
 * and the referenced file (or loopback body) into {@code body}. Optional {@code <lastmod>} is
 * noted in the Markdown body.
 *
 * <p>Stateless: {@link #discover} and {@link #load} always re-read the current local sitemap via
 * {@link Files#readString}. No path/mtime parse cache of {@code <loc>}/{@code <lastmod>} pages
 * is kept on the instance or in statics — a second build in the same JVM after an operator
 * {@link Path}/{@link Files} edit of {@code sitemap.xml} (loc, lastmod, or path) or of {@code
 * _config.yaml} {@code sitemap.file} must see the new bytes without a JVM restart. File
 * watchers are not used; {@code _config.yaml} is reloaded by {@link PSVirtualSiteBuildService},
 * not this source. XML parse is XXE fail-closed via {@link PSSecureXMLUtils}. {@link #load}
 * materializes only the matching loc body (one file read or loopback HTTP GET); it does not
 * re-fetch every loc.
 */
public class PSSitemapXmlVirtualSiteSource implements IPSVirtualSiteSource {

  static final String DEFAULT_SITEMAP_FILE = "sitemap.xml";
  /** Cap on sitemap.xml / sitemapindex XML (parser-abuse bound). */
  static final int MAX_SITEMAP_BYTES = 2_000_000;
  /** Cap on per-loc page bodies and loopback HTTP responses. */
  static final int MAX_PAGE_BYTES = 10_000_000;
  static final int MAX_INDEX_DEPTH = 4;
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
  private static final Set<String> LOOPBACK_HOSTS =
      Set.of("localhost", "127.0.0.1", "::1", "[::1]");
  private static final HttpClient LOOPBACK_HTTP_CLIENT =
      HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NEVER)
          .connectTimeout(CONNECT_TIMEOUT)
          .build();

  @Override
  public String sourceType() {
    return VirtualSiteSourceType.SITEMAP_XML.wireName();
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
      throw new VirtualSiteException("sitemap-xml item ref is required");
    }
    List<SitemapUrl> urls = loadAllUrlEntries(config);
    int index = 0;
    for (SitemapUrl url : urls) {
      index++;
      LoadedRow loaded = toLoadedRow(url, config, index, "");
      if (loaded.ref().versionId().equals(ref.versionId()) && loaded.ref().id().equals(ref.id())) {
        String pageBody = materializeLocBody(url);
        return new VirtualItem(
            loaded.ref(), loaded.frontmatter(), assembleBody(url.lastmod(), pageBody), url.sourcePath());
      }
    }
    throw new VirtualSiteException(
        "Unknown sitemap-xml page id '" + ref.id() + "' in version " + ref.versionId());
  }

  private static List<LoadedRow> loadAllRows(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    List<SitemapUrl> urls = loadAllUrlEntries(config);
    List<LoadedRow> rows = new ArrayList<>();
    Map<String, Path> seenIds = new HashMap<>();
    Map<String, Path> seenPaths = new HashMap<>();
    int index = 0;
    for (SitemapUrl url : urls) {
      index++;
      LoadedRow loaded = toLoadedRow(url, config, index, "");
      String idKey = loaded.ref().versionId() + "\0" + loaded.ref().id();
      Path previousId = seenIds.put(idKey, loaded.ref().relativePath());
      if (previousId != null) {
        throw new VirtualSiteException(
            "Duplicate sitemap-xml id '"
                + loaded.ref().id()
                + "' in version "
                + loaded.ref().versionId()
                + ": "
                + previousId
                + " and "
                + loaded.ref().relativePath());
      }
      String pathKey =
          loaded.ref().versionId()
              + "\0"
              + loaded.ref().relativePath().toString().replace('\\', '/');
      Path previousPath = seenPaths.put(pathKey, loaded.ref().relativePath());
      if (previousPath != null) {
        throw new VirtualSiteException(
            "Duplicate sitemap-xml path '"
                + loaded.ref().relativePath()
                + "' in version "
                + loaded.ref().versionId()
                + ": "
                + previousPath
                + " and "
                + loaded.ref().relativePath());
      }
      rows.add(loaded);
    }
    return rows;
  }

  private static List<SitemapUrl> loadAllUrlEntries(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    if (config == null) {
      throw new VirtualSiteException("Virtual Site config is required");
    }
    Path root = config.root();
    if (root == null || !PSVirtualSiteHelper.isSafeRootPath(root)) {
      throw new VirtualSiteException("sitemap-xml site root is missing or unsafe");
    }
    Path safeRoot = root.normalize();
    SitemapFetch fetch = readSitemap(config, safeRoot);
    List<SitemapUrl> urls = parseSitemapDocument(fetch.xmlText(), safeRoot, fetch.sourcePath(), 0);
    if (urls.isEmpty()) {
      throw new VirtualSiteException(
          "sitemap-xml fixture has no url loc entries: "
              + fetch.sourcePath().toAbsolutePath().normalize());
    }
    return urls;
  }

  private static SitemapFetch readSitemap(VirtualSiteConfig config, Path safeRoot)
      throws IOException, VirtualSiteException {
    SitemapSpec spec = config.sitemap();
    if (spec != null && spec.hasUrl()) {
      throw new VirtualSiteException(
          "sitemap.url is not supported (local sitemap.xml fixture only; no live crawl or remote"
              + " sitemap URLs)");
    }
    if (spec != null && spec.hasFile()) {
      Path file = resolveSitemapFile(safeRoot, spec.file());
      return new SitemapFetch(readSitemapFile(file), file);
    }
    Path defaultFile = safeRoot.resolve(DEFAULT_SITEMAP_FILE).normalize();
    if (Files.isRegularFile(defaultFile) && isInsideRoot(defaultFile, safeRoot)) {
      return new SitemapFetch(readSitemapFile(defaultFile), defaultFile);
    }
    throw new VirtualSiteException(
        "sitemap-xml feed not found: expected "
            + DEFAULT_SITEMAP_FILE
            + " under "
            + safeRoot.toAbsolutePath().normalize());
  }

  private static String readSitemapFile(Path file) throws IOException, VirtualSiteException {
    if (!Files.isRegularFile(file)) {
      throw new VirtualSiteException(
          "sitemap-xml file not found: " + file.toAbsolutePath().normalize());
    }
    long size = Files.size(file);
    if (size > MAX_SITEMAP_BYTES) {
      throw new VirtualSiteException(
          "sitemap-xml file exceeds " + MAX_SITEMAP_BYTES + " bytes: " + file);
    }
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  static Path resolveSitemapFile(Path root, String sitemapFile) throws VirtualSiteException {
    if (sitemapFile == null || sitemapFile.isBlank()) {
      throw new VirtualSiteException("sitemap-xml file is blank");
    }
    if (sitemapFile.indexOf('\0') >= 0) {
      throw new VirtualSiteException("sitemap-xml file must not contain NUL");
    }
    String logical = sitemapFile.trim().replace('\\', '/');
    if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
      throw new VirtualSiteException("sitemap-xml file must be relative to the site root");
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
          throw new VirtualSiteException("sitemap-xml file must not contain '..' or NUL segments");
        }
        relative = relative.resolve(seg);
      }
      if (relative.getNameCount() == 0 || relative.toString().isEmpty()) {
        throw new VirtualSiteException("sitemap-xml file is empty after normalize");
      }
      resolved = safeRoot.resolve(relative).normalize();
    } catch (InvalidPathException e) {
      throw new VirtualSiteException("sitemap-xml file is not a valid path", e);
    }
    if (!isInsideRoot(resolved, safeRoot)
        || !PSVirtualSiteHelper.isSafeRootPath(resolved)
        || remainingParent(resolved)) {
      throw new VirtualSiteException("sitemap-xml file escapes the site root");
    }
    return resolved;
  }

  private static List<SitemapUrl> parseSitemapDocument(
      String xmlText, Path safeRoot, Path sourcePath, int depth)
      throws IOException, VirtualSiteException {
    if (xmlText == null || xmlText.isBlank()) {
      throw new VirtualSiteException("sitemap-xml fixture is empty");
    }
    if (depth > MAX_INDEX_DEPTH) {
      throw new VirtualSiteException(
          "sitemap-xml sitemapindex nesting exceeds " + MAX_INDEX_DEPTH);
    }
    Document doc = parseXml(xmlText);
    Element root = doc.getDocumentElement();
    if (root == null) {
      throw new VirtualSiteException("sitemap-xml fixture has no document element");
    }
    String rootName = localName(root);
    if ("sitemapindex".equalsIgnoreCase(rootName)) {
      return expandSitemapIndex(root, safeRoot, sourcePath, depth);
    }
    if ("urlset".equalsIgnoreCase(rootName)) {
      return parseUrlset(root, safeRoot, sourcePath);
    }
    throw new VirtualSiteException(
        "sitemap-xml root must be urlset or sitemapindex. Found: " + rootName);
  }

  private static List<SitemapUrl> expandSitemapIndex(
      Element index, Path safeRoot, Path sourcePath, int depth)
      throws IOException, VirtualSiteException {
    List<Element> sitemaps = children(index, "sitemap");
    if (sitemaps.isEmpty()) {
      throw new VirtualSiteException("sitemap-xml sitemapindex has no sitemap entries");
    }
    Set<String> seen = new HashSet<>();
    List<SitemapUrl> urls = new ArrayList<>();
    int childIndex = 0;
    for (Element sitemap : sitemaps) {
      childIndex++;
      String loc = childText(sitemap, "loc");
      if (loc.isBlank()) {
        throw new VirtualSiteException(
            "sitemap-xml sitemapindex loc is required in sitemap[" + (childIndex - 1) + "]");
      }
      if (!seen.add(loc)) {
        throw new VirtualSiteException("Duplicate sitemap-xml sitemapindex loc: " + loc);
      }
      NestedSitemap nested = readNestedSitemap(loc, safeRoot, sourcePath);
      urls.addAll(parseSitemapDocument(nested.xmlText(), safeRoot, nested.sourcePath(), depth + 1));
    }
    return urls;
  }

  private static NestedSitemap readNestedSitemap(String loc, Path safeRoot, Path parentSource)
      throws IOException, VirtualSiteException {
    LocKind kind = classifyLoc(loc);
    if (kind == LocKind.HTTP) {
      URL url = requireSafeLoopbackHttpUrl(loc);
      return new NestedSitemap(fetchHttpBody(url), parentSource);
    }
    Path file = resolveLocFile(safeRoot, loc);
    return new NestedSitemap(readSitemapFile(file), file);
  }

  private static List<SitemapUrl> parseUrlset(Element urlset, Path safeRoot, Path sourcePath)
      throws IOException, VirtualSiteException {
    List<Element> urlEls = children(urlset, "url");
    if (urlEls.isEmpty()) {
      throw new VirtualSiteException("sitemap-xml urlset has no url entries");
    }
    List<SitemapUrl> urls = new ArrayList<>();
    int index = 0;
    for (Element urlEl : urlEls) {
      index++;
      String where = "sitemap-xml url[" + (index - 1) + "]";
      String loc = childText(urlEl, "loc");
      if (loc.isBlank()) {
        throw new VirtualSiteException("sitemap-xml loc is required in " + where);
      }
      String lastmod = childText(urlEl, "lastmod");
      LocKind kind = classifyLoc(loc);
      if (kind == LocKind.HTTP) {
        requireSafeLoopbackHttpUrl(loc);
        urls.add(new SitemapUrl(loc, lastmod, sourcePath, where, LocKind.HTTP));
      } else {
        Path file = resolveLocFile(safeRoot, loc);
        requireExistingPageFile(file);
        urls.add(new SitemapUrl(loc, lastmod, file, where, LocKind.FILE));
      }
    }
    return urls;
  }

  private static void requireExistingPageFile(Path file) throws IOException, VirtualSiteException {
    if (!Files.isRegularFile(file)) {
      throw new VirtualSiteException(
          "sitemap-xml loc file not found: " + file.toAbsolutePath().normalize());
    }
    long size = Files.size(file);
    if (size > MAX_PAGE_BYTES) {
      throw new VirtualSiteException(
          "sitemap-xml loc file exceeds " + MAX_PAGE_BYTES + " bytes: " + file);
    }
  }

  private static String readPageFile(Path file) throws IOException, VirtualSiteException {
    requireExistingPageFile(file);
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  private static String materializeLocBody(SitemapUrl url) throws IOException, VirtualSiteException {
    if (url.kind() == LocKind.HTTP) {
      return fetchHttpBody(requireSafeLoopbackHttpUrl(url.loc()));
    }
    return readPageFile(url.sourcePath());
  }

  static Path resolveLocFile(Path root, String loc) throws VirtualSiteException {
    if (loc == null || loc.isBlank()) {
      throw new VirtualSiteException("sitemap-xml loc is blank");
    }
    if (loc.indexOf('\0') >= 0) {
      throw new VirtualSiteException("sitemap-xml loc must not contain NUL");
    }
    Path safeRoot = root.normalize();
    String trimmed = loc.trim();
    Path resolved;
    try {
      URI uri = tryParseUri(trimmed);
      if (uri != null && uri.getScheme() != null && "file".equalsIgnoreCase(uri.getScheme())) {
        resolved = Path.of(uri).normalize();
      } else {
        String logical = stripQueryAndFragment(trimmed).replace('\\', '/');
        if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
          resolved = Path.of(logical).normalize();
        } else {
          Path relative = Path.of("");
          for (String seg : logical.split("/")) {
            if (seg.isEmpty() || ".".equals(seg)) {
              continue;
            }
            if ("..".equals(seg) || seg.indexOf('\0') >= 0) {
              throw new VirtualSiteException("sitemap-xml loc must not contain '..' or NUL segments");
            }
            relative = relative.resolve(seg);
          }
          if (relative.getNameCount() == 0 || relative.toString().isEmpty()) {
            throw new VirtualSiteException("sitemap-xml loc is empty after normalize");
          }
          resolved = safeRoot.resolve(relative).normalize();
        }
      }
    } catch (InvalidPathException e) {
      throw new VirtualSiteException("sitemap-xml loc is not a valid path: " + trimmed, e);
    }
    if (!isInsideRoot(resolved, safeRoot)
        || !PSVirtualSiteHelper.isSafeRootPath(resolved)
        || remainingParent(resolved)) {
      throw new VirtualSiteException("sitemap-xml loc escapes the site root");
    }
    return resolved;
  }

  static LocKind classifyLoc(String loc) throws VirtualSiteException {
    if (loc == null || loc.isBlank()) {
      throw new VirtualSiteException("sitemap-xml loc is blank");
    }
    URI uri = tryParseUri(loc.trim());
    if (uri == null || uri.getScheme() == null || uri.getScheme().isBlank()) {
      return LocKind.FILE;
    }
    String scheme = uri.getScheme();
    if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
      return LocKind.HTTP;
    }
    if ("file".equalsIgnoreCase(scheme)) {
      return LocKind.FILE;
    }
    throw new VirtualSiteException(
        "sitemap-xml loc scheme is not supported (file or loopback http(s) only): " + scheme);
  }

  static URL requireSafeLoopbackHttpUrl(String urlString) throws VirtualSiteException {
    if (urlString == null || urlString.isBlank()) {
      throw new VirtualSiteException("sitemap-xml loc url is required");
    }
    String raw = urlString.trim();
    if (raw.indexOf('\0') >= 0) {
      throw new VirtualSiteException("sitemap-xml loc must not contain NUL");
    }
    URI parsed;
    try {
      parsed = new URI(raw);
    } catch (URISyntaxException e) {
      throw new VirtualSiteException("sitemap-xml loc is not a valid URL: " + raw, e);
    }
    String protocol = parsed.getScheme();
    if (protocol == null
        || (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol))) {
      throw new VirtualSiteException(
          "sitemap-xml loc must be http or https when remote (SSRF fail-closed). Rejected: " + raw);
    }
    if (parsed.getUserInfo() != null && !parsed.getUserInfo().isBlank()) {
      throw new VirtualSiteException(
          "sitemap-xml loc must not contain userinfo (no secrets in the URL)");
    }
    URL validated;
    try {
      validated = URLValidation.validateURLString(raw);
    } catch (MalformedURLException e) {
      throw new VirtualSiteException("sitemap-xml loc is not a valid URL: " + raw, e);
    } catch (IllegalArgumentException | SecurityException e) {
      throw new VirtualSiteException(
          "sitemap-xml loc rejected (SSRF fail-closed): " + e.getMessage(), e);
    }
    if (!isLiteralLoopback(validated.getHost())) {
      throw new VirtualSiteException(
          "sitemap-xml loc must be loopback (no live crawl). Rejected host: " + validated.getHost());
    }
    return validated;
  }

  static String fetchHttpBody(URL current) throws IOException, VirtualSiteException {
    URI requestUri = toRequestUri(current);
    HttpRequest request =
        HttpRequest.newBuilder(requestUri) // codeql[java/ssrf]
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .header("Accept", "application/xml, text/xml, text/html, text/markdown, text/plain")
            .build();
    HttpResponse<byte[]> response;
    try {
      response = LOOPBACK_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new VirtualSiteException("sitemap-xml request interrupted: " + redactUrl(current), e);
    } catch (IOException e) {
      throw new VirtualSiteException(
          "sitemap-xml request failed: " + redactUrl(current) + ": " + e.getMessage(), e);
    }
    int status = response.statusCode();
    if (status != 200) {
      throw new VirtualSiteException(
          "sitemap-xml request failed: " + redactUrl(current) + " status " + status);
    }
    byte[] body = response.body() != null ? response.body() : new byte[0];
    if (body.length > MAX_PAGE_BYTES) {
      throw new VirtualSiteException(
          "sitemap-xml loc exceeds " + MAX_PAGE_BYTES + " bytes from " + redactUrl(current));
    }
    return new String(body, StandardCharsets.UTF_8);
  }

  static URI toRequestUri(URL validated) throws VirtualSiteException {
    String protocol = "https".equalsIgnoreCase(validated.getProtocol()) ? "https" : "http";
    String path = validated.getPath();
    if (path == null || path.isBlank()) {
      path = "/";
    }
    try {
      return new URI(
          protocol, null, validated.getHost(), validated.getPort(), path, validated.getQuery(), null);
    } catch (URISyntaxException e) {
      throw new VirtualSiteException("sitemap-xml loc could not be rebuilt as a request URI", e);
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

  private static Document parseXml(String xmlText) throws VirtualSiteException {
    try {
      DocumentBuilderFactory dbf =
          PSSecureXMLUtils.getSecuredDocumentBuilderFactory(PSXmlSecurityOptions.secure());
      dbf.setNamespaceAware(true);
      DocumentBuilder builder = dbf.newDocumentBuilder();
      builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
      InputSource source =
          new InputSource(new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8)));
      source.setEncoding(StandardCharsets.UTF_8.name());
      return builder.parse(source);
    } catch (ParserConfigurationException | SAXException e) {
      throw new VirtualSiteException(
          "sitemap-xml fixture is not well-formed XML: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new VirtualSiteException(
          "sitemap-xml fixture could not be parsed: " + e.getMessage(), e);
    }
  }

  private static LoadedRow toLoadedRow(
      SitemapUrl url, VirtualSiteConfig config, int order, String pageBody)
      throws VirtualSiteException {
    String where = url.where();
    String loc = url.loc() == null ? "" : url.loc().trim();
    String lastSegment = lastPathSegment(loc);
    String id = slugForPath(lastSegment);
    String title = titleFromSegment(lastSegment);
    if (id.isEmpty()) {
      throw new VirtualSiteException("sitemap-xml loc did not yield an id in " + where);
    }
    if (title.isEmpty()) {
      throw new VirtualSiteException("sitemap-xml loc did not yield a title in " + where);
    }
    VersionSpec version = resolveVersion(config, where);
    Path relative = resolvePagePath("", id, version.path(), where);
    VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, order, title);
    VirtualFrontmatter fm =
        new VirtualFrontmatter(id, title, "", version.id(), true, order, List.of(), false);
    String body = assembleBody(url.lastmod(), pageBody);
    return new LoadedRow(ref, fm, body, url.sourcePath());
  }

  static String assembleBody(String lastmod, String pageBody) {
    StringBuilder body = new StringBuilder();
    if (lastmod != null && !lastmod.isBlank()) {
      body.append("Last modified: ").append(lastmod.trim()).append("\n\n");
    }
    if (pageBody != null && !pageBody.isBlank()) {
      body.append(pageBody);
    }
    return body.toString();
  }

  static String lastPathSegment(String loc) {
    if (loc == null || loc.isBlank()) {
      return "";
    }
    String candidate = stripQueryAndFragment(loc.trim());
    int scheme = candidate.indexOf("://");
    if (scheme >= 0) {
      int slash = candidate.lastIndexOf('/');
      if (slash > scheme + 2) {
        candidate = slash < candidate.length() - 1 ? candidate.substring(slash + 1) : "";
      }
    } else {
      int slash = Math.max(candidate.lastIndexOf('/'), candidate.lastIndexOf('\\'));
      if (slash >= 0 && slash < candidate.length() - 1) {
        candidate = candidate.substring(slash + 1);
      }
    }
    return candidate;
  }

  static String titleFromSegment(String segment) {
    if (segment == null || segment.isBlank()) {
      return "";
    }
    String name = segment.trim();
    int dot = name.lastIndexOf('.');
    if (dot > 0) {
      name = name.substring(0, dot);
    }
    return name.isBlank() ? "" : name;
  }

  static String slugForPath(String id) {
    if (id == null || id.isBlank()) {
      return "page";
    }
    String candidate = titleFromSegment(id);
    if (candidate.isBlank()) {
      candidate = id.trim();
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < candidate.length(); i++) {
      char c = candidate.charAt(i);
      if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-') {
        sb.append(c);
      } else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '-') {
        sb.append('-');
      }
    }
    String slug = sb.toString();
    while (slug.startsWith("-") || slug.startsWith(".")) {
      slug = slug.substring(1);
    }
    while (slug.endsWith("-") || slug.endsWith(".")) {
      slug = slug.substring(0, slug.length() - 1);
    }
    return slug.isEmpty() ? "page" : slug;
  }

  private static VersionSpec resolveVersion(VirtualSiteConfig config, String where)
      throws VirtualSiteException {
    List<VersionSpec> versions = config.versions();
    if (versions == null || versions.isEmpty()) {
      throw new VirtualSiteException(
          "sitemap-xml config must declare at least one version (" + where + ")");
    }
    for (VersionSpec v : versions) {
      if (v.defaultVersion()) {
        return v;
      }
    }
    return versions.get(0);
  }

  static Path resolvePagePath(String pathRaw, String id, String versionPath, String where)
      throws VirtualSiteException {
    List<String> segments;
    if (pathRaw == null || pathRaw.isBlank()) {
      segments = List.of(versionPath, id + ".html");
    } else {
      if (pathRaw.indexOf('\0') >= 0) {
        throw new VirtualSiteException("sitemap-xml 'path' must not contain NUL in " + where);
      }
      String logical = pathRaw.trim().replace('\\', '/');
      if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
        throw new VirtualSiteException("sitemap-xml 'path' must be relative in " + where);
      }
      segments = new ArrayList<>();
      for (String seg : logical.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0) {
          throw new VirtualSiteException(
              "sitemap-xml 'path' must not contain '..' or NUL segments in " + where);
        }
        segments.add(seg);
      }
      if (segments.isEmpty()) {
        throw new VirtualSiteException("sitemap-xml 'path' is empty after normalize in " + where);
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
      throw new VirtualSiteException("sitemap-xml 'path' is not a safe relative path in " + where);
    }
    return normalized;
  }

  private static URI tryParseUri(String raw) {
    try {
      return new URI(raw);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  private static String stripQueryAndFragment(String raw) {
    String candidate = raw;
    int hash = candidate.indexOf('#');
    if (hash >= 0) {
      candidate = candidate.substring(0, hash);
    }
    int query = candidate.indexOf('?');
    if (query >= 0) {
      candidate = candidate.substring(0, query);
    }
    return candidate;
  }

  private static List<Element> children(Element parent, String expectedLocal) {
    List<Element> out = new ArrayList<>();
    if (parent == null) {
      return out;
    }
    NodeList nodes = parent.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);
      if (n instanceof Element el && localNameIs(el, expectedLocal)) {
        out.add(el);
      }
    }
    return out;
  }

  private static String childText(Element parent, String expectedLocal) {
    for (Element child : children(parent, expectedLocal)) {
      String text = child.getTextContent();
      if (text != null && !text.isBlank()) {
        return text.trim();
      }
    }
    return "";
  }

  private static boolean localNameIs(Element el, String expected) {
    if (el == null || expected == null) {
      return false;
    }
    return expected.equalsIgnoreCase(localName(el));
  }

  private static String localName(Element el) {
    String local = el.getLocalName();
    if (local != null && !local.isBlank()) {
      return local;
    }
    String tag = el.getTagName();
    if (tag == null) {
      return "";
    }
    int colon = tag.indexOf(':');
    return colon >= 0 ? tag.substring(colon + 1) : tag;
  }

  private static boolean looksAbsoluteWindows(String logical) {
    if (logical.length() < 2 || logical.charAt(1) != ':') {
      return false;
    }
    char drive = logical.charAt(0);
    return (drive >= 'A' && drive <= 'Z') || (drive >= 'a' && drive <= 'z');
  }

  /**
   * True when {@code candidate} is the same file/dir as {@code safeRoot} or a descendant.
   * Existing paths use {@link Files#isSameFile} parent walk so Windows case-insensitive
   * volumes are not rejected on drive-letter or component case. Missing paths use a
   * name-element {@link Path#startsWith} check (not a string prefix) so a sibling whose
   * name shares a prefix ({@code sm-abs} vs {@code sm-abs-other}) is not treated as inside.
   */
  static boolean isInsideRoot(Path candidate, Path safeRoot) {
    if (candidate == null || safeRoot == null) {
      return false;
    }
    Path absCandidate = candidate.toAbsolutePath().normalize();
    Path absRoot = safeRoot.toAbsolutePath().normalize();
    try {
      if (Files.exists(absCandidate) && Files.exists(absRoot)) {
        Path cursor = absCandidate;
        while (cursor != null) {
          if (Files.isSameFile(cursor, absRoot)) {
            return true;
          }
          cursor = cursor.getParent();
        }
        return false;
      }
    } catch (IOException e) {
      return false;
    }
    return isLexicalDescendant(absCandidate, absRoot);
  }

  /**
   * Name-element containment. {@link Path#startsWith} already compares path names, not
   * raw strings; the name-count bound rejects a sibling that only shares a string prefix.
   */
  private static boolean isLexicalDescendant(Path absCandidate, Path absRoot) {
    if (absCandidate.equals(absRoot)) {
      return true;
    }
    return absCandidate.startsWith(absRoot)
        && absCandidate.getNameCount() > absRoot.getNameCount();
  }

  private static boolean remainingParent(Path path) {
    for (Path part : path) {
      if ("..".equals(part.toString())) {
        return true;
      }
    }
    return false;
  }

  enum LocKind {
    FILE,
    HTTP
  }

  private record SitemapFetch(String xmlText, Path sourcePath) {}

  private record NestedSitemap(String xmlText, Path sourcePath) {}

  private record SitemapUrl(
      String loc, String lastmod, Path sourcePath, String where, LocKind kind) {}

  private record LoadedRow(
      VirtualItemRef ref, VirtualFrontmatter frontmatter, String body, Path sourcePath) {}
}
