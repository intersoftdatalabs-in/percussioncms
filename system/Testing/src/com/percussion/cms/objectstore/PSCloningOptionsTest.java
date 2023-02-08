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
package com.percussion.cms.objectstore;

import com.percussion.xml.PSXmlDocumentBuilder;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.w3c.dom.Document;

/**
 * Test the cloning options according to the schema defined in schema
 * sys_FolderParameters.xsd.
 */
public class PSCloningOptionsTest extends TestCase
{
   // see base class for documentation
   public PSCloningOptionsTest(String name)
   {
      super(name);
   }
   
   /**
    * Test all public constuctors.
    * 
    * @throws Exception for any error.
    */
   public void testConstructors() throws Exception
   {
      Map communityMappings = new HashMap();
      
      // test valid site type
      new PSCloningOptions(
         PSCloningOptions.TYPE_SITE, "siteToCopy", "siteName", "folderName", 
         PSCloningOptions.COPY_NO_CONTENT, PSCloningOptions.COPYCONTENT_AS_LINK, 
         communityMappings);
      
      // test valid subfolder type
      new PSCloningOptions(
         PSCloningOptions.TYPE_SITE_SUBFOLDER, "folderName", 
         PSCloningOptions.COPY_NO_CONTENT, PSCloningOptions.COPYCONTENT_AS_LINK, 
         null);
      
      // test invalid type
      Exception exception = null;
      try
      {
         new PSCloningOptions(
            50, null, "siteToCopy", "folderName", 
            PSCloningOptions.COPY_NO_CONTENT, 
            PSCloningOptions.COPYCONTENT_AS_LINK, communityMappings);
      }
      catch (Exception e)
      {
         exception = e;
      }
      assertTrue(exception instanceof IllegalArgumentException);
      
      // test null folderName
      exception = null;
      try
      {
         new PSCloningOptions(
            PSCloningOptions.TYPE_SITE, "siteToCopy", "siteName", null, 
            PSCloningOptions.COPY_NO_CONTENT, 
            PSCloningOptions.COPYCONTENT_AS_LINK, communityMappings);
      }
      catch (Exception e)
      {
         exception = e;
      }
      assertTrue(exception instanceof IllegalArgumentException);
      
      // test empty folderName
      exception = null;
      try
      {
         new PSCloningOptions(
            PSCloningOptions.TYPE_SITE, "siteToCopy", "siteName", " ", 
            PSCloningOptions.COPY_NO_CONTENT, 
            PSCloningOptions.COPYCONTENT_AS_LINK, communityMappings);
      }
      catch (Exception e)
      {
         exception = e;
      }
      assertTrue(exception instanceof IllegalArgumentException);
      
      // test invalid copyOptions
      exception = null;
      try
      {
         new PSCloningOptions(
            PSCloningOptions.TYPE_SITE, "siteToCopy", "siteName", "folderName", 
            -1, PSCloningOptions.COPYCONTENT_AS_LINK, 
            communityMappings);
      }
      catch (Exception e)
      {
         exception = e;
      }
      assertTrue(exception instanceof IllegalArgumentException);
      
      // test invalid copyContentOptions
      exception = null;
      try
      {
         new PSCloningOptions(
            PSCloningOptions.TYPE_SITE, "siteToCopy", "siteName", "folderName", 
            PSCloningOptions.COPY_NO_CONTENT, -1, 
            communityMappings);
      }
      catch (Exception e)
      {
         exception = e;
      }
      assertTrue(exception instanceof IllegalArgumentException);
   }
   
   /**
    * Test all public methods contracts.
    * 
    * @throws Exception for any error.
    */
   public void testPublicAPI() throws Exception
   {
      Map communityMappings = new HashMap();
      communityMappings.put(new Integer(1), new Integer(2));
      communityMappings.put(new Integer(3), new Integer(4));
      communityMappings.put(new Integer(5), new Integer(6));
      
      PSCloningOptions options_1 = new PSCloningOptions(
         PSCloningOptions.TYPE_SITE, "siteToCopy", "siteName", "folderName", 
         PSCloningOptions.COPY_NO_CONTENT, PSCloningOptions.COPYCONTENT_AS_LINK, 
         communityMappings);
      
      PSCloningOptions options_2 = new PSCloningOptions(
         PSCloningOptions.TYPE_SITE_SUBFOLDER, "folderName", 
         PSCloningOptions.COPY_NAVIGATION_CONTENT, 
         PSCloningOptions.COPYCONTENT_AS_LINK, null);
      
      assertTrue(!options_1.equals(options_2));
      
      // test copyFrom
      options_2.copyFrom(options_1);
      assertTrue(options_1.equals(options_2));
      
      // test clone
      assertTrue(options_1.equals(options_1.clone()));
      
      // test toXml / fromXml
      Document doc = PSXmlDocumentBuilder.createXmlDocument();
      PSCloningOptions options_1_copy = 
         new PSCloningOptions(options_1.toXml(doc), null, null);
      assertTrue(options_1.equals(options_1_copy));
      
      PSCloningOptions options_3 = new PSCloningOptions(
         PSCloningOptions.TYPE_SITE_SUBFOLDER, "folderName", 
         PSCloningOptions.COPY_ALL_CONTENT, 
         PSCloningOptions.COPYCONTENT_AS_NEW_COPY, 
         null);
      PSCloningOptions options_3_copy = 
         new PSCloningOptions(options_3.toXml(doc), null, null);
      assertTrue(options_3.equals(options_3_copy));
      
      PSCloningOptions options_4 = new PSCloningOptions(
         PSCloningOptions.TYPE_SITE_SUBFOLDER, "folderName", 
         PSCloningOptions.COPY_NAVIGATION_CONTENT, 
         PSCloningOptions.COPYCONTENT_AS_LINK, null);
      options_4.addSiteMapping(new Integer(100), new Integer(201));
      options_4.addSiteMapping(new Integer(101), new Integer(202));
      options_4.addSiteMapping(new Integer(102), new Integer(203));
      PSCloningOptions options_4_copy = 
         new PSCloningOptions(options_4.toXml(doc), null, null);
      assertTrue(options_4.equals(options_4_copy));
      
      PSCloningOptions options_5 = new PSCloningOptions(
         PSCloningOptions.TYPE_SITE, "siteToCopy", "siteName", "folderName", 
         PSCloningOptions.COPY_NO_CONTENT, PSCloningOptions.COPYCONTENT_AS_LINK, 
         communityMappings);
      options_5.addSiteMapping(new Integer(100), new Integer(201));
      options_5.addSiteMapping(new Integer(101), new Integer(202));
      options_5.addSiteMapping(new Integer(102), new Integer(203));
      PSCloningOptions options_5_copy = 
         new PSCloningOptions(options_5.toXml(doc), null, null);
      assertTrue(options_5.equals(options_5_copy));
   }
   
   // see base class for documentation
   public static Test suite()
   {
      TestSuite suite = new TestSuite(PSCloningOptionsTest.class);
      
      return suite;
   }
}
