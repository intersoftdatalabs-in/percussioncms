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

package com.percussion.data.jdbc;

import com.percussion.cms.IPSConstants;
import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PSFileSystemConnectionTest
{
   private static final Logger log = LogManager.getLogger(IPSConstants.TEST_LOG);

   // the root dir for the tests
   private File m_rootDir;

   // the connection properties for the single root elem doc
   private Properties m_connProperties;

   @Rule
   public Path tempFolder = new TemporaryFolder();

   @BeforeEach 
   public void setUp()
   {

      try
      {
         m_rootDir = tempFolder.newFolder("Testing");
         m_connProperties = new Properties();
         m_rootDir.mkdirs();
         m_connProperties.setProperty("catalog", m_rootDir.getCanonicalPath());
      }
      catch (IOException e)
      {
         log.error(PSExceptionUtils.getDebugMessageForLog(e));
      }
   }

   @Test
   public void testOpen() throws Exception
   {
      // make sure the class loads itself and registers itself
      Class.forName("com.percussion.data.jdbc.PSFileSystemDriver");

      Connection fileConn = DriverManager.getConnection("jdbc:psfilesystem",
            m_connProperties);

      assertFalse("Connection initially open", fileConn.isClosed());

      fileConn.close();

      assertTrue("Connection closes properly", fileConn.isClosed());
   }



}
