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
import com.percussion.server.PSHandlerStatistics;
import com.percussion.server.PSRemoteConsoleHandler;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Date;


/**
 * PSConsoleCommandShowStatusHandler is the base class for all classes
 * reporting statistics for request handlers through the "show status"
 * console command.
 *
 * @see         PSRemoteConsoleHandler
 *
 * @author      Tas Giakouminakis
 * @version      1.0
 * @since      1.0
 */
public abstract class PSConsoleCommandShowStatusHandler
   extends PSConsoleCommand
{
   /**
    * The constructor for this class.
    *
    * @param      cmdArgs      the argument string to use when executing
    *                           this command
    *
    */
   protected PSConsoleCommandShowStatusHandler(String cmdArgs)
      throws PSIllegalArgumentException
   {
      super(cmdArgs);
   }

   /**
    * Create the statistics tree as a child of the specified XML node.
    *   <P>
    * The statistics are structured as follows:
    * <PRE><CODE>
    *      &lt;ELEMENT PSXStatistics               (ElapsedTime, Counters, Timers)&gt;
    *
    *      &lt;--
    *         the time the application's been running
    *      --&gt;
    *      &lt;ELEMENT ElapsedTime                  (days, hours, minutes, seconds, milliseconds)&gt;
    *
    *      &lt;-- the number of days --&gt;
    *      &lt;ELEMENT days                        (#PCDATA)&gt;
    *
    *      &lt;-- the number of hours --&gt;
    *      &lt;ELEMENT hours                        (#PCDATA)&gt;
    *
    *      &lt;-- the number of minutes --&gt;
    *      &lt;ELEMENT minutes                     (#PCDATA)&gt;
    *
    *      &lt;-- the number of seconds --&gt;
    *      &lt;ELEMENT seconds                     (#PCDATA)&gt;
    *
    *      &lt;-- the number of milliseconds --&gt;
    *      &lt;ELEMENT milliseconds               (#PCDATA)&gt;
    *
    *      &lt;ELEMENT Counters                     (eventsProcessed, eventsFailed, eventsPending, cacheHits, cacheMisses)&gt;
    *
    *      &lt;-- the number of events successfully processed --&gt;
    *      &lt;ELEMENT eventsProcessed            (#PCDATA)&gt;
    *
    *      &lt;-- the number of events that failed during processing --&gt;
    *      &lt;ELEMENT eventsFailed               (#PCDATA)&gt;
    *
    *      &lt;-- the number of events pending (waiting to be processed) --&gt;
    *      &lt;ELEMENT eventsPending               (#PCDATA)&gt;
    *
    *      &lt;-- the number of data requests processed from cache --&gt;
    *      &lt;ELEMENT cacheHits                  (#PCDATA)&gt;
    *
    *      &lt;-- the number of data requests submitted to the back-end --&gt;
    *      &lt;ELEMENT cacheMisses                  (#PCDATA)&gt;
    *
    *      &lt;ELEMENT Timers                     (EventAverage, EventMinimum, EventMaximum)&gt;
    *
    *      &lt;-- the average time spent processing an event --&gt;
    *      &lt;ELEMENT EventAverage               (days, hours, minutes, seconds, milliseconds)&gt;
    *
    *      &lt;-- the minimum time spent processing an event --&gt;
    *      &lt;ELEMENT EventMinimum               (days, hours, minutes, seconds, milliseconds)&gt;
    *
    *      &lt;-- the maximum time spent processing an event --&gt;
    *      &lt;ELEMENT EventMaximum               (days, hours, minutes, seconds, milliseconds)&gt;
    * </CODE></PRE>
    *   
    * @param      request                     the requestor object
    *
    * @return                                 the result document
    *
    */
   public void createStatistics(
      PSHandlerStatistics stats, Document doc, Element root)
   {
      if (stats != null) {
         Element node = PSXmlDocumentBuilder.addEmptyElement(
            doc, root, "PSXStatistics");

         node = PSXmlDocumentBuilder.addEmptyElement(
            doc, root, "ElapsedTime");
         long timer = (new Date().getTime()) - 
                              stats.getStartTime().getTime();
         addTimeElement(doc, node, timer);

         node = PSXmlDocumentBuilder.addEmptyElement(
            doc, root, "Counters");
         PSXmlDocumentBuilder.addElement( doc, node, "eventsProcessed",
            String.valueOf(stats.getSuccessfulEventCount()));
         PSXmlDocumentBuilder.addElement( doc, node, "eventsFailed",
            String.valueOf(stats.getFailedEventCount()));
         PSXmlDocumentBuilder.addElement( doc, node, "eventsPending",
            String.valueOf(stats.getPendingEventCount()));
         PSXmlDocumentBuilder.addElement( doc, node, "cacheHits",
            String.valueOf(stats.getCacheHits()));
         PSXmlDocumentBuilder.addElement( doc, node, "cacheMisses",
            String.valueOf(stats.getCacheMisses()));

         node = PSXmlDocumentBuilder.addEmptyElement(
            doc, root, "Timers");
         addTimeElement(doc,
            PSXmlDocumentBuilder.addEmptyElement(doc, node, "EventAverage"),
            stats.getAverageEventTime());
         addTimeElement(doc,
            PSXmlDocumentBuilder.addEmptyElement(doc, node, "EventMinimum"),
            stats.getMinimumEventTime());
         addTimeElement(doc,
            PSXmlDocumentBuilder.addEmptyElement(doc, node, "EventMaximum"),
            stats.getMaximumEventTime());
      }
   }

   private void addTimeElement(Document doc, Element node, int timer)
   {
      long lTimer;

      if ((timer == Integer.MIN_VALUE) || (timer == Integer.MAX_VALUE))
         lTimer = 0;
      else
         lTimer = (long)timer;
         
      addTimeElement(doc, node, lTimer);
   }

   private void addTimeElement(Document doc, Element node, long timer)
   {
      PSXmlDocumentBuilder.addElement( doc, node, "days",
         String.valueOf(timer / MILLIS_IN_DAY));
      timer = timer % MILLIS_IN_DAY;
      PSXmlDocumentBuilder.addElement( doc, node, "hours",
         String.valueOf(timer / MILLIS_IN_HOUR));
      timer = timer % MILLIS_IN_HOUR;
      PSXmlDocumentBuilder.addElement( doc, node, "minutes",
         String.valueOf(timer / MILLIS_IN_MIN));
      timer = timer % MILLIS_IN_MIN;
      PSXmlDocumentBuilder.addElement( doc, node, "seconds",
         String.valueOf(timer / MILLIS_IN_SEC));
      timer = timer % MILLIS_IN_SEC;
      PSXmlDocumentBuilder.addElement( doc, node, "milliseconds",
         String.valueOf(timer));
   }


   private static final long MILLIS_IN_SEC   = 1000;
   private static final long MILLIS_IN_MIN   = MILLIS_IN_SEC * 60;
   private static final long MILLIS_IN_HOUR   = MILLIS_IN_MIN * 60;
   private static final long MILLIS_IN_DAY   = MILLIS_IN_HOUR * 24;

}

