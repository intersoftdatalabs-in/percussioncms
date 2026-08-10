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
package com.percussion.delivery.metadata.rdbms.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.delivery.metadata.extractor.data.PSMetadataEntry;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Structural coverage for metadata JPA / DTO real-fixes: final entity classes (this-escape) and
 * dropped Java {@link Serializable} on entities/DTOs that are not Java-serialized.
 */
public class PSDbEntityStructureTest {

  @Test
  public void entityClassesAreFinal() {
    assertTrue(Modifier.isFinal(PSDbBlogPostVisit.class.getModifiers()));
    assertTrue(Modifier.isFinal(PSDbCookieConsent.class.getModifiers()));
    assertTrue(Modifier.isFinal(PSDbMetadataEntry.class.getModifiers()));
    assertTrue(Modifier.isFinal(PSDbMetadataProperty.class.getModifiers()));
  }

  @Test
  public void entitiesAndExtractorEntryAreNotSerializable() {
    assertFalse(Serializable.class.isAssignableFrom(PSDbBlogPostVisit.class));
    assertFalse(Serializable.class.isAssignableFrom(PSDbCookieConsent.class));
    assertFalse(Serializable.class.isAssignableFrom(PSDbMetadataEntry.class));
    assertFalse(Serializable.class.isAssignableFrom(PSDbMetadataProperty.class));
    assertFalse(Serializable.class.isAssignableFrom(PSMetadataEntry.class));
  }

  @Test
  public void blogPostVisitConstructorAssignsFields() {
    LocalDate hit = LocalDate.of(2023, 11, 14);
    PSDbBlogPostVisit visit =
        new PSDbBlogPostVisit("/site/blog/post.html", hit, BigInteger.valueOf(3));
    assertEquals("/site/blog/post.html", visit.getPagepath());
    assertEquals(hit, visit.getHitDate());
    assertEquals(BigInteger.valueOf(3), visit.getHitCount());
  }

  @Test
  public void cookieConsentConstructorAssignsFields() {
    Instant consent = Instant.ofEpochMilli(1_700_000_000_000L);
    PSDbCookieConsent entity = new PSDbCookieConsent("site", "svc", consent, "10.0.0.1", true);
    assertEquals("site", entity.getSiteName());
    assertEquals("svc", entity.getService());
    assertEquals("10.0.0.1", entity.getIP());
    assertTrue(entity.getOptIn());
    assertEquals(consent, entity.getConsentDate());
  }

  @Test
  public void metadataEntryConstructorHashesPagepath() {
    PSDbMetadataEntry entry =
        new PSDbMetadataEntry("foo.html", "/folder", "/site/folder/foo.html", "page", "site");
    assertEquals("foo.html", entry.getName());
    assertEquals("/folder", entry.getFolder());
    assertEquals("/site/folder/foo.html", entry.getPagepath());
    assertEquals("page", entry.getType());
    assertEquals("site", entry.getSite());
    assertTrue(entry.getPagepathHash() != null && !entry.getPagepathHash().isEmpty());
  }
}
