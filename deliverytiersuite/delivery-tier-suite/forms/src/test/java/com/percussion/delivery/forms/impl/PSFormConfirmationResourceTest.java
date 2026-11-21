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
package com.percussion.delivery.forms.impl;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;

/**
 * Proves the default form confirmation targets are servable (issue #6893): the Jersey servlet maps
 * {@code /*}, so the {@code WEB-INF}-only pages need explicit resource endpoints at the deployment
 * context root.
 */
public class PSFormConfirmationResourceTest extends JerseyTest {

  private static final String SUCCESS_HTML =
      "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\" /></head>"
          + "<body><main data-testid=\"forms-confirmation-success\" role=\"status\">"
          + "<h4>Your form submission was successful.</h4></main></body></html>";

  private static final String ERROR_HTML =
      "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\" /></head>"
          + "<body><main data-testid=\"forms-confirmation-error\" role=\"alert\">"
          + "<h4>An error occurred during form submission.</h4></main></body></html>";

  private final Map<String, String> pages = new ConcurrentHashMap<>();

  public PSFormConfirmationResourceTest() {
    pages.put(PSFormConfirmationResource.SUCCESS_PAGE_RESOURCE, SUCCESS_HTML);
    pages.put(PSFormConfirmationResource.ERROR_PAGE_RESOURCE, ERROR_HTML);
  }

  @Override
  protected Application configure() {
    PSFormConfirmationResource resource =
        new PSFormConfirmationResource() {
          @Override
          String loadConfirmationPage(String resourcePath) {
            return pages.get(resourcePath);
          }
        };
    return new ResourceConfig()
        .register(resource)
        .property("jersey.config.server.provider.scanning.disableMetainf.services.lookup", true);
  }

  @Test
  public void successPageServesConfirmationMarker() {
    Response response = target("/success.html").request(MediaType.TEXT_HTML).get();

    assertEquals(200, response.getStatus());
    assertTrue(
        response.getHeaderString("Content-Type").startsWith(MediaType.TEXT_HTML),
        "success page must be served as text/html");
    String body = response.readEntity(String.class);
    assertTrue(
        body.contains("data-testid=\"forms-confirmation-success\""),
        "success page must carry the stable automation hook");
    assertTrue(
        body.contains("Your form submission was successful."),
        "success page must keep the long-standing user-visible copy");
  }

  @Test
  public void errorPageServesConfirmationMarker() {
    Response response = target("/error.html").request(MediaType.TEXT_HTML).get();

    assertEquals(200, response.getStatus());
    assertTrue(
        response.getHeaderString("Content-Type").startsWith(MediaType.TEXT_HTML),
        "error page must be served as text/html");
    String body = response.readEntity(String.class);
    assertTrue(
        body.contains("data-testid=\"forms-confirmation-error\""),
        "error page must carry the stable automation hook");
    assertTrue(
        body.contains("An error occurred during form submission."),
        "error page must keep the long-standing user-visible copy");
  }

  @Test
  public void missingPackagedPageAnswersNotFound() {
    pages.remove(PSFormConfirmationResource.ERROR_PAGE_RESOURCE);

    Response response = target("/error.html").request(MediaType.TEXT_HTML).get();

    assertEquals(404, response.getStatus());
  }
}
