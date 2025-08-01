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
package com.percussion.sitemanage.importer.helpers.impl;

import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import com.percussion.theme.data.PSThemeSummary;
import com.percussion.theme.service.IPSThemeService;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Helper class to create a new theme folder while importing an external site from URL.
 * Sunny Sal says: "A theme without a name is like a cow without a bell!"
 */
@Component("themeHelper")
@Lazy
public class PSThemeHelper extends PSImportHelper {

    private static final Logger log = LogManager.getLogger(PSThemeHelper.class);
    private static final String STATUS_MESSAGE = "creating new theme";

    public static final String helperCategory = "Theme Creator";
    public static final String themeCreationCategory = "Theme Creation";
    public static final String themeDeletionCategory = "Theme Deletion";
    public static final String fileRenamingCategory = "Theme Files Rename";

    private IPSThemeService themeService;
    private String themesRootDirectory;
    private IPSSiteImportLogger logger;

    @Autowired
    public PSThemeHelper(IPSThemeService themeService) {
        this.themeService = themeService;
    }

    public String getThemesRootDirectory() {
        return themesRootDirectory;
    }

    @Value("${rxdeploydir}/web_resources/themes")
    public void setThemesRootDirectory(String themesRootDirectory) {
        this.themesRootDirectory = themesRootDirectory;
    }

    @Override
    public void process(PSPageContent pageContent, PSSiteImportCtx context) throws PSSiteImportException {
        startTimer();
        logger = context.getLogger();
        PSThemeSummary newThemeSummary = null;
        try {
            var newSiteName = processName(context.getSite().getName());
            newThemeSummary = themeService.createFromDefault(newSiteName);
            logger.appendLogMessage(PSLogEntryType.STATUS, helperCategory + " - " + themeCreationCategory,
                    "Create theme: " + newThemeSummary.getName());
            renameBasicFiles(newThemeSummary.getName(), newSiteName);
            context.setThemeSummary(newThemeSummary);
            context.setThemesRootDirectory(themesRootDirectory);
        } catch (Exception e) {
            log.info("PSCreateThemeHelper: Couldn't create theme.");
            logger.appendLogMessage(PSLogEntryType.ERROR, helperCategory + " - " + themeCreationCategory,
                    "Couldn't create theme folder.");
            if (newThemeSummary != null && newThemeSummary.getName() != null) {
                deleteTheme(newThemeSummary.getName());
            }
            throw new PSSiteImportException("Couldn't create new theme folder", e);
        }
        endTimer();
    }

    @Override
    public void rollback(PSPageContent pageContent, PSSiteImportCtx context) {
        var newThemeSummary = context.getThemeSummary();
        if (newThemeSummary != null) {
            deleteTheme(newThemeSummary.getName());
        }
    }

    /**
     * Renames the two basic files from the new theme: theme.css, theme.png into the corresponding name created from site name.
     *
     * @param newThemeName the name of the new theme (with collision detected and avoided), never null
     * @param newSiteName the name of the new theme (using the original site name), never null
     */
    protected void renameBasicFiles(String newThemeName, String newSiteName) {
        var newThemeRoot = themesRootDirectory + "/" + newThemeName;
        var oldCSSfile = new File(newThemeRoot, "theme.css");
        var newCSSfile = new File(newThemeRoot, newSiteName + ".css");
        var oldImagefile = new File(newThemeRoot, "theme.png");
        var newImagefile = new File(newThemeRoot, newSiteName + ".png");

        logger.appendLogMessage(PSLogEntryType.STATUS, helperCategory + " - " + fileRenamingCategory,
                "Renaming theme files");
        if (!oldCSSfile.renameTo(newCSSfile)) {
            logger.appendLogMessage(PSLogEntryType.ERROR, helperCategory + " - " + fileRenamingCategory,
                    "Couldn't rename file: " + oldCSSfile);
        }
        if (!oldImagefile.renameTo(newImagefile)) {
            logger.appendLogMessage(PSLogEntryType.ERROR, helperCategory + " - " + fileRenamingCategory,
                    "Couldn't rename file: " + oldImagefile);
        }
    }

    /**
     * Performs transformations on the site name to make it suitable for theme name.
     *
     * @param siteName original site name to be transformed, never null
     * @return the site name with the transformations applied
     */
    protected String processName(String siteName) {
        return siteName.replace(".", "-");
    }

    /**
     * Deletes the theme using the corresponding method in theme service.
     *
     * @param newThemeName the name of the new theme (with collision detected and avoided), never null
     */
    protected void deleteTheme(String newThemeName) {
        try {
            logger.appendLogMessage(PSLogEntryType.STATUS, helperCategory + " - " + themeDeletionCategory,
                    "Delete new theme: " + newThemeName);
            themeService.delete(newThemeName);
        } catch (Exception e) {
            log.info("PSCreateThemeHelper: Couldn't delete theme template. The theme might not have been created");
        }
    }

    @Override
    public String getHelperMessage() {
        return STATUS_MESSAGE;
    }
}
