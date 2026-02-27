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
package com.percussion.delivery.forms.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.delivery.services.PSAbstractRestService;
import com.percussion.delivery.utils.PSVersionHelper;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author natechadwick
 */
public class PSFormRestServiceBaseTest extends JerseyTest {

  public PSFormRestServiceBaseTest() {}

  /***
   * Takes the context file as an arg and spins up grizzly to
   * test rest methods.
   */
  @Override
  protected Application configure() {
    return new ResourceConfig(TestFormRestService.class)
        .property("jersey.config.server.provider.scanning.disableMetainf.services.lookup", true);
  }

  @Test
  public void testGetRestVersion() {
    Response response = target("/forms/version").request(MediaType.APPLICATION_JSON).get();
    assertNotNull(response);
    Assertions.assertEquals(200, response.getStatus());
    Assertions.assertEquals(testGetVersion(), response.readEntity(String.class));
  }

  private String testGetVersion() {
    String version = PSVersionHelper.getVersion(this.getClass());
    assertNotNull(version);
    System.out.print(version);
    return version;
  }

  @Test
  public void testCSRF() {
    Response response = target("/forms/csrf").request(MediaType.APPLICATION_JSON).get();
    assertNotNull(response);
  }

  @Path("/forms")
  public static class TestFormRestService extends PSAbstractRestService {
    @Override
    public Response updateOldSiteEntries(String prevSiteName, String newSiteName) {
      return Response.noContent().build();
    }
  }
}
