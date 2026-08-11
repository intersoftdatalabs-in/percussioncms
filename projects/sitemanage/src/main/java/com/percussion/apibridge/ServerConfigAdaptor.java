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
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.util.IOTools;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;

/** Server configuration catalog (SY-02 read) over PSConfigurationTypes + system service. */
@PSSiteManageBean
@Lazy
public class ServerConfigAdaptor implements IServerConfigAdaptor {

  private static final Logger log = LogManager.getLogger(ServerConfigAdaptor.class);

  private static final List<String> DESIGN_GAPS =
      List.of(
          "Configuration create / update / save not supported via this API",
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

  public ServerConfigAdaptor() {
    this(PSSystemServiceLocator.getSystemService());
  }

  /** Package-visible for tests. */
  ServerConfigAdaptor(IPSSystemService systemService) {
    this.systemService = systemService;
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
    if (!isSafeConfigKey(name)) {
      return null;
    }
    PSConfigurationTypes type;
    try {
      type = PSConfigurationTypes.valueOf(name.trim());
    } catch (IllegalArgumentException e) {
      return null;
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

  static boolean isSafeConfigKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    // Enum names are simple identifiers
    return key.matches("[A-Za-z0-9_]+");
  }
}
