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

import com.percussion.services.virtualsite.VirtualSiteConfig.GraphQlSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.VersionSpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Virtual Site source: local GraphQL SDL fixture ({@code graphql-sdl}).
 *
 * <p>Default file under the site root: {@code schema.graphql}. {@code _config.yaml} {@code
 * graphql.file} overrides the filename. {@code graphql.url}, Git {@code virtual.remoteUrl},
 * credential properties, cloud URLs, remote {@code #import} URLs, and live GraphQL HTTP /
 * introspection are rejected — this slice is a local fixture only.
 *
 * <p>Each Query / Mutation / Subscription field maps into assemble {@code id}/{@code title}/{@code
 * body}. A fixture with no operations still emits one page from the schema so CLI assemble writes
 * HTML ({@code pagesWritten > 0}).
 *
 * <p>Stateless: {@link #discover} and {@link #load} always re-read the current local fixture via
 * {@link Files#readString}. No path/mtime parse cache is kept on the instance or in statics — a
 * second build in the same JVM after a GraphQL ({@code graphql.file} / default {@code
 * schema.graphql}) or {@code _config.yaml} edit must see the new bytes. File watchers are not used;
 * {@code _config.yaml} is reloaded by {@link PSVirtualSiteBuildService}, not this source.
 */
public class PSGraphQlSdlVirtualSiteSource implements IPSVirtualSiteSource {

  static final String DEFAULT_GRAPHQL_FILE = "schema.graphql";
  static final int MAX_GRAPHQL_BYTES = 2_000_000;

  private static final Set<String> ROOT_TYPES = Set.of("query", "mutation", "subscription");
  private static final Pattern IMPORT_URL =
      Pattern.compile("(?i)#\\s*import\\s+[\"'](https?://|graphql://)");

  @Override
  public String sourceType() {
    return VirtualSiteSourceType.GRAPHQL_SDL.wireName();
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
      throw new VirtualSiteException("graphql-sdl item ref is required");
    }
    List<LoadedRow> rows = loadAllRows(config);
    for (LoadedRow row : rows) {
      if (row.ref().versionId().equals(ref.versionId()) && row.ref().id().equals(ref.id())) {
        return new VirtualItem(row.ref(), row.frontmatter(), row.body(), row.sourcePath());
      }
    }
    throw new VirtualSiteException(
        "Unknown graphql-sdl page id '" + ref.id() + "' in version " + ref.versionId());
  }

  private static List<LoadedRow> loadAllRows(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    if (config == null) {
      throw new VirtualSiteException("Virtual Site config is required");
    }
    Path root = config.root();
    if (root == null || !PSVirtualSiteHelper.isSafeRootPath(root)) {
      throw new VirtualSiteException("graphql-sdl site root is missing or unsafe");
    }
    Path safeRoot = root.normalize();
    GraphQlFetch fetch = readGraphQl(config, safeRoot);
    List<LoadedOp> ops = parseOperations(fetch.text());
    List<LoadedRow> pages = toPages(ops, fetch, config);
    if (pages.isEmpty()) {
      throw new VirtualSiteException(
          "graphql-sdl fixture produced no pages: "
              + fetch.sourcePath().toAbsolutePath().normalize());
    }
    List<LoadedRow> rows = new ArrayList<>();
    Map<String, Path> seenIds = new HashMap<>();
    Map<String, Path> seenPaths = new HashMap<>();
    for (LoadedRow loaded : pages) {
      String idKey = loaded.ref().versionId() + "\0" + loaded.ref().id();
      Path previousId = seenIds.put(idKey, loaded.ref().relativePath());
      if (previousId != null) {
        throw new VirtualSiteException(
            "Duplicate graphql-sdl id '"
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
            "Duplicate graphql-sdl path '"
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

  private static GraphQlFetch readGraphQl(VirtualSiteConfig config, Path safeRoot)
      throws IOException, VirtualSiteException {
    GraphQlSpec spec = config.graphql();
    if (spec != null && spec.hasUrl()) {
      throw new VirtualSiteException(
          "graphql.url is not supported (local schema.graphql fixture only; no live HTTP fetch,"
              + " introspection, or remote GraphQL URLs)");
    }
    if (spec != null && spec.hasFile()) {
      Path file = resolveGraphQlFile(safeRoot, spec.file());
      return new GraphQlFetch(readGraphQlFile(file), file);
    }
    Path defaultFile = safeRoot.resolve(DEFAULT_GRAPHQL_FILE).normalize();
    if (Files.isRegularFile(defaultFile) && defaultFile.startsWith(safeRoot)) {
      return new GraphQlFetch(readGraphQlFile(defaultFile), defaultFile);
    }
    throw new VirtualSiteException(
        "graphql-sdl fixture not found: expected "
            + DEFAULT_GRAPHQL_FILE
            + " under "
            + safeRoot.toAbsolutePath().normalize());
  }

  private static String readGraphQlFile(Path file) throws IOException, VirtualSiteException {
    if (!Files.isRegularFile(file)) {
      throw new VirtualSiteException(
          "graphql-sdl file not found: " + file.toAbsolutePath().normalize());
    }
    long size = Files.size(file);
    if (size > MAX_GRAPHQL_BYTES) {
      throw new VirtualSiteException(
          "graphql-sdl file exceeds " + MAX_GRAPHQL_BYTES + " bytes: " + file);
    }
    String text = Files.readString(file, StandardCharsets.UTF_8);
    if (text == null || text.isBlank()) {
      throw new VirtualSiteException("graphql-sdl fixture is empty");
    }
    if (text.indexOf('\0') >= 0) {
      throw new VirtualSiteException("graphql-sdl file must not contain NUL");
    }
    return text;
  }

  static Path resolveGraphQlFile(Path root, String graphQlFile) throws VirtualSiteException {
    if (graphQlFile == null || graphQlFile.isBlank()) {
      throw new VirtualSiteException("graphql-sdl file is blank");
    }
    if (graphQlFile.indexOf('\0') >= 0) {
      throw new VirtualSiteException("graphql-sdl file must not contain NUL");
    }
    String logical = graphQlFile.trim().replace('\\', '/');
    if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
      throw new VirtualSiteException("graphql-sdl file must be relative to the site root");
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
          throw new VirtualSiteException(
              "graphql-sdl file must not contain '..' or NUL segments");
        }
        relative = relative.resolve(seg);
      }
      if (relative.getNameCount() == 0 || relative.toString().isEmpty()) {
        throw new VirtualSiteException("graphql-sdl file is empty after normalize");
      }
      resolved = safeRoot.resolve(relative).normalize();
    } catch (InvalidPathException e) {
      throw new VirtualSiteException("graphql-sdl file is not a valid path", e);
    }
    if (!resolved.startsWith(safeRoot)
        || !PSVirtualSiteHelper.isSafeRootPath(resolved)
        || remainingParent(resolved)) {
      throw new VirtualSiteException("graphql-sdl file escapes the site root");
    }
    return resolved;
  }

  static List<LoadedOp> parseOperations(String text) throws VirtualSiteException {
    if (text == null || text.isBlank()) {
      throw new VirtualSiteException("graphql-sdl fixture is empty");
    }
    Matcher importMatch = IMPORT_URL.matcher(text);
    if (importMatch.find()) {
      throw new VirtualSiteException(
          "graphql-sdl remote #import is not supported (local schema.graphql fixture only; no live"
              + " HTTP fetch).");
    }
    String stripped = stripCommentsAndStrings(text);
    if (!looksLikeSdl(stripped)) {
      throw new VirtualSiteException(
          "graphql-sdl fixture must declare GraphQL SDL (type/schema/interface/enum/scalar/input/"
              + "union); live GraphQL HTTP and introspection are not supported");
    }
    List<LoadedOp> ops = new ArrayList<>();
    int order = 0;
    int i = 0;
    while (i < stripped.length()) {
      i = skipWs(stripped, i);
      if (i >= stripped.length()) {
        break;
      }
      TypeHeader header = readTypeHeader(stripped, i);
      if (header == null) {
        i++;
        continue;
      }
      i = header.bodyStart();
      if (i >= stripped.length() || stripped.charAt(i) != '{') {
        i = header.next();
        continue;
      }
      int bodyEnd = matchingBrace(stripped, i);
      if (bodyEnd < 0) {
        throw new VirtualSiteException("graphql-sdl type body is not closed");
      }
      if (ROOT_TYPES.contains(header.typeName().toLowerCase(Locale.ROOT))) {
        String body = stripped.substring(i + 1, bodyEnd);
        List<FieldOp> fields = parseFields(body);
        for (FieldOp field : fields) {
          order++;
          ops.add(
              new LoadedOp(
                  header.typeName(), field.name(), field.args(), field.type(), order));
        }
      }
      i = bodyEnd + 1;
    }
    return ops;
  }

  static String schemaTitle(String text) {
    String desc = leadingDescription(text);
    if (desc != null && !desc.isBlank()) {
      String first = desc.strip().split("\\R", 2)[0].trim();
      if (!first.isEmpty()) {
        return first.length() > 80 ? first.substring(0, 80) : first;
      }
    }
    return "GraphQL Schema";
  }

  static String schemaDescription(String text) {
    String desc = leadingDescription(text);
    return desc != null ? desc.strip() : "";
  }

  private static List<LoadedRow> toPages(
      List<LoadedOp> ops, GraphQlFetch fetch, VirtualSiteConfig config)
      throws VirtualSiteException {
    if (ops.isEmpty()) {
      VersionSpec version = resolveVersion(config, "graphql-sdl fixture");
      String title = schemaTitle(fetch.text());
      String id = "graphql";
      Path relative = resolvePagePath("", id, version.path(), "graphql-sdl fixture");
      VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, 1, title);
      VirtualFrontmatter fm =
          new VirtualFrontmatter(id, title, "", version.id(), true, 1, List.of(), false);
      String body = assembleInfoBody(title, schemaDescription(fetch.text()), fetch.text());
      return List.of(new LoadedRow(ref, fm, body, fetch.sourcePath()));
    }
    List<LoadedRow> pages = new ArrayList<>(ops.size());
    for (LoadedOp op : ops) {
      pages.add(toLoadedRow(op, config, fetch.sourcePath()));
    }
    return pages;
  }

  private static LoadedRow toLoadedRow(LoadedOp op, VirtualSiteConfig config, Path sourcePath)
      throws VirtualSiteException {
    String where = "graphql-sdl operation[" + (op.order() - 1) + "]";
    String id = idForOp(op);
    String title = titleForOp(op);
    VersionSpec version = resolveVersion(config, where);
    Path relative = resolvePagePath("", slugForPath(id), version.path(), where);
    VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, op.order(), title);
    VirtualFrontmatter fm =
        new VirtualFrontmatter(id, title, "", version.id(), true, op.order(), List.of(), false);
    return new LoadedRow(ref, fm, assembleOpBody(op), sourcePath);
  }

  static String idForOp(LoadedOp op) {
    String slug = slugForPath(op.fieldName());
    return slug + "-" + op.order();
  }

  static String titleForOp(LoadedOp op) {
    return op.rootType() + "." + op.fieldName();
  }

  static String assembleOpBody(LoadedOp op) {
    StringBuilder body = new StringBuilder();
    body.append("## ").append(titleForOp(op)).append("\n\n");
    body.append("Root: ").append(op.rootType()).append('\n');
    body.append("Field: ").append(op.fieldName()).append('\n');
    if (op.args() != null && !op.args().isBlank()) {
      body.append("Args: ").append(op.args()).append('\n');
    }
    if (op.returnType() != null && !op.returnType().isBlank()) {
      body.append("Type: ").append(op.returnType()).append('\n');
    }
    return body.toString();
  }

  static String assembleInfoBody(String title, String description, String raw) {
    StringBuilder body = new StringBuilder();
    body.append("## ").append(title).append("\n\n");
    if (description != null && !description.isBlank()) {
      body.append(description).append("\n\n");
    }
    body.append("```\n").append(raw.strip()).append("\n```\n");
    return body.toString();
  }

  static String slugForPath(String id) {
    if (id == null || id.isBlank()) {
      return "graphql";
    }
    String candidate = id.trim();
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
    return slug.isEmpty() ? "graphql" : slug;
  }

  private static VersionSpec resolveVersion(VirtualSiteConfig config, String where)
      throws VirtualSiteException {
    List<VersionSpec> versions = config.versions();
    if (versions == null || versions.isEmpty()) {
      throw new VirtualSiteException(
          "graphql-sdl config must declare at least one version (" + where + ")");
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
        throw new VirtualSiteException("graphql-sdl 'path' must not contain NUL in " + where);
      }
      String logical = pathRaw.trim().replace('\\', '/');
      if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
        throw new VirtualSiteException("graphql-sdl 'path' must be relative in " + where);
      }
      segments = new ArrayList<>();
      for (String seg : logical.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0) {
          throw new VirtualSiteException(
              "graphql-sdl 'path' must not contain '..' or NUL segments in " + where);
        }
        segments.add(seg);
      }
      if (segments.isEmpty()) {
        throw new VirtualSiteException("graphql-sdl 'path' is empty after normalize in " + where);
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
      throw new VirtualSiteException("graphql-sdl 'path' is not a safe relative path in " + where);
    }
    return normalized;
  }

  private static final Pattern SDL_KEYWORD =
      Pattern.compile(
          "(?is)\\b(type|interface|enum|scalar|input|union)\\s+[A-Za-z_][A-Za-z0-9_]*|\\bschema\\s*\\{");

  private static boolean looksLikeSdl(String stripped) {
    return stripped != null && SDL_KEYWORD.matcher(stripped).find();
  }

  private static String stripCommentsAndStrings(String text) {
    StringBuilder out = new StringBuilder(text.length());
    int i = 0;
    while (i < text.length()) {
      char c = text.charAt(i);
      if (c == '#' && !inIdent(text, i)) {
        i = skipLine(text, i);
        out.append('\n');
        continue;
      }
      if (c == '"' && i + 2 < text.length() && text.charAt(i + 1) == '"' && text.charAt(i + 2) == '"') {
        i = skipBlockString(text, i + 3);
        out.append(' ');
        continue;
      }
      if (c == '"') {
        i = skipString(text, i + 1);
        out.append(' ');
        continue;
      }
      out.append(c);
      i++;
    }
    return out.toString();
  }

  private static boolean inIdent(String text, int i) {
    return i > 0 && Character.isLetterOrDigit(text.charAt(i - 1));
  }

  private static int skipLine(String text, int i) {
    while (i < text.length() && text.charAt(i) != '\n') {
      i++;
    }
    return i < text.length() ? i + 1 : i;
  }

  private static int skipBlockString(String text, int i) {
    while (i + 2 < text.length()) {
      if (text.charAt(i) == '"' && text.charAt(i + 1) == '"' && text.charAt(i + 2) == '"') {
        return i + 3;
      }
      i++;
    }
    return text.length();
  }

  private static int skipString(String text, int i) {
    while (i < text.length()) {
      char c = text.charAt(i);
      if (c == '\\' && i + 1 < text.length()) {
        i += 2;
        continue;
      }
      if (c == '"') {
        return i + 1;
      }
      i++;
    }
    return text.length();
  }

  private static String leadingDescription(String text) {
    int i = 0;
    while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
      i++;
    }
    if (i + 2 < text.length()
        && text.charAt(i) == '"'
        && text.charAt(i + 1) == '"'
        && text.charAt(i + 2) == '"') {
      int start = i + 3;
      int end = skipBlockString(text, start);
      if (end >= start + 3) {
        return text.substring(start, end - 3);
      }
    }
    return "";
  }

  private static int skipWs(String text, int i) {
    while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
      i++;
    }
    return i;
  }

  private static TypeHeader readTypeHeader(String text, int i) {
    int start = i;
    if (startsWithWord(text, i, "extend")) {
      i = skipWs(text, i + "extend".length());
    }
    if (!startsWithWord(text, i, "type")) {
      return null;
    }
    i = skipWs(text, i + "type".length());
    int nameStart = i;
    while (i < text.length() && isIdentChar(text.charAt(i))) {
      i++;
    }
    if (i == nameStart) {
      return null;
    }
    String name = text.substring(nameStart, i);
    i = skipWs(text, i);
    while (i < text.length() && text.charAt(i) != '{' && text.charAt(i) != '\n') {
      if (text.charAt(i) == '@') {
        i = skipDirective(text, i);
        i = skipWs(text, i);
        continue;
      }
      i++;
    }
    i = skipWs(text, i);
    return new TypeHeader(name, i, Math.max(i, start + 1));
  }

  private static boolean startsWithWord(String text, int i, String word) {
    if (i + word.length() > text.length()) {
      return false;
    }
    if (!text.regionMatches(true, i, word, 0, word.length())) {
      return false;
    }
    int after = i + word.length();
    return after >= text.length() || !isIdentChar(text.charAt(after));
  }

  private static boolean isIdentChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_';
  }

  private static int skipDirective(String text, int i) {
    i++;
    while (i < text.length() && isIdentChar(text.charAt(i))) {
      i++;
    }
    i = skipWs(text, i);
    if (i < text.length() && text.charAt(i) == '(') {
      i = matchingParen(text, i);
      if (i >= 0) {
        return i + 1;
      }
      return text.length();
    }
    return i;
  }

  private static int matchingBrace(String text, int open) {
    int depth = 0;
    for (int i = open; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return -1;
  }

  private static int matchingParen(String text, int open) {
    int depth = 0;
    for (int i = open; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return -1;
  }

  private static List<FieldOp> parseFields(String body) {
    List<FieldOp> fields = new ArrayList<>();
    int i = 0;
    while (i < body.length()) {
      i = skipWs(body, i);
      if (i >= body.length()) {
        break;
      }
      if (body.charAt(i) == '}') {
        break;
      }
      if (body.charAt(i) == '@') {
        i = skipDirective(body, i);
        continue;
      }
      int nameStart = i;
      while (i < body.length() && isIdentChar(body.charAt(i))) {
        i++;
      }
      if (i == nameStart) {
        i++;
        continue;
      }
      String name = body.substring(nameStart, i);
      if ("type".equalsIgnoreCase(name)
          || "extend".equalsIgnoreCase(name)
          || "schema".equalsIgnoreCase(name)
          || "interface".equalsIgnoreCase(name)
          || "enum".equalsIgnoreCase(name)
          || "input".equalsIgnoreCase(name)
          || "union".equalsIgnoreCase(name)
          || "scalar".equalsIgnoreCase(name)
          || "directive".equalsIgnoreCase(name)) {
        continue;
      }
      i = skipWs(body, i);
      String args = "";
      if (i < body.length() && body.charAt(i) == '(') {
        int close = matchingParen(body, i);
        if (close < 0) {
          break;
        }
        args = body.substring(i, close + 1).trim();
        i = close + 1;
      }
      i = skipWs(body, i);
      String type = "";
      if (i < body.length() && body.charAt(i) == ':') {
        i = skipWs(body, i + 1);
        int typeStart = i;
        while (i < body.length()) {
          char c = body.charAt(i);
          if (Character.isWhitespace(c) || c == '@' || c == '{' || c == '}') {
            break;
          }
          i++;
        }
        type = body.substring(typeStart, i).trim();
      }
      if (!name.isBlank() && !type.isBlank()) {
        fields.add(new FieldOp(name, args, type));
      }
    }
    return fields;
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

  private record GraphQlFetch(String text, Path sourcePath) {}

  private record TypeHeader(String typeName, int bodyStart, int next) {}

  private record FieldOp(String name, String args, String type) {}

  record LoadedOp(String rootType, String fieldName, String args, String returnType, int order) {}

  private record LoadedRow(
      VirtualItemRef ref, VirtualFrontmatter frontmatter, String body, Path sourcePath) {}
}
