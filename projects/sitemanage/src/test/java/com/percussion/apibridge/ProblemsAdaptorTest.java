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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.problems.DesignProblem;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Session Problems catalog: invalid-session fixture + Admin gate. */
@Tag("UnitTest")
class ProblemsAdaptorTest {

  private ProblemsAdaptor adaptor;

  @BeforeEach
  void setUp() {
    adaptor = new ProblemsAdaptor(() -> true);
  }

  @Test
  void listProblems_includesInvalidSessionFixture() {
    List<DesignProblem> list = adaptor.listProblems(null);
    assertEquals(1, list.size());
    DesignProblem row = list.get(0);
    assertEquals(ProblemsAdaptor.FIXTURE_PROBLEM_ID, row.getId());
    assertEquals(ProblemsAdaptor.FIXTURE_SEVERITY, row.getSeverity());
    assertEquals(ProblemsAdaptor.FIXTURE_CODE, row.getCode());
    assertEquals(ProblemsAdaptor.FIXTURE_MESSAGE, row.getMessage());
    assertEquals(ProblemsAdaptor.FIXTURE_OBJECT_TYPE, row.getObjectType());
    assertEquals(ProblemsAdaptor.FIXTURE_OBJECT_ID, row.getObjectId());
    assertEquals(ProblemsAdaptor.FIXTURE_NAVIGATE, row.getNavigateSection());
    assertTrue(ProblemsAdaptor.isNavigateSection(row.getNavigateSection()));
  }

  @Test
  void listProblems_invalidSessionQueryMatchesDefaultSession() {
    List<DesignProblem> withQuery = adaptor.listProblems("invalid-session");
    List<DesignProblem> session = adaptor.listProblems("  ");
    assertEquals(1, withQuery.size());
    assertEquals(session.get(0).getId(), withQuery.get(0).getId());
  }

  @Test
  void listProblems_nonAdminIs403() {
    adaptor = new ProblemsAdaptor(() -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.listProblems(null));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(ProblemsAdaptor.ADMIN_REQUIRED, ex.getMessage());
  }

  @Test
  void listProblems_unsafeFixtureIs400WithoutEcho() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.listProblems("../etc"));
    assertEquals(ProblemsAdaptor.INVALID_FIXTURE, ex.getMessage());
    assertFalse(ex.getMessage().contains(".."));
    assertFalse(ex.getMessage().contains("etc"));
  }

  @Test
  void listProblems_unknownSafeFixtureIs400() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.listProblems("other-fixture"));
    assertEquals(ProblemsAdaptor.UNKNOWN_FIXTURE, ex.getMessage());
  }

  @Test
  void isSafeFixture_rejectsTraversalAndJdbc() {
    assertTrue(ProblemsAdaptor.isSafeFixture("invalid-session"));
    assertFalse(ProblemsAdaptor.isSafeFixture(null));
    assertFalse(ProblemsAdaptor.isSafeFixture("../etc"));
    assertFalse(ProblemsAdaptor.isSafeFixture("jdbc:h2"));
    assertFalse(ProblemsAdaptor.isSafeFixture("1bad"));
  }
}
