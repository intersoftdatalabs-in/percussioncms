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
package com.percussion.pagemanagement.assembler;

import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.pagemanagement.data.PSRenderLink;
import com.percussion.pagemanagement.data.PSRenderLinkContext.Mode;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSAssetResource;
import com.percussion.pagemanagement.data.PSResourceInstance;
import com.percussion.pagemanagement.data.PSResourceLinkAndLocation;
import com.percussion.pagemanagement.data.PSResourceLocation;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.phloc.commons.url.URLValidator;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Static utility methods for resource locations and links.
 *
 * @author adamgent
 */
public class PSResourceLinkAndLocationUtils {

  /**
   * URL-escapes a path string while keeping the slashes of the path. The slashes ('/') will not be
   * escaped.
   *
   * <p>This is URL escaping for links, not encoding for forms.
   *
   * @param path never {@code null}.
   * @return never {@code null}.
   */
  public static String escapePathForUrl(String path) {
    notNull(path);
    try {
      var uri = new URI("http", "localhost", path, null);
      return uri.getRawPath();
    } catch (URISyntaxException e) {
      throw new RuntimeException("Bad path", e);
    }
  }

  /**
   * Concatenates a path by adding the appropriate path separator between each argument.
   *
   * @param start never {@code null} or empty.
   * @param end folder parts.
   * @return never {@code null}.
   */
  public static String concatPath(String start, String... end) {
    isTrue(isNotBlank(start), "start cannot be blank");
    notEmpty(end, "Must have end paths.");
    var path = start;
    for (var p : end) {
      if (isNotBlank(p)) {
        path = removeEnd(path, "/") + "/" + removeStart(p, "/");
      }
    }
    if ("/".equals(path)) {
      return path;
    }
    return removeEnd(path, "/");
  }

  /**
   * Creates a single link and location for the given filename. The default resolution for link and
   * location path will be used. The link will also be properly URL-escaped.
   *
   * @param r ($perc.resourceInstance) never {@code null}.
   * @param fileName never {@code null} or empty and should not contain '/'.
   * @return never {@code null}.
   */
  public static PSResourceLinkAndLocation createLinkAndLocationForFileName(
      PSResourceInstance r, String fileName) {
    notEmpty(fileName, "fileName");
    isTrue(!fileName.contains("/"), "fileName cannot contain slashes");
    var path = r.getLocationFolderPath();
    path = concatPath(path, fileName);
    var urlPath = removeStart(escapePathForUrl(path), "/");
    var baseUri = r.getRelativeBaseUri();
    notNull(baseUri);
    var uri = baseUri.resolve(urlPath);
    var url = uri.toASCIIString();

    // If our link is published, append analytics ID.
    url = appendAnalyticsId(r, url);

    return createLinkAndLocation(path, url);
  }

  /**
   * Append the analytics ID encoded onto the end of the URL.
   *
   * @param r The content containing our link and link context. Used to obtain the analytics ID.
   * @param url Our URL we will append onto.
   * @return The URL plus the analytics ID if valid when combined.
   */
  private static String appendAnalyticsId(PSResourceInstance r, String url) {
    if (r.getLinkContext().getMode().equals(Mode.PUBLISH)) {
      var contentItem = r.getItem();
      if (contentItem == null) {
        return url;
      }
      var analyticsId = (String) contentItem.getFields().get("analyticsId");
      if (StringUtils.isNotBlank(analyticsId)) {
        int indexOfQuestionMark = analyticsId.indexOf("?");
        if (indexOfQuestionMark != 0) {
          analyticsId = "?" + analyticsId;
        }
      }
      analyticsId = StringUtils.substringAfter(analyticsId, "?");
      if (StringUtils.isNotBlank(analyticsId)) {
        var analyticsIdHalves = analyticsId.split("&");
        for (int i = 0; i < analyticsIdHalves.length; i++) {
          try {
            if (analyticsId.contains("=")) {
              var individualParam = analyticsIdHalves[i].split("=");
              var encodedValue =
                  URLEncoder.encode(individualParam[0], UTF_8)
                      + "="
                      + URLEncoder.encode(individualParam[1], UTF_8);
              if (i == 0) {
                url += "?" + encodedValue;
              } else {
                url += "&" + encodedValue;
              }
            } else {
              url += "?" + URLEncoder.encode(analyticsId, UTF_8);
            }
          } catch (UnsupportedEncodingException e) {
            log.error("Failed to encode url: {}. Exception is: {}", url, e);
          }
        }
        // Cross-site links are fully qualified; non-cross-site are relative.
        var checkUrl = url.startsWith("http") ? url : "http://localhost" + url;
        if (!URLValidator.isValid(checkUrl)) {
          log.warn("The link to asset with analyticsId: {} appears to be invalid.", url);
        }
      }
    }
    return url;
  }

