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
package com.percussion.rxfix;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.rx.ui.PSHelpTopicMapping;
import com.percussion.rxfix.dbfixes.*;
import com.percussion.server.IPSStartupProcessManager;
import com.percussion.server.PSStartupProcessManager;
import com.percussion.server.cache.PSCacheManager;
import com.percussion.server.cache.PSCacheProxy;
import com.percussion.util.PSCacheException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A framework program that runs a series of fixup modules for a Rhythmyx
 * installation. The modules are run in order, when you add a module, please
 * consider its position in the overall list. Each module implements
 * {@link IPSFix}.
 *
 */
public class PSRxFix
{
   private static final Logger log = LogManager.getLogger(PSRxFix.class);

   /**
    * Represents an entry in the UI model.
    *
    * <p>This inner class follows Java 11 best practices with proper encapsulation
    * and type safety.
    */
   public static class Entry
   {
      private boolean dofix;
      private String fixname;
      private Class<? extends IPSFix> fix;
      private List<PSFixResult> results;

      /**
       * Creates a new Entry for the specified fix class.
       *
       * @param fixClass fix class that must implement {@link IPSFix}
       * @throws IllegalArgumentException if the fix class cannot be instantiated
       */
      public Entry(Class<? extends IPSFix> fixClass) {
         try {
            this.fix = fixClass;
            var fixInstance = this.fix.getDeclaredConstructor().newInstance();
            this.fixname = fixInstance.getOperation();
            this.dofix = true;
         } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate fix class: " + fixClass.getName(), e);
         }
      }

      /**
       * @return true if this fix should be executed
       */
      public boolean isDofix() {
         return dofix;
      }

      /**
       * @param dofix whether this fix should be executed
       */
      public void setDofix(boolean dofix) {
         this.dofix = dofix;
      }

      /**
       * @return the fix class
       */
      public Class<? extends IPSFix> getFix() {
         return fix;
      }

      /**
       * @param fix the fix class to set
       */
      public void setFix(Class<? extends IPSFix> fix) {
         this.fix = fix;
      }

      /**
       * @return the fix name
       */
      public String getFixname() {
         return fixname;
      }

      /**
       * @param fixname the fix name to set
       */
      public void setFixname(String fixname) {
         this.fixname = fixname;
      }

      /**
       * @return the results, may be null
       */
      public List<PSFixResult> getResults() {
         return results;
      }

