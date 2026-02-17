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
package com.percussion.services.content;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.data.PSAutoTranslation;
import com.percussion.services.content.data.PSKeyword;
import com.percussion.services.content.data.PSKeywordChoice;
import com.percussion.services.guidmgr.PSGuidUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for the {@link IPSContentService}
 */

public class PSContentServiceTest
{

   private static final Logger log = LogManager.getLogger(PSContentServiceTest.class);

   /**
    * UnitTesting MSM Functionality. This is installing the translation settings
    * fresh on to a system that has no such settings.
    * @throws Exception
    */
   @Test
   public void testDeserializeTranslationsAndSave() throws Exception
   {
      IPSContentService svc = PSContentServiceLocator.getContentService();

      String xlnStr =
         "<auto-translation id=\"1\"> <community-id>10</community-id>" +
         "<content-type-id>301</content-type-id> <locale>en-gb</locale>" +
         "<workflow-id>5</workflow-id> </auto-translation> <!--  -->";
      xlnStr +=
         " <auto-translation id=\"1\"> <community-id>10</community-id>" +
         " <content-type-id>302</content-type-id> <locale>en-gb</locale>" +
         "<workflow-id>5</workflow-id> </auto-translation><!--  -->";
      xlnStr +=
         " <auto-translation id=\"1\"> <community-id>10</community-id>" +
         " <content-type-id>310</content-type-id> <locale>en-gb</locale>" +
         "<workflow-id>5</workflow-id> </auto-translation><!--  -->";
      xlnStr +=
         " <auto-translation id=\"1\"> <community-id>10</community-id>" +
         " <content-type-id>311</content-type-id> <locale>en-gb</locale>" +
         "<workflow-id>5</workflow-id> </auto-translation><!--  -->";

      List<PSAutoTranslation> xlnList = new ArrayList<PSAutoTranslation>();

      String[] xlns = xlnStr.split("<!--  -->");
      int sz = xlns.length;
      for (int i = 0; i < sz; i++)
      {
         PSAutoTranslation at = new PSAutoTranslation();
         at.fromXML(xlns[i]);
         xlnList.add(at);
      }

      Iterator<PSAutoTranslation> it = xlnList.iterator();
      while(it.hasNext())
      {
         PSAutoTranslation at = it.next();
         // delete before you insert it again (use AUTO_TRANSLATIONS GUID for deletes)
         svc.deleteAutoTranslation(PSGuidUtils.makeGuid(at.getContentTypeId(), PSTypeEnum.AUTO_TRANSLATIONS));
         at.setVersion(null);
         svc.saveAutoTranslation(at);
      }

      List<PSAutoTranslation> all = svc.loadAutoTranslations(PSAutoTranslation.getAutoTranslationsGUID());
      List<PSAutoTranslation> enGb = all.stream().filter(t -> "en-gb".equals(t.getLocale())).toList();
      assertEquals(4, enGb.size());

      // good, now clean up like a good citizen
      it = enGb.iterator();
      while(it.hasNext())
      {
         PSAutoTranslation at = it.next();
         svc.deleteAutoTranslation(PSGuidUtils.makeGuid(at.getContentTypeId(), PSTypeEnum.AUTO_TRANSLATIONS));
      }
   }

   /**
    * Test CRUD services for {@link PSAutoTranslation}
    *
    * @throws Exception if the test fails.
    */
   @Test
   public void testAutoTranslations() throws Exception
   {
      IPSContentService service = PSContentServiceLocator.getContentService();

      PSAutoTranslation at1 = null;
      PSAutoTranslation at2 = null;

      boolean saved1 = false;
      boolean saved2 = false;
      try
      {
         // test create and save (new API: IPSGuid + locale)
         at1 = service.createAutoTranslation(PSGuidUtils.makeGuid(123, PSTypeEnum.NODEDEF), "test1");
         service.saveAutoTranslation(at1);
         saved1 = true;
         at2 = service.createAutoTranslation(PSGuidUtils.makeGuid(235, PSTypeEnum.NODEDEF), "test2");
         service.saveAutoTranslation(at2);
         saved2 = true;

         // test load for the two content types we created and validate
         List<PSAutoTranslation> atList = new ArrayList<>();
         atList.addAll(service.loadAutoTranslations(PSGuidUtils.makeGuid(123, PSTypeEnum.NODEDEF)));
         atList.addAll(service.loadAutoTranslations(PSGuidUtils.makeGuid(235, PSTypeEnum.NODEDEF)));
         assertTrue(atList.size() >= 2);
         for (PSAutoTranslation at : atList)
         {
            assertTrue(!StringUtils.isBlank(at.getLocale()));
            assertTrue(at.getCommunityId() > 0);
            assertTrue(at.getContentTypeId() > 0);
            assertTrue(at.getWorkflowId() > 0);

            List<PSAutoTranslation> scoped = service.loadAutoTranslations(PSGuidUtils.makeGuid(at.getContentTypeId(), PSTypeEnum.NODEDEF));
            PSAutoTranslation found = scoped.stream().filter(x -> at.getLocale().equals(x.getLocale())).findFirst().orElse(null);
            assertEquals(at, found);
         }

         // test re-save and load to compare (reuse the combined scoped list)
         for (int i = 0; i < atList.size(); i++)
         {
            PSAutoTranslation at = atList.get(i);
            at.setCommunityId(at.getCommunityId() + i);
            at.setWorkflowId(at.getWorkflowId() + i);
            service.saveAutoTranslation(at);

            List<PSAutoTranslation> scoped = service.loadAutoTranslations(PSGuidUtils.makeGuid(at.getContentTypeId(), PSTypeEnum.NODEDEF));
            PSAutoTranslation found = scoped.stream().filter(x -> at.getLocale().equals(x.getLocale())).findFirst().orElse(null);
            assertEquals(at, found);
         }

         // test delete (use AUTO_TRANSLATIONS GUID for deletes and scoped loads for verification)
         service.deleteAutoTranslation(PSGuidUtils.makeGuid(at1.getContentTypeId(), PSTypeEnum.AUTO_TRANSLATIONS));
         saved1 = false;
         List<PSAutoTranslation> scoped1 = service.loadAutoTranslations(PSGuidUtils.makeGuid(at1.getContentTypeId(), PSTypeEnum.NODEDEF));
         assertTrue(scoped1.stream().noneMatch(x -> "test1".equals(x.getLocale())));

         service.deleteAutoTranslation(PSGuidUtils.makeGuid(at2.getContentTypeId(), PSTypeEnum.AUTO_TRANSLATIONS));
         saved2 = false;
         List<PSAutoTranslation> scoped2 = service.loadAutoTranslations(PSGuidUtils.makeGuid(at2.getContentTypeId(), PSTypeEnum.NODEDEF));
         assertTrue(scoped2.stream().noneMatch(x -> "test2".equals(x.getLocale())));
      }
      finally
      {
         // quiet cleanup
         try
         {
            if (at1 != null && saved1)
            {
               service.deleteAutoTranslation(PSGuidUtils.makeGuid(at1.getContentTypeId(), PSTypeEnum.AUTO_TRANSLATIONS));
            }

            if (at2 != null && saved2)
            {
               service.deleteAutoTranslation(PSGuidUtils.makeGuid(at2.getContentTypeId(), PSTypeEnum.AUTO_TRANSLATIONS));
            }
         }
         catch (Exception e)
         {
            System.out.println("error deleteing auto translations: " +
               e.getLocalizedMessage());
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         }
      }
   }

