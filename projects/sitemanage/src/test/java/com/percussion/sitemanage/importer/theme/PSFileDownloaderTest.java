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

import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.server.PSServer;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.PSSiteImportLogger;

import com.percussion.utils.types.PSPair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

/**
 * Integration tests for PSFileDownloader.
 * @author Santiago M. Murchio, Sunny Sal (refactored)
 */
@Tag("IntegrationTest")
class PSFileDownloaderTest {

    private static final Logger log = LogManager.getLogger(PSFileDownloaderTest.class);

    private PSSiteImportCtx context;
    private IPSSiteImportLogger logger = new PSSiteImportLogger(PSLogObjectType.SITE);
    private IPSFileDownloader fileDownloader = new PSFileDownloader();

    private final List<File> filesToDelete = new ArrayList<>();
    private final File tempDir = new File(PSServer.getRxDir().getAbsolutePath() + "/temp/ImageDownloaderTest");

    @BeforeEach
    void setUp() {
        try {
            if (!tempDir.exists()) {
                forceMkdir(tempDir);
            }
        } catch (IOException e) {
            fail("No exception should have been thrown.");
        }
        context = new PSSiteImportCtx();
        context.setLogger(logger);
    }

    @AfterEach
    void tearDown() {
        for (var fileToDelete : filesToDelete) {
            if (fileToDelete.exists()) {
                try {
                    forceDelete(fileToDelete);
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    @Test
    void testDownloadFiles_emptyMap() {
        var filesMap = new HashMap<String, String>();
        var downloaded = fileDownloader.downloadFiles(filesMap, context, false);
        assertNotNull(downloaded);
        assertTrue(downloaded.isEmpty());
    }

    @Test
    void testDownloadFiles_malformedUrl() {
        var tempFile = createTempFile();
        assertNotNull(tempFile, "Could not create temporal file.");

        var invalidUrl = "invalid.url(to test)";
        var invalidUrlName = "/nonExistingFile.css";
        var invalidUrlDestinationPath = tempDir.getAbsolutePath() + invalidUrlName;

        var validUrl = "file://" + tempFile.getAbsolutePath().replace('\\', '/');
        var validUrlName = "/validFile.css";
        var validUrlDestinationPath = tempDir.getAbsolutePath() + validUrlName;

        var filesMap = new HashMap<String, String>();
        filesMap.put(invalidUrl, invalidUrlDestinationPath);
        filesMap.put(validUrl, validUrlDestinationPath);
        var downloaded = fileDownloader.downloadFiles(filesMap, context, false);

        var invalid = new File(invalidUrlDestinationPath);
        var valid = new File(validUrlDestinationPath);

        filesToDelete.add(invalid);
        filesToDelete.add(valid);
        filesToDelete.add(tempFile);

        assertNotNull(downloaded);
        for (var download : downloaded) {
            assertNotNull(download);
        }
        assertTrue(valid.exists());
        assertFalse(invalid.exists());
    }

    @Test
    void testDownloadTest_UrlWithSpaces() {
        // This test is a placeholder for manual/visual verification.
        var url = "http://www.frsf.utn.edu.ar/images/banners/ofa-frsf 1.gif";
        var dest = tempDir.getAbsolutePath() + "/ofa-frsf 1.gif";
        fileDownloader.downloadFile(url, dest);
        // No assertion: just ensure no exception is thrown.
    }

    @Test
    void testDownloadTest_WhenFileExists() {
        var tempFile = createTempFile();
        assertNotNull(tempFile, "Could not create temporal file.");
        filesToDelete.add(tempFile);

        var url = "file://" + tempFile.getAbsolutePath().replace('\\', '/');
        var dest = tempDir.getAbsolutePath() + "/logo3w.png";
        var downloadResult = fileDownloader.downloadFile(url, dest);

        // The file should be downloaded
        assertTrue(downloadResult.getFirst());

        var downloadedFile = new File(dest);

        // Call the downloader again, the file should not be downloaded
        var expectedMessage = "Skip download '" + url + "' to '" + dest + "', as such file already exists.";
        downloadResult = fileDownloader.downloadFile(url, dest);
        assertTrue(downloadResult.getFirst());
        assertEquals(expectedMessage, downloadResult.getSecond());

        filesToDelete.add(downloadedFile);
    }

    /**
     * Creates a temporal file with sample content.
     * @return File with sample content, or null if there was an error creating it.
     */
    private File createTempFile() {
        var tempFile = new File(tempDir, "tempFile.css");
        try {
            FileUtils.writeStringToFile(tempFile, "sample data for temp file.", StandardCharsets.UTF_8);
            return tempFile;
        } catch (IOException e) {
            return null;
        }
    }
}
