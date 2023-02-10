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
package com.percussion.workflow;

import com.percussion.server.PSRequestContext;
import com.percussion.utils.testing.IntegrationTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.experimental.categories.Category;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Calendar;

/**
 * The PSProcessTransitionTest class is a test class for the method
 * processTransition. See  {@link #HelpMessage} for command line options.
 */
@Category(IntegrationTest.class)
public class PSProcessTransitionTest extends PSAbstractWorkflowTest 
{

   private static final Logger log = LogManager.getLogger(PSProcessTransitionTest.class);

   /**
    * Constructor specifying command line arguments
    *
    * @param args   command line arguments - see  {@link #HelpMessage}
    *               for options.
    */
   public PSProcessTransitionTest (String[] args)
   {
      m_sArgs = args;
   }

   /* IMPLEMENTATION OF METHODS FROM CLASS PSAbstractWorkflowTest  */   

   public void ExecuteTest(Connection connection)
      throws PSWorkflowTestException
   {
      if (m_bIsVerbose) 
      {
         log.info("\nExecuting test of processTransition\n");
      }
      
      Exception except = null;
      String exceptionMessage = "";
      PSContentStatusContext csc = null;
      PSTransitionsContext tc = null;
      PSRequestContext request  = null;
      boolean transitionPerformed = false;
      PSTransitionsContext tcForAging = null;
      int transitionID = 0;
      Date nextAgingDate = null;
      
      transitionID = m_nTransitionID;
      
      try
      {
         log.info("contentID = {}", m_nContentID);

         // use the option to reset the start in state only if a next
         // transition is specified
         if (0 != transitionID) 
         {
            log.info("initial transitionID = {}", transitionID);
            startInState(m_nContentID,
                         m_nStateID,
                         transitionID,
                         m_nSetBackHours,
                         m_nReminderOffset,
                         m_now,
                         connection);
         }
         
         csc = new PSContentStatusContext(connection, m_nContentID);

         csc.close();
         transitionID = csc.getNextAgingTransition();
         nextAgingDate = csc.getNextAgingDate();
         if (!m_bStartNow) 
         {
             m_now = csc.getLastTransitionDate();
         }
         
         if (m_bIsVerbose) 
         {
            log.info("Initial State:");
            log.info(csc.toString(true));
         }
         log.info("next aging transition = {}", transitionID);
         log.info("polling interval = {}", m_nPollingInterval);
         log.info("Simulated start time is {}", PSWorkFlowUtils.DateString(m_now));
         int count = 0;

         /*
          * Perform a simulated polling loop.
          * Repeat until m_nMaxTransitions transitions have occurred ,
          * incrementing the time by m_nPollingInterval, stopping if the next
          * aging transition is null.
          */
         do
         {
            if (0 == transitionID) 
            {
                PSExitPerformTransition.updateAgingInformation(
                   csc,
                   null,  // no transition context for checkin
                   m_now,
                   request,
                   connection);
                
                csc.commit(connection);
                continue;
            }
            else 
            {
               if (m_bPrintPollingTime && count > 0) 
               {
                  log.info("Simulated time is now {}", PSWorkFlowUtils.DateString(m_now));
               }
               
               // Only perform transition if it is time eligible
               if (PSWorkFlowUtils.timeDiffSecs(m_now,
                                                nextAgingDate) < 0)
               {
                  if (!m_bPrintPollingTime) 
                  {
                     System.out.print("+");
                  }
                  
                  m_now = PSWorkFlowUtils.incrementDate(m_now,
                                                        m_nPollingInterval);
                  csc.setLastTransitionDate(m_now);
                  csc.commit(connection);
                  continue;
               }
               else 
               {
                  if (!m_bPrintPollingTime) 
                  {
                     log.info("");
                  }
               }
               
               tc =  new PSTransitionsContext(transitionID,
                                              m_nWorkflowID, 
                                              connection);
               tc.close();
               
               transitionPerformed = false;

               /**
                * @todo When time add teh logic to perform this test.  This
                * was commented out after the implementation of requirements:
                * 2002.11-01572 and 2002.11-01573 - Workflow Enhancements.
                * The method signature has changed.
                *
                     PSExitPerformTransition.processTransition(csc,
                                                               tc,
                                                               m_sUserName,
                                                               m_now,
                                                               request,
                                                               connection);
                 */
               if (m_bIsVerbose) 
               {
                  log.info("transition {} performed = {}", transitionID, transitionPerformed);
               }
            }
            
            csc = null;
            csc = new PSContentStatusContext(connection, m_nContentID);
            csc.close(); //release the JDBC resources

            if (m_bIsVerbose) 
            {
               log.info(csc.toString(true));
            }
            
            transitionID = csc.getNextAgingTransition();
            log.info("next aging transition = {}", transitionID);
            count++;
            m_now = PSWorkFlowUtils.incrementDate(m_now, m_nPollingInterval);
            nextAgingDate = csc.getNextAgingDate();      
         } while((null != nextAgingDate &&
                  (count < m_nMaxTransitions)));
      } 
      
      catch (SQLException e) 
      {
         exceptionMessage = "SQL exception: ";
         except = e;
      }
      catch (PSEntryNotFoundException e) 
      {
         exceptionMessage = "Entry not found";
         except = e;
      }
      finally 
      {
         if (m_bIsVerbose) 
         {
            log.info("\nEnd test of " +
                               "PSExitPerformTransition.processTransition\n");
         }
         
         if (null != except) 
         {
            throw new PSWorkflowTestException(exceptionMessage,
                                              except);
         }
      }      
   }

