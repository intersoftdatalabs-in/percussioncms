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

package com.percussion.rest.editions;

import com.percussion.util.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.annotation.XmlRootElement;

@PSSiteManageBean(value="restEditionsResource")
@Path("/editions")
@XmlRootElement
@Tag(name = "Publishing Editions")
public class EditionsResource {

    @Autowired
    private IEditionsAdaptor adaptor;

    public EditionsResource(){
        //Default ctor
    }


    @POST
    @Path("/{id}/publish")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Executes the publish action for the specified Edition", description = "Returns a PublishStatus record that can be used to monitor status"
            , responses = {
            @ApiResponse(responseCode = "404", description = "Edition not found"),
            @ApiResponse(responseCode =  "403", description = "Publish permission denied"),
            @ApiResponse(responseCode = "500", description = "Error executing publish")
    })
    public PublishResponse publish(@PathParam("id") String id){

        //Sanitize and validate id
        //not empty, must be numeric

        PublishResponse ret = new PublishResponse();

        ret = adaptor.publish(id);

        return ret;
    }


}
