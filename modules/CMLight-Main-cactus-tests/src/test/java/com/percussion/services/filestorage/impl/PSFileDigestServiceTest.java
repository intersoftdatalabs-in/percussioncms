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
package com.percussion.services.filestorage.impl;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.services.filestorage.IPSFileDigestService;
import com.percussion.util.PSPurgableTempFile;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import com.percussion.utils.testing.IntegrationTest;
import org.apache.cactus.ServletTestCase;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;

// REFACTORED: CP-JAVA11
@Tag(IntegrationTest.class)
public class PSFileDigestServiceTest extends ServletTestCase
{
   /**
    * @throws Exception
    */
   public void testCreateChecksum() throws Exception
   {
      var fdsvc = getFileDigestService();

      var testXml = createFile("This is a test xml file");
      var testTxt = createFile("This is a test txt file");

      assertFalse(org.apache.commons.io.FileUtils.contentEquals(testXml, testTxt));

      try (var finXml1 = new FileInputStream(testXml)) {
         var xmlChecksum1 = fdsvc.createChecksum(finXml1);
         assertNotNull(xmlChecksum1);

         try (var finXml2 = new FileInputStream(testXml)) {
            var xmlChecksum2 = fdsvc.createChecksum(finXml2);
            assertNotNull(xmlChecksum2);
            assertEquals(xmlChecksum1, xmlChecksum2);
         }
      }

      try (var finTxt = new FileInputStream(testTxt)) {
         var txtChecksum = fdsvc.createChecksum(finTxt);
         assertNotNull(txtChecksum);
         try (var finXml = new FileInputStream(testXml)) {
            var xmlChecksum = fdsvc.createChecksum(finXml);
            assertFalse(txtChecksum.equals(xmlChecksum));
         }
      }
   }

   /**
    * @throws Exception
    */
   public void testGetAlgorithm() throws Exception
   {
      var fdsvc = getFileDigestService();
      assertNotNull(fdsvc.getAlgorithm());
   }

   private PSPurgableTempFile createFile(String content) throws IOException
   {
      var f = new PSPurgableTempFile("tmp", "tmp", null);
      try (var fw = new FileWriter(f)) {
         fw.write(content);
      }
      return f;
   }

   private IPSFileDigestService getFileDigestService()
   {
      return (IPSFileDigestService) PSBaseServiceLocator.getBean(
            "sys_digestService");
   }
}
