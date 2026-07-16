/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.security.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for URLValidation — SSRF prevention + allow/block lists (issue #1205). */
@DisplayName("URLValidation - SSRF Prevention")
class URLValidationTest {

  @BeforeEach
  void resetConfig() {
    URLValidationConfig.resetDefault();
    URLValidationConfig.setDefault(new URLValidationConfig());
  }

  @AfterEach
  void clear() {
    URLValidationConfig.resetDefault();
  }

  @Nested
  @DisplayName("Baseline public and loopback")
  class Baseline {

    @Test
    void allowHttpsPublic() throws Exception {
      URLValidation.validateURL(new URL("https://example.com/api/resource"));
    }

    @Test
    void allowHttpPort80() throws Exception {
      URLValidation.validateURL(new URL("http://example.com/path"));
    }

    @Test
    void rejectNonStandardPortWithoutAllow() throws Exception {
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(new URL("http://example.com:8080/api")));
    }

    @Test
    void allowLocalhostHighPort() throws Exception {
      URLValidation.validateURL(new URL("http://localhost:8080/api"));
    }

    @Test
    void allowLoopback() throws Exception {
      URLValidation.validateURL(new URL("http://127.0.0.1:9992/"));
    }
  }

  @Nested
  @DisplayName("Hard / default blocks")
  class Blocks {

    @Test
    void rejectAwsMetadata() throws Exception {
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(new URL("http://169.254.169.254/latest/meta-data/")));
    }

    @Test
    void rejectPrivateWithoutAllow() throws Exception {
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(new URL("http://10.0.0.5/api")));
    }

    @Test
    void rejectFileScheme() throws Exception {
      assertThrows(
          SecurityException.class, () -> URLValidation.validateURL(new URL("file:///etc/passwd")));
    }

    @Test
    void blockListWinsOverAllow() throws Exception {
      URLValidationConfig cfg =
          URLValidationConfig.builder()
              .addAllowPattern("http://169.254.169.254/*")
              .addBlockPattern("http://169.254.169.254/*")
              .build();
      // Hard block also applies first for metadata host
      assertThrows(
          SecurityException.class,
          () ->
              URLValidation.validateURL(
                  new URL("http://169.254.169.254/latest/meta-data/"), cfg));
    }

    @Test
    void blockListWinsOverAllowForPrivate() throws Exception {
      URLValidationConfig cfg =
          URLValidationConfig.builder()
              .addAllowPattern("http://10.0.0.5/*")
              .addBlockPattern("http://10.0.0.5/*")
              .build();
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(new URL("http://10.0.0.5/api"), cfg));
    }
  }

  @Nested
  @DisplayName("Additive allow list (US1)")
  class AllowList {

    @Test
    void allowPrivateWithPattern() throws Exception {
      URLValidationConfig cfg =
          URLValidationConfig.builder().addAllowPattern("http://10.0.0.5/*").build();
      assertDoesNotThrow(
          () -> URLValidation.validateURL(new URL("http://10.0.0.5/api/status"), cfg));
    }

    @Test
    void allowCustomPortWithPattern() throws Exception {
      URLValidationConfig cfg =
          URLValidationConfig.builder()
              .addAllowPattern("http://api.example.com:8080/*")
              .build();
      assertDoesNotThrow(
          () -> URLValidation.validateURL(new URL("http://api.example.com:8080/v1"), cfg));
    }

    @Test
    void allowPathScopedPrivateHost() throws Exception {
      // Path scope only restricts non-baseline targets (private/custom-port)
      URLValidationConfig cfg =
          URLValidationConfig.builder()
              .addAllowPattern("http://10.0.0.9/v1/*")
              .build();
      assertDoesNotThrow(
          () -> URLValidation.validateURL(new URL("http://10.0.0.9/v1/translate"), cfg));
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(new URL("http://10.0.0.9/admin"), cfg));
    }

    @Test
    void emptyAllowStillPermitsBaselinePublic() throws Exception {
      URLValidationConfig cfg = new URLValidationConfig();
      assertDoesNotThrow(
          () -> URLValidation.validateURL(new URL("https://example.com/ok"), cfg));
    }

    @Test
    void loneStarDoesNotAllowAll() throws Exception {
      // builder ignores "*"; empty allow
      URLValidationConfig cfg = URLValidationConfig.builder().addAllowPattern("*").build();
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(new URL("http://10.1.1.1/x"), cfg));
    }
  }

  @Nested
  @DisplayName("Input validation")
  class Input {

    @Test
    void nullUrl() {
      assertThrows(IllegalArgumentException.class, () -> URLValidation.validateURL(null));
    }

    @Test
    void emptyString() {
      assertThrows(
          IllegalArgumentException.class, () -> URLValidation.validateURLString(""));
    }

    @Test
    void malformedString() {
      assertThrows(MalformedURLException.class, () -> URLValidation.validateURLString("not a url"));
    }
  }
}
