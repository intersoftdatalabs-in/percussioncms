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

package com.percussion.rest.preferences;

import com.percussion.rest.Status;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * REST resource for user preference operations. Sunny Sal: "User preferences? Bas yahi toh mera
 * kaam hai!"
 */
@PSSiteManageBean(value = "restPreferencesResource")
@Path("/preferences")
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Tag(name = "User Preferences", description = "User Preference operations")
public class PreferenceResource {

  private static final Logger log = LogManager.getLogger(PreferenceResource.class);

  @Autowired private IPreferenceAdaptor adaptor;

  /** Default constructor for servlet / Spring instantiation. */
  public PreferenceResource() {}

  /**
   * Test / explicit-wiring constructor.
   *
   * @param adaptor preference adaptor
   */
  public PreferenceResource(IPreferenceAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /** Gets all stored user preferences for the current user. */
  @Path("/")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get all stored user preferences for the given user",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK (empty list when the user has no stored preferences)",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = UserPreference.class)))),
        @ApiResponse(responseCode = "500", description = "Error message")
      })
  public UserPreferenceList getAllUserPreferences() {
    try {
      UserPreferenceList list = adaptor.getAllUserPreferences();
      return list != null ? list : new UserPreferenceList();
    } catch (NotFoundException e) {
      // Empty catalog is a valid Explorer/profile session — do not 404 the
      // browser (console-clean #3458 / parent #2745).
      log.debug("Didn't find any preferences");
      return new UserPreferenceList();
    } catch (Exception e) {
      log.error(
          "An unexpected error occurred getting preferences. {}",
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), e, 500);
    }
  }

  /** Saves all user preferences for the current user. */
  @Path("/all")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Save all user preferences for the current user",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = UserPreference.class)))),
        @ApiResponse(responseCode = "404", description = "No preferences found"),
        @ApiResponse(responseCode = "500", description = "Error message")
      })
  public UserPreferenceList saveAllUserPreferences(
      @Parameter(required = true) UserPreferenceList preferences) {
    try {
      return adaptor.saveAllUserPreferences(preferences);
    } catch (NotFoundException e) {
      log.debug("Didn't find any preferences");
      throw new WebApplicationException("No preferences found.", 404);
    } catch (Exception e) {
      log.error(
          "An unexpected error occurred saving preferences. {}",
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), e, 500);
    }
  }

  /** Gets a specific user preference for the logged in user. */
  @Path("/{preference}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get a specific user preference for the logged in user",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK (empty value when the named preference is not stored)",
            content = @Content(schema = @Schema(implementation = UserPreference.class))),
        @ApiResponse(responseCode = "500", description = "Error message")
      })
  public UserPreference loadPreference(@PathParam("preference") String preference) {
    try {
      UserPreference found = adaptor.loadPreference(preference);
      if (found != null) {
        return found;
      }
      return emptyPreference(preference);
    } catch (NotFoundException e) {
      // Unset prefs (e.g. perc_profile_gravatar_email) are normal on a clean
      // H2 session — 404 here is browser-console noise (#3458 / #2745).
      log.debug("Didn't find a match for preference {}", preference);
      return emptyPreference(preference);
    } catch (Exception e) {
      log.error(
          "An unexpected error occurred getting preference: {}, Error: {}",
          preference,
          e.getMessage());
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), e, 500);
    }
  }

  /** Saves a specific user preference for the given user. Username must be set on preference. */
  @PUT
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary =
          "Saves a specific user preference for the given user. username must be set on"
              + " preference.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = UserPreference.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Error message")
      })
  public UserPreference savePreference(UserPreference pref) {
    try {
      if (StringUtils.isEmpty(pref.getCategory())) {
        pref.setCategory("sys_preferences");
      }
      if (StringUtils.isEmpty(pref.getContext())) {
        pref.setContext("private");
      }
      return adaptor.savePreference(pref);
    } catch (NotFoundException e) {
      log.debug(
          "Didn't find a match for preference {} for user: {}", pref.getName(), pref.getUserName());
      throw new WebApplicationException("No preference found.", 404);
    } catch (Exception e) {
      log.error(
          "An unexpected error occurred saving preference: {} for userName: {}, Error: {}",
          pref.getName(),
          pref.getUserName(),
          e.getMessage());
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), e, 500);
    }
  }

  /** Deletes a specific user preference for the given user. */
  @DELETE
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Delete a specific user preference for the given user",
      responses = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "No preference found"),
        @ApiResponse(responseCode = "500", description = "Error message")
      })
  public Status deletePreference(UserPreference pref) {
    var ret = new Status(200, "OK");
    try {
      adaptor.deletePreference(pref);
    } catch (NotFoundException e) {
      log.debug(
          "Didn't find a match for preference {} for user: {}", pref.getName(), pref.getUserName());
      throw new WebApplicationException("No preference found.", 404);
    } catch (Exception e) {
      log.error(
          "An unexpected error occurred deleting preference: {} for userName: {}, Error: {}",
          pref.getName(),
          pref.getUserName(),
          e.getMessage());
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage(), e, 500);
    }
    return ret;
  }

  /**
   * Wire shape for an unset named preference. Empty {@code value} is the
   * product "not stored" signal — callers already treat blank as fallback.
   */
  static UserPreference emptyPreference(String preference) {
    UserPreference empty = new UserPreference();
    empty.setName(preference == null ? "" : preference);
    empty.setValue("");
    return empty;
  }
}