   public int GetArgValues(int i)
   {
      
      if (m_sArgs[i].equals("-w") || m_sArgs[i].equals("-workflowid"))
      {
         m_nWorkflowID =  Integer.parseInt(m_sArgs[++i]);
      }
      
      if (m_sArgs[i].equals("-c") || m_sArgs[i].equals("-contentid"))
      {
         m_nContentID =  Integer.parseInt(m_sArgs[++i]);
      }
      
      if (m_sArgs[i].equals("-t") || m_sArgs[i].equals("-transitionid"))
      {
         m_nTransitionID =  Integer.parseInt(m_sArgs[++i]);
      }
      
      if (m_sArgs[i].equals("-s") || m_sArgs[i].equals("-stateid"))
      {
         m_nStateID =  Integer.parseInt(m_sArgs[++i]);
      }
      
      if (m_sArgs[i].equals("-b") || m_sArgs[i].equals("-setbackhours"))
      {
         m_nSetBackHours =  Integer.parseInt(m_sArgs[++i]);
      }
            
      if (m_sArgs[i].equals("-p") || m_sArgs[i].equals("-pollinginterval"))
      {
         m_nPollingInterval =  Integer.parseInt(m_sArgs[++i]);
      }
      
      if (m_sArgs[i].equals("-r") || m_sArgs[i].equals("-reminderoffset"))
      {
         m_nReminderOffset =  Integer.parseInt(m_sArgs[++i]);
      }
      
      if (m_sArgs[i].equals("-m") || m_sArgs[i].equals("-maxtransitions"))
      {
         m_nMaxTransitions =  Integer.parseInt(m_sArgs[++i]);
      }
      
      if (m_sArgs[i].equals("-u") || m_sArgs[i].equals("-username"))
      {
         m_sUserName =  m_sArgs[++i];
      }
      if (m_sArgs[i].equals("-v") || m_sArgs[i].equals("-verbose"))
      {
         m_bIsVerbose = true;
      }
      if (m_sArgs[i].equals("-n") || m_sArgs[i].equals("-now"))
      {
         m_bStartNow = true;
      }
      if (m_sArgs[i].equals("-time"))
      {
         m_bPrintPollingTime = true;
      }
      return i;
   }

   public String HelpMessage()
   {
      StringBuilder buf = new StringBuilder();
      buf.append("Options are:");
      buf.append("   -w, -workflowid        workflow ID");
      buf.append("   -c, -contentid         content ID");
      buf.append("   -t, -transitionid      initial transition ID");
      buf.append("   -s, -stateid           initial state ID");
      buf.append("   -p, -pollinginterval   polling interval");
      buf.append("   -n, -now               start polling at current time");
      buf.append("   -b, -setbackhours      set back in hours");
      buf.append("   -r, -reminderoffset    reminder offset in minutes");
      buf.append("   -m, -maxtransitions    maximum transitions to perform");
      buf.append("   -u, -username          user name for mail sender");
      buf.append("   -v, -verbose           verbose, print content status");
      buf.append("   -time                  print simulated time");
      buf.append("   -h, -help          help");
      return buf.toString();
   }


