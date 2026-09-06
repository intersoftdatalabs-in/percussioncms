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

package com.percussion.rest.sites;

import com.percussion.cms.IPSConstants;
import com.percussion.system.utils.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/** REST resource for Site operations. Sunny Sal: "Sites resource, content ka force!" */
@PSSiteManageBean(value = "restSitesResource")
@Path("/sites")
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Tag(name = "Sites", description = "Site operations including Virtual Site properties")
@Lazy
public class SitesResource {

  /**
   * Package-private and non-final so unit tests can install a mock {@link Logger}.
   */
  static Logger log = LogManager.getLogger(IPSConstants.API_LOG);

  private final ISiteAdaptor adaptor;

  @Context private UriInfo uriInfo;

  public SitesResource() {
    this.adaptor = null;
  }

  @Autowired
  public SitesResource(ISiteAdaptor adaptor) {
    this.adaptor = adaptor;
  }

  /**
   * Lists all sites. Traditional rows may include {@code managedNavigation}; Virtual Sites omit
   * that flag.
   *
   * @return SiteList of all sites
   */
  @GET
  @Path("/")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Operation(
      summary = "List sites",
      description = "Lists CMS Sites. Summary payloads may omit virtual property details.",
      responses = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public SiteList listSites() {
    try {
      SiteList list = requireAdaptor().findAllSites();
      return list != null ? list : new SiteList();
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to list sites ({}): {}",
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Loads a site by name or GUID string, including Virtual Site properties when set.
   *
   * @param nameOrId site name or GUID
   * @return site detail
   */
  @GET
  @Path("/{nameOrId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Operation(
      summary = "Get site by name or GUID",
      description =
          "Returns site detail including virtual.* properties (sourceKind, rootPath, configFile,"
              + " siteKey) when configured.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Site.class))),
        @ApiResponse(responseCode = "404", description = "Site not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Site getSite(@PathParam("nameOrId") String nameOrId) {
    requireNonBlank(nameOrId, "nameOrId");
    try {
      Site site = resolveSite(nameOrId);
      if (site == null) {
        throw new WebApplicationException("Site not found: " + nameOrId, Response.Status.NOT_FOUND);
      }
      return site;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to get site '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Loads Virtual Site properties for a site.
   *
   * @param nameOrId site name or GUID
   * @return virtual properties (fields empty for traditional Sites)
   */
  @GET
  @Path("/{nameOrId}/virtual")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Operation(
      summary = "Get Virtual Site properties",
      description =
          "Returns virtual.sourceKind, virtual.rootPath, virtual.configFile, and virtual.siteKey"
              + " for the site. Traditional Sites return empty/default values with virtual=false.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = VirtualSiteProperties.class))),
        @ApiResponse(responseCode = "404", description = "Site not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public VirtualSiteProperties getVirtualProperties(@PathParam("nameOrId") String nameOrId) {
    requireNonBlank(nameOrId, "nameOrId");
    try {
      return requireAdaptor().getVirtualSiteProperties(nameOrId);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to get virtual properties for '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Creates or updates Virtual Site properties for a site.
   *
   * @param nameOrId site name or GUID
   * @param props properties to apply
   * @return persisted properties
   */
  @PUT
  @Path("/{nameOrId}/virtual")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Operation(
      summary = "Update Virtual Site properties",
      description =
          "Persists virtual.* properties. Validation aligns with PSVirtualSiteHelper:"
              + " sourceKind allow-list (git-filesystem, csv-filesystem, sql-database, http-json,"
              + " object-storage, rss-atom, icalendar, sitemap-xml, robots-txt, llms-txt,"
              + " openapi-yaml), required non-blank rootPath for"
              + " sql-database, http-json, object-storage, rss-atom, icalendar, sitemap-xml, robots-txt, llms-txt, and openapi-yaml; for"
              + " other kinds when virtual and remoteUrl is blank, optional remoteUrl+branch for"
              + " git-filesystem only (https/ssh/file/git@host:path; fail-closed on unsafe URLs /"
              + " '..'; csv-filesystem, sql-database, http-json, object-storage, rss-atom,"
              + " icalendar, sitemap-xml, robots-txt, llms-txt, and openapi-yaml reject remoteUrl — no secrets on this envelope), safe"
              + " NIO path, simple configFile name. sql-database JDBC URL/user/query live in"
              + " _config.yaml under rootPath (H2 mem only; never send passwords on this envelope)."
              + " http-json catalog URL/file live in _config.yaml (http.url / http.file); REST"
              + " persists a portable-safe rootPath JSON fixture. object-storage persists a"
              + " portable-safe local rootPath (no remaining '..'); cloud URLs and credential"
              + " properties return 400. rss-atom persist is local/loopback only (portable-safe local"
              + " rootPath; leftover remoteUrl, credentials, and cloud URL rootPath return 400; no"
              + " live feed credentials). icalendar persist is a local RFC 5545 fixture only"
              + " (portable-safe local rootPath; leftover remoteUrl, credentials, and cloud URL"
              + " rootPath return 400; no CalDAV). sitemap-xml persist is a local sitemap.xml fixture"
              + " only (portable-safe local rootPath; leftover remoteUrl, credentials, and cloud URL"
              + " rootPath return 400; no live crawl). robots-txt persist is a local robots.txt fixture"
              + " only (portable-safe local rootPath; leftover remoteUrl, credentials, and cloud URL"
              + " rootPath return 400; no live crawl). llms-txt persist is a local llms.txt fixture"
              + " only (portable-safe local rootPath; leftover remoteUrl, credentials, and cloud URL"
              + " rootPath return 400; no live HTTP fetch). openapi-yaml persist is a local OpenAPI 3 YAML fixture"
              + " only (portable-safe local rootPath; leftover remoteUrl, credentials, and cloud URL"
              + " rootPath return 400; no live spec fetch). GET after PUT round-trips the stored"
              + " sourceKind. Unknown kinds return 400. Blank/repository sourceKind clears virtual"
              + " configuration.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = VirtualSiteProperties.class))),
        @ApiResponse(responseCode = "400", description = "Validation failure"),
        @ApiResponse(responseCode = "404", description = "Site not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public VirtualSiteProperties updateVirtualProperties(
      @PathParam("nameOrId") String nameOrId, VirtualSiteProperties props) {
    requireNonBlank(nameOrId, "nameOrId");
    if (props == null) {
      throw new WebApplicationException(
          "VirtualSiteProperties body is required", Response.Status.BAD_REQUEST);
    }
    try {
      // JSON/XML DTO via Jackson/JAXB/CXF — not HTML body (see suppressions.md)
      return requireAdaptor().updateVirtualSiteProperties(nameOrId, props); // codeql[java/xss]
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to update virtual properties for '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Builds a Virtual Site from configured {@code virtual.*} properties.
   *
   * @param nameOrId site name or GUID
   * @param request optional body (output root override); may be null
   * @return pages-written and link-problem summary
   */
  @POST
  @Path("/{nameOrId}/virtual/build")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Operation(
      summary = "Build Virtual Site",
      description =
          "Runs the Virtual Site static build for a site configured with"
              + " virtual.sourceKind=git-filesystem, csv-filesystem, sql-database, http-json,"
              + " object-storage, rss-atom, icalendar, sitemap-xml, robots-txt, or llms-txt. git-filesystem: when virtual.remoteUrl is set, the"
              + " server clones or fetches that branch into a contained work directory, then"
              + " discovers Markdown. csv-filesystem: rootPath is a CSV tree (optional _config.yaml;"
              + " required columns id, title, body; fail-closed on unsafe paths). sql-database:"
              + " rootPath holds required _config.yaml sql: mapping (in-memory H2 jdbc:h2:mem: only;"
              + " user/query in yaml, never logged passwords). http-json: rootPath holds required"
              + " _config.yaml (versions plus http.url or http.file / default pages.json); local"
              + " JSON fixture or loopback catalog; virtual.remoteUrl is 400 (no secrets on this"
              + " envelope). object-storage: rootPath is a local object-key bucket (required"
              + " _config.yaml; Markdown / HTML / JSON keys; optional objects.keys); portable NIO"
              + " Path/Files; virtual.remoteUrl is 400 (no secrets / no cloud URLs on this envelope)."
              + " rss-atom: local RSS 2.0 / Atom fixture under rootPath (feed.xml / atom.xml or"
              + " _config.yaml rss.file; rss.url loopback only); no live remote feeds; leftover"
              + " virtual.remoteUrl, credential properties, and cloud rootPath are 400. icalendar:"
              + " local RFC 5545 fixture under rootPath (calendar.ics or _config.yaml"
              + " icalendar.file); no CalDAV or live remotes; leftover virtual.remoteUrl,"
              + " credential properties, and cloud rootPath are 400. sitemap-xml: local sitemap.xml"
              + " fixture under rootPath (sitemap.xml or _config.yaml sitemap.file; urlset of"
              + " portable files); no live crawl; leftover virtual.remoteUrl, credential"
              + " properties, and cloud rootPath are 400. robots-txt: local robots.txt fixture"
              + " under rootPath (robots.txt or _config.yaml robots.file); no live crawl; leftover"
              + " virtual.remoteUrl, credential properties, and cloud rootPath are 400. llms-txt:"
              + " local llms.txt fixture under rootPath (llms.txt or _config.yaml llms.file); no"
              + " live HTTP fetch; leftover virtual.remoteUrl, credential properties, and cloud"
              + " rootPath are 400. A second"
              + " sitemap-xml, robots-txt, or llms-txt Build after an in-process sitemap.xml /"
              + " robots.txt / llms.txt / sitemap.file / robots.file / llms.file or referenced-page"
              + " edit returns"
              + " pagesWritten>0 HTML that reflects the current file (no JVM / Jetty restart; no"
              + " file watchers). Unknown"
              + " source kinds return 400. Uses PSVirtualSiteBuildService.forSourceType with"
              + " portable NIO Path I/O. Requires Admin. Traditional repository Sites and invalid"
              + " source kinds/paths return 4xx. Optional body may set outputRoot; otherwise the"
              + " server writes under {install}/tmp/virtual-sites/{siteKey}. Link problems are"
              + " reported in the result (HTTP 200) without failing the build.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Build completed (check hasLinkProblems / linkProblemCount)",
            content = @Content(schema = @Schema(implementation = VirtualSiteBuildResult.class))),
        @ApiResponse(responseCode = "400", description = "Not virtual / validation / path failure"),
        @ApiResponse(responseCode = "403", description = "Not authorized (Admin required)"),
        @ApiResponse(responseCode = "404", description = "Site not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public VirtualSiteBuildResult buildVirtualSite(
      @PathParam("nameOrId") String nameOrId, VirtualSiteBuildRequest request) {
    requireNonBlank(nameOrId, "nameOrId");
    try {
      // JSON/XML DTO via Jackson/JAXB/CXF — not HTML body (see suppressions.md)
      return requireAdaptor().buildVirtualSite(nameOrId, request); // codeql[java/xss]
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to build virtual site '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Last-build preview availability (JSON). Missing assembled output is HTTP 200 with {@code
   * available=false}.
   *
   * @param nameOrId site name or GUID
   * @return preview status
   */
  @GET
  @Path("/{nameOrId}/virtual/preview")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Operation(
      summary = "Virtual Site preview status",
      description =
          "Reports whether the last Admin Virtual Site build can be opened from the product UI."
              + " Last-output based for git-filesystem, csv-filesystem, sql-database, http-json,"
              + " object-storage, rss-atom, icalendar, sitemap-xml, robots-txt, and llms-txt (not git-only). Uses the last"
              + " build output path (default {install}/tmp/virtual-sites/{siteKey}). After a successful"
              + " http-json, object-storage, rss-atom, icalendar, sitemap-xml, robots-txt, or llms-txt Build, available=true"
              + " plus homePath. rss-atom is a local RSS 2.0 / Atom fixture or loopback feed (no live"
              + " remote feeds). icalendar is a local RFC 5545 calendar.ics fixture (no CalDAV)."
              + " sitemap-xml is last-build local HTML only (sitemap.xml / sitemap.file; no live crawl;"
              + " leftover virtual.remoteUrl and credentials are 400). robots-txt is last-build local"
              + " HTML only (robots.txt / robots.file; no live crawl; leftover virtual.remoteUrl and"
              + " credentials are 400). llms-txt is last-build local"
              + " HTML only (llms.txt / llms.file; no live HTTP fetch; leftover virtual.remoteUrl and"
              + " credentials are 400). Missing or failed builds return"
              + " 200 with available=false (not 500). Requires Admin. Traditional repository Sites and"
              + " unknown sourceKind values return 400.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Status (check available / homePath)",
            content = @Content(schema = @Schema(implementation = VirtualSitePreviewStatus.class))),
        @ApiResponse(responseCode = "400", description = "Not virtual / validation failure"),
        @ApiResponse(responseCode = "403", description = "Not authorized (Admin required)"),
        @ApiResponse(responseCode = "404", description = "Site not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public VirtualSitePreviewStatus getVirtualSitePreviewStatus(
      @PathParam("nameOrId") String nameOrId) {
    requireNonBlank(nameOrId, "nameOrId");
    try {
      // JSON/XML DTO via Jackson/JAXB/CXF — not HTML body (see suppressions.md)
      return requireAdaptor().getVirtualSitePreviewStatus(nameOrId); // codeql[java/xss]
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to load virtual site preview status '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Streams one assembled file from the last Virtual Site build (navigable preview).
   *
   * @param nameOrId site name or GUID
   * @param relPath path under the output root; blank uses assembled home
   * @return file bytes
   */
  @GET
  @Path("/{nameOrId}/virtual/preview/{relPath:.*}")
  @Produces(MediaType.WILDCARD)
  @Operation(
      summary = "Preview Virtual Site file",
      description =
          "Streams a file from the last Virtual Site build output (git-filesystem,"
              + " csv-filesystem, sql-database, http-json, object-storage, rss-atom, icalendar,"
              + " sitemap-xml, robots-txt, or llms-txt). Paths are resolved with portable NIO Path under the last output root"
              + " (no '..' after normalize). HTML root-relative href/src/url() values are rewritten to"
              + " this preview prefix so navigation works. rss-atom is a local RSS 2.0 / Atom fixture or"
              + " loopback feed (no live remote feeds). icalendar is a local RFC 5545 calendar.ics"
              + " fixture (no CalDAV). sitemap-xml is last-build local HTML only (sitemap.xml /"
              + " sitemap.file; no live crawl; leftover virtual.remoteUrl and credentials are 400)."
              + " robots-txt is last-build local HTML only (robots.txt / robots.file; no live crawl;"
              + " leftover virtual.remoteUrl and credentials are 400)."
              + " llms-txt is last-build local HTML only (llms.txt / llms.file; no live HTTP fetch;"
              + " leftover virtual.remoteUrl and credentials are 400)."
              + " Requires Admin. Missing files return 404 (not 500). Unsafe paths,"
              + " unknown/repository sourceKind, and files larger than 20 MB return 400.",
      responses = {
        @ApiResponse(responseCode = "200", description = "File bytes"),
        @ApiResponse(responseCode = "400", description = "Not virtual / unsafe path"),
        @ApiResponse(responseCode = "403", description = "Not authorized (Admin required)"),
        @ApiResponse(responseCode = "404", description = "Site or file not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public Response previewVirtualSiteFile(
      @PathParam("nameOrId") String nameOrId, @PathParam("relPath") String relPath) {
    requireNonBlank(nameOrId, "nameOrId");
    try {
      VirtualSitePreviewFile file = requireAdaptor().previewVirtualSiteFile(nameOrId, relPath);
      byte[] body = file.getContent();
      if (file.isHtml()) {
        String base = uriInfo != null ? uriInfo.getBaseUri().getPath() : "/services/";
        String prefix = VirtualSitePreviewHtml.previewPrefix(base, nameOrId);
        body = VirtualSitePreviewHtml.rewriteRootRelative(body, prefix);
      }
      return Response.ok(body, file.getMediaType())
          .header("Cache-Control", "no-store")
          .build(); // codeql[java/xss]
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to preview virtual site file '{}' path='{}' ({}): {}",
          nameOrId,
          relPath,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Builds a Virtual Site and publishes the static output to the Site filesystem publish root.
   *
   * @param nameOrId site name or GUID
   * @return pages written, files copied, and publish path
   */
  @POST
  @Path("/{nameOrId}/virtual/publish")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Operation(
      summary = "Publish Virtual Site to Site filesystem target",
      description =
          "Runs the Virtual Site static build (always using the default output root; unlike POST"
              + " …/virtual/build, this endpoint does not accept an outputRoot override) for"
              + " git-filesystem, csv-filesystem, sql-database (in-memory H2 jdbc:h2:mem: only;"
              + " Oracle, MySQL, and SQL Server JDBC URLs return 400), http-json (local JSON"
              + " fixture or loopback catalog; catalog URL/file stay in _config.yaml;"
              + " virtual.remoteUrl is 400; no secrets on this envelope), object-storage"
              + " (portable-safe local object-key rootPath; no cloud URLs, IAM, or access keys;"
              + " leftover virtual.remoteUrl is 400), rss-atom (local RSS 2.0 / Atom fixture"
              + " or loopback feed.xml / atom.xml; leftover virtual.remoteUrl and credentials"
              + " are 400; no live feeds), icalendar (local RFC 5545 calendar.ics /"
              + " icalendar.file fixture; leftover virtual.remoteUrl and credentials are 400;"
              + " no CalDAV), sitemap-xml (local sitemap.xml / sitemap.file fixture;"
              + " leftover virtual.remoteUrl, credentials, and cloud URL rootPath are 400;"
              + " no live crawl), robots-txt (local robots.txt / robots.file fixture;"
              + " leftover virtual.remoteUrl, credentials, and cloud URL rootPath are 400;"
              + " no live crawl; missing assemble is 400 and does not invent pages), or llms-txt"
              + " (local llms.txt / llms.file fixture; leftover virtual.remoteUrl, credentials,"
              + " and cloud URL rootPath are 400; no live HTTP fetch; missing assemble is 400"
              + " and does not invent pages), then copies"
              + " assembled HTML/assets to the"
              + " Site publishing filesystem location (IPSSite.root) using portable NIO Path I/O."
              + " Requires Admin. Traditional repository Sites, missing/unsafe Site root, or"
              + " overlap with virtual.rootPath return 4xx with an operator-readable message"
              + " (never a silent no-op).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Published (check hasLinkProblems / filesCopied)",
            content = @Content(schema = @Schema(implementation = VirtualSitePublishResult.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Not virtual / missing Site root / unsafe path / overlap"),
        @ApiResponse(responseCode = "403", description = "Not authorized (Admin required)"),
        @ApiResponse(responseCode = "404", description = "Site not found"),
        @ApiResponse(responseCode = "503", description = "Adaptor not configured"),
        @ApiResponse(responseCode = "500", description = "Error")
      })
  public VirtualSitePublishResult publishVirtualSite(@PathParam("nameOrId") String nameOrId) {
    requireNonBlank(nameOrId, "nameOrId");
    try {
      // JSON/XML DTO via Jackson/JAXB/CXF — not HTML body (see suppressions.md)
      return requireAdaptor().publishVirtualSite(nameOrId); // codeql[java/xss]
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to publish virtual site '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private Site resolveSite(String nameOrId) {
    ISiteAdaptor a = requireAdaptor();
    Site byName = a.findByName(nameOrId);
    if (byName != null) {
      return byName;
    }
    return a.findByGuid(nameOrId);
  }

  private ISiteAdaptor requireAdaptor() {
    if (adaptor == null) {
      throw new WebApplicationException(
          "Site adaptor not configured", Response.Status.SERVICE_UNAVAILABLE);
    }
    return adaptor;
  }

  private static void requireNonBlank(String value, String field) {
    if (StringUtils.isBlank(value)) {
      throw new WebApplicationException(field + " is required", Response.Status.BAD_REQUEST);
    }
  }
}
