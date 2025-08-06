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
// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.importer.theme;

import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.PSSiteImportLogger;
import com.percussion.util.PSPurgableTempFile;

import com.percussion.utils.types.PSPair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.apache.commons.lang.Validate.notNull;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PSCSSParser.
 * @author Ignacio Erro, Sunny Sal (refactored)
 */
@Tag("IntegrationTest")
class PSCSSParserTest {

    private static final String BASE_URL = "http://generic:8080";
    private static final String THEME_NAME = "ownTheme";
    private static final String SITE_NAME = "generic";
    private static final String THEME_PATH = "/web_resources/themes/" + THEME_NAME;
    private static final String SAMPLE_UNCOMPRESSED_FILE_BACKUP = "src/test/resources/importer/sample_backup.css";
    private static final String SAMPLE_COMPRESSED_FILE_BACKUP = "src/test/resources/importer/sample-min_backup.css";
    private static final String IMPORT_FILE = "src/test/resources/importer";
    private static final String IMPORT_A = "/importA.css";
    private static final String IMPORT_B = "/importB.css";
    private static final String IMPORT_C = "/importC.cfm";

    private String absoluteThemePath;
    private IPSSiteImportLogger logger;
    private PSCSSParser parser;
    private PSPurgableTempFile tempCSSUncompressedFile;
    private PSPurgableTempFile tempCSSCompressedFile;
    private Downloader downloader;

    /**
     * Test file downloader that copies files locally instead of downloading.
     */
    private class Downloader implements IPSFileDownloader {
        @Override
        public List<PSPair<Boolean, String>> downloadFiles(Map<String, String> urlToPathMap, PSSiteImportCtx context, boolean createAsset) {
            notNull(urlToPathMap);
            notNull(context);
            notNull(context.getLogger());
            var downloaded = new ArrayList<PSPair<Boolean, String>>();
            for (var url : urlToPathMap.keySet()) {
                var fileName = getFileName(url);
                var filePath = getFilePath(fileName);
                downloaded.add(downloadFile(filePath, urlToPathMap.get(url).replace(fileName, "")));
            }
            return downloaded;
        }

        @Override
        public PSPair<Boolean, String> downloadFile(String url, String destination) {
            notNull(url);
            notNull(destination);
            notNull(logger);
            var fileName = getFileName(url);
            var filePath = getFilePath(fileName);
            var destFile = new File(destination);
            try {
                var in = new File(filePath);
                var dest = new File(destFile.getParent());
                FileUtils.copyFileToDirectory(in, dest);
                FileUtils.moveFile(new File(dest + fileName), destFile);
                return new PSPair<>(true, "Success");
            } catch (Exception e) {
                return new PSPair<>(false, "Error");
            }
        }
    }

    @BeforeEach
    void init() {
        try {
            tempCSSUncompressedFile = new PSPurgableTempFile("TempUncompressed", ".css", null);
            tempCSSCompressedFile = new PSPurgableTempFile("TempCompressed", ".css", null);
            absoluteThemePath = tempCSSUncompressedFile.getParent() + "/ImportedTheme/" + THEME_NAME;
            logger = new PSSiteImportLogger(PSLogObjectType.SITE);
            parser = new PSCSSParser(SITE_NAME, absoluteThemePath, THEME_PATH, logger);
            downloader = new Downloader();

            // Copy sample files to temp files
            var uncompressed = loadFileFromDisk(SAMPLE_UNCOMPRESSED_FILE_BACKUP);
            saveFile(new StringBuilder(uncompressed), tempCSSUncompressedFile.getAbsolutePath());
            var compressed = loadFileFromDisk(SAMPLE_COMPRESSED_FILE_BACKUP);
            saveFile(new StringBuilder(compressed), tempCSSCompressedFile.getAbsolutePath());
        } catch (Exception e) {
            fail("Error copying sample file.");
        }
    }

    @Test
    void testParserCSSInline() {
        parser.setFileDownloader(downloader);
        var cssInline = loadFileFromDisk(tempCSSUncompressedFile.getAbsolutePath());
        var results = parser.parse(BASE_URL, cssInline);
        var imagesToDownload = results.getFirst();
        var parsedCSS = results.getSecond();
        assertUrls(parsedCSS);
        assertImages(imagesToDownload);
    }

