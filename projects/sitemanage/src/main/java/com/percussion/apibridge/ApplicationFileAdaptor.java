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

package com.percussion.apibridge;

import com.percussion.design.objectstore.server.PSApplicationSummary;
import com.percussion.design.objectstore.server.PSServerXmlObjectStore;
import com.percussion.error.PSNotFoundException;
import com.percussion.rest.applicationfiles.ApplicationFileSummary;
import com.percussion.rest.applicationfiles.IApplicationFileAdaptor;
import com.percussion.security.PSAuthorizationException;
import com.percussion.security.PSSecurityToken;
import com.percussion.security.io.PSPathInjectionGuard;
import com.percussion.server.PSRequest;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.util.IOTools;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * SY-05 application CMS/resource files over {@link PSServerXmlObjectStore}.
 *
 * <p>Applications are allow-listed by resolving the path param against the object-store catalog
 * (trusted name only). Relative file paths are normalized and rejected on traversal / absolute
 * form before any object-store I/O. Distinct from SY-02 {@code /serverconfigs}.
 */
@PSSiteManageBean
@Lazy
public class ApplicationFileAdaptor implements IApplicationFileAdaptor {

  private static final Logger log = LogManager.getLogger(ApplicationFileAdaptor.class);

  static final String ADMIN_REQUIRED = "Admin role required to update application CMS/resource files";

  private static final List<String> DESIGN_GAPS =
      List.of(
          "Design locking / concurrent edit are not exposed on this Developer surface",
          "Binary files may not round-trip as UTF-8 text",
          "Create/delete folder and rename/move are not supported via this API",
          "Admin PUT may create a new file when the relative path does not yet exist under the application root",
          "Distinct from /serverconfigs (SY-02 fixed server configuration allow-list)");

  private final Function<PSSecurityToken, PSApplicationSummary[]> summaryLoader;
  private final ApplicationFileStore fileStore;
  private final BooleanSupplier adminChecker;
  private final Supplier<PSSecurityToken> tokenSupplier;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public ApplicationFileAdaptor() {
    this(
        tok -> PSServerXmlObjectStore.getInstance().getApplicationSummaryObjects(tok, false),
        new ObjectStoreApplicationFileStore(),
        null,
        ApplicationFileAdaptor::tokenFromCurrentRequest);
  }

  /** Package-visible for tests. */
  ApplicationFileAdaptor(
      Function<PSSecurityToken, PSApplicationSummary[]> summaryLoader,
      ApplicationFileStore fileStore,
      BooleanSupplier adminChecker,
      Supplier<PSSecurityToken> tokenSupplier) {
    this.summaryLoader = summaryLoader;
    this.fileStore = fileStore;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
    this.tokenSupplier =
        tokenSupplier != null ? tokenSupplier : ApplicationFileAdaptor::tokenFromCurrentRequest;
  }

