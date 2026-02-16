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
package com.percussion.services.contentmgr.impl;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Test some content utils methods
 * 
 * @author dougrand
 */
public class PSContentUtilsTest
{
   /**
    * Check that pattern matching is correct for the collection ref method
    */
   @Test
   public void testIdCollectionCheck()
   {
      assertTrue(PSContentUtils.isIdCollectionRef("t1.a.b"));
      assertTrue(PSContentUtils.isIdCollectionRef("t92.aa.bb"));
      assertFalse(PSContentUtils.isIdCollectionRef("t1.a"));
      assertFalse(PSContentUtils.isIdCollectionRef("t1.a.b.c"));
      assertFalse(PSContentUtils.isIdCollectionRef("t1a.b.c"));
      assertFalse(PSContentUtils.isIdCollectionRef("t1.1a.c"));
   }
}
