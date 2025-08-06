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

import com.percussion.services.assembly.impl.PSReplacementFilter;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.helpers.impl.PSImportThemeHelper.LogCategory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.Validate.notNull;
import static org.springframework.util.StringUtils.endsWithIgnoreCase;

/**
 * Given a URL, calculates its fully qualified URL, converts it to a theme link, and gets where the resource should be saved.
 */
public class PSURLConverter {

    private static final String IMPORT_FOLDER = "/import";
    private static final String ASSET_FOLDER = "/Assets/uploads/";
    private int indexOfFiles;
    private static final String CSS_EXTENSION = ".css";
    private final String baseUrl;
    private final String siteName;
    private final String themeRootDirectory;
    private final String themeRootUrl;
    private final IPSSiteImportLogger logger;

    public PSURLConverter(String baseUrl, String siteName, String themeRootDirectory, String themeRootUrl, IPSSiteImportLogger logger) {
        notNull(baseUrl);
        notNull(themeRootUrl);
        notNull(siteName);
        notNull(themeRootDirectory);
        notNull(logger);

        this.baseUrl = baseUrl;
        this.siteName = siteName;
        Path p = Paths.get(themeRootDirectory);
        try {
            this.themeRootDirectory = p.toFile().getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.themeRootUrl = themeRootUrl;
        this.logger = logger;
        this.indexOfFiles = initializeIndex();
    }

    public String getFullUrl(String linkPath) {
        notNull(linkPath);
        if (isBlank(linkPath))
            return "";
        try {
            var baseUrlObj = new URL(this.baseUrl);
            var resourceUrl = new URL(baseUrlObj, linkPath);
            var remoteUrl = resourceUrl.toString().replace("../", "");
            return remoteUrl;
        } catch (Exception e) {
            this.logger.appendLogMessage(IPSSiteImportLogger.PSLogEntryType.ERROR, LogCategory.ConvertURL.getName(), "Invalid URL: " + baseUrl);
            return "";
        }
    }

    public String convertToThemeLink(String resourceUrl) {
        return getConvertedThemeLink(resourceUrl, false);
    }

    public String convertToThemeLinkForCss(String remoteUrl) {
        var convertedLink = getConvertedThemeLink(remoteUrl, true);
        var linkAndParameters = convertedLink.split("\\?");
        Set<String> suffixes = new HashSet<>();
        suffixes.add(FileSuffixes.Css.getSuffix());
        suffixes.add(FileSuffixes.CssGz.getSuffix());
        linkAndParameters[0] = addSuffixIfNeeded(linkAndParameters[0], suffixes);
        return StringUtils.join(linkAndParameters, "?");
    }

    public String getFileSystemPath(String resourceUrl) {
        return getConvertedFileSystemPath(resourceUrl, false);
    }

    public String getFileSystemPathForCss(String remoteUrl) {
        var fullThemePath = getConvertedFileSystemPath(remoteUrl, true);
        Set<String> suffixes = new HashSet<>();
        suffixes.add(FileSuffixes.Css.getSuffix());
        suffixes.add(FileSuffixes.CssGz.getSuffix());
        fullThemePath = addSuffixIfNeeded(fullThemePath, suffixes);
        return fullThemePath;
    }

    public String getCmsFolderPathForImageAsset(String resourceUrl, String siteName) {
        notNull(resourceUrl);
        if (isBlank(resourceUrl))
            return "";
        try {
            var url = new URL(resourceUrl);
            var savePath = ASSET_FOLDER + siteName + IMPORT_FOLDER;
            return savePath + "/" + url.getHost() + validatePath(url.getPath());
        } catch (Exception e) {
            this.logger.appendLogMessage(IPSSiteImportLogger.PSLogEntryType.ERROR, LogCategory.ConvertURL.getName(),
                    "Invalid URL: " + resourceUrl);
            return "";
        }
    }

    private String getConvertedThemeLink(String resourceUrl, boolean getPathFromQuery) {
        notNull(resourceUrl);
        if (isBlank(resourceUrl))
            return "";
        try {
            var url = new URL(resourceUrl);
            var importPath = this.themeRootUrl + IMPORT_FOLDER;
            if (url.getQuery() != null && getPathFromQuery) {
                return importPath + "/" + url.getHost() + "/" + validatePath(getPathFromQuery(false));
            }
            return importPath + "/" + url.getHost() + validatePath(PSReplacementFilter.filter(url.getFile()));
        } catch (Exception e) {
            this.logger.appendLogMessage(IPSSiteImportLogger.PSLogEntryType.ERROR, LogCategory.ConvertURL.getName(),
                    "Invalid URL: " + resourceUrl);
            return "";
        }
    }

    private String getConvertedFileSystemPath(String resourceUrl, boolean getPathFromQuery) {
        notNull(resourceUrl);
        if (isBlank(resourceUrl))
            return "";
        try {
            var url = new URL(resourceUrl);
            var savePath = Paths.get(this.themeRootDirectory + IMPORT_FOLDER);
            String t;
            if (url.getQuery() != null && getPathFromQuery) {
                t = validatePath(getPathFromQuery(true));
                if (t.startsWith("/"))
                    t = t.substring(1);
                return savePath.resolve(url.getHost()).resolve(t).toFile().getCanonicalPath();
            } else {
                t = validatePath(PSReplacementFilter.filter(url.getFile()));
                if (t.startsWith("/"))
                    t = t.substring(1);
                return savePath.resolve(url.getHost()).resolve(t).toFile().getCanonicalPath();
            }
        } catch (Exception e) {
            this.logger.appendLogMessage(IPSSiteImportLogger.PSLogEntryType.ERROR, LogCategory.ConvertURL.getName(),
                    "Invalid URL: " + resourceUrl);
            return "";
        }
    }

    private String addSuffixIfNeeded(String path, Set<String> suffixes) {
        for (var suffix : suffixes) {
            if (endsWithIgnoreCase(path, suffix)) {
                return path;
            }
        }
        return path.concat(FileSuffixes.Css.getSuffix());
    }

    public enum FileSuffixes {
        Css(".css"), Js(".js"), CssGz(".css.gz");
        private final String suffix;
        FileSuffixes(String suffix) {
            this.suffix = suffix;
        }
        public String getSuffix() {
            return suffix;
        }
    }

    private String getPathFromQuery(boolean incrementIndex) {
        if (incrementIndex) {
            return this.siteName + "_" + ++indexOfFiles + CSS_EXTENSION;
        } else {
            return this.siteName + "_" + indexOfFiles + CSS_EXTENSION;
        }
    }

    private String validatePath(String path) {
        String decodedPath = "";
        try {
            decodedPath = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            this.logger.appendLogMessage(IPSSiteImportLogger.PSLogEntryType.ERROR, LogCategory.ConvertURL.getName(),
                    "Unable to validate path: " + path);
        }
        var returnString = decodedPath.replace(":", "(colon)");
        if (returnString.contains("?")) {
            var alteredQueryString = new String(returnString.substring(returnString.lastIndexOf("?")));
            returnString = returnString.replace(alteredQueryString, "");
            if (returnString.contains("/")) {
                returnString = returnString.substring(0, returnString.lastIndexOf("/") + 1);
            }
            try {
                alteredQueryString = new String(Base64.encodeBase64(alteredQueryString.getBytes(StandardCharsets.UTF_8)));
                returnString = returnString.substring(0, returnString.lastIndexOf("/") + 1) + alteredQueryString;
            } catch (Exception e) {
                this.logger.appendLogMessage(IPSSiteImportLogger.PSLogEntryType.ERROR, LogCategory.ConvertURL.getName(),
                        "Unable to properly convert URL to Path for: " + path);
            }
        }
        return returnString.replace(" ", "-");
    }

    private int initializeIndex() {
        var themeDirectory = this.themeRootDirectory + IMPORT_FOLDER;
        if (StringUtils.isNotBlank(themeDirectory)) {
            var folderPath = new File(themeDirectory);
            if (folderPath.isFile()) {
                folderPath = folderPath.getParentFile();
            }
            return getCurrentIndex(folderPath);
        }
        return 0;
    }

    private int getCurrentIndex(File folderPath) {
        var regex = this.siteName + "[0-9]+" + CSS_EXTENSION;
        int numberOfMatches = 0;
        var files = folderPath.list();
        if (files != null) {
            for (var file : files) {
                if (this.siteName.equals(file) && numberOfMatches < 0) {
                    numberOfMatches = 0;
                } else if (Pattern.matches(regex, file)) {
                    int extensionIndex = file.lastIndexOf(CSS_EXTENSION);
                    int number = Integer.parseInt(file.substring(this.siteName.length(), extensionIndex));
                    if (number > numberOfMatches) {
                        numberOfMatches = number;
                    }
                }
            }
        }
        return numberOfMatches;
    }
}
