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
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.Test;

/**
 * Behavioral equivalence for the #2045 deprecation-modernization batch: each replacement call site
 * must preserve the observable behavior of the deprecated API it replaces. Headless-safe: no
 * windows are created.
 */
public class PSDeprecationModernizationTest {

  @Test
  @SuppressWarnings("deprecation") // legacy mask referenced only to prove binding equivalence
  public void ctrlDownMaskProducesSameCopyPasteBindings() {
    assertEquals(
        KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK, true),
        KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK, true));
    assertEquals(
        KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK, true),
        KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK, true));
  }

  @Test
  public void stringsCsContainsMatchesLoginPanelSemantics() {
    assertTrue(
        Strings.CS.contains(
            "Login failed: Connection refused: connect to host",
            "Connection refused: connect"));
    assertTrue(
        Strings.CS.contains(
            "Login failed: java.net.UnknownHostException: cms", "java.net.UnknownHostException:"));
    assertFalse(Strings.CS.contains("Login failed", "Connection refused: connect"));
    assertFalse(Strings.CS.contains(null, "Connection refused: connect"));
  }

  @Test
  public void stringsCsLastIndexOfMatchesFileSaverSemantics() {
    assertEquals(7, Strings.CS.lastIndexOf("my_file.pdf", "."));
    assertEquals(-1, Strings.CS.lastIndexOf("noextension", "."));
    assertEquals(-1, Strings.CS.lastIndexOf(null, "."));
  }

  @Test
  public void passwordCharsRoundTripThroughGetPassword() {
    JPasswordField field = new JPasswordField();
    field.setText("s3cret");
    assertEquals("s3cret", new String(field.getPassword()));
    field.setText("");
    assertEquals("", new String(field.getPassword()));
    assertEquals(0, new String(field.getPassword()).trim().length());
  }

  @Test
  public void declaredConstructorInstantiatesDefaultAjaxSwingWrapper() throws Exception {
    assertNotNull(PSDefaultAjaxSwingWrapper.class.getDeclaredConstructor().newInstance());
  }
}
