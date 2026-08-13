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
package com.percussion.webui.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Behavioral tests for classic JSP → SPA view redirect (#3306 review). */
public class PSLegacyViewRedirectTest {

  @Test
  public void emptyQueryForcesDesign() {
    assertEquals("/cm/app/?view=design", PSLegacyViewRedirect.buildLocation("design", null));
    assertEquals("/cm/app/?view=design", PSLegacyViewRedirect.buildLocation("design", ""));
  }

  @Test
  public void existingViewIsOverriddenNotPreserved() {
    assertEquals("/cm/app/?view=design", PSLegacyViewRedirect.buildLocation("design", "view=admin"));
    assertEquals(
        "/cm/app/?view=design&section=templates",
        PSLegacyViewRedirect.buildLocation("design", "view=admin&section=templates"));
    assertEquals(
        "/cm/app/?view=design&_ts=1",
        PSLegacyViewRedirect.buildLocation("design", "VIEW=editor&_ts=1"));
  }

  @Test
  public void preservesSafePairsAndDropsMarkup() {
    assertEquals(
        "/cm/app/?view=design&site=Site1",
        PSLegacyViewRedirect.buildLocation("design", "site=Site1"));
    assertEquals(
        "/cm/app/?view=design",
        PSLegacyViewRedirect.buildLocation("design", "x=\"><script>alert(1)</script>"));
    assertEquals(
        "/cm/app/?view=design",
        PSLegacyViewRedirect.buildLocation("design", "x=1\r\nLocation:%20https://evil"));
  }

  @Test
  public void invalidForcedViewFallsBackToHome() {
    assertEquals("/cm/app/?view=home", PSLegacyViewRedirect.buildLocation("../x", null));
    assertEquals("/cm/app/?view=home", PSLegacyViewRedirect.buildLocation(null, null));
  }

  @Test
  public void locationHasNoCrLf() {
    String loc = PSLegacyViewRedirect.buildLocation("design", "a=1\nb=2");
    assertFalse(loc.contains("\r"));
    assertFalse(loc.contains("\n"));
    assertTrue(loc.startsWith("/cm/app/?view=design"));
  }

  @Test
  public void htmlAttributeEscapesAmpersandAndQuotes() {
    String loc = PSLegacyViewRedirect.buildLocation("design", "section=templates");
    assertEquals("/cm/app/?view=design&section=templates", loc);
    assertEquals(
        "/cm/app/?view=design&amp;section=templates", PSLegacyViewRedirect.escapeHtmlAttribute(loc));
    assertEquals("&quot;&lt;x&gt;&#39;", PSLegacyViewRedirect.escapeHtmlAttribute("\"<x>'"));
    assertEquals("", PSLegacyViewRedirect.escapeHtmlAttribute(null));
  }
}
