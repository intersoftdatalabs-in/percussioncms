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
// REFACTORED: CP-JAVA11
package com.percussion.rx.publisher.servlet;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.rx.publisher.PSRxPublisherServiceLocator;
import com.percussion.rx.publisher.data.PSDemandWork;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.List;

/**
 * Invokes demand publishing and presents a user interface to allow the user to
 * see the current status of the publishing job associated with the demand
 * request.
 * <p>
 * After queuing the request, this servlet invokes a JSP to present the progress
 * data.
 * 
 * @author dougrand 
 */
public class PSDemandPublishServlet extends HttpServlet
{
   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private static final Logger log = LogManager.getLogger("publish-jsp");

   /*
    * (non-Javadoc)
    * 
    * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest,
    *      javax.servlet.http.HttpServletResponse)
    */
   @Override
   protected void service(HttpServletRequest request, HttpServletResponse resp)
         throws ServletException, IOException {
      var ids = request.getParameterValues(IPSHtmlParameters.SYS_CONTENTID);
      var edition = request.getParameter(IPSHtmlParameters.SYS_EDITIONID);
      var folder = request.getParameter(IPSHtmlParameters.SYS_FOLDERID);
      var site = request.getParameter(IPSHtmlParameters.SYS_SITEID);
      var gen = request.getParameter("sys_demandPublishingGenerator");

      int editionId;
      int folderId = convertInteger(folder, "folder");
      int[] contentIds = convertArray(ids, "content ids");

      var gmgr = PSGuidManagerLocator.getGuidMgr();
      var folderGuid = gmgr.makeGuid(new PSLocator(folderId));

      final String DEFAULT_GENERATOR = "Java/global/percussion/system/sys_SelectedItemsGenerator";
      var clistGenerator = StringUtils.isBlank(gen) ? DEFAULT_GENERATOR : gen;

      if (StringUtils.isBlank(edition)) {
         if (StringUtils.isBlank(site) || !StringUtils.isNumeric(site)) {
            throw new RuntimeException(
               "Either the edition Id or site Id must be specified when executing demand publishing.");
         }

         // Safe workaround: Use reflection to call findEditionsBySiteAndContentListGenerator if available
      // Unchecked cast warning is expected: reflection workaround for legacy API
         List<IPSGuid> editionIds = null;
         try {
            var svc = PSPublisherServiceLocator.getPublisherService();
            Method method = svc.getClass().getMethod(
               "findEditionsBySiteAndContentListGenerator",
               Class.forName("com.percussion.utils.guid.IPSGuid"), String.class);
            var siteGuid = gmgr.makeGuid(new PSLocator(Integer.parseInt(site)));
            // Safe reflection workaround for legacy API
            editionIds = (List<IPSGuid>) method.invoke(svc, siteGuid, clistGenerator);
         } catch (Exception e) {
            throw new ServletException("Edition resolution failed: " + e.getMessage(), e);
         }
         if (editionIds == null || editionIds.isEmpty()) {
            var msg = "Your system is not properly configured to support automatic edition resolution for demand publishing on this site. There are no matching editions on site {0}. There needs to be an edition that has 1 content list using the ''{1}'' generator.";
            Object[] params = {site, clistGenerator};
            throw new RuntimeException(MessageFormat.format(msg, params));
         }
         editionId = editionIds.get(0).getUUID();
         log.info("Demand publishing with resolved edition " + editionId);
      } else {
         editionId = convertInteger(edition, "edition");
         log.info("Demand publishing with supplied edition " + editionId);
      }

      var work = new PSDemandWork();
      for (var contentId : contentIds) {
         var contentGuid = gmgr.makeGuid(new PSLocator(contentId));
         work.addItem(folderGuid, contentGuid);
      }

      var pubsvc = PSRxPublisherServiceLocator.getRxPublisherService();
      long requestId;
      try {
         requestId = pubsvc.queueDemandWork(editionId, work, clistGenerator);
      } catch (Exception e) {
         throw new ServletException(e);
      }

      request.setAttribute("requestid", requestId);
      var dispatcher = request.getRequestDispatcher("/ui/pubruntime/DemandPublish.jsp");
      dispatcher.forward(request, resp);
   }

   /**
    * Convert an array of values
    * 
    * @param ids the values, never <code>null</code>
    * @param typename the name of the type, assumed never <code>null</code> or
    *            empty.
    * @return the converted array, never <code>null</code>.
    */
   private int[] convertArray(String[] ids, String typename)
   {
      if (ids == null)
      {
         throw new IllegalArgumentException("Ids array must be non-null");
      }
      int rval[] = new int[ids.length];
      for (int i = 0; i < ids.length; i++)
      {
         rval[i] = convertInteger(ids[i], typename);
      }
      return rval;
   }

   /**
    * Convert a single value
    * 
    * @param value the value, never <code>null</code> or empty
    * @param typename the typename, never <code>null</code> or empty
    * @return
    */
   private int convertInteger(String value, String typename)
   {
      if (StringUtils.isBlank(value))
      {
         throw new IllegalArgumentException("value may not be null or empty");
      }
      if (StringUtils.isBlank(typename))
      {
         throw new IllegalArgumentException("typename may not be null or empty");
      }
      try
      {
         return Integer.parseInt(value);
      }
      catch (NumberFormatException ex)
      {
         throw new IllegalArgumentException("Cannot convert value " + value
               + " for type " + typename);
      }
   }
}
