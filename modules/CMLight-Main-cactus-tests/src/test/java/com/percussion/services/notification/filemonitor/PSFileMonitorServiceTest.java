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
package com.percussion.services.notification.filemonitor;

import com.percussion.utils.testing.IntegrationTest;
import org.apache.cactus.ServletTestCase;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

import java.lang.Thread;

import com.percussion.server.PSServer;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationServiceLocator;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.notification.filemonitor.impl.PSFileMonitorService;
import com.percussion.util.PSPurgableTempFile;
import org.junit.experimental.categories.Category;

/**
 * Unit Test for  File Monitor Notification service {@link PSFileMonitorService}
 */
@Category(IntegrationTest.class)
public class PSFileMonitorServiceTest extends ServletTestCase
      implements IPSNotificationListener
{
   private final int NUM_FILE = 5;

   private boolean m_isSuccess = false;

   private final File[] m_testFile = new File[NUM_FILE];
   private final Boolean[] m_eventReceived = new Boolean[] {false, false, false, false, false};
   private final Thread m_testThread = new Thread(() -> doTest());

   public void testMonitorFile()
   {
      m_testThread.start();
      int waitInSec = 0;
      while (m_testThread.isAlive() && waitInSec++ < 120)
      {
         try
         {
            Thread.sleep(1000);
         }
         catch (InterruptedException e)
         {
            assertTrue("Unexpected thread exception", false);
         }
      }
      if (m_isSuccess)
         System.out.println("testMonitorFile() ran successful.");
      else
         System.out.println("testMonitorFile() FAILED.");
   }

   public void doTest()
   {
      var notifyService = PSNotificationServiceLocator.getNotificationService();
      notifyService.addListener(EventType.FILE, this);

      try
      {
         var fileMonitorService = (PSFileMonitorService) PSFileMonitorServiceLocator.getFileMonitorService();
         int curDirWatcherCount = fileMonitorService.getDirWatcherCount();

         for (int i = 0; i < NUM_FILE; i++)
         {
            m_testFile[i] = createTestFile();
            fileMonitorService.monitorFile(m_testFile[i]);
         }
         Thread.sleep(10 * 1000);

         assertTrue(fileMonitorService.getDirWatcherCount() == 1 + curDirWatcherCount);

         for (int i = 0; i < NUM_FILE; i++)
         {
            writeToTestFile(m_testFile[i], "This is an update test.");
         }

         int tries = 0;
         while ((!recievedAll()) && tries++ < 60)
            Thread.sleep(1000);

         for (int i = 0; i < NUM_FILE; i++)
            assertTrue(m_eventReceived[i]);

         for (int i = 0; i < NUM_FILE; i++)
            fileMonitorService.unmonitorFile(m_testFile[i]);

         assertTrue(fileMonitorService.getDirWatcherCount() == curDirWatcherCount);

         m_isSuccess = true;
      }
      catch (InterruptedException e)
      {
         assertTrue(false);
      }
      finally
      {
         notifyService.removeListener(EventType.FILE, this);
         for (int i = 0; i < NUM_FILE; i++)
            m_testFile[i].delete();
      }
   }

   private boolean recievedAll()
   {
      for (int i = 0; i < NUM_FILE; i++)
      {
         if (!m_eventReceived[i])
            return false;
      }
      return true;
   }

   /*
    * see IPSNotificationListener.notifyEvent() method for details
    */
   @Override
   public synchronized void notifyEvent(PSNotificationEvent event)
   {
      if (event == null || event.getType() != EventType.FILE
            || (!(event.getTarget() instanceof File)))
      {
         throw new IllegalArgumentException(
               "event may not be null and must represent a file change event");
      }

      var tgtFile = (File) event.getTarget();
      int index = -1;
      for (int i = 0; i < NUM_FILE; i++)
      {
         if (tgtFile.getAbsolutePath().equals(m_testFile[i].getAbsolutePath()))
         {
            index = i;
            break;
         }
      }
      assertTrue(index >= 0);

      m_eventReceived[index] = true;
   }

   private File createTestFile()
   {
      File tmpFile = null;
      var tmpDir = new File(PSServer.getRxDir(), "temp");
      try
      {
         tmpFile = new PSPurgableTempFile("tmpFile", ".txt", tmpDir);
      }
      catch (IOException e)
      {
         System.out.println(
               "PSFileMonitorServiceTest.createTestFile: Failure to create temporary file.");
         assertTrue(false);
      }
      return tmpFile;
   }

   private void writeToTestFile(File testFile, String theString)
   {
      try (var testFileWriter = new FileWriter(testFile, true);
           var tfPrintWriter = new PrintWriter(testFileWriter)) {
         tfPrintWriter.println(theString);
         tfPrintWriter.flush();
      }
      catch (IOException e)
      {
         System.out.println("writeToTestFile: Failure to write to test file");
         assertTrue(false);
      }
   }
}
