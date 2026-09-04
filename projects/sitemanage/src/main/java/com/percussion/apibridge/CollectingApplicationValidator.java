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

import com.percussion.design.objectstore.IPSComponent;
import com.percussion.design.objectstore.PSApplication;
import com.percussion.design.objectstore.PSDataSet;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.design.objectstore.server.IPSObjectStoreHandler;
import com.percussion.design.objectstore.server.PSValidatorAdapter;
import com.percussion.rest.pipelines.ApplicationValidationProblem;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Collecting peer of {@link PSValidatorAdapter} for Admin pipeline validation/problems.
 *
 * <p>Base adapter either throws on the first error or discards issues when {@code
 * throwOnErrors(false)}. This subclass keeps going and accumulates ERROR / WARNING problems for the
 * REST summary payload.
 */
final class CollectingApplicationValidator extends PSValidatorAdapter {

  static final String SEVERITY_ERROR = "ERROR";
  static final String SEVERITY_WARNING = "WARNING";

  private final List<ApplicationValidationProblem> problems = new ArrayList<>();

  CollectingApplicationValidator(IPSObjectStoreHandler osHandler) {
    super(osHandler);
    throwOnErrors(false);
    treatWarningsAsErrors(false);
  }

  List<ApplicationValidationProblem> getProblems() {
    return List.copyOf(problems);
  }

  @Override
  public void validationWarning(IPSComponent component, int errorCode, Object[] args)
      throws PSSystemValidationException {
    problems.add(toProblem(SEVERITY_WARNING, errorCode, args, component));
  }

  @Override
  public void validationError(IPSComponent component, int errorCode, Object[] args)
      throws PSSystemValidationException {
    problems.add(toProblem(SEVERITY_ERROR, errorCode, args, component));
    // Do not throw — collect all issues for the Admin problems summary.
  }

  ApplicationValidationProblem toProblem(
      String severity, int errorCode, Object[] args, IPSComponent component) {
    PSSystemValidationException ex =
        new PSSystemValidationException(errorCode, args, getContainer(), component);
    ApplicationValidationProblem problem = new ApplicationValidationProblem();
    problem.setSeverity(severity);
    problem.setCode(Integer.toString(errorCode));
    String message = ex.getMessage();
    problem.setMessage(StringUtils.isNotBlank(message) ? message : "Validation issue " + errorCode);
    problem.setResource(resolveResourceName(component));
    problem.setPath(buildComponentPath(component));
    return problem;
  }

  String resolveResourceName(IPSComponent component) {
    if (component instanceof PSDataSet ds && StringUtils.isNotBlank(ds.getName())) {
      return ds.getName();
    }
    for (Object parent : getParentList()) {
      if (parent instanceof PSDataSet ds && StringUtils.isNotBlank(ds.getName())) {
        return ds.getName();
      }
    }
    if (getContainer() instanceof PSApplication app && StringUtils.isNotBlank(app.getName())) {
      return app.getName();
    }
    return null;
  }

  String buildComponentPath(IPSComponent component) {
    StringBuilder sb = new StringBuilder();
    for (Object parent : getParentList()) {
      if (parent instanceof IPSComponent pc) {
        appendSegment(sb, pc);
      }
    }
    if (component != null) {
      appendSegment(sb, component);
    }
    return sb.length() == 0 ? null : sb.toString();
  }

  private static void appendSegment(StringBuilder sb, IPSComponent component) {
    if (sb.length() > 0) {
      sb.append('/');
    }
    sb.append(component.getClass().getSimpleName());
    int id = component.getId();
    if (id != 0) {
      sb.append('#').append(id);
    }
    if (component instanceof PSDataSet ds && StringUtils.isNotBlank(ds.getName())) {
      sb.append('[').append(ds.getName()).append(']');
    }
  }
}
