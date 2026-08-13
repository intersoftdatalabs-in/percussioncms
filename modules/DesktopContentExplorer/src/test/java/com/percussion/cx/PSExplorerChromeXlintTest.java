/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Structural coverage for DCE chrome this-escape/serial cleanup (#3288): types are {@code final}
 * and non-serializable session collaborators are {@code transient}.
 */
public class PSExplorerChromeXlintTest {

  @Test
  public void chromeTypesAreFinal() {
    assertFinal(PSContentExplorerLoginPanel.class);
    assertFinal(PSContentExplorerMenuBar.class);
    assertFinal(PSContentExplorerHeader.class);
    assertFinal(PSContentExplorerFrame.class);
    assertFinal(PSMenuSource.class);
  }

  @Test
  public void loginPanelDeclaresSerialVersionUid() throws Exception {
    Field uid = PSContentExplorerLoginPanel.class.getDeclaredField("serialVersionUID");
    assertTrue(Modifier.isStatic(uid.getModifiers()));
    assertTrue(Modifier.isFinal(uid.getModifiers()));
    assertNotNull(uid);
  }

  @Test
  public void menuBarDeclaresSerialVersionUid() throws Exception {
    Field uid = PSContentExplorerMenuBar.class.getDeclaredField("serialVersionUID");
    assertTrue(Modifier.isStatic(uid.getModifiers()));
    assertTrue(Modifier.isFinal(uid.getModifiers()));
    assertNotNull(uid);
  }

  @Test
  public void loginPanelCollaboratorsAreTransient() throws Exception {
    assertTransient(
        PSContentExplorerLoginPanel.class,
        Set.of("applet", "m_parent", "m_res", "m_adminProps", "selectedLocale"));
  }

  @Test
  public void menuBarCollaboratorsAreTransient() throws Exception {
    assertTransient(
        PSContentExplorerMenuBar.class,
        Set.of("m_menuBar", "m_menuSource", "m_actManager", "m_menus", "m_menuActions"));
  }

  @Test
  public void frameCollaboratorsAreTransient() throws Exception {
    assertTransient(
        PSContentExplorerFrame.class, Set.of("helper", "parameters", "loginPanel"));
  }

  private static void assertFinal(Class<?> type) {
    assertTrue(
        Modifier.isFinal(type.getModifiers()),
        () -> type.getSimpleName() + " must be final to avoid this-escape in constructors");
  }

  private static void assertTransient(Class<?> type, Set<String> names) throws Exception {
    for (String name : names) {
      Field f = type.getDeclaredField(name);
      assertTrue(
          Modifier.isTransient(f.getModifiers()),
          () -> type.getSimpleName() + "." + name + " must be transient");
    }
  }
}
