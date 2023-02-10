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
package com.percussion.i18n.rxlt;

/**
 * This class displays idle dots to console at a regular interval. Any UI class
 * that has one or more operations consuming lot of time can extend this class
 * to display idle dots during that opertion.
 */

public class PSIdleDotter extends Thread
{
   /**
    * Implementation of the Thread's run method. Writes dots at a regular time
    * interval as long as the process is not ended and m_displayDot flag is
    * <code>true</code>.
    */
   public void run()
   {
      while(!m_processEnded)
      {
         if(!m_displayDot)
            continue;
         try
         {
            System.out.print(".");
            Thread.sleep(200L);
         }
         catch(Exception e){}
      }
   }

   /**
    * Method to set that flag to indicate dot session is ended.
    */
   public void endDotSession()
   {
      m_processEnded = true;
   }

   /**
    * Method to set that flag to display dots.
    * @param showOrStop falg to tell if the dots are to be displayed or not to.
    */
   public void showDots(boolean showOrStop)
   {
      if (!PSCommandLineProcessor.isLogEnabled())
         return;
      
      if (!PSCommandLineProcessor.areDotsEnabled())
         return;
      
      if(!m_started)
      {
         start();
         m_started = true;
      }
      m_displayDot = showOrStop;
      if(!showOrStop)
         //Just disply empty line after dot session
         System.out.println();

   }

   /**
    * Override this method to make sure the process ends at least during garbage
    * collection.
    */
   public void finlaize()
   {
      m_processEnded = true;
   }

   /**
    * Toggle switch to display or not to display idle dots.
    */
   private boolean m_displayDot = false;
   /**
    * switch to indicate that the process ended. Once this is true thread quits
    * the run() method.
    */
   private boolean m_processEnded = false;
   /**
    * Switch indicating if this thread is started or not.
    */
   private boolean m_started = false;

}
