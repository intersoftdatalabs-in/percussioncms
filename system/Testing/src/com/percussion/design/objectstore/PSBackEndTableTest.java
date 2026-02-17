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

package com.percussion.design.objectstore;

import com.percussion.xml.PSXmlDocumentBuilder;

import org.w3c.dom.Document;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class PSBackEndTableTest
{





   public void testEquals() throws Exception
   {
      PSBackEndTable tab = new PSBackEndTable();
      PSBackEndTable otherTab = new PSBackEndTable();
      assertEquals(tab, otherTab);

      tab = new PSBackEndTable("foobar");
      assertEquals(tab.getAlias(), "foobar");
      assertFalse(tab.equals(otherTab));

      otherTab.setAlias("foobarbaz");
      assertFalse(tab.equals(otherTab));
      otherTab.setAlias("foobar");
      assertEquals(tab, otherTab);

      tab.setDataSource("foods");
      assertEquals("foods", tab.getDataSource());
      assertFalse(tab.equals(otherTab));
      otherTab.setDataSource("foods");
      assertEquals(tab, otherTab);
      assertEquals("foods", otherTab.getDataSource());


      tab.setTable("footable");
      assertEquals(tab.getTable(), "footable");
      assertFalse(tab.equals(otherTab));
      otherTab.setTable("footable");
      assertEquals(tab, otherTab);
      assertEquals("footable", otherTab.getTable());

   }



   @Test
   public void testCopyFrom() throws Exception
   {
      PSBackEndTable tab = new PSBackEndTable();
      PSBackEndTable otherTab = new PSBackEndTable();
      assertEquals(tab, otherTab);

      tab = new PSBackEndTable("foobar");
      assertEquals(tab.getAlias(), "foobar");
      assertFalse(tab.equals(otherTab));

      otherTab.copyFrom(tab);
      assertEquals(tab, otherTab);

      tab.setDataSource("foods");
      assertEquals("foods", tab.getDataSource());
      assertFalse(tab.equals(otherTab));
      otherTab.copyFrom(tab);
      assertEquals(tab, otherTab);

      tab.setTable("footable");
      assertEquals(tab.getTable(), "footable");
      assertFalse(tab.equals(otherTab));
      otherTab.copyFrom(tab);
      assertEquals(tab, otherTab);
   }



   @Test
   public void testXml() throws Exception
   {
      PSBackEndTable tab = new PSBackEndTable();
      PSBackEndTable otherTab = new PSBackEndTable();
      assertEquals(tab, otherTab);

      tab = new PSBackEndTable("foobar");
      assertEquals(tab.getAlias(), "foobar");
      assertFalse(tab.equals(otherTab));

      tab.setDataSource("abc");
      tab.setTable("mno");

      Document doc = PSXmlDocumentBuilder.createXmlDocument();

      otherTab.fromXml(tab.toXml(doc), null, null);
      assertEquals(tab, otherTab);

      doc = PSXmlDocumentBuilder.createXmlDocument();
      tab.setDataSource("foods");
      assertEquals("foods", tab.getDataSource());
      assertFalse(tab.equals(otherTab));
      otherTab.fromXml(tab.toXml(doc), null, null);
      assertEquals(tab, otherTab);

      doc = PSXmlDocumentBuilder.createXmlDocument();
      tab.setTable("footable");
      assertEquals(tab.getTable(), "footable");
      assertFalse(tab.equals(otherTab));
      otherTab.fromXml(tab.toXml(doc), null, null);
      assertEquals(tab, otherTab);
   }

   // collect all tests into a TestSuite and return it

}
