/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.sitemanage.service;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import com.percussion.server.PSServer;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.service.impl.PSSiteDataService;
import com.percussion.test.PSServletTestCase;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.mock.web.MockHttpServletRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.getWebApplicationContext;

/**
 * Tool to create sites from URLs from a file at a known location.
 * // REFACTORED: CP-JAVA11
 */
public class PSSiteSuckerTest extends PSServletTestCase {

    private static final Logger log = LogManager.getLogger(PSSiteSuckerTest.class);

    public void testSiteSucker() {
        try {
            com.percussion.webservices.security.PSSecurityWsLocator.getSecurityWebservice()
                .login(request, response, "Admin", "demo", null, null, null);

            var logEntries = new ArrayList<PSSiteImportLogEntry>();
            List<String> urlsList = null;
            var wsUrlFile = new File(WEBSITE_URLS_FILE);
            if (!wsUrlFile.exists() || !wsUrlFile.isFile()) {
                log.warn("Website URLs file not found: {}", WEBSITE_URLS_FILE);
                return;
            }
            try {
                urlsList = parseSiteUrls(wsUrlFile);
            } catch (Exception e) {
                log.error("Failed to parse site URLs file: {}", e.getMessage());
            }
            if (urlsList != null) {
                for (var url : urlsList) {
                    try {
                        var site = createSiteFromURL(url);
                        logEntries.add(createLogEntry(site));
                    } catch (Exception e) {
                        logEntries.add(createLogEntry(url, e.getMessage()));
                    }
                }
            }
            generateLogFile(logEntries);
        } catch (Exception e) {
            log.error("Site sucker failed: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    private void generateLogFile(List<PSSiteImportLogEntry> logEntries) {
        try {
            var outputFileName = new File(REPORT_HTML_FILE);
            var xsltFile = new File(ENTRIES_TEMPLATE_FILE);

            var docFactory = PSSecureXMLUtils.getSecuredDocumentBuilderFactory(
                    new PSXmlSecurityOptions(true, true, true, false, true, false));
            var docBuilder = docFactory.newDocumentBuilder();

            var doc = docBuilder.newDocument();
            var rootElement = doc.createElement("entries");
            doc.appendChild(rootElement);

            for (var entry : logEntries) {
                var entryElem = doc.createElement("entry");
                rootElement.appendChild(entryElem);

                var siteUrl = doc.createElement("siteUrl");
                siteUrl.appendChild(doc.createTextNode(entry.siteUrl));
                entryElem.appendChild(siteUrl);

                var importError = doc.createElement("importError");
                importError.appendChild(doc.createTextNode(entry.importError));
                entryElem.appendChild(importError);

                var errorLog = doc.createElement("logUrl");
                errorLog.appendChild(doc.createTextNode(entry.logUrl));
                entryElem.appendChild(errorLog);

                var previewLink = doc.createElement("previewPageUrl");
                previewLink.appendChild(doc.createTextNode(entry.previewPageUrl));
                entryElem.appendChild(previewLink);

                var siteName = doc.createElement("importedSiteName");
                siteName.appendChild(doc.createTextNode(entry.importedSiteName));
                entryElem.appendChild(siteName);

                var remarks = doc.createElement("remarks");
                remarks.appendChild(doc.createTextNode(" "));
                entryElem.appendChild(remarks);
            }

            var transformerFactory = TransformerFactory.newInstance();
            var transformer = transformerFactory.newTransformer();
            var source = new DOMSource(doc);
            var result = new StreamResult(new File(ENTRIES_XML_FILE));
            transformer.transform(source, result);

            var xmlFile = new File(ENTRIES_XML_FILE);
            try (var htmlFile = new FileOutputStream(outputFileName)) {
                htmlFile.write(XmlTransform.getTransformedHtml(xmlFile, xsltFile).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    /**
     * Loads the supplied file and adds all the URLs to the list and returns the list.
     */
    private List<String> parseSiteUrls(File wsUrlFile) throws FileNotFoundException {
        var urlList = new ArrayList<String>();
        try (var scanner = new Scanner(new FileInputStream(wsUrlFile), StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                var urlLine = scanner.nextLine();
                if (StringUtils.isNotBlank(urlLine)) {
                    var urls = urlLine.split(",");
                    for (var url : urls) {
                        if (StringUtils.isNotBlank(url)) {
                            urlList.add(url.trim());
                        }
                    }
                }
            }
        }
        return urlList;
    }

    /**
     * Creates a site from the supplied URL. Uses the site host name as the name of the site.
     */
    private PSSite createSiteFromURL(String siteUrl)
            throws MalformedURLException, PSSiteImportException, PSValidationException {
        var siteUrlToImport = siteUrl;
        if (!siteUrl.startsWith("http://") && !siteUrl.startsWith("https://")) {
            siteUrlToImport = "http://" + siteUrl;
        }
        var url = new URL(siteUrlToImport);
        var pssite = new PSSite();
        pssite.setName(url.getHost());
        pssite.setBaseUrl(siteUrlToImport);

        var request = new MockHttpServletRequest();
        request.addParameter("User-Agent", USER_AGENT);

        return getSiteDataService().createSiteFromUrl(request, pssite);
    }

    private PSSiteImportLogEntry createLogEntry(PSSite site) {
        var logEntry = new PSSiteImportLogEntry();
        logEntry.siteUrl = site.getBaseUrl();
        logEntry.importError = "Yes";
        logEntry.logUrl = getLogUrl(site.getName());
        logEntry.previewPageUrl = getPreviewUrl(site.getName());
        logEntry.importedSiteName = site.getName();
        return logEntry;
    }

    private PSSiteImportLogEntry createLogEntry(String url, String errorMsg) {
        var logEntry = new PSSiteImportLogEntry();
        logEntry.siteUrl = url;
        logEntry.importError = "No - (see details: " + errorMsg + ")";
        return logEntry;
    }

    private String getPreviewUrl(String siteName) {
        return "http://" + PSServer.getHostName() + ":" + PSServer.getListenerPort() + "/Sites/" + siteName + "/index.html";
    }

    private String getLogUrl(String siteName) {
        return "http://" + PSServer.getHostName() + ":" + PSServer.getListenerPort() +
                "/Rhythmyx/services/sitemanage/site/importLogViewer?siteName=" + siteName;
    }

    private PSSiteDataService getSiteDataService() {
        return (PSSiteDataService) getWebApplicationContext().getBean("siteDataService");
    }

    static class PSSiteImportLogEntry {
        public String siteUrl;
        public String importError;
        public String logUrl;
        public String previewPageUrl;
        public String importedSiteName;
    }

    static class XmlTransform {
        public static String getTransformedHtml(File xmlFile, File xsltFile) throws TransformerException {
            var xml = getStringFromFile(xmlFile).getBytes(StandardCharsets.UTF_8);
            var xsl = getStringFromFile(xsltFile).getBytes(StandardCharsets.UTF_8);
            return getTransformedHtml(xml, xsl);
        }

        public static String getTransformedHtml(byte[] xml, byte[] xsl) throws TransformerException {
            Source srcXml = new StreamSource(new ByteArrayInputStream(xml));
            Source srcXsl = new StreamSource(new ByteArrayInputStream(xsl));
            var writer = new StringWriter();
            Result result = new StreamResult(writer);
            var tFactory = TransformerFactory.newInstance();
            var transformer = tFactory.newTransformer(srcXsl);
            transformer.transform(srcXml, result);
            return writer.toString();
        }

        private static String getStringFromFile(File f) {
            var sb = new StringBuilder(1000);
            try (var sc = new Scanner(f, StandardCharsets.UTF_8)) {
                while (sc.hasNextLine()) {
                    sb.append(sc.nextLine());
                }
            } catch (FileNotFoundException e) {
                log.error(PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
            return sb.toString();
        }
    }

    private static final String WEBSITE_URLS_FILE = PSServer.getRxDir() + "/SiteImporter/websiteurls.csv";
    private static final String ENTRIES_XML_FILE = PSServer.getRxDir() + "/SiteImporter/logEntries.xml";
    private static final String ENTRIES_TEMPLATE_FILE = PSServer.getRxDir() + "/SiteImporter/logEntries.xsl";
    private static final String REPORT_HTML_FILE = PSServer.getRxDir() + "/SiteImporter/SiteSuckerReport.html";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:12.0) Gecko/20100101 Firefox/12.0";
}