      /**
       * @param results the results to set
       */
      public void setResults(List<PSFixResult> results) {
         this.results = results;
      }
   }

   /**
    * Set after the preview has been done, guards the page flow
    */
   private boolean previewDone = false;

   /**
    * Set after the fix run has been done, used in page flow
    */
   private boolean fixDone = false;

   /**
    * The array of fixes that exist. The order of these fixes is important.
    * Using List instead of array for better type safety and modern Java practices.
    */
   private final List<Class<? extends IPSFix>> fixes = Arrays.asList(
      PSFixNextNumberTable.class,
      PSFixContentStatusHistory.class,
      //PSFixContentStatusHistoryWFInfo.class,
      PSFixOrphanedSlots.class,
      PSFixBrokenRelationships.class,
      // PSFixOrphanedData.class, omitted since the data is missing
      PSFixInvalidFolders.class,
      PSFixOrphanedFolders.class,
      PSFixInvalidFolderRelationships.class,
      PSFixDanglingAssociations.class,
      PSFixCommunityVisibilityForViews.class,
      PSFixTranslationRelationships.class,
      PSFixInvalidSysTitle.class,
      PSFixAllowedSitePropertiesWithBadSites.class,
      PSFixOrphanedContentChangeEvents.class,
      PSFixZerosInRelationshipProperties.class,
      PSFixOrphanedManagedLinks.class,
      PSFixStaleDataForContentTypes.class,
      PSFixPageCatalog.class,
      PSFixAcls.class,
      PSFixFormUrl.class,
      PSFixWidgetVisibility.class
   );

   /**
    * These entries dictate what to do for each fix. The data is presented and
    * modified in the UI as the model, and is used directly in the doFix call.
    * Initialized on reset or construction, and never {@code null} after.
    */
   private List<Entry> entries;

   /**
    * Creates a new PSRxFix instance.
    *
    * @throws IllegalStateException if initialization fails
    */
   public PSRxFix() {
      try {
         init();
      } catch (Exception e) {
         throw new IllegalStateException("Failed to initialize PSRxFix", e);
      }
   }

   /**
    * Initializes the state of this PSRxFix instance.
    * Uses Java 11 stream operations for modern collection processing.
    */
   private void init() {
      this.previewDone = false;
      this.fixDone = false;

      // Use streams for modern Java collection processing
      this.entries = fixes.stream()
         .map(Entry::new)
         .peek(entry -> entry.setResults(null))
         .collect(Collectors.toList());
   }

   /**
    * @return true if preview has been done
    */
   public boolean isPreviewDone() {
      return previewDone;
   }

   /**
    * @return true if fix has been done
    */
   public boolean isFixDone() {
      return fixDone;
   }

   /**
    * @param fixDone whether fix has been done
    */
   public void setFixDone(boolean fixDone) {
      this.fixDone = fixDone;
   }

   /**
    * @param previewDone whether preview has been done
    */
   public void setPreviewDone(boolean previewDone) {
      this.previewDone = previewDone;
   }

   /**
    * Gets all entries, which include result data.
    *
    * @return the entries, never {@code null}
    */
   public List<Entry> getEntries() {
      return Optional.ofNullable(entries).orElse(List.of());
   }

   /**
    * Gets only those entries that were actually run.
    * Uses Java 11 stream operations for filtering.
    *
    * @return the entries that should be executed, never {@code null}
    */
   public List<Entry> getRunentries() {
      return Optional.ofNullable(entries)
         .orElse(List.of())
         .stream()
         .filter(Entry::isDofix)
         .collect(Collectors.toList());
   }

   /**
    * Preview action - runs fixes in preview mode.
    *
    * @return the outcome
    */
   public String preview() {
      try {
         doFix(true);
      } catch (Exception e) {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
      return "admin-rxfix-preview";
   }

   /**
    * Fix action - runs fixes in actual fix mode.
    *
    * @return the outcome
    */
   public String next() {
      if (fixDone) {
         return "admin-rxfix";
      }

      try {
         doFix(false);
      } catch (Exception e) {
         log.error("PSRXFix Failed: {}", PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
      return "admin-rxfix-preview";
   }

   /**
    * Gets the UI label for next action.
    *
    * @return the label for the "next" button on the results page
    */
   public String getFixnextlabel() {
      return fixDone ? "Done" : "Fix";
   }

   /**
    * Reset action - reinitializes the fix state.
    *
    * @return outcome
    */
   public String reset() {
      try {
         init();
         return "reset";
      } catch (Exception e) {
         log.error("Failed to reset PSRxFix", e);
         throw new IllegalStateException("Reset failed", e);
      }
   }

   /**
    * Startup and do one or more fixes.
    *
    * @param preview Run the fixups in preview mode
    * @throws IllegalStateException if there is a problem setting up to perform the fixes
    */
   public void doFix(boolean preview) {
      doFix(preview, null);
   }

   /**
    * Startup and do one or more fixes with optional startup process manager.

    *
    * @param preview Run the fixups in preview mode
    * @param startupProcessManager Optional startup process manager
    * @throws IllegalStateException if there is a problem setting up to perform the fixes
    */
   public void doFix(boolean preview, IPSStartupProcessManager startupProcessManager) {
      var clearCache = false;

      // Use enhanced for-each loop with better exception handling
      for (var entry : getRunentries()) {
         try {
            // Instantiate fix using modern reflection practices
            var fixInstance = entry.getFix().getDeclaredConstructor().newInstance();
            log.info("Executing update {} in Preview mode: {}", fixInstance.toString(), preview);

            fixInstance.fix(preview);

            // Handle startup process removal if applicable
            if (startupProcessManager instanceof PSStartupProcessManager && fixInstance.removeStartupOnSuccess()) {
               ((PSStartupProcessManager) startupProcessManager)
                  .removeStartupProcess(fixInstance.getClass().getSimpleName());
            }

            // Get results and update cache flag
            var results = fixInstance.getResults();
            if (!clearCache) {
               clearCache = !results.isEmpty();
            }
            entry.setResults(results);

         } catch (Exception e) {
            log.error("Failed to execute fix: {}", entry.getFixname(), e);
            // Continue with other fixes rather than failing completely
         }
      }

      // Only clear the cache if there was data changed
      if (PSCacheManager.isAvailable() && clearCache && !preview) {
         try {
            var cacheManager = PSCacheManager.getInstance();
            cacheManager.flush();
            PSCacheProxy.flushFolderCache();
         } catch (PSCacheException e) {
            log.warn("Failed to clear cache after successful fixes", e);
         }
      }

      this.previewDone = preview;
      this.fixDone = !preview;
   }

   /**
    * Get the help file name for the RxFix page.
    *
    * @return the help file name, never {@code null} or empty
    */
   public String getHelpFile() {
      return PSHelpTopicMapping.getFileName("RxFix");
   }
}
