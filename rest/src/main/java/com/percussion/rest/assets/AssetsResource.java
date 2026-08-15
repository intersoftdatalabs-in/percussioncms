// REFACTORED: CP-JAVA11
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

package com.percussion.rest.assets;

import com.percussion.data.PSInternalRequestCallException;
import com.percussion.rest.Status;
import com.percussion.rest.errors.AssetNotFoundException;
import com.percussion.rest.errors.BackendException;
import com.percussion.rest.users.IUserAdaptor;
import com.percussion.rest.util.APIUtilities;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.workflow.PSWorkFlowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.springframework.beans.factory.annotation.Autowired;

@PSSiteManageBean(value = "restAssetResource")
@Path("/assets")
@XmlRootElement
@Tag(name = "Assets", description = "Operations related to Asset content types.")
public class AssetsResource {

  @Autowired private IAssetAdaptor assetAdaptor;

  @Autowired private IUserAdaptor userAdaptor;

  @Context private UriInfo uriInfo;

  /* package */ void setAssetAdaptor(IAssetAdaptor a) {
    this.assetAdaptor = a;
  }

  /* package */ void setUserAdaptor(IUserAdaptor u) {
    this.userAdaptor = u;
  }

  /* package */ void setUriInfo(UriInfo ui) {
    this.uriInfo = ui;
  }

  private final Pattern pathPattern = Pattern.compile("^/?([^/]+)(/(.*?))??(/([^/]+))?$");

  private static final String ALL_FILES_REPORT = "All Files";
  private static final String ALL_IMAGES_REPORT = "All Images";
  private static final String NON_ADA_COMPLAINT_FILES_REPORT = "Non ADA Complaint Files";
  private static final String NON_ADA_COMPLAINT_IMAGES_REPORT = "Non ADA Complaint Images";

  private static final Logger log = LogManager.getLogger(AssetsResource.class);

  private static class TikaConfigHolder {
    public static final TikaConfig INSTANCE = TikaConfig.getDefaultConfig();
  }

  private static TikaConfig getTikaConfig() {
    return TikaConfigHolder.INSTANCE;
  }

