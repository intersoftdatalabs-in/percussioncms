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

package com.percussion.pagemanagement.service.impl;

import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.service.exception.PSExtractHTMLException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Servlet that allows to import or export a given template from the system
 * using a XML file. When is a GET request, attempts to export a template and
 * return it as a XML file. When is a POST request, attempts to save the
 * template associated to the site supplied as parameter.
 * 
 * @author leonardohildt
 * 
 */
public class PSTemplateServlet extends HttpServlet  {
	
private static final long serialVersionUID = 1L;
  
private static final int DEFAULT_BUFFER_SIZE = 20480; // 20KB.
	
   public PSTemplateServlet()
   {
	   PSSpringWebApplicationContextUtils.injectDependencies(this);
   }
   
   /**
    * Handles queries for a xml file
    * @throws IOException 
    */
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
       throws ServletException, IOException
   {
       String templateName = "*";
       String templateId = "";
       String pathInfo = req.getPathInfo();
       PSTemplate templateSelected = null;
       
       if(pathInfo != null)
       {
          String[] path = pathInfo.split("/");
          if(path.length > 1)
          {
        	  templateId = path[1];
        	  templateName = path[2];
          }
       }

       try
       {
    	   // Get the selected template
    	   templateSelected = templateService.exportTemplate(templateId, templateName);
    	   // Init servlet response
           resp.reset();
           resp.setBufferSize(DEFAULT_BUFFER_SIZE);
           resp.setContentType("text/xml");
           resp.setHeader("Content-Disposition", "attachment; filename=\"" + templateName + "\"");
           resp.getWriter().write(PSSerializerUtils.marshal(templateSelected));
       }
       catch (Exception ex)
       {
    	   throw new ServletException("Failed to find Template with name = " + templateName, ex);
       }      
   }
   
	  @Override
	  @SuppressWarnings({"unchecked"})
	  protected void doPost(HttpServletRequest request,
	        HttpServletResponse response) throws ServletException, IOException
	  {
		  String siteId = null;
	      
	      //Get the site Id from the path
	      String pathInfo = request.getPathInfo();
	      if(pathInfo != null)
	      {
	         String[] path = pathInfo.split("/");
	         if(path.length > 1)
	         {
	       	  siteId = path[1];
	         }
	      }
	       
	      PSTemplate templateImported = null;
	      boolean isMultipart = ServletFileUpload.isMultipartContent(request);
	      
	      if (isMultipart)
	      {
	    	  try
			  {
			     List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory())
			           .parseRequest(request);
			     for (FileItem item : items)
			     {
			        if (!item.isFormField())
			        {
			           templateImported = importTemplate(siteId, item);
			        }           
			     }
			     // Return the imported template
			     response.getWriter().print(templateImported.getName());
			   }
			   catch (PSExtractHTMLException caE)
			   {
			       handleExtractionError(caE, response);
			   }
			   catch (Exception e)
			   {
			      throw new ServletException("The file you attempted to import is not a CM1 template XML file. Choose a valid CM1 template XML file for upload");
			   }
	      }
	   }
	 
	 /**
	    * Handles extraction errors.
	    * 
	    * @param e the extraction error / exception, assumed not <code>null</code>.
	    * @param response the HTTP response, assumed not <code>null</code>.
	    * 
	    * @throws IOException if there is an error occurs during set error and response on the HTTP response object.
	    */
	   private void handleExtractionError(PSExtractHTMLException e, HttpServletResponse response) throws IOException
	   {
	       String errorMsg = e.getMessage();
	       
	       if (StringUtils.isBlank(errorMsg) && e.getCause() != null)
	       {
	           errorMsg = e.getCause().getMessage();
	       }
	       else if (StringUtils.isNotBlank(errorMsg) && e.getCause() != null)
	       {
	           errorMsg = errorMsg + " The underlying error is: " + e.getCause().getMessage();
	       }
	       log.error(errorMsg);
	       
	       if (log.isDebugEnabled())
	       {
	           if (e.getCause() != null)
	               log.error("Got extraction error.", e.getCause());
	           else
	               log.error("Got extraction error.", e);
	       }
	       response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMsg);
	   }
	 
	   /**
	    * Create the template from the uploaded file.
	    * @param siteId the id of the site, assumed not <code>null</code>.
	    * @param item the file item, assumed not <code>null</code>.  The input stream of this item will be closed by this
	    * method.
	    * <code>false</code> otherwise.
	    * @return the newly created template, never <code>null</code>.
	    * @throws IOException
	    * @throws PSExtractHTMLException if fail to create template due to error on extracting content
	    */
	   private PSTemplate importTemplate(String siteId, FileItem item)
	      throws IOException, PSExtractHTMLException
	   {

	      PSTemplate convertedTemplate = new PSTemplate();

	      try(InputStream fileInput = item.getInputStream()){
	    	  //Build a string with the InputStream
			  try(InputStream is = item.getInputStream()) {
				  try(BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
					  StringBuilder sb = new StringBuilder();
					  String line = null;
					  while ((line = br.readLine()) != null) {
						  sb.append(line + "\n");
					  }
					  br.close();

					  String validStringXml = sb.toString();
					  validStringXml = validStringXml.trim().replaceFirst("^([\\W]+)<", "<");
					  convertedTemplate = PSSerializerUtils.unmarshal(validStringXml, PSTemplate.class);
					  //Import the template
					  PSTemplate importedTemplate = templateService.importTemplate(convertedTemplate, siteId);
					  return importedTemplate;
				  }
			  }
	      }
	      catch (Exception e)
	        {
	           String msg = "The file you attempted to import is not a Page template XML file. Choose a valid CM1 template XML file for upload";
	           Throwable cause = e.getCause();
	           if(cause != null && StringUtils.isNotBlank(cause.getLocalizedMessage()))
	           {
	              msg = cause.getMessage();
	           }
	           else if(StringUtils.isNotBlank(e.getLocalizedMessage()))
	           {
	              msg = e.getMessage();
	           }
	           log.error("Error getting the content from file: {}" ,  msg);
	           return new PSTemplate();
	        }

   }
   
   public IPSTemplateService getTemplateService() {
		return templateService;
	}

	public void setTemplateService(IPSTemplateService templateService) {
		this.templateService = templateService;
	} 

	private IPSTemplateService templateService;
	
	/**
	* The logger
	*/
	private static final Logger log = LogManager.getLogger("PSTemplateInfo");
   
}
