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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.utils.tools.PSBaseXmlConfigTest;
import com.percussion.xml.PSXmlDocumentBuilder;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

public class PSUpgradePluginAddMembershipDeliveryServerTest
{

   @Test
   public void testUpgradeConfig() throws Exception
   {
      File noMemberhipFile = File.createTempFile("TestMembershipUpgradePlugin", ".xml");
      File bak = File.createTempFile("TestMembershipUpgradePlugin", ".xml");

      // copy test resource to temp file
      try (InputStream srcNoMembershipStream = this.getClass().getResourceAsStream("/com/percussion/rxupgrade/deliveryServicesNoMembership.xml"))
      {
         FileUtils.copyInputStreamToFile(srcNoMembershipStream, noMemberhipFile);
      }

      // upgrade the file
      PSUpgradePluginAddMembershipDeliveryServer plugin = new PSUpgradePluginAddMembershipDeliveryServer();
      plugin.upgradeConfig(noMemberhipFile, bak);

      // create temporary file for expected result for comparison
      File withMemberhipFile = File.createTempFile("TestMembershipUpgradePlugin", ".xml");
      try (InputStream withMembershipStream = this.getClass().getResourceAsStream("/com/percussion/rxupgrade/deliveryServicesWithMembership.xml"))
      {
         FileUtils.copyInputStreamToFile(withMembershipStream, withMemberhipFile);
      }

      // compare
      PSBaseXmlConfigTest.compareXmlDocs(withMemberhipFile, noMemberhipFile, true);
   }
}
