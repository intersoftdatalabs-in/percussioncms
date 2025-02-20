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
package com.percussion.server.command;

import com.percussion.error.PSIllegalArgumentException;
import com.percussion.server.IPSServerErrors;
import com.percussion.server.PSApplicationHandler;
import com.percussion.server.PSApplicationStatistics;
import com.percussion.server.PSRemoteConsoleHandler;
import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * The PSConsoleCommandShowStatusApplication class implements processing of the
 * "show status application" console command.
 *
 * @see         PSRemoteConsoleHandler
 *
 * @author      Tas Giakouminakis
 * @version      1.0
 * @since      1.0
 */
public class PSConsoleCommandShowStatusApplication
   extends PSConsoleCommandShowStatusHandler
{
   /**
    * The constructor for this class.
    *
    * @param      cmdArgs      the argument string to use when executing
    *                           this command
    *
    */
   public PSConsoleCommandShowStatusApplication(String cmdArgs)
      throws PSIllegalArgumentException
   {
      super(cmdArgs);

      // need the application name for this command
      if ((cmdArgs == null) || (cmdArgs.length() == 0)) {
         throw new PSIllegalArgumentException(
            IPSServerErrors.RCONSOLE_APP_NAME_REQD, ms_cmdName);
      }
   }

   /**
    * Execute the command specified by this object. The results are returned
    * as an XML document of the appropriate structure for the command.
    *   <P>
    * The execution of this command results in the following XML document
    * structure:
    * <PRE><CODE>
    *      &lt;ELEMENT PSXConsoleCommandResults   (command, resultCode, resultText, PSXApplicationStatus?)&gt;
    *
    *      &lt;--
    *         the command that was executed
    *      --&gt;
    *      &lt;ELEMENT command                     (#PCDATA)&gt;
    *
    *      &lt;--
    *         the result code for the command execution
    *      --&gt;
    *      &lt;ELEMENT resultCode                  (#PCDATA)&gt;
    *
    *      &lt;--
    *         the message text associated with the result code
    *      --&gt;
    *      &lt;ELEMENT resultText                  (#PCDATA)&gt;
    *
    *      &lt;ELEMENT PSXApplicationStatus         (name, PSXStatistics)&gt;
    *
    *      &lt;-- the name of the application --&gt;
    *      &lt;ELEMENT name                        (#PCDATA)&gt;
    * </CODE></PRE>
    *   
    * @param      request                     the requestor object
    *
    * @return                                 the result document
    *
    * @exception   PSConsoleCommandException   if an error occurs during
    *                                          execution
    *
    * @see         PSConsoleCommandShowStatusHandler#createStatistics
    */
   public Document execute(PSRequest request)
      throws PSConsoleCommandException
   {
      Document respDoc = PSXmlDocumentBuilder.createXmlDocument();
      Element root = PSXmlDocumentBuilder.createRoot(
         respDoc, "PSXConsoleCommandResults");
      PSXmlDocumentBuilder.addElement(respDoc, root, "command", ms_cmdName + " " + m_cmdArgs);
      PSXmlDocumentBuilder.addElement(respDoc, root, "resultCode", "0");
      PSXmlDocumentBuilder.addElement(respDoc, root, "resultText", "");

      // not really the root, but using it as such...
      root = PSXmlDocumentBuilder.addEmptyElement(
         respDoc, root, "PSXApplicationStatus");
      PSXmlDocumentBuilder.addElement(respDoc, root, "name", m_cmdArgs);

      PSApplicationHandler ah = PSServer.getApplicationHandler(m_cmdArgs);
      if (ah != null) {
         PSApplicationStatistics stats = ah.getStatistics();
         if (stats != null) {
            createStatistics(stats, respDoc, root);
         }
         else {
            Object[] args = { ms_cmdName, m_cmdArgs };
            throw new PSConsoleCommandException(
               IPSServerErrors.RCONSOLE_APP_STATS_NOT_ENABLED, args);
         }
      }
      else {
         Object[] args = { ms_cmdName, m_cmdArgs };
         throw new PSConsoleCommandException(
            IPSServerErrors.RCONSOLE_APP_NOT_ACTIVE, args);
      }

      return respDoc;
   }


   /**
    * allow package members to see our command name
    */
   final static String   ms_cmdName = "show status application";
}

