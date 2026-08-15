/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

// REFACTORED: CP-JAVA11

package com.percussion.rest.folders;

import com.percussion.rest.MoveFolderItem;
import com.percussion.rest.Status;
import com.percussion.rest.errors.BackendException;
import com.percussion.rest.errors.FolderNotFoundException;
import com.percussion.rest.errors.LocationMismatchException;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * REST resource for Folder and Section operations. Sunny Sal: "Folders ka resource, operations ka
 * force!"
 */
@PSSiteManageBean(value = "restFoldersResource")
@Path("/folders")
@XmlRootElement
@Tag(name = "Folders", description = "Folder and Section operations")
public class FoldersResource {

  private final Pattern p = Pattern.compile("^\\/?([^\\/]+)(\\/(.*?))??(\\/([^\\/]+))?$");
  private static final Logger log = LogManager.getLogger(FoldersResource.class);

  private final IFolderAdaptor folderAdaptor;

  @Context private UriInfo uriInfo;

  /* package */ void setUriInfo(UriInfo ui) {
    this.uriInfo = ui;
  }

  @Autowired
  public FoldersResource(IFolderAdaptor adaptor) {
    this.folderAdaptor = adaptor;
  }

  /**
   * Get the specified folder by it's guid
   *
   * @param guid the guid of the folder
   * @return the folder with the specified guid
   */
  @GET
  @Path("/{guid}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get the specified folder by it's guid",
      responses = {
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Folder.class)))
      })
  public Folder getFolderById(@PathParam("guid") String guid) {
    try {
      return folderAdaptor.getFolder(uriInfo.getBaseUri(), guid);
    } catch (BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /**
   * Retrieve folder by Path
   *
   * @param path the path from the site to the folder
   * @return the folder at the specified path
   */
  @GET
  @Path("/by-path/{folderpath:.+}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Retrieve folder by Path",
      description =
          "Get folder with site name path and folder name.<br/> Simply send a GET request using the"
              + " site name, path to the folder, and folder name in the URL.<br/> Example URL:"
              + " http://localhost:9992/Rhythmyx/rest/folders/by-path/MySite/FolderA/FolderB/MyFolder"
              + " .<br/> <p> To work with Asset folders, replace MySite with Assets in the path,"
              + " for example: http://localhost:9992/Rhythmyx/rest/folders/by-path/Assets/uploads",
      responses = {
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Folder.class)))
      })
  public Folder getFolder(
      @Parameter(description = "The path from the site to the folder.", name = "folderpath")
          @PathParam("folderpath")
          String path) {
    // Path param should be url decoded by default.  CXF jars interacting when running in cm1
    try {
      path = java.net.URLDecoder.decode(path, "UTF-8");

      Matcher m = p.matcher(path);
      String siteName = "";
      String folderName = "";
      String apiPath = "";
      if (m.matches()) {
        siteName = StringUtils.defaultString(m.group(1));
        apiPath = StringUtils.defaultString(m.group(3));
        folderName = StringUtils.defaultString(m.group(5));
      }

      return folderAdaptor.getFolder(uriInfo.getBaseUri(), siteName, apiPath, folderName);
    } catch (FolderNotFoundException e) {
      throw new WebApplicationException(e.getMessage(), 404);
    } catch (BackendException | UnsupportedEncodingException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /**
   * Update or create the folder
   *
   * @param folder The folder to create or update.
   * @param path the path from the site to the folder
   * @return The updated or created folder representation.
   */
  @PUT
  @Path("/by-path/{folderpath:.+}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Create or update folder below root of site",
      description =
          "Create or update folder using site name, path, and folder name.<br/> Simply send a PUT"
              + " request using the site name, the path to the folder, and folder name in the URL"
              + " along with a JSON payload of the folder.<br/> <b>Note:</b> When sending a PUT"
              + " request do not include the id field. <br/> Example URL:"
              + " http://localhost:9992/Rhythmyx/rest/folders/by-path/MySite/FolderA/FolderB/MyFolder"
              + " .",
      responses = {
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Folder.class)))
      })
  public Folder updateFolder(
      @Parameter(description = "The body containing a JSON payload", name = "body") Folder folder,
      @Parameter(description = "The path from the site to the folder.", name = "folderpath")
          @PathParam("folderpath")
          String path) {
    // Path param should be url decoded by default.  CXF jars interacting when running in cm1
    try {
      path = java.net.URLDecoder.decode(path, "UTF-8");

      Matcher m = p.matcher(path);
      String siteName = "";
      String folderName = "";
      if (m.matches()) {
        siteName = StringUtils.defaultString(m.group(1));
        folderName = StringUtils.defaultString(m.group(5));
      }

      String objectName = folder.getName();
      String objectPath = folder.getPath();
      String objectSite = folder.getSiteName();

      if (objectName != null && !objectName.equals(folderName)) {
        throw new LocationMismatchException();
      }

      if (objectSite != null && !objectSite.equals(siteName)) {
        throw new LocationMismatchException();
      }
      folder.setName(folderName);
      folder.setPath(objectPath);
      folder.setSiteName(siteName);

      folder = folderAdaptor.updateFolder(uriInfo.getBaseUri(), folder);

      return folder;
    } catch (BackendException | UnsupportedEncodingException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /**
   * Delete a folder item below root of site
   *
   * @param itempath the path to the item
   * @return status of the delete operation
   */
  @DELETE
  @Path("/item/{itempath:.+}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Delete a folder item below root of site",
      description =
          "Delete a folder item below the first level of the site.<br/> Simple send a DELETE"
              + " request using the site name, path to the folder, and the folder name.<br/><br/>"
              + " Example URL:"
              + " http://localhost:9992/Rhythmyx/rest/folders/item/MySite/FolderA/FolderB/MyFolder/myitem.html"
              + " .",
      responses = {
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Status.class)))
      })
  public Status deleteFolderItem(@PathParam(value = "itempath") String itempath) {
    Status ret = new Status(500, "Error");

    try {
      itempath = java.net.URLDecoder.decode(itempath, "UTF-8");

      folderAdaptor.deleteFolderItem(uriInfo.getBaseUri(), itempath);

      ret.setMessage("Ok");
      ret.setStatusCode(200);

      return ret;
    } catch (BackendException | UnsupportedEncodingException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /**
   * Delete a folder below root of site
   *
   * @param path the path from the site to the folder
   * @param includeSubFolders boolean to delete subfolders along with the folder
   * @return status of the delete operation
   */
  @DELETE
  @Path("/by-path/{folderpath:.+}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Delete a folder below root of site",
      description =
          "Delete a folder below the first level of the site.<br/> Simple send a DELETE request"
              + " using the site name, path to the folder, and the folder name.<br/> <b>Note:</b>"
              + " If the folder has subfolders then to delete the request must include the"
              + " <b>\"includeSubFolders\" : \"True\"</b> header. <br/> Example URL:"
              + " http://localhost:9992/Rhythmyx/rest/folders/by-path/MySite/FolderA/FolderB/MyFolder"
              + " .",
      responses = {
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Status.class)))
      })
  public Status deleteFolder(
      @Parameter(description = "The path from the site to the folder.", name = "folderpath")
          @PathParam("folderpath")
          String path,
      @Parameter(
              description = "Boolean to delete subfolders along with the folder.",
              name = "includeSubFolders")
          @DefaultValue("false")
          @QueryParam("includeSubFolders")
          boolean includeSubFolders) {
    // Path param should be url decoded by default.  CXF jars interacting when running in cm1
    try {
      path = java.net.URLDecoder.decode(path, "UTF-8");

      Matcher m = p.matcher(path);
      String siteName = "";
      String folderName = "";
      String apiPath = "";
      if (m.matches()) {
        siteName = StringUtils.defaultString(m.group(1));
        apiPath = StringUtils.defaultString(m.group(3));
        folderName = StringUtils.defaultString(m.group(5));
      }

      folderAdaptor.deleteFolder(
          uriInfo.getBaseUri(), siteName, apiPath, folderName, includeSubFolders);
      return new Status(200, "Deleted");
    } catch (BackendException | UnsupportedEncodingException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), 500);
    } catch (FolderNotFoundException e) {
      throw new WebApplicationException("Folder not found", 404);
    }
  }

  /**
   * Moves the specified item in the MoveFolderItem request to the target path.
   *
   * @param moveRequest the request containing item and target folder paths
   * @return status of the move operation
   */
  @POST
  @Path("/move/item")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary =
          "Moves the specified item in the MoveFolderItem request to the target path.  Path should"
              + " include the full path to the item and folder, for example"
              + " /Sites/MySite/MyFolder/MyPage",
      responses = {
        @ApiResponse(responseCode = "404", description = "Item not found"),
        @ApiResponse(
            responseCode = "200",
            description = "Moved OK",
            content = @Content(schema = @Schema(implementation = Status.class)))
      })
  public Status moveFolderItem(MoveFolderItem moveRequest) {
    try {
      folderAdaptor.moveFolderItem(
          uriInfo.getBaseUri(), moveRequest.getItemPath(), moveRequest.getTargetFolderPath());
      return new Status("Moved OK");
    } catch (BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /**
   * Moves the specified Folder in the MoveFolderItem request to the target path.
   *
   * @param moveRequest the request containing source and target folder paths
   * @return status of the move operation
   */
  @POST
  @Path("/move/folder")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary =
          "Moves the specified Folder in the MoveFolderItem request to the target path.  Path"
              + " should include the full path to the source Folder and Target folder, for example"
              + " /Sites/MySite/MyFolder/MySubFolder",
      responses = {
        @ApiResponse(responseCode = "404", description = "Item not found"),
        @ApiResponse(
            responseCode = "200",
            description = "Moved OK",
            content = @Content(schema = @Schema(implementation = Status.class)))
      })
  public Status moveFolder(MoveFolderItem moveRequest) {
    try {
      folderAdaptor.moveFolderItem(
          uriInfo.getBaseUri(), moveRequest.getItemPath(), moveRequest.getTargetFolderPath());
      return new Status("Moved OK");
    } catch (BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /**
   * Copies the specified item in the CopyFolderItemRequest request to the target path.
   *
   * @param request the request containing item and target folder paths
   * @return status of the copy operation
   */
  @POST
  @Path("/copy/item")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary =
          "Copies the specified item in the CopyFolderItemRequest request to the target path.  Path"
              + " should include the full path to the item and folder, for example"
              + " /Sites/MySite/MyFolder/MyPage",
      responses = {
        @ApiResponse(responseCode = "404", description = "Item not found"),
        @ApiResponse(
            responseCode = "200",
            description = "Copied OK",
            content = @Content(schema = @Schema(implementation = Status.class))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Status copyFolderItem(CopyFolderItemRequest request) {
    try {
      folderAdaptor.copyFolderItem(
          uriInfo.getBaseUri(), request.getItemPath(), request.getTargetFolderPath());
      return new Status(200, "Copied OK");
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /**
   * Moves the specified Folder in the CopyFolderItem request to the target path.
   *
   * @param request the request containing source and target folder paths
   * @return status of the copy operation
   */
  @POST
  @Path("/copy/folder")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary =
          "Moves the specified Folder in the CopyFolderItem request to the target path.  Path"
              + " should include the full path to the folder, for example /Sites/MySite/MyFolder",
      responses = {
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(
            responseCode = "200",
            description = "Copied OK",
            content = @Content(schema = @Schema(implementation = Status.class))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Status copyFolder(CopyFolderItemRequest request) {
    try {
      folderAdaptor.copyFolder(
          uriInfo.getBaseUri(), request.getItemPath(), request.getTargetFolderPath());
      return new Status(200, "Copied OK");
    } catch (NotFoundException nfe) {
      return new Status(404, "Not Found");
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /**
   * Rename the specified Folder.
   *
   * @param path the path to the folder
   * @param newName the new name for the folder
   * @return the renamed folder
   */
  @POST
  @Path("/rename/{folderPath:.+}/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Rename the specified Folder.",
      description = "Renames the Folder at the given path.",
      responses = {
        @ApiResponse(responseCode = "404", description = "Folder not found"),
        @ApiResponse(
            responseCode = "200",
            description = "Update OK",
            content = @Content(schema = @Schema(implementation = Folder.class)))
      })
  public Folder renameFolder(
      @PathParam("folderPath") String path, @PathParam("name") String newName) {
    // Path param should be url decoded by default.  CXF jars interacting when running in cm1
    try {
      path = java.net.URLDecoder.decode(path, "UTF-8");

      Matcher m = p.matcher(path);
      String siteName = "";
      String folderName = "";
      String apiPath = "";

      if (m.matches()) {
        siteName = StringUtils.defaultString(m.group(1));
        apiPath = StringUtils.defaultString(m.group(3));
        folderName = StringUtils.defaultString(m.group(5));
      }

      return folderAdaptor.renameFolder(
          uriInfo.getBaseUri(), siteName, apiPath, folderName, newName);
    } catch (UnsupportedEncodingException | BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }
}
