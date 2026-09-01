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

package com.percussion.apibridge;

import com.percussion.cms.IPSConstants;
import com.percussion.rest.sites.ISiteAdaptor;
import com.percussion.rest.sites.Site;
import com.percussion.rest.sites.SiteList;
import com.percussion.rest.sites.VirtualSiteBuildRequest;
import com.percussion.rest.sites.VirtualSiteBuildResult;
import com.percussion.rest.sites.VirtualSitePreviewFile;
import com.percussion.rest.sites.VirtualSitePreviewStatus;
import com.percussion.rest.sites.VirtualSiteProperties;
import com.percussion.rest.sites.VirtualSitePublishResult;
import com.percussion.server.PSServer;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.services.sitemgr.data.PSSite;
import com.percussion.services.virtualsite.PSGitRemoteCheckout;
import com.percussion.services.virtualsite.PSInMemoryVirtualParticipantService;
import com.percussion.services.virtualsite.PSVirtualSiteBuildResult;
import com.percussion.services.virtualsite.PSVirtualSiteBuildService;
import com.percussion.services.virtualsite.PSVirtualSiteFilesystemPublisher;
import com.percussion.services.virtualsite.PSManagedNavSiteHelper;
import com.percussion.services.virtualsite.PSVirtualSiteHelper;
import com.percussion.services.virtualsite.VirtualSiteConfig;
import com.percussion.services.virtualsite.VirtualSiteConfigLoader;
import com.percussion.services.virtualsite.VirtualSiteException;
import com.percussion.services.virtualsite.VirtualSiteSourceType;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.sitemanage.service.IPSSiteSectionService;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.publishing.IPSPublishingWs;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/** Adaptor for managing sites in Percussion CMS, including Virtual Site properties and build. */
@PSSiteManageBean
@Lazy
public class SitesAdaptor implements ISiteAdaptor {
  private static final Logger log = LogManager.getLogger(IPSConstants.API_LOG);

  /** Preferred default context name when creating new virtual.* properties. */
  static final String DEFAULT_PROPERTY_CONTEXT = "Preview";

  /** Max link-problem / written-file lines returned on the wire (full report is on disk). */
  static final int MAX_RESULT_LINES = 200;

  /** Sidecar under default output {@code _meta} pointing at the last successful build root. */
  static final String LAST_OUTPUT_POINTER_FILE = "last-output-root.txt";

  static final String MISSING_PREVIEW_MESSAGE =
      "No assembled Virtual Site to preview. Run Build Virtual Site first.";

  static final long MAX_PREVIEW_FILE_BYTES = 20L * 1024 * 1024;

  @Autowired private IPSPublishingWs publishingWs;

  @Autowired private IPSSiteDataService siteDataService;

  @Autowired private IPSSiteSectionService siteSectionService;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired private IPSUserService userService;

  private final IPSSiteManager siteManager;

  /** Admin gate; production uses {@link #isCurrentUserAdmin()}, tests inject allow/deny. */
  private final BooleanSupplier adminChecker;

  /** Maps siteKey → default output Path when the request omits outputRoot. */
  private final Function<String, Path> defaultOutputRootResolver;

  /**
   * Optional build runner for unit tests. When null, production constructs {@link
   * PSVirtualSiteBuildService} with a participant meta directory under the output root.
   */
  private final BuildRunner buildRunner;

  /** Git remote checkout before discover; tests may inject a stub. */
  private final PSGitRemoteCheckout gitRemoteCheckout;

  /** Functional hook for the static build (production or test double). */
  @FunctionalInterface
  interface BuildRunner {
    PSVirtualSiteBuildResult build(VirtualSiteConfig config, Path outputRoot) throws Exception;
  }

  /**
   * Default constructor (Spring / locator). Admin checks use {@link #isCurrentUserAdmin()} after
   * {@link IPSUserService} field injection.
   */
  public SitesAdaptor() {
    this(
        PSSiteManagerLocator.getSiteManager(),
        null,
        SitesAdaptor::defaultOutputRootForSiteKey,
        null);
  }

  /**
   * Test-friendly constructor (Admin allowed for non-build tests; production default output /
   * build).
   *
   * @param siteManager site manager, not null
   */
  public SitesAdaptor(IPSSiteManager siteManager) {
    this(siteManager, () -> true, SitesAdaptor::defaultOutputRootForSiteKey, null);
  }

  /**
   * Fully injectable constructor for unit tests.
   *
   * @param siteManager site manager
   * @param adminChecker returns true when caller is Admin; null uses {@link #isCurrentUserAdmin()}
   * @param defaultOutputRootResolver maps siteKey → default output Path
   * @param buildRunner optional override for the build step; null uses production service
   */
  SitesAdaptor(
      IPSSiteManager siteManager,
      BooleanSupplier adminChecker,
      Function<String, Path> defaultOutputRootResolver,
      BuildRunner buildRunner) {
    this(siteManager, adminChecker, defaultOutputRootResolver, buildRunner, null);
  }

  /**
   * Fully injectable constructor including Git remote checkout.
   *
   * @param gitRemoteCheckout null uses the production {@link PSGitRemoteCheckout}
   */
  SitesAdaptor(
      IPSSiteManager siteManager,
      BooleanSupplier adminChecker,
      Function<String, Path> defaultOutputRootResolver,
      BuildRunner buildRunner,
      PSGitRemoteCheckout gitRemoteCheckout) {
    this.siteManager = siteManager != null ? siteManager : PSSiteManagerLocator.getSiteManager();
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
    this.defaultOutputRootResolver =
        defaultOutputRootResolver != null
            ? defaultOutputRootResolver
            : SitesAdaptor::defaultOutputRootForSiteKey;
    this.buildRunner = buildRunner;
    this.gitRemoteCheckout =
        gitRemoteCheckout != null ? gitRemoteCheckout : new PSGitRemoteCheckout();
  }

