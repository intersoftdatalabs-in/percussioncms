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
package com.percussion.utils.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Simple test for the PSModernHttpClient to verify basic functionality */
class PSModernHttpClientTest {

    private static final String BASE_URL = "https://httpbin.org";

    @Test
    void testGetReturnsResponse() throws IOException {
        var client = new PSModernHttpClient(BASE_URL);
        String response = client.get("/get");
        assertTrue(response.contains("\"url\": \"https://httpbin.org/get\""));
    }

    @Test
    void testGetWithParams() throws IOException {
        var client = new PSModernHttpClient(BASE_URL);
        Map<String, String> params = new HashMap<>();
        params.put("foo", "bar");
        String response = client.get("/get", params);
        assertTrue(response.contains("\"foo\": \"bar\""));
    }

    @Test
    void testGetBinaryReturnsInputStream() throws IOException {
        var client = new PSModernHttpClient(BASE_URL);
        try (InputStream is = client.getBinary("/image/png")) {
            assertNotNull(is);
            byte[] buffer = new byte[8];
            int read = is.read(buffer);
            assertTrue(read > 0);
        }
    }

    @Test
    void testPostReturnsResponse() throws IOException {
        var client = new PSModernHttpClient(BASE_URL);
        String body = "{\"hello\":\"world\"}";
        String response = client.post("/post", body);
        assertTrue(response.contains("\"hello\": \"world\""));
    }

    @Test
    void testPostFormReturnsResponse() throws IOException {
        var client = new PSModernHttpClient(BASE_URL);
        Map<String, String> params = new HashMap<>();
        params.put("foo", "bar");
        String response = client.postForm("/post", params);
        assertTrue(response.contains("\"foo\": \"bar\""));
    }

    @Test
    void testPutReturnsResponse() throws IOException {
        var client = new PSModernHttpClient(BASE_URL);
        String body = "{\"update\":\"true\"}";
        String response = client.put("/put", body);
        assertTrue(response.contains("\"update\": \"true\""));
    }

    @Test
    void testDeleteReturnsResponse() throws IOException {
        var client = new PSModernHttpClient(BASE_URL);
        String response = client.delete("/delete");
        assertTrue(response.contains("\"url\": \"https://httpbin.org/delete\""));
    }

    @Test
    void testAddHeader() throws IOException {
        var client = new PSModernHttpClient(BASE_URL);
        client.addHeader("X-Test-Header", "SunnySal");
        String response = client.get("/headers");
        assertTrue(response.contains("\"X-Test-Header\": \"SunnySal\""));
    }

    @Test
    void testSetTimeout() {
        var client = new PSModernHttpClient(BASE_URL);
        client.setTimeout(Duration.ofMillis(1));
        IOException thrown = assertThrows(IOException.class, () -> client.get("/delay/2"));
        assertTrue(thrown.getMessage().contains("HTTP connect timed out"), "Expected timeout exception not thrown, was: " + thrown.getMessage());
    }

    @Test
    void testErrorResponseThrowsIOException() {
        var client = new PSModernHttpClient(BASE_URL);
        IOException thrown = assertThrows(IOException.class, () -> client.get("/status/404"));
        assertTrue(thrown.getMessage().contains("HTTP Error 404"));
    }
}
