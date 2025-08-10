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
package com.percussion.sitemanage.importer.theme;

import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.Validate.notNull;

import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.helpers.impl.PSImportThemeHelper.LogCategory;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Importer class that gets the links and scripts from header of a given document. Also updates the
 * header with new paths for links and scripts.
 */
public class PSHTMLHeaderImporter {

  private final Document docSource;
  private final Element docHeader;
  private final Element docBody;
  private final String siteUrl;
  private final String siteName;
  private final PSURLConverter urlConverter;
  private final PSCSSParser cssParser;
  private final IPSSiteImportLogger logger;

  private static final String CONVERTED_CSS_URL = "Replaced CSS URL from ''{0}'' to ''{1}''.";
  private static final String CONVERTED_SCRIPT_URL = "Replaced script URL from ''{0}'' to ''{1}''.";
  private static final String CSS_REL_ATTRIBUTE = "stylesheet";
  private static final String END_SUFFIX_FLASH_FILE = ".swf";
  private static final String SHORT_ICON_REL_ATTRIBUTE = "shortcut icon";
  private static final String ICON_REL_ATTRIBUTE = "icon";

  /** Constructor. */
  public PSHTMLHeaderImporter(
      Document sourceDoc,
      String siteUrl,
      String siteName,
      String themeRootDirectory,
      String themeRootUrl,
      IPSSiteImportLogger logger) {
    notNull(sourceDoc);
    notNull(siteUrl);
    notNull(siteName);
    notNull(themeRootDirectory);
    notNull(themeRootUrl);
    notNull(logger);

    this.docSource = sourceDoc;
    this.docHeader = sourceDoc.head();
    this.docBody = sourceDoc.body();
    this.siteName = siteName;
    this.siteUrl = siteUrl;
    this.logger = logger;
    urlConverter =
        new PSURLConverter(this.siteUrl, siteName, themeRootDirectory, themeRootUrl, logger);
    cssParser = new PSCSSParser(siteName, themeRootDirectory, themeRootUrl, logger);
  }

  public Map<String, String> getLinkPaths() {
    var links = this.docSource.select("link");
    var linkPaths = new HashMap<String, String>();
    appendHeaderImporterMessage("Starting to process CSS links in the document header.");
    for (var link : links) {
      if (isValidLinkElement(link)) {
        var remoteUrl = urlConverter.getFullUrl(link.attr("href"));
        String fullThemePath;
        String convertedLink;
        if (!remoteUrl.startsWith(this.siteUrl)) {
          fullThemePath = remoteUrl;
          convertedLink = remoteUrl;
        } else {
          if (isValidCssLinkElement(link)) {
            fullThemePath = urlConverter.getFileSystemPathForCss(remoteUrl);
            convertedLink = urlConverter.convertToThemeLinkForCss(remoteUrl);
          } else {
            fullThemePath = urlConverter.getFileSystemPath(remoteUrl);
            convertedLink = urlConverter.convertToThemeLink(remoteUrl);
          }
        }
        linkPaths.put(remoteUrl, fullThemePath);
        appendHeaderImporterMessage(
            MessageFormat.format(CONVERTED_CSS_URL, link.attr("href"), convertedLink));
        link.attr("href", convertedLink);
      }
    }
    appendHeaderImporterMessage(
        "Finished the processing for link paths. Processed: " + linkPaths.size() + " elements.");
    return linkPaths;
  }

  public Map<String, String> getScriptPaths() {
    var scripts = docSource.select("script");
    var scriptPaths = new HashMap<String, String>();
    appendHeaderImporterMessage("Starting to process script paths in the document.");
    for (var script : scripts) {
      if (!isBlank(script.attr("src"))) {
        var remoteUrl = urlConverter.getFullUrl(script.attr("src"));
        if (isNotBlank(remoteUrl)) {
          String fullThemePath;
          String convertedLink;
          if (!remoteUrl.startsWith(this.siteUrl) && remoteUrl.toLowerCase().startsWith("http")) {
            fullThemePath = remoteUrl;
            convertedLink = remoteUrl;
          } else {
            fullThemePath = urlConverter.getFileSystemPath(remoteUrl);
            convertedLink = urlConverter.convertToThemeLink(remoteUrl);
          }
          scriptPaths.put(remoteUrl, fullThemePath);
          appendHeaderImporterMessage(
              MessageFormat.format(CONVERTED_SCRIPT_URL, script.attr("src"), convertedLink));
          script.attr("src", convertedLink);
        }
      }
    }
    appendHeaderImporterMessage(
        "Finished the processing for script paths. Processed: "
            + scriptPaths.size()
            + " elements.");
    return scriptPaths;
  }