  @Override
  public SiteList findAllSites() {
    var sites = siteDataService.findAll();
    return ApiUtils.convertSiteSummaryList(sites);
  }

  @Override
  public void saveSite(Site site) {
    // General site save remains a later slice; use updateVirtualSiteProperties for virtual.*.
    throw new WebApplicationException(
        "General site save is not implemented; use PUT /sites/{nameOrId}/virtual for Virtual Site"
            + " properties",
        Response.Status.NOT_IMPLEMENTED);
  }

  @Override
  public Site findByName(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    IPSSite site = siteManager.findSite(name.trim());
    return site == null ? null : toDetailSite(site);
  }

  @Override
  public Site findByGuid(String guid) {
    if (StringUtils.isBlank(guid)) {
      return null;
    }
    try {
      IPSGuid id = new PSGuid(guid.trim());
      IPSSite site = siteManager.findSite(id);
      return site == null ? null : toDetailSite(site);
    } catch (RuntimeException e) {
      log.debug("findByGuid: not a valid site guid '{}': {}", guid, e.getMessage());
      return null;
    }
  }

  @Override
  public void deleteSite(Site site) {
    throw new WebApplicationException(
        "Site delete is not implemented on this adaptor", Response.Status.NOT_IMPLEMENTED);
  }

  @Override
  public Site createSite() {
    throw new WebApplicationException(
        "Site create is not implemented on this adaptor", Response.Status.NOT_IMPLEMENTED);
  }

  @Override
  public VirtualSiteProperties getVirtualSiteProperties(String nameOrId) {
    IPSSite site = requireSite(nameOrId);
    return readVirtual(site);
  }

