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

import com.percussion.design.objectstore.PSLockedException;
import com.percussion.design.objectstore.server.IPSLockerId;
import com.percussion.design.objectstore.server.PSApplicationSummary;
import com.percussion.design.objectstore.server.PSServerXmlObjectStore;
import com.percussion.design.objectstore.server.PSXmlObjectStoreLockerId;
import com.percussion.error.PSNotLockedException;
import com.percussion.error.PSNotFoundException;
import com.percussion.rest.ObjectLockSummary;
import com.percussion.rest.applicationfiles.ApplicationFileSummary;
import com.percussion.rest.applicationfiles.IApplicationFileAdaptor;
import com.percussion.security.PSAuthorizationException;
import com.percussion.security.PSSecurityToken;
import com.percussion.security.io.PSPathInjectionGuard;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.request.PSRequestInfo;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
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

  static final String INVALID_PATH = "Invalid path";

  static final String TARGET_EXISTS = "Destination already exists";

  static final String SOURCE_IS_DESTINATION = "fromPath and toPath must differ";

  static final String NESTED_MOVE = "Cannot move a folder into itself";

  static final String LOCK_REQUIRED = "Design lock required, or locked by another user";

  static final String LOCK_HELD_BY_OTHER = "Could not acquire design lock; locked by another user";

  static final String SESSION_REQUIRED = "session and user are required for design lock";

  private static final List<String> DESIGN_GAPS =
      List.of(
          "Admin PUT may create a new file when the relative path does not yet exist under the application root",
          "Distinct from /serverconfigs (SY-02 fixed server configuration allow-list)");

  private final Function<PSSecurityToken, PSApplicationSummary[]> summaryLoader;
  private final ApplicationFileStore fileStore;
  private final BooleanSupplier adminChecker;
  private final Supplier<PSSecurityToken> tokenSupplier;
  private final ApplicationDesignLockStore lockStore;
  private final Supplier<String> sessionSupplier;
  private final Supplier<String> userSupplier;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public ApplicationFileAdaptor() {
    this(
        tok -> PSServerXmlObjectStore.getInstance().getApplicationSummaryObjects(tok, false),
        new ObjectStoreApplicationFileStore(),
        null,
        ApplicationFileAdaptor::tokenFromCurrentRequest,
        new ObjectStoreApplicationDesignLockStore(),
        ApplicationFileAdaptor::currentSession,
        ApplicationFileAdaptor::currentUser);
  }

  /** Package-visible for tests (in-memory design lock). */
  ApplicationFileAdaptor(
      Function<PSSecurityToken, PSApplicationSummary[]> summaryLoader,
      ApplicationFileStore fileStore,
      BooleanSupplier adminChecker,
      Supplier<PSSecurityToken> tokenSupplier) {
    this(
        summaryLoader,
        fileStore,
        adminChecker,
        tokenSupplier,
        new InMemoryApplicationDesignLockStore(),
        () -> "test-session",
        () -> "Admin");
  }

  /** Package-visible for tests. */
  ApplicationFileAdaptor(
      Function<PSSecurityToken, PSApplicationSummary[]> summaryLoader,
      ApplicationFileStore fileStore,
      BooleanSupplier adminChecker,
      Supplier<PSSecurityToken> tokenSupplier,
      ApplicationDesignLockStore lockStore,
      Supplier<String> sessionSupplier,
      Supplier<String> userSupplier) {
    this.summaryLoader = summaryLoader;
    this.fileStore = fileStore;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
    this.tokenSupplier =
        tokenSupplier != null ? tokenSupplier : ApplicationFileAdaptor::tokenFromCurrentRequest;
    this.lockStore = lockStore != null ? lockStore : new InMemoryApplicationDesignLockStore();
    this.sessionSupplier =
        sessionSupplier != null ? sessionSupplier : ApplicationFileAdaptor::currentSession;
    this.userSupplier = userSupplier != null ? userSupplier : ApplicationFileAdaptor::currentUser;
  }

  @Override
  public List<ApplicationFileSummary> listFiles(String appName) {
    ResolvedApp resolved = resolveApp(appName);
    if (resolved == null) {
      return null;
    }
    try {
      List<ApplicationFileSummary> out = new ArrayList<>();
      Iterator<File> files = fileStore.listFiles(resolved.trustedName(), resolved.appRoot());
      while (files != null && files.hasNext()) {
        File f = files.next();
        if (f == null) {
          continue;
        }
        String rel = toApiRelativePath(f.getPath());
        if (rel == null || normalizeSafeRelativePath(rel) == null) {
          continue;
        }
        // Resolve directory flag against the app root, not the JVM working dir: the store yields
        // relative Files, on which File.isDirectory() resolves against the process CWD and
        // misreports every directory as a file (renders a clickable row that 404s on open).
        boolean isDir = fileStore.isDirectory(resolved.trustedName(), resolved.appRoot(), f);
        out.add(toListSummary(resolved.trustedName(), rel, isDir));
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
      byte[] bytes = in.readAllBytes();
      return toDetail(resolved.trustedName(), safePath, bytes, currentLockSummary(resolved, safePath));
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
  public byte[] getFileBytes(String appName, String relativePath) {
    String safePath = requireSafeRelativePath(relativePath);
    ResolvedApp resolved = resolveApp(appName);
    if (resolved == null) {
      return null;
    }
    PSSecurityToken tok = currentToken();
    try (InputStream in = fileStore.read(resolved.trustedName(), new File(toOsRelativePath(safePath)), tok)) {
      if (in == null) {
        return null;
      }
      return in.readAllBytes();
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
      log.warn("Failed to read application file bytes {}:{}", resolved.trustedName(), safePath, e);
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
    requireHeldLock(resolved, safePath);
    PSSecurityToken tok = currentToken();
    byte[] bytes = body.getContent().getBytes(StandardCharsets.UTF_8);
    try (InputStream in = new ByteArrayInputStream(bytes)) {
      fileStore.write(
          resolved.trustedName(),
          new File(toOsRelativePath(safePath)),
          in,
          true,
          tok,
          currentLockerId());
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
    } catch (PSNotLockedException | PSLockedException e) {
      throw new WebApplicationException(LOCK_REQUIRED, Response.Status.CONFLICT);
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
    return toDetail(
        resolved.trustedName(), safePath, body.getContent(), currentLockSummary(resolved, safePath));
  }

  @Override
  public ApplicationFileSummary putFileBytes(
      String appName, String relativePath, byte[] bytes) {
    requireAdmin();
    if (bytes == null) {
      throw new IllegalArgumentException("body is required");
    }
    String safePath = requireSafeRelativePath(relativePath);
    ResolvedApp resolved = resolveApp(appName);
    if (resolved == null) {
      return null;
    }
    requireHeldLock(resolved, safePath);
    PSSecurityToken tok = currentToken();
    try (InputStream in = new ByteArrayInputStream(bytes)) {
      fileStore.write(
          resolved.trustedName(),
          new File(toOsRelativePath(safePath)),
          in,
          true,
          tok,
          currentLockerId());
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
    } catch (PSNotLockedException | PSLockedException e) {
      throw new WebApplicationException(LOCK_REQUIRED, Response.Status.CONFLICT);
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
    return toDetail(
        resolved.trustedName(), safePath, bytes, currentLockSummary(resolved, safePath));
  }

  @Override
  public ObjectLockSummary lockFile(String appName, String relativePath) {
    requireAdmin();
    requireSessionUser();
    ResolvedApp resolved = resolveApp(appName);
    if (resolved == null) {
      return null;
    }
    String safePath = normalizeSafeRelativePath(relativePath);
    if (safePath == null) {
      return null;
    }
    String lockName = designLockName(resolved.trustedName(), safePath);
    try {
      lockStore.acquire(lockName, userName(), sessionId(), 30);
      return currentLockSummary(resolved, safePath);
    } catch (PSLockedException e) {
      throw new WebApplicationException(LOCK_HELD_BY_OTHER, Response.Status.CONFLICT);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to lock application file {}:{}", resolved.trustedName(), safePath, e);
      throw new IllegalStateException("Failed to lock application file", e);
    }
  }

  @Override
  public Boolean unlockFile(String appName, String relativePath) {
    requireAdmin();
    requireSessionUser();
    ResolvedApp resolved = resolveApp(appName);
    if (resolved == null) {
      return null;
    }
    String safePath = normalizeSafeRelativePath(relativePath);
    if (safePath == null) {
      return null;
    }
    String lockName = designLockName(resolved.trustedName(), safePath);
    try {
      if (lockStore.heldByOther(lockName, userName(), sessionId())) {
        throw new WebApplicationException(LOCK_HELD_BY_OTHER, Response.Status.CONFLICT);
      }
      lockStore.release(lockName, userName(), sessionId());
      return Boolean.TRUE;
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to unlock application file {}:{}", resolved.trustedName(), safePath, e);
      throw new IllegalStateException("Failed to unlock application file", e);
    }
  }

  @Override
  public ApplicationFileSummary createFolder(String appName, String relativePath) {
    requireAdmin();
    String safePath = requireSafeRelativePath(relativePath);
    ResolvedApp resolved = resolveApp(appName);
    if (resolved == null) {
      return null;
    }
    PSSecurityToken tok = currentToken();
    File rel = new File(toOsRelativePath(safePath));
    try {
      if (fileStore.exists(resolved.appRoot(), rel)
          && !fileStore.isDirectory(resolved.trustedName(), resolved.appRoot(), rel)) {
        throw new FileAlreadyExistsException(safePath);
      }
      fileStore.mkdir(resolved.trustedName(), rel, tok);
    } catch (PSNotFoundException e) {
      log.debug(
          "Application not found for mkdir {}:{} — {}",
          resolved.trustedName(),
          safePath,
          e.toString());
      return null;
    } catch (PSAuthorizationException e) {
      throw new WebApplicationException(
          "Not authorized to create application folder", Response.Status.FORBIDDEN);
    } catch (FileAlreadyExistsException e) {
      throw new WebApplicationException(TARGET_EXISTS, Response.Status.CONFLICT);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to create application folder {}:{}: {}",
          resolved.trustedName(),
          safePath,
          e.getMessage());
      throw new WebApplicationException(
          "Failed to create application folder: " + e.getMessage(),
          e,
          Response.Status.INTERNAL_SERVER_ERROR);
    }
    return toListSummary(resolved.trustedName(), safePath, true);
  }

  @Override
  public Boolean deletePath(String appName, String relativePath) {
    requireAdmin();
    String safePath = requireSafeRelativePath(relativePath);
    ResolvedApp resolved = resolveApp(appName);
    if (resolved == null) {
      return null;
    }
    PSSecurityToken tok = currentToken();
    try {
      boolean deleted =
          fileStore.delete(
              resolved.trustedName(),
              resolved.appRoot(),
              new File(toOsRelativePath(safePath)),
              tok);
      return deleted ? Boolean.TRUE : null;
    } catch (PSNotFoundException e) {
      log.debug(
          "Application file not found for delete {}:{} — {}",
          resolved.trustedName(),
          safePath,
          e.toString());
      return null;
    } catch (PSAuthorizationException e) {
      throw new WebApplicationException(
          "Not authorized to delete application file", Response.Status.FORBIDDEN);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to delete application path {}:{}: {}",
          resolved.trustedName(),
          safePath,
          e.getMessage());
      throw new WebApplicationException(
          "Failed to delete application path: " + e.getMessage(),
          e,
          Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public ApplicationFileSummary movePath(String appName, String fromPath, String toPath) {
    requireAdmin();
    String safeFrom = requireSafeRelativePath(fromPath);
    String safeTo = requireSafeRelativePath(toPath);
    if (safeFrom.equals(safeTo)) {
      throw new IllegalArgumentException(SOURCE_IS_DESTINATION);
    }
    if (isNestedDestination(safeFrom, safeTo)) {
      throw new IllegalArgumentException(NESTED_MOVE);
    }
    ResolvedApp resolved = resolveApp(appName);
    if (resolved == null) {
      return null;
    }
    PSSecurityToken tok = currentToken();
    try {
      boolean moved =
          fileStore.rename(
              resolved.trustedName(),
              resolved.appRoot(),
              new File(toOsRelativePath(safeFrom)),
              new File(toOsRelativePath(safeTo)),
              tok);
      if (!moved) {
        return null;
      }
    } catch (PSNotFoundException e) {
      log.debug(
          "Application path not found for move {}:{} — {}",
          resolved.trustedName(),
          safeFrom,
          e.toString());
      return null;
    } catch (PSAuthorizationException e) {
      throw new WebApplicationException(
          "Not authorized to move application file", Response.Status.FORBIDDEN);
    } catch (FileAlreadyExistsException e) {
      throw new WebApplicationException(TARGET_EXISTS, Response.Status.CONFLICT);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to move application path {}:{} -> {}: {}",
          resolved.trustedName(),
          safeFrom,
          safeTo,
          e.getMessage());
      throw new WebApplicationException(
          "Failed to move application path: " + e.getMessage(),
          e,
          Response.Status.INTERNAL_SERVER_ERROR);
    }
    boolean directory = false;
    try {
      directory =
          fileStore.isDirectory(
              resolved.trustedName(),
              resolved.appRoot(),
              new File(toOsRelativePath(safeTo)));
    } catch (Exception e) {
      log.debug("Could not stat moved path {}:{}", resolved.trustedName(), safeTo, e);
    }
    return toListSummary(resolved.trustedName(), safeTo, directory);
  }

  /**
   * True when {@code toPath} is the same as {@code fromPath} or a descendant (folder moved into
   * itself).
   */
  static boolean isNestedDestination(String fromPath, String toPath) {
    if (fromPath == null || toPath == null) {
      return false;
    }
    return toPath.startsWith(fromPath + "/");
  }

  /** Unsafe relative paths are 400 for folder create/delete/move (not 404). */
  static String requireSafeRelativePath(String relativePath) {
    String safe = normalizeSafeRelativePath(relativePath);
    if (safe == null) {
      throw new IllegalArgumentException(INVALID_PATH);
    }
    return safe;
  }

  private ResolvedApp resolveApp(String appName) {
    if (StringUtils.isBlank(appName) || !isSafeApplicationName(appName.trim())) {
      return null;
    }
    PSSecurityToken tok = currentToken();
    PSApplicationSummary[] sums = summaryLoader.apply(tok);
    String trusted = resolveApplicationName(appName.trim(), sums);
    if (trusted == null) {
      return null;
    }
    String appRoot = trusted;
    if (sums != null) {
      for (PSApplicationSummary sum : sums) {
        if (sum != null && trusted.equals(sum.getName()) && StringUtils.isNotBlank(sum.getAppRoot())) {
          appRoot = sum.getAppRoot();
          break;
        }
      }
    }
    return new ResolvedApp(trusted, appRoot);
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

  private void requireSessionUser() {
    if (StringUtils.isBlank(sessionId()) || StringUtils.isBlank(userName())) {
      throw new WebApplicationException(SESSION_REQUIRED, Response.Status.CONFLICT);
    }
  }

  private String sessionId() {
    String s = sessionSupplier.get();
    return s != null ? s : "";
  }

  private String userName() {
    String u = userSupplier.get();
    return u != null ? u : "";
  }

  static String currentSession() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
  }

  static String currentUser() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
  }

  static String designLockName(String trustedApp, String apiPath) {
    return trustedApp + "-" + leafName(apiPath);
  }

  private IPSLockerId currentLockerId() {
    return new PSXmlObjectStoreLockerId(userName(), true, sessionId());
  }

  private void requireHeldLock(ResolvedApp resolved, String safePath) {
    requireSessionUser();
    String lockName = designLockName(resolved.trustedName(), safePath);
    try {
      if (lockStore.heldByOther(lockName, userName(), sessionId())) {
        throw new WebApplicationException(LOCK_REQUIRED, Response.Status.CONFLICT);
      }
      if (!lockStore.heldBy(lockName, userName(), sessionId())) {
        throw new WebApplicationException(LOCK_REQUIRED, Response.Status.CONFLICT);
      }
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(LOCK_REQUIRED, Response.Status.CONFLICT);
    }
  }

  private ObjectLockSummary currentLockSummary(ResolvedApp resolved, String safePath) {
    try {
      Properties info =
          lockStore.info(designLockName(resolved.trustedName(), safePath), userName(), sessionId());
      if (info == null || StringUtils.isBlank(info.getProperty("lockerName"))) {
        return null;
      }
      ObjectLockSummary summary = new ObjectLockSummary();
      summary.setLocker(info.getProperty("lockerName"));
      summary.setSession(info.getProperty("lockerSession"));
      summary.setRemainingTime(30L);
      return summary;
    } catch (RuntimeException e) {
      return null;
    }
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
    return toDetail(appName, apiPath, content, null);
  }

  static ApplicationFileSummary toDetail(
      String appName, String apiPath, String content, ObjectLockSummary lock) {
    ApplicationFileSummary s = toListSummary(appName, apiPath, false);
    s.setContent(content);
    s.setCharacterEncoding(StandardCharsets.UTF_8.name());
    s.setMimeType(guessMimeType(apiPath));
    if (content != null) {
      s.setContentLength((long) content.getBytes(StandardCharsets.UTF_8).length);
    }
    s.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
    s.setLock(lock);
    return s;
  }

  /**
   * Binary-aware detail. Valid UTF-8 bodies decode to text (binary stays null); everything else is
   * reported as {@code binary=true} with no {@code content} so JSON never mangles the bytes.
   */
  static ApplicationFileSummary toDetail(
      String appName, String apiPath, byte[] bytes, ObjectLockSummary lock) {
    if (isUtf8Text(bytes)) {
      return toDetail(appName, apiPath, new String(bytes, StandardCharsets.UTF_8), lock);
    }
    ApplicationFileSummary s = toListSummary(appName, apiPath, false);
    s.setBinary(true);
    s.setMimeType(guessMimeType(apiPath));
    s.setContentLength(bytes == null ? null : (long) bytes.length);
    s.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
    s.setLock(lock);
    return s;
  }

  /**
   * True when the bytes decode as UTF-8 without malformed/unmappable sequences and contain no NUL.
   * NUL anywhere is treated as binary — it is not representable in the JSON text editor surface.
   */
  static boolean isUtf8Text(byte[] bytes) {
    if (bytes == null) {
      return false;
    }
    try {
      CharsetDecoder decoder =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT);
      String text = decoder.decode(ByteBuffer.wrap(bytes)).toString();
      return text.indexOf('\0') < 0;
    } catch (CharacterCodingException e) {
      return false;
    }
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

  private record ResolvedApp(String trustedName, String appRoot) {}

  /** Object-store I/O seam for unit tests. */
  interface ApplicationFileStore {
    Iterator<File> listFiles(String trustedAppName, String appRoot) throws Exception;

    InputStream read(String trustedAppName, File relativeFile, PSSecurityToken tok)
        throws Exception;

    void write(
        String trustedAppName,
        File relativeFile,
        InputStream in,
        boolean overwrite,
        PSSecurityToken tok,
        IPSLockerId lockId)
        throws Exception;

    void mkdir(String trustedAppName, File relativeDir, PSSecurityToken tok) throws Exception;

    boolean delete(String trustedAppName, String appRoot, File relativeFile, PSSecurityToken tok)
        throws Exception;

    boolean rename(
        String trustedAppName,
        String appRoot,
        File fromFile,
        File toFile,
        PSSecurityToken tok)
        throws Exception;

    boolean isDirectory(String trustedAppName, String appRoot, File relativeFile) throws Exception;

    boolean exists(String appRoot, File relativeFile) throws Exception;
  }

  static final class ObjectStoreApplicationFileStore implements ApplicationFileStore {
    @Override
    public Iterator<File> listFiles(String trustedAppName, String appRoot) throws Exception {
      File appDir = resolveAppRootDir(appRoot);
      if (appDir == null || !appDir.isDirectory()) { // codeql[java/path-injection]
        return PSServerXmlObjectStore.getInstance().getApplicationFiles(trustedAppName);
      }
      List<File> out = new ArrayList<>();
      Path base = appDir.toPath();
      try (var walk = Files.walk(base)) { // codeql[java/path-injection]
        walk.filter(p -> !p.equals(base))
            .forEach(
                p -> {
                  Path rel = base.relativize(p);
                  if (rel.getNameCount() > 0) {
                    out.add(rel.toFile());
                  }
                });
      }
      return out.iterator();
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
        PSSecurityToken tok,
        IPSLockerId lockId)
        throws Exception {
      if (lockId == null) {
        throw new PSNotLockedException(
            com.intsof.percussioncms.auditlog.codes.ObjectStoreErrorCodes.LOCK_NOT_HELD
                .numericCode(),
            trustedAppName);
      }
      PSServerXmlObjectStore.getInstance()
          .saveApplicationFile(trustedAppName, relativeFile, in, overwrite, lockId, tok, false);
    }

    @Override
    public void mkdir(String trustedAppName, File relativeDir, PSSecurityToken tok)
        throws Exception {
      PSServerXmlObjectStore.getInstance()
          .saveApplicationFileWithoutLocking(trustedAppName, relativeDir, null, true, tok, true);
    }

    @Override
    public boolean delete(
        String trustedAppName, String appRoot, File relativeFile, PSSecurityToken tok)
        throws Exception {
      File target = resolveUnderAppRoot(appRoot, relativeFile);
      if (target == null || !target.exists()) { // codeql[java/path-injection]
        return false;
      }
      deleteRecursively(target.toPath());
      return true;
    }

    @Override
    public boolean rename(
        String trustedAppName,
        String appRoot,
        File fromFile,
        File toFile,
        PSSecurityToken tok)
        throws Exception {
      File from = resolveUnderAppRoot(appRoot, fromFile);
      File to = resolveUnderAppRoot(appRoot, toFile);
      if (from == null || !from.exists()) { // codeql[java/path-injection]
        return false;
      }
      if (to == null) {
        return false;
      }
      if (to.exists()) { // codeql[java/path-injection]
        throw new FileAlreadyExistsException(to.getPath());
      }
      Path toPath = to.toPath();
      Path parent = toPath.getParent();
      if (parent != null && !Files.exists(parent)) { // codeql[java/path-injection]
        Files.createDirectories(parent); // codeql[java/path-injection]
      }
      Files.move(from.toPath(), toPath); // codeql[java/path-injection]
      return true;
    }

    @Override
    public boolean isDirectory(String trustedAppName, String appRoot, File relativeFile)
        throws Exception {
      File target = resolveUnderAppRoot(appRoot, relativeFile);
      return target != null && target.isDirectory(); // codeql[java/path-injection]
    }

    @Override
    public boolean exists(String appRoot, File relativeFile) throws Exception {
      File target = resolveUnderAppRoot(appRoot, relativeFile);
      return target != null && target.exists(); // codeql[java/path-injection]
    }
  }

  /**
   * Resolve a relative application file under the catalog app root. Uses NIO {@link Path#resolve}
   * per segment and {@link PSPathInjectionGuard#requireUnderBase} so traversal cannot escape RxDir.
   */
  static File resolveAppRootDir(String appRoot) {
    if (StringUtils.isBlank(appRoot)) {
      return null;
    }
    File rxDir = PSServer.getRxDir();
    if (rxDir == null) {
      return null;
    }
    return PSPathInjectionGuard.requireUnderBase(rxDir, appRoot);
  }

  static File resolveUnderAppRoot(String appRoot, File relativeFile) {
    if (relativeFile == null) {
      return null;
    }
    File appDir = resolveAppRootDir(appRoot);
    if (appDir == null) {
      return null;
    }
    String rel = toApiRelativePath(relativeFile.getPath());
    if (rel == null || normalizeSafeRelativePath(rel) == null) {
      throw new IllegalArgumentException(INVALID_PATH);
    }
    Path nioRel = nioRelativePath(rel);
    return PSPathInjectionGuard.requireUnderBase(appDir, nioRel.toString());
  }

  /** Build a relative NIO path from API {@code /}-separated segments (portable). */
  static Path nioRelativePath(String apiRelativePath) {
    String[] segs = apiRelativePath.split("/");
    Path p = Path.of(segs[0]);
    for (int i = 1; i < segs.length; i++) {
      p = p.resolve(segs[i]);
    }
    return p;
  }

  static void deleteRecursively(Path target) throws Exception {
    if (target == null || !Files.exists(target)) { // codeql[java/path-injection]
      return;
    }
    try (var walk = Files.walk(target)) { // codeql[java/path-injection]
      List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
      for (Path p : paths) {
        Files.deleteIfExists(p); // codeql[java/path-injection]
      }
    }
  }

  /** Object-store design lock seam for unit tests. */
  interface ApplicationDesignLockStore {
    void acquire(String lockName, String user, String session, int minutes) throws Exception;

    void release(String lockName, String user, String session) throws Exception;

    boolean heldBy(String lockName, String user, String session) throws Exception;

    boolean heldByOther(String lockName, String user, String session) throws Exception;

    Properties info(String lockName, String user, String session);
  }

  private record HeldLock(String user, String session) {}

  /** In-memory lock map for adaptor unit tests. */
  static final class InMemoryApplicationDesignLockStore implements ApplicationDesignLockStore {
    private final Map<String, HeldLock> held = new ConcurrentHashMap<>();

    @Override
    public void acquire(String lockName, String user, String session, int minutes)
        throws Exception {
      HeldLock existing = held.get(lockName);
      if (existing != null && (!existing.user().equals(user) || !existing.session().equals(session))) {
        throw new PSLockedException(0, lockName);
      }
      held.put(lockName, new HeldLock(user, session));
    }

    @Override
    public void release(String lockName, String user, String session) {
      HeldLock existing = held.get(lockName);
      if (existing != null && existing.user().equals(user) && existing.session().equals(session)) {
        held.remove(lockName);
      }
    }

    @Override
    public boolean heldBy(String lockName, String user, String session) {
      HeldLock existing = held.get(lockName);
      return existing != null && existing.user().equals(user) && existing.session().equals(session);
    }

    @Override
    public boolean heldByOther(String lockName, String user, String session) {
      HeldLock existing = held.get(lockName);
      return existing != null
          && (!existing.user().equals(user) || !existing.session().equals(session));
    }

    @Override
    public Properties info(String lockName, String user, String session) {
      HeldLock existing = held.get(lockName);
      if (existing == null) {
        return null;
      }
      Properties p = new Properties();
      p.setProperty("lockerName", existing.user());
      p.setProperty("lockerSession", existing.session());
      return p;
    }
  }

  private static final class ObjectStoreApplicationDesignLockStore
      implements ApplicationDesignLockStore {
    @Override
    public void acquire(String lockName, String user, String session, int minutes)
        throws Exception {
      IPSLockerId id = new PSXmlObjectStoreLockerId(user, true, session);
      PSServerXmlObjectStore os = PSServerXmlObjectStore.getInstance();
      if (os.isApplicationLocked(id, lockName)) {
        os.getApplicationLock(id, lockName, minutes);
        return;
      }
      Properties info = os.getApplicationLockInfo(id, lockName);
      if (info != null && StringUtils.isNotBlank(info.getProperty("lockerName"))) {
        String locker = info.getProperty("lockerName");
        String lockerSession = info.getProperty("lockerSession");
        if (!user.equals(locker) || (lockerSession != null && !session.equals(lockerSession))) {
          throw new PSLockedException(0, lockName);
        }
      }
      os.getApplicationLock(id, lockName, minutes);
    }

    @Override
    public void release(String lockName, String user, String session) throws Exception {
      IPSLockerId id = new PSXmlObjectStoreLockerId(user, true, session);
      PSServerXmlObjectStore.getInstance().releaseApplicationLock(id, lockName);
    }

    @Override
    public boolean heldBy(String lockName, String user, String session) throws Exception {
      IPSLockerId id = new PSXmlObjectStoreLockerId(user, true, session);
      return PSServerXmlObjectStore.getInstance().isApplicationLocked(id, lockName);
    }

    @Override
    public boolean heldByOther(String lockName, String user, String session) throws Exception {
      IPSLockerId id = new PSXmlObjectStoreLockerId(user, true, session);
      PSServerXmlObjectStore os = PSServerXmlObjectStore.getInstance();
      if (os.isApplicationLocked(id, lockName)) {
        return false;
      }
      Properties info = os.getApplicationLockInfo(id, lockName);
      return info != null && StringUtils.isNotBlank(info.getProperty("lockerName"));
    }

    @Override
    public Properties info(String lockName, String user, String session) {
      IPSLockerId id = new PSXmlObjectStoreLockerId(user, true, session);
      return PSServerXmlObjectStore.getInstance().getApplicationLockInfo(id, lockName);
    }
  }
}
