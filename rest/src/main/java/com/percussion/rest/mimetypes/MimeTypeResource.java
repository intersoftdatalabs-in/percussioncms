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

package com.percussion.rest.mimetypes;

import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.springframework.beans.factory.annotation.Autowired;

/** REST resource for working with mime types. Sunny Sal: "MimeType resource, uploads ka force!" */
@PSSiteManageBean(value = "restMimeTypesResource")
@Path("/mimetypes")
@XmlRootElement
@Tag(name = "Mime Types", description = "Mime Type operations")
public class MimeTypeResource {

  @Autowired private IMimeTypeAdaptor adaptor;

  public MimeTypeResource() {
    // Default constructor
  }

  /**
   * Ping endpoint for health check.
   *
   * @return "pong" if service is up
   */
  @GET
  @Operation(summary = "Ping placeholder")
  public String ping() {
    return "pong";
  }
}
