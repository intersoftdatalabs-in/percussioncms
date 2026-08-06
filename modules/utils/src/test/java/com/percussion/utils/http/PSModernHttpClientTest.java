/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.utils.http;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Simple test for the PSModernHttpClient to verify basic functionality */
class PSModernHttpClientTest {

  private static HttpServer server;
  private static String baseUrl;

  @BeforeAll
  static void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

    server.createContext(
        "/get",
        exchange -> {
          String query = exchange.getRequestURI().getQuery();
          String response;
          if (query != null && query.contains("foo=bar")) {
            response = "{\"foo\": \"bar\"}";
          } else {
            response = "{\"url\": \"" + baseUrl + "/get\"}";
          }
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });

    server.createContext(
        "/post",
        exchange -> {
          InputStream is = exchange.getRequestBody();
          String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
          String response = "";
          if (body.contains("foo=bar")) {
            response = "{\"foo\": \"bar\"}";
          } else if (body.contains("hello")) {
            response = "{\"hello\": \"world\"}";
          }
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });

    server.createContext(
        "/put",
        exchange -> {
          InputStream is = exchange.getRequestBody();
          String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
          String response = "";
          if (body.contains("update")) {
            response = "{\"update\": \"true\"}";
          }
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });

    server.createContext(
        "/delete",
        exchange -> {
          String response = "{\"url\": \"" + baseUrl + "/delete\"}";
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });

    server.createContext(
        "/headers",
        exchange -> {
          String testHeader = exchange.getRequestHeaders().getFirst("X-Test-Header");
          String response = "{\"X-Test-Header\": \"" + testHeader + "\"}";
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });

    server.createContext(
        "/delay/2",
        exchange -> {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          String response = "{}";
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });

    server.createContext(
        "/status/404",
        exchange -> {
          exchange.sendResponseHeaders(404, -1);
          exchange.close();
        });

    server.createContext(
        "/image/png",
        exchange -> {
          byte[] dummyPng = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
          exchange.getResponseHeaders().set("Content-Type", "image/png");
          exchange.sendResponseHeaders(200, dummyPng.length);
          exchange.getResponseBody().write(dummyPng);
          exchange.close();
        });

    server.start();
    baseUrl = "http://localhost:" + server.getAddress().getPort();
  }

  @AfterAll
  static void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void testGetReturnsResponse() throws IOException {
    var client = new PSModernHttpClient(baseUrl);
    String response = client.get("/get");
    assertTrue(response.contains("\"url\": \"" + baseUrl + "/get\""));
  }

  @Test
  void testGetWithParams() throws IOException {
    var client = new PSModernHttpClient(baseUrl);
    Map<String, String> params = new HashMap<>();
    params.put("foo", "bar");
    String response = client.get("/get", params);
    assertTrue(response.contains("\"foo\": \"bar\""));
  }

  @Test
  void testGetBinaryReturnsInputStream() throws IOException {
    var client = new PSModernHttpClient(baseUrl);
    try (InputStream is = client.getBinary("/image/png")) {
      assertNotNull(is);
      byte[] buffer = new byte[8];
      int read = is.read(buffer);
      assertTrue(read > 0);
    }
  }

  @Test
  void testPostReturnsResponse() throws IOException {
    var client = new PSModernHttpClient(baseUrl);
    String body = "{\"hello\":\"world\"}";
    String response = client.post("/post", body);
    assertTrue(response.contains("\"hello\": \"world\""));
  }

  @Test
  void testPostFormReturnsResponse() throws IOException {
    var client = new PSModernHttpClient(baseUrl);
    Map<String, String> params = new HashMap<>();
    params.put("foo", "bar");
    String response = client.postForm("/post", params);
    assertTrue(response.contains("\"foo\": \"bar\""));
  }

  @Test
  void testPutReturnsResponse() throws IOException {
    var client = new PSModernHttpClient(baseUrl);
    String body = "{\"update\":\"true\"}";
    String response = client.put("/put", body);
    assertTrue(response.contains("\"update\": \"true\""));
  }

  @Test
  void testDeleteReturnsResponse() throws IOException {
    var client = new PSModernHttpClient(baseUrl);
    String response = client.delete("/delete");
    assertTrue(response.contains("\"url\": \"" + baseUrl + "/delete\""));
  }

  @Test
  void testAddHeader() throws IOException {
    var client = new PSModernHttpClient(baseUrl);
    client.addHeader("X-Test-Header", "SunnySal");
    String response = client.get("/headers");
    assertTrue(response.contains("\"X-Test-Header\": \"SunnySal\""));
  }

  @Test
  void testSetTimeout() {
    var client = new PSModernHttpClient(baseUrl);
    client.setTimeout(Duration.ofMillis(1));
    IOException thrown = assertThrows(IOException.class, () -> client.get("/delay/2"));
    assertTrue(
        thrown.getMessage().contains("timed out")
            || thrown.getMessage().contains("connect timed out")
            || thrown.getMessage().contains("HTTP connect timed out"),
        "Expected timeout exception not thrown, was: " + thrown.getMessage());
  }

  @Test
  void testErrorResponseThrowsIOException() {
    var client = new PSModernHttpClient(baseUrl);
    IOException thrown = assertThrows(IOException.class, () -> client.get("/status/404"));
    assertTrue(thrown.getMessage().contains("HTTP Error 404"));
  }
}
