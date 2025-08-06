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

import com.percussion.services.filestorage.IPSFileStorageService;
import com.percussion.services.filestorage.IPSHashedFieldCataloger;
import com.percussion.services.filestorage.PSFileStorageServiceLocator;
import com.percussion.services.filestorage.PSHashedFieldCatalogerLocator;
import com.percussion.services.filestorage.data.PSHashedColumn;

import java.util.HashSet;
import java.util.Set;

import com.percussion.utils.testing.IntegrationTest;
import org.apache.cactus.ServletTestCase;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

// REFACTORED: CP-JAVA11
@Tag(IntegrationTest.class)
public class PSHashedFieldCatalogerTest extends ServletTestCase
{
   
   IPSFileStorageService storageService;
   
   @Test
   public void testGetHashedFileFields()
   {
      var src = PSFileStorageServiceLocator.getFileStorageService();
      var cataloger = PSHashedFieldCatalogerLocator.getHashedFileCatalogerService();

      var hashedColumns = cataloger.getServerHashedColumns();
      System.out.println(hashedColumns);
      var testSet = new HashSet<PSHashedColumn>();
      testSet.add(new PSHashedColumn("img1_hash","RXS_CT_SHAREDIMAGE","IMG1_HASH"));
      testSet.add(new PSHashedColumn("img2_hash","RXS_CT_SHAREDIMAGE","IMG2_HASH"));
      testSet.add(new PSHashedColumn("activeimg_hash","RXS_CT_NAVIGATIONIMAGETRIPLE","ACTIVEIMG_HASH"));
      testSet.add(new PSHashedColumn("inactiveimg_hash","RXS_CT_NAVIGATIONIMAGETRIPLE","INACTIVEIMG_HASH"));
      testSet.add(new PSHashedColumn("rolloverimg_hash","RXS_CT_NAVIGATIONIMAGETRIPLE","ROLLOVERIMG_HASH"));
      testSet.add(new PSHashedColumn("item_file_attachement_hash","RXS_CT_SHAREDBINARY","ITEM_FILE_ATTACHMENT_HASH"));

      src.getText("111");

      assertTrue(org.apache.commons.collections.CollectionUtils.isEqualCollection(testSet, hashedColumns));
   }

   @Test
   public void testStoreHashedFileFields()
   {
      var testSet = new HashSet<PSHashedColumn>();
      testSet.add(new PSHashedColumn("img1_hash","RXS_CT_SHAREDIMAGE","IMG1_HASH"));
      testSet.add(new PSHashedColumn("img2_hash","RXS_CT_SHAREDIMAGE","IMG2_HASH"));
      testSet.add(new PSHashedColumn("activeimg_hash","RXS_CT_NAVIGATIONIMAGETRIPLE","ACTIVEIMG_HASH"));
      testSet.add(new PSHashedColumn("inactiveimg_hash","RXS_CT_NAVIGATIONIMAGETRIPLE","INACTIVEIMG_HASH"));
      testSet.add(new PSHashedColumn("rolloverimg_hash","RXS_CT_NAVIGATIONIMAGETRIPLE","ROLLOVERIMG_HASH"));
      testSet.add(new PSHashedColumn("item_file_attachement_hash","RXS_CT_SHAREDBINARY","ITEM_FILE_ATTACHMENT_HASH"));

      var service = PSHashedFieldCatalogerLocator.getHashedFileCatalogerService();

      service.storeColumns(testSet);
      var storedColumns = service.getStoredColumns();

      assertTrue(org.apache.commons.collections.CollectionUtils.isEqualCollection(testSet,storedColumns));
   }

   @Test
   public void testValidateFields()
   {
      var service = PSHashedFieldCatalogerLocator.getHashedFileCatalogerService();
      var storedColumns = service.validateColumns();
      System.out.println("Stored columns size "+storedColumns.size());
      for(var column : storedColumns) {
         System.out.println("Column "+column);
         assertTrue(column.isColumnExists());
      }
   }

   public void validateNewField()
   {
      // Placeholder for future test
   }
   


}
