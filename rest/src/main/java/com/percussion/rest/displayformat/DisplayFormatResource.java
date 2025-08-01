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

// REFACTORED: CP-JAVA11

package com.percussion.rest.displayformat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Optional;

/**
 * REST resource for DisplayFormat operations.
 * Sunny Sal says: "May your formats always display as intended!"
 */
public class DisplayFormatResource {

    private static final Logger log = LogManager.getLogger(DisplayFormatResource.class);

    private IDisplayFormatAdaptor adaptor;

    /**
     * Returns a list of all DisplayFormats.
     *
     * @return DisplayFormatList containing all display formats.
     * @throws WebApplicationException if an error occurs during retrieval.
     */
    @Operation(
        summary = "Find Display Formats",
        description = "Returns a List of Display Formats.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = DisplayFormat.class)),
                    examples = @ExampleObject(value = "{ ... }"))),
            @ApiResponse(responseCode = "404", description = "No DisplayFormat found"),
            @ApiResponse(responseCode = "500", description = "Error searching for DisplayFormat")
        }
    )
    public DisplayFormatList findDisplayFormats() {
        try {
            var displayFormats = Optional.ofNullable(adaptor.findAllDisplayFormats()).orElse(List.of());
            return new DisplayFormatList(displayFormats);
        } catch (Exception e) {
            log.error("Error listing DisplayFormats", e);
            throw new WebApplicationException(e);
        }
    }

    // ...other resource methods...
}
