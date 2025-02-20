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
package com.percussion.utils.jboss;

import com.percussion.utils.tools.PSTestUtils;
import com.percussion.xml.PSXmlDocumentBuilder;

import junit.framework.TestCase;

import org.w3c.dom.Document;

/**
 * Test case for the {@link PSJndiDatasource} class.
 */
public class PSJndiDatasourceTest extends TestCase
{
   /**
    * Tests the constructor that takes the datasource props.
    * 
    * @throws Exception if the test fails
    */
   public void testCtor() throws Exception
   {
     /*
      Class[] params = new Class[7];

      for (int i = 0; i < params.length; i++)
      {
         params[i] = String.class;
      }
      
      PSTestUtils.testCtor(PSJndiDatasource.class, params, new Object[]
      {"name", "driverName", "classname", "mapping", "server", "uid", "pwd"},
         false);
      PSTestUtils.testCtor(PSJndiDatasource.class, params, new Object[]
      {"", "driverName", "classname", "mapping", "server", "uid", "pwd"}, true);
      PSTestUtils.testCtor(PSJndiDatasource.class, params, new Object[]
      {null, "driverName", "classname", "mapping", "server", "uid", "pwd"},
         true);
      PSTestUtils.testCtor(PSJndiDatasource.class, params, new Object[]
      {"name", "", "classname", "mapping", "server", "uid", "pwd"}, true);
      PSTestUtils.testCtor(PSJndiDatasource.class, params, new Object[]
      {"name", null, "classname", "mapping", "server", "uid", "pwd"}, true);
      PSTestUtils.testCtor(PSJndiDatasource.class, params, new Object[]
      {"name", "driverName", "", "mapping", "server", "uid", "pwd"}, true);
      PSTestUtils.testCtor(PSJndiDatasource.class, params, new Object[]
      {"name", "driverName", null, "mapping", "server", "uid", "pwd"}, true);
      PSTestUtils.testCtor(PSJndiDatasource.class, params, new Object[]
      {"name", "driverName", "classname", "", "server", "uid", "pwd"}, true);
      PSTestUtils.testCtor(PSJndiDatasource.class, params, new Object[]
      {"name", "driverName", "classname", null, "server", "uid", "pwd"}, true);
      PSTestUtils.testCtor(PSJndiDatasource.class, params, new Object[]
      {"name", "driverName", "classname", "mapping", "", "uid", "pwd"}, true);
      PSTestUtils.testCtor(PSJndiDatasource.class, params, new Object[]
      {"name", "driverName", "classname", "mapping", null, "uid", "pwd"}, true);
      PSTestUtils.testCtor(PSJndiDatasource.class, params, new Object[]
      {"name", "driverName", "classname", "mapping", "server", "", ""}, false);
      PSTestUtils.testCtor(PSJndiDatasource.class, params, new Object[]
      {"name", "driverName", "classname", "mapping", "server", null, null},
         false);
   */
   }

   /**
    * Tests the ctor that takes an element, and the toXml() method.
    * 
    * @throws Exception if the test fails
    */
   public void testXml() throws Exception
   {
   /*   PSJndiDatasource ds = new PSJndiDatasource("name", "jtds:sqlserver",
         "classname", "mapping", "server", "uid", "pwd");
      
      Document doc = PSXmlDocumentBuilder.createXmlDocument();
      assertTrue(ds.equals(new PSJndiDatasource(ds.toXml(doc))));
      PSJndiDatasource ds2 = new PSJndiDatasource("name", "jtds:sqlserver",
         "classname", "mapping", "server", null, null);
      assertTrue(ds2.equals(new PSJndiDatasource(ds2.toXml(doc))));
      
      // test security domain setting
      ds.setSecurityDomain("myDomain");
      ds2 = new PSJndiDatasource(ds.toXml(doc));
      assertTrue(!ds.equals(ds2));
      assertNull(ds2.getUserId());
      assertNull(ds2.getPassword());
      assertTrue(ds2.equals(new PSJndiDatasource(ds2.toXml(doc))));
      
      // test conn checker/exception sorter
      ds = new PSJndiDatasource("name", "oracle:thin", "classname", "mapping",
         "server", "uid", "pwd");
      assertTrue(ds.equals(new PSJndiDatasource(ds.toXml(doc))));
      */
   }

   /**
    * Test copyFrom
    * 
    * @throws Exception
    */
   public void testCopyFrom() throws Exception
   {
     /* PSJndiDatasource ds = new PSJndiDatasource("name", "jtds:sqlserver", "classname", "mapping", "server", "uid", "pwd");
      ds.setSecurityDomain("myDomain");
      
      PSJndiDatasource ds_2 = new PSJndiDatasource("name2", "jtds:sqlserver", "classname2", "mapping2", "server2", "uid2", "pwd2");
      assertFalse(ds.equals(ds_2));
      
      ds_2.copyFrom(ds);
      assertTrue(ds.equals(ds_2));
      */
   }
   
   /**
    * Tests all set and get methdos
    * 
    * @throws Exception if the test fails
    */
   public void testAccessors() throws Exception
   {
      /*
      PSJndiDatasource ds = new PSJndiDatasource("name", "jtds:sqlserver",
         "classname", "mapping", "server", "uid", "pwd");
      assertEquals("name", ds.getName());
      assertEquals("jtds:sqlserver", ds.getDriverName());
      assertEquals("classname", ds.getDriverClassName());
      assertEquals("mapping", ds.getContainerTypeMapping());
      assertEquals("server", ds.getServer());
      assertEquals("uid", ds.getUserId());
      assertEquals("pwd", ds.getPassword());
      
      PSTestUtils.testSetter(ds, "Name", "name2", false);
      PSTestUtils.testSetter(ds, "DriverName", "oracle:thin", false);
      PSTestUtils.testSetter(ds, "DriverClassName", "classname2", false);
      PSTestUtils.testSetter(ds, "ContainerTypeMapping", "mapping2", false);
      PSTestUtils.testSetter(ds, "Server", "server2", false);
      PSTestUtils.testSetter(ds, "UserId", "uid2", false);
      PSTestUtils.testSetter(ds, "Password", "pwd2", false);
      
      PSTestUtils.testSetter(ds, "Name", "", true);
      PSTestUtils.testSetter(ds, "DriverName", "", true);
      PSTestUtils.testSetter(ds, "DriverClassName", "", true);
      PSTestUtils.testSetter(ds, "ContainerTypeMapping", "", true);
      PSTestUtils.testSetter(ds, "Server", "", true);
      PSTestUtils.testSetter(ds, "UserId", "", false);
      PSTestUtils.testSetter(ds, "Password", "", false);
 
      PSTestUtils.testSetter(ds, "Name", null, true);
      PSTestUtils.testSetter(ds, "DriverName", null, true);
      PSTestUtils.testSetter(ds, "DriverClassName", null, true);
      PSTestUtils.testSetter(ds, "ContainerTypeMapping", null, true);
      PSTestUtils.testSetter(ds, "Server", null, true);
      PSTestUtils.testSetter(ds, "UserId", null, false);
      PSTestUtils.testSetter(ds, "Password", null, false);
      
      PSTestUtils.testSetter(ds, "SecurityDomain", null, false);
      PSTestUtils.testSetter(ds, "SecurityDomain", "test", false);
      PSTestUtils.testSetter(ds, "SecurityDomain", "", true);
   */
   }
}


