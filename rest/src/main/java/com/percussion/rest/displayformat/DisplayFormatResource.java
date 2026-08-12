/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.rest.displayformat;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Read-only display format catalog for the Developer module (UI-05 list/detail) and the modern
 * Content Explorer folder list (issue #2400 / FR-027).
 *
 * <p>Write endpoints remain unimplemented at the adaptor layer (later slice).
 */
@PSSiteManageBean(value = "restDisplayFormatResource")
@Path("/displayformats")
@XmlRootElement
@Tag(name = "Display Formats", description = "CX display format design catalog (read-only)")
public class DisplayFormatResource {

  private final IDisplayFormatAdaptor adaptor;

  public DisplayFormatResource() {
    this.adaptor = null;
  }

  @Autowired
  public DisplayFormatResource(IDisplayFormatAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "List display formats",
      description =
          "Lists Content Explorer display formats with columns and usage flags. Optional filters"
              + " support the modern Explorer folder list (validForFolder) and search/view UIs"
              + " (validForViewsAndSearches). Create/edit/delete are later slices.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = DisplayFormat.class)))),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public List<DisplayFormat> listDisplayFormats(
      @Parameter(description = "When true, only formats valid for folder list views")
          @QueryParam("validForFolder")
          Boolean validForFolder,
      @Parameter(description = "When true, only formats valid for views and searches")
          @QueryParam("validForViewsAndSearches")
          Boolean validForViewsAndSearches) {
    try {
      List<DisplayFormat> list = requireAdaptor().findAllDisplayFormats();
      if (list == null || list.isEmpty()) {
        return new DisplayFormatList();
      }
      if (validForFolder == null && validForViewsAndSearches == null) {
        return asDisplayFormatList(list);
      }
      List<DisplayFormat> filtered = new ArrayList<>(list.size());
      for (DisplayFormat df : list) {
        if (df == null) {
          continue;
        }
        if (Boolean.TRUE.equals(validForFolder) && !df.isValidForFolder()) {
          continue;
        }
        if (Boolean.FALSE.equals(validForFolder) && df.isValidForFolder()) {
          continue;
        }
        if (Boolean.TRUE.equals(validForViewsAndSearches) && !df.isValidForViewsAndSearches()) {
          continue;
        }
        if (Boolean.FALSE.equals(validForViewsAndSearches) && df.isValidForViewsAndSearches()) {
          continue;
        }
        filtered.add(df);
      }
      return asDisplayFormatList(filtered);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  @GET
  @Path("/{idOrName}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get display format detail",
      description =
          "Loads one display format by internal name or GUID string. Write remains unsupported.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = DisplayFormat.class))),
        @ApiResponse(responseCode = "404", description = "Display format not found"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public DisplayFormat getDisplayFormat(@PathParam("idOrName") String idOrName) {
    try {
      DisplayFormat df = requireAdaptor().findDisplayFormatByKey(idOrName);
      if (df == null) {
        // Generic body: do not echo raw idOrName (path probing).
        throw new WebApplicationException("Display format not found", 404);
      }
      return df;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new WebApplicationException(e, 500);
    }
  }

  /**
   * Return a {@link DisplayFormatList} so Jackson uses this package's {@code
   * JacksonContextResolver} (WRAP_ROOT_VALUE) instead of a raw {@code ArrayList} mapper.
   */
  private static DisplayFormatList asDisplayFormatList(List<DisplayFormat> list) {
    if (list instanceof DisplayFormatList displayFormatList) {
      return displayFormatList;
    }
    return new DisplayFormatList(list);
  }

  private IDisplayFormatAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new IllegalStateException(
          "Display format adaptor not configured (resource constructed without injection)");
    }
    return adaptor;
  }
}
