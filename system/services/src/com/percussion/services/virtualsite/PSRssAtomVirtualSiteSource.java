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
import com.percussion.services.virtualsite.VirtualSiteConfig.RssSpec;
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
 * Virtual Site source: local RSS 2.0 / Atom XML fixture, or a loopback HTTP GET of the same.
 *
 * <p>Default files under the site root: {@code feed.xml}, then {@code atom.xml}. {@code _config.yaml}
 * {@code rss.file} overrides the filename; {@code rss.url} is loopback {@code http}/{@code https}
 * only (in-process {@code HttpServer} in tests). Live cloud feeds, userinfo, Authorization, and
 * API keys are rejected.
 *
 * <p>Each item/entry maps {@code title} + {@code id}/{@code guid}/{@code link} + {@code content} /
 * {@code summary}/{@code description} into assemble {@code id}/{@code title}/{@code body} like
 * csv-filesystem / http-json.
 *
 * <p>Stateless: {@link #discover} and {@link #load} always re-read the current file or HTTP body.
 * No path/mtime parse cache. XML parse is XXE fail-closed via {@link PSSecureXMLUtils}.
 */
public class PSRssAtomVirtualSiteSource implements IPSVirtualSiteSource {

  static final String DEFAULT_RSS_FILE = "feed.xml";
  static final String DEFAULT_ATOM_FILE = "atom.xml";
  static final int MAX_FEED_BYTES = 2_000_000;
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
  private static final Set<String> LOOPBACK_HOSTS =
      Set.of("localhost", "127.0.0.1", "::1", "[::1]");

  @Override
  public String sourceType() {
    return VirtualSiteSourceType.RSS_ATOM.wireName();
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
      throw new VirtualSiteException("rss-atom item ref is required");
    }
    List<LoadedRow> rows = loadAllRows(config);
    for (LoadedRow row : rows) {
      if (row.ref().versionId().equals(ref.versionId()) && row.ref().id().equals(ref.id())) {
        return new VirtualItem(row.ref(), row.frontmatter(), row.body(), row.sourcePath());
      }
    }
    throw new VirtualSiteException(
        "Unknown rss-atom page id '" + ref.id() + "' in version " + ref.versionId());
  }

  private static List<LoadedRow> loadAllRows(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    if (config == null) {
      throw new VirtualSiteException("Virtual Site config is required");
    }
    Path root = config.root();
    if (root == null || !PSVirtualSiteHelper.isSafeRootPath(root)) {
      throw new VirtualSiteException("rss-atom site root is missing or unsafe");
    }
    Path safeRoot = root.normalize();
    FeedFetch fetch = readFeed(config, safeRoot);
    List<FeedEntry> entries = parseEntries(fetch.xmlText());
    List<LoadedRow> rows = new ArrayList<>();
    Map<String, Path> seenIds = new HashMap<>();
    Map<String, Path> seenPaths = new HashMap<>();
    for (FeedEntry entry : entries) {
      LoadedRow loaded = toLoadedRow(entry, config, fetch.sourcePath());
      String idKey = loaded.ref().versionId() + "\0" + loaded.ref().id();
      Path previousId = seenIds.put(idKey, loaded.ref().relativePath());
      if (previousId != null) {
        throw new VirtualSiteException(
            "Duplicate rss-atom id '"
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
            "Duplicate rss-atom path '"
                + loaded.ref().relativePath()
                + "' in version "
                + loaded.ref().versionId());
      }
      rows.add(loaded);
    }
    return rows;
  }

  private static FeedFetch readFeed(VirtualSiteConfig config, Path safeRoot)
      throws IOException, VirtualSiteException {
    RssSpec spec = config.rss();
    boolean hasUrl = spec != null && spec.hasUrl();
    boolean hasFile = spec != null && spec.hasFile();
    if (hasUrl && hasFile) {
      throw new VirtualSiteException("rss-atom must set either rss.url or rss.file, not both");
    }
    if (hasUrl) {
      String xml = fetchHttpFeed(spec.url());
      Path source = safeRoot.resolve(VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE).normalize();
      if (!source.startsWith(safeRoot)) {
        source = safeRoot;
      }
      return new FeedFetch(xml, source);
    }
    if (hasFile) {
      Path file = resolveFeedFile(safeRoot, spec.file());
      return new FeedFetch(readFeedFile(file), file);
    }
    Path rssFile = safeRoot.resolve(DEFAULT_RSS_FILE).normalize();
    Path atomFile = safeRoot.resolve(DEFAULT_ATOM_FILE).normalize();
    if (Files.isRegularFile(rssFile) && rssFile.startsWith(safeRoot)) {
      return new FeedFetch(readFeedFile(rssFile), rssFile);
    }
    if (Files.isRegularFile(atomFile) && atomFile.startsWith(safeRoot)) {
      return new FeedFetch(readFeedFile(atomFile), atomFile);
    }
    throw new VirtualSiteException(
        "rss-atom feed not found: expected "
            + DEFAULT_RSS_FILE
            + " or "
            + DEFAULT_ATOM_FILE
            + " under "
            + safeRoot.toAbsolutePath().normalize());
  }

  private static String readFeedFile(Path file) throws IOException, VirtualSiteException {
    if (!Files.isRegularFile(file)) {
      throw new VirtualSiteException(
          "rss-atom feed file not found: " + file.toAbsolutePath().normalize());
    }
    long size = Files.size(file);
    if (size > MAX_FEED_BYTES) {
      throw new VirtualSiteException(
          "rss-atom feed exceeds " + MAX_FEED_BYTES + " bytes: " + file);
    }
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  static String fetchHttpFeed(String urlString) throws IOException, VirtualSiteException {
    URL current = requireSafeLoopbackHttpUrl(urlString);
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
              .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
              .build();
      HttpResponse<byte[]> response;
      try {
        response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new VirtualSiteException("rss-atom request interrupted: " + redactUrl(current), e);
      } catch (IOException e) {
        throw new VirtualSiteException(
            "rss-atom request failed: " + redactUrl(current) + ": " + e.getMessage(), e);
      }
      int status = response.statusCode();
      if (status >= 300 && status < 400) {
        List<String> locations = response.headers().allValues("Location");
        if (locations.isEmpty()) {
          throw new VirtualSiteException(
              "rss-atom redirect refused (no Location): " + redactUrl(current) + " status " + status);
        }
        URL next;
        try {
          URI nextUri = current.toURI().resolve(locations.get(0).trim());
          next = nextUri.toURL();
        } catch (IllegalArgumentException | MalformedURLException | URISyntaxException e) {
          throw new VirtualSiteException(
              "rss-atom redirect Location is not a valid URL from " + redactUrl(current), e);
        }
        URL safeNext = requireSafeLoopbackHttpUrl(next.toExternalForm());
        hops++;
        if (hops > 1) {
          throw new VirtualSiteException(
              "rss-atom redirect refused (more than one hop) from " + redactUrl(current));
        }
        current = safeNext;
        continue;
      }
      if (status != 200) {
        throw new VirtualSiteException(
            "rss-atom request failed: " + redactUrl(current) + " status " + status);
      }
      byte[] body = response.body() != null ? response.body() : new byte[0];
      if (body.length > MAX_FEED_BYTES) {
        throw new VirtualSiteException(
            "rss-atom feed exceeds " + MAX_FEED_BYTES + " bytes from " + redactUrl(current));
      }
      return new String(body, StandardCharsets.UTF_8);
    }
  }

  /**
   * Loopback http(s) only: no cloud feeds, no userinfo, {@link URLValidation} SSRF baseline.
   */
  static URL requireSafeLoopbackHttpUrl(String urlString) throws VirtualSiteException {
    if (urlString == null || urlString.isBlank()) {
      throw new VirtualSiteException("rss-atom url is required");
    }
    String raw = urlString.trim();
    if (raw.indexOf('\0') >= 0) {
      throw new VirtualSiteException("rss-atom url must not contain NUL");
    }
    URI parsed;
    try {
      parsed = new URI(raw);
    } catch (URISyntaxException e) {
      throw new VirtualSiteException("rss-atom url is not a valid URL: " + raw, e);
    }
    String protocol = parsed.getScheme();
    if (protocol == null
        || (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol))) {
      throw new VirtualSiteException(
          "rss-atom url must be http or https (SSRF fail-closed). Rejected: " + raw);
    }
    if (parsed.getUserInfo() != null && !parsed.getUserInfo().isBlank()) {
      throw new VirtualSiteException(
          "rss-atom url must not contain userinfo (no secrets in the URL)");
    }
    URL validated;
    try {
      validated = URLValidation.validateURLString(raw);
    } catch (MalformedURLException e) {
      throw new VirtualSiteException("rss-atom url is not a valid URL: " + raw, e);
    } catch (IllegalArgumentException | SecurityException e) {
      throw new VirtualSiteException(
          "rss-atom url rejected (SSRF fail-closed): " + e.getMessage(), e);
    }
    if (!isLiteralLoopback(validated.getHost())) {
      throw new VirtualSiteException(
          "rss-atom url must be loopback (no cloud feeds). Rejected host: " + validated.getHost());
    }
    return validated;
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
      throw new VirtualSiteException("rss-atom url could not be rebuilt as a request URI", e);
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

  static Path resolveFeedFile(Path root, String feedFile) throws VirtualSiteException {
    if (feedFile == null || feedFile.isBlank()) {
      throw new VirtualSiteException("rss-atom file is blank");
    }
    if (feedFile.indexOf('\0') >= 0) {
      throw new VirtualSiteException("rss-atom file must not contain NUL");
    }
    String logical = feedFile.trim().replace('\\', '/');
    if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
      throw new VirtualSiteException("rss-atom file must be relative to the site root");
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
          throw new VirtualSiteException("rss-atom file must not contain '..' or NUL segments");
        }
        relative = relative.resolve(seg);
      }
      if (relative.getNameCount() == 0 || relative.toString().isEmpty()) {
        throw new VirtualSiteException("rss-atom file is empty after normalize");
      }
      resolved = safeRoot.resolve(relative).normalize();
    } catch (InvalidPathException e) {
      throw new VirtualSiteException("rss-atom file is not a valid path", e);
    }
    if (!resolved.startsWith(safeRoot)
        || !PSVirtualSiteHelper.isSafeRootPath(resolved)
        || remainingParent(resolved)) {
      throw new VirtualSiteException("rss-atom file escapes the site root");
    }
    return resolved;
  }

  private static List<FeedEntry> parseEntries(String xmlText) throws VirtualSiteException {
    if (xmlText == null || xmlText.isBlank()) {
      throw new VirtualSiteException("rss-atom feed is empty");
    }
    Document doc = parseXml(xmlText);
    Element root = doc.getDocumentElement();
    if (root == null) {
      throw new VirtualSiteException("rss-atom feed has no document element");
    }
    String rootName = localName(root);
    if ("rss".equalsIgnoreCase(rootName)) {
      return parseRssItems(root);
    }
    if ("feed".equalsIgnoreCase(rootName)) {
      return parseAtomEntries(root);
    }
    throw new VirtualSiteException(
        "rss-atom feed root must be rss or feed (Atom). Found: " + rootName);
  }

  private static Document parseXml(String xmlText) throws VirtualSiteException {
    try {
      DocumentBuilderFactory dbf =
          PSSecureXMLUtils.getSecuredDocumentBuilderFactory(PSXmlSecurityOptions.secure());
      dbf.setNamespaceAware(true);
      DocumentBuilder builder = dbf.newDocumentBuilder();
      builder.setEntityResolver(
          (publicId, systemId) -> new InputSource(new StringReader("")));
      InputSource source =
          new InputSource(new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8)));
      source.setEncoding(StandardCharsets.UTF_8.name());
      return builder.parse(source);
    } catch (ParserConfigurationException | SAXException e) {
      throw new VirtualSiteException("rss-atom feed is not well-formed XML: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new VirtualSiteException("rss-atom feed could not be parsed: " + e.getMessage(), e);
    }
  }

  private static List<FeedEntry> parseRssItems(Element rss) throws VirtualSiteException {
    Element channel = firstChild(rss, "channel");
    if (channel == null) {
      throw new VirtualSiteException("rss-atom RSS feed requires a channel element");
    }
    List<Element> items = children(channel, "item");
    List<FeedEntry> entries = new ArrayList<>(items.size());
    int index = 0;
    for (Element item : items) {
      index++;
      String where = "rss-atom item[" + (index - 1) + "]";
      String title = childText(item, "title");
      String link = childText(item, "link");
      String guid = childText(item, "guid");
      String content = childText(item, "encoded");
      if (content.isBlank()) {
        content = childText(item, "content");
      }
      if (content.isBlank()) {
        content = childText(item, "description");
      }
      String id = firstNonBlank(guid, link);
      entries.add(new FeedEntry(id, title, content, index, where));
    }
    return entries;
  }

  private static List<FeedEntry> parseAtomEntries(Element feed) throws VirtualSiteException {
    List<Element> atomEntries = children(feed, "entry");
    List<FeedEntry> entries = new ArrayList<>(atomEntries.size());
    int index = 0;
    for (Element entry : atomEntries) {
      index++;
      String where = "rss-atom entry[" + (index - 1) + "]";
      String title = childText(entry, "title");
      String id = childText(entry, "id");
      if (id.isBlank()) {
        id = atomLinkHref(entry);
      }
      String content = childText(entry, "content");
      if (content.isBlank()) {
        content = childText(entry, "summary");
      }
      entries.add(new FeedEntry(id, title, content, index, where));
    }
    return entries;
  }

  private static LoadedRow toLoadedRow(FeedEntry entry, VirtualSiteConfig config, Path sourcePath)
      throws VirtualSiteException {
    String where = entry.where();
    String id = entry.id() == null ? "" : entry.id().trim();
    String title = entry.title() == null ? "" : entry.title().trim();
    String body = entry.body() == null ? "" : entry.body();
    if (id.isEmpty()) {
      throw new VirtualSiteException("rss-atom 'id' is required in " + where);
    }
    if (title.isEmpty()) {
      throw new VirtualSiteException("rss-atom 'title' is required in " + where);
    }
    VersionSpec version = resolveVersion(config, where);
    Path relative = resolvePagePath("", slugForPath(id), version.path(), where);
    VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, entry.order(), title);
    VirtualFrontmatter fm =
        new VirtualFrontmatter(id, title, "", version.id(), true, entry.order(), List.of(), false);
    return new LoadedRow(ref, fm, body, sourcePath);
  }

  static String slugForPath(String id) {
    if (id == null || id.isBlank()) {
      return "item";
    }
    String candidate = id.trim();
    int scheme = candidate.indexOf("://");
    if (scheme >= 0) {
      int slash = candidate.lastIndexOf('/');
      if (slash > scheme + 2 && slash < candidate.length() - 1) {
        candidate = candidate.substring(slash + 1);
        int query = candidate.indexOf('?');
        if (query >= 0) {
          candidate = candidate.substring(0, query);
        }
        int hash = candidate.indexOf('#');
        if (hash >= 0) {
          candidate = candidate.substring(0, hash);
        }
      }
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
    return slug.isEmpty() ? "item" : slug;
  }

  private static VersionSpec resolveVersion(VirtualSiteConfig config, String where)
      throws VirtualSiteException {
    List<VersionSpec> versions = config.versions();
    if (versions == null || versions.isEmpty()) {
      throw new VirtualSiteException("rss-atom config must declare at least one version (" + where + ")");
    }
    for (VersionSpec v : versions) {
      if (v.defaultVersion()) {
        return v;
      }
    }
    return versions.get(0);
  }

  /**
   * Map the optional page path to a site-root-relative path using NIO {@link Path#resolve}. Logical
   * {@code /} is a href-style separator. Omitted path defaults to {@code {id}.html} under the
   * version folder.
   */
  static Path resolvePagePath(String pathRaw, String id, String versionPath, String where)
      throws VirtualSiteException {
    List<String> segments;
    if (pathRaw == null || pathRaw.isBlank()) {
      segments = List.of(versionPath, id + ".html");
    } else {
      if (pathRaw.indexOf('\0') >= 0) {
        throw new VirtualSiteException("rss-atom 'path' must not contain NUL in " + where);
      }
      String logical = pathRaw.trim().replace('\\', '/');
      if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
        throw new VirtualSiteException("rss-atom 'path' must be relative in " + where);
      }
      segments = new ArrayList<>();
      for (String seg : logical.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0) {
          throw new VirtualSiteException(
              "rss-atom 'path' must not contain '..' or NUL segments in " + where);
        }
        segments.add(seg);
      }
      if (segments.isEmpty()) {
        throw new VirtualSiteException("rss-atom 'path' is empty after normalize in " + where);
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
      throw new VirtualSiteException("rss-atom 'path' is not a safe relative path in " + where);
    }
    return normalized;
  }

  private static String atomLinkHref(Element entry) {
    String fallback = "";
    for (Element link : children(entry, "link")) {
      String href = attr(link, "href");
      if (href.isBlank()) {
        continue;
      }
      String rel = attr(link, "rel");
      if (rel.isBlank() || "alternate".equalsIgnoreCase(rel)) {
        return href;
      }
      if (fallback.isBlank()) {
        fallback = href;
      }
    }
    return fallback;
  }

  private static String attr(Element el, String name) {
    if (el == null || !el.hasAttribute(name)) {
      return "";
    }
    String value = el.getAttribute(name);
    return value != null ? value.trim() : "";
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v.trim();
      }
    }
    return "";
  }

  private static Element firstChild(Element parent, String expectedLocal) {
    List<Element> found = children(parent, expectedLocal);
    return found.isEmpty() ? null : found.get(0);
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
    Element child = firstChild(parent, expectedLocal);
    if (child == null) {
      return "";
    }
    String text = child.getTextContent();
    return text != null ? text.trim() : "";
  }

  private static boolean localNameIs(Element el, String expected) {
    if (el == null || expected == null) {
      return false;
    }
    String local = localName(el);
    return expected.equalsIgnoreCase(local);
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

  private record FeedFetch(String xmlText, Path sourcePath) {}

  private record FeedEntry(String id, String title, String body, int order, String where) {}

  private record LoadedRow(
      VirtualItemRef ref, VirtualFrontmatter frontmatter, String body, Path sourcePath) {}
}
