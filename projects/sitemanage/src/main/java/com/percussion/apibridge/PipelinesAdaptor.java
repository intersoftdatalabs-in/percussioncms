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

import com.percussion.design.objectstore.PSApplication;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSDataSet;
import com.percussion.design.objectstore.PSRequestor;
import com.percussion.design.objectstore.server.PSApplicationSummary;
import com.percussion.design.objectstore.server.PSServerXmlObjectStore;
import com.percussion.error.PSNotFoundException;
import com.percussion.rest.pipelines.ApplicationDataSetSummary;
import com.percussion.rest.pipelines.ApplicationDetail;
import com.percussion.rest.pipelines.ApplicationSummary;
import com.percussion.rest.pipelines.IPipelinesAdaptor;
import com.percussion.security.PSAuthorizationException;
import com.percussion.security.PSSecurityToken;
import com.percussion.server.PSRequest;
import com.percussion.conn.PSServerException;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.server.PSServer;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import jakarta.ws.rs.core.Response;
import java.util.function.BooleanSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import com.percussion.services.pipeline.IPSPipelineIrService;
import com.percussion.services.pipeline.IPSPipelineRuntimeService;
import com.percussion.services.pipeline.PSPipelineIrException;
import com.percussion.services.pipeline.PSPipelineIrServiceLocator;
import com.percussion.services.pipeline.PSPipelineRuntimeServiceLocator;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.util.PSCollection;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Lists classic XML Applications (pipeline packages) visible to the current security token,
 * Admin start/stop via {@link PSServer}, read-only Pipeline IR via {@link IPSPipelineIrService},
 * and thin IR execute via {@link IPSPipelineRuntimeService}.
 *
 * <p>Uses {@link PSServerXmlObjectStore} for summaries; mapping/filter/limit are pure helpers so
 * they can be unit-tested without the object-store singleton. Execute never calls classic {@code
 * PSQueryHandler}. IR GET never persists native IR or classic import results. Start/stop peer
 * console {@code start application} / {@code stop application}.
 */
@PSSiteManageBean
public class PipelinesAdaptor implements IPipelinesAdaptor {

  private static final Logger log = LogManager.getLogger(PipelinesAdaptor.class);

  /** Default page size when callers pass non-positive limit. */
  public static final int DEFAULT_LIMIT = 500;

  /** Hard cap to avoid unbounded payloads on large servers. */

  static final String ADMIN_REQUIRED = "Admin role required to start or stop pipeline applications";

  static final String HIDDEN_NOT_ALLOWED =
      "Hidden applications cannot be started or stopped via this API";

  static final String DISABLED_NOT_ALLOWED = "Application is disabled";

  private final Function<PSSecurityToken, PSApplicationSummary[]> summaryLoader;
  private final Supplier<IPSPipelineRuntimeService> runtimeSupplier;
  private final Supplier<IPSPipelineIrService> irSupplier;
  private final BiFunction<String, PSSecurityToken, PSApplication> applicationLoader;
  private final BooleanSupplier adminChecker;
  private final ApplicationLifecycleOps lifecycleOps;
  private final ApplicationDetailLoader detailLoader;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public PipelinesAdaptor() {
    this(
        tok -> PSServerXmlObjectStore.getInstance().getApplicationSummaryObjects(tok, false),
        PSPipelineRuntimeServiceLocator::getPipelineRuntimeService,
        PSPipelineIrServiceLocator::getPipelineIrService,
        PipelinesAdaptor::loadApplicationObject,
        null,
        null,
        null);
  }

  /** Package-visible for unit tests that inject a fake summary source. */
  PipelinesAdaptor(Function<PSSecurityToken, PSApplicationSummary[]> summaryLoader) {
    this(
        summaryLoader,
        PSPipelineRuntimeServiceLocator::getPipelineRuntimeService,
        PSPipelineIrServiceLocator::getPipelineIrService,
        PipelinesAdaptor::loadApplicationObject,
        null,
        null,
        null);
  }

  /**
   * Package-visible for unit tests that inject summary source and/or runtime service without the
   * static locator.
   */
  PipelinesAdaptor(
      Function<PSSecurityToken, PSApplicationSummary[]> summaryLoader,
      Supplier<IPSPipelineRuntimeService> runtimeSupplier) {
    this(
        summaryLoader,
        runtimeSupplier,
        PSPipelineIrServiceLocator::getPipelineIrService,
        PipelinesAdaptor::loadApplicationObject,
        null,
        null,
        null);
  }

