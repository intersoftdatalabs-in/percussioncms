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
package com.percussion.services.security.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.PSAuthenticateUserUtils;
import com.percussion.design.objectstore.PSAttribute;
import com.percussion.design.objectstore.PSRelativeSubject;
import com.percussion.design.objectstore.PSRoleConfiguration;
import com.percussion.design.objectstore.PSSubject;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Persist helper for user-subject attributes (sys_defaultCommunity, issue #3508).
 *
 * <p>Uses production {@link PSRoleConfiguration} / {@link PSSubject} types.
 */
@Tag("UnitTest")
class PSBackEndRoleMgrSubjectAttributeTest {

  @Test
  void applySubjectAttributeWritesSysDefaultCommunity() {
    PSRoleConfiguration cfg = new PSRoleConfiguration();
    PSBackEndRoleMgr.applySubjectAttribute(
        cfg, "Admin", PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY, "Corporate");

    PSSubject subject =
        cfg.getGlobalSubject(
            new PSRelativeSubject("Admin", PSSubject.SUBJECT_TYPE_USER, null), false);
    assertNotNull(subject);
    PSAttribute attr =
        subject.getAttributes().getAttribute(PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY);
    assertNotNull(attr);
    assertEquals("Corporate", attr.getValues().get(0));
  }

  @Test
  void applySubjectAttributeBlankClearsStoredValue() {
    PSRoleConfiguration cfg = new PSRoleConfiguration();
    PSBackEndRoleMgr.applySubjectAttribute(
        cfg, "Editor1", PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY, "Default");
    PSBackEndRoleMgr.applySubjectAttribute(
        cfg, "Editor1", PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY, "  ");

    PSSubject subject =
        cfg.getGlobalSubject(
            new PSRelativeSubject("Editor1", PSSubject.SUBJECT_TYPE_USER, null), false);
    assertNotNull(subject);
    PSAttribute attr =
        subject.getAttributes().getAttribute(PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY);
    assertNotNull(attr);
    List<?> values = attr.getValues();
    assertTrue(values == null || values.isEmpty());
  }

  @Test
  void applySubjectAttributeRejectsBlankNameOrAttribute() {
    PSRoleConfiguration cfg = new PSRoleConfiguration();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSBackEndRoleMgr.applySubjectAttribute(
                cfg, "  ", PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY, "Default"));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSBackEndRoleMgr.applySubjectAttribute(cfg, "Admin", "  ", "Default"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSBackEndRoleMgr.applySubjectAttribute(
                null, "Admin", PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY, "Default"));
  }
}
