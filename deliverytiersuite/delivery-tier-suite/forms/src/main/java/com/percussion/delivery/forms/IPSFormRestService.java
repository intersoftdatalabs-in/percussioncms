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
package com.percussion.delivery.forms;

import com.percussion.delivery.forms.data.PSFormSummaries;
import com.percussion.delivery.services.IPSRestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import org.glassfish.jersey.server.ContainerRequest;

/**
 * REST/Webservice interface for the forms delivery-tier service. Endpoints cover collecting
 * form submissions from the published site and managing persisted form data for the CMS UI.
 *
 * <p>All endpoints are secured with SSL and HTTP Basic Authentication.</p>
 */
@Path("/forms")
public interface IPSFormRestService extends IPSRestService {

  /**
   * Delete a form using the name provided if it was exported. If 'formName' is null or empty, then
   * all exported forms are deleted. Form name comparison is case-insensitive
   *
   * @param formName The name of the form to delete.
   */
  @DELETE
  @Path("/form/cms/{formName}")
  public abstract void delete(@PathParam("formName") String formName);

  /**
   * Processes an entry form and adds a new form to the form service. Upon form addition the form
   * redirects back to the referer.
   *
   * @param containerRequest the Jersey container request providing access to the decoded form
   *     body and Jersey-specific properties.
   * @param action the form action value supplied by the published site, may be <code>null</code>.
   * @param header the HTTP headers associated with the incoming request, never
   *     <code>null</code>.
   * @param request the servlet request, never <code>null</code>.
   * @param resp the servlet response used to deliver the redirect, never <code>null</code>.
   * @throws IOException if an I/O error occurs while writing the redirect response.
   * @throws WebApplicationException if the submitted form fails validation or processing.
   */
  @POST
  @Path("/form/collect")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public abstract void create(
      @Context ContainerRequest containerRequest,
      @FormParam("action") String action,
      @Context HttpHeaders header,
      @Context HttpServletRequest request,
      @Context HttpServletResponse resp)
      throws WebApplicationException, IOException;

  /**
   * Retrieves the form given the name.
   *
   * @param formName the name of the form to be found an returned.
   * @return the form if found, never <code>null</code>, may be empty.
   */
  @GET
  @Path("/form/cms/{formName}")
  @Produces({MediaType.APPLICATION_JSON})
  public abstract PSFormSummaries get(@PathParam("formName") String formName);

  /**
   * Retrieves list of form summaries. Form summaries include the name, total forms count, and total
   * exported forms count.
   *
   * @return list of form summaries, never <code>null</code>, may be empty.
   */
  @GET
  @Path("/form/cms/list")
  @Produces({MediaType.APPLICATION_JSON})
  public abstract PSFormSummaries get();

  /**
   * Export the form given the name.
   *
   * @param formName the name of the form to be found an returned.
   * @param csvFile the name for the CSV file.
   * @return the csv if form was found, never <code>null</code>, may be empty.
   */
  @GET
  @Path("/form/cms/{formName}/{csvFile}")
  @Produces({"text/csv"})
  public abstract Response export(
      @PathParam("formName") String formName, @PathParam("csvFile") String csvFile);
}
