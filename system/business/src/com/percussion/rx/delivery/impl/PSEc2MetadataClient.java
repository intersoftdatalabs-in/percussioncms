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
package com.percussion.rx.delivery.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * IMDSv2-aware EC2 instance metadata client used for CMS EC2 detection and region resolution.
 *
 * <p>Amazon Linux 2023+ and many newer AMIs default instance metadata to {@code
 * HttpTokens=required} (IMDSv2 only). A plain IMDSv1 GET against {@code
 * http://169.254.169.254/latest/meta-data/} fails on those hosts, which previously caused the CMS
 * to treat the instance as non-EC2 and force Access Key / Secret for S3 publishing.
 *
 * <p>This client:
 *
 * <ol>
 *   <li>Obtains a short-lived session token via {@code PUT /latest/api/token} with header {@code
 *       X-aws-ec2-metadata-token-ttl-seconds}
 *   <li>Performs GETs with header {@code X-aws-ec2-metadata-token}
 *   <li>Falls back to IMDSv1 (no token) when the token endpoint is unavailable (older AMIs with
 *       {@code HttpTokens=optional})
 *   <li>Uses short connect/request timeouts so non-EC2 hosts fail quickly
 * </ol>
 *
 * <p><b>Operator notes (containers / hop limit):</b> When CMS runs inside a container on EC2, set
 * the instance metadata option {@code HttpPutResponseHopLimit} to at least {@code 2} so the IMDSv2
 * PUT from the container can reach the metadata service. On Amazon Linux 2023+ keep {@code
 * HttpTokens=required}; after this fix the CMS probe no longer requires IMDSv1.
 *
 * @see <a
 *     href="https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html">AWS
 *     IMDS documentation</a>
 */
public class PSEc2MetadataClient {

  /** Link-local EC2 metadata base URL. */
  public static final String DEFAULT_METADATA_BASE = "http://169.254.169.254";

  static final String TOKEN_PATH = "/latest/api/token";
  static final String META_DATA_ROOT = "/latest/meta-data/";
  static final String AVAILABILITY_ZONE_PATH =
      "/latest/meta-data/placement/availability-zone/";

  static final String TOKEN_HEADER = "X-aws-ec2-metadata-token";
  static final String TOKEN_TTL_HEADER = "X-aws-ec2-metadata-token-ttl-seconds";
  static final String TOKEN_TTL_SECONDS = "60";

  private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(1000);
  private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(1000);

  private static final Logger log = LogManager.getLogger(PSEc2MetadataClient.class);

  private final String metadataBase;
  private final HttpClient httpClient;

  /** Production client targeting the EC2 link-local metadata address. */
  public PSEc2MetadataClient() {
    this(DEFAULT_METADATA_BASE, defaultHttpClient());
  }

  /**
   * Package-visible constructor for unit tests (custom base URL / client).
   *
   * @param metadataBase base URL including scheme and host (no trailing path slash required)
   * @param httpClient HTTP client (should use short timeouts)
   */
  PSEc2MetadataClient(String metadataBase, HttpClient httpClient) {
    if (metadataBase == null || metadataBase.isBlank()) {
      throw new IllegalArgumentException("metadataBase must not be blank");
    }
    if (httpClient == null) {
      throw new IllegalArgumentException("httpClient must not be null");
    }
    String base = metadataBase.trim();
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    this.metadataBase = base;
    this.httpClient = httpClient;
  }

  static HttpClient defaultHttpClient() {
    return HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  /**
   * @return {@code true} if the metadata root responds successfully (IMDSv2 or v1)
   */
  public boolean isAvailable() {
    return getMetadata(META_DATA_ROOT) != null;
  }

  /**
   * @return availability zone string (e.g. {@code us-east-1a}), or {@code null} on failure
   */
  public String getAvailabilityZone() {
    String az = getMetadata(AVAILABILITY_ZONE_PATH);
    if (az == null) {
      return null;
    }
    String trimmed = az.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /**
   * Resolves the EC2 region by stripping the trailing AZ letter from the availability zone.
   *
   * @return region id (e.g. {@code us-east-1}), or {@code null} on failure
   */
  public String getRegion() {
    String az = getAvailabilityZone();
    if (az == null || az.length() <= 1) {
      return null;
    }
    return az.substring(0, az.length() - 1);
  }

  /**
   * GET a metadata path using IMDSv2 when possible, with IMDSv1 fallback.
   *
   * @param path absolute path starting with {@code /} (e.g. {@code /latest/meta-data/})
   * @return response body on HTTP 2xx, else {@code null}
   */
  String getMetadata(String path) {
    if (path == null || !path.startsWith("/")) {
      throw new IllegalArgumentException("path must start with /");
    }
    try {
      String token = fetchImdsV2Token();
      if (token != null && !token.isBlank()) {
        String body = getWithOptionalToken(path, token);
        if (body != null) {
          return body;
        }
        log.debug("IMDSv2 GET failed for {}; trying IMDSv1 fallback", path);
      }
      return getWithOptionalToken(path, null);
    } catch (Exception e) {
      log.debug(
          "EC2 metadata probe failed for {} (host is likely not EC2 or IMDS unreachable): {}",
          path,
          e.toString());
      return null;
    }
  }

  private String fetchImdsV2Token() {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(metadataBase + TOKEN_PATH))
              .timeout(REQUEST_TIMEOUT)
              .header(TOKEN_TTL_HEADER, TOKEN_TTL_SECONDS)
              .PUT(HttpRequest.BodyPublishers.noBody())
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (isSuccess(response.statusCode())) {
        String token = response.body();
        return token == null ? null : token.trim();
      }
      log.debug("IMDSv2 token request returned status {}", response.statusCode());
      return null;
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      log.debug("IMDSv2 token request failed: {}", e.toString());
      return null;
    }
  }

  private String getWithOptionalToken(String path, String token)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder()
            .uri(URI.create(metadataBase + path))
            .timeout(REQUEST_TIMEOUT)
            .GET();
    if (token != null && !token.isBlank()) {
      builder.header(TOKEN_HEADER, token);
    }
    HttpResponse<String> response =
        httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    if (isSuccess(response.statusCode())) {
      return response.body();
    }
    log.debug("Metadata GET {} returned status {}", path, response.statusCode());
    return null;
  }

  private static boolean isSuccess(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }
}
