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
package com.percussion.install;

import com.percussion.testing.PSAbstractSpringContextTest;
import com.percussion.utils.annotations.IgnoreInWebAppSpringContext;
import org.junit.jupiter.api.Disabled;

import com.percussion.utils.testing.SpringContextTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test menu visibility acl install plugin
 *
 * @author dougrand
 */
@Tag("IntegrationTest")
@Tag("SpringContextTest")
 @ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@Disabled
public class PSCreateMenuVisibilityAclsTest extends PSAbstractSpringContextTest
{
   /**
    * Test visibility acl checker
    */
   @Test
   public void testMenuVisibility()
   {
      PSCreateMenuVisibilityAcls mva = new PSCreateMenuVisibilityAcls();

      mva.process(null, null);
   }
}
