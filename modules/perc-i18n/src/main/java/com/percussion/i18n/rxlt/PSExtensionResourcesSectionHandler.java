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

import com.percussion.i18n.tmxdom.IPSTmxDocument;
import com.percussion.i18n.tmxdom.PSTmxDocument;
import com.percussion.util.PSFileFilter;
import com.percussion.util.PSFilteredFileList;
import com.percussion.utils.tools.PSPatternMatcher;
import com.percussion.utils.*;
import com.percussion.xml.PSXmlDocumentBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class is to generate a single TMX Document out of all TMX resource
 * bundles under the directories {@link #SYSRESOURCES_DIR} and
 * {@link #EXTENSIONS_DIR}.
 * <p>
 * Under the first directory we have all the resource keys that are not covered
 * by CMS Tables, Content Editors and XSL Stylesheets, in one or more TMX
 * document(s). This includes all the error messages that support localization
 * from core server and extensions we ship with the product.
 * </p>
 * <p>
 * Present under second directory are the TMX resource bundles containing the
 * keys for the localized error messages for the Java extensions. These
 * extensions are typically written by the customers and the resource bundle
 * files are uploaded to the server along with class files implementing the
 * extensions.
 * </p>
 */

public class PSExtensionResourcesSectionHandler extends PSIdleDotter
   implements IPSSectionHandler
{
   /*
    * Implementation of the method defined in the interface.
    * See {@link IPSSectionHandler#process(Element)} for
    * details about this method.
    */
   public IPSTmxDocument process(Element cfgData)
      throws IllegalArgumentException, PSActionProcessingException

   {
      if(cfgData == null)
      {
         throw new IllegalArgumentException("cfgdata must not be null");
      }
      String rxroot = cfgData.getOwnerDocument().getDocumentElement()
            .getAttribute(PSRxltConfigUtils.ATTR_RXROOT);
      ms_SectionName = cfgData.getAttribute(PSRxltConfigUtils.ATTR_NAME);

      PSCommandLineProcessor.logMessage("blankLine", "");
      PSCommandLineProcessor.logMessage("processingSection", ms_SectionName);
      PSCommandLineProcessor.logMessage("blankLine", "");

      IPSTmxDocument tmxDoc = null;
      try
      {
         PSCommandLineProcessor.logMessage("retrievingExtensionTMXFiles", "");
         //show idle dots
         showDots(true);
         PSPatternMatcher pattern = new PSPatternMatcher('?', '*', "*.tmx");
         PSFileFilter filter = new PSFileFilter(PSFileFilter.IS_FILE);
         filter.setNamePattern(pattern);
         PSFilteredFileList  lister = new PSFilteredFileList(filter);
         List listFiles = lister.getFiles(new File(rxroot, SYSRESOURCES_DIR));
         //show idle dots
         showDots(false);

         filter = new PSFileFilter(
            PSFileFilter.IS_FILE | PSFileFilter.IS_INCLUDE_ALL_DIRECTORIES);
         filter.setNamePattern(pattern);
         lister = new PSFilteredFileList(filter);
         List listFilesExt = lister.getFiles(new File(rxroot, EXTENSIONS_DIR));

         listFiles.addAll(listFilesExt);
         File file = null;
         Document doc = null;
         tmxDoc = new PSTmxDocument();
         IPSTmxDocument tmxDocTemp = null;
         for(int i=0; listFiles!=null && i<listFiles.size(); i++)
         {
            try
            {
               file = (File)listFiles.get(i);
               PSCommandLineProcessor.logMessage("processingFile",
                  file.getCanonicalPath());
               try(InputStreamReader ir = new InputStreamReader(
                       new FileInputStream(file), StandardCharsets.UTF_8) ) {
                  doc = PSXmlDocumentBuilder.createXmlDocument(ir
                          , false);
                  tmxDocTemp = new PSTmxDocument(doc);
                  tmxDoc.merge(tmxDocTemp);
               }
            }
            catch(Exception e)
            {
               PSCommandLineProcessor.logMessage("errorProcessingFile "+ file.getAbsolutePath(),
                  e.toString());
            }
         }
      }
      catch(Exception e) //catch any exception
      {
         throw new PSSectionProcessingException(e.getMessage());
      }
      finally
      {
         endDotSession();
      }
      return tmxDoc;
   }

   /**
    * Default name of section that is implemented by this class. Overridden
    * during processing by the name specified in the config element.
    * @see #process
    */
   private static String ms_SectionName = "Extension Resources";

   /**
    * String constant defining the location of the directory where all TMX
    * resource system files are available (relative to the Rhythmyx root
    * directory);
    */
   private static final String SYSRESOURCES_DIR =
      "sys_resources" + File.separator + "i18n";

   /**
    * String constant defining the location of the directory underwhich all TMX
    * resource files for customer specific extensions are available (relative
    * to the Rhythmyx root directory);
    */
   private static final String EXTENSIONS_DIR =
      "Extensions" + File.separator + "Handlers" + File.separator + "Java";
}
