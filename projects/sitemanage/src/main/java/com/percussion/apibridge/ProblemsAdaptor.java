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

import com.percussion.rest.problems.DesignProblem;
import com.percussion.rest.problems.IProblemsAdaptor;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * Developer Problems (Workbench §12.4) — read-only session validation list.
 *
 * <p>This increment always includes the known invalid open-editor fixture
 * {@code invalid-session} so Admin Playwright and operators can see at least
 * one problem row with navigate-to-source to Content Types. Distinct from
 * pipeline application validation.
 */
@PSSiteManageBean
@Lazy
public class ProblemsAdaptor implements IProblemsAdaptor {

  private static final Logger log = LogManager.getLogger(ProblemsAdaptor.class);

  static final String ADMIN_REQUIRED = "Admin role required to list Problems";
  static final String INVALID_FIXTURE = "Invalid fixture";
  static final String UNKNOWN_FIXTURE = "Unknown fixture";

  /** Query/catalog token for the known invalid open-editor/session fixture. */
  public static final String INVALID_SESSION_FIXTURE = "invalid-session";

  static final String FIXTURE_PROBLEM_ID = "invalid-session";
  static final String FIXTURE_CODE = "FIXTURE";
  static final String FIXTURE_SEVERITY = "ERROR";
  static final String FIXTURE_OBJECT_TYPE = "content-types";
  static final String FIXTURE_OBJECT_ID = "perc-problems-fixture-invalid";
  static final String FIXTURE_OBJECT_NAME = "Invalid open editor (fixture)";
  static final String FIXTURE_LOCATION = "name";
  static final String FIXTURE_NAVIGATE = "content-types";
  static final String FIXTURE_MESSAGE = "Open editor is missing a required name.";

  private static final Pattern SAFE_FIXTURE = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,63}");

  private static final Set<String> NAVIGATE_SECTIONS =
      Set.of(
          "content-types",
          "templates",
          "slots",
          "keywords",
          "locales",
          "shared-fields",
          "system-def",
          "item-filters",
          "display-formats",
          "action-menus",
          "searches",
          "views",
          "extensions",
          "relationship-types",
          "workflows",
          "server-configs",
          "application-files",
          "file-explorer",
          "database-explorer",
          "ce-controls",
          "sites",
          "communities",
          "roles",
          "community-visibility",
          "pipelines",
          "preferences",
          "problems");

  private final BooleanSupplier adminChecker;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public ProblemsAdaptor() {
    this(null);
  }

  /** Package-visible for tests. */
  ProblemsAdaptor(BooleanSupplier adminChecker) {
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  @Override
  public List<DesignProblem> listProblems(String fixture) {
    requireAdmin();
    String token = normalizeFixture(fixture);
    if (token == null) {
      return sessionProblems();
    }
    if (!isSafeFixture(token)) {
      throw new IllegalArgumentException(INVALID_FIXTURE);
    }
    if (!INVALID_SESSION_FIXTURE.equals(token)) {
      throw new IllegalArgumentException(UNKNOWN_FIXTURE);
    }
    return sessionProblems();
  }

  private List<DesignProblem> sessionProblems() {
    List<DesignProblem> out = new ArrayList<>();
    out.add(invalidSessionFixture());
    return out;
  }

  static DesignProblem invalidSessionFixture() {
    DesignProblem row = new DesignProblem();
    row.setId(FIXTURE_PROBLEM_ID);
    row.setSeverity(FIXTURE_SEVERITY);
    row.setCode(FIXTURE_CODE);
    row.setMessage(FIXTURE_MESSAGE);
    row.setObjectType(FIXTURE_OBJECT_TYPE);
    row.setObjectId(FIXTURE_OBJECT_ID);
    row.setObjectName(FIXTURE_OBJECT_NAME);
    row.setLocation(FIXTURE_LOCATION);
    row.setNavigateSection(FIXTURE_NAVIGATE);
    return row;
  }

  static String normalizeFixture(String fixture) {
    if (fixture == null) {
      return null;
    }
    String trimmed = fixture.trim();
    return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
  }

  static boolean isSafeFixture(String fixture) {
    return fixture != null && SAFE_FIXTURE.matcher(fixture).matches();
  }

  static boolean isNavigateSection(String section) {
    return section != null && NAVIGATE_SECTIONS.contains(section);
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
