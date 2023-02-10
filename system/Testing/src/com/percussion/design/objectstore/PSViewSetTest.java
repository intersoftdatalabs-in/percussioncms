/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.design.objectstore;

import com.percussion.util.PSCollection;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for the PSView and PSViewSet classes.
 */
public class PSViewSetTest extends TestCase
{
   // see base class
   public PSViewSetTest(String name)
   {
      super(name);
   }

   /**
    * Tests creating views, adding them to a view set and retrieving them from
    * the viewset by name.
    *
    * @throws Exception if the test fails or an error occurs
    */
   public void testViewSet() throws Exception
   {
      PSViewSet viewSet = new PSViewSet();

      List fieldList = new ArrayList();
      fieldList.add("field1");
      fieldList.add("field2");
      PSView view1 = new PSView("view1", fieldList.iterator());
      viewSet.addView(view1);

      assertEquals("getView", viewSet.getView("VIEW1"), view1);
      assertTrue("get non-existant view", null == viewSet.getView("VIEW2"));

      // test conditional views
      assertTrue("get invalid conditional views not null",
         viewSet.getCondtionalViews("VIEW2") != null);
      assertTrue("get invalid conditional views returns empty iterator",
         !viewSet.getCondtionalViews("VIEW2").hasNext());

      boolean didThrow = false;
      PSCollection conditions = new PSCollection(PSConditional.class);
      conditions.add(new PSConditional(new PSTextLiteral("a"), "=",
         new PSTextLiteral("b")));

      PSConditionalView condView = new PSConditionalView("VIEW1",
         fieldList.iterator(), conditions);
      viewSet.addConditionalView(condView);
      assertTrue("getCondView not null",
         viewSet.getCondtionalViews("VIEW1") != null);
      assertTrue("getCondView has next",
         viewSet.getCondtionalViews("VIEW1").hasNext());
      assertEquals("getCondView equals", condView,
         viewSet.getCondtionalViews("VIEW1").next());

   }

   // collect all tests into a TestSuite and return it - see base class
   public static Test suite()
   {
      TestSuite suite = new TestSuite();
      suite.addTest(new PSViewSetTest("testViewSet"));
      return suite;
   }


}