  /**
   * Creates a default link and location using the item's title. It also appends the "suffix" of the
   * template if the context is delivery (publish location).
   *
   * @param evalContext never {@code null}.
   * @return never {@code null}.
   */
  public static PSResourceLinkAndLocation createDefaultLinkAndLocation(
      PSResourceScriptEvaluatorContext evalContext) {
    return createDefaultLinkAndLocation(evalContext.getResourceInstance(), getAssemblyService());
  }

  private static IPSAssemblyService getAssemblyService() {
    if (ms_assemblyService == null) {
      ms_assemblyService = PSAssemblyServiceLocator.getAssemblyService();
    }
    return ms_assemblyService;
  }

  private static IPSAssemblyService ms_assemblyService = null;

  /**
   * This does the same as {@link #createDefaultLinkAndLocation(PSResourceScriptEvaluatorContext)}.
   *
   * @param r the resource instance, never {@code null}.
   * @param assemblyService the assembly service, never {@code null}.
   * @return the default link and location.
   */
  public static PSResourceLinkAndLocation createDefaultLinkAndLocation(
      PSResourceInstance r, IPSAssemblyService assemblyService) {
    notNull(r);
    notNull(assemblyService);

    if (log.isDebugEnabled()) {
      log.debug("Getting default links for resource: {}", r);
    }
    var baseName = (String) r.getItem().getFields().get("sys_title");
    var fileName = baseName;
    if (r.getLinkContext().isDeliveryContext()) {
      var suffix = getLocationSuffix(r.getResourceDefinition(), assemblyService);
      if (!isBlank(suffix)) {
        fileName = fileName + suffix;
      }
    }
    return createLinkAndLocationForFileName(r, fileName);
  }

  /**
   * Gets the location suffix property from the template that is defined by the specified asset
   * resource.
   *
   * @param r the asset resource, assumed not {@code null}.
   * @param assemblyService the assembly service, never {@code null}.
   * @return the location suffix, or {@code null} if the template does not exist.
   */
  private static String getLocationSuffix(PSAssetResource r, IPSAssemblyService assemblyService) {
    var templateName = r.getLegacyTemplate();
    try {
      var template = assemblyService.findTemplateByName(templateName);
      return template.getLocationSuffix();
    } catch (PSAssemblyException e) {
      log.error("Failed to find template: \"{}\"", templateName, e);
      return null;
    }
  }

  /**
   * Create a list with a single entry of the default link and location for the item.
   *
   * @param evalContext never {@code null}.
   * @return a list with a single link and location; the list can be updated and is not immutable.
   */
  public static List<PSResourceLinkAndLocation> createDefaultLinkAndLocations(
      PSResourceScriptEvaluatorContext evalContext) {
    var locations = new ArrayList<PSResourceLinkAndLocation>();
    locations.add(createDefaultLinkAndLocation(evalContext));
    return locations;
  }

  /**
   * Creates a link and location.
   *
   * @param filePath never {@code null} or empty.
   * @param url should be an escaped URL, never {@code null} or empty.
   * @return never {@code null}.
   */
  public static PSResourceLinkAndLocation createLinkAndLocation(String filePath, String url) {
    notEmpty(filePath, "filePath");
    notEmpty(url, "url");
    var rl = new PSResourceLinkAndLocation();
    var link = new PSRenderLink();
    var location = new PSResourceLocation();
    link.setUrl(url);
    location.setFilePath(filePath);
    rl.setRenderLink(link);
    rl.setResourceLocation(location);
    return rl;
  }

  /**
   * Validates that the path is safe to use as a physical filesystem folder path on Windows and
   * Linux. Mainly checks to see if there are any bad characters in the folder path. The bad
   * characters that are not allowed come from the Windows operating system as it is more
   * restrictive than Unix.
   *
   * <p>The following characters are bad: {@code \ | < > ? " : *}
   *
   * @param path never {@code null} or empty.
   * @throws IllegalArgumentException if the path is bad
   */
  public static void validateAsPhysicalPath(String path) {
    notEmpty(path, "invalid path");
    isTrue(!containsAny(path, "\\|<>?\":*"), "invalid path");
  }

  private static final Logger log = LogManager.getLogger(PSResourceLinkAndLocationUtils.class);
  private static final String UTF_8 = "UTF-8";
}
