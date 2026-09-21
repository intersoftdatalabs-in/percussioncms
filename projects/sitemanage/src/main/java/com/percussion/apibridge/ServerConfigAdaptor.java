/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

import com.percussion.design.objectstore.PSLockedException;
import com.percussion.design.objectstore.server.IPSLockerId;
import com.percussion.design.objectstore.server.PSServerXmlObjectStore;
import com.percussion.design.objectstore.server.PSXmlObjectStoreLockerId;
import com.percussion.rest.ObjectLockSummary;
import com.percussion.rest.serverconfigs.IServerConfigAdaptor;
import com.percussion.rest.serverconfigs.ServerConfigSummary;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.system.PSSystemServiceLocator;
import com.percussion.services.system.data.PSConfigurationTypes;
import com.percussion.services.system.data.PSMimeContentAdapter;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.util.IOTools;
import com.percussion.utils.request.PSRequestInfo;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * Server configuration catalog (SY-02) over {@link PSConfigurationTypes} + {@link
 * IPSSystemService}. Admin PUT updates allow-listed enum names only — no arbitrary filesystem
 * write.
 */
@PSSiteManageBean
@Lazy
public class ServerConfigAdaptor implements IServerConfigAdaptor {

  private static final Logger log = LogManager.getLogger(ServerConfigAdaptor.class);

  static final String ADMIN_REQUIRED =
      "Admin role required to update server configuration files";

  static final String LOCK_REQUIRED = "Design lock required, or locked by another user";

  static final String LOCK_HELD_BY_OTHER = "Could not acquire design lock; locked by another user";

  static final String SESSION_REQUIRED = "session and user are required for design lock";

  private static final List<String> DESIGN_GAPS =
      List.of("Configuration create is not supported via this API (fixed allow-listed set only)");

  private static final Map<PSConfigurationTypes, String> DISPLAY =
      new EnumMap<>(PSConfigurationTypes.class);

  static {
    DISPLAY.put(PSConfigurationTypes.SERVER_PAGE_TAGS, "Server page tags");
    DISPLAY.put(PSConfigurationTypes.TIDY_CONFIG, "Tidy properties");
    DISPLAY.put(PSConfigurationTypes.LOG_CONFIG, "Logging configuration");
    DISPLAY.put(PSConfigurationTypes.NAV_CONFIG, "Navigation properties");
    DISPLAY.put(PSConfigurationTypes.WF_CONFIG, "Workflow properties");
    DISPLAY.put(PSConfigurationTypes.THUMBNAIL_CONFIG, "Thumbnail URL properties");
    DISPLAY.put(PSConfigurationTypes.SYSTEM_VELOCITY_MACROS, "System Velocity macros");
    DISPLAY.put(PSConfigurationTypes.USER_VELOCITY_MACROS, "User Velocity macros");
    DISPLAY.put(PSConfigurationTypes.AUTH_TYPES, "Auth types");
  }

  private final IPSSystemService systemService;
  private final BooleanSupplier adminChecker;
  private final ServerConfigDesignLockStore lockStore;
  private final Supplier<String> sessionSupplier;
  private final Supplier<String> userSupplier;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public ServerConfigAdaptor() {
    this(
        PSSystemServiceLocator.getSystemService(),
        null,
        new ObjectStoreServerConfigDesignLockStore(),
        ServerConfigAdaptor::currentSession,
        ServerConfigAdaptor::currentUser);
  }

  /** Package-visible for tests. */
  ServerConfigAdaptor(IPSSystemService systemService) {
    this(systemService, null);
  }

  /** Package-visible for tests with an explicit Admin gate. */
  ServerConfigAdaptor(IPSSystemService systemService, BooleanSupplier adminChecker) {
    this(
        systemService,
        adminChecker,
        new InMemoryServerConfigDesignLockStore(),
        () -> "s1",
        () -> "Admin");
  }

  /** Package-visible for tests with an explicit lock store and identity. */
  ServerConfigAdaptor(
      IPSSystemService systemService,
      BooleanSupplier adminChecker,
      ServerConfigDesignLockStore lockStore,
      Supplier<String> sessionSupplier,
      Supplier<String> userSupplier) {
    this.systemService = systemService;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
    this.lockStore = lockStore != null ? lockStore : new InMemoryServerConfigDesignLockStore();
    this.sessionSupplier = sessionSupplier != null ? sessionSupplier : () -> "";
    this.userSupplier = userSupplier != null ? userSupplier : () -> "";
  }

  @Override
  public List<ServerConfigSummary> listConfigs() {
    List<ServerConfigSummary> out = new ArrayList<>();
    for (PSConfigurationTypes type : PSConfigurationTypes.values()) {
      out.add(toSummary(type, false));
    }
    return out;
  }

  @Override
  public ServerConfigSummary findConfigByName(String name) {
    PSConfigurationTypes type = resolveAllowListedType(name);
    if (type == null) {
      return null;
    }
    return toSummary(type, true);
  }

