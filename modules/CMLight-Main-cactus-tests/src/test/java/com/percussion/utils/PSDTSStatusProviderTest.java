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

package com.percussion.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.integritymanagement.data.PSIntegrityTask.TaskStatus;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


public class PSDTSStatusProviderTest {

  private PSDTSStatusProvider statusProvider;

  public PSDTSStatusProvider getStatusProvider() {
    return statusProvider;
  }

  public void setStatusProvider(PSDTSStatusProvider statusProvider) {
    this.statusProvider = statusProvider;
  }

  @BeforeEach
  public void setUp() throws Exception {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  @Test
  public void testGetStatusReport() {
    var status = getStatusProvider().getDTSStatusReport();
    assertEquals(TaskStatus.SUCCESS, status.get("dts").getFirst());
    assertEquals(TaskStatus.SUCCESS, status.get("feeds").getFirst());
    assertEquals(TaskStatus.SUCCESS, status.get("perc-form-processor").getFirst());
    assertEquals(TaskStatus.SUCCESS, status.get("perc-comments-services").getFirst());
    assertEquals(TaskStatus.SUCCESS, status.get("perc-metadata-services").getFirst());
    assertEquals(TaskStatus.SUCCESS, status.get("perc-membership-services").getFirst());
    assertEquals(TaskStatus.SUCCESS, status.get("perc-polls-services").getFirst());
  }
}