  public Map<String, String> processInlineStyles() {
    var processedInlineImages = new HashMap<String, String>();
    processedInlineImages.putAll(processHeaderInlineStyles());
    processedInlineImages.putAll(processBodyInlineStyles());
    processedInlineImages.putAll(processBodyStyleAtributes());
    return processedInlineImages;
  }

  public Map<String, String> processCssFiles(Map<String, String> cssFilesMap) {
    appendHeaderImporterMessage("Starting to process images included in CSS files.");
    notNull(cssFilesMap);
    var images = cssParser.parse(cssFilesMap);
    appendHeaderImporterMessage(
        "Completed the processing for images in CSS files. Processed: "
            + images.size()
            + " elements.");
    return images;
  }

  public Map<String, String> processHeaderAndBodyImages() {
    var imagesMapInline = new HashMap<String, String>();
    var imgElements = docSource.getElementsByTag("img");
    imgElements.addAll(docSource.select("input[type=image]"));
    appendHeaderImporterMessage(
        "Starting to process the images referenced in <img> and <input type=image> tags.");
    for (var imgElement : imgElements) {
      if (!isBlank(imgElement.attr("src"))) {
        var remoteUrl = urlConverter.getFullUrl(imgElement.attr("src"));
        var fullThemePath = urlConverter.getCmsFolderPathForImageAsset(remoteUrl, this.siteName);
        imagesMapInline.put(remoteUrl, fullThemePath);
        appendHeaderImporterMessage(
            MessageFormat.format(CONVERTED_SCRIPT_URL, imgElement.attr("src"), fullThemePath));
        imgElement.attr("src", fullThemePath);
      }
    }
    appendHeaderImporterMessage(
        "Finished the processing for images referenced in <img> and <input type=image> tags."
            + " Processed: "
            + imagesMapInline.size()
            + " elements.");
    return imagesMapInline;
  }

  public Map<String, String> processFlashFiles(String siteName) {
    var flashObjects = docSource.select("object");
    var embedFlashPaths = new HashMap<String, String>();
    appendHeaderImporterMessage("Starting to process swf files in <object> tags in the document.");
    for (var flash : flashObjects) {
      if (!isValidObjectFlash(flash)) continue;
      embedFlashPaths.putAll(processDataAttribute(flash, siteName));
      embedFlashPaths.putAll(processFlashObject(flash, "param[name=movie]", "value", siteName));
      embedFlashPaths.putAll(processFlashObject(flash, "embed", "src", siteName));
    }
    appendHeaderImporterMessage(
        "Finished the processing for swf files in <object> tags. Processed: "
            + embedFlashPaths.size()
            + " elements.");
    return embedFlashPaths;
  }

  private void appendHeaderImporterMessage(String message) {
    this.logger.appendLogMessage(
        IPSSiteImportLogger.PSLogEntryType.STATUS, LogCategory.ImportHeader.getName(), message);
  }

  private boolean isValidLinkElement(Element link) {
    if (isValidCssLinkElement(link)) {
      return true;
    }
    var relAttribute = link.attr("rel");
    return equalsIgnoreCase(relAttribute, ICON_REL_ATTRIBUTE)
        || equalsIgnoreCase(relAttribute, SHORT_ICON_REL_ATTRIBUTE);
  }

  private boolean isValidCssLinkElement(Element link) {
    var hrefAttribute = link.attr("href");
    var relAttribute = link.attr("rel");
    return !isBlank(hrefAttribute) && containsIgnoreCase(relAttribute, CSS_REL_ATTRIBUTE);
  }