  @Override
  public VirtualSiteProperties updateVirtualSiteProperties(
      String nameOrId, VirtualSiteProperties props) {
    if (props == null) {
      throw new WebApplicationException(
          "VirtualSiteProperties body is required", Response.Status.BAD_REQUEST);
    }
    IPSSite found = requireSite(nameOrId);
    try {
      IPSSite modifiable = loadModifiable(found);
      if (!(modifiable instanceof PSSite psSite)) {
        throw new WebApplicationException(
            "Site entity does not support property bag", Response.Status.INTERNAL_SERVER_ERROR);
      }

      IPSGuid contextId = resolvePropertyContext(psSite);
      String sourceKind = blankToNull(props.getSourceKind());
      String rootPath = blankToNull(props.getRootPath());
      String configFile = blankToNull(props.getConfigFile());
      String siteKey = blankToNull(props.getSiteKey());

      // Clear virtual config when sourceKind is blank or explicit repository.
      if (sourceKind == null
          || PSVirtualSiteHelper.SOURCE_KIND_REPOSITORY.equalsIgnoreCase(sourceKind)) {
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_SOURCE_KIND, null);
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_ROOT_PATH, null);
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_CONFIG_FILE, null);
        PSVirtualSiteHelper.putProperty(psSite, contextId, PSVirtualSiteHelper.PROP_SITE_KEY, null);
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_REMOTE_URL, null);
        PSVirtualSiteHelper.putProperty(psSite, contextId, PSVirtualSiteHelper.PROP_BRANCH, null);
      } else {
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_SOURCE_KIND, sourceKind);
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_ROOT_PATH, rootPath);
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_CONFIG_FILE, configFile);
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_SITE_KEY, siteKey);
        // Null (omitted on the wire) keeps an existing remote so older UIs do not wipe it.
        if (props.getRemoteUrl() != null) {
          PSVirtualSiteHelper.putProperty(
              psSite, contextId, PSVirtualSiteHelper.PROP_REMOTE_URL, blankToNull(props.getRemoteUrl()));
        }
        if (props.getBranch() != null) {
          PSVirtualSiteHelper.putProperty(
              psSite, contextId, PSVirtualSiteHelper.PROP_BRANCH, blankToNull(props.getBranch()));
        }
      }

      try {
        // Allow-list includes git-filesystem, csv-filesystem, sql-database, http-json,
        // object-storage, rss-atom, icalendar, and sitemap-xml. object-storage, rss-atom,
        // icalendar, and sitemap-xml are local-root only (NIO Path; no remaining '..'); cloud
        // URLs and credential properties fail closed (400). rss-atom persist is local/loopback
        // only (no live feed credentials). icalendar persist is a local RFC 5545 fixture only
        // (no CalDAV). sitemap-xml persist is a local sitemap.xml fixture only (no live crawl).
        PSVirtualSiteHelper.validate(psSite);
      } catch (VirtualSiteException e) {
        throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
      }

      siteManager.saveSite(psSite);
      return readVirtual(psSite);
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSNotFoundException e) {
      throw new WebApplicationException("Site not found: " + nameOrId, Response.Status.NOT_FOUND);
    } catch (Exception e) {
      log.error(
          "Failed to update virtual site properties for '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public VirtualSiteBuildResult buildVirtualSite(
      String nameOrId, VirtualSiteBuildRequest request) {
    requireAdmin();
    IPSSite site = requireSite(nameOrId);

    if (!PSVirtualSiteHelper.isVirtual(site)) {
      throw new WebApplicationException(
          "Site '"
              + site.getName()
              + "' is not a Virtual Site (virtual.sourceKind is blank or '"
              + PSVirtualSiteHelper.SOURCE_KIND_REPOSITORY
              + "'). Configure virtual.* properties before building.",
          Response.Status.BAD_REQUEST);
    }

    try {
      PSVirtualSiteHelper.validate(site);
    } catch (VirtualSiteException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    }

    VirtualSiteSourceType type =
        PSVirtualSiteHelper.virtualSourceType(site)
            .orElseThrow(
                () ->
                    new WebApplicationException(
                        "Unsupported virtual.sourceKind for build. Allowed: "
                            + String.join(", ", PSVirtualSiteHelper.allowedSourceKindWireNames()),
                        Response.Status.BAD_REQUEST));

    Path siteRoot;
    try {
      siteRoot = resolveDiscoverRoot(site);
    } catch (VirtualSiteException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    } catch (IOException e) {
      boolean remote = PSVirtualSiteHelper.hasRemote(site);
      log.error(
          "Virtual Site {} I/O failed for '{}' ({}): {}",
          remote ? "Git checkout" : "root path",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(
          (remote ? "Virtual Site Git checkout failed: " : "Virtual Site root path failed: ")
              + PSGitRemoteCheckout.redact(e.getMessage()),
          Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (!Files.isDirectory(siteRoot)) {
      String which =
          PSVirtualSiteHelper.hasRemote(site)
              ? "Git checkout / relative rootPath"
              : PSVirtualSiteHelper.PROP_ROOT_PATH;
      throw new WebApplicationException(
          which + " is not an existing directory: '" + siteRoot + "'",
          Response.Status.BAD_REQUEST);
    }

    String siteKey = PSVirtualSiteHelper.siteKey(site);
    String configFile = PSVirtualSiteHelper.configFile(site);
    // Barrier: only paths that pass requireSafeOutputRoot reach NIO create/resolve sinks.
    Path outputRoot = requireSafeOutputRoot(resolveOutputRoot(request, siteKey));

    try {
      // Barrier already applied: requireSafeOutputRoot → isSafeRootPath (no empty / '..').
      // Admin-only. Model: SitesAdaptor.requireSafeOutputRoot. See suppressions.md #1961.
      Files.createDirectories(outputRoot); // codeql[java/path-injection]
      Path metaDir = outputRoot.resolve("_meta"); // codeql[java/path-injection]
      Files.createDirectories(metaDir); // codeql[java/path-injection]

      VirtualSiteConfig config = loadBuildConfig(type, siteRoot, configFile, siteKey);
      PSVirtualSiteBuildResult built = runBuild(type, config, outputRoot, metaDir);
      recordLastOutputRoot(siteKey, outputRoot);
      return toWireResult(site.getName(), siteKey, built);
    } catch (VirtualSiteException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    } catch (IOException e) {
      log.error(
          "Virtual Site build I/O failed for '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(
          "Virtual Site build failed: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Virtual Site build failed for '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(
          "Virtual Site build failed: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Build then NIO-copy assembled files to {@link IPSSite#getRoot()} for git-filesystem,
   * csv-filesystem, sql-database, http-json, object-storage, rss-atom, and icalendar Virtual
   * Sites. Fail-closed on blank/unsafe/overlapping publish roots. {@code http-json} uses a local
   * JSON fixture (or loopback catalog from {@code _config.yaml}); leftover {@code
   * virtual.remoteUrl} is 400. {@code object-storage} uses a portable-safe local object-key
   * {@code rootPath}; leftover {@code virtual.remoteUrl} is 400 (no cloud URLs, IAM, or access
   * keys). {@code rss-atom} uses a local RSS 2.0 / Atom fixture or loopback feed; leftover
   * {@code virtual.remoteUrl} and credential properties are 400 (no live feeds). {@code
   * icalendar} uses a local RFC 5545 fixture ({@code calendar.ics} / {@code icalendar.file});
   * leftover {@code virtual.remoteUrl} and credential properties are 400 (no CalDAV).
   */
  @Override
  public VirtualSitePublishResult publishVirtualSite(String nameOrId) {
    requireAdmin();
    IPSSite site = requireSite(nameOrId);

    if (!PSVirtualSiteHelper.isVirtual(site)) {
      throw new WebApplicationException(
          "Site '"
              + site.getName()
              + "' is not a Virtual Site (virtual.sourceKind is blank or '"
              + PSVirtualSiteHelper.SOURCE_KIND_REPOSITORY
              + "'). Configure virtual.* properties before publishing.",
          Response.Status.BAD_REQUEST);
    }

    Path publishRoot;
    try {
      publishRoot = PSVirtualSiteFilesystemPublisher.selectFilesystemTarget(site);
    } catch (VirtualSiteException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    }
    Path safePublishRoot = requireSafeOutputRoot(publishRoot);

    VirtualSiteBuildResult built = buildVirtualSite(nameOrId, null);
    String outputPath = built.getOutputPath();
    if (StringUtils.isBlank(outputPath)) {
      throw new WebApplicationException(
          "Virtual Site build did not report an output path.",
          Response.Status.INTERNAL_SERVER_ERROR);
    }
    Path buildOutput = Path.of(outputPath);

    try {
      // Do not mention PSVirtualSitePublishCopyResult here: Spring lookup-method
      // resolution loads every declared parameter type while creating sitesAdaptor.
      int filesCopied =
          PSVirtualSiteFilesystemPublisher.copyBuildFileCountToTarget(
              buildOutput, safePublishRoot);
      return toWirePublishResult(
          site.getName(),
          PSVirtualSiteHelper.siteKey(site),
          built,
          safePublishRoot,
          filesCopied);
    } catch (VirtualSiteException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    } catch (IOException e) {
      log.error(
          "Virtual Site publish I/O failed for '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(
          "Virtual Site publish failed: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Virtual Site publish failed for '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(
          "Virtual Site publish failed: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Load {@code _config.yaml} (required for git-filesystem, sql-database, http-json,
   * object-storage, rss-atom, and icalendar). CSV trees may omit the file and infer versions from
   * child directories. HTTP JSON catalog URL/file live in the yaml ({@code http.url} / {@code
   * http.file} or default {@code pages.json}). Object-storage optional {@code objects.keys} live
   * in the yaml. RSS / Atom optional {@code rss.file} / {@code rss.url} live in the yaml (default
   * {@code feed.xml} then {@code atom.xml}). iCalendar optional {@code icalendar.file} lives in
   * the yaml (default {@code calendar.ics}).
   */
  static VirtualSiteConfig loadBuildConfig(
      VirtualSiteSourceType type, Path siteRoot, String configFile, String siteKey)
      throws IOException, VirtualSiteException {
    if (type == VirtualSiteSourceType.CSV_FILESYSTEM) {
      return VirtualSiteConfigLoader.loadOrDefault(siteRoot, configFile, siteKey);
    }
    return VirtualSiteConfigLoader.load(siteRoot, configFile, siteKey);
  }

  private PSVirtualSiteBuildResult runBuild(
      VirtualSiteSourceType type, VirtualSiteConfig config, Path outputRoot, Path metaDir)
      throws Exception {
    if (buildRunner != null) {
      return buildRunner.build(config, outputRoot);
    }
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(
            type, new PSInMemoryVirtualParticipantService(metaDir));
    return service.build(config, outputRoot);
  }

  /**
   * Map domain site to rest detail DTO including virtual properties.
   *
   * @param site domain site, not null
   * @return rest site
   */
  Site toDetailSite(IPSSite site) {
    Site ret = new Site();
    ret.setName(site.getName());
    ret.setDescription(site.getDescription());
    ret.setBaseUrl(site.getBaseUrl());
    ret.setDefaultFileExtention(site.getDefaultFileExtension());
    ret.setCanonical(site.isCanonical());
    ret.setCanonicalDist(site.getCanonicalDist());
    ret.setCanonicalReplace(site.isCanonicalReplace());
    ret.setDefaultDocument(site.getDefaultDocument());
    ret.setSiteProtocol(site.getSiteProtocol());
    ret.setOverrideSystemFoundation(site.isOverrideSystemFoundation());
    ret.setOverrideSystemJQuery(site.isOverrideSystemJQuery());
    ret.setOverrideSystemJQueryUI(site.isOverrideSystemJQueryUI());
    ret.setSiteAdditionalHeadContent(site.getSiteAdditionalHeadContent());
    ret.setSiteAfterBodyOpenContent(site.getSiteAfterBodyOpenContent());
    ret.setSiteBeforeBodyCloseContent(site.getSiteBeforeBodyCloseContent());
    if (site.getGUID() != null) {
      ret.setGuid(ApiUtils.convertGuid(site.getGUID()));
    }
    ret.setVirtual(readVirtual(site));
    ret.setManagedNavigation(PSManagedNavSiteHelper.flagForNonVirtual(site));
    return ret;
  }

  /**
   * Read virtual.* into the wire DTO.
   *
   * @param site domain site
   * @return never null
   */
  static VirtualSiteProperties readVirtual(IPSSite site) {
    VirtualSiteProperties v = new VirtualSiteProperties();
    PSVirtualSiteHelper.findProperty(site, PSVirtualSiteHelper.PROP_SOURCE_KIND)
        .ifPresent(v::setSourceKind);
    PSVirtualSiteHelper.findProperty(site, PSVirtualSiteHelper.PROP_ROOT_PATH)
        .ifPresent(v::setRootPath);
    // configFile: expose stored value only (not default) so clients see "unset" vs override
    PSVirtualSiteHelper.findProperty(site, PSVirtualSiteHelper.PROP_CONFIG_FILE)
        .ifPresent(v::setConfigFile);
    PSVirtualSiteHelper.findProperty(site, PSVirtualSiteHelper.PROP_SITE_KEY)
        .ifPresent(v::setSiteKey);
    PSVirtualSiteHelper.findProperty(site, PSVirtualSiteHelper.PROP_REMOTE_URL)
        .ifPresent(v::setRemoteUrl);
    PSVirtualSiteHelper.findProperty(site, PSVirtualSiteHelper.PROP_BRANCH)
        .ifPresent(v::setBranch);
    v.setVirtual(PSVirtualSiteHelper.isVirtual(site));
    return v;
  }

  @Override
  public VirtualSitePreviewStatus getVirtualSitePreviewStatus(String nameOrId) {
    IPSSite site = requireVirtualAdminSite(nameOrId);
    String siteKey = PSVirtualSiteHelper.siteKey(site);
    Path outputRoot = resolveLastOutputRoot(siteKey);
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    if (outputRoot != null) {
      status.setOutputPath(outputRoot.toAbsolutePath().normalize().toString());
    }
    String home = outputRoot == null ? null : findHomeRelativePath(outputRoot);
    if (home == null) {
      status.setAvailable(false);
      status.setMessage(MISSING_PREVIEW_MESSAGE);
      return status;
    }
    status.setAvailable(true);
    status.setHomePath(home);
    return status;
  }

  @Override
  public VirtualSitePreviewFile previewVirtualSiteFile(String nameOrId, String relativePath) {
    IPSSite site = requireVirtualAdminSite(nameOrId);
    String siteKey = PSVirtualSiteHelper.siteKey(site);
    Path outputRoot = resolveLastOutputRoot(siteKey);
    if (outputRoot == null || !Files.isDirectory(outputRoot)) {
      throw new WebApplicationException(MISSING_PREVIEW_MESSAGE, Response.Status.NOT_FOUND);
    }
    String rel = blankToNull(relativePath);
    if (rel == null) {
      String home = findHomeRelativePath(outputRoot);
      if (home == null) {
        throw new WebApplicationException(MISSING_PREVIEW_MESSAGE, Response.Status.NOT_FOUND);
      }
      rel = home;
    }
    Path file = resolvePreviewFile(outputRoot, rel);
    if (file == null) {
      throw new WebApplicationException(
          "Preview file not found: " + rel, Response.Status.NOT_FOUND);
    }
    try {
      long size = Files.size(file); // codeql[java/path-injection]
      if (size > MAX_PREVIEW_FILE_BYTES) {
        throw new WebApplicationException(
            "Preview file is too large to stream", Response.Status.BAD_REQUEST);
      }
      byte[] bytes = Files.readAllBytes(file); // codeql[java/path-injection]
      return new VirtualSitePreviewFile(mediaTypeFor(file), toWirePath(outputRoot, file), bytes);
    } catch (WebApplicationException e) {
      throw e;
    } catch (IOException e) {
      throw new WebApplicationException(
          "Could not read preview file", Response.Status.NOT_FOUND);
    }
  }

  /**
   * Admin + Virtual Site gate shared by preview status/file (missing output is handled by callers).
   *
   * <p>Preview is last-output based and applies to allow-listed Virtual kinds ({@code
   * git-filesystem}, {@code csv-filesystem}, {@code sql-database}, {@code http-json}, {@code
   * object-storage}, {@code rss-atom}, and {@code icalendar}), not git-only. {@code rss-atom}
   * streams last-build HTML from a local RSS 2.0 / Atom fixture (or loopback feed); leftover
   * {@code virtual.remoteUrl} is 400. {@code icalendar} streams last-build HTML from a local
   * RFC 5545 fixture; leftover {@code virtual.remoteUrl} is 400 (no CalDAV). Traditional
   * {@code repository} Sites and unknown {@code virtual.sourceKind} values return 400 via
   * {@link PSVirtualSiteHelper#validate}.
   */
  IPSSite requireVirtualAdminSite(String nameOrId) {
    requireAdmin();
    IPSSite site = requireSite(nameOrId);
    if (!PSVirtualSiteHelper.isVirtual(site)) {
      throw new WebApplicationException(
          "Site '"
              + site.getName()
              + "' is not a Virtual Site (virtual.sourceKind is blank or '"
              + PSVirtualSiteHelper.SOURCE_KIND_REPOSITORY
              + "'). Configure virtual.* properties before previewing.",
          Response.Status.BAD_REQUEST);
    }
    try {
      PSVirtualSiteHelper.validate(site);
    } catch (VirtualSiteException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    }
    return site;
  }

  /**
   * Persist the last successful build output root so preview can reuse {@code result.outputPath}
   * (including custom {@code outputRoot}).
   */
  void recordLastOutputRoot(String siteKey, Path outputRoot) {
    Path pointerDir =
        defaultOutputRootResolver.apply(safePathSegment(siteKey)).resolve("_meta");
    try {
      Path safeOut = requireSafeOutputRoot(outputRoot.toAbsolutePath().normalize());
      Files.createDirectories(pointerDir); // codeql[java/path-injection]
      Files.writeString(
          pointerDir.resolve(LAST_OUTPUT_POINTER_FILE),
          safeOut.toString(),
          StandardCharsets.UTF_8); // codeql[java/path-injection]
    } catch (RuntimeException | IOException e) {
      log.warn(
          "Could not record last Virtual Site output path for site '{}': {}",
          siteKey,
          e.getMessage());
    }
  }

  /**
   * Last recorded output root when the pointer is a safe existing directory; otherwise the default
   * output directory when it exists.
   */
  Path resolveLastOutputRoot(String siteKey) {
    Path defaultRoot =
        requireSafeOutputRoot(defaultOutputRootResolver.apply(safePathSegment(siteKey)));
    Path pointer = defaultRoot.resolve("_meta").resolve(LAST_OUTPUT_POINTER_FILE);
    if (Files.isRegularFile(pointer)) {
      try {
        String raw = Files.readString(pointer, StandardCharsets.UTF_8).trim(); // codeql[java/path-injection]
        if (!raw.isEmpty()) {
          Path candidate = Path.of(raw).toAbsolutePath().normalize();
          if (PSVirtualSiteHelper.isSafeRootPath(candidate) && Files.isDirectory(candidate)) {
            return candidate;
          }
        }
      } catch (RuntimeException | IOException e) {
        log.debug("Ignoring invalid last-output pointer: {}", e.getMessage());
      }
    }
    if (Files.isDirectory(defaultRoot)) {
      return defaultRoot;
    }
    return null;
  }

  static String findHomeRelativePath(Path outputRoot) {
    if (outputRoot == null || !Files.isDirectory(outputRoot)) {
      return null;
    }
    Path root = outputRoot.toAbsolutePath().normalize();
    if (Files.isRegularFile(root.resolve("index.html"))) {
      return "index.html";
    }
    if (Files.isRegularFile(root.resolve("8.2").resolve("index.html"))) {
      return "8.2/index.html";
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
      List<Path> dirs = new ArrayList<>();
      for (Path p : stream) {
        if (Files.isDirectory(p)) {
          dirs.add(p);
        }
      }
      dirs.sort(SitesAdaptor::compareHomeCandidateDirectories);
      for (Path dir : dirs) {
        if (isSkippedHomeDirectory(dir)) {
          continue;
        }
        Path idx = dir.resolve("index.html");
        if (Files.isRegularFile(idx)) {
          return toWirePath(root, idx);
        }
      }
    } catch (IOException e) {
      return null;
    }
    return null;
  }

  /** Skip assembler sidecar dirs that are never a product-docs home. */
  static boolean isSkippedHomeDirectory(Path dir) {
    Path name = dir == null ? null : dir.getFileName();
    return name != null && "_meta".equals(name.toString());
  }

  /**
   * Version directories ({@code 8.2}, {@code 10.0}) newest-first; other names last,
   * alphabetically. Avoids lexical {@code 10.0} before {@code 9.0}.
   */
  static int compareHomeCandidateDirectories(Path left, Path right) {
    String a = fileNameOrEmpty(left);
    String b = fileNameOrEmpty(right);
    int[] va = parseDottedVersionParts(a);
    int[] vb = parseDottedVersionParts(b);
    if (va != null && vb != null) {
      int n = Math.max(va.length, vb.length);
      for (int i = 0; i < n; i++) {
        int ai = i < va.length ? va[i] : 0;
        int bi = i < vb.length ? vb[i] : 0;
        int cmp = Integer.compare(bi, ai);
        if (cmp != 0) {
          return cmp;
        }
      }
      return 0;
    }
    if (va != null) {
      return -1;
    }
    if (vb != null) {
      return 1;
    }
    return a.compareTo(b);
  }

  static int[] parseDottedVersionParts(String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }
    String[] bits = name.split("\\.", -1);
    int[] parts = new int[bits.length];
    for (int i = 0; i < bits.length; i++) {
      String bit = bits[i];
      if (bit.isEmpty()) {
        return null;
      }
      for (int c = 0; c < bit.length(); c++) {
        if (!Character.isDigit(bit.charAt(c))) {
          return null;
        }
      }
      try {
        parts[i] = Integer.parseInt(bit);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return parts;
  }

  private static String fileNameOrEmpty(Path path) {
    if (path == null || path.getFileName() == null) {
      return "";
    }
    return path.getFileName().toString();
  }

  static Path resolvePreviewFile(Path outputRoot, String relativePath) {
    Path root = requireSafeOutputRoot(outputRoot.toAbsolutePath().normalize());
    Path rel = requireSafeRelativePreviewPath(relativePath);
    Path resolved = root.resolve(rel).normalize(); // codeql[java/path-injection]
    if (!resolved.startsWith(root)) {
      throw new WebApplicationException(
          "Preview path is outside the build output", Response.Status.BAD_REQUEST);
    }
    if (Files.isDirectory(resolved)) {
      Path index = resolved.resolve("index.html").normalize(); // codeql[java/path-injection]
      if (!index.startsWith(root) || !Files.isRegularFile(index)) {
        return null;
      }
      return index;
    }
    if (!Files.isRegularFile(resolved)) {
      return null;
    }
    return resolved;
  }

  static Path requireSafeRelativePreviewPath(String relativePath) {
    if (relativePath == null || relativePath.isBlank() || relativePath.indexOf('\0') >= 0) {
      throw new WebApplicationException("Preview path is required", Response.Status.BAD_REQUEST);
    }
    String cleaned = relativePath.trim().replace('\\', '/');
    while (cleaned.startsWith("/")) {
      cleaned = cleaned.substring(1);
    }
    Path path;
    try {
      path = Path.of(cleaned).normalize();
    } catch (InvalidPathException e) {
      throw new WebApplicationException("Preview path is not valid", Response.Status.BAD_REQUEST);
    }
    if (path.isAbsolute()) {
      throw new WebApplicationException(
          "Preview path must be relative", Response.Status.BAD_REQUEST);
    }
    for (Path part : path) {
      String name = part.toString();
      if ("..".equals(name) || name.isEmpty()) {
        throw new WebApplicationException(
            "Preview path must not contain '..'", Response.Status.BAD_REQUEST);
      }
    }
    return path;
  }

  static String toWirePath(Path outputRoot, Path file) {
    Path root = outputRoot.toAbsolutePath().normalize();
    Path resolved = file.toAbsolutePath().normalize();
    Path rel = root.relativize(resolved);
    return rel.toString().replace('\\', '/');
  }

  static String mediaTypeFor(Path file) {
    String name = file.getFileName() != null ? file.getFileName().toString() : "";
    String lower = name.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".html") || lower.endsWith(".htm")) {
      return "text/html; charset=UTF-8";
    }
    if (lower.endsWith(".css")) {
      return "text/css; charset=UTF-8";
    }
    if (lower.endsWith(".js")) {
      return "application/javascript; charset=UTF-8";
    }
    if (lower.endsWith(".json")) {
      return "application/json; charset=UTF-8";
    }
    if (lower.endsWith(".svg")) {
      return "image/svg+xml";
    }
    if (lower.endsWith(".png")) {
      return "image/png";
    }
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    if (lower.endsWith(".gif")) {
      return "image/gif";
    }
    if (lower.endsWith(".woff2")) {
      return "font/woff2";
    }
    if (lower.endsWith(".woff")) {
      return "font/woff";
    }
    if (lower.endsWith(".txt")) {
      return "text/plain; charset=UTF-8";
    }
    return "application/octet-stream";
  }

  private void requireAdmin() {
    boolean allowed;
    try {
      allowed = adminChecker.getAsBoolean();
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.debug("Admin check failed: {}", e.getMessage());
      throw new WebApplicationException(
          "Admin role required to build or publish Virtual Sites", Response.Status.FORBIDDEN);
    }
    if (!allowed) {
      throw new WebApplicationException(
          "Admin role required to build or publish Virtual Sites", Response.Status.FORBIDDEN);
    }
  }

  /**
   * Production Admin check via {@link IPSUserService}. Used when Spring wires the no-arg ctor and
   * {@link #adminChecker} is the instance method reference.
   */
  boolean isCurrentUserAdmin() {
    if (userService == null) {
      return false;
    }
    try {
      PSCurrentUser current = userService.getCurrentUser();
      if (current == null || StringUtils.isBlank(current.getName())) {
        return false;
      }
      return userService.isAdminUser(current.getName());
    } catch (PSDataServiceException e) {
      log.debug("Unable to resolve current user for Admin check: {}", e.getMessage());
      return false;
    }
  }

  Path resolveOutputRoot(VirtualSiteBuildRequest request, String siteKey) {
    String override =
        request != null ? blankToNull(request.getOutputRoot()) : null;
    if (override != null) {
      Path path;
      try {
        path = Path.of(override).normalize();
      } catch (InvalidPathException e) {
        throw new WebApplicationException(
            "outputRoot is not a valid filesystem path: '" + override + "'",
            Response.Status.BAD_REQUEST);
      }
      return requireSafeOutputRoot(path);
    }
    return requireSafeOutputRoot(defaultOutputRootResolver.apply(safePathSegment(siteKey)));
  }

  /**
   * Path-injection barrier for Virtual Site build output roots.
   *
   * <p>Rejects empty / {@code .} / remaining {@code ..} name elements after normalize (delegates to
   * {@link PSVirtualSiteHelper#isSafeRootPath(Path)}). Modeled for CodeQL as a {@code
   * path-injection} barrier; callers must not use the input path after a failed check.
   *
   * @param path candidate output root (already normalized preferred)
   * @return the same path after validation
   * @throws WebApplicationException 400 when the path is unsafe
   */
  static Path requireSafeOutputRoot(Path path) {
    if (!PSVirtualSiteHelper.isSafeRootPath(path)) {
      throw new WebApplicationException(
          "outputRoot must be a non-empty path with no '..' segments after normalize. Rejected: '"
              + path
              + "'",
          Response.Status.BAD_REQUEST);
    }
    return path.normalize();
  }

  /**
   * Local discover root, or a Git checkout work tree when {@code virtual.remoteUrl} is set.
   *
   * @param site validated virtual site
   * @return existing or newly fetched tree
   */
  Path resolveDiscoverRoot(IPSSite site) throws VirtualSiteException, IOException {
    if (!PSVirtualSiteHelper.hasRemote(site)) {
      return PSVirtualSiteHelper.resolveDiscoverRoot(site, null);
    }
    Path workBase = defaultCheckoutWorkBase();
    return gitRemoteCheckout.ensureCurrent(site, workBase);
  }

  /**
   * Contained checkout parent: {@code {rxDir}/tmp/virtual-site-checkouts} when the install root is
   * known, else {@code {java.io.tmpdir}/percussion-virtual-site-checkouts}.
   */
  static Path defaultCheckoutWorkBase() {
    try {
      File rx = PSServer.getRxDir();
      if (rx != null) {
        Path base = rx.toPath().normalize();
        if (PSVirtualSiteHelper.isSafeRootPath(base)) {
          return base.resolve("tmp").resolve("virtual-site-checkouts");
        }
      }
    } catch (RuntimeException e) {
      log.debug("PSServer.getRxDir unavailable for virtual checkout work base: {}", e.getMessage());
    }
    return Path.of(System.getProperty("java.io.tmpdir"), "percussion-virtual-site-checkouts");
  }

  /**
   * Default output: {@code {rxDir}/tmp/virtual-sites/{siteKey}} when install root is known, else
   * {@code {java.io.tmpdir}/percussion-virtual-sites/{siteKey}}. Uses portable NIO {@link Path}.
   */
  static Path defaultOutputRootForSiteKey(String siteKey) {
    String key = safePathSegment(siteKey);
    try {
      File rx = PSServer.getRxDir();
      if (rx != null) {
        Path base = rx.toPath().normalize();
        if (PSVirtualSiteHelper.isSafeRootPath(base)) {
          return base.resolve("tmp").resolve("virtual-sites").resolve(key);
        }
      }
    } catch (RuntimeException e) {
      // Fall through to JVM temp — unit tests / early boot may lack install root.
      log.debug("PSServer.getRxDir unavailable for virtual build output: {}", e.getMessage());
    }
    return Path.of(System.getProperty("java.io.tmpdir"), "percussion-virtual-sites", key);
  }

  static VirtualSiteBuildResult toWireResult(
      String siteName, String siteKey, PSVirtualSiteBuildResult built) {
    VirtualSiteBuildResult dto = new VirtualSiteBuildResult();
    dto.setSiteName(siteName);
    dto.setSiteKey(siteKey);
    if (built.outputRoot() != null) {
      dto.setOutputPath(built.outputRoot().toAbsolutePath().normalize().toString());
    }
    dto.setPagesWritten(built.pageCount());
    List<String> problems = built.linkProblems();
    dto.setLinkProblemCount(problems.size());
    dto.setHasLinkProblems(!problems.isEmpty());
    dto.setLinkProblems(truncate(problems, MAX_RESULT_LINES));
    dto.setWrittenFiles(truncate(built.writtenFiles(), MAX_RESULT_LINES));
    return dto;
  }

  static VirtualSitePublishResult toWirePublishResult(
      String siteName,
      String siteKey,
      VirtualSiteBuildResult built,
      Path publishRoot,
      int filesCopied) {
    VirtualSitePublishResult dto = new VirtualSitePublishResult();
    dto.setSiteName(siteName);
    dto.setSiteKey(siteKey);
    if (publishRoot != null) {
      dto.setPublishPath(publishRoot.toAbsolutePath().normalize().toString());
    }
    if (built != null) {
      if (built.getOutputPath() != null) {
        dto.setBuildOutputPath(built.getOutputPath());
      }
      dto.setPagesWritten(built.getPagesWritten());
      dto.setLinkProblemCount(built.getLinkProblemCount());
      dto.setHasLinkProblems(built.getHasLinkProblems());
      dto.setLinkProblems(built.getLinkProblems());
    }
    dto.setFilesCopied(Math.max(filesCopied, 0));
    return dto;
  }

  private static List<String> truncate(List<String> lines, int max) {
    if (lines == null || lines.isEmpty()) {
      return new ArrayList<>();
    }
    if (lines.size() <= max) {
      return new ArrayList<>(lines);
    }
    List<String> out = new ArrayList<>(lines.subList(0, max));
    out.add("… truncated " + (lines.size() - max) + " more line(s); see link-report.txt / output");
    return out;
  }

  /** Sanitize siteKey for use as a single path segment (no separators / traversal). */
  static String safePathSegment(String siteKey) {
    String raw = StringUtils.isBlank(siteKey) ? "default" : siteKey.trim();
    StringBuilder sb = new StringBuilder(raw.length());
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (c == '/' || c == '\\' || c == ':' || c == 0) {
        sb.append('_');
      } else {
        sb.append(c);
      }
    }
    String cleaned = sb.toString();
    if ("..".equals(cleaned) || ".".equals(cleaned) || cleaned.isEmpty()) {
      return "default";
    }
    return cleaned;
  }

  private IPSSite requireSite(String nameOrId) {
    if (StringUtils.isBlank(nameOrId)) {
      throw new WebApplicationException("nameOrId is required", Response.Status.BAD_REQUEST);
    }
    String key = nameOrId.trim();
    IPSSite site = siteManager.findSite(key);
    if (site != null) {
      return site;
    }
    try {
      site = siteManager.findSite(new PSGuid(key));
    } catch (RuntimeException e) {
      site = null;
    }
    if (site == null) {
      throw new WebApplicationException("Site not found: " + nameOrId, Response.Status.NOT_FOUND);
    }
    return site;
  }

  private IPSSite loadModifiable(IPSSite found) throws PSNotFoundException {
    if (found.getGUID() != null) {
      return siteManager.loadSiteModifiable(found.getGUID());
    }
    return siteManager.loadSiteModifiable(found.getName());
  }

  /**
   * Resolve context for new virtual.* properties: reuse existing property context when present,
   * else Preview (or first available publishing context).
   */
  IPSGuid resolvePropertyContext(PSSite site) throws PSNotFoundException {
    Optional<IPSGuid> existing =
        PSVirtualSiteHelper.findPropertyContext(site, PSVirtualSiteHelper.PROP_SOURCE_KIND);
    if (existing.isEmpty()) {
      existing =
          PSVirtualSiteHelper.findPropertyContext(site, PSVirtualSiteHelper.PROP_ROOT_PATH);
    }
    if (existing.isPresent()) {
      return existing.get();
    }
    try {
      IPSPublishingContext preview = siteManager.loadContext(DEFAULT_PROPERTY_CONTEXT);
      if (preview != null && preview.getGUID() != null) {
        return preview.getGUID();
      }
    } catch (PSNotFoundException e) {
      log.debug("Preview publishing context not found; falling back to first context");
    }
    List<IPSPublishingContext> contexts = siteManager.findAllContexts();
    if (contexts == null || contexts.isEmpty()) {
      throw new WebApplicationException(
          "No publishing context available to store virtual site properties",
          Response.Status.INTERNAL_SERVER_ERROR);
    }
    return contexts.get(0).getGUID();
  }

  private static String blankToNull(String value) {
    return StringUtils.isBlank(value) ? null : value.trim();
  }
}
