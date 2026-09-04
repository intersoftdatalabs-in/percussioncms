/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

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
import java.util.function.BooleanSupplier;
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

  private static final List<String> DESIGN_GAPS =
      List.of(
          "Configuration create is not supported via this API (fixed allow-listed set only)",
          "Locking and concurrent edit are not exposed on this Developer surface");

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

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public ServerConfigAdaptor() {
    this(PSSystemServiceLocator.getSystemService(), null);
  }

  /** Package-visible for tests. */
  ServerConfigAdaptor(IPSSystemService systemService) {
    this(systemService, null);
  }

  /** Package-visible for tests with an explicit Admin gate. */
  ServerConfigAdaptor(IPSSystemService systemService, BooleanSupplier adminChecker) {
    this.systemService = systemService;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
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
    requireAdmin();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    if (body.getContent() == null) {
      throw new IllegalArgumentException("content is required");
    }
    PSConfigurationTypes type = resolveAllowListedType(name);
    if (type == null) {
      return null;
    }

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
}
