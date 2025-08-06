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
package com.percussion.tablefactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit test for PSJdbcDbmsDef.
 */
public class PSJdbcDbmsDefTest
{

   @Rule
   public Path temporaryFolder;

   private String rxdeploydir;

   @BeforeEach 
   public void setup(){
      rxdeploydir = System.getProperty("rxdeploydir");
      System.setProperty("rxdeploydir",temporaryFolder.getRoot().getAbsolutePath());
   }

   @AfterEach
   public void teardown(){
      //Reset the deploy dir property if it was set prior to test
      if(rxdeploydir != null)
         System.setProperty("rxdeploydir",rxdeploydir);
   }


   public PSJdbcDbmsDefTest() {}

   /**
    * Test the def
    */
   @Test
   public void testDef() throws Exception
   {
      Properties props = new Properties();
      props.setProperty(PSJdbcDbmsDef.DB_BACKEND_PROPERTY, "MSSQL");
      props.setProperty(PSJdbcDbmsDef.DB_DRIVER_CLASS_NAME_PROPERTY,
         "com.inet.tds.TdsDriver");
      props.setProperty(PSJdbcDbmsDef.DB_DRIVER_NAME_PROPERTY, "inetdae7");
      props.setProperty(PSJdbcDbmsDef.DB_SERVER_PROPERTY, "Fry");
      props.setProperty(PSJdbcDbmsDef.DB_NAME_PROPERTY, "rxMaster");
      props.setProperty(PSJdbcDbmsDef.DB_SCHEMA_PROPERTY, "dbo");
      props.setProperty(PSJdbcDbmsDef.UID_PROPERTY, "Rhythmyx");
      props.setProperty(PSJdbcDbmsDef.PWD_PROPERTY, "Rhythmyx");

      PSJdbcDbmsDef def = new PSJdbcDbmsDef(props);

      assertEquals(props.getProperty(PSJdbcDbmsDef.DB_BACKEND_PROPERTY),
         def.getBackEndDB());
      assertEquals(props.getProperty(PSJdbcDbmsDef.DB_DRIVER_CLASS_NAME_PROPERTY),
         def.getDriverClassName());
      assertEquals(props.getProperty(PSJdbcDbmsDef.DB_DRIVER_NAME_PROPERTY),
         def.getDriver());
      assertEquals(props.getProperty(PSJdbcDbmsDef.DB_SERVER_PROPERTY),
         def.getServer());
      assertEquals(props.getProperty(PSJdbcDbmsDef.DB_NAME_PROPERTY),
         def.getDataBase());
      assertEquals(props.getProperty(PSJdbcDbmsDef.DB_SCHEMA_PROPERTY),
         def.getSchema());
      assertEquals(props.getProperty(PSJdbcDbmsDef.UID_PROPERTY),
         def.getUserId());
      assertEquals(props.getProperty(PSJdbcDbmsDef.PWD_PROPERTY),
         def.getPassword());

      PSJdbcDbmsDef def2 = new PSJdbcDbmsDef(props);
      assertTrue(def.equals(def2));

   }


}
