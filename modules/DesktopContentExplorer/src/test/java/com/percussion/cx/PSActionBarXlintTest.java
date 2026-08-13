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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cx.wizards.PSCopySiteNamePage;
import com.percussion.wizard.PSWizardPanel;
import com.percussion.wizard.PSWizardStartFinishPanel;
import com.percussion.wizard.PSWizardValidationError;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Structural coverage for the DCE wizard + ActionBar this-escape/serial cluster (#3298): leaf
 * Swing types are {@code final}, session collaborators are {@code transient}, and the abstract
 * wizard page base stays open for copy-site subclasses.
 */
public class PSActionBarXlintTest {

  @Test
  public void actionBarClassIsFinal() {
    assertTrue(
        Modifier.isFinal(PSActionBar.class.getModifiers()),
        "PSActionBar must be final to avoid this-escape in the ctor");
  }

  @Test
  public void actionBarDeclaresSerialVersionUid() throws Exception {
    assertSerialVersionUid(PSActionBar.class);
  }

  @Test
  public void actionBarCollaboratorsAreTransient() throws Exception {
    assertTransientFields(
        PSActionBar.class, Set.of("m_applet", "m_navSelection", "m_viewChangeListeners", "m_contentItem"));
  }

  @Test
  public void cxWizardDialogClassIsFinal() {
    assertTrue(
        Modifier.isFinal(PSWizardDialog.class.getModifiers()),
        "cx PSWizardDialog must be final to avoid this-escape in the ctor");
  }

  @Test
  public void cxWizardDialogCollaboratorsAreTransient() throws Exception {
    assertSerialVersionUid(PSWizardDialog.class);
    assertTransientFields(
        PSWizardDialog.class, Set.of("m_applet", "m_wizardCommands", "m_pages", "m_skippedPages"));
  }

  @Test
  public void startFinishPanelIsFinalWithSerialUid() throws Exception {
    assertTrue(Modifier.isFinal(PSWizardStartFinishPanel.class.getModifiers()));
    assertSerialVersionUid(PSWizardStartFinishPanel.class);
  }

  @Test
  public void copySiteNamePageIsFinalWithSerialUid() throws Exception {
    assertTrue(Modifier.isFinal(PSCopySiteNamePage.class.getModifiers()));
    assertSerialVersionUid(PSCopySiteNamePage.class);
    Field input = PSCopySiteNamePage.class.getDeclaredField("m_input");
    assertTrue(Modifier.isTransient(input.getModifiers()));
  }

  @Test
  public void validationErrorIsFinalWithSerialUid() throws Exception {
    assertTrue(Modifier.isFinal(PSWizardValidationError.class.getModifiers()));
    assertSerialVersionUid(PSWizardValidationError.class);
  }

  @Test
  public void wizardPanelStaysOpenForPageSubclasses() throws Exception {
    assertFalse(
        Modifier.isFinal(PSWizardPanel.class.getModifiers()),
        "PSWizardPanel must remain non-final for copy-site / start-finish pages");
    assertTrue(Modifier.isAbstract(PSWizardPanel.class.getModifiers()));
    assertSerialVersionUid(PSWizardPanel.class);
    Field applet = PSWizardPanel.class.getDeclaredField("m_applet");
    assertTrue(Modifier.isTransient(applet.getModifiers()));
    Field data = PSWizardPanel.class.getDeclaredField("m_data");
    assertTrue(Modifier.isTransient(data.getModifiers()));
    Method init = PSWizardPanel.class.getDeclaredMethod("initPanel", javax.swing.JPanel.class);
    assertTrue(Modifier.isFinal(init.getModifiers()), "initPanel must be final (this-escape)");
  }

  private static void assertSerialVersionUid(Class<?> type) throws Exception {
    Field uid = type.getDeclaredField("serialVersionUID");
    assertTrue(Modifier.isStatic(uid.getModifiers()));
    assertTrue(Modifier.isFinal(uid.getModifiers()));
    assertNotNull(uid);
  }

  private static void assertTransientFields(Class<?> type, Set<String> names) throws Exception {
    for (String name : names) {
      Field f = type.getDeclaredField(name);
      assertTrue(
          Modifier.isTransient(f.getModifiers()),
          () -> name + " must be transient (non-serializable collaborator)");
    }
  }
}