  private boolean isValidObjectFlash(Element flashObject) {
    var dataAttribute = flashObject.attr("data");
    if (!isBlank(dataAttribute) && dataAttribute.endsWith(END_SUFFIX_FLASH_FILE)) {
      return true;
    }
    var movies = flashObject.select("param[name=movie]");
    for (var movie : movies) {
      var valueAttribute = movie.attr("value");
      if (!isBlank(valueAttribute) && valueAttribute.endsWith(END_SUFFIX_FLASH_FILE)) {
        return true;
      }
    }
    return false;
  }

  private Map<String, String> processHeaderInlineStyles() {
    var styleElements = docHeader.select("style");
    var inlineImagesHeader = new HashMap<String, String>();
    appendHeaderImporterMessage("Processing inline stypes included in document header.");
    for (var el : styleElements) {
      appendHeaderImporterMessage("Style content before replacing: " + el.data());
      var parserResult = cssParser.parse(this.siteUrl, el.data());
      el.empty();
      el.appendText(parserResult.getSecond().replace("\"", "'"));
      appendHeaderImporterMessage("Style content after replacing: " + parserResult.getSecond());
      inlineImagesHeader.putAll(parserResult.getFirst());
    }
    return inlineImagesHeader;
  }

  private Map<String, String> processBodyInlineStyles() {
    var styleElements = docBody.getElementsByTag("style");
    var imagesMapInline = new HashMap<String, String>();
    appendHeaderImporterMessage("Processing inline styles included in document body.");
    for (var el : styleElements) {
      appendHeaderImporterMessage("Style content before replacing: " + el.data());
      var parserResult = cssParser.parse(this.siteUrl, el.data());
      el.empty();
      el.appendText(parserResult.getSecond().replace("\"", "'"));
      appendHeaderImporterMessage("Style content after replacing: " + parserResult.getSecond());
      imagesMapInline.putAll(parserResult.getFirst());
    }
    return imagesMapInline;
  }

  private Map<String, String> processBodyStyleAtributes() {
    var styleElements = docBody.getElementsByAttribute("style");
    var imagesMapInline = new HashMap<String, String>();
    appendHeaderImporterMessage("Processing inline styles attributes in document body.");
    for (var el : styleElements) {
      appendHeaderImporterMessage("Style content before replacing: " + el.attr("style"));
      var parserResult = cssParser.parse(this.siteUrl, el.attr("style"));
      el.attr("style", parserResult.getSecond());
      appendHeaderImporterMessage("Style content after replacing: " + parserResult.getSecond());
      imagesMapInline.putAll(parserResult.getFirst());
    }
    return imagesMapInline;
  }

  private Map<String, String> processFlashObject(
      Element flash, String cssQuery, String attribValue, String siteName) {
    var flashElements = flash.select(cssQuery);
    var flashPaths = new HashMap<String, String>();
    for (var movie : flashElements) {
      if (!isBlank(movie.attr(attribValue))) {
        var remoteUrl = urlConverter.getFullUrl(movie.attr(attribValue));
        var fullThemePath = urlConverter.getCmsFolderPathForImageAsset(remoteUrl, siteName);
        flashPaths.put(remoteUrl, fullThemePath);
        appendHeaderImporterMessage(
            MessageFormat.format(CONVERTED_SCRIPT_URL, movie.attr(attribValue), fullThemePath));
        movie.attr(attribValue, fullThemePath);
      }
    }
    return flashPaths;
  }

  private Map<String, String> processDataAttribute(Element flash, String siteName) {
    var flashPaths = new HashMap<String, String>();
    if (!isBlank(flash.attr("data"))) {
      var remoteUrl = urlConverter.getFullUrl(flash.attr("data"));
      var fullThemePath = urlConverter.getCmsFolderPathForImageAsset(remoteUrl, siteName);
      flashPaths.put(remoteUrl, fullThemePath);
      appendHeaderImporterMessage(
          MessageFormat.format(CONVERTED_SCRIPT_URL, flash.attr("data"), fullThemePath));
      flash.attr("data", fullThemePath);
    }
    return flashPaths;
  }
}
