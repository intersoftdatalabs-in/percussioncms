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

import com.percussion.server.PSServer;
import com.percussion.services.PSBaseServiceLocator;
import com.percussion.services.filestorage.IPSFileDigestService;
import com.percussion.services.filestorage.IPSFileMeta;
import com.percussion.services.filestorage.IPSFileStorageService;
import com.percussion.services.filestorage.IPSHashedFieldCataloger;
import com.percussion.services.filestorage.PSFileStorageServiceLocator;
import com.percussion.services.filestorage.PSHashedFieldCatalogerLocator;
import com.percussion.services.filestorage.data.PSHashedColumn;
import com.percussion.services.filestorage.data.PSMeta;
import com.percussion.services.filestorage.error.PSFileStorageException;
import com.percussion.util.IOTools;
import com.percussion.util.PSPurgableTempFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.percussion.utils.testing.IntegrationTest;
import org.apache.cactus.ServletTestCase;
import org.junit.jupiter.api.Test;
import org.junit.experimental.categories.Category;

// REFACTORED: CP-JAVA11
@Category(IntegrationTest.class)
public class PSDbStorageServiceTest extends ServletTestCase
{
   private PSPurgableTempFile testXml;

   private PSPurgableTempFile testTxt;

   private PSPurgableTempFile noFilenameTxt;

   /**
    * Each unit test stores here the hashes that will be removed by the setUp
    * method.
    */
   private static List<String> hashesToRemove;

   @Override
   public void setUp()
   {
      try
      {
         testXml = createFile("<xml>This is a test xml file</xml>", "test1.xml", "text/xml", "UTF-8");
         testTxt = createFile("This is a test txt file", "test2.txt", "text/plain", "UTF-8");
         noFilenameTxt = createFile("This is a test txt file no filename", null, null, null);
         // Delete all PSHashedFile and PSHashedMeta objects in
         // the database
         var fssvc = PSFileStorageServiceLocator.getFileStorageService();

         if (hashesToRemove == null)
         {
            hashesToRemove = new ArrayList<>();
         }
         else
         {
            for (var hash : hashesToRemove)
               fssvc.delete(hash);

            hashesToRemove.clear();
         }
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * Also tests using File instead of PurgeableTempFile
    * 
    * @throws Exception
    */
   public void testStore_HugeFile() throws Exception
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();

      var hugeFilePath = PSServer.getRxFile("InstallableApps/RxApp/rxapp.ear");

      var hugeFileHash = fssvc.store(new File(hugeFilePath));
      assertNotNull(hugeFileHash);
      assertTrue(fssvc.fileExists(hugeFileHash));
      hashesToRemove.add(hugeFileHash);
   }

   /**
    * Test error returned if not filename for purgeable temp file
    * 
    * @throws Exception
    */
   @Test
   public void testStore_NoFilename() throws Exception
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();

      var xmlMeta = new PSMeta();
      assertTrue(xmlMeta.isEmpty());

      try
      {
         fssvc.store(noFilenameTxt);
         assertTrue(false);
      }
      catch (PSFileStorageException e)
      {
         // this is expected
      }

   }

   /**
    * @throws Exception
    */
   public void testStore() throws Exception
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();

      var xmlHash = fssvc.store(testXml);
      assertNotNull(xmlHash);

      assertTrue(fssvc.fileExists(xmlHash));
      hashesToRemove.add(xmlHash);

      var txtHash = fssvc.store(testTxt);
      assertNotNull(txtHash);
      assertTrue(fssvc.fileExists(txtHash));
      hashesToRemove.add(txtHash);

