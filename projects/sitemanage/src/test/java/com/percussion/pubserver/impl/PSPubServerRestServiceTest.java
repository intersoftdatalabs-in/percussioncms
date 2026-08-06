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
package com.percussion.pubserver.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.regions.Region;

/**
 * Unit tests for the v2 migration of {@link PSPubServerRestService#getAvailableRegions()}. Verifies
 * the REST endpoint returns region IDs from v2's {@link RegionMetadata#regions()} (default
 * partition) instead of v1's {@code Regions.values()}.
 */
@ExtendWith(MockitoExtension.class)
public class PSPubServerRestServiceTest {

  private PSPubServerRestService service;
  private MockedStatic<Region> regionStatic;

  @BeforeEach
  public void setUp() {
    service = new PSPubServerRestService(null);
  }

  @AfterEach
  public void tearDown() {
    if (regionStatic != null) regionStatic.close();
  }

  @Test
  public void getAvailableRegions_returnsJsonArrayOfRegionIds()
      throws tools.jackson.core.JacksonException {
    List<Region> mockRegions = List.of(Region.US_EAST_1, Region.US_WEST_2, Region.EU_WEST_1);
    regionStatic = mockStatic(Region.class);
    regionStatic.when(Region::regions).thenReturn(mockRegions);

    String json = service.getAvailableRegions();
    assertNotNull(json);
    var arr = tools.jackson.databind.json.JsonMapper.builder().build().readTree(json);
    assertEquals(3, arr.size());
    assertEquals("us-east-1", arr.get(0).asString());
    assertEquals("us-west-2", arr.get(1).asString());
    assertEquals("eu-west-1", arr.get(2).asString());
  }

  @Test
  public void getAvailableRegions_emptyRegions_returnsNull()
      throws tools.jackson.core.JacksonException {
    regionStatic = mockStatic(Region.class);
    regionStatic.when(Region::regions).thenReturn(List.of());

    String json = service.getAvailableRegions();
    assertNull(json);
  }
}
