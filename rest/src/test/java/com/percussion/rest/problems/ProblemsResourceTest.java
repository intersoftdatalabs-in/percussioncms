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

package com.percussion.rest.problems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class ProblemsResourceTest {

  private IProblemsAdaptor adaptor;
  private ProblemsResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IProblemsAdaptor.class);
    resource = new ProblemsResource(adaptor);
  }

  @Test
  public void listProblemsDelegates() {
    DesignProblem row = new DesignProblem();
    row.setId("invalid-session");
    when(adaptor.listProblems(isNull())).thenReturn(List.of(row));
    List<DesignProblem> out = resource.listProblems(null);
    assertEquals(1, out.size());
    assertEquals("invalid-session", out.get(0).getId());
    verify(adaptor).listProblems(null);
  }

  @Test
  public void listProblemsPassesFixture() {
    when(adaptor.listProblems(eq("invalid-session"))).thenReturn(List.of());
    resource.listProblems("invalid-session");
    verify(adaptor).listProblems("invalid-session");
  }

  @Test
  public void listProblemsNullSafe() {
    when(adaptor.listProblems(any())).thenReturn(null);
    assertTrue(resource.listProblems(null).isEmpty());
  }

  @Test
  public void listProblemsWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listProblems(any())).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listProblems(null));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void listProblemsNonAdminIs403() {
    when(adaptor.listProblems(any()))
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listProblems(null));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailable() {
    ProblemsResource bare = new ProblemsResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.listProblems(null));
    assertEquals(503, ex.getResponse().getStatus());
    verify(adaptor, never()).listProblems(any());
  }

  @Test
  public void unsafeFixtureIs400WithoutEcho() {
    when(adaptor.listProblems(eq("../etc")))
        .thenThrow(new IllegalArgumentException("jdbc:h2:mem:secret;PASSWORD=x"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listProblems("../etc"));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(ProblemsResource.INVALID_FIXTURE, ex.getMessage());
    assertFalse(ex.getMessage().contains("jdbc"));
    assertFalse(ex.getMessage().contains("PASSWORD"));
  }

  @Test
  public void unknownFixturePassesSafeMessage() {
    when(adaptor.listProblems(eq("nope")))
        .thenThrow(new IllegalArgumentException("Unknown fixture"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listProblems("nope"));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("Unknown fixture", ex.getMessage());
  }

  @Test
  public void looksLikeRawPath() {
    assertTrue(ProblemsResource.looksLikeRawPath("jdbc:h2:mem:x"));
    assertTrue(ProblemsResource.looksLikeRawPath("password=secret"));
    assertTrue(ProblemsResource.looksLikeRawPath("C:\\Windows\\secret"));
    assertTrue(ProblemsResource.looksLikeRawPath("/etc/passwd"));
    assertFalse(ProblemsResource.looksLikeRawPath("Invalid fixture"));
    assertFalse(ProblemsResource.looksLikeRawPath("Unknown fixture"));
  }
}
