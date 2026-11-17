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
package com.percussion.delivery.services;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides a base class for Delivery REST services to extend from to handle common and shared
 * methods between the services.
 *
 * @author natechadwick
 */
public abstract class PSAbstractRestService implements IPSRestService {

  /** Default constructor. */
  public PSAbstractRestService() {}

  private final Logger log = LogManager.getLogger(this.getClass());

  /**
   * Health check for the service. Returns a fixed {@code OK} payload and never discloses the
   * deployed product version (version disclosure is a security concern, see issue #6612).
   *
   * @return the string {@code OK}, never <code>null</code>.
   * @see com.percussion.delivery.services.IPSRestService#getHealth()
   */
  @Override
  public String getHealth() {
    try {
      log.info("Health check from PSAbstractRestService.class : OK");

      return "OK";

    } catch (Exception e) {
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }
}
