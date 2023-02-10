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
import com.percussion.xml.PSXmlDocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Directory object store class testing, including constructors,
 * <code>PSComponent</code> functionality, accessors and XML functionality.
 */
public class PSDirectorySetTest extends TestCase
{
   /**
    * Constructor to call base class constructor.
    *
    * @see TestCase#TestCase(String) for more information.
    */
   public PSDirectorySetTest(String name)
   {
      super(name);
   }

   // See base class
   public void setUp()
   {
   }

   /**
    * Test component constructors and accessors.
    *
    * @throws Exception if any exceptions occur or assertions fail.
    */
   public void testComponent() throws Exception
   {
      PSDirectorySet dirSet1 = new PSDirectorySet("dirSet1", "uid");
      dirSet1.addDirectoryRef(ms_ref1);

      PSDirectorySet dirSet2 = new PSDirectorySet("dirSet2", "uid");
      dirSet2.addDirectoryRef(ms_ref2);

      PSDirectorySet dirSet3 = new PSDirectorySet("dirSet1", "uid");
      dirSet3.addDirectoryRef(ms_ref1);
      dirSet3.addDirectoryRef(ms_ref2);

      // testing equals
      assertTrue(dirSet1.equals(dirSet1));
      assertTrue(!dirSet1.equals(dirSet2));

      // testing clone/copyFrom
      Document doc = PSXmlDocumentBuilder.createXmlDocument();

      PSDirectorySet clone = (PSDirectorySet) dirSet1.clone();
      assertTrue(clone.equals(dirSet1));
      dirSet2.copyFrom(dirSet1);
      assertTrue(dirSet2.equals(dirSet3));

      // testing name accessors
      boolean didThrow = false;
      try
      {
         dirSet1.setName(null);
      }
      catch (IllegalArgumentException e)
      {
         didThrow = true;
      }
      assertTrue(didThrow);

      didThrow = false;
      try
      {
         dirSet1.setName(" ");
      }
      catch (IllegalArgumentException e)
      {
         didThrow = true;
      }
      assertTrue(didThrow);

      // testing directory accessors
      didThrow = false;
      try
      {
         dirSet1.addDirectoryRef(null);
      }
      catch (IllegalArgumentException e)
      {
         didThrow = true;
      }
      assertTrue(didThrow);
      didThrow = false;
      try
      {
         dirSet1.addDirectoryRef(new PSReference("dir1", "invalidType"));
      }
      catch (IllegalArgumentException e)
      {
         didThrow = true;
      }
      assertTrue(didThrow);
      
      // testing known attributes accessors
      didThrow = false;
      try
      {
         dirSet1.setEmailAttributeName(null);
      }
      catch (IllegalArgumentException e)
      {
         didThrow = true;
      }
      assertTrue(!didThrow);
      didThrow = false;
      try
      {
         dirSet1.setEmailAttributeName(" ");
      }
      catch (IllegalArgumentException e)
      {
         didThrow = true;
      }
      assertTrue(didThrow);
      didThrow = false;
      try
      {
         dirSet1.setObjectAttributeName(null);
      }
      catch (IllegalArgumentException e)
      {
         didThrow = true;
      }
      assertTrue(didThrow);
      didThrow = false;
      try
      {
         dirSet1.setObjectAttributeName(" ");
      }
      catch (IllegalArgumentException e)
      {
         didThrow = true;
      }
      assertTrue(didThrow);
      assertTrue(dirSet1.getRequiredAttributeName(
         PSDirectorySet.OBJECT_ATTRIBUTE_KEY) != null);
   }

   /**
    * Test to and from xml methods of this os object.
    *
    * @throws Exception If any exceptions occur or assertions fail.
    */
   public void testXml() throws Exception
   {
      Document doc = PSXmlDocumentBuilder.createXmlDocument();

      PSDirectorySet dirSet1 = new PSDirectorySet("dirSet1", "uid");
      dirSet1.add(ms_ref1);
      dirSet1.add(ms_ref2);
      dirSet1.add(ms_ref3);
      System.out.println("directorySet 1:\n" +
         PSXmlDocumentBuilder.toString(dirSet1.toXml(doc)));
      PSDirectorySet dirSet2 = new PSDirectorySet(dirSet1.toXml(doc));
      System.out.println("directorySet 2:\n" +
         PSXmlDocumentBuilder.toString(dirSet2.toXml(doc)));
      assertTrue(dirSet1.equals(dirSet2));
   }

   /**
    * Collect all tests into a TestSuite and return it.
    *
    * @return The suite of test methods for this class.  Not <code>null</code>.
    */
   public static Test suite()
   {
      TestSuite suite = new TestSuite();

      suite.addTest(new PSDirectorySetTest("testComponent"));
      suite.addTest(new PSDirectorySetTest("testXml"));

      return suite;
   }

   /**
    * Directory reference 1.
    */
   public static PSReference ms_ref1 = new PSReference("dir1",
      PSDirectory.class.getName());

   /**
    * Directory reference 2.
    */
   public static PSReference ms_ref2 = new PSReference("dir2",
      PSDirectory.class.getName());

   /**
    * Directory reference 3.
    */
   public static PSReference ms_ref3 = new PSReference("dir3",
      PSDirectory.class.getName());
}