   /* Helper Method */
      /**
    * Update some state and time status information for a content item.
    * See details below.
    *
    * @param contentID            item content ID          
    * @param stateID              ID of state item should be in      
    * @param initSelfTransition   next aging transition, should be a self
    *                             transition for the specified state. A
    *                             "content start date " transition is a good
    *                             choice.
    * @param setBackHours         number of hours before current time to set
    *                             state entered date, and other initial times.
    * @param m_nReminderOffset    number of hours after the state entered
    *                             date to set the reminder date
    * @param now                  the current time
    * @param connection           database connection
    *
    * The CONTENTSTATUS status table is updated as follows:
    * now = current time
    * start time = current time - setBackHours
    *
    * CONTENTSTATEID                    stateID
    * CONTENTCHECKOUTUSERNAME           "" (empty string)
    * CONTENTSTARTDATE                  start time
    * CONTENTEXPIRYDATE                 now + setBackHours
    * REMINDERDATE                      now + ReminderOffset + setBackHours
    * EDITREVISION                      -1
    * LASTTRANSITIONDATE                start time
    * STATEENTEREDDATE                  start time
    * NEXTAGINGDATE                     now
    * NEXTAGINGTRANSITION               initSelfTransition
    * REPEATEDAGINGTRANSSTARTDATE       now
    *
    * @throws                     SQLException if an SQL error occurs
    * @throws                     PSEntryNotFoundException if an error occurs
    */
   public void startInState(int contentID,
                            int stateID,
                            int initSelfTransition,
                            int setBackHours,
                            int reminderOffset,
                            Date now,
                            Connection connection)
      throws SQLException, PSEntryNotFoundException
   {      
      if (null == connection) 
      {
         throw new IllegalArgumentException("connection may not be null"); 
      }   
      PSContentStatusContext
            csc = new PSContentStatusContext(connection, contentID);
      csc.setContentStateID(stateID);
      csc.setContentCheckedOutUserName("");
      csc.setEditRevision(-1);
      Date startDate = null;
      
      Calendar workingVarCal =  Calendar.getInstance();
      workingVarCal = PSWorkFlowUtils.incrementCalendar(
         workingVarCal,
         now,
         -60 * setBackHours);
      startDate = PSWorkFlowUtils.sqlDateFromCalendar(workingVarCal);
      csc.setLastTransitionDate(startDate);
      csc.setStateEnteredDate(startDate);
      csc.setRepeatedAgingTransitionStartDate(startDate);
      
      csc.setNextAgingDate(now);
      csc.setNextAgingTransition(initSelfTransition);

      csc.setStartDate(startDate);      

      csc.setReminderDate(PSWorkFlowUtils.sqlDateFromCalendar(
         PSWorkFlowUtils.incrementCalendar(workingVarCal,
                                          startDate,
                                          60 * reminderOffset)));

      csc.setExpiryDate(PSWorkFlowUtils.sqlDateFromCalendar(
         PSWorkFlowUtils.incrementCalendar(workingVarCal,
                                           startDate,
                                           60 * (reminderOffset +
                                                 setBackHours))));
      csc.commit(connection);
   }

   public static void main(String[] args)
   {
      PSProcessTransitionTest wfTest = new PSProcessTransitionTest(args);
      wfTest.Test();
   }

   private int m_nWorkflowID = 1;
   private int m_nContentID = 302; 
   private int m_nTransitionID = 0;  
   private int m_nStateID = 1;
   private int m_nSetBackHours = 2;
   private int m_nReminderOffset = 3;
   private String m_sUserName = "Aaron";
   private int m_nMaxTransitions = 1;
   private int m_nPollingInterval = 10;
   private Date m_now = new Date(new java.util.Date().getTime());
   private boolean m_bIsVerbose = false;
   private boolean m_bStartNow = false;
   private boolean m_bPrintPollingTime = false;
}
