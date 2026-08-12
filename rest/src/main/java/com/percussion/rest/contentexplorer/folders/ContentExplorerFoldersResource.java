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

package com.percussion.rest.contentexplorer.folders;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import java.net.URI;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Public REST façade for Rhythmyx folder operations used by Content Explorer and integrators
 * (#3073 / parent #3054).
 *
 * <pre>
 *   GET    /Rhythmyx/rest/content-explorer/folders/by-path/{path}
 *   GET    /Rhythmyx/rest/content-explorer/folders/by-id/{id}
 *   GET    .../children | .../child-folders
 *   POST   /Rhythmyx/rest/content-explorer/folders
 *   POST   /Rhythmyx/rest/content-explorer/folders/tree
 *   PUT    /Rhythmyx/rest/content-explorer/folders/by-id/{id}
 *   POST   .../move-children | add-children | remove-children
 *   DELETE /Rhythmyx/rest/content-explorer/folders/by-id/{id}?purge=
 * </pre>
 *
 * <p>Thin HTTP layer over classic {@code IPSContentWs} folder methods (same domain path as SOAP
 * content folder ops). Does <strong>not</strong> replace CM1 pathmanagement browse/pagination or
 * the site-centric {@code /folders} resource.
 *
 * <p><strong>Out of scope:</strong> Explorer WebUI dual-run switch (#3074).
 */
@PSSiteManageBean(value = "restContentExplorerFoldersResource")
@Path("/content-explorer/folders")
@Tag(
    name = "Content Explorer Folders",
    description =
        "Rhythmyx folder ops façade over IPSContentWs (//Folders / //Sites paths; not CM1 site"
            + " FoldersResource)")
public class ContentExplorerFoldersResource {

  private static final Logger log = LogManager.getLogger(ContentExplorerFoldersResource.class);

  private final IContentExplorerFolderAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public ContentExplorerFoldersResource() {
    this.adaptor = null;
  }

  @Autowired
  public ContentExplorerFoldersResource(IContentExplorerFolderAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /** Package-private test hook so unit tests need not reflect on {@code uriInfo}. */
  void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  @GET
  @Path("/by-path/{path:.+}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Load folder by RX path",
      description =
          "Loads a folder via IPSContentWs.loadFolders(paths). Accepts //Folders/… and //Sites/…"
              + " forms; single-slash /Folders and /Sites are normalized. Use path '/' for root"
              + " folders (Folders + Sites).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = RxFolder.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path"),
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public RxFolder loadByPath(
      @Parameter(name = "path", required = true, description = "Fully qualified RX path")
          @PathParam("path")
          String path) {
    return call(
        () -> {
          RxFolder out = requireAdaptor().loadByPath(baseUri(), decodePath(path));
          if (out == null) {
            throw notFound("Folder not found");
          }
          return out;
        },
        "load folder by path");
  }

  @GET
  @Path("/by-id/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Load folder by id",
      description = "Loads a folder via IPSContentWs.loadFolder(guid).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = RxFolder.class))),
        @ApiResponse(responseCode = "400", description = "Invalid id"),
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public RxFolder loadById(
      @Parameter(name = "id", required = true, description = "Folder guid or content id")
          @PathParam("id")
          String id) {
    return call(
        () -> {
          RxFolder out = requireAdaptor().loadById(baseUri(), id);
          if (out == null) {
            throw notFound("Folder not found");
          }
          return out;
        },
        "load folder by id");
  }

  @GET
  @Path("/by-id/{id}/children")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List direct children by folder id",
      description = "IPSContentWs.findFolderChildren(id) — items and folders.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = RxFolderChildList.class))),
        @ApiResponse(responseCode = "400", description = "Invalid id"),
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public RxFolderChildList childrenById(@PathParam("id") String id) {
    return call(() -> requireAdaptor().findChildrenById(baseUri(), id), "list children by id");
  }

  @GET
  @Path("/by-path/{path:.+}/children")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List direct children by RX path",
      description = "IPSContentWs.findFolderChildren(path). Path '/' returns root Folders + Sites.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = RxFolderChildList.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path"),
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public RxFolderChildList childrenByPath(@PathParam("path") String path) {
    return call(
        () -> requireAdaptor().findChildrenByPath(baseUri(), decodePath(path)),
        "list children by path");
  }

  @GET
  @Path("/by-id/{id}/child-folders")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List direct child folders by id",
      description = "IPSContentWs.findChildFolders(id) — folders only.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = RxFolderChildList.class))),
        @ApiResponse(responseCode = "400", description = "Invalid id"),
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public RxFolderChildList childFoldersById(@PathParam("id") String id) {
    return call(
        () -> requireAdaptor().findChildFoldersById(baseUri(), id), "list child folders by id");
  }

  @GET
  @Path("/by-path/{path:.+}/child-folders")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List direct child folders by RX path",
      description = "Resolves path then IPSContentWs.findChildFolders.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = RxFolderChildList.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path"),
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public RxFolderChildList childFoldersByPath(@PathParam("path") String path) {
    return call(
        () -> requireAdaptor().findChildFoldersByPath(baseUri(), decodePath(path)),
        "list child folders by path");
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Add a folder under a parent path",
      description = "IPSContentWs.addFolder(name, parentPath[, sourcePath]).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created",
            content = @Content(schema = @Schema(implementation = RxFolder.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Not allowed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public RxFolder addFolder(AddFolderRequest body) {
    if (body == null) {
      throw badRequest("Request body is required.");
    }
    return call(() -> requireAdaptor().addFolder(baseUri(), body), "add folder");
  }

  @POST
  @Path("/tree")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Add a folder tree (missing path segments)",
      description = "IPSContentWs.addFolderTree(path).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Created folders (may be empty if path already exists)",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = RxFolder.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Not allowed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public List<RxFolder> addFolderTree(AddFolderTreeRequest body) {
    if (body == null) {
      throw badRequest("Request body is required.");
    }
    return call(() -> requireAdaptor().addFolderTree(baseUri(), body), "add folder tree");
  }

  @PUT
  @Path("/by-id/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Save folder name / description / community / locale / properties",
      description =
          "Loads the existing folder, applies non-null body fields, then IPSContentWs.saveFolder.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Saved",
            content = @Content(schema = @Schema(implementation = RxFolder.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "403", description = "Not allowed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public RxFolder saveFolder(@PathParam("id") String id, RxFolder body) {
    if (body == null) {
      throw badRequest("Request body is required.");
    }
    return call(() -> requireAdaptor().saveFolder(baseUri(), id, body), "save folder");
  }

  @POST
  @Path("/move-children")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Move folder children source → target",
      description = "IPSContentWs.moveFolderChildren.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Moved"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Not allowed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public Response moveChildren(FolderChildrenRequest body) {
    if (body == null) {
      throw badRequest("Request body is required.");
    }
    call(
        () -> {
          requireAdaptor().moveChildren(baseUri(), body);
          return null;
        },
        "move children");
    return Response.noContent().build();
  }

  @POST
  @Path("/add-children")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Add children to a parent folder",
      description = "IPSContentWs.addFolderChildren.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Added"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Not allowed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public Response addChildren(FolderChildrenRequest body) {
    if (body == null) {
      throw badRequest("Request body is required.");
    }
    call(
        () -> {
          requireAdaptor().addChildren(baseUri(), body);
          return null;
        },
        "add children");
    return Response.noContent().build();
  }

  @POST
  @Path("/remove-children")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Remove children from a parent folder",
      description = "IPSContentWs.removeFolderChildren (optional purge).",
      responses = {
        @ApiResponse(responseCode = "204", description = "Removed"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Not allowed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public Response removeChildren(FolderChildrenRequest body) {
    if (body == null) {
      throw badRequest("Request body is required.");
    }
    call(
        () -> {
          requireAdaptor().removeChildren(baseUri(), body);
          return null;
        },
        "remove children");
    return Response.noContent().build();
  }

  @DELETE
  @Path("/by-id/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Delete folder (recursive)",
      description = "IPSContentWs.deleteFolders(ids, purgeItems).",
      responses = {
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid id"),
        @ApiResponse(responseCode = "403", description = "Not allowed"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Server error")
      })
  public Response deleteFolder(
      @PathParam("id") String id,
      @Parameter(description = "When true, purge child items (admin). Default false.")
          @QueryParam("purge")
          @DefaultValue("false")
          boolean purge) {
    call(
        () -> {
          requireAdaptor().deleteFolder(baseUri(), id, purge);
          return null;
        },
        "delete folder");
    return Response.noContent().build();
  }

  private <T> T call(CheckedSupplier<T> action, String op) {
    try {
      return action.get();
    } catch (WebApplicationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(
          e, Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
    } catch (SecurityException e) {
      throw new WebApplicationException(
          e, Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build());
    } catch (Exception e) {
      log.error("Failed to {} ({}): {}", op, e.getClass().getName(), e.getMessage(), e);
      throw new WebApplicationException(
          e,
          Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Failed to " + op + ".")
              .build());
    }
  }

  private static String decodePath(String path) {
    if (path == null) {
      return null;
    }
    try {
      return java.net.URLDecoder.decode(path, java.nio.charset.StandardCharsets.UTF_8);
    } catch (RuntimeException e) {
      return path;
    }
  }

  private URI baseUri() {
    return uriInfo == null ? null : uriInfo.getBaseUri();
  }

  private IContentExplorerFolderAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Content explorer folders adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }

  private static WebApplicationException notFound(String msg) {
    return new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(msg).build());
  }

  private static WebApplicationException badRequest(String msg) {
    return new WebApplicationException(
        Response.status(Response.Status.BAD_REQUEST).entity(msg).build());
  }

  @FunctionalInterface
  private interface CheckedSupplier<T> {
    T get() throws Exception;
  }
}