  /**
   * Package-visible for unit tests covering IR read with injected IR service and optional classic
   * application loader (no object-store / locator singletons).
   */
  PipelinesAdaptor(
      Function<PSSecurityToken, PSApplicationSummary[]> summaryLoader,
      Supplier<IPSPipelineRuntimeService> runtimeSupplier,
      Supplier<IPSPipelineIrService> irSupplier,
      BiFunction<String, PSSecurityToken, PSApplication> applicationLoader) {
    this(summaryLoader, runtimeSupplier, irSupplier, applicationLoader, null, null, null);
  }

  /**
   * Package-visible for unit tests covering Admin start/stop with injected collaborators (no {@link
   * PSServer} / object-store singletons). Uses production IR loaders.
   */
  PipelinesAdaptor(
      Function<PSSecurityToken, PSApplicationSummary[]> summaryLoader,
      Supplier<IPSPipelineRuntimeService> runtimeSupplier,
      BooleanSupplier adminChecker,
      ApplicationLifecycleOps lifecycleOps,
      ApplicationDetailLoader detailLoader) {
    this(
        summaryLoader,
        runtimeSupplier,
        PSPipelineIrServiceLocator::getPipelineIrService,
        PipelinesAdaptor::loadApplicationObject,
        adminChecker,
        lifecycleOps,
        detailLoader);
  }

  /**
   * Full package-visible constructor for tests that inject IR and Admin lifecycle collaborators.
   */
  PipelinesAdaptor(
      Function<PSSecurityToken, PSApplicationSummary[]> summaryLoader,
      Supplier<IPSPipelineRuntimeService> runtimeSupplier,
      Supplier<IPSPipelineIrService> irSupplier,
      BiFunction<String, PSSecurityToken, PSApplication> applicationLoader,
      BooleanSupplier adminChecker,
      ApplicationLifecycleOps lifecycleOps,
      ApplicationDetailLoader detailLoader) {
    this.summaryLoader = summaryLoader;
    this.runtimeSupplier =
        runtimeSupplier != null
            ? runtimeSupplier
            : PSPipelineRuntimeServiceLocator::getPipelineRuntimeService;
    this.irSupplier =
        irSupplier != null ? irSupplier : PSPipelineIrServiceLocator::getPipelineIrService;
    this.applicationLoader =
        applicationLoader != null ? applicationLoader : PipelinesAdaptor::loadApplicationObject;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
    this.lifecycleOps = lifecycleOps != null ? lifecycleOps : new DefaultApplicationLifecycleOps();
    this.detailLoader = detailLoader != null ? detailLoader : this::loadDetailFromObjectStore;
  }

  private static PSApplication loadApplicationObject(String name, PSSecurityToken tok) {
    try {
      // fixupCeFields=false: IR import only; avoid CE field rewrite cost
      return PSServerXmlObjectStore.getInstance().getApplicationObject(name, tok, false);
    } catch (PSNotFoundException | PSAuthorizationException e) {
      return null;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load application for pipeline IR", e);
    }
  }

  @Override
  public List<ApplicationSummary> listApplications(
      URI baseUri, String nameFilter, int limit, int offset) {
    // baseUri reserved for HATEOAS link building (interface contract)
    PSRequest req = PSSecurityFilter.getCurrentRequest();
    if (req == null) {
      throw new IllegalStateException("No current request for application catalog");
    }
    PSSecurityToken tok = req.getSecurityToken();
    PSApplicationSummary[] sums = summaryLoader.apply(tok);
    return mapFilterSortLimit(sums, nameFilter, limit, offset);
  }

  @Override
  public ApplicationDetail getApplication(URI baseUri, String idOrName) {
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    PSRequest req = PSSecurityFilter.getCurrentRequest();
    if (req == null) {
      throw new IllegalStateException("No current request for application detail");
    }
    PSSecurityToken tok = req.getSecurityToken();
    // Resolve only against the object-store catalog so the name passed to
    // getApplicationObject is never the raw path param (java/path-injection).
    String name = resolveApplicationName(idOrName.trim(), summaryLoader.apply(tok));
    if (name == null) {
      return null;
    }
    return detailLoader.load(name, tok);
  }

