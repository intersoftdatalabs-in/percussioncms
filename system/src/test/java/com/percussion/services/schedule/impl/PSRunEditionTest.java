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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.schedule.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PSRunEdition} publishing-log URL assembly (issue #1844).
 *
 * <p>Asserts absolute URL shape with pure string components — no filesystem path construction — so
 * asserts are portable on Windows and Unix.
 */
class PSRunEditionTest {

  @Test
  void buildPublishingLogURL_httpEmptyRoot_matchesModernShellLogs() {
    String url = PSRunEdition.buildPublishingLogURL("http", "cms.example.com", 9992, "");

    assertEquals("http://cms.example.com:9992/cm/app/?view=publish&section=logs", url);
  }

  @Test
  void buildPublishingLogURL_httpsWithRequestRoot_prefixesRoot() {
    String url = PSRunEdition.buildPublishingLogURL("https", "cms.example.com", 8443, "/Rhythmyx");

    assertEquals("https://cms.example.com:8443/Rhythmyx/cm/app/?view=publish&section=logs", url);
  }

  @Test
  void buildPublishingLogURL_nullRequestRoot_treatedAsEmpty() {
    String url = PSRunEdition.buildPublishingLogURL("http", "localhost", 8080, null);

    assertEquals("http://localhost:8080/cm/app/?view=publish&section=logs", url);
  }

  @Test
  void buildPublishingLogURL_dropsLegacyJobPubLogFacesAndJobIdQuery() {
    String url = PSRunEdition.buildPublishingLogURL("http", "host.local", 80, "/Rhythmyx");

    assertFalse(url.contains("JobPubLog"));
    assertFalse(url.contains(".faces"));
    assertFalse(url.contains("sys_publishingJobId"));
    assertFalse(url.contains("sys_publishjobid"));
    assertTrue(url.contains("view=publish"));
    assertTrue(url.contains("section=logs"));
    // URL path separator is always '/'; never OS file separator
    assertFalse(url.contains("\\"));
  }

  @Test
  void buildPublishingLogURL_rejectsBlankProtocolOrHost() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSRunEdition.buildPublishingLogURL("", "host", 80, ""));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSRunEdition.buildPublishingLogURL("http", "  ", 80, ""));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSRunEdition.buildPublishingLogURL(null, "host", 80, ""));
    // StringUtils.isBlank(null) is true — host must be non-null and non-blank
    assertThrows(
        IllegalArgumentException.class,
        () -> PSRunEdition.buildPublishingLogURL("http", null, 80, ""));
  }
}
