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
package com.percussion.wizard;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/** Construction / type validation coverage for {@link PSWizardCommandPanel}. */
public class PSWizardCommandPanelTest {

  @Test
  public void constructorRejectsNullDialog() {
    assertThrows(IllegalArgumentException.class, () -> new PSWizardCommandPanel(null));
  }

  @Test
  public void commandPanelClassIsFinal() {
    assertTrue(
        Modifier.isFinal(PSWizardCommandPanel.class.getModifiers()),
        "PSWizardCommandPanel must be final to avoid this-escape in the ctor");
  }

  @Test
  public void commandPanelDeclaresSerialVersionUid() throws Exception {
    Field uid = PSWizardCommandPanel.class.getDeclaredField("serialVersionUID");
    assertTrue(Modifier.isStatic(uid.getModifiers()));
    assertTrue(Modifier.isFinal(uid.getModifiers()));
    assertNotNull(uid);
  }

  @Test
  public void dialogCollaboratorIsTransient() throws Exception {
    Field f = PSWizardCommandPanel.class.getDeclaredField("m_dialog");
    assertTrue(Modifier.isTransient(f.getModifiers()), "m_dialog must be transient");
  }
}