  @Override
  public List<ApplicationFileSummary> listFiles(String appName) {
    ResolvedApp resolved = resolveApp(appName);
    if (resolved == null) {
      return null;
    }
    try {
      List<ApplicationFileSummary> out = new ArrayList<>();
      Iterator<File> files = fileStore.listFiles(resolved.trustedName());
      while (files != null && files.hasNext()) {
        File f = files.next();
        if (f == null) {
          continue;
        }
        String rel = toApiRelativePath(f.getPath());
        if (rel == null || normalizeSafeRelativePath(rel) == null) {
          continue;
        }
        out.add(toListSummary(resolved.trustedName(), rel, f.isDirectory()));
      }
      out.sort(
          Comparator.comparing(
              ApplicationFileSummary::getPath, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
      return out;
    } catch (RuntimeException e) {
      // Must precede catch (Exception): otherwise RuntimeException is wrapped as IllegalStateException.
      throw e;
    } catch (Exception e) {
      log.warn("Failed to list application files for {}", resolved.trustedName(), e);
      throw new IllegalStateException("Failed to list application files", e);
    }
  }

  @Override
  public ApplicationFileSummary getFile(String appName, String relativePath) {
    ResolvedApp resolved = resolveApp(appName);
    if (resolved == null) {
      return null;
    }
    String safePath = normalizeSafeRelativePath(relativePath);
    if (safePath == null) {
      return null;
    }
    PSSecurityToken tok = currentToken();
    try (InputStream in = fileStore.read(resolved.trustedName(), new File(toOsRelativePath(safePath)), tok)) {
      if (in == null) {
        return null;
      }
      String text = IOTools.getContent(in);
      return toDetail(resolved.trustedName(), safePath, text);
    } catch (PSNotFoundException e) {
      log.debug("Application file not found {}:{} — {}", resolved.trustedName(), safePath, e.toString());
      return null;
    } catch (PSAuthorizationException e) {
      log.debug(
          "Not authorized to read application file {}:{} — {}",
          resolved.trustedName(),
          safePath,
          e.toString());
      return null;
    } catch (RuntimeException e) {
      // Must precede catch (Exception): otherwise RuntimeException is wrapped as IllegalStateException.
      throw e;
    } catch (Exception e) {
      log.warn("Failed to read application file {}:{}", resolved.trustedName(), safePath, e);
      throw new IllegalStateException("Failed to read application file", e);
    }
  }

  @Override
  public ApplicationFileSummary putFile(
      String appName, String relativePath, ApplicationFileSummary body) {
    requireAdmin();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    if (body.getContent() == null) {
      throw new IllegalArgumentException("content is required");
    }
    ResolvedApp resolved = resolveApp(appName);
    if (resolved == null) {
      return null;
    }
    String safePath = normalizeSafeRelativePath(relativePath);
    if (safePath == null) {
      return null;
    }
    PSSecurityToken tok = currentToken();
    byte[] bytes = body.getContent().getBytes(StandardCharsets.UTF_8);
    try (InputStream in = new ByteArrayInputStream(bytes)) {
      fileStore.write(
          resolved.trustedName(), new File(toOsRelativePath(safePath)), in, true, tok);
    } catch (PSNotFoundException e) {
      log.debug(
          "Application not found for write {}:{} — {}",
          resolved.trustedName(),
          safePath,
          e.toString());
      return null;
    } catch (PSAuthorizationException e) {
      throw new WebApplicationException(
          "Not authorized to update application file", Response.Status.FORBIDDEN);
    } catch (RuntimeException e) {
      // Must precede catch (Exception): otherwise RuntimeException is remapped to HTTP 500.
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to save application file {}:{}: {}",
          resolved.trustedName(),
          safePath,
          e.getMessage());
      throw new WebApplicationException(
          "Failed to save application file: " + e.getMessage(),
          e,
          Response.Status.INTERNAL_SERVER_ERROR);
    }
    return toDetail(resolved.trustedName(), safePath, body.getContent());
  }

  private ResolvedApp resolveApp(String appName) {
    if (StringUtils.isBlank(appName) || !isSafeApplicationName(appName.trim())) {
      return null;
    }
    PSSecurityToken tok = currentToken();
    String trusted = resolveApplicationName(appName.trim(), summaryLoader.apply(tok));
    if (trusted == null) {
      return null;
    }
    return new ResolvedApp(trusted);
  }

  private PSSecurityToken currentToken() {
    PSSecurityToken tok = tokenSupplier.get();
    if (tok == null) {
      throw new IllegalStateException("No current request for application files");
    }
    return tok;
  }

  private static PSSecurityToken tokenFromCurrentRequest() {
    PSRequest req = PSSecurityFilter.getCurrentRequest();
    if (req == null) {
      return null;
    }
    return req.getSecurityToken();
  }

  private void requireAdmin() {
    boolean allowed;
    try {
      allowed = adminChecker.getAsBoolean();
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.error("Admin check failed unexpectedly", e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (!allowed) {
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
    }
  }

  boolean isCurrentUserAdmin() {
    if (userService == null) {
      return false;
    }
    try {
      PSCurrentUser current = userService.getCurrentUser();
      if (current == null || StringUtils.isBlank(current.getName())) {
        return false;
      }
      return userService.isAdminUser(current.getName());
    } catch (PSDataServiceException e) {
      log.debug("Unable to resolve current user for Admin check: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Application names become object-store directory names. Reject path traversal and separators so
   * a user-supplied name cannot escape the apps root.
   */
  static boolean isSafeApplicationName(String name) {
    if (StringUtils.isBlank(name)) {
      return false;
    }
    return !name.contains("..")
        && name.indexOf('/') < 0
        && name.indexOf('\\') < 0
        && name.indexOf('\0') < 0;
  }

  /**
   * Resolve numeric id or application name against the catalog summary list. Always returns a
   * trusted catalog name, never the raw user string.
   */
  static String resolveApplicationName(String idOrName, PSApplicationSummary[] sums) {
    if (!isSafeApplicationName(idOrName) || sums == null) {
      return null;
    }
    if (StringUtils.isNumeric(idOrName)) {
      int id = Integer.parseInt(idOrName);
      for (PSApplicationSummary sum : sums) {
        if (sum != null && sum.getId() == id) {
          String trusted = sum.getName();
          return isSafeApplicationName(trusted) ? trusted : null;
        }
      }
      return null;
    }
    for (PSApplicationSummary sum : sums) {
      if (sum != null && idOrName.equalsIgnoreCase(sum.getName())) {
        String trusted = sum.getName();
        return isSafeApplicationName(trusted) ? trusted : null;
      }
    }
    return null;
  }

  /**
   * Normalize a client-supplied relative path under an application root. Rejects blank, absolute,
   * drive-letter, NUL, empty, and parent-traversal forms. Returns a portable {@code /}-separated
   * relative path, or {@code null} when unsafe.
   *
   * <p>Segments are validated <em>before</em> {@link Path#normalize()} so inputs like {@code
   * a/../b.txt} cannot collapse into an apparently safe leaf name.
   */
  static String normalizeSafeRelativePath(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      return null;
    }
    if (relativePath.indexOf('\0') >= 0) {
      return null;
    }
    String unified = relativePath.trim().replace('\\', '/');
    if (unified.startsWith("/") || unified.startsWith("~")) {
      return null;
    }
    // Reject Windows drive / UNC style before Path resolution.
    if (unified.length() >= 2 && unified.charAt(1) == ':') {
      return null;
    }
    if (unified.startsWith("//")) {
      return null;
    }
    String[] rawSegments = unified.split("/");
    if (rawSegments.length == 0) {
      return null;
    }
    StringBuilder apiPath = new StringBuilder();
    for (int i = 0; i < rawSegments.length; i++) {
      String segment = rawSegments[i];
      // Empty segment means leading/trailing/duplicate slash — reject rather than normalize away.
      if (segment == null || segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
        return null;
      }
      try {
        PSPathInjectionGuard.requireSafeFileName(segment);
      } catch (IllegalArgumentException e) {
        return null;
      }
      if (i > 0) {
        apiPath.append('/');
      }
      apiPath.append(segment);
    }
    // Defensive: Path absolute check after rebuild (should never be absolute for relative segs).
    try {
      if (Path.of(apiPath.toString()).isAbsolute()) {
        return null;
      }
    } catch (RuntimeException e) {
      return null;
    }
    return apiPath.toString();
  }

  /** Convert API {@code /}-path to a relative File path string for object-store APIs. */
  static String toOsRelativePath(String apiRelativePath) {
    if (apiRelativePath == null) {
      return null;
    }
    return apiRelativePath.replace('/', File.separatorChar);
  }

  /** Normalize an object-store File path to API {@code /} form. */
  static String toApiRelativePath(String osPath) {
    if (osPath == null || osPath.isBlank()) {
      return null;
    }
    return osPath.replace('\\', '/');
  }

  static ApplicationFileSummary toListSummary(String appName, String apiPath, boolean directory) {
    ApplicationFileSummary s = new ApplicationFileSummary();
    s.setApplicationName(appName);
    s.setPath(apiPath);
    s.setName(leafName(apiPath));
    s.setDirectory(directory);
    s.setDesignGaps(null);
    return s;
  }

  static ApplicationFileSummary toDetail(String appName, String apiPath, String content) {
    ApplicationFileSummary s = toListSummary(appName, apiPath, false);
    s.setContent(content);
    s.setCharacterEncoding(StandardCharsets.UTF_8.name());
    s.setMimeType(guessMimeType(apiPath));
    if (content != null) {
      s.setContentLength((long) content.getBytes(StandardCharsets.UTF_8).length);
    }
    s.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
    return s;
  }

  static String leafName(String apiPath) {
    if (apiPath == null || apiPath.isEmpty()) {
      return apiPath;
    }
    int slash = apiPath.lastIndexOf('/');
    return slash >= 0 ? apiPath.substring(slash + 1) : apiPath;
  }

  static String guessMimeType(String apiPath) {
    if (apiPath == null) {
      return "text/plain";
    }
    String lower = apiPath.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".xml") || lower.endsWith(".xsl") || lower.endsWith(".xslt")) {
      return "application/xml";
    }
    if (lower.endsWith(".css")) {
      return "text/css";
    }
    if (lower.endsWith(".js")) {
      return "text/javascript";
    }
    if (lower.endsWith(".html") || lower.endsWith(".htm")) {
      return "text/html";
    }
    if (lower.endsWith(".json")) {
      return "application/json";
    }
    if (lower.endsWith(".dtd")) {
      return "application/xml-dtd";
    }
    return "text/plain";
  }

  private record ResolvedApp(String trustedName) {}

  /** Object-store I/O seam for unit tests. */
  interface ApplicationFileStore {
    Iterator<File> listFiles(String trustedAppName) throws Exception;

    InputStream read(String trustedAppName, File relativeFile, PSSecurityToken tok)
        throws Exception;

    void write(
        String trustedAppName,
        File relativeFile,
        InputStream in,
        boolean overwrite,
        PSSecurityToken tok)
        throws Exception;
  }

  private static final class ObjectStoreApplicationFileStore implements ApplicationFileStore {
    @Override
    public Iterator<File> listFiles(String trustedAppName) throws Exception {
      return PSServerXmlObjectStore.getInstance().getApplicationFiles(trustedAppName);
    }

    @Override
    public InputStream read(String trustedAppName, File relativeFile, PSSecurityToken tok)
        throws Exception {
      return PSServerXmlObjectStore.getInstance()
          .getApplicationFile(trustedAppName, relativeFile, tok);
    }

    @Override
    public void write(
        String trustedAppName,
        File relativeFile,
        InputStream in,
        boolean overwrite,
        PSSecurityToken tok)
        throws Exception {
      // Admin REST does not expose design locks (design gap) — same pattern as SY-02 config save.
      PSServerXmlObjectStore.getInstance()
          .saveApplicationFileWithoutLocking(
              trustedAppName, relativeFile, in, overwrite, tok, false);
    }
  }
}
