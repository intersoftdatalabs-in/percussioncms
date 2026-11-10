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
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.webservices.security.data.PSLocale;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for the {@code getLocaleList} helpers (#5851): URL building, payload reading,
 * and JSON parsing must preserve the legacy contract after the complexity refactor.
 */
public class PSContentExplorerLoginPanelLocaleTest {

  @Test
  public void buildLocaleUrlAppendsLocaleJsp() {
    URL withSlash = PSContentExplorerLoginPanel.buildLocaleUrl("http://localhost:9980/Rhythmyx/");
    assertNotNull(withSlash);
    assertTrue(withSlash.toString().endsWith("/locale.jsp"));

    URL withoutSlash =
        PSContentExplorerLoginPanel.buildLocaleUrl("http://localhost:9980/Rhythmyx");
    assertNotNull(withoutSlash);
    assertTrue(withoutSlash.toString().endsWith("/locale.jsp"));
  }

  @Test
  public void readLocalePayloadKeepsLastNonEmptyLine() throws IOException {
    String payload =
        PSContentExplorerLoginPanel.readLocalePayload(
            new BufferedReader(new StringReader("\n{\"a\":1}\n\n{\"activelocales\":[]}")));
    assertEquals("{\"activelocales\":[]}", payload);
  }

  @Test
  public void readLocalePayloadDefaultsToEmptyObject() throws IOException {
    assertEquals(
        "{}",
        PSContentExplorerLoginPanel.readLocalePayload(new BufferedReader(new StringReader("\n"))));
    assertEquals(
        "{}",
        PSContentExplorerLoginPanel.readLocalePayload(new BufferedReader(new StringReader(""))));
  }

  @Test
  public void parseLocaleJsonMapsCodeAndLabel() {
    List<PSLocale> locales =
        PSContentExplorerLoginPanel.parseLocaleJson(
            "{\"activelocales\":["
                + "{\"localecode\":\"en-us\",\"localedisplayname\":\"English (US)\"},"
                + "{\"localecode\":\"de-de\",\"localedisplayname\":\"German\"}]}");
    assertEquals(2, locales.size());
    assertEquals("en-us", locales.get(0).getCode());
    assertEquals("English (US)", locales.get(0).getLabel());
    assertEquals("de-de", locales.get(1).getCode());
    assertEquals("German", locales.get(1).getLabel());
  }

  @Test
  public void parseLocaleJsonReturnsEmptyOnInvalidPayload() {
    assertTrue(PSContentExplorerLoginPanel.parseLocaleJson("{}").isEmpty());
    assertTrue(PSContentExplorerLoginPanel.parseLocaleJson("not-json").isEmpty());
  }
}
