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
package com.percussion.guitools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * Structural coverage that {@link PSAboutDialog} no longer references the JDK applet API removed in
 * JDK 24+.
 */
public class PSAboutDialogTest {

  @Test
  public void setAppletContextAcceptsObject() throws Exception {
    Method method = PSAboutDialog.class.getMethod("setAppletContext", Object.class);
    assertEquals(Object.class, method.getParameterTypes()[0]);
  }

  @Test
  public void declaredMethodsDoNotUseJavaAppletTypes() {
    for (Method method : PSAboutDialog.class.getDeclaredMethods()) {
      assertFalse(
          method.getReturnType().getName().startsWith("java.applet."),
          () -> "return type of " + method.getName());
      for (Class<?> parameter : method.getParameterTypes()) {
        assertFalse(
            parameter.getName().startsWith("java.applet."),
            () -> "parameter of " + method.getName() + ": " + parameter.getName());
      }
    }
  }
}
