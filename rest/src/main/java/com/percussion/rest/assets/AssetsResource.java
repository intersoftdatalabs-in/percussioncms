/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
import com.percussion.error.PSExceptionUtils;
import com.percussion.rest.Status;
import com.percussion.rest.errors.AssetNotFoundException;
import com.percussion.rest.errors.BackendException;
import com.percussion.rest.users.IUserAdaptor;
import com.percussion.rest.users.User;
import com.percussion.rest.util.APIUtilities;
import com.percussion.util.PSSiteManageBean;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.workflow.PSWorkFlowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PSSiteManageBean(value="restAssetResource")
@Path("/assets")
@XmlRootElement
@Tag(name = "Assets", description = "Operations related to Asset content types.")
@Lazy
public class AssetsResource
{
	@Autowired
    private IAssetAdaptor assetAdaptor;

    @Autowired
    private IUserAdaptor userAdaptor;


    @Context
    private UriInfo uriInfo;
    
    private final Pattern p = Pattern.compile("^/?([^/]+)(/(.*?))??(/([^/]+))?$");

    private static final String ALL_FILES_REPORT= "All Files";
    private static final String ALL_IMAGES_REPORT="All Images";
    private static final String NON_ADA_COMPLAINT_FILES_REPORT="Non ADA Complaint Files";
    private static final String NON_ADA_COMPLIANT_IMAGES_REPORT="Non ADA Complaint Images";

    private static final Logger log = LogManager.getLogger(AssetsResource.class);

    private static class TikaConfigHolder {
        public static final TikaConfig INSTANCE = TikaConfig.getDefaultConfig();
    }


    private static TikaConfig getTikaConfig(){
        return TikaConfigHolder.INSTANCE;
    }

    
    @GET
    @Path("/by-path/{assetpath:.+}")
    @Produces(
    {MediaType.APPLICATION_JSON})
    @Operation(summary = "Get asset metadata at specified path. Get asset with path e.g. Assets/uploads/file1.jpg",
            responses = {
                @ApiResponse(responseCode = "200", description = "OK", content =
                    @Content(schema=@Schema(implementation = Asset.class))),
                @ApiResponse(responseCode = "404", description = "Path not found"),
                @ApiResponse(responseCode = "500", description = "Error")
            })
    public Asset findByPath(@PathParam("assetpath") String path)
    {
        // Path param should be url decoded by default.  CXF jars interacting when running in cm1
        try
        {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            // UTF-8 always supported
        }
        try {
            return assetAdaptor.getSharedAssetByPath(uriInfo.getBaseUri(), path);
        }catch(AssetNotFoundException e){
            throw new WebApplicationException(404);
        }
    }

