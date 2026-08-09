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
package com.percussion.cx.javafx;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cx.PSJavaBridge;
import com.percussion.cx.PSSelection;
import com.percussion.cx.objectstore.PSMenuAction;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.swing.JFrame;
import org.junit.jupiter.api.Test;

/**
 * Coverage for {@link PSDesktopExplorerWindow} this-escape remediation (#2444): static nested state
 * provider and lazy {@link PSJavaBridge} so constructors do not publish {@code this}.
 */
public class PSDesktopExplorerWindowTest {

  /** Minimal concrete window for construction and bridge lazy-init tests. */
  private static final class TestExplorerWindow extends PSDesktopExplorerWindow {
    @Override
    public boolean validateOpen(
        String mi_actionurl,
        String mi_target,
        String mi_style,
        PSSelection selection,
        PSMenuAction action) {
      return true;
    }

    @Override
    public JFrame instanceOpen() {
      return this;
    }
  }

  @Test
  public void stateProviderIsStaticNestedClass() {
    assertTrue(
        Modifier.isStatic(
            PSDesktopExplorerWindow.PSDesktopExplorerStateProvider.class.getModifiers()),
        "PSDesktopExplorerStateProvider must be static to avoid this-escape on outer construction");
  }

  @Test
  public void stateProviderCanBeConstructedWithoutOuterWindow() {
    PSDesktopExplorerWindow.PSDesktopExplorerStateProvider provider =
        new PSDesktopExplorerWindow.PSDesktopExplorerStateProvider();
    assertNotNull(provider.getState());
  }

  @Test
  public void windowConstructionDoesNotCreateBridge() throws Exception {
    TestExplorerWindow window = new TestExplorerWindow();
    Field bridgeField = PSDesktopExplorerWindow.class.getDeclaredField("bridge");
    bridgeField.setAccessible(true);
    assertNull(
        bridgeField.get(window),
        "bridge must remain null after construction (lazy-init, no this-escape)");
  }

  @Test
  public void getBridgeLazilyCreatesStableInstance() {
    TestExplorerWindow window = new TestExplorerWindow();
    PSJavaBridge first = window.getBridge();
    assertNotNull(first);
    assertSame(first, window.getBridge(), "getBridge must return the same instance");
  }

  @Test
  public void myStateProviderIsInitializedOnConstruction() {
    TestExplorerWindow window = new TestExplorerWindow();
    assertNotNull(window.myStateProvider);
    assertNotNull(window.myStateProvider.getState());
  }

  @Test
  public void bridgeAndStateProviderFieldsAreTransient() throws Exception {
    Field bridgeField = PSDesktopExplorerWindow.class.getDeclaredField("bridge");
    Field stateField = PSDesktopExplorerWindow.class.getDeclaredField("myStateProvider");
    assertTrue(
        Modifier.isTransient(bridgeField.getModifiers()),
        "bridge must be transient (non-serializable collaborator)");
    assertTrue(
        Modifier.isTransient(stateField.getModifiers()),
        "myStateProvider must be transient (non-serializable collaborator)");
  }
}
