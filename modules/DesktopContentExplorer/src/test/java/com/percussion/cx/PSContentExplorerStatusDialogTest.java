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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Structural and pure-helper coverage for {@link PSContentExplorerStatusDialog} after residual
 * Xlint cleanup (#2445): class is {@code final} (this-escape), non-serializable session
 * collaborators are {@code transient}, and error-message HTML resolution is behavior-preserving.
 */
public class PSContentExplorerStatusDialogTest {

  /** Fields that hold non-serializable session collaborators (must be transient). */
  private static final Set<String> TRANSIENT_COLLABORATOR_FIELDS = Set.of("m_monitor", "m_applet");

  @Test
  public void dialogClassIsFinal() {
    assertTrue(
        Modifier.isFinal(PSContentExplorerStatusDialog.class.getModifiers()),
        "PSContentExplorerStatusDialog must be final to avoid this-escape in the ctor");
  }

  @Test
  public void dialogDeclaresSerialVersionUid() throws Exception {
    Field uid = PSContentExplorerStatusDialog.class.getDeclaredField("serialVersionUID");
    assertTrue(Modifier.isStatic(uid.getModifiers()));
    assertTrue(Modifier.isFinal(uid.getModifiers()));
    assertNotNull(uid);
  }

  @Test
  public void nonSerializableCollaboratorsAreTransient() throws Exception {
    for (String name : TRANSIENT_COLLABORATOR_FIELDS) {
      Field f = PSContentExplorerStatusDialog.class.getDeclaredField(name);
      assertTrue(
          Modifier.isTransient(f.getModifiers()),
          () -> name + " must be transient (non-serializable collaborator)");
    }
  }

  @Test
  public void resolveErrorMessageView_nullIsPlainEmpty() {
    PSContentExplorerStatusDialog.ErrorMessageView view =
        PSContentExplorerStatusDialog.resolveErrorMessageView(null);
    assertEquals(PSContentExplorerStatusDialog.TEXT_BY_TEXT, view.contentType());
    assertEquals("", view.displayText());
  }

  @Test
  public void resolveErrorMessageView_plainTextUnchanged() {
    String plain = "Something went wrong";
    PSContentExplorerStatusDialog.ErrorMessageView view =
        PSContentExplorerStatusDialog.resolveErrorMessageView(plain);
    assertEquals(PSContentExplorerStatusDialog.TEXT_BY_TEXT, view.contentType());
    assertEquals(plain, view.displayText());
  }

  @Test
  public void resolveErrorMessageView_htmlFragmentExtracted() {
    String raw = "prefix<html><b>err</b></html>suffix";
    PSContentExplorerStatusDialog.ErrorMessageView view =
        PSContentExplorerStatusDialog.resolveErrorMessageView(raw);
    assertEquals(PSContentExplorerStatusDialog.TEXT_BY_HTML, view.contentType());
    // Historical end offset uses HTML_OPEN_TAG.length() (6), not HTML_CLOSE_TAG (7), so the
    // trailing '>' of </html> is intentionally omitted — behavior-preserving for #2445.
    assertEquals("<html><b>err</b></html", view.displayText());
  }

  @Test
  public void resolveErrorMessageView_unclosedHtmlIsPlain() {
    String raw = "<html>not closed";
    PSContentExplorerStatusDialog.ErrorMessageView view =
        PSContentExplorerStatusDialog.resolveErrorMessageView(raw);
    assertEquals(PSContentExplorerStatusDialog.TEXT_BY_TEXT, view.contentType());
    assertEquals(raw, view.displayText());
  }
}