  @Override
  public PipelineIrDocument getPipelineIr(URI baseUri, String idOrName) {
    // baseUri reserved for HATEOAS link building (interface contract)
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    PSRequest req = PSSecurityFilter.getCurrentRequest();
    if (req == null) {
      throw new IllegalStateException("No current request for pipeline IR");
    }
    PSSecurityToken tok = req.getSecurityToken();
    // Resolve only against the object-store catalog so the name passed to IR loaders
    // is never the raw path param (java/path-injection).
    String name = resolveApplicationName(idOrName.trim(), summaryLoader.apply(tok));
    if (name == null) {
      return null;
    }
    try {
      Optional<PipelineIrDocument> nativeIr = irSupplier.get().load(name);
      if (nativeIr.isPresent()) {
        return nativeIr.get();
      }
      PSApplication app = applicationLoader.apply(name, tok);
      if (app == null) {
        return null;
      }
      // Classic import is read-only for this endpoint — do not save native IR.
      return irSupplier.get().importClassicApplication(app);
    } catch (PSPipelineIrException e) {
      String msg = e.getMessage() != null ? e.getMessage() : "Failed to load pipeline IR";
      if (isNotFoundMessage(msg)) {
        throw new WebApplicationException("Pipeline IR not found", 404);
      }
      // Import/decode failures are client-visible validation problems; no path echo.
      throw new WebApplicationException(msg, 400);
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.warn("Unexpected failure loading pipeline IR for {}", name, e);
      throw e;
    }
  }

  @Override
  public PipelineExecuteResult execute(
      URI baseUri, String appName, String resourceName, PipelineExecuteRequest request) {
    // baseUri reserved for HATEOAS link building (interface contract)
    if (StringUtils.isBlank(appName) || !isSafeApplicationName(appName.trim())) {
      throw new WebApplicationException("Invalid pipeline application name", 400);
    }
    if (StringUtils.isBlank(resourceName) || !isSafeResourceName(resourceName.trim())) {
      throw new WebApplicationException("Invalid pipeline resource name", 400);
    }
    String safeApp = appName.trim();
    String safeResource = resourceName.trim();
    PipelineExecuteRequest req = request != null ? request : PipelineExecuteRequest.empty();
    try {
      return runtimeSupplier.get().execute(safeApp, safeResource, req);
    } catch (PSPipelineIrException e) {
      String msg = e.getMessage() != null ? e.getMessage() : "Pipeline execute failed";
      // Generic 404 bodies: do not echo raw path params (name probing).
      if (isNotFoundMessage(msg)) {
        throw new WebApplicationException("Pipeline application or resource not found", 404);
      }
      // Planner/validation failures are client errors; keep message (no path echo).
      throw new WebApplicationException(msg, 400);
    }
  }

  @Override
  public ApplicationDetail startApplication(URI baseUri, String idOrName) {
    requireAdmin();
    ResolvedApp resolved = resolveForLifecycle(idOrName);
    if (resolved == null) {
      return null;
    }
    if (resolved.summary.isHidden()) {
      throw new WebApplicationException(HIDDEN_NOT_ALLOWED, Response.Status.BAD_REQUEST);
    }
    if (!resolved.summary.isEnabled()) {
      throw new WebApplicationException(DISABLED_NOT_ALLOWED, Response.Status.BAD_REQUEST);
    }
    if (!lifecycleOps.isActive(resolved.name)) {
      try {
        lifecycleOps.start(resolved.name);
      } catch (PSNotFoundException e) {
        log.debug("Application not found on start {}: {}", resolved.name, e.toString());
        return null;
      } catch (PSAuthorizationException e) {
        throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
      } catch (PSSystemValidationException e) {
        String msg =
            StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "Application failed validation";
        throw new WebApplicationException(msg, Response.Status.BAD_REQUEST);
      } catch (PSServerException e) {
        throw new IllegalStateException("Failed to start application", e);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalStateException("Failed to start application", e);
      }
    }
    return detailAfterLifecycle(resolved);
  }

  @Override
  public ApplicationDetail stopApplication(URI baseUri, String idOrName) {
    requireAdmin();
    ResolvedApp resolved = resolveForLifecycle(idOrName);
    if (resolved == null) {
      return null;
    }
    if (resolved.summary.isHidden()) {
      throw new WebApplicationException(HIDDEN_NOT_ALLOWED, Response.Status.BAD_REQUEST);
    }
    // Idempotent when already stopped (peer console stop when not running).
    if (lifecycleOps.isActive(resolved.name)) {
      lifecycleOps.stop(resolved.name);
    }
    return detailAfterLifecycle(resolved);
  }

  private ResolvedApp resolveForLifecycle(String idOrName) {
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    PSRequest req = PSSecurityFilter.getCurrentRequest();
    if (req == null) {
      throw new IllegalStateException("No current request for application lifecycle");
    }
    PSSecurityToken tok = req.getSecurityToken();
    PSApplicationSummary[] sums = summaryLoader.apply(tok);
    String name = resolveApplicationName(idOrName.trim(), sums);
    if (name == null) {
      return null;
    }
    PSApplicationSummary match = findSummaryByName(name, sums);
    if (match == null) {
      return null;
    }
    return new ResolvedApp(name, match, tok);
  }