   /**
    * Reproduces a test case when deleting a keyword choice with value "1"
    * removed all the keywords. See RX-12295.
    */
   @Test
   public void testSaveKeyword_WithChoice1() throws PSContentException
   {
      final IPSContentService service =
            PSContentServiceLocator.getContentService();
      final PSKeyword keyword =
            service.createKeyword("Label 1", "Keyword Description 1");
      add0_1KeywordChoices(keyword);

      service.saveKeyword(keyword);

      keyword.getChoices().remove(0);
      service.saveKeyword(keyword);
      assertNotNull(service.loadKeyword(keyword.getGUID(), null));

      keyword.getChoices().remove(0);
      service.saveKeyword(keyword);
      assertTrue(keyword.getChoices().isEmpty());
      assertNotNull(service.loadKeyword(keyword.getGUID(), null));

      service.deleteKeyword(keyword.getGUID());
      try
      {
         service.loadKeyword(keyword.getGUID(), null);
         fail();
      }
      catch (PSContentException success) {}
   }

   /**
    * Adds a couple of sample keyword choices with values "0", "1"
    * to the keyword.
    * @param keyword the keyword to add the choices to. Assumed not null.
    */
   private void add0_1KeywordChoices(final PSKeyword keyword)
   {
      {
         final PSKeywordChoice choice = new PSKeywordChoice();
         choice.setLabel("Choice 0 label");
         choice.setDescription("Choice 0 description");
         choice.setValue("0");
         choice.setSequence(0);

         keyword.setChoice(choice);
      }

      {
         final PSKeywordChoice choice = new PSKeywordChoice();
         choice.setLabel("Choice 1 label");
         choice.setDescription("Choice 1 description");
         choice.setValue("1");
         choice.setSequence(1);

         keyword.setChoice(choice);
      }
   }

   @Test
   public void testDeleteKeyword() throws PSContentException
   {
      final IPSContentService service =
         PSContentServiceLocator.getContentService();
      final PSKeyword keyword =
            service.createKeyword("Label 1", "Keyword Description 1");
      add0_1KeywordChoices(keyword);

      service.saveKeyword(keyword);
      assertNotNull(service.loadKeyword(keyword.getGUID(), null), "Keyword should be retrieved");
      checkKeywordDeletion(keyword);

      try
      {
         service.deleteKeyword(null);
         fail();
      }
      catch (IllegalArgumentException success) {}
   }

   public void testDeleteKeyword_noChoices() throws PSContentException
   {
      final IPSContentService service =
         PSContentServiceLocator.getContentService();
      final PSKeyword keyword =
            service.createKeyword("Label 1", "Keyword Description 1");

      service.saveKeyword(keyword);
      assertNotNull(service.loadKeyword(keyword.getGUID(), null), "Keyword should be retrieved");
      checkKeywordDeletion(keyword);
   }

   /**
    * Checks that a keyword can be safely deleted.
    * @param keyword
    */
   private void checkKeywordDeletion(final PSKeyword keyword)
   {
      final IPSContentService service =
         PSContentServiceLocator.getContentService();
      service.deleteKeyword(keyword.getGUID());
      try
      {
         service.loadKeyword(keyword.getGUID(), null);
         fail("Keyword should not be retrieved");
      }
      catch (PSContentException success) {}
   }
}