    @GET
    @Path("/import")
    @Produces(
    {MediaType.APPLICATION_OCTET_STREAM})
    @Operation(summary = "Previews an import option with the supplied options.  A CSV file is generated listing the Assets that would be imported.", description = "Useful to run before running an import of Assets to determine the impact of the import. The report will also indicate the Asset type that files would be imported to, currently Image, File, or Flash assets are supported.",
            responses ={
            @ApiResponse(responseCode="200", description = "OK", content=@Content(schema=
            @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "Path not found"),
            @ApiResponse(responseCode = "500", description = "Error")
            })
    public Response importPreview(@QueryParam("osPath") String osPath, 
    		@QueryParam("assetPath") String assetPath, @QueryParam("replace") boolean replace, @QueryParam("onlyIfDifferent") boolean onlyIfDifferent, @QueryParam("autoApprove") boolean autoApprove)
    {
    	PSCSVStreamingOutput out;
		List<String> rows;
	
		try {
			 rows = assetAdaptor.previewAssetImport(uriInfo.getBaseUri(), osPath, assetPath, replace, onlyIfDifferent, autoApprove);
			 out = new PSCSVStreamingOutput(rows);
		} catch (Exception e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
			return Response.serverError().build();
		}
        
        ResponseBuilder r = Response.ok(out);
        
        r.header("Content-Type", "application/csv");
        r.header("Content-Disposition", "attachment; filename=" + APIUtilities.getReportFileName("asset-import-preview","csv"));
  
        return r.build();
	}

    
    @POST
    @Path("/binary/{assetpath:.+}/{forceCheckOut}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Creates a binary asset by uploading the binary", description = "Create a new asset by uploading a binary file. "
            + "Asset type will be based upon the file type.  Images will create image assets, flash files will"
            + " create flash assets and everything else will create file assets.  Optional assetType query parameter can be passed "
            + "to override this default (Options are file, flash, or image).", responses=
            {
                    @ApiResponse(responseCode = "500", description = "Could not check out asset as it is checked out by another user."),
                    @ApiResponse(responseCode = "200", description = "Update OK", content=@Content(
                            schema=@Schema(implementation = Asset.class)
                    ))})
    public Asset uploadBinaryToAsset(
            @PathParam("assetpath") String path, @PathParam("forceCheckOut") Boolean forceCheckOut, @QueryParam("assetType") String assetType,
            List<Attachment> body) throws IOException
    {
       
        // Path param should be url decoded by default.  CXF jars interacting when running in cm1
        path = java.net.URLDecoder.decode(path, StandardCharsets.UTF_8.name());

        if (body == null || body.isEmpty())
            throw new RuntimeException("No file sent");
        Attachment att = body.get(0);

        String uploadFilename = att.getContentDisposition().getFilename();
        // Strip out any path portion of the uploaded filename as per rfc spec.
        uploadFilename = StringUtils.replace(uploadFilename, "\\", "/");

        if (StringUtils.contains(uploadFilename, "/"))
            uploadFilename = StringUtils.substringAfter(uploadFilename, "/");

        Detector det = getTikaConfig().getDetector();

        TikaInputStream tis = TikaInputStream.get(att.getObject(InputStream.class));

        Metadata metadata = new Metadata();

        org.apache.tika.mime.MediaType mimeType = det.detect(tis, metadata);
        String fileMimeType = mimeType.toString();

        try {
            return assetAdaptor.uploadBinary(uriInfo.getBaseUri(), path, assetType, tis,
                    uploadFilename, fileMimeType, forceCheckOut);
        } catch (BackendException e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/binary/{assetpath:.+}")
    @Operation(summary = "Retrieve a binary file.", 
            description = "Get the binary for an image, flash or file asset. " +
                    "Returns a javax.ws.rs.core.Response object.", 
            responses = {
                    @ApiResponse(responseCode = "404", description = "Asset not found"),
                    @ApiResponse(responseCode = "500", description = "Error"),
                    @ApiResponse(responseCode = "200", description = "OK", content=@Content(
                            schema=@Schema(implementation = Response.class)
                    ))
    })
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getBinary(@PathParam("assetpath") String path/*, @Context HttpServletRequest request*/)
    {
        // Path param should be url decoded by default.  CXF jars interacting when running in cm1
        try
        {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
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
        Asset asset = assetAdaptor.getSharedAssetByPath(uriInfo.getBaseUri(), path);

        ResponseBuilder r = Response.ok(out);
        String filename = StringUtils.substringAfter(path, "/");
        boolean thumbReq = filename.startsWith("thumb_");

        String type = "application/octet-stream";

        if (asset.getImage() != null && thumbReq)
            type = asset.getThumbnail().getType();
        else if (asset.getFile() != null)
            type = asset.getFile().getType();
        else if (asset.getImage() != null)
            type = asset.getImage().getType();
        else if (asset.getFlash() != null)
            type = asset.getFlash().getType();

        r.header("Content-Type", type);

        r.header("Content-Disposition", "attachment; filename=" + filename);
        return r.build();
    }

    @PUT
    @Path("/by-path/{assetpath:.+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create or updates a shared Asset by path", description = "Creates a new shared Asset or updates the asset at a given path. "
            + "As the /binary/{path:.+} method cannot update binaries on existing assets, these types should normally created with that method first. An example path would be /Assets/MyFolder/MyAssetName"
            + "Fields are type specific, it is useful to get an existing item of the required type first to identify the available field names.",
            responses = {
                @ApiResponse(responseCode = "404", description = "Asset not found"),
                @ApiResponse(responseCode = "200", description = "Update OK", content = @Content(
                        schema=@Schema(implementation = Asset.class)
                ))})
    public Asset upsertAssetByPath(Asset asset, @PathParam("assetpath") String path)
    {
        // Path param should be url decoded by default.  CXF jars interacting when running in cm1
        try
        {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            // UTF-8 always supported
        }
        
        String filename = StringUtils.substringAfterLast(path, "/");
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
    @Operation(summary = "Delete asset by path", description = "Delete asset by path",
            responses = {
                @ApiResponse(responseCode = "404", description = "Asset not found"),
                @ApiResponse(responseCode = "200", description = "Delete OK", content=@Content(
                        schema=@Schema(implementation = Status.class)))})
    public Status deleteSingleAsset(@PathParam("assetpath") String path)
    {
        // Path param should be url decoded by default.  CXF jars interacting when running in cm1
        try
        {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            // UTF-8 always supported
        }
        try {
            return assetAdaptor.deleteSharedAssetByPath(path);
        } catch(AssetNotFoundException e){
            return new Status(404,e.getMessage());
        } catch (BackendException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            return new Status(500,e.getMessage());
        }
    }
    
   
    
    @POST
    @Path("/rename/{assetpath:.+}/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Rename the specified Asset.",
            description = "Renames the asset at the given path.",
            responses = {
                @ApiResponse(responseCode = "404", description = "Asset not found"),
                @ApiResponse(responseCode = "200", description = "Update OK", content=@Content(
                        schema=@Schema(implementation = Asset.class)
                )),
                @ApiResponse(responseCode = "500", description = "Error")
    })
    public Asset renameAsset(@PathParam("assetpath") String path, @PathParam("name") String newName)
    {
   	 	// Path param should be url decoded by default.  CXF jars interacting when running in cm1
        try
        {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            // UTF-8 always supported
        }
        
        Matcher m = p.matcher(path);
        String siteName = "";
        String assetName = "";
        String apiPath = "";

        if(m.matches()) {
            siteName = StringUtils.defaultString(m.group(1));
            apiPath = StringUtils.defaultString(m.group(3));
            assetName = StringUtils.defaultString(m.group(5));
        }
        
        try {
            return assetAdaptor.renameSharedAsset(uriInfo.getBaseUri(), siteName, apiPath, assetName, newName);
        } catch (BackendException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }
    
    @GET
    @Path("/reports/non-ada-compliant-images")
    @Operation(summary = "Sends an email with a report in CSV format listing all Images in the system that have detactable ADA Compliance issues to current users's email Id",
            description = "Current rules look for empty Alt Text, Empty title, Alt Text or Title with Filename, ",
            responses = {
                @ApiResponse(responseCode = "404", description = "Path not found"),
                @ApiResponse(responseCode = "500", description = "Error"),
                @ApiResponse(responseCode = "202", description = "Request Accepted", content=@Content(
                        schema=@Schema(implementation = Response.class)
                ))
    })
    public Response nonADACompliantImagesReport()
    {
        // Added logger | CMS-3216
        if(log.isDebugEnabled()) {
            log.debug("Generating Non ADA compliant images report");
        }
        try{
            generateReport(NON_ADA_COMPLIANT_IMAGES_REPORT);
        } catch (PSErrorResultsException |PSInternalRequestCallException | BackendException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return Response.serverError().build();
        }
        return Response.accepted().build();
    }

    
    @GET
    @Path("/reports/non-ada-compliant-files")
    @Produces(
    {MediaType.APPLICATION_OCTET_STREAM})
    @Operation(summary = "Sends an email with a report in CSV format listing all File assets in the system that have detactable ADA Compliance issues, to current users's email Id",
            description = "Current rules look for empty Alt Text, Empty title, Alt Text or Title with Filename, ",
            responses = {
                    @ApiResponse(responseCode = "404", description = "Path not found"),
                    @ApiResponse(responseCode = "500", description = "Error"),
                    @ApiResponse(responseCode = "202", description = "Request Accepted", content=@Content(
                            schema=@Schema(implementation = Response.class)
                    ))
    })
    public Response nonadacompliantFilesReport()
    {
        // Added logger | CMS-3216
        if(log.isDebugEnabled()) {
            log.debug("Generating Non ADA compliant files report");
        }
        try{
            generateReport(NON_ADA_COMPLAINT_FILES_REPORT);
        } catch (PSErrorResultsException |PSInternalRequestCallException | BackendException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return Response.serverError().build();
        }
        return Response.accepted().build();
        
    }

    @GET
    @Path("/reports/all-images")
    @Produces(
    {MediaType.APPLICATION_OCTET_STREAM})
    @Operation(summary = "Sends an email with a report in CSV format listing all Images in the system, to current users's email Id",
            description = "NOTE:  This report can take a very long time to run, on a system with allot of images.  Be sure to set timeouts accordingly if requesting programatically.",
            responses = {
                    @ApiResponse(responseCode = "404", description = "Path not found"),
                    @ApiResponse(responseCode = "500", description = "Error"),
                    @ApiResponse(responseCode = "202", description = "Request Accepted", content=@Content(
                            schema=@Schema(implementation = Response.class)
                    ))
    })
    public Response allImagesReport()
    {
        // Added logger | CMS-3216
        if(log.isDebugEnabled()) {
            log.debug("Generating all image report");
        }
        try {
            generateReport(ALL_IMAGES_REPORT);
        } catch (PSErrorResultsException |PSInternalRequestCallException | BackendException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return Response.serverError().build();
        }
        return Response.accepted().build();
    }


    @GET
    @Path("/reports/all-files")
    @Produces(
    {MediaType.APPLICATION_OCTET_STREAM})
    @Operation(summary = "Sends an email with a report in CSV format listing all Files in the system to current users's email Id",
            responses = {
                    @ApiResponse(responseCode = "404", description = "Path not found"),
                    @ApiResponse(responseCode = "500", description = "Error"),
                    @ApiResponse(responseCode = "202", description = "Request Accepted", content=@Content(
                            schema=@Schema(implementation = Response.class)
                    ))
    })
    public Response allFilesReport()
    {
        // Added logger | CMS-3216
        if(log.isDebugEnabled()) {
            log.debug("Generating All files report");
        }
        try {
            generateReport(ALL_FILES_REPORT);
        } catch (PSErrorResultsException |PSInternalRequestCallException | BackendException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return Response.serverError().build();
        }
        return Response.accepted().build();

    }

    private void generateReport(String reportType) throws PSErrorResultsException, PSInternalRequestCallException, BackendException {
        synchronized (this) {
            URI baseUri = uriInfo.getBaseUri();
            Map<String, Object> requestThreadMap = new HashMap<>();
            requestThreadMap.putAll(PSRequestInfoBase.getRequestInfoMap());
            String currentUser = (String) PSRequestInfoBase.getRequestInfo(PSRequestInfo.KEY_USER);
            User user = userAdaptor.getUser(null, currentUser);
            String toAddress = user.getEmailAddress();

            CompletableFuture<PSCSVStreamingOutput> a = new CompletableFuture<PSCSVStreamingOutput>() {{
                CompletableFuture.runAsync(() -> {
                    try {
                        if(!PSRequestInfoBase.isInited()) {
                            PSRequestInfoBase.initRequestInfo(requestThreadMap);
                        }
                        List<String> report = getReportList(reportType, baseUri);
                        PSCSVStreamingOutput out = null;
                        if (report != null) {
                            out = new PSCSVStreamingOutput(report);
                        }
                        sendMail(out, reportType,toAddress);
                    } catch (Exception ex) {
                        String errorStr = "Error occurred while generating" + reportType + "report";
                        log.error(errorStr + "cause:" + PSExceptionUtils.getMessageForLog(ex));
                        log.debug(PSExceptionUtils.getDebugMessageForLog(ex));
                        sendMail(errorStr, reportType,toAddress);
                    }
                });
            }};
        }
    }

    private List<String> getReportList(String reportType, URI baseUri) throws BackendException {
        switch (reportType){
            case ALL_FILES_REPORT: return assetAdaptor.allFilesReport(baseUri);
            case ALL_IMAGES_REPORT: return assetAdaptor.allImagesReport(baseUri);
            case NON_ADA_COMPLAINT_FILES_REPORT: return assetAdaptor.nonADACompliantFilesReport(baseUri);
            case NON_ADA_COMPLIANT_IMAGES_REPORT:return assetAdaptor.nonADACompliantImagesReport(baseUri);
        }
        return null;
    }

    /**
     * This API sends email to given address with outputstream as file and given report name.
     * @param out
     * @param reportName
     * @param toAddress
     */
    private void sendMail(Object out,String reportName,String toAddress){
        //Generate a temp file from Report Data
        File tempFile = null;
        String subject = "Report :" + reportName + " Generated";
        String mailMessage = "Please find attached the Report";
        try {
            if(out instanceof PSCSVStreamingOutput) {
                try {
                    tempFile = Files.createTempFile(reportName, ".csv").toFile();
                    FileOutputStream outputStream = new FileOutputStream(tempFile);
                    ((PSCSVStreamingOutput) out).write(outputStream);
                } catch (IOException e) {
                    log.error("Error Sending Report Mail: {}", PSExceptionUtils.getMessageForLog(e));
                    log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                    PSWorkFlowUtils.sendMailWithAttachment(tempFile,subject,mailMessage,toAddress);
                }
            }else if (out instanceof String){
                mailMessage = (String)out;
                mailMessage = mailMessage + "\n" + "Please contact System Administrator";
            }else{
                mailMessage = "No Data found for report";
            }
            PSWorkFlowUtils.sendMailWithAttachment(tempFile,subject,mailMessage,toAddress);

        }catch (EmailException e){
            log.error("Error Sending Report Mail: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }

    }

    @POST
    @Path("/reports/non-ada-compliant-images")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Bulk updates File assets based on values in the report"
    , responses = {
            @ApiResponse(responseCode = "404", description = "Path not found"),
            @ApiResponse(responseCode = "500", description = "Error"),
            @ApiResponse(responseCode = "200", description = "OK", content=@Content(
                    schema=@Schema(implementation = Status.class)
            ))})
    public Status bulkupdateNonADACompliantImages(List<Attachment> atts)
    {
    	  if (atts == null || atts.isEmpty())
              throw new RuntimeException("No file sent");
        
    	  Attachment att = atts.get(0);
          InputStream stream = att.getObject(InputStream.class);
          return assetAdaptor.bulkupdateNonADACompliantImages(uriInfo.getBaseUri(),stream);
    }
    
    
    @POST
    @Path("/reports/non-ada-compliant-files")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Bulk updates File assets based on values in the report",
            description = "Edit the CSV file generated by the GET on this resource, then post the changed file to bulk update.",
            responses = {
                    @ApiResponse(responseCode = "404", description = "Path not found"),
                    @ApiResponse(responseCode = "500", description = "Error"),
                    @ApiResponse(responseCode = "200", description = "OK", content=@Content(
                            schema=@Schema(implementation = Status.class)
                    ))})
    public Status bulkupdateNonADACompliantFiles(List<Attachment> atts) {
      
        if (atts == null || atts.isEmpty())
            throw new RuntimeException("No file sent");
      
        Attachment att = atts.get(0);
        InputStream stream = att.getObject(InputStream.class);
        return assetAdaptor.bulkupdateNonADACompliantFiles(uriInfo.getBaseUri(),stream);
    }
    
    
    @POST
    @Path("/reports/all-images")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Bulk updates Image assets based on values in the report", description = "Edit the CSV file generated by the GET on this resource, then post the changed file to bulk update.",
            responses = {
                    @ApiResponse(responseCode = "404", description = "Path not found"),
                    @ApiResponse(responseCode = "500", description = "Error"),
                    @ApiResponse(responseCode = "200", description = "OK", content=@Content(
                            schema=@Schema(implementation = Status.class)
                    ))
    })
    public Status bulkupdateImageAssets(List<Attachment> atts) {
        if (atts == null || atts.isEmpty())
            throw new RuntimeException("No file sent");
      
        Attachment att = atts.get(0);
        InputStream stream = att.getObject(InputStream.class);
        return assetAdaptor.bulkupdateImageAssets(uriInfo.getBaseUri(),stream);
    }
    
    @POST
    @Path("/reports/all-files")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Bulk updates File assets based on values in the report", description = "Edit the CSV file generated by the GET on this resource, then post the changed file to bulk update.",
            responses = {
                    @ApiResponse(responseCode = "404", description = "Path not found"),
                    @ApiResponse(responseCode = "500", description = "Error"),
                    @ApiResponse(responseCode = "200", description = "OK", content=@Content(
                            schema=@Schema(implementation = Status.class)
                    ))})
    public Status bulkupdateFileAssets(List<Attachment> atts) {
    	  if (atts == null || atts.isEmpty())
              throw new RuntimeException("No file sent");
          
          Attachment att = atts.get(0);
          InputStream stream = att.getObject(InputStream.class);
          return assetAdaptor.bulkupdateFileAssets(uriInfo.getBaseUri(),stream);
    }
    
    @POST
    @Path("/approve-all/{folderPath:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Approves every shared Asset on the system that are not in an Archive state and that live in the specified folder.", description = "Passing the root folder will result in all Assets being approved.  Will override any checkout status and approve the Assets as the method caller.",
            responses = {
                    @ApiResponse(responseCode = "404", description = "Path not found"),
                    @ApiResponse(responseCode = "500", description = "Error"),
                    @ApiResponse(responseCode = "200", description = "OK", content=@Content(
                            schema=@Schema(implementation = Status.class)
                    ))
    })
    public Status approveAllAssets(@PathParam("folderPath") String folder){
    	Status status = new Status("OK");

    	try {
            int ctr = assetAdaptor.approveAllAssets(uriInfo.getBaseUri(), folder);
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
    @Operation(summary = "Archives every shared Asset on the system that live in the specified folder.", description = "Passing the root folder will result in all Assets being Archived.  Will override any checkout status and Archive the Assets as the method caller.",
            responses = {
                    @ApiResponse(responseCode = "404", description = "Path not found"),
                    @ApiResponse(responseCode = "500", description = "Error"),
                    @ApiResponse(responseCode = "200", description = "OK", content=@Content(
                            schema=@Schema(implementation = Status.class)
                    ))})
    public Status archiveAllAssets(@PathParam("folderPath") String folder){
    	Status status = new Status("OK");

    	try {
            int ctr = assetAdaptor.archiveAllAsets(uriInfo.getBaseUri(), folder);
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
    @Operation(summary = "Submits every shared Asset on the system that lives in the specified folder to the Review state.", description = "Passing the root folder will result in all Assets being submitted.  Will override any checkout status and Submit the Assets as the method caller.",
            responses = {
                    @ApiResponse(responseCode = "404", description = "Path not found"),
                    @ApiResponse(responseCode = "500", description = "Error"),
                    @ApiResponse(responseCode = "200", description = "OK", content=@Content(
                            schema=@Schema(implementation = Status.class)
                    ))})
    public Status submitAllAssets(@PathParam("folderPath") String folder){
    	Status status = new Status("OK");
    	try {
            int ctr = assetAdaptor.submitForReviewAllAsets(uriInfo.getBaseUri(), folder);
            status.setMessage("Submitted " + ctr + " Assets");
            return status;
        } catch (BackendException e) {
           log.error(PSExceptionUtils.getMessageForLog(e));
           log.debug(PSExceptionUtils.getDebugMessageForLog(e));
           throw new WebApplicationException(e);
        }
    }
}
