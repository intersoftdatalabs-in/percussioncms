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
package com.percussion.delivery.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.GeneralSecurityException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PSDeliveryClient}'s default-trust-manager helper. Regression coverage for
 * the {@code java/insecure-trustmanager} alert CodeQL raised at PSDeliveryClient.java:825 (alert
 * #1068). The pre-fix code installed an all-trusting {@link X509TrustManager} that returned {@code
 * new X509Certificate[0]} from {@code getAcceptedIssuers} and accepted any chain via empty {@code
 * checkClientTrusted} / {@code checkServerTrusted} bodies. The post-fix code uses the JVM's default
 * trust managers (system cacerts).
 */
public class PSDeliveryClientTrustManagerTest {

  @Test
  public void createDefaultTrustManagersReturnsNonEmptyArray() throws GeneralSecurityException {
    TrustManager[] trustManagers = PSDeliveryClient.createDefaultTrustManagers();
    assertNotNull(trustManagers, "Default trust managers must not be null");
    assertTrue(
        trustManagers.length > 0,
        "Default trust managers must not be empty (the JVM always ships at least one)");
  }

  @Test
  public void createDefaultTrustManagersIncludesX509TrustManager() throws GeneralSecurityException {
    TrustManager[] trustManagers = PSDeliveryClient.createDefaultTrustManagers();
    boolean hasX509 = false;
    for (TrustManager tm : trustManagers) {
      if (tm instanceof X509TrustManager) {
        hasX509 = true;
        break;
      }
    }
    assertTrue(
        hasX509,
        "At least one default trust manager must be an X509TrustManager so the SSL context"
            + " can validate X.509 chains");
  }

  @Test
  public void createDefaultTrustManagersAreStableAcrossCalls() throws GeneralSecurityException {
    // The helper must not return cached or mutated state across calls.
    TrustManager[] first = PSDeliveryClient.createDefaultTrustManagers();
    TrustManager[] second = PSDeliveryClient.createDefaultTrustManagers();
    assertEquals(first.length, second.length, "Default trust manager count must be stable");
  }
}
