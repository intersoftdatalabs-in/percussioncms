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

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Serves the visitor-facing form confirmation pages at the deployment-context root.
 *
 * <p>The Jersey servlet maps {@code /*} (see {@code WEB-INF/web.xml}), so the container default
 * servlet never sees war-root statics and the {@code WEB-INF} copies of {@code success.html} /
 * {@code error.html} are not web-accessible on their own. These two {@code GET} endpoints close
 * that gap: {@code GET /perc-form-processor/success.html} answers the default success redirect
 * sent by {@link PSFormRestService}, and {@code GET /perc-form-processor/error.html} answers both
 * the default error redirect and the container {@code error-page} forwards (which already point at
 * the context-root {@code /error.html}). The markup stays sourced from the single packaged copies
 * under {@code WEB-INF}, so the polished pages (stable {@code data-testid} hooks, screen-reader
 * roles) are served byte-identical.
 *
 * <p>The paths carry no security constraint in {@code web.xml}, matching the openly served submit
 * surface: visitors land here without a role after submitting a form.
 */
@Component
@Path("/")
public class PSFormConfirmationResource {

  /** {@code WEB-INF} location of the packaged success page (not web-accessible directly). */
  static final String SUCCESS_PAGE_RESOURCE = "/WEB-INF/success.html";

  /** {@code WEB-INF} location of the packaged error page (not web-accessible directly). */
  static final String ERROR_PAGE_RESOURCE = "/WEB-INF/error.html";

  private static final Logger log = LogManager.getLogger(PSFormConfirmationResource.class);

  @Context private ServletContext servletContext;

  private volatile String successPage;

  private volatile String errorPage;

  /**
   * Default constructor used by the Jersey/Spring runtime. Confirmation markup is loaded lazily
   * from the packaged {@code WEB-INF} pages on first request.
   */
  public PSFormConfirmationResource() { // NOOP
  }

  /**
   * Answers the default success redirect target with the packaged success page.
   *
   * @return HTTP 200 with the success markup as {@code text/html}, or 404 when the packaged page
   *     cannot be read.
   */
  @GET
  @Path("success.html")
  @Produces(MediaType.TEXT_HTML)
  public Response success() {
    return confirmationPage(SUCCESS_PAGE_RESOURCE, true);
  }

  /**
   * Answers the default error redirect target (and the container error-page forwards) with the
   * packaged error page.
   *
   * @return HTTP 200 with the error markup as {@code text/html}, or 404 when the packaged page
   *     cannot be read.
   */
  @GET
  @Path("error.html")
  @Produces(MediaType.TEXT_HTML)
  public Response error() {
    return confirmationPage(ERROR_PAGE_RESOURCE, false);
  }

  private Response confirmationPage(String resourcePath, boolean success) {
    String page = success ? cachedSuccessPage() : cachedErrorPage();
    if (page == null) {
      log.error("Confirmation page unavailable: {} could not be read", resourcePath);
      return Response.status(Response.Status.NOT_FOUND)
          .entity("Confirmation page unavailable")
          .type(MediaType.TEXT_PLAIN)
          .build();
    }
    return Response.ok(page, "text/html;charset=UTF-8").build();
  }

  private String cachedSuccessPage() {
    String page = successPage;
    if (page == null) {
      synchronized (this) {
        page = successPage;
        if (page == null) {
          page = loadConfirmationPage(SUCCESS_PAGE_RESOURCE);
          successPage = page;
        }
      }
    }
    return page;
  }

  private String cachedErrorPage() {
    String page = errorPage;
    if (page == null) {
      synchronized (this) {
        page = errorPage;
        if (page == null) {
          page = loadConfirmationPage(ERROR_PAGE_RESOURCE);
          errorPage = page;
        }
      }
    }
    return page;
  }

  /**
   * Reads one packaged confirmation page. Split out (rather than inlined in the request path) so
   * unit tests can override the source without a servlet container.
   *
   * @param resourcePath context-relative path of the packaged page, never {@code null}.
   * @return the page markup, or {@code null} when it cannot be read.
   */
  String loadConfirmationPage(String resourcePath) {
    ServletContext context = servletContext;
    if (context == null) {
      return null;
    }
    try (InputStream in = context.getResourceAsStream(resourcePath)) {
      if (in == null) {
        return null;
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error("Failed to read confirmation page {}", resourcePath, e);
      return null;
    }
  }
}
