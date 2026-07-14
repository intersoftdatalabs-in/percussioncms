// REFACTORED: CP-JAVA11
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
package com.percussion.theme.service.impl;

import static com.percussion.share.service.exception.PSParameterValidationUtils.rejectIfNull;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSRequest;
import com.percussion.share.service.IPSDataService.DataServiceDeleteException;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSDataService.DataServiceNotFoundException;
import com.percussion.share.service.IPSDataService.DataServiceSaveException;
import com.percussion.share.service.IPSDataService.PSThemeNotFoundException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.theme.data.PSRegionCSS;
import com.percussion.theme.data.PSRegionCssList;
import com.percussion.theme.data.PSRichTextCustomStyle;
import com.percussion.theme.data.PSTheme;
import com.percussion.theme.data.PSThemeSummary;
import com.percussion.theme.service.IPSThemeService;
import com.percussion.security.io.PSPathInjectionGuard;
import com.percussion.utils.request.PSRequestInfo;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link IPSThemeService}.
 *
 * @author YuBingChen
 */
@Component("themeService")
@Lazy
public class PSThemeService implements IPSThemeService {
  @PostConstruct
  public void init() {
    String tempThemeDir = getThemesTempRootDirectory();
    File tempDir = new File(tempThemeDir);
    FileUtils.deleteQuietly(tempDir);
    // Wire the trusted region-CSS roots into the region CSS file service so
    // its CWE-22 path-traversal validation (PSRegionCSSFileService#
    // requireSafeFilePath) checks containment against these server-
    // controlled directories rather than against a path reconstructed from
    // the untrusted input. Both roots are injected via @Value from the
    // deployment configuration. This closes the path-traversal window
    // flagged by the CRITICAL review thread on PR #1209.
    cssFileService.setAllowedRoots(
        new File(getThemesRootDirectory()), new File(getThemesTempRootDirectory()));
  }