  @GET
  @Path("/by-path/{assetpath:.+}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary =
          "Get asset metadata at specified path. Get asset with path e.g. Assets/uploads/file1.jpg",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Asset.class))),
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Asset findByPath(@PathParam("assetpath") String path) {
    try {
      path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
      return assetAdaptor.getSharedAssetByPath(uriInfo.getBaseUri(), path);
    } catch (UnsupportedEncodingException e) {
      // UTF-8 always supported
      log.error("UTF-8 encoding not supported: {}", e.getMessage());
      throw new WebApplicationException(e);
    } catch (AssetNotFoundException e) {
      throw new WebApplicationException(404);
    }
  }

  @GET
  @Path("/import")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Operation(
      summary =
          "Previews an import option with the supplied options.  A CSV file is generated listing"
              + " the Assets that would be imported.",
      description =
          "Useful to run before running an import of Assets to determine the impact of the import."
              + " The report will also indicate the Asset type that files would be imported to,"
              + " currently Image or File assets are supported.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Response.class))),
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response importPreview(
      @QueryParam("osPath") String osPath,
      @QueryParam("assetPath") String assetPath,
      @QueryParam("replace") boolean replace,
      @QueryParam("onlyIfDifferent") boolean onlyIfDifferent,
      @QueryParam("autoApprove") boolean autoApprove) {
    try {
      var rows =
          assetAdaptor.previewAssetImport(
              uriInfo.getBaseUri(), osPath, assetPath, replace, onlyIfDifferent, autoApprove);
      var out = new PSCSVStreamingOutput(rows);
      var r =
          Response.ok(out)
              .header("Content-Type", "application/csv")
              .header(
                  "Content-Disposition",
                  "attachment; filename="
                      + APIUtilities.getReportFileName("asset-import-preview", "csv"));
      return r.build();
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.serverError().build();
    }
  }

  @POST
  @Path("/binary/{assetpath:.+}/{forceCheckOut}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(
      summary = "Creates a binary asset by uploading the binary",
      description =
          "Create a new asset by uploading a binary file. Asset type will be based upon the file"
              + " type.  Images will create image assets and everything else will create file"
              + " assets.  Optional assetType query parameter can be passed to override this"
              + " default (Options are file or image).",
      responses = {
        @ApiResponse(
            responseCode = "500",
            description = "Could not check out asset as it is checked out by another user."),
        @ApiResponse(
            responseCode = "200",
            description = "Update OK",
            content = @Content(schema = @Schema(implementation = Asset.class)))
      })
  public Asset uploadBinaryToAsset(
      @PathParam("assetpath") String path,
      @PathParam("forceCheckOut") Boolean forceCheckOut,
      @QueryParam("assetType") String assetType,
      List<Attachment> body)
      throws IOException {

    path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());

    if (body == null || body.isEmpty()) throw new WebApplicationException("No file sent", 400);

    var att = body.get(0);
    var uploadFilename = StringUtils.replace(att.getContentDisposition().getFilename(), "\\", "/");
    if (StringUtils.contains(uploadFilename, "/"))
      uploadFilename = StringUtils.substringAfterLast(uploadFilename, "/");

    var det = getTikaConfig().getDetector();
    try (var tis = TikaInputStream.get(att.getObject(InputStream.class))) {
      var metadata = new Metadata();
      var mimeType = det.detect(tis, metadata);
      var fileMimeType = mimeType.toString();
      return assetAdaptor.uploadBinary(
          uriInfo.getBaseUri(), path, assetType, tis, uploadFilename, fileMimeType, forceCheckOut);
    } catch (BackendException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/binary/{assetpath:.+}")
  @Operation(
      summary = "Retrieve a binary file.",
      description =
          "Get the binary for an image or file asset. Returns a jakarta.ws.rs.core.Response"
              + " object.",
      responses = {
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Response.class)))
      })
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response getBinary(@PathParam("assetpath") String path) {
    try {
      path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      // UTF-8 always supported
    }
    StreamingOutput out;
    try {
      out = assetAdaptor.getBinary(path);
    } catch (BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
    var asset = assetAdaptor.getSharedAssetByPath(uriInfo.getBaseUri(), path);
    var filename = StringUtils.substringAfter(path, "/");
    var thumbReq = filename.startsWith("thumb_");
    var type = "application/octet-stream";
    if (asset.getImage() != null && thumbReq) {
      var thumbType = asset.getThumbnail() != null ? asset.getThumbnail().getType() : null;
      if (thumbType != null) {
        type = thumbType;
      }
    } else if (asset.getFile() != null && asset.getFile().getType() != null) {
      type = asset.getFile().getType();
    } else if (asset.getImage() != null && asset.getImage().getType() != null) {
      type = asset.getImage().getType();
    }

    var r =
        Response.ok(out)
            .header("Content-Type", type)
            .header("Content-Disposition", "attachment; filename=" + filename);
    return r.build();
  }

  @PUT
  @Path("/by-path/{assetpath:.+}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Create or updates a shared Asset by path",
      description =
          "Creates a new shared Asset or updates the asset at a given path. As the"
              + " /binary/{path:.+} method cannot update binaries on existing assets, these types"
              + " should normally created with that method first. An example path would be"
              + " /Assets/MyFolder/MyAssetNameFields are type specific, it is useful to get an"
              + " existing item of the required type first to identify the available field names.",
      responses = {
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(
            responseCode = "200",
            description = "Update OK",
            content = @Content(schema = @Schema(implementation = Asset.class)))
      })
  public Asset upsertAssetByPath(Asset asset, @PathParam("assetpath") String path) {
    try {
      path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      // UTF-8 always supported
    }
    var filename = StringUtils.substringAfterLast(path, "/");
    asset.setName(filename);
    asset.setFolderPath(StringUtils.substringBeforeLast(path, "/"));
    try {
      return assetAdaptor.createOrUpdateSharedAsset(uriInfo.getBaseUri(), path, asset);
    } catch (BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @DELETE
  @Path("/by-path/{assetpath:.+}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Delete asset by path",
      description = "Delete asset by path",
      responses = {
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(
            responseCode = "200",
            description = "Delete OK",
            content = @Content(schema = @Schema(implementation = Status.class)))
      })
  public Status deleteSingleAsset(@PathParam("assetpath") String path) {
    try {
      path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      // UTF-8 always supported
    }
    try {
      return assetAdaptor.deleteSharedAssetByPath(path);
    } catch (AssetNotFoundException e) {
      return new Status(404, e.getMessage());
    } catch (BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      return new Status(500, e.getMessage());
    }
  }

  @POST
  @Path("/rename/{assetpath:.+}/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Rename the specified Asset.",
      description = "Renames the asset at the given path.",
      responses = {
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(
            responseCode = "200",
            description = "Update OK",
            content = @Content(schema = @Schema(implementation = Asset.class))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Asset renameAsset(@PathParam("assetpath") String path, @PathParam("name") String newName) {
    try {
      path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      // UTF-8 always supported
    }
    var m = pathPattern.matcher(path);
    var siteName = "";
    var assetName = "";
    var apiPath = "";
    if (m.matches()) {
      siteName = StringUtils.defaultString(m.group(1));
      apiPath = StringUtils.defaultString(m.group(3));
      assetName = StringUtils.defaultString(m.group(5));
    }
    try {
      return assetAdaptor.renameSharedAsset(
          uriInfo.getBaseUri(), siteName, apiPath, assetName, newName);
    } catch (BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  // --- Report Endpoints ---

  @GET
  @Path("/reports/non-ada-compliant-images")
  @Operation(
      summary =
          "Sends an email with a report in CSV format listing all Images in the system that have"
              + " detactable ADA Compliance issues to current users's email Id",
      description =
          "Current rules look for empty Alt Text, Empty title, Alt Text or Title with Filename, ",
      responses = {
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "202",
            description = "Request Accepted",
            content = @Content(schema = @Schema(implementation = Response.class)))
      })
  public Response nonADACompliantImagesReport() {
    log.debug("Generating Non ADA compliant images report");
    try {
      generateReport(NON_ADA_COMPLAINT_IMAGES_REPORT);
    } catch (PSErrorResultsException | PSInternalRequestCallException | BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.serverError().build();
    }
    return Response.accepted().status(202).build();
  }

  @GET
  @Path("/reports/non-ada-compliant-files")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Operation(
      summary =
          "Sends an email with a report in CSV format listing all File assets in the system that"
              + " have detactable ADA Compliance issues, to current users's email Id",
      description =
          "Current rules look for empty Alt Text, Empty title, Alt Text or Title with Filename, ",
      responses = {
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "202",
            description = "Request Accepted",
            content = @Content(schema = @Schema(implementation = Response.class)))
      })
  public Response nonadacompliantFilesReport() {
    log.debug("Generating Non ADA compliant files report");
    try {
      generateReport(NON_ADA_COMPLAINT_FILES_REPORT);
    } catch (PSErrorResultsException | PSInternalRequestCallException | BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.serverError().build();
    }
    return Response.accepted().status(202).build();
  }

  @GET
  @Path("/reports/all-images")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Operation(
      summary =
          "Sends an email with a report in CSV format listing all Images in the system, to current"
              + " users's email Id",
      description =
          "NOTE:  This report can take a very long time to run, on a system with a lot of images. "
              + " Be sure to set timeouts accordingly if requesting programmatically.",
      responses = {
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "202",
            description = "Request Accepted",
            content = @Content(schema = @Schema(implementation = Response.class)))
      })
  public Response allImagesReport() {
    log.debug("Generating all image report");
    try {
      generateReport(ALL_IMAGES_REPORT);
    } catch (PSErrorResultsException | PSInternalRequestCallException | BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.serverError().build();
    }
    return Response.accepted().status(202).build();
  }

  @GET
  @Path("/reports/all-files")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Operation(
      summary =
          "Sends an email with a report in CSV format listing all Files in the system to current"
              + " users's email Id",
      responses = {
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "202",
            description = "Request Accepted",
            content = @Content(schema = @Schema(implementation = Response.class)))
      })
  public Response allFilesReport() {
    log.debug("Generating All files report");
    try {
      generateReport(ALL_FILES_REPORT);
    } catch (PSErrorResultsException | PSInternalRequestCallException | BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.serverError().build();
    }
    return Response.accepted().status(202).build();
  }

  private void generateReport(String reportType)
      throws PSErrorResultsException, PSInternalRequestCallException, BackendException {
    synchronized (this) {
      var baseUri = uriInfo.getBaseUri();
      var requestThreadMap = new HashMap<>(PSRequestInfoBase.getRequestInfoMap());
      var currentUser = (String) PSRequestInfoBase.getRequestInfo(PSRequestInfo.KEY_USER);
      var user = userAdaptor.getUser(null, currentUser);
      var toAddress = user.getEmailAddress().orElse(null);
      CompletableFuture.runAsync(
          () -> {
            try {
              if (!PSRequestInfoBase.isInited()) {
                PSRequestInfoBase.initRequestInfo(requestThreadMap);
              }
              var report = getReportList(reportType, baseUri);
              PSCSVStreamingOutput out = null;
              if (report != null) {
                out = new PSCSVStreamingOutput(report);
              }
              sendMail(out, reportType, toAddress);
            } catch (Exception ex) {
              var errorStr = "Error occurred while generating " + reportType + " report";
              log.error(errorStr, ex);
              sendMail(errorStr, reportType, toAddress);
            }
          });
    }
  }

  private List<String> getReportList(String reportType, URI baseUri) throws BackendException {
    switch (reportType) {
      case ALL_FILES_REPORT:
        return assetAdaptor.allFilesReport(baseUri);
      case ALL_IMAGES_REPORT:
        return assetAdaptor.allImagesReport(baseUri);
      case NON_ADA_COMPLAINT_FILES_REPORT:
        return assetAdaptor.nonADACompliantFilesReport(baseUri);
      case NON_ADA_COMPLAINT_IMAGES_REPORT:
        return assetAdaptor.nonADACompliantImagesReport(baseUri);
      default:
        return Collections.emptyList();
    }
  }

  /** Sends email to given address with outputstream as file and given report name. */
  private void sendMail(Object out, String reportName, String toAddress) {
    File tempFile = null;
    var subject = "Report :" + reportName + " Generated";
    var mailMessage = "Please find attached the Report";
    try {
      if (out instanceof PSCSVStreamingOutput) {
        tempFile = Files.createTempFile(reportName, ".csv").toFile();
        try (var outputStream = new FileOutputStream(tempFile)) {
          ((PSCSVStreamingOutput) out).write(outputStream);
        }
      } else if (out instanceof String) {
        mailMessage = out + "\nPlease contact System Administrator";
      } else {
        mailMessage = "No Data found for report";
      }
      PSWorkFlowUtils.sendMailWithAttachment(tempFile, subject, mailMessage, toAddress);
    } catch (Exception e) {
      log.error("Error Sending Report Mail: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  // --- Bulk update endpoints ---

  @POST
  @Path("/reports/non-ada-compliant-images")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(
      summary = "Bulk updates File assets based on values in the report",
      responses = {
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Status.class)))
      })
  public Status bulkupdateNonADACompliantImages(List<Attachment> atts) {
    if (atts == null || atts.isEmpty()) throw new WebApplicationException("No file sent", 400);
    var att = atts.get(0);
    var stream = att.getObject(InputStream.class);
    return assetAdaptor.bulkupdateNonADACompliantImages(uriInfo.getBaseUri(), stream);
  }

  @POST
  @Path("/reports/non-ada-compliant-files")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(
      summary = "Bulk updates File assets based on values in the report",
      description =
          "Edit the CSV file generated by the GET on this resource, then post the changed file to"
              + " bulk update.",
      responses = {
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Status.class)))
      })
  public Status bulkupdateNonADACompliantFiles(List<Attachment> atts) {
    if (atts == null || atts.isEmpty()) throw new WebApplicationException("No file sent", 400);
    var att = atts.get(0);
    var stream = att.getObject(InputStream.class);
    return assetAdaptor.bulkupdateNonADACompliantFiles(uriInfo.getBaseUri(), stream);
  }

  @POST
  @Path("/reports/all-images")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(
      summary = "Bulk updates Image assets based on values in the report",
      description =
          "Edit the CSV file generated by the GET on this resource, then post the changed file to"
              + " bulk update.",
      responses = {
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Status.class)))
      })
  public Status bulkupdateImageAssets(List<Attachment> atts) {
    if (atts == null || atts.isEmpty()) throw new WebApplicationException("No file sent", 400);
    var att = atts.get(0);
    var stream = att.getObject(InputStream.class);
    return assetAdaptor.bulkupdateImageAssets(uriInfo.getBaseUri(), stream);
  }

  @POST
  @Path("/reports/all-files")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(
      summary = "Bulk updates File assets based on values in the report",
      description =
          "Edit the CSV file generated by the GET on this resource, then post the changed file to"
              + " bulk update.",
      responses = {
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Status.class)))
      })
  public Status bulkupdateFileAssets(List<Attachment> atts) {
    if (atts == null || atts.isEmpty()) throw new WebApplicationException("No file sent", 400);
    var att = atts.get(0);
    var stream = att.getObject(InputStream.class);
    return assetAdaptor.bulkupdateFileAssets(uriInfo.getBaseUri(), stream);
  }

  @POST
  @Path("/approve-all/{folderPath:.+}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary =
          "Approves every shared Asset on the system that are not in an Archive state and that live"
              + " in the specified folder.",
      description =
          "Passing the root folder will result in all Assets being approved.  Will override any"
              + " checkout status and approve the Assets as the method caller.",
      responses = {
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Status.class)))
      })
  public Status approveAllAssets(@PathParam("folderPath") String folder) {
    var status = new Status("OK");
    try {
      var ctr = assetAdaptor.approveAllAssets(uriInfo.getBaseUri(), folder);
      status.setMessage("Approved " + ctr + " Assets");
      return status;
    } catch (BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("/archive-all/{folderPath:.+}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Archives every shared Asset on the system that live in the specified folder.",
      description =
          "Passing the root folder will result in all Assets being Archived.  Will override any"
              + " checkout status and Archive the Assets as the method caller.",
      responses = {
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Status.class)))
      })
  public Status archiveAllAssets(@PathParam("folderPath") String folder) {
    var status = new Status("OK");
    try {
      var ctr = assetAdaptor.archiveAllAsets(uriInfo.getBaseUri(), folder);
      status.setMessage("Archived " + ctr + " Assets");
      return status;
    } catch (BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("/submit-all/{folderPath:.+}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary =
          "Submits every shared Asset on the system that lives in the specified folder to the"
              + " Review state.",
      description =
          "Passing the root folder will result in all Assets being submitted.  Will override any"
              + " checkout status and Submit the Assets as the method caller.",
      responses = {
        @ApiResponse(responseCode = "404", description = "Path not found"),
        @ApiResponse(responseCode = "500", description = "Error"),
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Status.class)))
      })
  public Status submitAllAssets(@PathParam("folderPath") String folder) {
    var status = new Status("OK");
    try {
      var ctr = assetAdaptor.submitForReviewAllAsets(uriInfo.getBaseUri(), folder);
      status.setMessage("Submitted " + ctr + " Assets");
      return status;
    } catch (BackendException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }
}
