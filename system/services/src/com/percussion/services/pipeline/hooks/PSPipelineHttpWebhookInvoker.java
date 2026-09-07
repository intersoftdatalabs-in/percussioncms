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

package com.percussion.services.pipeline.hooks;

import com.percussion.services.pipeline.PSPipelineIrException;
import com.percussion.services.pipeline.http.PSPipelineHttpUrl;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.model.PipelineWebhookHooksIr;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * HTTP webhook pre/post execute deliveries: bundled classpath fixture or live loopback POST.
 *
 * <p>Missing URL is skip (no invented delivery). Present URLs are fail-closed (loopback only, no
 * credentials, no redirects off loopback).
 */
public class PSPipelineHttpWebhookInvoker {

  static final String BUNDLED_RESOURCE = "pipeline-webhook-fixture.json";
  static final int MAX_BODY_BYTES = 1_000_000;
  static final int SNIPPET_CHARS = 500;
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
  private static final String LABEL = "HTTP webhook URL";

  private static final HttpClient HTTP =
      HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NEVER)
          .connectTimeout(CONNECT_TIMEOUT)
          .build();

  public void invokePre(PipelineHookContext context) throws PSPipelineIrException {
    invoke(context, "pre", null);
  }

  public void invokePost(PipelineHookContext context, PipelineExecuteResult result)
      throws PSPipelineIrException {
    invoke(context, "post", result);
  }

  void invoke(PipelineHookContext context, String phase, PipelineExecuteResult result)
      throws PSPipelineIrException {
    if (context == null || context.getResource() == null) {
      return;
    }
    PipelineResourceIr resource = context.getResource();
    PipelineWebhookHooksIr hooks = resource.getWebhookHooks();
    if (hooks == null || !hooks.isPresent()) {
      return;
    }
    String rawUrl = "pre".equals(phase) ? hooks.getPreUrl() : hooks.getPostUrl();
    if (StringUtils.isBlank(rawUrl)) {
      context.addTrace(phase + ":webhook skipped (no URL)");
      if (result != null) {
        result.getMeta().put(phase + "WebhookSkipped", true);
      }
      return;
    }
    if (hooks != null && !PipelineWebhookHooksIr.METHOD_POST.equals(hooks.resolvedMethod())) {
      throw new PSPipelineIrException("HTTP webhook supports POST only in this slice");
    }
    URL safe = PSPipelineHttpUrl.requireSafe(rawUrl, LABEL);
    String payload = buildPayload(context, phase, result);
    Delivery delivery =
        PSPipelineHttpUrl.isBundledWebhookFixture(safe)
            ? bundledDelivery()
            : postLoopback(safe, payload);
    context.addTrace(
        phase
            + ":webhook status="
            + delivery.status
            + " body="
            + snippet(delivery.body));
    if (result != null) {
      result.getMeta().put(phase + "WebhookStatus", delivery.status);
      result.getMeta().put(phase + "WebhookBody", snippet(delivery.body));
    } else {
      context.setPreWebhookStatus(delivery.status);
      context.setPreWebhookBody(snippet(delivery.body));
    }
  }

  static String buildPayload(
      PipelineHookContext context, String phase, PipelineExecuteResult result) {
    JSONObject obj = new JSONObject();
    obj.put("phase", phase);
    String app =
        context.getDocument() != null && context.getDocument().getApp() != null
            ? context.getDocument().getApp().getName()
            : "";
    obj.put("app", app != null ? app : "");
    String resourceName =
        context.getResource() != null ? context.getResource().getName() : "";
    obj.put("resource", resourceName != null ? resourceName : "");
    obj.put(
        "kind",
        context.getResource() != null && context.getResource().getKind() != null
            ? context.getResource().getKind()
            : "");
    if (result != null) {
      obj.put("operation", result.getOperation() != null ? result.getOperation() : "");
      obj.put("rowCount", result.getRowCount());
    }
    return obj.toString();
  }

  static Delivery bundledDelivery() throws PSPipelineIrException {
    return new Delivery(200, readBundledFixture());
  }

  static String readBundledFixture() throws PSPipelineIrException {
    try (InputStream in =
        PSPipelineHttpWebhookInvoker.class.getResourceAsStream(BUNDLED_RESOURCE)) {
      if (in == null) {
        throw new PSPipelineIrException("Bundled HTTP webhook fixture is missing");
      }
      byte[] bytes = in.readAllBytes();
      if (bytes.length > MAX_BODY_BYTES) {
        throw new PSPipelineIrException("Bundled HTTP webhook fixture exceeds size limit");
      }
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new PSPipelineIrException("Failed to read bundled HTTP webhook fixture", e);
    }
  }

  static Delivery postLoopback(URL validated, String jsonBody) throws PSPipelineIrException {
    URI requestUri = PSPipelineHttpUrl.toRequestUri(validated);
    byte[] payload =
        jsonBody != null ? jsonBody.getBytes(StandardCharsets.UTF_8) : new byte[0];
    HttpRequest request =
        HttpRequest.newBuilder(requestUri) // codeql[java/ssrf]
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Accept", "application/json, text/plain;q=0.9, */*;q=0.1")
            .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
            .build();
    HttpResponse<byte[]> response;
    try {
      response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new PSPipelineIrException(
          "HTTP webhook request interrupted: " + PSPipelineHttpUrl.redact(validated), e);
    } catch (IOException e) {
      throw new PSPipelineIrException(
          "HTTP webhook request failed: " + PSPipelineHttpUrl.redact(validated), e);
    }
    int status = response.statusCode();
    if (status >= 300 && status < 400) {
      throw new PSPipelineIrException(
          "HTTP webhook redirect refused (open redirect / SSRF fail-closed): "
              + PSPipelineHttpUrl.redact(validated)
              + " status "
              + status);
    }
    byte[] body = response.body() != null ? response.body() : new byte[0];
    if (body.length > MAX_BODY_BYTES) {
      throw new PSPipelineIrException(
          "HTTP webhook body exceeds "
              + MAX_BODY_BYTES
              + " bytes from "
              + PSPipelineHttpUrl.redact(validated));
    }
    return new Delivery(status, new String(body, StandardCharsets.UTF_8));
  }

  static String snippet(String body) {
    if (body == null) {
      return "";
    }
    String normalized = body.replace("\r\n", "\n").trim();
    if (normalized.length() <= SNIPPET_CHARS) {
      return normalized;
    }
    return normalized.substring(0, SNIPPET_CHARS) + "…";
  }

  static final class Delivery {
    final int status;
    final String body;

    Delivery(int status, String body) {
      this.status = status;
      this.body = body != null ? body : "";
    }
  }
}