      assertFalse(txtHash.equals(xmlHash));
   }

   /**
    * @throws Exception
    */
   public void testStore_HashAlreadyExists() throws Exception
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();

      var xmlHash = fssvc.store(testXml);
      assertNotNull(xmlHash);

      assertTrue(fssvc.fileExists(xmlHash));
      hashesToRemove.add(xmlHash);

      // Save the same file again
      var xmlHash2 = fssvc.store(testXml);
      assertNotNull(xmlHash2);

      assertTrue(fssvc.fileExists(xmlHash2));

      assertEquals(xmlHash, xmlHash2);
   }

   /**
    * 
    * @throws Exception
    */
   public void testDelete() throws Exception
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();

      var xmlHash = fssvc.store(testXml);
      assertNotNull(xmlHash);

      assertTrue(fssvc.fileExists(xmlHash));

      // Act
      fssvc.delete(xmlHash);

      // Assert
      assertFalse(fssvc.fileExists(xmlHash));

      var meta = fssvc.getMeta(xmlHash);
      assertNotNull(meta);
      assertTrue(meta.isEmpty());
   }

   /**
    * 
    * @throws Exception
    */
   public void testDelete_ObjectDoesNotExist() throws Exception
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();

      fssvc.delete("NonExistantHash");
   }

   /**
    * @throws Exception
    */
   public void testFileExists() throws Exception
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();

      var xmlHash = fssvc.store(testXml);
      assertTrue(fssvc.fileExists(xmlHash));
      assertFalse(fssvc.fileExists("foo"));
      hashesToRemove.add(xmlHash);
   }

   /**
    * @throws Exception
    */
   public void testGetStream() throws Exception
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();

      var xmlHash = fssvc.store(testXml);
      var xmlIn = fssvc.getStream(xmlHash);
      assertNotNull(xmlIn);
      assertTrue(IOTools.compareStreams(new FileInputStream(testXml), xmlIn));
      hashesToRemove.add(xmlHash);

      var txtHash = fssvc.store(testTxt);
      assertFalse(IOTools.compareStreams(fssvc.getStream(xmlHash), fssvc.getStream(txtHash)));
      hashesToRemove.add(txtHash);

      assertNull(fssvc.getStream("foo"));
   }

   /**
    * @throws Exception
    */
   public void testGetMeta() throws Exception
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();

      var xmlHash = fssvc.store(testXml);
      var xmlMeta = fssvc.getMeta(xmlHash);
      assertFalse(xmlMeta.isEmpty());
      assertEquals(xmlMeta.entrySet(), fssvc.getMeta(xmlHash).entrySet());
      hashesToRemove.add(xmlHash);

      var txtHash = fssvc.store(testTxt);
      var txtMeta = fssvc.getMeta(txtHash);
      assertFalse(txtMeta.isEmpty());
      assertEquals(txtMeta.entrySet(), fssvc.getMeta(txtHash).entrySet());
      assertFalse(txtMeta.equals(xmlMeta));
      hashesToRemove.add(txtHash);
   }

   /**
    * @throws Exception
    */
   public void testGetAlgorithm() throws Exception
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();

      assertNotNull(fssvc.getAlgorithm());
   }

   private PSPurgableTempFile createFile(String content, String sourceFile, String contentType, String encType)
         throws IOException
   {
      var f = new PSPurgableTempFile("tmp", "tmp", null, sourceFile, contentType, encType);

      try (var fw = new FileWriter(f)) {
         fw.write(content);
      }

      return f;
   }

   @Test
   public void testCountOld()
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();
      assertEquals(0, fssvc.countOlderThan(1));
   }

   @Test
   public void testDeleteOld()
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();
      fssvc.deleteOlderThan(1);
      assertEquals(0, fssvc.countOlderThan(1));
   }

   @Test
   public void testTouchHashes()
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();
      var service = PSHashedFieldCatalogerLocator.getHashedFileCatalogerService();
      var columns = service.validateColumns();
      fssvc.touchAllHashes(columns);
   }

   @Test
   public void testExportAll()
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();
      fssvc.exportAllBinary("exportTest");
   }

   @Test
   public void testExportAllLegacy()
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();
      fssvc.exportAllLegacyBinary("exportTest");
   }

   @Test
   public void testImportAll()
   {
      var fssvc = PSFileStorageServiceLocator.getFileStorageService();
      fssvc.importAllBinary("exportTest");
   }

   private IPSFileDigestService getFileDigestService()
   {
      return (IPSFileDigestService) PSBaseServiceLocator.getBean("sys_digestService");
   }
}
