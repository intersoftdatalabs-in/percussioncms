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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.auth.mfa.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.auth.mfa.impl.PSWebAuthnConfigResolver.PSWebAuthnRpConfig;
import com.webauthn4j.data.client.Origin;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class PSWebAuthnConfigResolverTest {

  private final PSWebAuthnConfigResolver resolver = new PSWebAuthnConfigResolver();

  @Test
  void resolvesExplicitRpIdAndAllowedHttpsOrigins() {
    Properties props = new Properties();
    props.setProperty(PSWebAuthnConfigResolver.PROP_RP_ID, "example.com");
    props.setProperty(PSWebAuthnConfigResolver.PROP_RP_NAME, "Acme CMS");
    props.setProperty(
        PSWebAuthnConfigResolver.PROP_ALLOWED_ORIGINS,
        "https://cms.example.com:8443/Rhythmyx, https://admin.example.com");
    props.setProperty(PSWebAuthnConfigResolver.PROP_USER_VERIFICATION, "required");

    PSWebAuthnRpConfig cfg = resolver.resolve(props, "https://cms.example.com:8443");
    assertEquals("example.com", cfg.rpId());
    assertEquals("Acme CMS", cfg.rpName());
    assertTrue(cfg.registrationUvRequired());
    assertTrue(cfg.allowedOrigins().contains(Origin.create("https://cms.example.com:8443")));
    assertTrue(cfg.allowedOrigins().contains(Origin.create("https://admin.example.com")));
  }

  @Test
  void rejectsHostMismatchAndNonHttpsOriginsForExplicitRp() {
    Properties mismatch = new Properties();
    mismatch.setProperty(PSWebAuthnConfigResolver.PROP_RP_ID, "cms.example.com");
    mismatch.setProperty(
        PSWebAuthnConfigResolver.PROP_ALLOWED_ORIGINS, "https://evil.example.org");
    assertThrows(IllegalStateException.class, () -> resolver.resolve(mismatch, null));

    Properties httpProd = new Properties();
    httpProd.setProperty(PSWebAuthnConfigResolver.PROP_RP_ID, "cms.example.com");
    httpProd.setProperty(
        PSWebAuthnConfigResolver.PROP_ALLOWED_ORIGINS, "http://cms.example.com");
    assertThrows(IllegalStateException.class, () -> resolver.resolve(httpProd, null));
  }

  @Test
  void fallsBackToPublicBaseUrlThenLocalhostOnlyAndFailsClosedForNonLocalhost() {
    Properties publicBase = new Properties();
    publicBase.setProperty(
        PSWebAuthnConfigResolver.PROP_PUBLIC_BASE_URL, "https://cms.corp.example/Rhythmyx");
    PSWebAuthnRpConfig baseCfg = resolver.resolve(publicBase, null);
    assertEquals("cms.corp.example", baseCfg.rpId());
    assertEquals(1, baseCfg.allowedOrigins().size());
    assertTrue(baseCfg.allowedOrigins().contains(Origin.create("https://cms.corp.example")));
    assertFalse(baseCfg.registrationUvRequired());

    Properties empty = new Properties();
    PSWebAuthnRpConfig localCfg = resolver.resolve(empty, "http://localhost:9992");
    assertEquals("localhost", localCfg.rpId());
    assertTrue(localCfg.allowedOrigins().contains(Origin.create("http://localhost:9992")));

    assertThrows(
        IllegalStateException.class, () -> resolver.resolve(empty, "https://cms.example.com"));
  }
}
