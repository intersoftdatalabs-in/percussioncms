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

package com.percussion.rest.pipelines;

import com.percussion.services.pipeline.model.PipelineBinaryPayload;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineRequestTrace;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Catalog of classic XML Applications (pipeline packages), Admin start/stop, validation/problems,
 * read-only Pipeline IR, OpenAPI from resources, and thin IR execute.
 *
 * <p>Registered via {@link PSSiteManageBean} like sibling catalog resources ({@code Keywords},
 * {@code Slots}). IR read uses {@code IPSPipelineIrService}; execute delegates to {@code
 * IPSPipelineRuntimeService} only (never classic {@code PSQueryHandler} as the public path).
 * Start/stop peer {@code PSServer.startApplication} / {@code shutdownApplication}. Validation
 * peers {@code PSValidatorAdapter#validateApplication}.
 */
@PSSiteManageBean(value = "restPipelinesResource")
@Path("/pipelines")
@XmlRootElement
@Tag(
    name = "Pipelines",
    description =
        "Data pipeline / XML application design catalog, Admin start/stop, validation, Pipeline IR, OpenAPI from resources, HTTP backend tank persist, nested filter groups, binary resource persist/retrieve, HTML result-page persist, request tracing, and IR execute")
public class PipelinesResource {

  private final IPipelinesAdaptor adaptor;

  @Context private UriInfo uriInfo;

  /**
   * No-arg constructor for bean-discovery edge cases. Production uses {@link
   * #PipelinesResource(IPipelinesAdaptor)}; invoking list methods without injection fails with a
   * clear diagnostic rather than a bare NPE.
   */
  public PipelinesResource() {
    this.adaptor = null;
  }

  @Autowired
  public PipelinesResource(IPipelinesAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List pipeline applications",
      description =
          "Lists non-hidden server applications (classic XML Applications) visible to the"
              + " current user. Supports optional name filter and limit/offset. Includes"
              + " runtime active flag. Read-only IR is GET /{idOrName}/ir; IR write / graph"
              + " editor remain later slices.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = ApplicationSummary.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<ApplicationSummary> listApplications(
      @QueryParam("name") String name,
      @QueryParam("limit") @DefaultValue("500") int limit,
      @QueryParam("offset") @DefaultValue("0") int offset) {
    try {
      IPipelinesAdaptor bridge = requireAdaptor();
      return bridge.listApplications(uriInfo.getBaseUri(), name, limit, offset);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      // Preserve cause; matches Keywords/Slots catalog resources
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{idOrName}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get pipeline application detail",
      description =
          "Loads one classic XML Application by name or numeric id. Includes data set catalog and"
              + " runtime active flag. Pipe IR read is GET /{idOrName}/ir; IR write / classic"
              + " import-export remain later slices (see designGaps). Admin start/stop are"
              + " separate POST endpoints.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ApplicationDetail.class))),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ApplicationDetail getApplication(@PathParam("idOrName") String idOrName) {
    try {
      ApplicationDetail detail = requireAdaptor().getApplication(uriInfo.getBaseUri(), idOrName);
      if (detail == null) {
        // Generic body: do not echo raw idOrName (path-injection / name probing).
        throw new WebApplicationException("Application not found", 404);
      }
      return detail;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Path("/{idOrName}/start")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Start a pipeline application",
      description =
          "Admin only. Starts a non-hidden classic XML Application via PSServer.startApplication."
              + " Idempotent when already running. Returns refreshed ApplicationDetail with"
              + " active=true. Does not echo raw path params on errors.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Started or already running",
            content = @Content(schema = @Schema(implementation = ApplicationDetail.class))),
        @ApiResponse(responseCode = "400", description = "Hidden, disabled, or invalid application"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ApplicationDetail startApplication(@PathParam("idOrName") String idOrName) {
    try {
      ApplicationDetail detail =
          requireAdaptor().startApplication(uriInfo.getBaseUri(), idOrName);
      if (detail == null) {
        throw new WebApplicationException("Application not found", 404);
      }
      return detail;
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @POST
  @Path("/{idOrName}/stop")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Stop a pipeline application",
      description =
          "Admin only. Stops a non-hidden classic XML Application via PSServer.shutdownApplication."
              + " Idempotent when already stopped. Returns refreshed ApplicationDetail with"
              + " active=false. Does not echo raw path params on errors.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Stopped or already stopped",
            content = @Content(schema = @Schema(implementation = ApplicationDetail.class))),
        @ApiResponse(responseCode = "400", description = "Hidden or invalid application"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ApplicationDetail stopApplication(@PathParam("idOrName") String idOrName) {
    try {
      ApplicationDetail detail = requireAdaptor().stopApplication(uriInfo.getBaseUri(), idOrName);
      if (detail == null) {
        throw new WebApplicationException("Application not found", 404);
      }
      return detail;
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{idOrName}/validation")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Validate a pipeline application (problems summary)",
      description =
          "Admin only. Runs design-time object-store validation for a non-hidden classic XML"
              + " Application (peer PSValidatorAdapter.validateApplication) and returns a problems"
              + " summary (severity, code, message, optional resource/path). Does not echo raw path"
              + " params on errors. Graph edit / IR write / classic ZIP import remain out of scope.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Validation summary (may be empty / valid)",
            content =
                @Content(schema = @Schema(implementation = ApplicationValidationResult.class))),
        @ApiResponse(responseCode = "400", description = "Hidden or invalid application"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public ApplicationValidationResult getValidation(@PathParam("idOrName") String idOrName) {
    try {
      ApplicationValidationResult result =
          requireAdaptor().getValidation(uriInfo.getBaseUri(), idOrName);
      if (result == null) {
        // Generic body: do not echo raw idOrName (path-injection / name probing).
        throw new WebApplicationException("Application not found", 404);
      }
      return result;
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Read-only Pipeline IR (pipeline-ir-v1): native IR file when present, otherwise a classic
   * object-store import into IR (not persisted).
   *
   * <p>Path is multi-segment so it does not collide with {@code GET /{idOrName}} catalog detail.
   */
  @GET
  @Path("/{idOrName}/ir")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get pipeline IR document",
      description =
          "Returns a read-only Pipeline IR document (app meta + resources with stage presence,"
              + " tanks, mapper mappings) for Developer UI structure views. Loads native IR when"
              + " present; otherwise imports from the classic XML Application without saving."
              + " IR write / graph editor / ZIP import-export remain unsupported.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = PipelineIrDocument.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Application or IR not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public PipelineIrDocument getPipelineIr(@PathParam("idOrName") String idOrName) {
    try {
      PipelineIrDocument ir = requireAdaptor().getPipelineIr(uriInfo.getBaseUri(), idOrName);
      if (ir == null) {
        // Generic body: do not echo raw idOrName (path-injection / name probing).
        throw new WebApplicationException("Pipeline IR not found", 404);
      }
      return ir;
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * OpenAPI 3 generated from the pipeline IR resources (native or classic import). Query {@code
   * format=yaml|json} (default yaml). Does not echo raw path params on errors.
   */
  @GET
  @Path("/{idOrName}/openapi")
  @Produces({
    MediaType.APPLICATION_JSON,
    PipelineOpenApiGenerator.MEDIA_TYPE_YAML,
    "text/yaml",
    "application/vnd.oai.openapi"
  })
  @Operation(
      summary = "Get OpenAPI 3 generated from pipeline resources",
      description =
          "Returns OpenAPI 3 documenting POST …/resources/{resource}/execute for each IR"
              + " resource (native IR or classic import preview). format=yaml (default) or"
              + " json. Unknown applications 404; unsafe names and hidden apps 400. Does not"
              + " echo raw path params. Does not publish to an external registry.",
      responses = {
        @ApiResponse(responseCode = "200", description = "OpenAPI 3 YAML or JSON"),
        @ApiResponse(responseCode = "400", description = "Hidden, unsafe, or invalid format"),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response getOpenApi(
      @PathParam("idOrName") String idOrName,
      @QueryParam("format") @DefaultValue("yaml") String format) {
    try {
      String fmt = PipelineOpenApiGenerator.normalizeFormat(format);
      Map<String, Object> spec = requireAdaptor().getOpenApi(uriInfo.getBaseUri(), idOrName);
      if (spec == null) {
        throw new WebApplicationException("Application not found", 404);
      }
      if (PipelineOpenApiGenerator.FORMAT_JSON.equals(fmt)) {
        return Response.ok(spec, MediaType.APPLICATION_JSON_TYPE).build();
      }
      return Response.ok(PipelineOpenApiGenerator.toYaml(spec), PipelineOpenApiGenerator.MEDIA_TYPE_YAML)
          .build();
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Thin Developer smoke invoke: POST JSON params/rows → pipeline runtime → {@link
   * PipelineExecuteResult} JSON.
   *
   * <p>Path is multi-segment so it does not collide with {@code GET /{idOrName}} catalog detail.
   */
  @POST
  @Path("/{app}/resources/{resource}/execute")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Execute a pipeline IR resource",
      description =
          "Runs a native pipeline IR resource via IPSPipelineRuntimeService (SQL adapter +"
              + " parameterized plans). Body is PipelineExecuteRequest ({params}, {rows}). Does not"
              + " call classic PSQueryHandler/PSUpdateHandler. Designer UI is out of scope.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = PipelineExecuteResult.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or unsupported resource"),
        @ApiResponse(responseCode = "404", description = "Application or resource not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public PipelineExecuteResult execute(
      @PathParam("app") String app,
      @PathParam("resource") String resource,
      PipelineExecuteRequest body) {
    try {
      return requireAdaptor().execute(uriInfo.getBaseUri(), app, resource, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Persist native IR HTTP backend tank (adapterType=HTTP + loopback/local fixture URL). Does not
   * rewrite classic XML Applications.
   */
  @PUT
  @Path("/{app}/resources/{resource}/backendTank")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Persist HTTP backend tank on a native pipeline resource",
      description =
          "Admin only. Saves native IR backendTank.adapterType=HTTP (or REST) and a"
              + " loopback/local fixture URL. Cloud hosts, credentials (userinfo), and non-http(s)"
              + " schemes are 400. Classic XML Applications stay read-only.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Saved",
            content = @Content(schema = @Schema(implementation = PipelineHttpBackendTank.class))),
        @ApiResponse(responseCode = "400", description = "Invalid URL, adapter, or name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public PipelineHttpBackendTank putHttpBackendTank(
      @PathParam("app") String app,
      @PathParam("resource") String resource,
      PipelineHttpBackendTank body) {
    try {
      return requireAdaptor().putHttpBackendTank(uriInfo.getBaseUri(), app, resource, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Persist native IR HTTP webhook pre/post execute hooks (loopback/local fixture URL). Does not
   * rewrite classic XML Applications. Missing URL is skip (no invented delivery).
   */
  @PUT
  @Path("/{app}/resources/{resource}/webhookHooks")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Persist HTTP webhook hooks on a native pipeline resource",
      description =
          "Admin only. Saves native IR webhook pre/post execute URLs (loopback/local fixture)."
              + " Cloud hosts, credentials (userinfo), and non-http(s) schemes are 400. Blank"
              + " URLs skip delivery on execute (documented skip, not a fake success). Classic"
              + " XML Applications stay read-only.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Saved",
            content = @Content(schema = @Schema(implementation = PipelineWebhookHooks.class))),
        @ApiResponse(responseCode = "400", description = "Invalid URL, method, or name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public PipelineWebhookHooks putWebhookHooks(
      @PathParam("app") String app,
      @PathParam("resource") String resource,
      PipelineWebhookHooks body) {
    try {
      return requireAdaptor().putWebhookHooks(uriInfo.getBaseUri(), app, resource, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Persist nested AND/OR selector filter groups on native IR. Does not rewrite classic XML
   * Applications. Malformed groups, leftover credentials, and non-local backends are 400.
   */
  @PUT
  @Path("/{app}/resources/{resource}/filterGroup")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Persist nested boolean filter groups on a native pipeline resource",
      description =
          "Admin only. Saves a nested AND/OR selector filter group on native IR. Malformed"
              + " groups, leftover HTTP credentials (userinfo), and non-local backends are 400."
              + " Classic XML Applications stay read-only.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Saved",
            content = @Content(schema = @Schema(implementation = PipelineFilterGroup.class))),
        @ApiResponse(responseCode = "400", description = "Invalid group, URL, or name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public PipelineFilterGroup putFilterGroup(
      @PathParam("app") String app,
      @PathParam("resource") String resource,
      PipelineFilterGroup body) {
    try {
      return requireAdaptor().putFilterGroup(uriInfo.getBaseUri(), app, resource, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Persist native IR binary resource (content type + portable-safe local fixture path). Does not
   * rewrite classic XML Applications. Cloud URLs, credentials, and traversal are 400.
   */
  @PUT
  @Path("/{app}/resources/{resource}/binaryResource")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Persist a native pipeline binary resource",
      description =
          "Admin only. Saves native IR binary resource path + content type. Path must be the"
              + " bundled local fixture token or a portable-safe relative path. Cloud URLs,"
              + " credentials (userinfo), and path traversal are 400. Classic XML Applications"
              + " stay read-only.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Saved",
            content = @Content(schema = @Schema(implementation = PipelineBinaryResource.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path, content type, or name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public PipelineBinaryResource putBinaryResource(
      @PathParam("app") String app,
      @PathParam("resource") String resource,
      PipelineBinaryResource body) {
    try {
      if (body == null) {
        throw new IllegalArgumentException("Binary resource path is required");
      }
      body.requireWriteFields();
      return requireAdaptor().putBinaryResource(uriInfo.getBaseUri(), app, resource, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Persist native IR result-page binding (stylesheet URI + request extension), or {@code
   * presentation=none} to disable/omit XSL. Does not rewrite classic XML Applications. Cloud
   * URLs, credentials, and traversal are 400.
   */
  @PUT
  @Path("/{app}/resources/{resource}/resultPage")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Persist a native pipeline result-page binding",
      description =
          "Admin only. Saves native IR result-page request extension (.html) + stylesheet URI,"
              + " or presentation=none to return raw JSON/XML (no XSL). URI must be the bundled"
              + " local fixture token or a portable-safe relative path. Cloud URLs, credentials"
              + " (userinfo), and path traversal are 400. Classic XML Applications stay"
              + " read-only. JSON (.json / Accept: application/json) never applies XSL even"
              + " when a result page remains.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Saved",
            content = @Content(schema = @Schema(implementation = PipelineResultPage.class))),
        @ApiResponse(responseCode = "400", description = "Invalid stylesheet URI, extension, presentation, or name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public PipelineResultPage putResultPage(
      @PathParam("app") String app,
      @PathParam("resource") String resource,
      PipelineResultPage body) {
    try {
      if (body == null) {
        throw new IllegalArgumentException("Result page stylesheet URI is required");
      }
      body.requireWriteFields();
      return requireAdaptor().putResultPage(uriInfo.getBaseUri(), app, resource, body);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Retrieve fixture bytes for a native BINARY resource. Missing/empty fixtures are 404 (not
   * invented content). Content-Type comes from IR.
   */
  @GET
  @Path("/{app}/resources/{resource}/binary")
  @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.WILDCARD})
  @Operation(
      summary = "Retrieve native pipeline binary fixture bytes",
      description =
          "Returns the local fixture bytes for a native BINARY resource. Content-Type is the"
              + " IR content type. Missing or empty fixtures are 404 (bytes are never invented)."
              + " Unsafe names are 400. Does not fetch from the internet.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Fixture bytes"),
        @ApiResponse(responseCode = "400", description = "Invalid name or path"),
        @ApiResponse(responseCode = "404", description = "Application, resource, or fixture not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response getBinary(@PathParam("app") String app, @PathParam("resource") String resource) {
    try {
      PipelineBinaryPayload payload =
          requireAdaptor().retrieveBinary(uriInfo.getBaseUri(), app, resource);
      if (payload == null || payload.getByteLength() == 0) {
        throw new WebApplicationException("Binary fixture not found", 404);
      }
      String type =
          payload.getContentType() != null && !payload.getContentType().isBlank()
              ? payload.getContentType()
              : MediaType.APPLICATION_OCTET_STREAM;
      return Response.ok(payload.getBytes()).type(type).build();
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Admin: enable or disable request tracing on a native pipeline application. Disabled tracing
   * clears last-trace (fail-closed).
   */
  @PUT
  @Path("/{idOrName}/tracing")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Enable or disable request tracing on a native pipeline",
      description =
          "Admin only. Persists native IR app.tracingEnabled. When disabled, last-trace is"
              + " cleared. Last-trace never includes passwords, tokens, or Authorization values."
              + " Classic XML Applications stay read-only.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Saved",
            content = @Content(schema = @Schema(implementation = PipelineTracingSettings.class))),
        @ApiResponse(responseCode = "400", description = "Invalid name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public PipelineTracingSettings putTracing(
      @PathParam("idOrName") String idOrName, PipelineTracingSettings body) {
    try {
      PipelineTracingSettings settings = body != null ? body : new PipelineTracingSettings();
      return requireAdaptor().putTracing(uriInfo.getBaseUri(), idOrName, settings);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /** Admin: current tracing on/off (defaults to false when native IR has no flag). */
  @GET
  @Path("/{idOrName}/tracing")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get request tracing on/off for a pipeline application",
      description = "Admin only. Returns tracingEnabled from native IR (false when unset).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = PipelineTracingSettings.class))),
        @ApiResponse(responseCode = "400", description = "Invalid name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public PipelineTracingSettings getTracing(@PathParam("idOrName") String idOrName) {
    try {
      PipelineTracingSettings settings =
          requireAdaptor().getTracing(uriInfo.getBaseUri(), idOrName);
      if (settings == null) {
        throw new WebApplicationException("Application not found", 404);
      }
      return settings;
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Admin: last fail-closed request trace after Test invoke with tracing enabled. 404 when none.
   */
  @GET
  @Path("/{idOrName}/lastTrace")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get last pipeline request trace",
      description =
          "Admin only. Returns stages and timings from the last execute while tracing was on."
              + " Passwords, tokens, and Authorization values are redacted. Result rows are never"
              + " copied. 404 when no last-trace exists.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = PipelineRequestTrace.class))),
        @ApiResponse(responseCode = "400", description = "Invalid name"),
        @ApiResponse(responseCode = "403", description = "Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application or last-trace not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public PipelineRequestTrace getLastTrace(@PathParam("idOrName") String idOrName) {
    try {
      PipelineRequestTrace trace = requireAdaptor().getLastTrace(uriInfo.getBaseUri(), idOrName);
      if (trace == null) {
        throw new WebApplicationException("Last request trace not found", 404);
      }
      return trace;
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  private IPipelinesAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new IllegalStateException(
          "Pipelines adaptor not configured (resource constructed without injection)");
    }
    return adaptor;
  }
}
