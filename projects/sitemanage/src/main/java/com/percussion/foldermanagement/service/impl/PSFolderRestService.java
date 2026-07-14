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
package com.percussion.foldermanagement.service.impl;

import org.owasp.encoder.Encode;

import com.percussion.foldermanagement.data.PSFolderItem;
import com.percussion.foldermanagement.data.PSGetAssignedFoldersJobStatus;
import com.percussion.foldermanagement.data.PSWorkflowAssignment;
import com.percussion.foldermanagement.service.IPSFolderService;
import com.percussion.foldermanagement.service.IPSFolderService.PSWorkflowAssignmentInProgressException;
import com.percussion.foldermanagement.service.IPSFolderService.PSWorkflowNotFoundException;
import com.percussion.pathmanagement.service.IPSPathService.PSPathNotFoundServiceException;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.dao.IPSGenericDao.LoadException;
import com.percussion.share.data.PSLightWeightObject;
import com.percussion.share.data.PSLightWeightObjectList;
import com.percussion.share.service.exception.PSValidationException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Exposes the services provided by {@link PSFolderService} through a REST API, and handles
 * exceptions and HTTP error codes accordingly.
 *
 * <p>Sunny Sal says: "Folders, workflows, and Java 11 - what a combo!"
 */
@Path("/folders")
@Component("folderRestService")
public class PSFolderRestService {
  private static final Logger log = LogManager.getLogger(PSFolderRestService.class);

  private final IPSFolderService folderService;

  @Autowired
  public PSFolderRestService(IPSFolderService folderService) {
    this.folderService = folderService;
  }

  /**
   * Start the async job to get associated folders.
   *
   * @param workflowName The name of the workflow to get folders for, not {@code null} or empty,
   *     must be a valid workflow name.
   * @param path The root path to get folders from, not {@code null}.
   * @return The job id to use to get the status/result.
   */
  @GET
  @Path("/GetAssociatedFoldersJob/start/{workflowName}/{path:.*}")
  @Produces(MediaType.TEXT_PLAIN)
  public String startGetAssociatedFoldersJob(
      @PathParam("workflowName") String workflowName,
      @PathParam("path") String path,
      @QueryParam("includeFoldersWithDifferentWorkflow") @DefaultValue("false")
          Boolean includeFoldersWithDifferentWorkflow) {
    try {
      return folderService.startGetAssignedFoldersJob(
          workflowName, path, includeFoldersWithDifferentWorkflow);
    } catch (PSWorkflowNotFoundException e) {
      log.error("Workflow not found: {}", PSExceptionUtils.getMessageForLog(e));
      throw new WebApplicationException(
          Response.status(Status.NOT_FOUND)
              .type(MediaType.TEXT_PLAIN)
              .entity(Encode.forHtml("Workflow not found: " + workflowName))
              .build());
    }
  }

  /**
   * Get the status/result for a running async job to get associated folders.
   *
   * @param jobId The id of the job, must be a valid job.
   * @return The status, {@link PSGetAssignedFoldersJobStatus#getStatus()} will return the resulting
   *     folder items once the job is complete, otherwise it will be {@code null}. If the job has
   *     failed, {@link PSGetAssignedFoldersJobStatus#getStatus()} will return -1, if completed it
   *     will return 100, and if running it will return 1.
   */
  @GET
  @Path("/GetAssociatedFoldersJob/status/{jobId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSGetAssignedFoldersJobStatus getAssociatedFoldersJobStatus(
      @PathParam("jobId") String jobId) {
    return folderService.getAssignedFoldersJobStatus(jobId);
  }

  /**
   * Cancel a running async job to get associated folders. Method will block until the job is
   * actually stopped.
   *
   * @param jobId The id of the job, must be a valid job.
   * @return The status, indicating the job has been aborted.
   */
  @GET
  @Path("/GetAssociatedFoldersJob/cancel/{jobId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSGetAssignedFoldersJobStatus cancelAssociatedFoldersJob(
      @PathParam("jobId") String jobId) {
    return folderService.cancelAssignedFoldersJob(jobId);
  }

