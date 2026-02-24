/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.sitemanage.service.impl;

import static com.percussion.test.TestAssertions.*;

import com.percussion.metadata.data.PSMetadata;
import com.percussion.metadata.service.IPSMetadataService;
import java.util.Collection;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

// REFACTORED: CP-JAVA11
class PSPublishStagingTest {

  static class TestablePSPublishStagingService extends PSPublishStagingService {

    private boolean stagingEnabled = false;

    TestablePSPublishStagingService(IPSMetadataService metadata) {
      super(metadata);
    }

    @Override
    public boolean isStagingFeatureEnabled() {
      return stagingEnabled;
    }

    /**
     * Sets the staging feature enabled flag.
     *
     * @param stagingEnabled the stagingEnabled to set
     */
    public void setStagingFeatureEnabled(boolean stagingEnabled) {
      this.stagingEnabled = stagingEnabled;
    }
  }

  static class MockMetadataService implements IPSMetadataService {

    private final HashMap<String, PSMetadata> metadata = new HashMap<>();

    @Override
    public PSMetadata find(String key) {
      return metadata.get(key);
    }

    @Override
    public Collection<PSMetadata> findByPrefix(String prefix) {
      // Not implemented
      return null;
    }

    @Override
    public void save(PSMetadata data) {
      metadata.put(data.getKey(), data);
    }

    @Override
    public void delete(String key) {
      metadata.remove(key);
    }

    @Override
    public void deleteByPrefix(String prefix) {
      // Not implemented
    }
  }

  @Test
  void testStagingActive() {
    var stgService = new TestablePSPublishStagingService(new MockMetadataService());

    // false,false
    assertFalse(stgService.isStagingActive());
    stgService.setStagingOn();
    // false,true
    assertFalse(stgService.isStagingActive());
    stgService.setStagingFeatureEnabled(true);
    // true,true
    assertTrue(stgService.isStagingActive());
    stgService.setStagingOff();
    // true, false
    assertFalse(stgService.isStagingActive());
  }
}
