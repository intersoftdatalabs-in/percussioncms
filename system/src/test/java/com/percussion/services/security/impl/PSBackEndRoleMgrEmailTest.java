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
package com.percussion.services.security.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.percussion.conn.PSServerException;
import com.percussion.design.objectstore.PSAttribute;
import com.percussion.design.objectstore.PSRelativeSubject;
import com.percussion.design.objectstore.PSRoleConfiguration;
import com.percussion.design.objectstore.PSSubject;
import com.percussion.design.objectstore.server.PSServerXmlObjectStore;
import com.percussion.design.objectstore.server.PSXmlObjectStoreLockerId;
import com.percussion.security.PSSecurityToken;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests proving {@link PSBackEndRoleMgr#setSubjectEmail} persists emails and fails loud. */
@Tag("UnitTest")
class PSBackEndRoleMgrEmailTest {

  @Test
  void setSubjectEmailUpdatesAttributeSavesRoleConfigAndReleasesLock() throws Exception {
    PSRoleConfiguration roleCfg = new PSRoleConfiguration();
    PSServerXmlObjectStore objectStore = mock(PSServerXmlObjectStore.class);
    PSXmlObjectStoreLockerId lockId = mock(PSXmlObjectStoreLockerId.class);
    PSSecurityToken token = mock(PSSecurityToken.class);

    PSBackEndRoleMgr.RoleConfig cfg = new PSBackEndRoleMgr.RoleConfig();
    cfg.roleCfg = roleCfg;
    cfg.objectStore = objectStore;
    cfg.lockId = lockId;
    cfg.securityToken = token;

    PSBackEndRoleMgr mgr =
        new PSBackEndRoleMgr() {
          @Override
          RoleConfig acquireRoleConfig() {
            return cfg;
          }
        };

    mgr.setSubjectEmail("Editor1", "  editor1@example.com  ");

    PSSubject subject =
        roleCfg.getGlobalSubject(
            new PSRelativeSubject("Editor1", PSSubject.SUBJECT_TYPE_USER, null), false);
    assertNotNull(subject);
    PSAttribute emailAttr = subject.getAttributes().getAttribute("sys_email");
    assertNotNull(emailAttr);
    assertEquals("editor1@example.com", emailAttr.getValues().get(0));

    verify(objectStore).saveRoleConfiguration(roleCfg, lockId, token);
    verify(objectStore).releaseServerConfigLock(lockId);
  }

  @Test
  void setSubjectEmailThrowsRuntimeExceptionOnPersistenceFailureAndStillReleasesLock()
      throws Exception {
    PSRoleConfiguration roleCfg = new PSRoleConfiguration();
    PSServerXmlObjectStore objectStore = mock(PSServerXmlObjectStore.class);
    PSXmlObjectStoreLockerId lockId = mock(PSXmlObjectStoreLockerId.class);
    PSSecurityToken token = mock(PSSecurityToken.class);

    PSServerException cause = new PSServerException(0, "role store unavailable");
    doThrow(cause).when(objectStore).saveRoleConfiguration(roleCfg, lockId, token);

    PSBackEndRoleMgr.RoleConfig cfg = new PSBackEndRoleMgr.RoleConfig();
    cfg.roleCfg = roleCfg;
    cfg.objectStore = objectStore;
    cfg.lockId = lockId;
    cfg.securityToken = token;

    PSBackEndRoleMgr mgr =
        new PSBackEndRoleMgr() {
          @Override
          RoleConfig acquireRoleConfig() {
            return cfg;
          }
        };

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> mgr.setSubjectEmail("Editor1", "e1@example.com"));
    assertSame(cause, thrown.getCause());
    verify(objectStore).releaseServerConfigLock(lockId);
  }

  @Test
  void setSubjectEmailRejectsBlankSubjectName() {
    PSBackEndRoleMgr mgr = new PSBackEndRoleMgr();
    assertThrows(
        IllegalArgumentException.class, () -> mgr.setSubjectEmail("  ", "user@example.com"));
  }
}
