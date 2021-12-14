/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.services.assembly.impl;

import com.percussion.data.PSStylesheetCleanupFilter;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.util.PSResourceUtils;
import com.percussion.utils.testing.UnitTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * @author dougrand
 */
@Category(UnitTest.class)
public class PSNamespaceCleanupTest
{
   /**
    * Input file for configuration
    */
   static File ms_cfile = null;

   @BeforeClass
   public static void setUp() throws Exception
   {
      ms_cfile = PSResourceUtils.getFile(PSNamespaceCleanupTest.class,
              "/com/percussion/services/assembly/namespaceConfig.xml", null);

      PSStylesheetCleanupFilter.getInstance(ms_cfile);
   }

   /**
    * Input
    */
   static final String ms_input = "<?xml version='1.0'?>\n"
         + "<!-- comment --><div a='1' xmlns:goofy='http://www.goofy.org'>"
         + "<el1 b='2'><el2 c='3' xmlns:bletch='somethingelse'/></el1>"
         + "<el1 xmlns:foobar='someotheruri'/>"
         + "</div>";
   
   /**
    * Expected result
    */
   static final String ms_result = "<!-- comment --><div a=\"1\"><el1 b=\"2\"><el2 c=\"3\"/>"
         + "</el1><el1/></div>";

   /**
    * Test cleanup
    */
   @Test
   public void testNSCleanup() throws PSNotFoundException {
      PSNamespaceCleanup cleanup = new PSNamespaceCleanup(null);
      String result = (String) cleanup.translate(ms_input);
      assertEquals(ms_result, result);
   }
}
