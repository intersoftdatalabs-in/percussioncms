/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import com.percussion.design.objectstore.PSExtensionParamDef;
import com.percussion.error.PSNonUniqueException;
import com.percussion.error.PSNotFoundException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSExtensionHandler;
import com.percussion.extension.PSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionRef;
import com.percussion.extensions.IPSExtensionService;
import com.percussion.rest.extensions.Extension;
import com.percussion.rest.extensions.ExtensionFilterOptions;
import com.percussion.rest.extensions.ExtensionMethod;
import com.percussion.rest.extensions.ExtensionParameter;
import com.percussion.rest.extensions.IExtensionAdaptor;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.BooleanSupplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Adaptor for Extension catalog and Admin user-extension write (SY-01) over {@link
 * IPSExtensionService}. System ({@code global/percussion/...}) and handler-owned extensions are
 * never mutated. New registrations are forced under context {@code user/}.
 */
@PSSiteManageBean
public class ExtensionAdaptor implements IExtensionAdaptor {

  private static final Logger log = LogManager.getLogger(ExtensionAdaptor.class);

  static final String ADMIN_REQUIRED =
      "Admin role required to register, update, or delete user extensions";

  static final String IMMUTABLE_EXTENSION =
      "System or handler-owned extensions cannot be updated or deleted";

  /** Canonical context for Admin-registered user extensions (Workbench convention). */
  static final String USER_CONTEXT = "user/";

  static final String DEFAULT_HANDLER = "Java";

  private final IPSExtensionService extensionService;
  private final BooleanSupplier adminChecker;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  @Autowired
  public ExtensionAdaptor(IPSExtensionService extensionService) {
    this(extensionService, null);
  }

