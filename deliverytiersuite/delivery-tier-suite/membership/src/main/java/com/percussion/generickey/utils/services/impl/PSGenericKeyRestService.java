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
package com.percussion.generickey.utils.services.impl;

import com.percussion.generickey.services.IPSGenericKeyService;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * REST/Webservice layer that exposes {@link IPSGenericKeyService} under {@code /key}. Creates,
 * validates and deletes time-limited generic keys.
 *
 * @author leonardohildt
 */
@Path("/key")
@Component
@Scope("singleton")
public class PSGenericKeyRestService {

  private IPSGenericKeyService genericKeyService;

  /**
   * Default constructor for frameworks that require it (e.g. Jersey).
   */
  public PSGenericKeyRestService() {}

  /**
   * Ctor, autowired by spring.
   *
   * @param service The service to use, may not be <code>null</code>.
   */
  public PSGenericKeyRestService(IPSGenericKeyService service) {
    Validate.notNull(service);
    genericKeyService = service;
  }

  /**
   * Creates a reset key.
   *
   * @return the reset key value generated.
   * @throws WebApplicationException if the underlying service fails.
   */
  @POST
  @Path("/requestKey")
  @Produces(MediaType.TEXT_PLAIN)
  public String generateKey() {
    String resetKey = "";
    try {
      resetKey = genericKeyService.generateKey(DAY_IN_MILLISECONDS);
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.serverError().build());
    }
    return resetKey;
  }

  /**
   * Processes the reset key provided, and if exists, compare the reset date against with the
   * current date, to determine if the key is still valid.
   *
   * @param key the reset key to validate. Never <code>null</code>, or empty.
   * @return true if the key is valid, otherwise false.
   * @throws WebApplicationException if the underlying service fails.
   */
  @POST
  @Path("/isvalid/{key}")
  @Produces(MediaType.TEXT_PLAIN)
  public String isValidKey(@PathParam("key") String key) {
    Boolean keyIsValid = false;
    try {
      keyIsValid = genericKeyService.isValidKey(key);
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.serverError().build());
    }
    return keyIsValid.toString();
  }

  /**
   * Delete a reset key using the key provided. If 'key' doesn't exist, throw a web application
   * Exception.
   *
   * @param key The reset key to delete. Never <code>null</code>, or empty.
   * @throws WebApplicationException if the underlying service fails or the key is unknown.
   */
  @DELETE
  @Path("/{key}")
  public void deleteKey(@PathParam("key") String key) {
    try {
      genericKeyService.deleteKey(key);
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /** Constant to set the duration time one day into milliseconds */
  private static final long DAY_IN_MILLISECONDS = 86400000;
}