    @Test
    void testParserUnCompressedCSS() {
        parser.setFileDownloader(downloader);
        var cssFiles = new HashMap<String, String>();
        cssFiles.put(BASE_URL, tempCSSUncompressedFile.getAbsolutePath());
        var imagesToDownload = parser.parse(cssFiles);
        var cssFile = loadFileFromDisk(tempCSSUncompressedFile.getAbsolutePath());
        assertFileChanged(SAMPLE_UNCOMPRESSED_FILE_BACKUP, tempCSSUncompressedFile.getAbsolutePath());
        assertKnownChanges(SAMPLE_UNCOMPRESSED_FILE_BACKUP, tempCSSUncompressedFile.getAbsolutePath());
        assertUrls(cssFile);
        assertImages(imagesToDownload);
    }

    @Test
    void testParserCompressedCSS() {
        parser.setFileDownloader(downloader);
        var cssFiles = new HashMap<String, String>();
        cssFiles.put(BASE_URL, tempCSSCompressedFile.getAbsolutePath());
        var imagesToDownload = parser.parse(cssFiles);
        var cssFile = loadFileFromDisk(tempCSSCompressedFile.getAbsolutePath());
        assertFileChanged(SAMPLE_COMPRESSED_FILE_BACKUP, tempCSSCompressedFile.getAbsolutePath());
        assertKnownChanges(SAMPLE_COMPRESSED_FILE_BACKUP, tempCSSCompressedFile.getAbsolutePath());
        assertUrls(cssFile);
        assertImages(imagesToDownload);
    }

    @Test
    void testParseImports() {
        parser.setFileDownloader(downloader);
        var cssFiles = new HashMap<String, String>();
        cssFiles.put(BASE_URL, tempCSSUncompressedFile.getAbsolutePath());
        var imagesToDownload = parser.parse(cssFiles);

        String cssImportA = "";
        String cssImportB = "";
        String cssImportC = "";
        try {
            cssImportA = loadFileFromDisk(absoluteThemePath + "/import/generic" + IMPORT_A);
            cssImportB = loadFileFromDisk(absoluteThemePath + "/import/generic" + IMPORT_B);
            cssImportC = loadFileFromDisk(absoluteThemePath + "/import/generic" + IMPORT_C + ".css");
        } catch (Exception e) {
            fail("Error reading files.");
        }

        assertFalse(cssImportA.isEmpty());
        assertFalse(cssImportB.isEmpty());
        assertFalse(cssImportC.isEmpty());
        assertEquals(7, imagesToDownload.size());

        assertTrue(cssImportA.contains("@import \"/web_resources/themes/ownTheme/import/generic/importB.css\";"));
        assertTrue(cssImportA.contains("url(/web_resources/themes/ownTheme/import/generic/images/buttonOk.png)"));

        assertTrue(cssImportB.contains("@import \"/web_resources/themes/ownTheme/import/generic/importC.cfm.css\";"));
        assertTrue(cssImportB.contains("@import \"/web_resources/themes/ownTheme/import/generic/importA.css\";"));
        assertTrue(cssImportB.contains("url(http://www.percussion.com/images/images/buttonOk.png);"));

        assertTrue(cssImportC.contains("url(/web_resources/themes/ownTheme/import/generic/images/buttonCancel.png)"));
    }

    @AfterEach
    void tearDown() {
        tempCSSUncompressedFile.delete();
        tempCSSCompressedFile.delete();
        try {
            FileUtils.forceDelete(new File(absoluteThemePath));
        } catch (Exception e) {
            System.out.println("Error deleting temp files.");
        }
    }

    private void assertFileChanged(String sampleFilePath, String modifiedFilePath) {
        var sampleFile = loadFileFromDisk(sampleFilePath);
        var modifiedFile = loadFileFromDisk(modifiedFilePath);
        assertNotEquals(sampleFile, modifiedFile);
    }

