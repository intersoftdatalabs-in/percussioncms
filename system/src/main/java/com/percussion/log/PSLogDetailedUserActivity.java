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

package com.percussion.log;


/**
 * The PSLogDetailedUserActivity class is used to log detailed user activity.
 * <p>
 * The following information is logged:
 * <ul>
 * <li>the session id of the user making the request</li>
 * <li>all data submitted with the request (POST body or XML file)</li>
 * <li>request statistics (rows processed, etc.)</li>
 * </ul>
 * All basic user activity logging information is also logged. Enabling
 * detailed logging automatically enables basic logging as well.
 *
 * @author     Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public class PSLogDetailedUserActivity extends PSLogInformation {
   
   
   /**
    * Construct a log message containing the detailed user activity
    * information.
    * <p>
    * The following list contains the supported keys and values:
    * <ul>
    *   <li>sessionId - (subtype 1) - the session id of the user making
    *       this request.</li>
    *   <li>postBody - (subtype 2) the body of the POST request</li>
    *   <li>xmlFile - (subtype 3) the XML file submitted with the request</li>
    *   <li>rowsSelected - (subtype 4) the number of rows selected</li>
    *   <li>rowsInserted - (subtype 5) the number of rows inserted</li>
    *   <li>rowsUpdated - (subtype 6) the number of rows updated</li>
    *   <li>rowsDeleted - (subtype 7) the number of rows deleted</li>
    *   <li>rowsSkipped - (subtype 8) the number of rows skipped</li>
    *   <li>rowsFailed - (subtype 9) the number of rows failed</li>
    *   <li>duration - (subtype 10) the amount of time to process the
    *         request (in milliseconds)</li>
    * </ul>
    *
    * @param   info     the information to be reported
    */
   public PSLogDetailedUserActivity(int applId, java.util.Map info)
   {
      super(LOG_TYPE, applId);

      String sessionId = (String)info.get("sessionId");
      if (sessionId == null)
         sessionId = "";

      String postBody = (String)info.get("postBody");
      if (postBody == null)
         postBody = "";

      String xmlFile = (String)info.get("xmlFile");
      if (xmlFile == null)
         xmlFile = "";

      String rowsSelected = (String)info.get("rowsSelected");
      if (rowsSelected == null)
         rowsSelected = "";

      String rowsInserted = (String)info.get("rowsInserted");
      if (rowsInserted == null)
         rowsInserted = "";

      String rowsUpdated = (String)info.get("rowsUpdated");
      if (rowsUpdated == null)
         rowsUpdated = "";

      String rowsDeleted = (String)info.get("rowsDeleted");
      if (rowsDeleted == null)
         rowsDeleted = "";

      String rowsSkipped = (String)info.get("rowsSkipped");
      if (rowsSkipped == null)
         rowsSkipped = "";

      String rowsFailed = (String)info.get("rowsFailed");
      if (rowsFailed == null)
         rowsFailed = "";

      String duration = (String)info.get("duration");
      if (duration == null)
         duration = "";

      m_subs      = new PSLogSubMessage[10];
      m_subs[0]   = new PSLogSubMessage(1, sessionId);
      m_subs[1]   = new PSLogSubMessage(2, postBody);
      m_subs[2]   = new PSLogSubMessage(3, xmlFile);
      m_subs[3]   = new PSLogSubMessage(4, rowsSelected);
      m_subs[4]   = new PSLogSubMessage(5, rowsInserted);
      m_subs[5]   = new PSLogSubMessage(6, rowsUpdated);
      m_subs[6]   = new PSLogSubMessage(7, rowsDeleted);
      m_subs[7]   = new PSLogSubMessage(8, rowsSkipped);
      m_subs[8]   = new PSLogSubMessage(9, rowsFailed);
      m_subs[9]   = new PSLogSubMessage(10, duration);
   }

   /**
    * Get the sub-messages (type and text). A sub-message is created
    * for each piece of information reported when this object was created.
    * See the
    * {@link #PSLogDetailedUserActivity(int,java.util.Map) constructor}
    * for more details.
    *
    * @return  an array of sub-messages (PSLogSubMessage)
    */
   public PSLogSubMessage[] getSubMessages()
   {
      return m_subs;
   }

   /**
    * Detailed user activity is set as type 8.
    */
   private static final int LOG_TYPE = 8;
   private PSLogSubMessage[] m_subs = null;
}