  /** Package-visible for unit tests. */
  ExtensionAdaptor(IPSExtensionService extensionService, BooleanSupplier adminChecker) {
    this.extensionService = extensionService;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  private Extension copyExtensionRef(PSExtensionRef ref) {
    var ret = new Extension();
    ret.setCategory(ref.getCategory());
    ret.setContext(ref.getContext());
    ret.setExtensionName(ref.getExtensionName());
    ret.setHandlerName(ref.getHandlerName());
    ret.setFqn(ref.getFQN());

    try {
      var def = extensionService.getExtensionDef(ref);

      ret.setDeprecated(def.isDeprecated());
      ret.setJexlExtension(def.isJexlExtension());
      ret.setVersion(def.getVersion());
      ret.setRestoreRequestParamsOnError(def.isRestoreRequestParamsOnError());

      // Copy interfaces
      var interfaces = new ArrayList<String>();
      def.getInterfaces().forEachRemaining(interfaces::add);
      ret.setSupportedInterfaces(interfaces);

      // Init params
      var initParams = new HashMap<String, String>();
      def.getInitParameterNames()
          .forEachRemaining(name -> initParams.put(name, def.getInitParameter(name)));
      ret.setInitParameters(initParams);

      // Methods
      var methods = new HashMap<String, ExtensionMethod>();
      def.getMethods()
          .forEachRemaining(
              defMethod -> {
                var meth = new ExtensionMethod();
                meth.setName(defMethod.getName());
                meth.setDescription(defMethod.getDescription());

                var methParams = new ArrayList<ExtensionParameter>();
                defMethod
                    .getParameters()
                    .forEachRemaining(
                        emp -> {
                          var ep = new ExtensionParameter();
                          ep.setDataType(emp.getType());
                          ep.setDescription(emp.getDescription());
                          ep.setName(emp.getName());
                          methParams.add(ep);
                        });
                meth.setParameters(methParams);
                methods.put(defMethod.getName(), meth);
              });
      ret.setMethods(methods);

      // Required applications
      var apps = new ArrayList<String>();
      def.getRequiredApplications().forEachRemaining(app -> apps.add(app.toString()));
      ret.setRequiredApplications(apps);

      // Resource locations
      var resources = new ArrayList<String>();
      def.getResourceLocations().forEachRemaining(res -> resources.add(res.toString()));
      ret.setResourceLocations(resources);

      // Supplied resources
      var supplied = new ArrayList<String>();
      Iterator<URL> suppliedIt = def.getSuppliedResources();
      if (suppliedIt != null) {
        suppliedIt.forEachRemaining(res -> supplied.add(res.toString()));
      }
      ret.setSuppliedResources(supplied);

      // Runtime parameters
      var runParams = new ArrayList<ExtensionParameter>();
      def.getRuntimeParameterNames()
          .forEachRemaining(
              name -> {
                var runP = new ExtensionParameter();
                var defParam = def.getRuntimeParameter(name);
                runP.setName(defParam.getName());
                runP.setDescription(defParam.getDescription());
                runP.setDataType(defParam.getDataType());
                runParams.add(runP);
              });
      ret.setRuntimeParameters(runParams);

    } catch (PSExtensionException | PSNotFoundException e) {
      log.error("Error copying extension ref {}", ref, e);
    }
    return ret;
  }

  /** Gets all extensions based on the specified ExtensionFilterOptions. */
  @Override
  public List<Extension> getExtensions(URI baseURI, ExtensionFilterOptions filter) {
    var response = new ArrayList<Extension>();
    try {
      var it =
          extensionService.getExtensionNames(
              filter.getHandlerNamePattern(),
              filter.getContext(),
              filter.getInterfacePattern(),
              filter.getExtensionNamePattern());
      while (it.hasNext()) {
        var ref = it.next();
        response.add(copyExtensionRef(ref));
      }
    } catch (PSExtensionException e) {
      log.error("Error getting getExtensionNames", e);
    }
    return response;
  }

  @Override
  public List<Extension> listExtensions(URI baseURI) {
    return getExtensions(baseURI, new ExtensionFilterOptions());
  }

  @Override
  public Extension findExtensionByKey(URI baseURI, String idOrName) {
    if (!isSafeExtensionKey(idOrName)) {
      return null;
    }
    String key = idOrName.trim();
    Extension direct = resolveExistingForWrite(baseURI, key);
    if (direct != null) {
      return direct;
    }
    return null;
  }

  /**
   * Prefer {@link IPSExtensionService#getExtensionDef} (O(1)) over catalog enumeration. Tries FQN
   * first, then {@code Java/user/&lt;name&gt;} for Admin/SPA short names, then scans the catalog.
   */
  private Extension resolveExistingForWrite(URI baseURI, String idOrName) {
    String key = idOrName.trim();
    Extension direct = tryLoadByExtensionRef(key);
    if (direct != null) {
      return direct;
    }
    if (key.indexOf('/') < 0) {
      Extension userGuess = tryLoadByExtensionRef(DEFAULT_HANDLER + "/" + USER_CONTEXT + key);
      if (userGuess != null) {
        return userGuess;
      }
    }
    List<Extension> all = listExtensions(baseURI);
    if (all == null) {
      return null;
    }
    for (Extension e : all) {
      if (e == null) {
        continue;
      }
      if (key.equalsIgnoreCase(e.getFqn()) || key.equalsIgnoreCase(e.getExtensionName())) {
        return e;
      }
    }
    return null;
  }

  private Extension tryLoadByExtensionRef(String fqnOrFull) {
    if (StringUtils.isBlank(fqnOrFull) || fqnOrFull.indexOf('/') < 0) {
      return null;
    }
    final PSExtensionRef ref;
    try {
      ref = new PSExtensionRef(fqnOrFull);
    } catch (IllegalArgumentException e) {
      return null;
    }
    try {
      // Probe existence — copyExtensionRef swallows not-found and would return a skeleton.
      extensionService.getExtensionDef(ref);
    } catch (PSNotFoundException e) {
      return null;
    } catch (PSExtensionException e) {
      log.debug("Direct extension def lookup failed for {}: {}", fqnOrFull, e.getMessage());
      return null;
    }
    return copyExtensionRef(ref);
  }

  @Override
  public Extension registerExtension(URI baseURI, Extension body) {
    requireAdmin();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String extensionName = requireValidExtensionName(body.getExtensionName());
    String handlerName = resolveHandlerName(body.getHandlerName());
    String category = StringUtils.defaultString(body.getCategory());
    List<String> interfaces = requireInterfaces(body.getSupportedInterfaces());
    rejectImmutableRegistrationContext(body.getContext());

    PSExtensionRef ref = new PSExtensionRef(category, handlerName, USER_CONTEXT, extensionName);
    assertRefAvailable(ref);
    if ("Java".equalsIgnoreCase(handlerName)) {
      requireJavaClassName(body.getInitParameters());
    }

    PSExtensionDef def =
        buildDef(
            ref,
            interfaces,
            body.getInitParameters(),
            body.getRuntimeParameters(),
            body.getResourceLocations(),
            body.getSuppliedResources(),
            body.getRequiredApplications(),
            body.isDeprecated(),
            body.isRestoreRequestParamsOnError(),
            body.getVersion());

    try {
      extensionService.installExtension(def, Collections.emptyIterator());
    } catch (PSNonUniqueException e) {
      throw new WebApplicationException("Extension already exists: " + ref.getFQN(), 409);
    } catch (PSNotFoundException e) {
      throw new IllegalArgumentException("Extension handler not found: " + handlerName, e);
    } catch (PSExtensionException e) {
      throw new IllegalStateException("Failed to register extension: " + e.getMessage(), e);
    }

    Extension created = findExtensionByKey(baseURI, ref.getFQN());
    if (created == null) {
      created = copyExtensionRef(ref);
    }
    return created;
  }

  @Override
  public Extension updateExtension(URI baseURI, String idOrName, Extension body) {
    requireAdmin();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    if (!isSafeExtensionKey(idOrName)) {
      return null;
    }
    Extension existing = resolveExistingForWrite(baseURI, idOrName);
    if (existing == null) {
      return null;
    }
    rejectImmutableMutation(existing);

    PSExtensionRef ref = refFromExtension(existing);
    IPSExtensionDef current;
    try {
      current = extensionService.getExtensionDef(ref);
    } catch (PSNotFoundException e) {
      return null;
    } catch (PSExtensionException e) {
      throw new IllegalStateException("Failed to load extension: " + e.getMessage(), e);
    }

    List<String> interfaces =
        body.getSupportedInterfaces() != null && !body.getSupportedInterfaces().isEmpty()
            ? requireInterfaces(body.getSupportedInterfaces())
            : collectInterfaces(current);
    Map<String, String> initParams =
        mergeInitParams(current, body.getInitParameters());
    if ("Java".equalsIgnoreCase(ref.getHandlerName())) {
      requireJavaClassName(initParams);
    }
    List<ExtensionParameter> runtimeParams =
        body.getRuntimeParameters() != null
            ? body.getRuntimeParameters()
            : copyRuntimeParams(current);
    List<String> resourceLocations =
        body.getResourceLocations() != null
            ? body.getResourceLocations()
            : copyUrlList(current.getResourceLocations());
    List<String> suppliedResources =
        body.getSuppliedResources() != null
            ? body.getSuppliedResources()
            : copyUrlList(current.getSuppliedResources());
    List<String> requiredApps =
        body.getRequiredApplications() != null
            ? body.getRequiredApplications()
            : copyRequiredApps(current);

    // Wire booleans are primitives — clients should round-trip GET then PUT.
    boolean deprecated = body.isDeprecated();
    boolean restoreOnError = body.isRestoreRequestParamsOnError();
    long version = body.getVersion() > 0 ? body.getVersion() : current.getVersion();

    PSExtensionDef def =
        buildDef(
            ref,
            interfaces,
            initParams,
            runtimeParams,
            resourceLocations,
            suppliedResources,
            requiredApps,
            deprecated,
            restoreOnError,
            version);

    try {
      extensionService.updateExtension(def, Collections.emptyIterator());
    } catch (PSNotFoundException e) {
      return null;
    } catch (PSExtensionException e) {
      throw new IllegalStateException("Failed to update extension: " + e.getMessage(), e);
    }

    Extension updated = findExtensionByKey(baseURI, ref.getFQN());
    if (updated == null) {
      updated = copyExtensionRef(ref);
    }
    return updated;
  }

  @Override
  public boolean deleteExtension(URI baseURI, String idOrName) {
    requireAdmin();
    if (!isSafeExtensionKey(idOrName)) {
      return false;
    }
    Extension existing = findExtensionByKey(baseURI, idOrName);
    if (existing == null) {
      return false;
    }
    rejectImmutableMutation(existing);
    PSExtensionRef ref = refFromExtension(existing);
    try {
      extensionService.removeExtension(ref);
      return true;
    } catch (PSNotFoundException e) {
      return false;
    } catch (PSExtensionException e) {
      throw new IllegalStateException("Failed to delete extension: " + e.getMessage(), e);
    }
  }

  private PSExtensionDef buildDef(
      PSExtensionRef ref,
      List<String> interfaces,
      Map<String, String> initParameters,
      List<ExtensionParameter> runtimeParameters,
      List<String> resourceLocations,
      List<String> suppliedResources,
      List<String> requiredApplications,
      boolean deprecated,
      boolean restoreRequestParamsOnError,
      long version) {
    Properties initProps = new Properties();
    if (initParameters != null) {
      for (Map.Entry<String, String> e : initParameters.entrySet()) {
        if (e.getKey() != null && e.getValue() != null) {
          initProps.setProperty(e.getKey(), e.getValue());
        }
      }
    }
    if (version > 0) {
      initProps.setProperty(IPSExtensionDef.INIT_PARAM_VERSION, Long.toString(version));
    }

    List<PSExtensionParamDef> runtime = new ArrayList<>();
    if (runtimeParameters != null) {
      for (ExtensionParameter p : runtimeParameters) {
        if (p == null || StringUtils.isBlank(p.getName())) {
          continue;
        }
        PSExtensionParamDef pd =
            new PSExtensionParamDef(
                p.getName().trim(),
                StringUtils.defaultIfBlank(p.getDataType(), "java.lang.String"));
        if (p.getDescription() != null) {
          pd.setDescription(p.getDescription());
        }
        runtime.add(pd);
      }
    }

    List<URL> resourceUrls = toUrls(resourceLocations, "resourceLocations");
    List<URL> suppliedUrls = toUrls(suppliedResources, "suppliedResources");

    PSExtensionDef def =
        new PSExtensionDef(
            ref,
            interfaces.iterator(),
            resourceUrls.iterator(),
            initProps,
            runtime.iterator(),
            suppliedUrls.iterator(),
            deprecated,
            restoreRequestParamsOnError);

    if (requiredApplications != null && !requiredApplications.isEmpty()) {
      List<PSExtensionRef> apps = new ArrayList<>();
      for (String app : requiredApplications) {
        if (StringUtils.isBlank(app)) {
          continue;
        }
        try {
          apps.add(new PSExtensionRef(app.trim()));
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("invalid requiredApplications entry: " + app, e);
        }
      }
      def.setRequiredApplications(apps.iterator());
    }
    return def;
  }

  private void assertRefAvailable(PSExtensionRef ref) {
    try {
      if (extensionService.exists(ref)) {
        throw new WebApplicationException("Extension already exists: " + ref.getFQN(), 409);
      }
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSExtensionException e) {
      throw new IllegalStateException("Failed to check extension existence: " + e.getMessage(), e);
    }
  }

  private static void rejectImmutableRegistrationContext(String context) {
    if (StringUtils.isBlank(context)) {
      return;
    }
    String normalized = PSExtensionRef.canonicalizeContext(context.trim());
    if (isImmutableContext(normalized)) {
      throw new WebApplicationException(
          "Cannot register extensions under system or handler context", 409);
    }
    // Non-user contexts are rejected for Admin REST create; force user/.
    if (!USER_CONTEXT.equalsIgnoreCase(normalized)
        && !"user".equalsIgnoreCase(context.trim())) {
      throw new IllegalArgumentException(
          "context must be user/ for Admin registration (got " + context + ")");
    }
  }

  private void rejectImmutableMutation(Extension existing) {
    if (isImmutableExtension(existing)) {
      throw new WebApplicationException(IMMUTABLE_EXTENSION, 409);
    }
  }

  static boolean isImmutableExtension(Extension e) {
    if (e == null) {
      return false;
    }
    String handler = StringUtils.trimToEmpty(e.getHandlerName());
    // Fail closed: blank handler on a catalog row — treat as immutable (SPA mirrors this).
    if (handler.isEmpty()) {
      return true;
    }
    if (IPSExtensionHandler.HANDLER_HANDLER.equalsIgnoreCase(handler)) {
      return true;
    }
    // Also covers handlers/... and global/percussion/... via context prefix.
    return isImmutableContext(e.getContext());
  }

  static boolean isImmutableContext(String context) {
    if (context == null || context.isBlank()) {
      return false;
    }
    String c = context.trim().toLowerCase(Locale.ROOT);
    if (!c.endsWith("/")) {
      c = c + "/";
    }
    if (c.startsWith("handlers/")) {
      return true;
    }
    return c.startsWith("global/percussion/");
  }

  static String requireValidExtensionName(String raw) {
    if (StringUtils.isBlank(raw)) {
      throw new IllegalArgumentException("extensionName is required");
    }
    String name = raw.trim();
    if (!PSExtensionRef.isValidExtensionName(name)) {
      throw new IllegalArgumentException("invalid extensionName");
    }
    return name;
  }

  static String resolveHandlerName(String raw) {
    if (StringUtils.isBlank(raw)) {
      return DEFAULT_HANDLER;
    }
    String handler = raw.trim();
    if (!PSExtensionRef.isValidExtensionName(handler)) {
      throw new IllegalArgumentException("invalid handlerName");
    }
    if (IPSExtensionHandler.HANDLER_HANDLER.equalsIgnoreCase(handler)) {
      throw new WebApplicationException("Cannot register handler-owned extensions", 409);
    }
    return handler;
  }

  static List<String> requireInterfaces(List<String> interfaces) {
    if (interfaces == null || interfaces.isEmpty()) {
      throw new IllegalArgumentException("supportedInterfaces is required");
    }
    List<String> out = new ArrayList<>();
    for (String iface : interfaces) {
      if (StringUtils.isBlank(iface)) {
        continue;
      }
      out.add(iface.trim());
    }
    if (out.isEmpty()) {
      throw new IllegalArgumentException("supportedInterfaces is required");
    }
    return out;
  }

  static void requireJavaClassName(Map<String, String> initParams) {
    if (initParams == null
        || StringUtils.isBlank(initParams.get(IPSExtensionDef.INIT_PARAM_CLASSNAME))) {
      throw new IllegalArgumentException(
          "initParameters.className is required for Java extensions");
    }
  }

  private static PSExtensionRef refFromExtension(Extension e) {
    String category = StringUtils.defaultString(e.getCategory());
    String handler = e.getHandlerName();
    String context = e.getContext();
    String name = e.getExtensionName();
    if (StringUtils.isAnyBlank(handler, context, name)) {
      if (StringUtils.isNotBlank(e.getFqn())) {
        return new PSExtensionRef(e.getFqn());
      }
      throw new IllegalStateException("extension identity incomplete");
    }
    return new PSExtensionRef(category, handler, context, name);
  }

  private static List<String> collectInterfaces(IPSExtensionDef def) {
    List<String> out = new ArrayList<>();
    def.getInterfaces().forEachRemaining(out::add);
    return out;
  }

  private static Map<String, String> mergeInitParams(
      IPSExtensionDef current, Map<String, String> bodyParams) {
    Map<String, String> merged = new HashMap<>();
    current
        .getInitParameterNames()
        .forEachRemaining(name -> merged.put(name, current.getInitParameter(name)));
    if (bodyParams != null) {
      for (Map.Entry<String, String> e : bodyParams.entrySet()) {
        if (e.getKey() == null) {
          continue;
        }
        if (e.getValue() == null) {
          merged.remove(e.getKey());
        } else {
          merged.put(e.getKey(), e.getValue());
        }
      }
    }
    return merged;
  }

  private static List<ExtensionParameter> copyRuntimeParams(IPSExtensionDef def) {
    List<ExtensionParameter> out = new ArrayList<>();
    def.getRuntimeParameterNames()
        .forEachRemaining(
            name -> {
              var p = def.getRuntimeParameter(name);
              ExtensionParameter ep = new ExtensionParameter();
              ep.setName(p.getName());
              ep.setDescription(p.getDescription());
              ep.setDataType(p.getDataType());
              out.add(ep);
            });
    return out;
  }

  private static List<String> copyUrlList(Iterator<URL> it) {
    List<String> out = new ArrayList<>();
    if (it == null) {
      return out;
    }
    it.forEachRemaining(u -> out.add(u.toString()));
    return out;
  }

  private static List<String> copyRequiredApps(IPSExtensionDef def) {
    List<String> out = new ArrayList<>();
    def.getRequiredApplications().forEachRemaining(app -> out.add(app.toString()));
    return out;
  }

  private static List<URL> toUrls(List<String> locations, String field) {
    List<URL> out = new ArrayList<>();
    if (locations == null) {
      return out;
    }
    for (String loc : locations) {
      if (StringUtils.isBlank(loc)) {
        continue;
      }
      try {
        out.add(URI.create(loc.trim()).toURL());
      } catch (IllegalArgumentException | MalformedURLException e) {
        throw new IllegalArgumentException("invalid " + field + " URL: " + loc, e);
      }
    }
    return out;
  }

  private void requireAdmin() {
    boolean allowed;
    try {
      allowed = adminChecker.getAsBoolean();
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.debug("Admin check failed: {}", e.getMessage());
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
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

  /** Allow FQN-style keys (may contain '/'). Reject traversal and backslash/null. */
  static boolean isSafeExtensionKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    if (key.contains("..")) {
      return false;
    }
    return key.indexOf('\\') < 0 && key.indexOf('\0') < 0;
  }
}
