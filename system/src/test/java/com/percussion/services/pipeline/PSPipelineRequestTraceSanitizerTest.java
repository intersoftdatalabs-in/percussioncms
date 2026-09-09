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

package com.percussion.services.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pipeline request-trace sanitizer (fail-closed secrets)")
class PSPipelineRequestTraceSanitizerTest {

  @Test
  void redactsPasswordTokenAndAuthorizationKeys() {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("sku", "SKU-1");
    params.put("password", "s3cret-password-value");
    params.put("token", "s3cret-token-value");
    params.put("Authorization", "Bearer s3cret-auth-value");
    params.put("apiKey", "s3cret-api-key");

    Map<String, Object> out = PSPipelineRequestTraceSanitizer.sanitizeParams(params);
    assertEquals("SKU-1", out.get("sku"));
    assertEquals(PSPipelineRequestTraceSanitizer.REDACTED, out.get("password"));
    assertEquals(PSPipelineRequestTraceSanitizer.REDACTED, out.get("token"));
    assertEquals(PSPipelineRequestTraceSanitizer.REDACTED, out.get("Authorization"));
    assertEquals(PSPipelineRequestTraceSanitizer.REDACTED, out.get("apiKey"));
    String json = out.toString();
    assertFalse(json.contains("s3cret"));
  }

  @Test
  void redactsNestedMapsAndBearerValues() {
    Map<String, Object> nested = new LinkedHashMap<>();
    nested.put("passwd", "nested-secret");
    nested.put("note", "ok");
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("headers", nested);
    params.put("authHeader", "Bearer abc.def");
    params.put("items", List.of(Map.of("secret", "nope")));

    Map<String, Object> out = PSPipelineRequestTraceSanitizer.sanitizeParams(params);
    @SuppressWarnings("unchecked")
    Map<String, Object> headers = (Map<String, Object>) out.get("headers");
    assertEquals(PSPipelineRequestTraceSanitizer.REDACTED, headers.get("passwd"));
    assertEquals("ok", headers.get("note"));
    assertEquals(PSPipelineRequestTraceSanitizer.REDACTED, out.get("authHeader"));
    assertTrue(PSPipelineRequestTraceSanitizer.isSensitiveKey("PASSWORD"));
    assertFalse(PSPipelineRequestTraceSanitizer.isSensitiveKey("sku"));
  }
}
