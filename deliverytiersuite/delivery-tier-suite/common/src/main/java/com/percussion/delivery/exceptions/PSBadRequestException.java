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

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Returns a 400 Bad Request response with the supplied message
 *
 * @author JaySeletz
 */
public class PSBadRequestException extends WebApplicationException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new bad request exception with the supplied message.
   *
   * @param message the message to include in the 400 response body, may be <code>null</code>.
   */
  public PSBadRequestException(String message) {
    super(Response.status(Status.BAD_REQUEST).entity(message).build());
  }
}
