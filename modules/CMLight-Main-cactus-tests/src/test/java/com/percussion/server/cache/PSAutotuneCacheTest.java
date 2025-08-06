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

package com.percussion.server.cache;

import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("IntegrationTest")
public class PSAutotuneCacheTest
{
   @Test
   public void testConstructors()
   {
      boolean didThrow = false;
      try 
      {
         var settings = PSAutotuneCacheLocator.getAutotuneCache();
      }
      catch (Exception ex) 
      {
         didThrow = true;
      }
      assertTrue(didThrow, "Null file passed as ehcache");

      didThrow = false;
      try
      {
         var settings = PSAutotuneCacheLocator.getAutotuneCache();
      }
      catch (Exception ex) 
      {
         didThrow = true;
      }
      assertFalse(didThrow, "Default Constructor OK.");
   }

   @Test
   public void testUpdateCache()
   {
      boolean didThrow = false;
      try
      {             
         var settings = PSAutotuneCacheLocator.getAutotuneCache();
         settings.updateEhcache();
      }
      catch (Exception ex)
      {
         didThrow = true;
      }
      assertFalse(didThrow, "Update ehcache OK");
   }
}