  @GET
  @Path("/{workflowName}/{path:.*}")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response getAssociatedFolders(
      @PathParam("workflowName") String workflowName,
      @PathParam("path") String path,
      @QueryParam("includeFoldersWithDifferentWorkflow") @DefaultValue("false")
          Boolean includeFoldersWithDifferentWorkflow) {
    var message =
        "There was an error getting the associated folders to workflow '"
            + workflowName
            + "', from the path '"
            + path
            + "'. ";
    try {
      var folderItems =
          new GenericEntity<List<PSFolderItem>>(
              folderService.getAssignedFolders(
                  workflowName, "/" + path, includeFoldersWithDifferentWorkflow)) {};
      return Response.ok(folderItems).build();
    } catch (PSWorkflowNotFoundException e) {
      log.error("{}, Error: {}", message, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.status(Status.NOT_FOUND).type(MediaType.TEXT_PLAIN).entity(Encode.forHtml(message)).build();
    } catch (IllegalArgumentException e) {
      log.error("{}, Error: {}", message, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.status(Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(Encode.forHtml(message)).build();
    } catch (PSPathNotFoundServiceException | LoadException e) {
      log.error("{}, Error: {}", message, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.status(Status.NOT_FOUND).type(MediaType.TEXT_PLAIN).entity(Encode.forHtml(message)).build();
    } catch (Exception e) {
      log.error("{}, Error: {}", message, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.serverError().type(MediaType.TEXT_PLAIN).entity(Encode.forHtml(message)).build();
    }
  }

  @GET
  @Path("/workflowassignment/isInProgress")
  @Produces(MediaType.TEXT_PLAIN)
  public Boolean isContentWorkflowAssignmentInProgress() {
    return folderService.isContentWorkflowAssignmentInProgress();
  }

  @POST
  @Path("/workflowassignment")
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response assignFoldersToWorkflow(PSWorkflowAssignment workflowAssignment) {
    var message =
        "There was an error associating the workflow '"
            + workflowAssignment.getWorkflowName()
            + "' to the folders: ["
            + StringUtils.join(workflowAssignment.getAssignedFolders(), ",")
            + "]. ";
    try {
      folderService.assignFoldersToWorkflow(workflowAssignment);
      return Response.noContent().build();
    } catch (PSWorkflowNotFoundException e) {
      log.error("{}, Error: {}", message, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.status(Status.NOT_FOUND).type(MediaType.TEXT_PLAIN).entity(Encode.forHtml(message)).build();
    } catch (PSWorkflowAssignmentInProgressException e) {
      log.error("{}, Error: {}", message, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.status(Status.CONFLICT).type(MediaType.TEXT_PLAIN).entity(Encode.forHtml("Workflow assignment is already in progress.")).build();
    } catch (IllegalArgumentException e) {
      log.error("{}, Error: {}", message, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.status(Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(Encode.forHtml(message)).build();
    } catch (Exception e) {
      log.error("{}, Error: {}", message, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return Response.serverError().type(MediaType.TEXT_PLAIN).entity(Encode.forHtml(message)).build();
    }
  }

  @GET
  @Path("/folderpages/id/{id}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSLightWeightObject> getFolderPagesById(@PathParam("id") String id) {
    try {
      return new PSLightWeightObjectList(folderService.getPagesFromFolder(id));
    } catch (IPSFolderService.PSFolderNotFoundException
        | IPSFolderService.PSPagesNotFoundException
        | PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(
          Response.status(Status.INTERNAL_SERVER_ERROR)
              .type(MediaType.TEXT_PLAIN)
              .entity(Encode.forHtml("Error retrieving pages for folder: " + id))
              .build());
    }
  }
}