    private void assertKnownChanges(String sampleFilePath, String modifiedFilePath) {
        var sampleFile = loadFileFromDisk(sampleFilePath);
        var modifiedFile = loadFileFromDisk(modifiedFilePath);

        modifiedFile = modifiedFile.replace("/web_resources/themes/ownTheme/import/generic/importA.css", "importA.css");
        modifiedFile = modifiedFile.replace("url(/web_resources/themes/ownTheme/import/generic/images/fondo_1.png)", "url(\"../images/fondo_1.png\")");
        modifiedFile = modifiedFile.replace("url(/web_resources/themes/ownTheme/import/generic/images/corner.modulo.png)", "url(\"/images/corner.modulo.png\")");
        modifiedFile = modifiedFile.replace("url(/web_resources/themes/ownTheme/import/generic/images/boton.gif)", "url(\"images/boton.gif\")");
        modifiedFile = modifiedFile.replace("url(http://generic:9980/images/back.modulo.png)", "url(\"http://generic:9980/images/back.modulo.png\")");
        modifiedFile = modifiedFile.replace(
                "url(data:image/gif;base64,AABgASAAAIMAAVCBxIsKDBgwgTDkzAsKGAhxARSJx4oKJFAxgzFtjIkYDHjwNCigxAsiSAkygDAgA7)",
                "url(data:image/gif;base64,AABgASAAAIMAAVCBxIsKDBgwgTDkzAsKGAhxARSJx4oKJFAxgzFtjIkYDHjwNCigxAsiSAkygDAgA7)");

        assertEquals(sampleFile, modifiedFile);
    }

    private void assertUrls(String cssFile) {
        assertTrue(cssFile.contains("url(/web_resources/themes/ownTheme/import/generic/images/fondo_1.png)"));
        assertTrue(cssFile.contains("url(/web_resources/themes/ownTheme/import/generic/images/corner.modulo.png)"));
        assertTrue(cssFile.contains("url(/web_resources/themes/ownTheme/import/generic/images/boton.gif)"));
        assertTrue(cssFile.contains("url(http://generic:9980/images/back.modulo.png)"));
        assertTrue(cssFile.contains("url(\"c:/malformed/url.png/\")"));
        assertTrue(cssFile.contains("url(data:image/gif;base64,AABgASAAAIMAAVCBxIsKDBgwgTDkzAsKGAhxARSJx4oKJFAxgzFtjIkYDHjwNCigxAsiSAkygDAgA7)"));
    }

    private void assertImages(Map<String, String> imagesToDownload) {
        assertEquals(absoluteThemePath + "/import/generic/images/corner.modulo.png", imagesToDownload.get("http://generic:8080/images/corner.modulo.png"));
        assertEquals(absoluteThemePath + "/import/generic/images/boton.gif", imagesToDownload.get("http://generic:8080/images/boton.gif"));
        assertEquals(absoluteThemePath + "/import/generic/images/fondo_1.png", imagesToDownload.get("http://generic:8080/images/fondo_1.png"));
        assertEquals(absoluteThemePath + "/import/generic/images/back.modulo.png", imagesToDownload.get("http://generic:9980/images/back.modulo.png"));
        assertEquals(absoluteThemePath + "/import/generic/images/buttonCancel.png", imagesToDownload.get("http://generic:8080/images/buttonCancel.png"));
        assertEquals(absoluteThemePath + "/import/generic/images/buttonOk.png", imagesToDownload.get("http://generic:8080/images/buttonOk.png"));
        assertFalse(imagesToDownload.containsKey("/malformed/url.png/"));
    }

    private String loadFileFromDisk(String path) {
        try (InputStream in = new FileInputStream(new File(path))) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println("Error loading file: " + path);
            return "";
        }
    }

    private void saveFile(StringBuilder sb, String path) {
        try (FileWriter fstream = new FileWriter(path); PrintWriter out = new PrintWriter(fstream)) {
            out.write(sb.toString());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Get the file's name from the given url.
     */
    private String getFileName(String url) {
        try {
            var u = new URL(url);
            return u.getFile();
        } catch (Exception e) {
            System.out.println("Error trying to get file name.");
        }
        return null;
    }

    /**
     * Get the file's path for the given file name.
     */
    private String getFilePath(String fileName) {
        if (fileName != null) {
            if (fileName.equals(IMPORT_A)) {
                return IMPORT_FILE + IMPORT_A;
            }
            if (fileName.equals(IMPORT_B)) {
                return IMPORT_FILE + IMPORT_B;
            }
            if (fileName.equals(IMPORT_C)) {
                return IMPORT_FILE + IMPORT_C;
            }
        }
        return null;
    }
}
