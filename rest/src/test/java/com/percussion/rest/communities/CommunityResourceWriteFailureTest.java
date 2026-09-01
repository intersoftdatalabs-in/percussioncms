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
package com.percussion.rest.communities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class CommunityResourceWriteFailureTest {

  @Test
  public void mapWriteFailurePreservesAdaptor409() {
    WebApplicationException conflict =
        new WebApplicationException("Community already exists: Default", 409);
    assertSame(conflict, CommunityResource.mapWriteFailure("createCommunities", conflict));
  }

  @Test
  public void mapWriteFailureIllegalArgumentIs400() {
    WebApplicationException mapped =
        CommunityResource.mapWriteFailure(
            "createCommunities", new IllegalArgumentException("name cannot be null or empty"));
    assertEquals(400, mapped.getResponse().getStatus());
  }

  @Test
  public void mapWriteFailureUnexpectedIs500() {
    WebApplicationException mapped =
        CommunityResource.mapWriteFailure(
            "deleteCommunities", new IllegalStateException("Failed to persist communities"));
    assertEquals(500, mapped.getResponse().getStatus());
  }
}
