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

import com.percussion.rest.apikeys.ApiKeyAssignment;
import com.percussion.rest.apikeys.ApiKeyScope;
import com.percussion.rest.apikeys.ApiKeyScopeCatalog;
import com.percussion.rest.apikeys.ApiKeyScopePreset;
import com.percussion.rest.apikeys.IApiKeyScopeAdaptor;
import com.percussion.server.PSServer;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Developer API key scope registry (#6481).
 *
 * <p>There is no Workbench twin for API keys (no {@code IPS*DesignWs} owns them), so this adaptor
 * is the documented ALT system owner: assignments persist in a JSON registry file under the
 * install tree ({@code rxconfig/ApiKeyScopes/apikey-scopes.json} below the deploy dir), resolved
 * with portable NIO {@link Path} APIs. The scope catalog and least-privilege presets stay
 * product-defined in {@link ApiKeyScopeCatalog} (rest module) — the file stores only per-key
 * scope sets.
 *
 * <p>Fail-closed rules: corrupted registry reads as empty (logged); writes are atomic (temp file
 * + move); unknown scopes and unsafe key names are rejected before any write; non-Admin callers
 * get 403 on every method.
 */
@PSSiteManageBean
@Lazy
public class ApiKeyScopeAdaptor implements IApiKeyScopeAdaptor {

  private static final Logger log = LogManager.getLogger(ApiKeyScopeAdaptor.class);

  static final String ADMIN_REQUIRED_LIST = "Admin role required to browse API key scopes";
  static final String ADMIN_REQUIRED_WRITE = "Admin role required to update API key scopes";
  static final String INVALID_KEY_NAME = "Invalid API key name";
  static final String NAME_MISMATCH = "Body name must match the path key name";
  static final String UNKNOWN_SCOPE = "Unknown scope: ";

  /** Key names are registry file keys: start with a letter, then letters/digits/dash/dot. */
  private static final Pattern SAFE_KEY_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_.-]{0,63}");

  static final String REGISTRY_DIR_NAME = "ApiKeyScopes";
  static final String REGISTRY_FILE_NAME = "apikey-scopes.json";

  private static final ObjectMapper MAPPER = JsonMapper.builder().build();

  private final Supplier<Path> registryFile;
  private final BooleanSupplier adminChecker;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public ApiKeyScopeAdaptor() {
    this(ApiKeyScopeAdaptor::defaultRegistryFile, null);
  }

  /** Package-visible for tests. */
  ApiKeyScopeAdaptor(Supplier<Path> registryFile, BooleanSupplier adminChecker) {
    this.registryFile =
        registryFile != null ? registryFile : ApiKeyScopeAdaptor::defaultRegistryFile;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  @Override
  public List<ApiKeyScope> listScopes() {
    requireAdmin(ADMIN_REQUIRED_LIST);
    return ApiKeyScopeCatalog.scopes();
  }

  @Override
  public List<ApiKeyScopePreset> listPresets() {
    requireAdmin(ADMIN_REQUIRED_LIST);
    return ApiKeyScopeCatalog.presets();
  }

  @Override
  public List<ApiKeyAssignment> listKeys() {
    requireAdmin(ADMIN_REQUIRED_LIST);
    List<ApiKeyAssignment> out = new ArrayList<>();
    for (Map.Entry<String, StoredKey> e : readRegistry().entrySet()) {
      out.add(toAssignment(e.getKey(), e.getValue()));
    }
    out.sort(Comparator.comparing(ApiKeyAssignment::getName, String.CASE_INSENSITIVE_ORDER));
    return out;
  }

  @Override
  public ApiKeyAssignment getKey(String name) {
    requireAdmin(ADMIN_REQUIRED_LIST);
    String key = normalizeKeyName(name);
    StoredKey stored = readRegistry().get(key);
    return stored == null ? null : toAssignment(key, stored);
  }

  @Override
  public ApiKeyAssignment saveKey(String name, ApiKeyAssignment body) {
    requireAdmin(ADMIN_REQUIRED_WRITE);
    String key = normalizeKeyName(name);
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String bodyName = body.getName();
    if (bodyName != null && !bodyName.isBlank() && !bodyName.trim().equals(key)) {
      throw new IllegalArgumentException(NAME_MISMATCH);
    }
    List<String> scopes = canonicalScopes(body.getScopes());
    String displayName =
        body.getDisplayName() == null || body.getDisplayName().isBlank()
            ? null
            : body.getDisplayName().trim();

    Map<String, StoredKey> registry = readRegistry();
    registry.put(key, new StoredKey(displayName, scopes));
    writeRegistry(registry);
    return toAssignment(key, registry.get(key));
  }

  /** Trim + validate a key name. Blank or unsafe names are 400 (fail closed). */
  static String normalizeKeyName(String name) {
    String trimmed = name == null ? "" : name.trim();
    if (trimmed.isEmpty() || !SAFE_KEY_NAME.matcher(trimmed).matches()) {
      throw new IllegalArgumentException(INVALID_KEY_NAME);
    }
    return trimmed;
  }

  /**
   * Deduplicate, trim, catalog-sort, and validate request scopes. Unknown scope keys are 400 —
   * the stored set can only ever contain catalog keys.
   */
  static List<String> canonicalScopes(List<String> scopes) {
    List<String> cleaned = new ArrayList<>();
    if (scopes != null) {
      for (String s : scopes) {
        if (s == null || s.isBlank()) {
          continue;
        }
        String key = s.trim();
        if (!ApiKeyScopeCatalog.isKnownScope(key)) {
          throw new IllegalArgumentException(UNKNOWN_SCOPE + key);
        }
        if (!cleaned.contains(key)) {
          cleaned.add(key);
        }
      }
    }
    cleaned.sort(Comparator.comparingInt(ApiKeyScopeCatalog::scopeOrder));
    return cleaned;
  }

  private ApiKeyAssignment toAssignment(String name, StoredKey stored) {
    ApiKeyAssignment out = new ApiKeyAssignment();
    out.setName(name);
    out.setDisplayName(stored.displayName());
    out.setScopes(new ArrayList<>(stored.scopes()));
    out.setPresetId(ApiKeyScopeCatalog.matchPreset(stored.scopes()));
    return out;
  }

  /** Registry read. Corrupt or unreadable files read as empty (logged, fail closed). */
  Map<String, StoredKey> readRegistry() {
    Path file = registryFileOrWarn();
    if (file == null || !Files.isRegularFile(file)) {
      return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }
    try {
      return parseRegistryText(Files.readString(file, StandardCharsets.UTF_8));
    } catch (IOException | RuntimeException e) {
      log.error("API key scope registry unreadable, treating as empty", e);
      return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }
  }

  private Path registryFileOrWarn() {
    try {
      return registryFile.get();
    } catch (RuntimeException e) {
      log.error("API key scope registry location unavailable", e);
      return null;
    }
  }

  /** Parse registry JSON text; unknown/unsafe entries are skipped, never thrown. */
  static Map<String, StoredKey> parseRegistryText(String text) {
    Map<String, StoredKey> out = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    if (text == null || text.isBlank()) {
      return out;
    }
    try {
      JsonNode keys = MAPPER.readTree(text).get("keys");
      if (keys == null || !keys.isObject()) {
        return out;
      }
      keys.properties().forEach(e -> parseStoredEntry(e.getKey(), e.getValue()).ifPresent(
          entry -> out.put(entry.getKey(), entry.getValue())));
    } catch (RuntimeException e) {
      log.error("API key scope registry unreadable, treating as empty", e);
      return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }
    return out;
  }

  /** Parse one registry entry; empty when the name is unsafe or the node is unusable. */
  static Optional<Map.Entry<String, StoredKey>> parseStoredEntry(
      String keyName, JsonNode node) {
    String candidate = keyName == null ? "" : keyName.trim();
    if (!SAFE_KEY_NAME.matcher(candidate).matches()) {
      log.warn("Skipping unsafe API key name in registry: {}", keyName);
      return Optional.empty();
    }
    try {
      return Optional.of(
          Map.entry(candidate, new StoredKey(parseDisplayName(node), parseStoredScopes(node))));
    } catch (RuntimeException ex) {
      log.warn("Skipping unreadable API key entry in registry: {}", keyName, ex);
      return Optional.empty();
    }
  }

  private static String parseDisplayName(JsonNode node) {
    if (node == null || !node.has("displayName") || !node.get("displayName").isString()) {
      return null;
    }
    String display = node.get("displayName").stringValue();
    return display == null || display.isBlank() ? null : display.trim();
  }

  private static List<String> parseStoredScopes(JsonNode node) {
    List<String> scopes = new ArrayList<>();
    JsonNode rawScopes = node == null ? null : node.get("scopes");
    if (rawScopes == null || !rawScopes.isArray()) {
      return scopes;
    }
    for (JsonNode s : rawScopes) {
      if (!s.isString()) {
        continue;
      }
      String scope = s.stringValue();
      if (scope == null) {
        continue;
      }
      String key = scope.trim();
      if (ApiKeyScopeCatalog.isKnownScope(key) && !scopes.contains(key)) {
        scopes.add(key);
      }
    }
    scopes.sort(Comparator.comparingInt(ApiKeyScopeCatalog::scopeOrder));
    return scopes;
  }

  /** Atomic registry write (temp file + move). Parent dirs are created on first write. */
  void writeRegistry(Map<String, StoredKey> registry) {
    Path file = registryFile.get();
    if (file == null) {
      throw new IllegalStateException("API key scope registry location unavailable");
    }
    try {
      Path parent = file.toAbsolutePath().normalize().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      ObjectNode root = MAPPER.createObjectNode();
      ObjectNode keys = root.putObject("keys");
      for (Map.Entry<String, StoredKey> e : registry.entrySet()) {
        ObjectNode node = keys.putObject(e.getKey());
        if (e.getValue().displayName() != null) {
          node.put("displayName", e.getValue().displayName());
        }
        for (String scope : e.getValue().scopes()) {
          node.withArrayProperty("scopes").add(scope);
        }
      }
      Path tmp =
          Files.createTempFile(
              parent != null ? parent : Path.of(System.getProperty("java.io.tmpdir")),
              "apikey-scopes",
              ".tmp");
      try {
        Files.writeString(
            tmp, MAPPER.writeValueAsString(root), StandardCharsets.UTF_8,
            StandardOpenOption.TRUNCATE_EXISTING);
        try {
          Files.move(
              tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException amnse) {
          Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
      } finally {
        Files.deleteIfExists(tmp);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to persist API key scopes", e);
    }
  }

  static Path defaultRegistryFile() {
    Path base = rxDirFromServer();
    if (base == null) {
      return null;
    }
    return base.resolve("rxconfig").resolve(REGISTRY_DIR_NAME).resolve(REGISTRY_FILE_NAME);
  }

  static Path rxDirFromServer() {
    try {
      File rx = PSServer.getRxDir();
      return rx == null ? null : rx.toPath();
    } catch (RuntimeException e) {
      log.debug("Unable to resolve Rx dir for API key registry: {}", e.getMessage());
      return null;
    }
  }

  private void requireAdmin(String message) {
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
      throw new WebApplicationException(message, Response.Status.FORBIDDEN);
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

  /** Stored registry entry. */
  record StoredKey(String displayName, List<String> scopes) {
    StoredKey(String displayName, List<String> scopes) {
      this.displayName = displayName;
      this.scopes = scopes != null ? List.copyOf(scopes) : List.of();
    }
  }

  /** Visible for tests: file the adaptor persists to. */
  Path registryPathForTest() {
    return registryFile.get();
  }
}
