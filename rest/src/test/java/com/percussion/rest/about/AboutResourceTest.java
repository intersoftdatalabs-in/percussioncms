/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.rest.about;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class AboutResourceTest {

  private IAboutAdaptor adaptor;
  private AboutResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IAboutAdaptor.class);
    resource = new AboutResource(adaptor);
  }

  @Test
  public void getAboutDelegatesToAdaptor() {
    AboutDetail detail = new AboutDetail();
    detail.setVersionString("Version 8.2.0 Build 20260731 (1)");
    detail.setCopyright("Percussion CMS Copyright (C) Percussion Software, Inc.  1999-2026");
    detail.setThirdPartyCopyright("This product includes software developed by...");
    when(adaptor.getAbout()).thenReturn(detail);

    AboutDetail result = resource.getAbout();

    assertEquals("Version 8.2.0 Build 20260731 (1)", result.getVersionString());
    verify(adaptor).getAbout();
  }

  @Test
  public void getAboutWrapsFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("server not initialized");
    when(adaptor.getAbout()).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getAbout());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void getAboutWithoutInjectionFailsWithDiagnostic() {
    AboutResource bare = new AboutResource();
    WebApplicationException ex = assertThrows(WebApplicationException.class, bare::getAbout);
    assertEquals(500, ex.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, ex.getCause());
  }
}
