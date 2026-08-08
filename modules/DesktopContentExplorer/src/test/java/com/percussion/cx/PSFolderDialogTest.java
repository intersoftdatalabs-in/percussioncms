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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Structural coverage for {@link PSFolderDialog} after residual Xlint cleanup (#2441): class is
 * {@code final} (this-escape) and non-serializable session collaborators are {@code transient}.
 */
public class PSFolderDialogTest {

  /** Fields that hold non-serializable session collaborators (must be transient). */
  private static final Set<String> TRANSIENT_COLLABORATOR_FIELDS =
      Set.of("m_parentFolderNode", "m_folderNode", "m_folderMgr", "m_userInfo", "m_applet");

  @Test
  public void dialogClassIsFinal() {
    assertTrue(
        Modifier.isFinal(PSFolderDialog.class.getModifiers()),
        "PSFolderDialog must be final to avoid this-escape in the ctor");
  }

  @Test
  public void dialogDeclaresSerialVersionUid() throws Exception {
    Field uid = PSFolderDialog.class.getDeclaredField("serialVersionUID");
    assertTrue(Modifier.isStatic(uid.getModifiers()));
    assertTrue(Modifier.isFinal(uid.getModifiers()));
    assertNotNull(uid);
  }

  @Test
  public void nonSerializableCollaboratorsAreTransient() throws Exception {
    for (String name : TRANSIENT_COLLABORATOR_FIELDS) {
      Field f = PSFolderDialog.class.getDeclaredField(name);
      assertTrue(
          Modifier.isTransient(f.getModifiers()),
          () -> name + " must be transient (non-serializable collaborator)");
    }
  }
}
