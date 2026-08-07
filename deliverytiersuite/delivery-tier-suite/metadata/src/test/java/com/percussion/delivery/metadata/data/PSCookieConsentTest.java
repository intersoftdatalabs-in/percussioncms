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
package com.percussion.delivery.metadata.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Covers direct field assignment constructors for cookie consent DTOs (this-escape real-fix) and
 * finality of {@link PSCookieConsentQuery}.
 */
public class PSCookieConsentTest {

  @Test
  public void cookieConsentQueryIsFinal() {
    assertTrue(Modifier.isFinal(PSCookieConsentQuery.class.getModifiers()));
  }

  @Test
  public void populatedConstructorAssignsInstantConsentDate() {
    Instant consent = Instant.ofEpochMilli(1_700_000_000_000L);
    PSCookieConsent entry =
        new PSCookieConsent("siteA", "analytics", consent, "127.0.0.1", true);

    assertEquals("siteA", entry.getSiteName());
    assertEquals("analytics", entry.getService());
    assertEquals("127.0.0.1", entry.getIP());
    assertTrue(entry.getOptIn());
    assertEquals(consent, entry.getConsentDate());
  }

  @Test
  public void populatedConstructorRejectsNulls() {
    Instant now = Instant.now();
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSCookieConsent(null, "svc", now, "1.1.1.1", true));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSCookieConsent("site", null, now, "1.1.1.1", true));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSCookieConsent("site", "svc", null, "1.1.1.1", true));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSCookieConsent("site", "svc", now, null, true));
  }

  @Test
  public void cookieConsentQueryConstructorSetsServices() {
    Instant now = Instant.now();
    List<String> services = Arrays.asList("cookie-a", "cookie-b");
    PSCookieConsentQuery query = new PSCookieConsentQuery("mysite", now, true, services);

    assertEquals("mysite", query.getSiteName());
    assertEquals(services, query.getServices());
    assertEquals("undefined", query.getService());
    assertEquals("undefined", query.getIP());
  }

  @Test
  public void cookieConsentQueryRejectsNullServices() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSCookieConsentQuery("site", Instant.now(), true, null));
  }
}
