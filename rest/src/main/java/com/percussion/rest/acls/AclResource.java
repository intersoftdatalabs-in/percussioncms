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
package com.percussion.rest.acls;

import com.percussion.rest.Guid;
import com.percussion.rest.GuidList;
import com.percussion.rest.Status;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/** REST resource exposing ACL operations. */
@PSSiteManageBean(value = "restAclResource")
@Path("/acls")
@XmlRootElement
@Tag(name = "ACLS", description = "ACL operations")
@Lazy
public class AclResource {

  /** Logger used by this resource. */
  private final Logger log = LogManager.getLogger(getClass());

  /** Adaptor that implements the ACL operations. */
  @Autowired private IAclAdaptor adaptor;

  /** No-op constructor. */
  public AclResource() {}

  @POST
  @Path("/object")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Retrieves the acl for the object with the supplied id")
  public UserAccessLevel getUserAccessLevel(
      @Parameter(
              description =
                  "The guid of the object for which the current user's effective access level needs"
                      + " to be computed. Must not be null.",
              required = true)
          Guid objectGuid) {
    try {
      return adaptor.getUserAccessLevel(objectGuid);
    } catch (Exception e) {
      log.error(
          "Exception getting User Access Level for Object: {}, Error: {}",
          objectGuid.getStringValue(),
          e.getMessage());
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), Response.serverError().build());
    }
  }

  @GET
  @Path("/user/{aclGuid}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary =
          "Computes the current user's effective access level to the object protected by the"
              + " supplied ACL. The effective access level of a user is the highest permission he"
              + " or she can get on the associated object based on all entries in the ACL.")
  public UserAccessLevel calculateUserAccessLevel(@PathParam("aclGuid") String aclGuid) {
    try {
      return adaptor.calculateUserAccessLevel(aclGuid);
    } catch (Exception e) {
      log.error(
          "Error checking access level for Acl: {}, Error: {}",
          aclGuid,
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), Response.serverError().build());
    }
  }

  @POST
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Create an acl for the specified object.")
  public Acl createAcl(
      @Parameter(required = true, description = "A valid CreateAclRequest object")
          CreateAclRequest request) {
    try {
      return adaptor.createAcl(request.getObjectGuid(), request.getOwner());
    } catch (Exception e) {
      log.error(
          "Error creating Acl for Owner: {} and object: {}, Error: {}",
          request.getOwner().getName(),
          request.getObjectGuid().getStringValue(),
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), Response.serverError().build());
    }
  }

  @GET
  @Path("/bulk")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary =
          "Load ACLs for given list of ACL GUIDs. These objects are cached and shared between"
              + " threads and should be treated read-only. See the class description for more"
              + " details.")
  public AclList loadAcls(GuidList aclGuids) {
    try {
      return adaptor.loadAcls(aclGuids);
    } catch (Exception e) {
      log.error(
          "Error loading acls for guid list: {}, Error: {}",
          aclGuids,
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), Response.serverError().build());
    }
  }

  /**
   * Loads the ACL identified by the supplied GUID string.
   *
   * @param guid the ACL GUID
   * @return the loaded ACL
   */
  @GET
  @Path("/{guid}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Acl loadAcl(@PathParam("guid") String guid) {
    try {
      return adaptor.loadAcl(new Guid(guid));
    } catch (Exception e) {
      log.error("Error loading acl for guid: {} {}", guid, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), Response.serverError().build());
    }
  }

  /**
   * Loads the ACLs for the supplied object GUIDs.
   *
   * @param objectGuids the object GUIDs whose ACLs should be loaded
   * @return the loaded ACLs
   */
  @POST
  @Path("/bulk")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public AclList loadAclsForObjects(GuidList objectGuids) {
    try {
      return adaptor.loadAclsForObjects(objectGuids);
    } catch (Exception e) {
      log.error(
          "Error loading acl for guids: {} {}", objectGuids, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), Response.serverError().build());
    }
  }

  @GET
  @Path("/object/{objectGuid}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Load ACL for object",
      description =
          "Returns the design-time ACL for a securable object GUID (template, content type,"
              + " etc.). Used by the Developer module ACL viewer.")
  public Acl loadAclForObject(
      @Parameter(description = "Object GUID stringValue", required = true) @PathParam("objectGuid")
          String objectGuid) {
    try {
      return adaptor.loadAclForObject(new Guid(objectGuid));
    } catch (NotFoundException n) {
      log.debug("No ACL's found for object: {} {}", objectGuid, n.getMessage());
      log.debug(n.getMessage(), n);
      throw new WebApplicationException(
          "No ACL's found for Object with GUID:" + objectGuid, Response.status(404).build());
    } catch (Exception e) {
      log.error("Error loading acl for Object guid: {}, {}", objectGuid, e.getMessage());
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), Response.serverError().build());
    }
  }

  /**
   * Persists the supplied ACL list JSON.
   *
   * <p>The body is parsed by {@link AclListJsonReader} so CXF Jackson/Jettison cannot bind a raw
   * {@code ArrayList} (HTTP 400 ClassCast, #3391) or reject a bare JSON array (org.json
   * ParseError). Accepts {@code {"AclList":[…]}} and {@code [{…}]}.
   *
   * @param json the ACL list JSON to save
   * @return the save status
   */
  @PUT
  @Path("/bulk")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Status saveAcls(String json) {
    return persistAcls(AclListJsonReader.parse(json));
  }

  /**
   * Persists an already-bound ACL list (unit tests and {@link #saveAcls(String)}).
   *
   * @param aclList the ACL list to save
   * @return the save status
   */
  Status persistAcls(AclList aclList) {
    var ret = new Status(200, "OK");
    try {
      adaptor.saveAcls(aclList);
    } catch (Exception e) {
      log.error("Error saving acl list {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), Response.serverError().build());
    }
    return ret;
  }

  /**
   * Deletes the ACL with the supplied GUID.
   *
   * @param aclGuid the ACL GUID to delete
   * @return the deletion status
   */
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Status deleteAcl(Guid aclGuid) {
    var ret = new Status(200, "OK");
    try {
      adaptor.deleteAcl(aclGuid);
    } catch (Exception e) {
      log.error(
          "Error deleting acl: {}, Error: {}",
          aclGuid.getStringValue(),
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), Response.serverError().build());
    }
    return ret;
  }

  /**
   * Filters ACLs by community membership. Not implemented.
   *
   * @param aclList the ACL list to filter
   * @param communityNames community names to include
   * @return {@code null}, indicating the operation is not implemented
   */
  @POST
  @Path("/community/filter")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public GuidList filterByCommunities(GuidList aclList, List<String> communityNames) {
    // Not implemented
    return null;
  }

  /**
   * Finds objects visible to the supplied communities. Not implemented.
   *
   * @return {@code null}, indicating the operation is not implemented
   */
  @GET
  @Path("/community")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public GuidList findObjectsVisibleToCommunities() {
    // Not implemented
    return null;
  }
}
