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
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.util.PSCollection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Lists classic XML Applications (pipeline packages) visible to the current security token.
 *
 * <p>Uses {@link PSServerXmlObjectStore} for summaries; mapping/filter/limit are pure helpers so
 * they can be unit-tested without the object-store singleton.
 */
@PSSiteManageBean
public class PipelinesAdaptor implements IPipelinesAdaptor {

  private static final Logger log = LogManager.getLogger(PipelinesAdaptor.class);

  /** Default page size when callers pass non-positive limit. */
  public static final int DEFAULT_LIMIT = 500;

  /** Hard cap to avoid unbounded payloads on large servers. */
  public static final int MAX_LIMIT = 1000;

  private final Function<PSSecurityToken, PSApplicationSummary[]> summaryLoader;

  public PipelinesAdaptor() {
    this(tok -> PSServerXmlObjectStore.getInstance().getApplicationSummaryObjects(tok, false));
  }

  /** Package-visible for unit tests that inject a fake summary source. */
  PipelinesAdaptor(Function<PSSecurityToken, PSApplicationSummary[]> summaryLoader) {
    this.summaryLoader = summaryLoader;
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
    try {
      // fixupCeFields=false: catalog/detail only; avoid CE field rewrite cost
      PSApplication app =
          PSServerXmlObjectStore.getInstance().getApplicationObject(name, tok, false);
      if (app == null) {
        return null;
      }
      return toDetail(app);
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

    List<String> gaps = new ArrayList<>();
    gaps.add("Pipe IR / SQL mapper / resource tanks not exposed (Pipelines Slice A+)");
    gaps.add("Start / stop / enable application not supported via this API");
    gaps.add("Classic application import/export not supported via this API");
    d.setDesignGaps(gaps);
    return d;
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
    dto.setAppRoot(sum.getAppRoot());
    if (sum.getAppType() != null) {
      dto.setAppType(sum.getAppType().name());
    }
    dto.setVersion(sum.getVersion());
    dto.setEmpty(sum.isEmpty());
    dto.setHidden(sum.isHidden());
    return dto;
  }
}
