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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PSEc2MetadataClient} using a local {@link HttpServer} to simulate IMDS (no
 * live AWS). Covers IMDSv2 success, IMDSv1-only fallback, v2-required (v1 fails / v2 ok), and
 * non-EC2 (connection refused / error) paths.
 */
class PSEc2MetadataClientTest {

  private HttpServer server;
  private String baseUrl;
  private HttpClient httpClient;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.start();
    baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(500))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void imdsV2_success_detectsEc2AndRegion() {
    AtomicInteger tokenPuts = new AtomicInteger();
    AtomicBoolean sawTokenHeader = new AtomicBoolean();

    server.createContext(
        PSEc2MetadataClient.TOKEN_PATH,
        exchange -> {
          tokenPuts.incrementAndGet();
          if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "method not allowed");
            return;
          }
          String ttl = exchange.getRequestHeaders().getFirst(PSEc2MetadataClient.TOKEN_TTL_HEADER);
          assertEquals(PSEc2MetadataClient.TOKEN_TTL_SECONDS, ttl);
          respond(exchange, 200, "test-session-token");
        });
    server.createContext(
        PSEc2MetadataClient.META_DATA_ROOT,
        exchange -> {
          String token = exchange.getRequestHeaders().getFirst(PSEc2MetadataClient.TOKEN_HEADER);
          if ("test-session-token".equals(token)) {
            sawTokenHeader.set(true);
            respond(exchange, 200, "ami-id\nhostname\n");
          } else {
            respond(exchange, 401, "Unauthorized");
          }
        });
    server.createContext(
        PSEc2MetadataClient.AVAILABILITY_ZONE_PATH,
        exchange -> {
          String token = exchange.getRequestHeaders().getFirst(PSEc2MetadataClient.TOKEN_HEADER);
          if ("test-session-token".equals(token)) {
            respond(exchange, 200, "us-east-1a");
          } else {
            respond(exchange, 401, "Unauthorized");
          }
        });

    PSEc2MetadataClient client = new PSEc2MetadataClient(baseUrl, httpClient);

    assertTrue(client.isAvailable());
    assertTrue(tokenPuts.get() >= 1);
    assertTrue(sawTokenHeader.get());
    assertEquals("us-east-1a", client.getAvailabilityZone());
    assertEquals("us-east-1", client.getRegion());
  }

  @Test
  void imdsV1Only_tokenFails_fallbackGetSucceeds() {
    AtomicInteger unauthGets = new AtomicInteger();

    server.createContext(
        PSEc2MetadataClient.TOKEN_PATH, exchange -> respond(exchange, 404, "no token on this AMI"));
    server.createContext(
        PSEc2MetadataClient.META_DATA_ROOT,
        exchange -> {
          String token = exchange.getRequestHeaders().getFirst(PSEc2MetadataClient.TOKEN_HEADER);
          if (token != null) {
            respond(exchange, 401, "unexpected token");
            return;
          }
          unauthGets.incrementAndGet();
          respond(exchange, 200, "instance-id\n");
        });

    PSEc2MetadataClient client = new PSEc2MetadataClient(baseUrl, httpClient);
    assertTrue(client.isAvailable());
    assertTrue(unauthGets.get() >= 1);
  }

  @Test
  void imdsV2Required_v1Fails_v2Ok_doesNotFalseNegative() {
    // Simulates HttpTokens=required: unauthenticated GET fails; token + header succeeds.
    server.createContext(
        PSEc2MetadataClient.TOKEN_PATH, exchange -> respond(exchange, 200, "v2-token"));
    server.createContext(
        PSEc2MetadataClient.META_DATA_ROOT,
        exchange -> {
          String token = exchange.getRequestHeaders().getFirst(PSEc2MetadataClient.TOKEN_HEADER);
          if ("v2-token".equals(token)) {
            respond(exchange, 200, "ok");
          } else {
            respond(exchange, 401, "IMDSv1 disabled");
          }
        });

    PSEc2MetadataClient client = new PSEc2MetadataClient(baseUrl, httpClient);
    assertTrue(client.isAvailable(), "must succeed via IMDSv2 when v1 is rejected");
  }

  @Test
  void nonEc2_connectionRefused_returnsFalse() {
    // Port with no listener → connection refused / fail fast.
    String deadBase = "http://127.0.0.1:" + findFreePortThenClose();
    PSEc2MetadataClient client = new PSEc2MetadataClient(deadBase, httpClient);
    assertFalse(client.isAvailable());
    assertNull(client.getRegion());
  }

  @Test
  void nonEc2_errorStatus_returnsFalse() {
    server.createContext(PSEc2MetadataClient.TOKEN_PATH, exchange -> respond(exchange, 500, "err"));
    server.createContext(
        PSEc2MetadataClient.META_DATA_ROOT, exchange -> respond(exchange, 500, "err"));

    PSEc2MetadataClient client = new PSEc2MetadataClient(baseUrl, httpClient);
    assertFalse(client.isAvailable());
  }

  private static int findFreePortThenClose() {
    try (var socket = new java.net.ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }
}