  private ApplicationDetail detailAfterLifecycle(ResolvedApp resolved) {
    ApplicationDetail detail = detailLoader.load(resolved.name, resolved.token);
    if (detail == null) {
      // Fallback from trusted catalog summary when object-store detail is unavailable.
      detail = toDetailFromSummary(resolved.summary);
    }
    detail.setActive(lifecycleOps.isActive(resolved.name));
    return detail;
  }

  private ApplicationDetail loadDetailFromObjectStore(String name, PSSecurityToken tok) {
    try {
      // fixupCeFields=false: catalog/detail only; avoid CE field rewrite cost
      PSApplication app =
          PSServerXmlObjectStore.getInstance().getApplicationObject(name, tok, false);
      if (app == null) {
        return null;
      }
      ApplicationDetail detail = toDetail(app);
      detail.setActive(lifecycleOps.isActive(name));
      return detail;
    } catch (PSNotFoundException | PSAuthorizationException e) {
      // Expected miss / no design access → resource maps null to generic 404
      log.debug("Application not found or not visible {}: {}", name, e.toString());
      return null;
    } catch (RuntimeException e) {
      log.warn("Unexpected failure loading application detail for {}", name, e);
      throw e;
    } catch (Exception e) {
      // Checked object-store failures (e.g. PSServerException) surface as 500 via resource
      log.warn("Failed to load application detail for {}", name, e);
      throw new IllegalStateException("Failed to load application detail", e);
    }
  }

  private void requireAdmin() {
    boolean allowed;
    try {
      allowed = adminChecker.getAsBoolean();
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      // Unexpected authz failures must surface as 500, not a misleading 403.
      log.warn("Unexpected failure during Admin check", e);
      throw new WebApplicationException(
          "Admin authorization check failed", Response.Status.INTERNAL_SERVER_ERROR);
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
      log.warn("Unable to resolve current user for Admin check: {}", e.getMessage());
      return false;
    }
  }

  /** True when the runtime reports missing IR app or resource (not validation failures). */
  static boolean isNotFoundMessage(String message) {
    if (message == null) {
      return false;
    }
    String m = message.toLowerCase(Locale.ROOT);
    return m.contains("pipeline ir not found") || m.contains("resource not found in ir");
  }

  /**
   * Application names become object-store directory names. Reject path traversal and separators so
   * a user-supplied {@code idOrName} cannot escape the apps root ({@code java/path-injection}).
   */
  static boolean isSafeApplicationName(String name) {
    if (StringUtils.isBlank(name)) {
      return false;
    }
    // Single path component only — matches CodeQL path-injection sanitizer patterns.
    return !name.contains("..")
        && name.indexOf('/') < 0
        && name.indexOf('\\') < 0
        && name.indexOf('\0') < 0;
  }

  /**
   * Resource names are IR identifiers (not filesystem paths). Same single-segment rules as
   * application names to reject traversal / separators in the path param.
   */
  static boolean isSafeResourceName(String name) {
    return isSafeApplicationName(name);
  }

