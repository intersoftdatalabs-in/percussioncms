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
package com.percussion.design.objectstore.server;

import com.percussion.design.objectstore.PSApplication;
import com.percussion.design.objectstore.PSObjectFactory;
import com.percussion.error.PSException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Multithreading tests for the lock manager implementation. Set up
 * some fake applications to lock, then spawn a bunch of threads that
 * attempt to acquire locks with different parameters.
 */
public class PSXmlObjectStoreLockManagerTest
{

   private static final Logger log = LogManager.getLogger(PSXmlObjectStoreLockManagerTest.class);

   @Rule
   public TemporaryFolder temporaryFolder = new TemporaryFolder();

   public PSXmlObjectStoreLockManagerTest()
   {
   }

   @Test
   public void testThreads() throws Exception
   {
      int numThreads = 10;
      int numTimes = 10;
      SecureRandom rand = new SecureRandom();

      File lockDir = temporaryFolder.newFolder("Temp","Testing","TestLocks");

      IPSObjectStoreLockManager locker = new PSXmlObjectStoreLockManager(lockDir);
      
      ArrayList threads = new ArrayList(numThreads*2);

      for (int i = 0; i < numThreads; i++)
      {
         threads.add(new Thread(new AppLockTest("testApp", locker, numTimes, rand.nextDouble())));
         threads.add(new Thread(new AppLockTest("testApp_2", locker, numTimes, rand.nextDouble())));
      }

      for (int i = 0; i < threads.size(); i++)
      {
         ((Thread)threads.get(i)).start();
      }

      for (int i = 0; i < threads.size(); i++)
      {
         ((Thread)threads.get(i)).join();
      }
   }

   public void doAssert(String msg, boolean cond)
   {
      assertTrue(msg, cond);
   }

   /**
    * Test {@link PSServerXmlObjectStore.RecoverableFile} class
    */
   @Test
   @Ignore("TODO: This test has a logic problem.  Please fix it.")
   public void testBackFile() throws IOException, PSException {
      PSServerXmlObjectStore.RecoverableFile dirBkup;
      
      // cleanup both source and destination directories if exist
      File newAppDir = temporaryFolder.newFolder("Temp","TestDirDest");
      File appDir = temporaryFolder.newFolder("Temp","TestDir");
      dirBkup = new PSServerXmlObjectStore.RecoverableFile(newAppDir);
      dirBkup.delete();
      dirBkup = new PSServerXmlObjectStore.RecoverableFile(appDir);
      dirBkup.delete();

      
      dirBkup = new PSServerXmlObjectStore.RecoverableFile(appDir);
      assertTrue(!appDir.exists());

      // test rename
      boolean renamed = dirBkup.renameTo(newAppDir);
      assertTrue(renamed);
      assertTrue(newAppDir.exists());
      
      // test recover
      boolean recovered = dirBkup.recover();
      assertTrue(recovered);
      assertTrue(appDir.exists());

      // failed to recover the 2nd time because it has already been done.
      recovered = dirBkup.recover();
      assertFalse(recovered);

      // test delete
      dirBkup = new PSServerXmlObjectStore.RecoverableFile(appDir);
      boolean deleted = dirBkup.delete();
      assertTrue(deleted);
      
      // has already been deleted
      assertFalse(dirBkup.delete());
   }


   class AppLockTest extends PSObjectFactory implements Runnable
   {
      public AppLockTest(String appName, IPSObjectStoreLockManager locker, int numTimes,
         double releaseProb)
      {
         m_appName = appName;
         m_locker = locker;
         m_numTimes = numTimes;
         m_releaseProb = releaseProb;
      }

      public void run()
      {
         try
         {
            SecureRandom rand = new SecureRandom();
            PSApplication testApp = PSObjectFactory.createApplication();
            testApp.setName(m_appName);
            while (m_numTimes-- > 0)
            {
               Object lockKey = m_locker.getLockKey(testApp, m_locker.LOCKTYPE_EXCLUSIVE);
               
               PSXmlObjectStoreLockManagerTest.this.doAssert("" + lockKey, null != lockKey);

               PSXmlObjectStoreLockerId id = new PSXmlObjectStoreLockerId(
                  "" + Thread.currentThread(),
                  false,
                  "session" + Thread.currentThread().hashCode());

               boolean didLock = m_locker.acquireLock(
                  id, lockKey, rand.nextInt(29000) + 1000,
                  rand.nextInt(60000) - rand.nextInt(1000), null);
               if (!didLock)
               {
                  System.err.println(Thread.currentThread().toString() + " timed out.");
               }
               else
               {
                  System.err.println(Thread.currentThread().toString() + " acquired lock.");
                  if (rand.nextDouble() < m_releaseProb)
                  {
                     m_locker.releaseLock(id, lockKey);
                     System.err.println(Thread.currentThread().toString() + " released lock.");
                  }
               }
            }
         }
         catch (Throwable t)
         {
            log.error(t.getMessage());
            log.debug(t.getMessage(), t);
            PSXmlObjectStoreLockManagerTest.this.doAssert("Caught exception: " + t.toString(), false);
         }
      }

      private int m_numTimes;
      private String m_appName;
      private IPSObjectStoreLockManager m_locker;
      private double m_releaseProb;
   }
}
