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

package com.percussion.delivery.metadata;

import com.percussion.delivery.metadata.rdfa.IPSDocumentSource;
import com.percussion.delivery.metadata.rdfa.PSReaderDocumentSource;
import com.percussion.delivery.metadata.rdfa.PSTripleHandler;
import com.percussion.delivery.metadata.extractor.data.PSMetadataEntry;
import com.percussion.delivery.metadata.extractor.data.PSMetadataProperty;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.contains;
import static org.apache.commons.lang.StringUtils.endsWith;
import static org.apache.commons.lang.StringUtils.removeStart;
import static org.apache.commons.lang.StringUtils.startsWith;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notEmpty;

/**
 * Responsible for extracting the metadata from a given page and returning a
 * PSMetadataEntry with its properties (PSMetadataProperty).
 * 
 * @author miltonpividori
 * 
 */
public class PSMetadataExtractorService implements IPSMetadataExtractorService
{

    /**
     * Logger for this class.
     */
    public static final Logger log = LogManager.getLogger(PSMetadataExtractorService.class);

    public static final String ALTERNATIVE_PROPERTY_NAME = "dcterms:alternative";

    public static final String TYPE_PROPERTY_NAME = "dterms:type";

    private static final String APPS_SUFFIX = "apps";

    public PSMetadataExtractorService()
    {
       
    }

    /* (non-Javadoc)
     * @see com.percussion.metadata.scanner.impl.IPSMetadataExtractorService#process(java.io.File, java.io.File)
     */
    /*public IPSMetadataEntry process(File tomcatHomeDirectory, File fileToScan)
    {
        log.debug("Extracting metadata from file: " + fileToScan.getPath());

        // Get the pagepath
        String pagePath = PSPagepathUtils.processPath(fileToScan.getAbsolutePath().substring(
                tomcatHomeDirectory.getAbsolutePath().length()));
        
        // Get the site directory
        File siteDirectory = new File(tomcatHomeDirectory, pagePath.split("/")[1]);
        
        // Get folder
        String folder = getEntryFolder(siteDirectory, fileToScan);
        
        // Get site
        String site = getEntrySite(siteDirectory.getName()); 
        
        return runExtraction(new PSFileDocumentSource(fileToScan), pagePath,
                fileToScan.getName(), folder, site);
    }*/
    
    public static class PathSplit {
        private String site;
        private String folder;
        private String pageName;
        
        public PathSplit(String pathToSite, String fullPath) {
            if (pathToSite == null) {
                pathToSite = "/";
            }
            notEmpty(pathToSite);
            notEmpty(fullPath);
            pathToSite = FilenameUtils.separatorsToUnix(pathToSite);
            fullPath = FilenameUtils.separatorsToUnix(fullPath);
            isTrue(startsWith(fullPath, pathToSite), "Path to site should be in full path.");
            if (! endsWith(pathToSite, "/"))
                pathToSite = pathToSite + "/";
            
            pageName = FilenameUtils.getName(fullPath);
            notEmpty(pageName, "filename (page name) is blank");
            String subPath = removeStart(fullPath, pathToSite);
            isTrue(contains(subPath, "/"), "Either site or folder sub folder is missing");
            site = substringBefore(subPath, "/");
            notEmpty(site, "site cannot be resolved");
            folder = "/" + FilenameUtils.getPath(substringAfter(subPath, "/"));
        }
        
        public String getPagePath() {
            return "/" + getSite() + getFolder() + getPageName();
        }
        public String getSite()
        {
            return site;
        }
        public String getFolder()
        {
            return folder;
        }
        public String getPageName()
        {
            return pageName;
        }

        @Override
        public String toString()
        {
            return "PathSplit [site=" + site + ", folder=" + folder + ", pageName=" + pageName + "]";
        }
        
        
    }
    /*
     * (non-Javadoc)
     * @see com.percussion.metadata.extractor.IPSMetadataExtractorService#process(java.io.Reader, java.lang.String)
     */
    public PSMetadataEntry process(Reader reader, String mimeType, String pagePath, Map<String,Object> additional)
    {
        log.debug("Extracting metadata from Reader source");

        PathSplit ps = new PathSplit("/", pagePath);
        PSReaderDocumentSource source = null;
        PSMetadataEntry ret;
        try {
            if (reader == null) {
                //The file isn't going to be handled by the extraction tool so return the additional meta data
                ret = new PSMetadataEntry();
                if (additional != null) {
                    for (String key : additional.keySet()) {
                        ret.addProperty(new PSMetadataProperty(key, additional.get(key).toString()));
                    }
                }
            } else {

                source = new PSReaderDocumentSource(reader, mimeType);
                return runExtraction(source, ps.getPagePath(),
                        ps.getPageName(), ps.getFolder(), ps.getSite(), additional);

            }
            return ret;
        }
        catch (IOException e)
        {
          
            String message = "Error reading from the reader object";
            
            log.error(message, e);
            throw new RuntimeException(message,e);
        }
        finally {
                 if (source!=null)
                    try{source.close();}catch(Exception e){/*Ignore*/}
        }
    }
    