  /**
   * Resolve numeric id or application name against the catalog summary list.
   *
   * <p>Always returns {@link PSApplicationSummary#getName()} from a matching summary (trusted
   * object-store catalog value), never the raw user string. Unknown / unsafe input yields {@code
   * null}.
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

  /** Package-visible for unit tests. */
  static ApplicationDetail toDetail(PSApplication app) {
    ApplicationDetail d = new ApplicationDetail();
    d.setId(app.getId());
    d.setName(app.getName());
    d.setDescription(app.getDescription());
    d.setEnabled(app.isEnabled());
    d.setHidden(app.isHidden());
    d.setAppRoot(app.getRequestRoot());
    if (app.getApplicationType() != null) {
      d.setAppType(app.getApplicationType().name());
    }
    d.setVersion(app.getVersion());

    List<ApplicationDataSetSummary> sets = new ArrayList<>();
    PSCollection dataSets = app.getDataSets();
    if (dataSets != null) {
      for (Object o : dataSets) {
        if (!(o instanceof PSDataSet ds)) {
          continue;
        }
        ApplicationDataSetSummary s = new ApplicationDataSetSummary();
        s.setName(ds.getName());
        s.setDescription(ds.getDescription());
        PSRequestor req = ds.getRequestor();
        if (req != null) {
          s.setRequestPage(req.getRequestPage());
        }
        s.setKind(ds instanceof PSContentEditor ? "CONTENT_EDITOR" : "DATASET");
        sets.add(s);
      }
    }
    sets.sort(
        Comparator.comparing(
            ApplicationDataSetSummary::getName,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    d.setDataSets(sets);
    d.setDesignGaps(defaultDesignGaps());
    return d;
  }

  static ApplicationDetail toDetailFromSummary(PSApplicationSummary sum) {
    ApplicationDetail d = new ApplicationDetail();
    d.setId(sum.getId());
    d.setName(sum.getName());
    d.setDescription(sum.getDescription());
    d.setEnabled(sum.isEnabled());
    d.setHidden(sum.isHidden());
    d.setAppRoot(sum.getAppRoot());
    if (sum.getAppType() != null) {
      d.setAppType(sum.getAppType().name());
    }
    d.setVersion(sum.getVersion());
    d.setDataSets(List.of());
    d.setDesignGaps(defaultDesignGaps());
    return d;
  }

  /**
   * Remaining design gaps after IR <strong>read</strong> and Admin start/stop ship. IR write /
   * enable / ZIP import remain later slices.
   */
  static List<String> defaultDesignGaps() {
    List<String> gaps = new ArrayList<>();
    gaps.add(
        "Pipe IR write / graph editor / native IR save not supported (GET …/ir is read-only)");
    gaps.add("Enable / disable application not supported via this API");
    gaps.add("Classic application import/export ZIP not supported via this API");
    return gaps;
  }

  /** Pure mapping path used by production and unit tests (no object-store singleton). */
  static List<ApplicationSummary> mapFilterSortLimit(
      PSApplicationSummary[] sums, String nameFilter, int limit, int offset) {
    int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
    int safeOffset = Math.max(0, offset);
    String q = StringUtils.isBlank(nameFilter) ? null : nameFilter.trim().toLowerCase(Locale.ROOT);

    List<ApplicationSummary> out = new ArrayList<>();
    if (sums != null) {
      for (PSApplicationSummary sum : sums) {
        if (sum == null) {
          continue;
        }
        try {
          ApplicationSummary dto = toSummary(sum);
          if (q != null && !matchesNameFilter(dto, q)) {
            continue;
          }
          out.add(dto);
        } catch (Exception e) {
          log.debug("Skipping application summary {}: {}", sum.getName(), e.getMessage());
        }
      }
    }
    out.sort(
        Comparator.comparing(
            ApplicationSummary::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    if (safeOffset >= out.size()) {
      return List.of();
    }
    int end = Math.min(out.size(), safeOffset + safeLimit);
    return new ArrayList<>(out.subList(safeOffset, end));
  }

  static boolean matchesNameFilter(ApplicationSummary dto, String qLower) {
    String name = dto.getName() != null ? dto.getName().toLowerCase(Locale.ROOT) : "";
    String desc = dto.getDescription() != null ? dto.getDescription().toLowerCase(Locale.ROOT) : "";
    return name.contains(qLower) || desc.contains(qLower);
  }

  static ApplicationSummary toSummary(PSApplicationSummary sum) {
    ApplicationSummary dto = new ApplicationSummary();
    dto.setId(sum.getId());
    dto.setName(sum.getName());
    dto.setDescription(sum.getDescription());
    dto.setEnabled(sum.isEnabled());
    dto.setActive(sum.isActive());
    dto.setAppRoot(sum.getAppRoot());
    if (sum.getAppType() != null) {
      dto.setAppType(sum.getAppType().name());
    }
    dto.setVersion(sum.getVersion());
    dto.setEmpty(sum.isEmpty());
    dto.setHidden(sum.isHidden());
    return dto;
  }

  private static final class ResolvedApp {
    final String name;
    final PSApplicationSummary summary;
    final PSSecurityToken token;

    ResolvedApp(String name, PSApplicationSummary summary, PSSecurityToken token) {
      this.name = name;
      this.summary = summary;
      this.token = token;
    }
  }

  /** Start/stop/active peers of {@link PSServer} — injectable for unit tests. */
  interface ApplicationLifecycleOps {
    boolean isActive(String appName);

    void start(String appName) throws Exception;

    boolean stop(String appName);
  }

  /** Load detail by trusted catalog name — injectable for unit tests. */
  @FunctionalInterface
  interface ApplicationDetailLoader {
    ApplicationDetail load(String name, PSSecurityToken token);
  }

  private static final class DefaultApplicationLifecycleOps implements ApplicationLifecycleOps {
    @Override
    public boolean isActive(String appName) {
      return PSServer.isApplicationActive(appName);
    }

    @Override
    public void start(String appName) throws Exception {
      PSServer.startApplication(appName);
    }

    @Override
    public boolean stop(String appName) {
      return PSServer.shutdownApplication(appName);
    }
  }
}
