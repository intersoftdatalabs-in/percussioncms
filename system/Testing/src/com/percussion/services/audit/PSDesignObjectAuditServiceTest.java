/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.services.audit;

import com.percussion.services.audit.data.PSAuditLogEntry;
import com.percussion.services.audit.data.PSAuditLogEntry.AuditTypes;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;



import junit.framework.TestCase;

/**
 * Test case for the {@link IPSDesignObjectAuditService} class.
 */
 public class PSDesignObjectAuditServiceTest extends TestCase
{
   /**
    * Test that the configuration can be obtained.
    * 
    * @throws Exception If the test fails.
    */
   public void fixMetestConfig() throws Exception
   {
      IPSDesignObjectAuditService svc = 
         PSDesignObjectAuditServiceLocator.getAuditService();
      
      assertNotNull(svc.getConfig());
   }
   
   /**
    * Test creating, saving, and deleting audit entries.
    * 
    * @throws Exception If the test fails.
    */
   public void fixMetestAuditEntries() throws Exception
   {
      IPSDesignObjectAuditService svc = 
         PSDesignObjectAuditServiceLocator.getAuditService();
      
      // delete old entries
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.YEAR, 10);
      svc.deleteAuditLogEntriesByDate(cal.getTime());

      // ensure there are none
      Collection<PSAuditLogEntry> entries = svc.findAuditLogEntries();
      assertNotNull(entries);
      assertTrue(entries.isEmpty());
      
      // test create
      Date date = new Date();
      PSAuditLogEntry entry = createEntry(svc, date, 
         new PSGuid(PSTypeEnum.ACL, 301), "admin1", AuditTypes.SAVE);
      
      // save it
      svc.saveAuditLogEntry(entry);
      
      // reload and test it saved
      entries = svc.findAuditLogEntries();
      assertNotNull(entries);
      assertEquals(1, entries.size());
      assertEquals(entry, entries.iterator().next());
      
      
      // test saving multiples
      cal = Calendar.getInstance();
      cal.add(Calendar.DATE, 1);
      
      Set<PSAuditLogEntry> entrySet1 = new HashSet<PSAuditLogEntry>();
      entrySet1.add(createEntry(svc, cal.getTime(), new PSGuid(
         PSTypeEnum.CONTENT_LIST, 1002), "editor1", AuditTypes.SAVE));

      cal.add(Calendar.SECOND, 5);
      Date beforeDate = cal.getTime();

      cal.add(Calendar.MINUTE, 5);
      Date nextDate = cal.getTime();
      entrySet1.add(createEntry(svc, nextDate, new PSGuid(
         PSTypeEnum.CONTEXT, 1003), "editor2", AuditTypes.SAVE));
      
      cal.add(Calendar.DATE, 1);
      nextDate = cal.getTime();
      entrySet1.add(createEntry(svc, nextDate, new PSGuid(
         PSTypeEnum.EDITION, 1004), "author1", AuditTypes.DELETE));
    
      svc.saveAuditLogEntries(entrySet1);
      Set<PSAuditLogEntry> entrySet2 = new HashSet<PSAuditLogEntry>(
         svc.findAuditLogEntries());
      assertFalse(entrySet1.equals(entrySet2));
      entrySet1.add(entry);
      assertTrue(entrySet1.equals(entrySet2));
      
      // set delete multiples
      svc.deleteAuditLogEntriesByDate(beforeDate);
      assertEquals(2, svc.findAuditLogEntries().size());
      cal.add(Calendar.DATE, 1);
      svc.deleteAuditLogEntriesByDate(cal.getTime());
      
      assertTrue(svc.findAuditLogEntries().isEmpty());
   }
   
   public void testdummy(){
      
   }

   /**
    * Creates an entry, tests that is was assigned a guid, fills in the 
    * supplied values.
    * 
    * @param svc The service to use, assumed not <code>null</code>.
    * @param date The date to set, assumed not <code>null</code>.
    * @param guid The object guid to set, assumed not <code>null</code>.
    * @param user The name of the user to set, assumed not <code>null</code> or 
    * empty.
    * @param type The action type to set, assumed not <code>null</code>.
    * 
    * @return The entry, never <code>null</code>.
    * 
    * @throws Exception If there is an error.
    */
   private PSAuditLogEntry createEntry(IPSDesignObjectAuditService svc,
      Date date, IPSGuid guid, String user, AuditTypes type) throws Exception
   {
      PSAuditLogEntry entry = svc.createAuditLogEntry();
      assertNotNull(entry);
      assertNotNull(entry.getGUID());
      entry.setObjectGUID(guid);
      entry.setDate(date);
      entry.setUserName(user);
      entry.setAction(type);
      return entry;
   }
}

