/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.delivery.feeds.services;

import com.percussion.delivery.services.PSAbstractRestService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Base class for feeds REST service JerseyTest tests.
 *
 * <p>Registers a lightweight stub resource (same {@code /rss} path as {@link PSFeedService}) so
 * health-endpoint tests run against the JerseyTest container without a Spring context or database.
 */
public abstract class PSFeedServiceTestsBase extends JerseyTest {

  /**
   * Registers the stub feeds resource with the JerseyTest container.
   *
   * @return the test application configuration.
   */
  @Override
  protected Application configure() {
    return new ResourceConfig(TestFeedsRestService.class)
        .property("jersey.config.server.provider.scanning.disableMetainf.services.lookup", true);
  }

  @Test
  public void testGetRestHealth() {

    Response response = target("/rss/health").request(MediaType.APPLICATION_JSON).get();

    Assertions.assertNotNull(response);
    Assertions.assertEquals(200, response.getStatus());
    Assertions.assertEquals("OK", response.readEntity(String.class));
  }

  @Path("/rss")
  public static class TestFeedsRestService extends PSAbstractRestService {
    @Override
    public Response updateOldSiteEntries(String prevSiteName, String newSiteName) {
      return Response.noContent().build();
    }
  }
}
