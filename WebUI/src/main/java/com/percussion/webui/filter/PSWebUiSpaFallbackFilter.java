/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.webui.filter;

import com.percussion.webui.util.PSEditorHostRedirect;
import com.percussion.webui.util.PSLegacyViewRedirect;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * PR-9: internal forward for path-based SPA client routes so BrowserRouter refresh does not 404
 * under Jetty.
 *
 * <p>GET {@code /cm/app/{entry}[/**]} and {@code /cm/pages/app/{entry}[/**]} → forward to the
 * tree's {@code spa.jsp} with an allowlisted {@code entry} (and section/tab when present). Real
 * JSPs, static assets, and non-SPA first segments pass through unchanged.
 *
 * <p>Retired Architecture hosts {@code /cm/app/siteArchitecture.jsp} and {@code
 * /cm/pages/app/siteArchitecture.jsp} are not shipped (#3587). GET bookmarks 301 to SPA {@code
 * ?view=arch} so operators keep the old URL. Retired {@code editAsset.jsp} hosts (#3473) 301 to
 * {@code ?view=editor}, which the dispatcher maps to {@code spa.jsp?entry=editor}.
 *
 * <p>Misplaced editor/chrome images under {@code /cm/app/images/**} or {@code
 * /cm/pages/app/images/**} are remapped to {@code /cm/images/**} so those URLs return 200 instead
 * of 404 (#3332).
 *
 * <p>Server deep links and login return remain the query contract ({@code spa.jsp?entry=…}); this
 * filter only supports clean client path URLs after first paint.
 */
public class PSWebUiSpaFallbackFilter implements Filter {

  /** Default constructor for servlet container instantiation. */
  public PSWebUiSpaFallbackFilter() {}

  /** SPA entry tokens (lockstep with TS SPA_ENTRIES). */
  static final Set<String> SPA_ENTRIES =
      Set.of(
          "home",
          "publish",
          "workflow",
          "admin",
          "widget-builder",
          "developer",
          "design",
          "architecture",
          "explorer",
          "profile",
          "assembly",
          "editor",
          "unavailable");

  private static final String APP_PREFIX = "/cm/app";
  private static final String PAGES_PREFIX = "/cm/pages/app";
  /** Canonical static image tree (not under the SPA mount). */
  private static final String CANONICAL_IMAGES = "/cm/images";

  @Override
  public void init(FilterConfig filterConfig) {
    // no-op
  }

  @Override
  public void destroy() {
    // no-op
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
      chain.doFilter(request, response);
      return;
    }
    HttpServletRequest httpReq = (HttpServletRequest) request;
    String method = httpReq.getMethod();
    if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
      chain.doFilter(request, response);
      return;
    }

    String pathWithinContext = pathWithinContext(httpReq);
    String retiredLocation =
        buildRetiredJspRedirectLocation(pathWithinContext, httpReq.getQueryString());
    if (retiredLocation != null) {
      HttpServletResponse httpResp = (HttpServletResponse) response;
      httpResp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
      httpResp.setHeader(
          "Location", withContextPath(httpReq.getContextPath(), retiredLocation));
      return;
    }

    String imageRemap = buildStaticImageRemapPath(pathWithinContext);
    if (imageRemap != null) {
      RequestDispatcher remapDispatcher = request.getRequestDispatcher(imageRemap);
      if (remapDispatcher != null) {
        remapDispatcher.forward(request, response);
        return;
      }
    }

    String forward = buildSpaForwardPath(pathWithinContext, httpReq.getQueryString());
    if (forward == null) {
      chain.doFilter(request, response);
      return;
    }

    RequestDispatcher dispatcher = request.getRequestDispatcher(forward);
    if (dispatcher == null) {
      chain.doFilter(request, response);
      return;
    }
    dispatcher.forward(request, response);
  }

  /**
   * Path inside the webapp (context path stripped). Never null; starts with {@code /} when
   * non-empty.
   */
  static String pathWithinContext(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri == null) {
      return "";
    }
    String context = request.getContextPath();
    if (context != null && !context.isEmpty() && uri.startsWith(context)) {
      uri = uri.substring(context.length());
    }
    // Decode is left to the container for path info; strip semicolon matrix params
    int semi = uri.indexOf(';');
    if (semi >= 0) {
      uri = uri.substring(0, semi);
    }
    if (uri.isEmpty()) {
      return "/";
    }
    return uri;
  }

  /**
   * 301 Location for retired classic JSP bookmarks (#3587 Architecture, #3473 editAsset).
   *
   * <p>The JSP hosts are no longer in the WAR. Bookmarks still land on the SPA via {@link
   * PSLegacyViewRedirect}. Other JSPs return {@code null} (pass through).
   *
   * @param pathWithinContext webapp path (context may still be present if the container did not
   *     strip it); may be null
   * @param queryString raw query without {@code ?}; may be null
   * @return SPA Location starting with {@code /cm/app/?view=}, or {@code null}
   */
  static String buildRetiredJspRedirectLocation(String pathWithinContext, String queryString) {
    if (pathWithinContext == null || pathWithinContext.isEmpty()) {
      return null;
    }
    String lower = pathWithinContext.toLowerCase(Locale.ROOT);
    if (isRetiredArchitectureJsp(lower)) {
      return PSLegacyViewRedirect.buildLocation("arch", queryString);
    }
    if (PSEditorHostRedirect.isRetiredEditAssetJsp(lower)) {
      return PSLegacyViewRedirect.buildLocation("editor", queryString);
    }
    return null;
  }

  /**
   * True when {@code lowerPath} is a retired Architecture JSP bookmark (app or pages tree). Accepts
   * an optional leading context prefix such as {@code /rhythmyx} when {@link
   * #pathWithinContext(HttpServletRequest)} could not strip it.
   */
  static boolean isRetiredArchitectureJsp(String lowerPath) {
    if (lowerPath == null || !lowerPath.endsWith("/sitearchitecture.jsp")) {
      return false;
    }
    return lowerPath.contains(APP_PREFIX + "/") || lowerPath.contains(PAGES_PREFIX + "/");
  }

  /**
   * Prefix a root-relative Location with the servlet context path when the container deploys the
   * WAR under a non-empty context ({@code /Rhythmyx}).
   *
   * @param contextPath {@code request.getContextPath()}; may be null or empty
   * @param location root-relative Location from {@link PSLegacyViewRedirect}; never rewritten when
   *     null/empty or already context-prefixed
   * @return never null when {@code location} is non-null
   */
  public static String withContextPath(String contextPath, String location) {
    if (location == null || location.isEmpty()) {
      return location;
    }
    if (contextPath == null || contextPath.isEmpty() || "/".equals(contextPath)) {
      return location;
    }
    String ctx = contextPath;
    if (ctx.endsWith("/")) {
      ctx = ctx.substring(0, ctx.length() - 1);
    }
    if (!location.startsWith("/") || location.startsWith(ctx + "/") || location.equals(ctx)) {
      return location;
    }
    return ctx + location;
  }

  /**
   * Remap misplaced SPA-mount image URLs onto the real {@code /cm/images} tree.
   *
   * <p>Editor chrome historically requested {@code /cm/pages/app/images/icons/editor/*.png} (and
   * {@code /cm/app/images/...}) after a dual-tree rewrite. Those paths 404 because assets live at
   * {@code /cm/images/...}. Returning a forward path here serves the file without waiting on JSP
   * callers to be updated.
   *
   * @return canonical {@code /cm/images/...} path, or {@code null} when this is not a remappable
   *     image request
   */
  static String buildStaticImageRemapPath(String pathWithinContext) {
    if (pathWithinContext == null || pathWithinContext.isEmpty()) {
      return null;
    }
    String lower = pathWithinContext.toLowerCase(Locale.ROOT);
    String misplacedPrefix;
    if (lower.startsWith(PAGES_PREFIX + "/images/")) {
      misplacedPrefix = PAGES_PREFIX + "/images";
    } else if (lower.startsWith(APP_PREFIX + "/images/")) {
      misplacedPrefix = APP_PREFIX + "/images";
    } else {
      return null;
    }
    if (!isRemappableImageExtension(lower)) {
      return null;
    }
    String rest = pathWithinContext.substring(misplacedPrefix.length());
    if (rest.isEmpty() || "/".equals(rest)) {
      return null;
    }
    String[] segments = rest.split("/");
    for (String seg : segments) {
      if (seg.isEmpty()) {
        continue;
      }
      if (isUnsafePathSegment(seg)) {
        return null;
      }
    }
    return CANONICAL_IMAGES + rest;
  }

  private static boolean isRemappableImageExtension(String lowerPath) {
    return lowerPath.endsWith(".png")
        || lowerPath.endsWith(".gif")
        || lowerPath.endsWith(".jpg")
        || lowerPath.endsWith(".jpeg")
        || lowerPath.endsWith(".svg")
        || lowerPath.endsWith(".ico")
        || lowerPath.endsWith(".webp");
  }

  /**
   * Build an internal forward path to {@code spa.jsp?entry=…} when {@code pathWithinContext} is a
   * path-based SPA client route; otherwise {@code null} (pass through).
   *
   * @param pathWithinContext e.g. {@code /cm/app/home/library} or {@code /Rhythmyx}-stripped form
   * @param queryString raw query without {@code ?}, may be null
   */
  static String buildSpaForwardPath(String pathWithinContext, String queryString) {
    if (pathWithinContext == null || pathWithinContext.isEmpty()) {
      return null;
    }
    // Never rewrite explicit resources
    String lower = pathWithinContext.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".jsp")
        || lower.endsWith(".js")
        || lower.endsWith(".css")
        || lower.endsWith(".map")
        || lower.endsWith(".png")
        || lower.endsWith(".gif")
        || lower.endsWith(".jpg")
        || lower.endsWith(".jpeg")
        || lower.endsWith(".svg")
        || lower.endsWith(".ico")
        || lower.endsWith(".woff")
        || lower.endsWith(".woff2")
        || lower.endsWith(".ttf")
        || lower.contains("/cm/modern/")) {
      return null;
    }

    String prefix;
    String remainder;
    if (pathWithinContext.equals(APP_PREFIX) || pathWithinContext.startsWith(APP_PREFIX + "/")) {
      prefix = APP_PREFIX;
      remainder =
          pathWithinContext.length() == APP_PREFIX.length()
              ? ""
              : pathWithinContext.substring(APP_PREFIX.length());
    } else if (pathWithinContext.equals(PAGES_PREFIX)
        || pathWithinContext.startsWith(PAGES_PREFIX + "/")) {
      prefix = PAGES_PREFIX;
      remainder =
          pathWithinContext.length() == PAGES_PREFIX.length()
              ? ""
              : pathWithinContext.substring(PAGES_PREFIX.length());
    } else {
      return null;
    }

    // /cm/app or /cm/app/ alone → index.jsp / dispatcher, not SPA path route
    if (remainder.isEmpty() || "/".equals(remainder)) {
      return null;
    }
    if (!remainder.startsWith("/")) {
      return null;
    }

    // Split path segments (ignore empty from trailing slash)
    String trimmed = remainder;
    while (trimmed.startsWith("/")) {
      trimmed = trimmed.substring(1);
    }
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    if (trimmed.isEmpty()) {
      return null;
    }
    // split on non-empty trimmed path always yields length >= 1
    String[] segments = trimmed.split("/");

    // Reject path traversal / odd segments (decoded + encoded forms — Kilo #1542)
    for (String seg : segments) {
      if (isUnsafePathSegment(seg)) {
        return null;
      }
    }

    String entry = segments[0].toLowerCase(Locale.ROOT);
    if ("widgetbuilder".equals(entry)) {
      entry = "widget-builder";
    }
    if ("arch".equals(entry) || "navigation".equals(entry)) {
      entry = "architecture";
    }
    if (!SPA_ENTRIES.contains(entry)) {
      return null;
    }

    StringBuilder forward = new StringBuilder(prefix).append("/spa.jsp?entry=");
    forward.append(urlEncode(entry));

    if (segments.length >= 2) {
      String second = segments[1];
      if ("home".equals(entry)
          || "publish".equals(entry)
          || "developer".equals(entry)
          || "design".equals(entry)) {
        forward.append("&section=").append(urlEncode(second));
      } else if ("workflow".equals(entry) || "admin".equals(entry)) {
        forward.append("&tab=").append(urlEncode(second));
      } else if ("architecture".equals(entry)) {
        // Site name path segment (#3094) → query site=
        forward.append("&site=").append(urlEncode(second));
      }
      // explorer / widget-builder / unavailable: ignore extra path segments
    }

    if (queryString != null && !queryString.isBlank()) {
      // Preserve allowlisted deep-link params (path, siteId, serverId, section, tab, …)
      // without duplicating entry if already present.
      String[] pairs = queryString.split("&");
      for (String pair : pairs) {
        if (pair.isEmpty()) {
          continue;
        }
        int eq = pair.indexOf('=');
        String key = eq >= 0 ? pair.substring(0, eq) : pair;
        if ("entry".equalsIgnoreCase(key)) {
          continue;
        }
        forward.append('&').append(pair);
      }
    }
    return forward.toString();
  }

  private static String urlEncode(String raw) {
    return URLEncoder.encode(raw, StandardCharsets.UTF_8);
  }

  /**
   * True when a path segment must not be forwarded as an SPA route segment. Rejects empty segments,
   * {@code ..}, backslashes, embedded slashes after decode, and URL-encoded dot sequences ({@code
   * %2e%2e}) for defense-in-depth when the container leaves encoding in {@code getRequestURI()}.
   */
  static boolean isUnsafePathSegment(String segment) {
    if (segment == null || segment.isEmpty()) {
      return true;
    }
    if (segment.indexOf('\\') >= 0) {
      return true;
    }
    // Raw encoded traversal (case-insensitive), before or without decode
    String lower = segment.toLowerCase(Locale.ROOT);
    if (lower.contains("%2e") || lower.contains("%252e")) {
      return true;
    }
    if ("..".equals(segment) || ".".equals(segment)) {
      return true;
    }
    String decoded = segment;
    try {
      decoded = URLDecoder.decode(segment, StandardCharsets.UTF_8);
      // Double-encoded payloads
      if (decoded.contains("%")) {
        decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
      }
    } catch (IllegalArgumentException ex) {
      // Malformed escape — do not forward
      return true;
    }
    if ("..".equals(decoded) || ".".equals(decoded)) {
      return true;
    }
    // Reject decode that introduces path separators (encoded slash in segment)
    if (decoded.indexOf('/') >= 0 || decoded.indexOf('\\') >= 0) {
      return true;
    }
    return false;
  }
}
