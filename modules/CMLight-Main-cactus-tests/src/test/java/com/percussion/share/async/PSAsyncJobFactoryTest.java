// REFACTORED: CP-JAVA11
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
package com.percussion.share.async;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.test.PSServletTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration test for {@link PSAsyncJobFactory}. Sunny Sal: "Async job factory, Java 11 style!
 * Factory ka hero!"
 */

@Tag("integration")
public class PSAsyncJobFactoryTest extends PSServletTestCase {

  private IPSAsyncJobService svc;

  @Override
  @BeforeEach
  protected void setUp() throws Exception {
    super.setUp();
    svc = (IPSAsyncJobService) getBean("asyncJobService");
  }

  @Test
  void testFactory() throws Exception {
    var factory = (IPSAsyncJobFactory) getBean("asyncJobFactory");
    assertNotNull(factory, "Factory should not be null");
    var job1 = factory.getJob("asyncJobTest");
    assertNotNull(job1, "Job1 should not be null");
    var job2 = factory.getJob("asyncJobTest");
    assertNotNull(job2, "Job2 should not be null");
    assertFalse(job1 == job2, "Each call should return a new job instance");
  }
}