  @Override
  public ServerConfigSummary updateConfig(String name, ServerConfigSummary body) {
    // Validate body before Admin so missing content is 400 (not 403) per REST contract.
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    if (body.getContent() == null) {
      throw new IllegalArgumentException("content is required");
    }
    requireAdmin();
    PSConfigurationTypes type = resolveAllowListedType(name);
    if (type == null) {
      return null;
    }
    requireHeldLock(type);

    // saveConfiguration resolves the on-disk path solely from the enum name — never from
    // client-supplied file paths.
    PSMimeContentAdapter config = new PSMimeContentAdapter();
    config.setName(type.name());
    byte[] bytes = body.getContent().getBytes(StandardCharsets.UTF_8);
    config.setContent(new ByteArrayInputStream(bytes));
    config.setContentLength(bytes.length);

    try {
      systemService.saveConfiguration(config);
    } catch (IOException e) {
      log.error("Failed to save configuration {}: {}", type.name(), e.getMessage());
      throw new WebApplicationException(
          "Failed to save configuration: " + e.getMessage(),
          e,
          Response.Status.INTERNAL_SERVER_ERROR);
    }

    return toSummary(type, true);
  }

  @Override
  public ObjectLockSummary lockConfig(String name) {
    requireAdmin();
    requireSessionUser();
    PSConfigurationTypes type = resolveAllowListedType(name);
    if (type == null) {
      return null;
    }
    String lockName = designLockName(type);
    try {
      lockStore.acquire(lockName, userName(), sessionId(), 30);
      return currentLockSummary(type);
    } catch (PSLockedException e) {
      throw new WebApplicationException(LOCK_HELD_BY_OTHER, Response.Status.CONFLICT);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to lock server config {}", type.name(), e);
      throw new IllegalStateException("Failed to lock server configuration", e);
    }
  }

  @Override
  public Boolean unlockConfig(String name) {
    requireAdmin();
    requireSessionUser();
    PSConfigurationTypes type = resolveAllowListedType(name);
    if (type == null) {
      return null;
    }
    String lockName = designLockName(type);
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
      log.error("Failed to unlock server config {}", type.name(), e);
      throw new IllegalStateException("Failed to unlock server configuration", e);
    }
  }

  private ServerConfigSummary toSummary(PSConfigurationTypes type, boolean loadContent) {
    ServerConfigSummary s = new ServerConfigSummary();
    s.setName(type.name());
    s.setDisplayName(DISPLAY.getOrDefault(type, type.name()));
    s.setFileName(type.getFileName());
    s.setDescription(type.getDescription());
    s.setTypeId(type.getId());
    // REST-GAPS-02: identical static gaps only on detail, not every list row (NON_NULL omits null).
    if (loadContent) {
      s.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
      loadContentInto(s, type);
    } else {
      s.setDesignGaps(null);
    }
    return s;
  }

  private void loadContentInto(ServerConfigSummary s, PSConfigurationTypes type) {
    try {
      PSMimeContentAdapter content = systemService.loadConfiguration(type);
      if (content == null) {
        return;
      }
      s.setMimeType(content.getMimeType());
      s.setCharacterEncoding(content.getCharacterEncoding());
      Long len = content.getContentLength();
      if (len != null && len >= 0) {
        s.setContentLength(len);
      }
      InputStream in = content.getContent();
      if (in != null) {
        s.setContent(IOTools.getContent(in));
      }
    } catch (IOException e) {
      log.warn("Failed to load configuration content for {}: {}", type.name(), e.getMessage());
      log.debug("Configuration content I/O failure for {}", type.name(), e);
      // Still return meta; SPA can show gaps/error for empty content
    }
  }

  /**
   * Resolve an allow-listed {@link PSConfigurationTypes} key. Rejects blank, path traversal, and
   * unknown enum names — never opens an arbitrary filesystem path.
   */
  private static PSConfigurationTypes resolveAllowListedType(String name) {
    if (!isSafeConfigKey(name)) {
      return null;
    }
    try {
      return PSConfigurationTypes.valueOf(name.trim());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  static boolean isSafeConfigKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    // Enum names are simple identifiers — reject separators / traversal
    return key.matches("[A-Za-z0-9_]+");
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

  static String designLockName(PSConfigurationTypes type) {
    return "serverconfig-" + type.name();
  }

  private void requireHeldLock(PSConfigurationTypes type) {
    requireSessionUser();
    String lockName = designLockName(type);
    try {
      if (lockStore.heldByOther(lockName, userName(), sessionId())
          || !lockStore.heldBy(lockName, userName(), sessionId())) {
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

  private ObjectLockSummary currentLockSummary(PSConfigurationTypes type) {
    try {
      Properties info = lockStore.info(designLockName(type), userName(), sessionId());
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

  interface ServerConfigDesignLockStore {
    void acquire(String lockName, String user, String session, int minutes) throws Exception;

    void release(String lockName, String user, String session) throws Exception;

    boolean heldBy(String lockName, String user, String session) throws Exception;

    boolean heldByOther(String lockName, String user, String session) throws Exception;

    Properties info(String lockName, String user, String session);
  }

  private record HeldLock(String user, String session) {}

  static final class InMemoryServerConfigDesignLockStore implements ServerConfigDesignLockStore {
    private final Map<String, HeldLock> held = new ConcurrentHashMap<>();

    @Override
    public void acquire(String lockName, String user, String session, int minutes)
        throws Exception {
      HeldLock existing = held.get(lockName);
      if (existing != null
          && (!existing.user().equals(user) || !existing.session().equals(session))) {
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

  private static final class ObjectStoreServerConfigDesignLockStore
      implements ServerConfigDesignLockStore {
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
