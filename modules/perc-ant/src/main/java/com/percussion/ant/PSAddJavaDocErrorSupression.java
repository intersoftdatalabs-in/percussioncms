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
package com.percussion.ant;

import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Task to get around a bug that when viewed via eclipse a 
 * javascript error is occurring. This error is not affecting any functionality
 * but is annoying. To supress this error we inject a javascript error handler
 * into the index.html file that will supress all javascript errors.
 */
public class PSAddJavaDocErrorSupression extends Task
{

   private static final Logger log = LogManager.getLogger(PSAddJavaDocErrorSupression.class);
   
   /**
    * The directory where the javadoc is contained
    * @param dir cannot be <code>null</code>.
    */
   public void setDir(File dir)
   {
      m_dir = dir;
   }
   
   /* 
    * @see org.apache.tools.ant.Task#execute()
    */
   @Override
   public void execute()
   {
      if(m_dir == null || !m_dir.isDirectory())
         throw new BuildException("Directory cannot be null and must" +
            " be a valid directory.");
      try
      {
         log.info("Adding error suppression to: {}", new File(m_dir, INDEX_HTML_FILE).getAbsolutePath());
         StringBuilder contents = getContents();
         int idx = indexOfIgnoreCase(contents, "</TITLE>");
         if(idx != -1)
         {
            contents.insert(idx + 8, ERROR_SUPPRESS_STRING);
            saveFile(contents);
         }
      }
      catch (IOException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new BuildException(e);         
      }
   }
   
   /**
    * Helper method to find the index of a string ignoring
    * case.
    * @param sb assumed not <code>null</code>.
    * @param str assumed not <code>null</code>.
    * @return the index position or -1 if not found.
    */
   private int indexOfIgnoreCase(StringBuilder sb, String str)
   {
      String buffer = sb.toString().toLowerCase();
      return buffer.indexOf(str.toLowerCase());
   }
   
   /**
    * Retrieves the contents of the index html file as a
    * <code>StringBuilder</code>.
    * @return never <code>null</code>.
    * @throws IOException on any error.
    */
   private StringBuilder getContents()
      throws IOException
   {
      final StringBuilder sb = new StringBuilder();
      FileInputStream fis = null;
      try
      {
         fis = new FileInputStream(new File(m_dir, INDEX_HTML_FILE));
         int c = 0;
         while((c = fis.read()) != -1)
         {
            sb.append((char)c);            
         }
         return sb;
      }
      finally
      {
         if(fis != null)
         {
           fis.close();
         }
      }
   }
   
   /**
    * Saves the updated content back into the index.html file
    * @param contents assumed not <code>null</code>.
    * @throws IOException upon any error.
    */
   private void saveFile(final StringBuilder contents)
      throws IOException
   {
      FileOutputStream fos = null;
      try
      {
         fos = new FileOutputStream(new File(m_dir, INDEX_HTML_FILE));
         int len = contents.length();
         for(int i = 0; i < len; i++)
         {
            fos.write(contents.charAt(i));
         }
      }
      finally
      {
         if(fos != null)
         {
            fos.flush();
            fos.close();
         }
         
      }
   }
   
   public static void main(String[] args)
   {
      PSAddJavaDocErrorSupression task = new PSAddJavaDocErrorSupression();
      task.setDir(new File("E:\\rxMain\\system\\publicJavaDocs\\docs"));
      task.execute();
   }
   
   /**
    * The directory where the javadoc is contained, set
    * in {@link #setDir(File)}, never <code>null</code> after
    * that.
    */
   private File m_dir;
   
   /**
    * Constant for the name of the index.html file
    */
   private static final String INDEX_HTML_FILE = "index.html";
   
   /**
    * Constant for error suppression string
    */
   private static final String ERROR_SUPPRESS_STRING = 
      "\n<SCRIPT type=\"text/javascript\">\nwindow.onerror = new Function" + "" +
            "(\"return true\");\n</SCRIPT>\n";

}
