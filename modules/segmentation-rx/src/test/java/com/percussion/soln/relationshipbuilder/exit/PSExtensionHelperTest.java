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

package com.percussion.soln.relationshipbuilder.exit;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import org.custommonkey.xmlunit.XMLUnit;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.percussion.soln.relationshipbuilder.IPSRelationshipBuilder;
import com.percussion.soln.relationshipbuilder.exit.PSExtensionHelper;
import com.percussion.soln.segment.rx.editor.XMLTestHelper;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.xml.PSXmlDocumentBuilder;

public class PSExtensionHelperTest
{

   private Set<Integer> m_output;


   @BeforeEach
   protected void setUp() throws Exception
   {
      m_output = new HashSet<Integer>();
      XMLUnit.setIgnoreWhitespace(true);
      XMLUnit.setIgnoreAttributeOrder(true);
   }

   @Test
   public void testConvertRejectsNullOutput()
   {
      boolean threw = false;
      try
      {
         PSExtensionHelper.convert(null, null);
      }
      catch (IllegalArgumentException e)
      {
         threw = true;
      }
      assertTrue(threw);
   }

   @Test
   public void testConvertHandlesNullInput()
   {
      Collection<Object> invalids = PSExtensionHelper.convert(null, m_output);
      assertNotNull(invalids);
      assertEquals(0, invalids.size());
   }

   @Test
   public void testConvertHandlesAllNullsInInput()
   {
      Object[] input = new Object[] {null, null};
      Collection<Object> invalids = PSExtensionHelper.convert(input, m_output);
      assertNotNull(invalids);
      assertEquals(2, invalids.size());
   }

   @Test
   public void testConvertHandlesNullsInInput()
   {
      Object[] input = new Object[] {"700", null, 301};
      Collection<Object> invalids = PSExtensionHelper.convert(input, m_output);
      assertNotNull(invalids);
      assertEquals(1, invalids.size());
      assertEquals(2, m_output.size());
   }

   @Test
   public void testConvertHandlesEmptysInInput()
   {
      Object[] input = new Object[] {"700", "", 301};
      Collection<Object> invalids = PSExtensionHelper.convert(input, m_output);
      assertNotNull(invalids);
      assertEquals(1, invalids.size());
      assertEquals(2, m_output.size());
   }

   @Test
   public void testConvertSingleEmptyString() {
	      Object[] input = new Object[] {""};
	      Collection<Object> invalids = PSExtensionHelper.convert(input, m_output);
	      assertNotNull(invalids);
	      assertEquals(1, invalids.size());
	      assertEquals(0, m_output.size());
          if (invalids.size() == 1 && invalids.contains("")) {

          }
          else {
        	  fail();
          }
   }

   @Test
   public void testUpdateDisplayChoicesSelectAll() throws Exception {
       Map<String, String> params = new HashMap<String, String>();
       params.put(PSExtensionHelper.IDS_FIELD_NAME, "tree");
       params.put(IPSHtmlParameters.SYS_CONTENTID, "100");
       IPSRelationshipBuilder builder = new IPSRelationshipBuilder() {
            public Collection<Integer> retrieve(int sourceId)  {
                return null;
            }

            public void synchronize(int sourceId, Set<Integer> targetIds)  {

            }


        };
       PSExtensionHelper helper = new PSExtensionHelper(builder, params, null);
       Document actual = getXml("BeforeCe.xml");
       Document expected = getXml("ExpectedSelectAllCe.xml");
       helper.updateDisplayChoices(actual, true);
       // XMLUnit 1.6 Diff is incompatible with modern DOM/Xerces attribute maps here.
       assertEquals(
           XMLTestHelper.normalizeXml(PSXmlDocumentBuilder.toString(expected)),
           XMLTestHelper.normalizeXml(PSXmlDocumentBuilder.toString(actual)));
   }

   private Document getXml(String file) throws IOException, SAXException {
       String resourceName = file;
        return PSXmlDocumentBuilder.createXmlDocument(
                getClass().getResourceAsStream(resourceName), false);
    }

   @Test
   public void testUpdateDisplayChoices() throws Exception {
       Map<String, String> params = new HashMap<String, String>();
       params.put(PSExtensionHelper.IDS_FIELD_NAME, "tree");
       params.put(IPSHtmlParameters.SYS_CONTENTID, "100");

       IPSRelationshipBuilder stub = new IPSRelationshipBuilder() {

           public Collection<Integer> retrieve(int sourceId) {
               if (sourceId != 100) throw new IllegalStateException("Content id is wrong");
               return Arrays.asList(307,318);
           }

           public void synchronize(int sourceId, Set<Integer> targetIds) {
               throw new IllegalStateException("Should not be called");

           }


      };

       PSExtensionHelper helper = new PSExtensionHelper(stub, params, null);

       Document actual = getXml("BeforeCe.xml");
       Document expected = getXml("ExpectedCe.xml");
       helper.updateDisplayChoices(actual, false);

       // XMLUnit 1.6 Diff is incompatible with modern DOM/Xerces attribute maps here.
       assertEquals(
           XMLTestHelper.normalizeXml(PSXmlDocumentBuilder.toString(expected)),
           XMLTestHelper.normalizeXml(PSXmlDocumentBuilder.toString(actual)));
       //307, 318

   }

}
