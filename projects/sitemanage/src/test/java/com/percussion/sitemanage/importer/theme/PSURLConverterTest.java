// REFACTORED: CP-JAVA11
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

package com.percussion.sitemanage.importer.theme;

import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.PSSiteImportLogger;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PSURLConverterTest {

    private IPSSiteImportLogger logger = new PSSiteImportLogger(PSLogObjectType.SITE);

    private static final String BASE_URL = "http://generic:9980";
    private static final String THEME_NAME = "ExampleTheme";
    private static final String SITE_NAME = "generic";
    private String ABSOLUTE_THEME_PATH;
    private static final String THEME_URL = "/web_resources/themes/" + THEME_NAME;
    private static final String ASSETS_PATH = "/Assets/uploads/" + THEME_NAME;

    @BeforeEach
    void init() throws IOException {
        ABSOLUTE_THEME_PATH = Paths.get("c:/themePath/" + THEME_NAME).toFile().getCanonicalPath();
    }

    @Test
    void testUrlConverter_WithEmptyValues() {
        String remoteUrl;
        String baseUrl = "";
        String siteName = "";
        String absoluteThemePath = "";
        String link = "";
        PSURLConverter urlConverter;

        urlConverter = new PSURLConverter(baseUrl, siteName, absoluteThemePath, "", logger);
        remoteUrl = urlConverter.getFullUrl(link);
        assertEquals("", remoteUrl);
        assertEquals("", urlConverter.convertToThemeLink(remoteUrl));
        assertEquals("", urlConverter.getFileSystemPath(remoteUrl));

        siteName = "generic";
        urlConverter = new PSURLConverter(baseUrl, siteName, absoluteThemePath, "", logger);
        remoteUrl = urlConverter.getFullUrl(link);
        assertEquals("", remoteUrl);
        assertEquals("", urlConverter.convertToThemeLink(remoteUrl));
        assertEquals("", urlConverter.getFileSystemPath(remoteUrl));

        absoluteThemePath = "c:/themePath/";
        urlConverter = new PSURLConverter(baseUrl, siteName, absoluteThemePath, "", logger);
        remoteUrl = urlConverter.getFullUrl(link);
        assertEquals("", remoteUrl);
        assertEquals("", urlConverter.convertToThemeLink(remoteUrl));
        assertEquals("", urlConverter.getFileSystemPath(remoteUrl));

        urlConverter = new PSURLConverter(baseUrl, siteName, absoluteThemePath, "", logger);
        remoteUrl = urlConverter.getFullUrl(link);
        assertEquals("", remoteUrl);
        assertEquals("", urlConverter.convertToThemeLink(remoteUrl));
        assertEquals("", urlConverter.getFileSystemPath(remoteUrl));

        baseUrl = "http://generic:9980/folder1/folder2/cssFile.css";
        urlConverter = new PSURLConverter(baseUrl, siteName, absoluteThemePath, "", logger);
        remoteUrl = urlConverter.getFullUrl(link);
        assertEquals("", remoteUrl);
        assertEquals("", urlConverter.convertToThemeLink(remoteUrl));
        assertEquals("", urlConverter.getFileSystemPath(remoteUrl));
    }

    @Test
    void testUrlConverter_RelativeCurrentPath() {
        String remoteUrl;
        String link;
        PSURLConverter urlConverter;

        urlConverter = new PSURLConverter(BASE_URL + "/folder1/folder2/", SITE_NAME, ABSOLUTE_THEME_PATH, THEME_URL, logger);
        link = "images/ExampleImage.png";
        remoteUrl = urlConverter.getFullUrl(link);
        assertEquals(BASE_URL + "/folder1/folder2/" + "images/ExampleImage.png", remoteUrl);
        assertEquals(THEME_URL + "/import/" + SITE_NAME + "/folder1/folder2/images/ExampleImage.png", urlConverter.convertToThemeLink(remoteUrl));
        assertEquals((ABSOLUTE_THEME_PATH + "/import/" + SITE_NAME + "/folder1/folder2/images/ExampleImage.png").replace("/", File.separator),
                urlConverter.getFileSystemPath(remoteUrl));
    }

    @Test
    void testUrlConverter_RelativeHost() {
        String remoteUrl;
        String link;
        PSURLConverter urlConverter;

        urlConverter = new PSURLConverter(BASE_URL, SITE_NAME, ABSOLUTE_THEME_PATH, THEME_URL, logger);
        link = "/backgroundImages/ExampleBackGround1.png";
        remoteUrl = urlConverter.getFullUrl(link);
        assertEquals(BASE_URL + "/backgroundImages/ExampleBackGround1.png", remoteUrl);
        assertEquals(THEME_URL + "/import/" + SITE_NAME + "/backgroundImages/ExampleBackGround1.png", urlConverter.convertToThemeLink(remoteUrl));
        assertEquals((ABSOLUTE_THEME_PATH + "/import/" + SITE_NAME + "/backgroundImages/ExampleBackGround1.png").replace("/", File.separator),
                urlConverter.getFileSystemPath(remoteUrl));
    }

    @Test
    void testUrlConverter_RelativeUsingDots() {
        String remoteUrl;
        String link;
        PSURLConverter urlConverter;

        urlConverter = new PSURLConverter(BASE_URL + "/folder1/folder2/", SITE_NAME, ABSOLUTE_THEME_PATH, THEME_URL, logger);
        link = "../textures/texture1.jpg";
        remoteUrl = urlConverter.getFullUrl(link);
        assertEquals(BASE_URL + "/folder1" + "/textures/texture1.jpg", remoteUrl);
        assertEquals(THEME_URL + "/import/" + SITE_NAME + "/folder1/textures/texture1.jpg", urlConverter.convertToThemeLink(remoteUrl));
        assertEquals((ABSOLUTE_THEME_PATH + "/import/" + SITE_NAME + "/folder1/textures/texture1.jpg").replace("/", File.separator),
                urlConverter.getFileSystemPath(remoteUrl));
    }

    @Test
    void testUrlConverter_FixUrlUsingDots() {
        String remoteUrl;
        String link;
        PSURLConverter urlConverter;

        urlConverter = new PSURLConverter(BASE_URL, SITE_NAME, ABSOLUTE_THEME_PATH, THEME_URL, logger);
        link = "../textures/texture1.jpg";
        remoteUrl = urlConverter.getFullUrl(link);
        assertEquals(BASE_URL + "/textures/texture1.jpg", remoteUrl);
    }

    @Test
    void testUrlConverter_BinaryImage() {
        String remoteUrl;
        String link;
        PSURLConverter urlConverter;

        urlConverter = new PSURLConverter(BASE_URL, SITE_NAME, ABSOLUTE_THEME_PATH, THEME_URL, logger);
        link = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAARMA...";
        remoteUrl = urlConverter.getFullUrl(link);
        assertEquals("", remoteUrl);
        assertEquals("", urlConverter.convertToThemeLink(remoteUrl));
        assertEquals("", urlConverter.getFileSystemPath(remoteUrl));
    }

    @Test
    void testUrlConverter_AbsoluteUrlWithSameHost() {
        String remoteUrl;
        String link;
        PSURLConverter urlConverter;

        urlConverter = new PSURLConverter(BASE_URL, SITE_NAME, ABSOLUTE_THEME_PATH, THEME_URL, logger);
        link = BASE_URL + "/images/srpr/logo3w.png";
        remoteUrl = urlConverter.getFullUrl(link);
        assertFalse(remoteUrl.isEmpty());
        assertEquals(THEME_URL + "/import/" + SITE_NAME + "/images/srpr/logo3w.png", urlConverter.convertToThemeLink(remoteUrl));
        assertEquals((ABSOLUTE_THEME_PATH + "/import/" + SITE_NAME + "/images/srpr/logo3w.png").replace("/", File.separator),
                urlConverter.getFileSystemPath(remoteUrl));
    }

    @Test
    void testGetFileSystemPathForCss_cssEndedFile() {
        String remoteUrl = BASE_URL + "/folder1/textures/texture1.css";
        var urlConverter = new PSURLConverter(BASE_URL, SITE_NAME, ABSOLUTE_THEME_PATH, THEME_URL, logger);
        assertEquals((ABSOLUTE_THEME_PATH + "/import/" + SITE_NAME + "/folder1/textures/texture1.css").replace("/", File.separator),
                urlConverter.getFileSystemPathForCss(remoteUrl));
    }

    @Test
    void testGetFileSystemPathForCss_otherSuffix() {
        String remoteUrl = BASE_URL + "/folder1/textures/texture1.cfm";
        var urlConverter = new PSURLConverter(BASE_URL, SITE_NAME, ABSOLUTE_THEME_PATH, THEME_URL, logger);
        assertEquals((ABSOLUTE_THEME_PATH + "/import/" + SITE_NAME + "/folder1/textures/texture1.cfm.css").replace("/", File.separator),
                urlConverter.getFileSystemPathForCss(remoteUrl));
    }

    @Test
    void testConvertToThemeLinkForCss_cssEndedFile() {
        String remoteUrl = BASE_URL + "/folder1/textures/texture1.css";
        var urlConverter = new PSURLConverter(BASE_URL, SITE_NAME, ABSOLUTE_THEME_PATH, THEME_URL, logger);
        assertEquals(THEME_URL + "/import/" + SITE_NAME + "/folder1/textures/texture1.css", urlConverter.convertToThemeLinkForCss(remoteUrl));
    }

    @Test
    void testConvertToThemeLinkForCss_otherSuffix() {
        String remoteUrl = BASE_URL + "/folder1/textures/texture1.cfm";
        var urlConverter = new PSURLConverter(BASE_URL, SITE_NAME, ABSOLUTE_THEME_PATH, THEME_URL, logger);
        assertEquals(THEME_URL + "/import/" + SITE_NAME + "/folder1/textures/texture1.cfm.css", urlConverter.convertToThemeLinkForCss(remoteUrl));
    }

    @Test
    void testGetFileSystemPathForImg_cssEndedFile() {
        String remoteUrl = BASE_URL + "/homepage/2011/047.jpg";
        var urlConverter = new PSURLConverter(BASE_URL, SITE_NAME, ABSOLUTE_THEME_PATH, THEME_URL, logger);
        assertEquals(ASSETS_PATH + "/import/" + SITE_NAME + "/homepage/2011/047.jpg", urlConverter.getCmsFolderPathForImageAsset(remoteUrl, THEME_NAME));
    }

    @Test
    void testConvertCssResourceWithParamenters() {
        var urlConverter = new PSURLConverter(BASE_URL, SITE_NAME, ABSOLUTE_THEME_PATH, THEME_URL, logger);

        String remoteUrl1 = BASE_URL + "/min/?f=/includes/templates/freetemplate2/css/stylesheet1.css&1327766570";
        String remoteUrl2 = BASE_URL + "/?css=_stylesheets/print.v.1317061408";

        assertEquals((ABSOLUTE_THEME_PATH + "/import/" + SITE_NAME + "/generic_1.css").replace("/", File.separator),
                urlConverter.getFileSystemPathForCss(remoteUrl1));
        assertEquals((ABSOLUTE_THEME_PATH + "/import/" + SITE_NAME + "/generic_2.css").replace("/", File.separator),
                urlConverter.getFileSystemPathForCss(remoteUrl2));
    }

    @Test
    void testConvertResourceWithInvalidCharacters() {
        String remoteUrlWithColon = "http://generic:9980/media%3a/js/site-min.js";
        var urlConverter = new PSURLConverter(BASE_URL, SITE_NAME, ABSOLUTE_THEME_PATH, THEME_URL, logger);
        assertEquals((ABSOLUTE_THEME_PATH + "/import/" + SITE_NAME + "/media-/js/site-min.js".toLowerCase()).replace("/", File.separator),
                urlConverter.getFileSystemPath(remoteUrlWithColon));
    }
}
