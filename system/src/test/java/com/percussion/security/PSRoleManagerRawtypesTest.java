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

package com.percussion.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSAttribute;
import com.percussion.design.objectstore.PSAttributeList;
import com.percussion.design.objectstore.PSGlobalSubject;
import com.percussion.design.objectstore.PSSubject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for {@link PSRoleManager} residual rawtypes cleanup (issue #2459, residual of
 * #2386 / parent epic #2022).
 *
 * <p>Covers pure logic that does not require a live security cataloger stack: invalid-argument
 * short-circuits on {@link PSRoleManager#isMemberOfRole(String, String)} and role/global subject
 * attribute merge via {@link PSRoleManager#mergeRoleAndGlobalSubjects(List, List)}.
 */
@Tag("UnitTest")
public class PSRoleManagerRawtypesTest {

  @Test
  void isMemberOfRoleReturnsFalseForNullOrEmptyArgs() {
    PSRoleManager mgr = PSRoleManager.getInstance();

    assertFalse(mgr.isMemberOfRole(null, "Admin"));
    assertFalse(mgr.isMemberOfRole("", "Admin"));
    assertFalse(mgr.isMemberOfRole("   ", "Admin"));
    assertFalse(mgr.isMemberOfRole("admin", null));
    assertFalse(mgr.isMemberOfRole("admin", ""));
    assertFalse(mgr.isMemberOfRole("admin", "   "));
    assertFalse(mgr.isMemberOfRole(null, null));
  }

  @Test
  void mergeRoleAndGlobalSubjectsPrefersRoleAttributes() {
    PSAttributeList roleAttrs = new PSAttributeList();
    roleAttrs.setAttribute("email", List.of("role@example.com"));
    roleAttrs.setAttribute("dept", List.of("Engineering"));

    PSAttributeList globalAttrs = new PSAttributeList();
    globalAttrs.setAttribute("email", List.of("global@example.com"));
    globalAttrs.setAttribute("location", List.of("Boston"));

    PSSubject roleSubject =
        new PSGlobalSubject("alice", PSSubject.SUBJECT_TYPE_USER, roleAttrs);
    PSSubject globalSubject =
        new PSGlobalSubject("alice", PSSubject.SUBJECT_TYPE_USER, globalAttrs);

    List<PSSubject> merged =
        PSRoleManager.mergeRoleAndGlobalSubjects(
            List.of(roleSubject), List.of(globalSubject));

    assertEquals(1, merged.size());
    PSSubject result = merged.get(0);
    assertEquals("alice", result.getName());

    PSAttribute email = result.getAttributes().getAttribute("email");
    assertEquals(List.of("role@example.com"), email.getValues());

    PSAttribute dept = result.getAttributes().getAttribute("dept");
    assertEquals(List.of("Engineering"), dept.getValues());

    PSAttribute location = result.getAttributes().getAttribute("location");
    assertEquals(List.of("Boston"), location.getValues());
  }

  @Test
  void mergeRoleAndGlobalSubjectsAppendsGlobalOnlySubjects() {
    PSAttributeList roleAttrs = new PSAttributeList();
    roleAttrs.setAttribute("roleOnly", List.of("r"));

    PSAttributeList globalAttrs = new PSAttributeList();
    globalAttrs.setAttribute("globalOnly", List.of("g"));

    PSSubject roleSubject =
        new PSGlobalSubject("bob", PSSubject.SUBJECT_TYPE_USER, roleAttrs);
    PSSubject globalOnly =
        new PSGlobalSubject("carol", PSSubject.SUBJECT_TYPE_USER, globalAttrs);

    List<PSSubject> merged =
        PSRoleManager.mergeRoleAndGlobalSubjects(
            List.of(roleSubject), List.of(globalOnly));

    assertEquals(2, merged.size());
    // sorted case-insensitive by name: bob, carol
    assertEquals("bob", merged.get(0).getName());
    assertEquals("carol", merged.get(1).getName());
    assertTrue(merged.get(0).getAttributes().getAttribute("roleOnly") != null);
    assertTrue(merged.get(1).getAttributes().getAttribute("globalOnly") != null);
  }

  @Test
  void mergeRoleAndGlobalSubjectsHandlesEmptyInputs() {
    assertEquals(
        Collections.emptyList(),
        PSRoleManager.mergeRoleAndGlobalSubjects(new ArrayList<>(), new ArrayList<>()));

    PSSubject onlyGlobal =
        new PSGlobalSubject(
            "dave", PSSubject.SUBJECT_TYPE_USER, new PSAttributeList());
    List<PSSubject> globalsOnly =
        PSRoleManager.mergeRoleAndGlobalSubjects(
            new ArrayList<>(), List.of(onlyGlobal));
    assertEquals(1, globalsOnly.size());
    assertEquals("dave", globalsOnly.get(0).getName());
  }

  @Test
  void mergeRoleAndGlobalSubjectsDistinguishesSubjectTypes() {
    PSAttributeList userAttrs = new PSAttributeList();
    userAttrs.setAttribute("kind", List.of("user"));
    PSAttributeList groupAttrs = new PSAttributeList();
    groupAttrs.setAttribute("kind", List.of("group"));

    PSSubject user =
        new PSGlobalSubject("team", PSSubject.SUBJECT_TYPE_USER, userAttrs);
    PSSubject group =
        new PSGlobalSubject("team", PSSubject.SUBJECT_TYPE_GROUP, groupAttrs);

    List<PSSubject> merged =
        PSRoleManager.mergeRoleAndGlobalSubjects(List.of(user), List.of(group));

    assertEquals(2, merged.size());
  }
}
