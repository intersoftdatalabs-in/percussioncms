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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import org.apache.commons.io.IOUtils;
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
  @SuppressWarnings("deprecation") // legacy mask referenced only to prove binding equivalence
  public void menuShortcutKeyExPreservesMacClipboardBindings() {
    // Behavioral equivalence for the #5713 getMenuShortcutKeyMask -> getMenuShortcutKeyMaskEx
    // migration in PSContentExplorerApplet.bindMacClipboardShortcuts: AWTKeyStroke normalizes
    // legacy and extended modifier masks to the same KeyStroke, so the copy/paste/cut
    // InputMap bindings are unchanged. Uses the same 2-arg overload as the applet.
    // Headless-safe: KeyStroke/Toolkit only, no windows created.
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    assertEquals(
        KeyStroke.getKeyStroke(KeyEvent.VK_C, toolkit.getMenuShortcutKeyMask()),
        KeyStroke.getKeyStroke(KeyEvent.VK_C, toolkit.getMenuShortcutKeyMaskEx()));
    assertEquals(
        KeyStroke.getKeyStroke(KeyEvent.VK_V, toolkit.getMenuShortcutKeyMask()),
        KeyStroke.getKeyStroke(KeyEvent.VK_V, toolkit.getMenuShortcutKeyMaskEx()));
    assertEquals(
        KeyStroke.getKeyStroke(KeyEvent.VK_X, toolkit.getMenuShortcutKeyMask()),
        KeyStroke.getKeyStroke(KeyEvent.VK_X, toolkit.getMenuShortcutKeyMaskEx()));
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

  @Test
  public void ioUtilsCopyMatchesLegacyCopyStreamSemantics() throws Exception {
    // Behavioral equivalence for the #5712 IOTools.copyStream -> IOUtils.copy migration in
    // PSImageIconLoader: full byte-for-byte copy, including payloads larger than the legacy 8k
    // buffer and the empty-stream edge case. Headless-safe: pure stream copy, no UI.
    byte[] payload = new byte[20000];
    for (int i = 0; i < payload.length; i++) {
      payload[i] = (byte) (i * 31 + 7);
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IOUtils.copy(new ByteArrayInputStream(payload), out);
    assertEquals(payload.length, out.toByteArray().length);
    for (int i = 0; i < payload.length; i++) {
      assertEquals(payload[i], out.toByteArray()[i]);
    }

    ByteArrayOutputStream empty = new ByteArrayOutputStream();
    IOUtils.copy(new ByteArrayInputStream(new byte[0]), empty);
    assertEquals(0, empty.toByteArray().length);
  }

  @Test
  public void loadIconCopiesArchiveBytesThroughCommonsIo() throws Exception {
    // Exercises the migrated PSImageIconLoader.loadIcon(String) path end to end: the
    // src/test/resources/images/CopyProbe.gif fixture (1x1 GIF) is found via the
    // classpath-root-relative lookup and its bytes copied through IOUtils.
    Icon first = PSImageIconLoader.loadIcon("CopyProbe");
    assertNotNull(first, "CopyProbe fixture must load from test resources");

    // The fixture itself must decode to the expected 1x1 image (ImageIO is synchronous and
    // headless-safe; ImageIcon pixel loading is async, so dimensions are pinned here instead).
    try (InputStream in =
        getClass().getClassLoader().getResourceAsStream("images/CopyProbe.gif")) {
      assertNotNull(in, "CopyProbe fixture must be on the test classpath");
      BufferedImage image = ImageIO.read(in);
      assertNotNull(image);
      assertEquals(1, image.getWidth());
      assertEquals(1, image.getHeight());
    }

    // A repeat load re-resolves the same archive entry (the single-arg overload reads the
    // shared cache-or-archive path but never stores; store-through caching lives in the 3-arg
    // applet overload, which needs a live server and is out of scope for this unit test).
    Icon second = PSImageIconLoader.loadIcon("CopyProbe");
    assertNotNull(second, "repeat load must re-resolve the fixture");
  }

  @Test
  public void loadIconReturnsNullForMissingKey() {
    assertNull(PSImageIconLoader.loadIcon("DefinitelyMissingIconXyz123"));
  }
}
