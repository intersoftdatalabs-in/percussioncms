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
package com.percussion.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSAttributeList;
import com.percussion.design.objectstore.PSGlobalSubject;
import com.percussion.design.objectstore.PSSubject;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Default-community resolution helpers used at login and profile persist (issue #3508).
 */
@Tag("UnitTest")
class PSAuthenticateUserUtilsDefaultCommunityTest {

  @Test
  void subjectValueWinsOverRoleValue() {
    assertEquals(
        "Corporate",
        PSAuthenticateUserUtils.resolveDefaultCommunityName("Corporate", "Default"));
    assertEquals(
        "Default", PSAuthenticateUserUtils.resolveDefaultCommunityName("  ", "Default"));
    assertEquals(
        "Default", PSAuthenticateUserUtils.resolveDefaultCommunityName(null, " Default "));
    assertNull(PSAuthenticateUserUtils.resolveDefaultCommunityName("  ", "  "));
    assertNull(PSAuthenticateUserUtils.resolveDefaultCommunityName(null, null));
  }

  @Test
  void firstSubjectAttributeValueReadsSysDefaultCommunity() {
    PSAttributeList attrs = new PSAttributeList();
    attrs.setAttribute(
        PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY, Collections.singletonList("Engineering"));
    PSSubject subject =
        new PSGlobalSubject("Admin", PSSubject.SUBJECT_TYPE_USER, attrs);

    assertEquals(
        "Engineering",
        PSAuthenticateUserUtils.firstSubjectAttributeValue(
            List.of(subject), PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY));
    assertNull(
        PSAuthenticateUserUtils.firstSubjectAttributeValue(
            List.of(subject), "sys_email"));
    assertNull(PSAuthenticateUserUtils.firstSubjectAttributeValue(null, "sys_defaultCommunity"));
    assertNull(
        PSAuthenticateUserUtils.firstSubjectAttributeValue(List.of(), "sys_defaultCommunity"));
  }

  @Test
  void firstSubjectAttributeValueSkipsEmptyShells() {
    PSAttributeList empty = new PSAttributeList();
    empty.setAttribute(PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY, List.of());
    PSSubject blank =
        new PSGlobalSubject("Admin", PSSubject.SUBJECT_TYPE_USER, empty);

    assertNull(
        PSAuthenticateUserUtils.firstSubjectAttributeValue(
            List.of(blank), PSAuthenticateUserUtils.SYS_DEFAULTCOMMUNITY));
  }

  @Test
  void isCommunityAllowedIsCaseInsensitive() {
    List<String> allowed = List.of("Default", "Enterprise Investments");
    assertTrue(PSAuthenticateUserUtils.isCommunityAllowed("default", allowed));
    assertTrue(PSAuthenticateUserUtils.isCommunityAllowed("Enterprise Investments", allowed));
    assertFalse(PSAuthenticateUserUtils.isCommunityAllowed("Unknown", allowed));
    assertFalse(PSAuthenticateUserUtils.isCommunityAllowed("Default", List.of()));
    assertFalse(PSAuthenticateUserUtils.isCommunityAllowed("  ", allowed));
    assertFalse(PSAuthenticateUserUtils.isCommunityAllowed("Default", null));
  }
}