  /*
   * //see base interface method for details
   */
  public List<PSThemeSummary> findAll() {
    List<PSThemeSummary> themes = new ArrayList<>();
    File root = getThemesRoot();
    if (!root.exists()) {
      return themes;
    }

    for (File thFile : Objects.requireNonNull(root.listFiles())) {
      if (thFile.isDirectory()) {
        // codeql[java/path-injection] reason: thFile.getName() is a
        // directory name under the controlled themes root, used here as a
        // file-system lookup key (not concatenated into a path). The
        // theme-name is validated against the segment-marker contract
        // (rejects ".", "..", and any path separator) at the API entry
        // points in this class (see getThemeFolder etc.) which is the
        // authoritative check; the listFiles() call here is a read-only
        // directory scan that does not escape the themes root.
        try {
          String themeName = thFile.getName();
          PSPathInjectionGuard.requireSafeFileName(themeName);
          themes.add(find(themeName));
        } catch (DataServiceLoadException
            | DataServiceNotFoundException
            | PSValidationException
            | IllegalArgumentException e) {
          // IllegalArgumentException is added so a malformed theme
          // directory name (caught by requireSafeFileName at line 96)
          // is logged per-directory rather than aborting the whole
          // enumeration. Per the review on PR #1208.
          log.error("Failed to load theme: {}", thFile.getName());
          log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
      }
    }

    return themes;
  }

  protected void loadThemeSummary(File file, PSThemeSummary summary)
      throws PSThemeNotFoundException {
    File root = getThemesRoot();
    String themeName = file.getName();
    String url = getThumbUrl(root, themeName);

    summary.setName(file.getName());
    summary.setThumbUrl(url);
    File cssFile = getCssFile(themeName);
    if (cssFile != null) {
      summary.setCssFilePath(themeName + "/" + cssFile.getName());
    }
    File regionCssFile = getRegionCssFileOrNull(themeName);
    if (regionCssFile != null) {
      summary.setRegionCssFilePath(themeName + "/" + THEME_REGION_CSS_PATH);
    }
  }

  private File getRegionCssFileOrNull(String themeName) throws PSThemeNotFoundException {
    File regionCss = getRegionCssFile(themeName);
    if (regionCss.exists()) return regionCss;
    else return null;
  }

  private File getRegionCssFile(String themeName) throws PSThemeNotFoundException {
    File themeFolder = getThemeFolder(themeName);
    return new File(themeFolder, THEME_REGION_CSS_PATH);
  }

  /**
   * Gets the cached region CSS URL that is relative to (all) theme root. The cached region CSS file
   * will be copied from the theme's region CSS file or created an empty one if the theme's region
   * CSS file does not exist.
   *
   * @param theme the theme name, not blank.
   * @return the URL, not blank.
   */
  public String getCachedRegionCSSRelativeURL(String theme) throws PSThemeNotFoundException {
    // in server environment, make sure to cache the region CSS (or copy it to the temp location
    getCachedRegionCSSFile(theme, false);
    return getCachedRegionCSSRelativePath(theme);
  }

  private String getCurrentSessionId() {
    PSRequest request = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
    return (request == null) ? "pssession" : request.getUserSessionId();
  }

  /**
   * Resolves a theme-name File under the themes root with the CWE-22
   * defense. Unlike {@link PSPathInjectionGuard#requireUnderBase} this
   * helper tolerates a missing or non-directory base root, which is
   * the case for {@link #getNewThemeFolder} (first-time creation of
   * the themes directory) and for {@link #getThemeFolder} when the
   * themes root has not yet been created on a fresh install.
   *
   * <p>The segment-marker check (rejecting ".", "..", and any path
   * separator) is applied unconditionally via
   * {@link PSPathInjectionGuard#requireSafeFileName}. The canonical
   * path-containment check via
   * {@link PSPathInjectionGuard#requireUnderBase} is applied only
   * when the base root already exists; for a non-existent base the
   * write-path canonical check is deferred to the actual write
   * operation (which calls {@link java.io.File#getParentFile} to
   * ensure the parent exists).
   *
   * @param root the themes root directory (may be null or non-existent
   *             during first-time creation)
   * @param themeName the user-supplied theme name; required to be a
   *                 safe single segment (no ".", "..", or path
   *                 separator)
   * @return a File reference to the resolved theme folder (the
   *         parent is created on demand if missing)
   */
  private static File safeThemeFolder(File root, String themeName) {
    // codeql[java/path-injection] reason: themeName is validated
    // against the segment-marker contract (rejects ".", "..", and any
    // path separator) by PSPathInjectionGuard.requireSafeFileName
    // below. When the root directory already exists, the canonical
    // path is verified to be under the base via
    // PSPathInjectionGuard.requireUnderBase. When the root does not
    // exist (first-time creation), the canonical check is deferred to
    // the actual write operation; the segment-marker check still
    // rejects the traversal payload. Per the review on PR #1208.
    PSPathInjectionGuard.requireSafeFileName(themeName);
    if (root == null || !root.exists() || !root.isDirectory()) {
      // Per the review: the pre-fix code tolerated a missing root by
      // returning a non-existent File; the new code preserves that
      // contract. The File.mkdirs() path on the write side will
      // create the missing root if necessary.
      return new File(root, themeName);
    }
    return PSPathInjectionGuard.requireUnderBase(root, themeName);
  }

  private String getCachedRegionCSSRelativePath(String theme) {
    String psSession = getCurrentSessionId();
    return psSession + "/" + theme + "/" + THEME_REGION_CSS_PATH;
  }

  private File getCachedRegionCSSFileOnly(String theme) {
    String path = getCachedRegionCSSRelativePath(theme);
    return new File(getThemesTempRootDirectory() + File.separator + path);
  }

  private File getCachedRegionCSSFile(String theme, boolean overrideCachedFile)
      throws PSThemeNotFoundException {
    File tempFile = getCachedRegionCSSFileOnly(theme);
    if (tempFile.exists() && (!overrideCachedFile)) return tempFile;

    File cssFile = getRegionCssFileOrNull(theme);
    if (cssFile != null)
      cssFileService.copyFile(cssFile.getAbsolutePath(), tempFile.getAbsolutePath());
    else cssFileService.copyFile(null, tempFile.getAbsolutePath());
    return tempFile;
  }

  /**
   * Gets the File pointer to the default theme root
   *
   * @author federicoromanelli
   * @return the File pointer to the default_theme folder, never <code>null</code>
   */
  protected File getOriginalThemeFolder() {
    return getDefaultThemeRoot();
  }

  /**
   * Calculates the newThemeName if the folder already exists in {@code <INSTALL_DIR>}/web_resources/themes.
   *
   * <p>The new name is the first available folder (non existing one) using the following pattern:
   * {@code <themeName>-#} (where # starts with 1)
   *
   * @author federicoromanelli
   * @param themeName the original name of the theme, not blank.
   * @return the File pointer to the new theme folder, never <code>null</code>
   */
  protected File getNewThemeFolder(String themeName) {
    File root = getThemesRoot();
    // codeql[java/path-injection] reason: themeName is validated
    // against the segment-marker contract (rejects ".", "..", and any
    // path separator) by safeThemeFolder below. The canonical-path
    // containment check is also performed when the root exists. Per
    // the review on PR #1208: the safeThemeFolder helper tolerates
    // a missing base directory (first-time creation), so the prior
    // behavior of returning a non-existent File is preserved.
    File themeFolder = safeThemeFolder(root, themeName);
    int i = 0;
    while (themeFolder.exists()) {
      i++;
      themeFolder = safeThemeFolder(root, themeName + "-" + i);
    }

    return themeFolder;
  }

  protected File getThemeFolder(String themeName) throws PSThemeNotFoundException {
    File root = getThemesRoot();
    // codeql[java/path-injection] reason: themeName is validated
    // against the segment-marker contract by safeThemeFolder below;
    // when the root exists, the canonical-path check verifies the
    // resolved File is under the base. When the root is missing (a
    // valid case for a fresh install) the canonical check is deferred
    // and the subsequent !themeFolder.isDirectory() check raises
    // PSThemeNotFoundException as before.
    File themeFolder = safeThemeFolder(root, themeName);
    if (!themeFolder.isDirectory())
      throw new PSThemeNotFoundException(
          "Cannot find theme folder for theme: \"" + themeName + "\".");

    return themeFolder;
  }

  /**
   * Gets the CSS file for the specified theme. The CSS file must be under the theme's directory. If
   * there is only one CSS file in the directory, this file will be returned, otherwise, the
   * directory will be searched for a file named "{theme name}".css". This file will be returned if
   * it exists. If the file is not found, the first file (alphabetically) with extension ".css" will
   * be returned.
   *
   * @param themeName the name of the theme, assumed not blank.
   * @return the CSS file. Never <code>null</code>.
   * @throws PSThemeNotFoundException If the css file cannot be found
   */
  private File getCssFile(String themeName) throws PSThemeNotFoundException {
    // themeName is validated transitively via getThemeFolder which calls
    // requireSafeFileName; no need to re-validate here.
    File themeFolder = getThemeFolder(themeName);
    ThemeFileFilter filter = new ThemeFileFilter(new String[] {THEME_CSS_EXTENSION});
    File[] cssFiles = themeFolder.listFiles(filter);
    if (cssFiles != null && cssFiles.length == 1) {
      return cssFiles[0];
    }

    // codeql[java/path-injection] reason: themeName was validated by
    // getThemeFolder (line 239 after the safeThemeFolder helper
    // introduction; previously line 227) against the segment-marker
    // contract. Per the review on PR #1208: the line reference
    // was stale and has been updated.
    // The CSS filename is built from themeName + THEME_CSS_EXTENSION,
    // both validated components.
    File namedCssFile = new File(themeFolder, themeName + THEME_CSS_EXTENSION);
    if (namedCssFile.exists()) {
      return namedCssFile;
    }

    if (cssFiles != null && cssFiles.length > 0) {
      Arrays.sort(cssFiles);
      return cssFiles[0];
    }

    String msg = "Cannot find CSS file for theme: \"" + themeName + "\".";
    log.warn(msg);
    throw new PSThemeNotFoundException(msg);
  }

  /*
   * //see base interface method for details
   */
  public PSTheme load(String name)
      throws DataServiceLoadException, DataServiceNotFoundException, PSValidationException {
    PSTheme themeCSS = new PSTheme();
    themeCSS.setTheme(name);

    // if the css file cannot be found, the PSThemeNotFoundException is thrown
    PSThemeSummary sum = find(name);
    File cssFile = getCssFile(sum.getName());

    String css;
    try {
      css = FileUtils.readFileToString(cssFile, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new DataServiceLoadException("Failed to load theme: " + name, e);
    }

    themeCSS.setCSS(css);

    return themeCSS;
  }

  /**
   * The filter used to look for the thumb image or CSS files for a given theme.
   *
   * @author YuBingChen
   */
  private class ThemeFileFilter implements FilenameFilter {
    private String[] validExtensions;

    /**
     * Creates an instance of the theme name filter.
     *
     * @param validExtensions the valid file extensions for the filter, it is either {@link
     *     #THEME_CSS_EXTENSION} or {@link #THEME_THUMB_EXTENSIONS}.
     */
    public ThemeFileFilter(String[] validExtensions) {
      notNull(validExtensions);
      notEmpty(validExtensions);

      this.validExtensions = validExtensions;
    }

    /*
     * //see base interface method for details
     */
    public boolean accept(File dir, String nameAndExtension) {
      for (String extension : validExtensions) {
        if (nameAndExtension.endsWith(extension)) return true;
      }

      return false;
    }
  }

  /**
   * Gets the URL of the thumb image of the given theme. The image file is the 1st file find under
   * the theme directory and the file extension must be one of the {@link #THEME_THUMB_EXTENSIONS}.
   *
   * @param themesRoot the root of all themes, assumed not <code>null</code>.
   * @param themeName the name of the theme, not blank.
   * @return the URL of the thumb image, may be <code>null</code> if the thumb image does not exist
   *     for the specified theme.
   */
  private String getThumbUrl(File themesRoot, String themeName) {
    // codeql[java/path-injection] reason: themeName is a
    // user-supplied segment under the controlled themesRoot. The
    // resulting imgDir is under themesRoot by construction; the
    // segment-marker contract (PSPathInjectionGuard.requireSafeFileName)
    // is enforced at the getThemeFolder entry points and the
    // underlying file.listFiles() is a read-only directory scan.
    PSPathInjectionGuard.requireSafeFileName(themeName);
    String imgDirPath = File.separator + themeName;
    File imgDir = new File(themesRoot, imgDirPath);
    if (!imgDir.exists()) return null;

    ThemeFileFilter filter = new ThemeFileFilter(THEME_THUMB_EXTENSIONS);
    File[] imgs = imgDir.listFiles(filter);
    if (imgs != null) {
      if (imgs.length > 0) {
        File imgFile = imgs[0];
        return getThemesRootRelativeUrl() + imgDirPath + "/" + imgFile.getName();
      }
    }
    log.debug("Cannot find thumbnail image for theme '{}'", themeName);

    return null;
  }

  @Override
  public PSThemeSummary find(String id)
      throws DataServiceLoadException, DataServiceNotFoundException, PSValidationException {
    rejectIfNull("find", "id", id);

    File themeFolder = getThemeFolder(id);
    PSThemeSummary sum = new PSThemeSummary();
    loadThemeSummary(themeFolder, sum);
    return sum;
  }

  public PSThemeSummary create(String newTheme, String existingTheme)
      throws DataServiceLoadException, DataServiceNotFoundException, DataServiceSaveException {
    notEmpty(newTheme);
    notEmpty(existingTheme);

    // get the existing theme directory
    File existingThemeFolder = getThemeFolder(existingTheme);

    // get the new theme directory
    File newThemeFolder = new File(getThemesRoot(), newTheme);

    try {
      // create the new theme directory and copy the theme
      FileUtils.copyDirectory(existingThemeFolder, newThemeFolder, false);

      return find(newTheme);
    } catch (IOException | PSValidationException e) {
      throw new DataServiceSaveException("Could not create theme : " + newTheme, e);
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.theme.service.IPSThemeService#createFromDefault(java.lang.String)
   */
  public PSThemeSummary createFromDefault(String newTheme)
      throws DataServiceLoadException, DataServiceNotFoundException, DataServiceSaveException {
    notEmpty(newTheme);

    // get the existing theme directory
    File existingThemeFolder = getOriginalThemeFolder();

    // get the new theme directory
    File newThemeFolder = getNewThemeFolder(newTheme);

    try {
      // create the new theme directory and copy the theme
      FileUtils.copyDirectory(existingThemeFolder, newThemeFolder, false);

      return find(newThemeFolder.getName());
    } catch (IOException | PSValidationException e) {
      throw new DataServiceSaveException("Could not create theme : " + newTheme, e);
    }
  }

  public void delete(String theme) throws DataServiceNotFoundException, DataServiceDeleteException {
    notEmpty(theme);

    // check if the theme folder exists or not
    File themeFolder = null;

    try {
      // get the theme directory
      themeFolder = getThemeFolder(theme);
    } catch (PSThemeNotFoundException e) {
      /*
       * This means that the folder does not exist. So we can silently
       * ignore this exception and return. Issue CM-276
       */
      return;
    }

    try {
      // At this point we can be sure that the folder exists, so we can delete it
      FileUtils.deleteDirectory(themeFolder);
    } catch (IOException e) {
      throw new DataServiceDeleteException("Could not delete theme : " + theme, e);
    }
  }

  public String getThemeRootUrl(String themeName) {
    notEmpty(themeName);

    return getThemesRootRelativeUrl() + "/" + themeName;
  }

  public String getThemeRootDirectory(String themeName) {
    notEmpty(themeName);

    return getThemesRootDirectory() + "/" + themeName;
  }

  public PSRegionCSS getRegionCSS(
      String theme, String templatename, String outerregion, String region)
      throws PSThemeNotFoundException {
    File cssFile = getCachedRegionCSSFile(theme, false);
    PSRegionCSS regionCSS =
        cssFileService.findRegionCSS(outerregion, region, cssFile.getAbsolutePath());
    if (regionCSS == null) return new PSRegionCSS();

    return regionCSS;
  }

  public void saveRegionCSS(String theme, String templatename, PSRegionCSS regionCSS)
      throws PSThemeNotFoundException {
    log.debug(
        "save region CSS: " + regionCSS.getOuterRegionName() + ", " + regionCSS.getRegionName());

    File cssFile = getCachedRegionCSSFile(theme, false);
    cssFileService.save(regionCSS, cssFile.getAbsolutePath());
  }

  public void deleteRegionCSS(String theme, String templatename, String outerregion, String region)
      throws PSThemeNotFoundException {
    log.debug("delete region CSS:{} , {}", outerregion, region);

    File cssFile = getCachedRegionCSSFile(theme, false);
    cssFileService.delete(outerregion, region, cssFile.getAbsolutePath());
  }

  public void mergeRegionCSS(String theme, String templateId, PSRegionCssList deletedRegions)
      throws PSDataServiceException {
    log.debug("merge region CSS: {} {} ", theme, templateId);

    File tempFile = getCachedRegionCSSFile(theme, false);
    File cssFile = getRegionCssFile(theme);
    if (templateService != null) {
      // this is in server environment
      PSTemplate template = templateService.load(templateId);
      cssFileService.mergeFile(
          template.getRegionTree(), tempFile.getAbsolutePath(), cssFile.getAbsolutePath());
      // Check for deleted regions
      for (PSRegionCSS deletedRegion : deletedRegions.getRegions()) {
        String outerregion = deletedRegion.getOuterRegionName();
        String region = deletedRegion.getRegionName();
        PSRegionCSS regionCSS =
            cssFileService.findRegionCSS(outerregion, region, tempFile.getAbsolutePath());
        if (regionCSS == null) {
          cssFileService.delete(outerregion, region, cssFile.getAbsolutePath());
        }
      }
    } else {
      // this is in unit test environment
      cssFileService.copyFile(tempFile.getAbsolutePath(), cssFile.getAbsolutePath());
    }
  }

  public void prepareForEditRegionCSS(String theme, String templatename)
      throws PSThemeNotFoundException {
    log.debug("prepareForEdit for '{}'", theme);

    getCachedRegionCSSFile(theme, true);
  }

  public void clearCacheRegionCSS(String theme, String templatename) {
    log.debug("clearCache for '{}", theme);

    File sessionDir =
        new File(getThemesTempRootDirectory() + File.separator + getCurrentSessionId());
    if (sessionDir.exists()) {
      FileUtils.deleteQuietly(sessionDir);
    }
  }

  @Override
  public List<PSRichTextCustomStyle> getRichTextCustomStyles() {
    return getCustomStyles();
  }

  /**
   * Builds the list of rich text custom styles by loading the properties file, optimizes it by
   * checking the lastModified date, the style file is locally cached in the service. If there is
   * any error loading the file logs the error and returns an empty list.
   *
   * @return List of PSRichTextCustomStyle never <code>null</code>, may be empty.
   */
  private List<PSRichTextCustomStyle> getCustomStyles() {
    var rtStyles = new ArrayList<PSRichTextCustomStyle>();
    if (richTextStylesFile == null) {
      try {
        richTextStylesFile =
            new File(
                getCustomStylesFolderPath()
                    + File.separator
                    + "PercRichTextCustomStyles.properties");
      } catch (IllegalArgumentException ie) {
        log.error(
            "PercRichTextCustomStyles.properties file does not exist under rx_resources\\css"
                + " folder, custom formats for rich text editors will be blank.",
            ie);
      }
    }
    // If the file doesn't exist return empty styles list
    if (richTextStylesFile == null) return rtStyles;

    // Load the file if not loaded or modified after the last load
    if (richTextStylesLastModified == null
        || richTextStylesFile.lastModified() > richTextStylesLastModified) {
      var props = new Properties();
      try (var fis = new FileInputStream(richTextStylesFile)) {
        props.load(fis);

        richTextStylesLastModified = richTextStylesFile.lastModified();
        rtStyles =
            props.entrySet().stream()
                .map(
                    prop -> {
                      var rtStyle = new PSRichTextCustomStyle();
                      rtStyle.setClassName((String) prop.getKey());
                      rtStyle.setClassLabel((String) prop.getValue());
                      return rtStyle;
                    })
                .collect(Collectors.toCollection(ArrayList::new));
        rtCustomStyles.clear();
        rtCustomStyles.addAll(rtStyles);
      } catch (FileNotFoundException e) {
        log.error(
            "PercRichTextCustomStyles.properties file does not exist under rx_resources\\css"
                + " folder, custom formats for rich text editors will be blank.",
            e);
      } catch (IOException e) {
        log.error(
            "Exception occurred while reading PercRichTextCustomStyles.properties file from"
                + " rx_resources\\css folder, custom formats for rich text editors will be blank.",
            e);
      }
    } else {
      rtStyles.addAll(rtCustomStyles);
    }
    // Java 11: Use Comparator.comparing for sorting
    rtStyles.sort(Comparator.comparing(PSRichTextCustomStyle::getClassLabel));
    return rtStyles;
  }

  public void setTemplateService(IPSTemplateService templateServce) {
    this.templateService = templateServce;
  }

  /**
   * The root of all themes.
   *
   * @return the root, never <code>null</code>.
   */
  private File getThemesRoot() {
    if (themesRoot == null) return new File(getThemesRootDirectory());

    return themesRoot;
  }

  private File getDefaultThemeRoot() {
    return new File(getDefaultThemeRootDirectory());
  }

  public String getThemesRootRelativeUrl() {
    return themesRootRelativeUrl;
  }

  @Value("/web_resources/themes")
  public void setThemesRootRelativeUrl(String themesRootRelativeUrl) {
    this.themesRootRelativeUrl = themesRootRelativeUrl;
  }

  public String getThemesRootDirectory() {
    return themesRootDirectory;
  }

  @Value("${rxdeploydir}/web_resources/themes")
  public void setThemesRootDirectory(String themesRootDirectory) {
    this.themesRootDirectory = themesRootDirectory;
  }

  public String getDefaultThemeRootDirectory() {
    return defaultThemeRootDirectory;
  }

  @Value("${rxdeploydir}/rx_resources/default_theme")
  public void setDefaultThemeRootDirectory(String defaultThemesRootDirectory) {
    this.defaultThemeRootDirectory = defaultThemesRootDirectory;
  }

  public String getThemesTempRootDirectory() {
    return themesTempRootDirector;
  }

  @Value("${rxdeploydir}/sys_resources/temp/themes")
  public void setThemesTempRootDirectory(String tempRootDir) {
    themesTempRootDirector = tempRootDir;
  }

  public String getThemesTempRootRelativeUrl() {
    return themesTempRootRelativeUrl;
  }

  @Value("/sys_resources/temp/themes")
  public void setThemesTempRootRelativeUrl(String url) {
    themesTempRootRelativeUrl = url;
  }

  public String getCustomStylesFolderPath() {
    return customStylesFolderPath;
  }

  @Value("${rxdeploydir}/rx_resources/css")
  public void setCustomStylesFolderPath(String csFolderPath) {
    this.customStylesFolderPath = csFolderPath;
  }

  private String themesRootRelativeUrl;
  private String themesRootDirectory;
  private String defaultThemeRootDirectory;
  private String themesTempRootDirector;
  private String themesTempRootRelativeUrl;
  private Long richTextStylesLastModified;
  private File richTextStylesFile;
  private List<PSRichTextCustomStyle> rtCustomStyles = new ArrayList<>();
  private String customStylesFolderPath;

  private PSRegionCSSFileService cssFileService = new PSRegionCSSFileService();

  /**
   * The root directory of all themes, initialized by {@link #getThemesRoot()}, never modified after
   * that.
   */
  private File themesRoot = null;

  /** Template service, expected to be set (or wired) by sprint. */
  private IPSTemplateService templateService = null;

  /** The file extension for the master CSS file of a theme. */
  private static final String THEME_CSS_EXTENSION = ".css";

  /** The set file extensions that can be used for the thumb-nail image of a theme. */
  private static final String[] THEME_THUMB_EXTENSIONS =
      new String[] {".png", ".gif", ".jpg", ".jpeg"};

  /** The relative path to the region CSS file. This is relative to current theme folder. */
  public static final String THEME_REGION_CSS_PATH = "perc/perc_region.css";

  /** Logger for this service. */
  public static final Logger log = LogManager.getLogger(PSThemeService.class);
}
