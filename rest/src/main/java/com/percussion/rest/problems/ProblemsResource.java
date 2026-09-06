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

package com.percussion.rest.problems;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Developer Problems catalog (Workbench §12.4).
 *
 * <p>Admin-only read-only list of validation/design problems for the open
 * editor/session. Distinct from pipeline {@code GET /pipelines/{id}/validation}.
 * Unsafe fixture tokens are 400 without echoing paths or JDBC URLs.
 */
@PSSiteManageBean(value = "restProblemsResource")
@Path("/problems")
@XmlRootElement
@Tag(
    name = "Problems",
    description = "Developer session design/validation problems (read-only; navigate-to-source)")
public class ProblemsResource {

  static final String INVALID_FIXTURE = "Invalid fixture";

  private final IProblemsAdaptor adaptor;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #ProblemsResource(IProblemsAdaptor)}; catalog methods call {@link #requireAdaptor()}.
   */
  public ProblemsResource() {
    this.adaptor = null;
  }

  @Autowired
  public ProblemsResource(IProblemsAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List Developer session design problems",
      description =
          "Admin. Lists validation/design problems for the open editor/session (Workbench"
              + " §12.4). Optional query fixture=invalid-session selects the known invalid"
              + " open-editor fixture. Responses contain catalog tokens only — never filesystem"
              + " paths or JDBC URLs. Distinct from /pipelines/{id}/validation.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = DesignProblem.class)))),
        @ApiResponse(responseCode = "400", description = "Unsafe or unknown fixture token"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<DesignProblem> listProblems(@QueryParam("fixture") String fixture) {
    try {
      List<DesignProblem> list = requireAdaptor().listProblems(fixture);
      return list != null ? list : List.of();
    } catch (RuntimeException e) {
      throw mapFailure(e);
    }
  }

  static WebApplicationException mapFailure(RuntimeException e) {
    if (e instanceof WebApplicationException wae) {
      return wae;
    }
    if (e instanceof IllegalArgumentException) {
      return new WebApplicationException(safeClientMessage(e.getMessage()), 400);
    }
    return new WebApplicationException(e, 500);
  }

  static String safeClientMessage(String message) {
    if (message == null || message.isBlank()) {
      return INVALID_FIXTURE;
    }
    if (looksLikeRawPath(message)) {
      return INVALID_FIXTURE;
    }
    return message;
  }

  /**
   * True when the message looks like a filesystem path, JDBC URL, or SQL fragment.
   * Adaptor messages such as {@code Invalid fixture} pass through.
   */
  static boolean looksLikeRawPath(String message) {
    String lower = message.toLowerCase();
    if (lower.contains("jdbc:") || lower.contains("password") || lower.contains("user=")) {
      return true;
    }
    if (message.contains("..")
        || message.indexOf('\\') >= 0
        || message.indexOf(';') >= 0
        || message.indexOf('\'') >= 0) {
      return true;
    }
    if (message.startsWith("/") || message.startsWith("~")) {
      return true;
    }
    if (message.length() >= 2
        && Character.isLetter(message.charAt(0))
        && message.charAt(1) == ':') {
      return true;
    }
    return false;
  }

  private IProblemsAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Problems adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }
}
