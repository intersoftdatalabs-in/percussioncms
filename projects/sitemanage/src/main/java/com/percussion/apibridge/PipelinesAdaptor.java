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

import com.percussion.design.objectstore.server.PSApplicationSummary;
import com.percussion.design.objectstore.server.PSServerXmlObjectStore;
import com.percussion.rest.pipelines.ApplicationSummary;
import com.percussion.rest.pipelines.IPipelinesAdaptor;
import com.percussion.security.PSSecurityToken;
import com.percussion.server.PSRequest;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.system.utils.PSSiteManageBean;
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
    this(
        tok ->
            PSServerXmlObjectStore.getInstance().getApplicationSummaryObjects(tok, false));
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

  /**
   * Pure mapping path used by production and unit tests (no object-store singleton).
   */
  static List<ApplicationSummary> mapFilterSortLimit(
      PSApplicationSummary[] sums, String nameFilter, int limit, int offset) {
    int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
    int safeOffset = Math.max(0, offset);
    String q =
        StringUtils.isBlank(nameFilter) ? null : nameFilter.trim().toLowerCase(Locale.ROOT);

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
            ApplicationSummary::getName,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    if (safeOffset >= out.size()) {
      return List.of();
    }
    int end = Math.min(out.size(), safeOffset + safeLimit);
    return new ArrayList<>(out.subList(safeOffset, end));
  }

  static boolean matchesNameFilter(ApplicationSummary dto, String qLower) {
    String name = dto.getName() != null ? dto.getName().toLowerCase(Locale.ROOT) : "";
    String desc =
        dto.getDescription() != null ? dto.getDescription().toLowerCase(Locale.ROOT) : "";
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