    /**
     * Runs an extraction process. Creates an RDF4J RDFa parser with the specified
     * IPSDocumentSource (using Semargl's RDF4J implementation) and sets to the PSMetadataEntry returned 
     * the pagepath, pagename, folder and site specified.
     * 
     * @param documentSource An IPSDocumentSource to parse.
     * @param pagePath The pagepath of the page.
     * @param pageName The name of the page.
     * @param folder The folder of the page.
     * @param site The site of the page.
     * @param additional 
     * @return A PSMetadataEntry object with the page information along with its metadata
     * properties.
     */
   private PSMetadataEntry runExtraction(IPSDocumentSource documentSource, String pagePath, String pageName,
       String folder, String site, Map<String, Object> additional)
   {
      RDFParser parser = null;
      PSTripleHandler handler = null;
      
      try
      {
         // Create metadata entry
         PSMetadataEntry metadataEntry = new PSMetadataEntry();
         Set<IPSMetadataProperty> propSet = new HashSet<>();


         if (documentSource != null) {

             // Create RDF4J RDFa parser (Semargl implementation)
             parser = Rio.createParser(RDFFormat.RDFA);
             handler = new PSTripleHandler();
             
             // Configure parser settings for lenient parsing
             parser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false);
             parser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, false);
             parser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false);
             parser.getParserConfig().set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false);
             parser.getParserConfig().set(BasicParserSettings.VERIFY_RELATIVE_URIS, false);
             
             parser.setRDFHandler(handler);

             // Parse the document to extract RDF triples (RDFa metadata)
             // Sanitize HTML first to handle malformed entities (e.g., bare '&')
             String baseIri = documentSource.getDocumentIRI();
             String sanitizedHtml;
             try (InputStream inputStream = documentSource.openInputStream()) {
                   Document doc = Jsoup.parse(inputStream, null, "/");
                   // Remove script/style to avoid malformed entities (&) within text nodes breaking XML parsing
                   // But preserve JSON-LD scripts
                   doc.select("script:not([type='application/ld+json']), style").remove();
                 doc.outputSettings()
                        .escapeMode(org.jsoup.nodes.Entities.EscapeMode.base)
                        .charset("UTF-8")
                        .syntax(Document.OutputSettings.Syntax.xml); // XHTML-like output
                 sanitizedHtml = doc.outerHtml();
                 
                 // Regex replace named entities that are NOT XML predefined (lt, gt, amp, apos, quot)
                 // to numeric entities, because the SAX parser used by RDF4J/Semargl doesn't know HTML entities.
                 java.util.regex.Pattern entityPattern = java.util.regex.Pattern.compile("&(?!(?:lt|gt|amp|apos|quot);)([a-zA-Z0-9]+);");
                 java.util.regex.Matcher matcher = entityPattern.matcher(sanitizedHtml);
                 StringBuffer sb = new StringBuffer();
                 while (matcher.find()) {
                     String entityName = matcher.group(1);
                     // Use StringEscapeUtils to resolve the entity to a character
                     String resolved = org.apache.commons.lang.StringEscapeUtils.unescapeHtml("&" + entityName + ";");
                     if (resolved.length() == 1) {
                         // Replace with numeric entity
                         matcher.appendReplacement(sb, "&#" + (int) resolved.charAt(0) + ";");
                     } else {
                         // Fallback: keep as is if resolution fails or returns multiple chars
                         matcher.appendReplacement(sb, matcher.group());
                     }
                 }
                 matcher.appendTail(sb);
                 sanitizedHtml = sb.toString();
             }

             try (java.io.Reader rdr = new java.io.StringReader(sanitizedHtml)) {
                 parser.parse(rdr, baseIri);
             }

             /** Redo Abstract as any23 is corrupting it. */
             try (InputStream is = documentSource.openInputStream()) {
                 Document doc = null;
                 doc = Jsoup.parse(is, null, "/");

                 Element abstractEle = doc.select("div[property=dcterms:abstract]").first();
                 String originalAbstract = null;
                 if (abstractEle != null) {
                     originalAbstract = abstractEle.html();
                 }
                 metadataEntry.setLinktext(handler.getPageLinktext());
                 metadataEntry.setType(handler.getPageType());

                 if (metadataEntry.getType() == null || metadataEntry.getType() == "") {
                     log.warn("The detected type of this item is null or empty.  It is possible that the doctype of the template"
                             + " does not include the required prefix/dcterms."
                             + " The item name is: " + pageName + ". Setting default type to 'page.'");
                     metadataEntry.setType("page");
                 }
                 // Properties
                 for (PSMetadataProperty prop : handler.getProperties()) {
                     if (null != originalAbstract && prop.getName().equals("dcterms:abstract")) {
                         prop.setValue(originalAbstract);
                     }
                     propSet.add(prop);
                 }
             }

             if (additional != null) {
                 for (Entry<String, Object> property : additional.entrySet()) {
                     Object value = property.getValue();
                     if (value != null) {
                         if (property.getKey().equals("linktext")) {
                             metadataEntry.setLinktext(value.toString());
                         } else if (property.getKey().equals("type")) {
                             metadataEntry.setType(value.toString());
                         } else if (value instanceof Collection) {
                             Collection col = (Collection) value;
                             for (Object item : col) {
                                 if (item != null)
                                     propSet.add(new PSMetadataProperty(property.getKey(), item.toString()));
                             }
                         } else if (property.getValue() instanceof Object[]) {
                             Object[] col = (Object[]) value;
                             for (Object item : col) {
                                 if (item != null)
                                     propSet.add(new PSMetadataProperty(property.getKey(), item.toString()));
                             }
                         } else {
                             propSet.add(new PSMetadataProperty(property.getKey(), value
                                     .toString()));
                         }
                     }
                 }
             }
             metadataEntry.setPagepath(pagePath);
             metadataEntry.setName(pageName);

             // Folder
             metadataEntry.setFolder(folder);

             // Site
             metadataEntry.setSite(site);

             metadataEntry.setProperties(propSet);
             logMetadataFields(metadataEntry);

         }
          return metadataEntry;
      }
      catch (Exception ex)
      {
         throw new RuntimeException("Error in extracting metadata from file: ", ex);
      }
      finally
      {
         if (documentSource != null)
            documentSource.close();

         parser = null;
         handler = null;
      }
   }

    /**
     * Creates the 'folder' value for a metadata entry, according to the site
     * directory and file given. Some special cases are considered (for example,
     * the special folder 'ROOT').
     * 
     * @param siteDirectory The site directory of the file.
     * @param fileToScan The file to extract the folder from.
     * @return A 'folder' value ready to be stored in the metadata entry.
     */
    /*private String getEntryFolder(File siteDirectory, File fileToScan)
    {
        // Remove the site path from the file path
        String folderWithFileName = PSPagepathUtils.processPath(
                fileToScan.getAbsolutePath()
                    .substring(siteDirectory.getAbsolutePath().length())
        );
        
        // Remove the ROOT folder from the folder field value
        if (folderWithFileName.startsWith("/ROOT"))
            folderWithFileName = folderWithFileName.substring(5);
        
        // Remove the file name to get the folder
        String folder = folderWithFileName
                    .substring(0, folderWithFileName.length() - fileToScan.getName().length());
        
        if (StringUtils.isEmpty(folder))
            return "/";
        
        return folder;
    }*/
    
    /**
     * Creates the 'site' value for a metadata entry, according to the site
     * directory. It removes the "apps" suffix from the site directory name.
     * 
     * @param siteDirectory A site directory name to extract the 'site'
     * field value from.
     * @return A 'site' value ready to be stored in the metadata entry.
     */
    private String getEntrySite(String siteDirectory)
    {
        if (!siteDirectory.endsWith(APPS_SUFFIX))
            return siteDirectory;

        return siteDirectory.substring(0, siteDirectory.length() - APPS_SUFFIX.length());
    }

    /**
     * Logs all fields of the given metadata entry, along with its properties.
     * 
     * @param metadataEntry A metadata object to log. Should never be
     *            <code>null</code>.
     */
    private void logMetadataFields(PSMetadataEntry metadataEntry)
    {
        if (!log.isTraceEnabled())
            return;
        
        log.trace("Metadata entry info: " +
                new ToStringBuilder(metadataEntry, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("pagepath", metadataEntry.getPagepath())
                    .append("site", metadataEntry.getSite())
                    .append("name", metadataEntry.getName())
                    .append("linktext", metadataEntry.getLinktext())
                    .append("type", metadataEntry.getType())
                    .append("folder", metadataEntry.getFolder())
                    .append("properties", metadataEntry.getProperties())
                    .toString());
    }

    /**
     * Checks if a PSMetadataEntry has the minimum required fields present.
     * 
     * @param metadataEntry A PSMetadataEntry to check. Should never be
     *            <code>null</code>.
     * @return 'true' if the metadata entry has the minimum required fields.
     *         'false' otherwise.
     */
    private boolean metadataEntryHasRequiredFields(IPSMetadataEntry metadataEntry)
    {
        return true;
    }
}
