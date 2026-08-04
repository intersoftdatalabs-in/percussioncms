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
package com.percussion.rx.publisher.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PSDemandPublishServlet} publishing-status redirect URL assembly (issue
 * #1842).
 *
 * <p>Asserts context-relative URL shape with pure string components — no filesystem path
 * construction — so asserts are portable on Windows and Unix. Peer: {@code PSRunEditionTest}
 * JobPubLog rewire (#1844).
 */
class PSDemandPublishServletTest {

  @Test
  void buildPublishingStatusRedirectURL_emptyContext_matchesModernShellStatus() {
    String url = PSDemandPublishServlet.buildPublishingStatusRedirectURL("");

    assertEquals("/cm/app/?view=publish&section=status", url);
  }

  @Test
  void buildPublishingStatusRedirectURL_withRequestRoot_prefixesRoot() {
    String url = PSDemandPublishServlet.buildPublishingStatusRedirectURL("/Rhythmyx");

    assertEquals("/Rhythmyx/cm/app/?view=publish&section=status", url);
  }

  @Test
  void buildPublishingStatusRedirectURL_nullContext_treatedAsEmpty() {
    String url = PSDemandPublishServlet.buildPublishingStatusRedirectURL(null);

    assertEquals("/cm/app/?view=publish&section=status", url);
  }

  @Test
  void buildPublishingStatusRedirectURL_dropsLegacyDemandPublishJsp() {
    String url = PSDemandPublishServlet.buildPublishingStatusRedirectURL("/Rhythmyx");

    assertFalse(url.contains("DemandPublish"));
    assertFalse(url.contains("pubruntime"));
    assertFalse(url.contains(".jsp"));
    assertFalse(url.contains("requestid"));
    assertTrue(url.contains("view=publish"));
    assertTrue(url.contains("section=status"));
    // URL path separator is always '/'; never OS file separator
    assertFalse(url.contains("\\"));
  }

  @Test
  void publishingStatusPath_constant_matchesItemPublishPathsPeer() {
    assertEquals(
        "/cm/app/?view=publish&section=status", PSDemandPublishServlet.PUBLISHING_STATUS_PATH);
  }
}
