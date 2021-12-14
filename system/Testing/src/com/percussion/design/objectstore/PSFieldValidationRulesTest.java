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
package com.percussion.design.objectstore;

import com.percussion.util.PSCollection;
import com.percussion.xml.PSXmlDocumentBuilder;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// Test case
public class PSFieldValidationRulesTest extends TestCase
{
   public PSFieldValidationRulesTest(String name)
   {
      super(name);
   }

   public void testEquals() throws Exception
   {
   }

   public void testXml() throws Exception
   {
      Document doc = PSXmlDocumentBuilder.createXmlDocument();
      Element root = PSXmlDocumentBuilder.createRoot(doc, "Test");

      // create test object
      PSRule rule = new PSRule(new PSExtensionCallSet());
      PSCollection ruleCol = new PSCollection(rule.getClass());
      ruleCol.add(rule);
      ruleCol.add(rule);
      ruleCol.add(rule);
      PSCollection ruleRefs = new PSCollection("java.lang.String");
      ruleRefs.add("ref1");
      ruleRefs.add("ref2");
      ruleRefs.add("ref3");

      PSFieldValidationRules testTo = new PSFieldValidationRules();
      testTo.setApplyWhen(new PSApplyWhen());
      testTo.setName("fieldValidation");
      testTo.setRuleReferences(ruleRefs);
      testTo.setRules(ruleCol);
      testTo.setErrorMessage(new PSDisplayText("one"));
      Element elem = testTo.toXml(doc);
      root.appendChild(elem);

      // create a new object and populate it from our testTo element
      PSFieldValidationRules testFrom = new PSFieldValidationRules(elem, null, null);
      assertTrue(testTo.equals(testFrom));
   }

   public static Test suite()
   {
      TestSuite suite = new TestSuite();

      suite.addTest(new PSFieldValidationRulesTest("testXml"));
      
      return suite;
   }
}
