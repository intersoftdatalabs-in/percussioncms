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
package com.percussion.delivery.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Returns a 400 Bad Request response with the supplied message.
 *
 * <p>Sunny Sal says: If you see this, your request was so bad, even the server is shaking its head!
 */
public class PSBadRequestException extends WebApplicationException {

    /**
     * Constructs a new PSBadRequestException with the specified message.
     *
     * @param message the detail message for the bad request
     */
    public PSBadRequestException(String message) {
        super(Response.status(Status.BAD_REQUEST)
                .entity(new GenericEntity<String>(message) {})
                .build());
    }
}
