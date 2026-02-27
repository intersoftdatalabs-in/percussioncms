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
package com.percussion.delivery.metadata;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.delivery.services.PSAbstractRestService;
import com.percussion.delivery.utils.PSVersionHelper;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;

/**
 * @author natechadwick
 */
public class PSBaseMetadataRestServiceTest extends JerseyTest {

  @Path("/membership")
  public static class TestRestService extends PSAbstractRestService {
    @Override
    public Response updateOldSiteEntries(String prevSiteName, String newSiteName) {
      return Response.noContent().build();
    }
  }

  /***
   * Takes the context file as an arg and spins up grizzly to
   * test rest methods.
   *
   */
  @Override
  protected Application configure() {
    ResourceConfig resourceConfig = new ResourceConfig(TestRestService.class);
    resourceConfig.property(ServerProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);
    return resourceConfig;
  }

  @Test
  public void testGetRestVersion() {
    Response response = target("membership/version").request(MediaType.TEXT_PLAIN).get();

    assertNotNull(response);
    assertEquals(200, response.getStatus());
    String version_string = PSVersionHelper.getVersion(this.getClass());
    assertNotNull(version_string);
    assertEquals(version_string, response.readEntity(String.class));
  }

  @Test
  public void testGetVersion() {
    String version = PSVersionHelper.getVersion(this.getClass());
    assertNotNull(version);
    System.out.print(version);
  }
}
