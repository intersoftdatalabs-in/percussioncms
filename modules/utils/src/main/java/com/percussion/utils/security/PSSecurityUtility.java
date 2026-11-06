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

package com.percussion.utils.security;

import com.percussion.util.PSProperties;
import com.percussion.utils.io.PathUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class currently exposes utility functions for server security options.
 *
 * @see com.percussion.security.SecureStringUtils in the perc-security-utils module to find, add, or
 *     update string functions for validation / security.
 *     <p>Please add any new general security / validation routines there instead of this class.
 */
public final class PSSecurityUtility {

  /** Requires HTTPS flag property. */
  public static final String REQUIRE_HTTPS = "requireHTTPS";
  /** Default for the require-HTTPS flag. */
  public static final String REQUIRE_HTTPS_DEFAULT = "false";
  /** Requires content security policy flag property. */
  public static final String REQUIRE_CONTENT_SECURITY_POLICY = "requireContentSecurityPolicy";
  /** Default for the require-CSP flag. */
  public static final String REQUIRE_CONTENT_SECURITY_POLICY_DEFAULT = "false";
  /** Content security policy property. */
  public static final String CONTENT_SECURITY_POLICY = "contentSecurityPolicy";
  /** Default content security policy. */
  public static final String CONTENT_SECURITY_POLICY_DEFAULT =
      "default-src * data: https: *.percussion.com *.percussion.marketing *.percussion.services ;"
          + " img-src * 'self' data: https: 'unsafe-inline' 'unsafe-eval'; font-src *"
          + " 'unsafe-inline' 'unsafe-eval'; script-src * 'unsafe-inline' 'unsafe-eval'"
          + " *.siteimprove.net ; style-src * 'unsafe-inline' 'unsafe-eval'; frame-src * 'self'"
          + " data: https: http: *.percussion.com *.percussion.marketing *.percussion.services"
          + " 'unsafe-inline' 'unsafe-eval'; frame-ancestors * 'self' ;";
  /** Requires X-Frame-Options flag property. */
  public static final String REQUIRE_XFRAME_OPTIONS = "requireXFrameOptions";
  /** Default for the require-X-Frame-Options flag. */
  public static final String REQUIRE_XFRAME_OPTIONS_DEFAULT = "true";
  /** X-Frame-Options property. */
  public static final String XFRAME_OPTIONS = "xFrameOptions";
  /** Default X-Frame-Options value. */
  public static final String XFRAME_OPTIONS_DEFAULT = "SAMEORIGIN";
  /** Requires XSS protection flag property. */
  public static final String REQUIRE_XSS_PROTECTION = "requireXXSSProtection";
  /** Default for the require-XSS-protection flag. */
  public static final String REQUIRE_XSS_PROTECTION_DEFAULT = "true";
  /** XSS protection property. */
  public static final String XSS_PROTECTION = "xXSSProtection";
  /** Default XSS protection value. */
  public static final String XSS_PROTECTION_DEFAULT = "1; mode=block";
  /** Requires X-Content-Type-Options flag property. */
  public static final String REQUIRE_XCONTENTTYPE_OPTIONS = "requireXContentTypeOptions";
  /** Default for the require-X-Content-Type-Options flag. */
  public static final String REQUIRE_XCONTENTTYPE_OPTIONS_DEFAULT = "true";
  /** X-Content-Type-Options property. */
  public static final String XCONTENTTYPE_OPTIONS = "xContentTypeOptions";
  /** Default X-Content-Type-Options value. */
  public static final String XCONTENTTYPE_OPTIONS_DEFAULT = "nosniff";
  /** Requires strict transport security flag property. */
  public static final String REQUIRE_STRICT_TRANSPORT_SECURITY = "requireStrictTransportSecurity";
  /** Default for the require-HSTS flag. */
  public static final String REQUIRE_STRICT_TRANSPORT_SECURITY_DEFAULT = "false";
  /** HSTS max-age property. */
  public static final String STRICT_TRANSPORT_SECURITY_MAXAGE = "stricttransportsecuritymaxage";
  /** Default HSTS max-age. */
  public static final String STRICT_TRANSPORT_SECURITY_MAXAGE_DEFAULT = "300";
  /** Requires cache control flag property. */
  public static final String REQUIRE_CACHE_CONTROL = "requireCacheControl";
  /** Default for the require-cache-control flag. */
  public static final String REQUIRE_CACHE_CONTROL_DEFAULT = "true";
  /** Cache control property. */
  public static final String CACHE_CONTROL = "cacheControl";
  /** Default cache control value. */
  public static final String CACHE_CONTROL_DEFAULT =
      "no-cache='Set-Cookie, Set-Cookie2',must-revalidate";

  /** X-Frame-Options header name. */
  public static final String HEADER_XFRAMEOPTIONS = "X-Frame-Options";
  /** XSS protection header name. */
  public static final String HEADER_XSSPROTECTION = "X-XSS-Protection";
  /** X-Content-Type-Options header name. */
  public static final String HEADER_XCONTENTTYPEOPTIONS = "X-Content-Type-Options";
  /** Content-Security-Policy header name. */
  public static final String HEADER_CONTENTSECURITY_POLICY = "Content-Security-Policy";
  /** Strict-Transport-Security header name. */
  public static final String HEADER_STRICTTRANSPORTSECURITY = "Strict-Transport-Security";
  /** Max-age header name. */
  public static final String HEADER_MAXAGE = "max-age";
  /** Cache-Control header name. */
  public static final String HEADER_CACHE_CONTROL = "Cache-Control";
  /** Session cache control value. */
  public static final String CACHE_CONTROL_SESSION = "no-cache,must-revalidate";

  private Boolean isHTTPSRequired = null;
  private Boolean isStrictTransportSecurityRequired = null;
  private Boolean isContentSecurityPolicyRequired = null;
  private Boolean isxFrameOptionsRequired = null;
  private Boolean isXXSSProtectionRequired = null;
  private Boolean isXContentTypeOptionsRequired = null;
  private Boolean cacheControlRequired = null;
  private String contentSecurityPolicy = null;
  private Integer strictTransportSecurityMaxAge = null;
  private String xFrameOptions = null;
  private String xXSSProtection = null;
  private String xContentTypeOptions = null;
  private String cacheControl = null;

  private static final Logger log = LogManager.getLogger("Securityy");

  /** Creates a security utility. */
  public PSSecurityUtility() {}

  /**
   * If requireHTTPS server property exists and its value is set to true or yes. The value is
   * locally stored in a variable, instead of reading from a property each time.
   *
   * @return <code>true</code> if it is a set to true.
   */
  public boolean httpsRequired() {
    if (isHTTPSRequired != null) {
      return isHTTPSRequired;
    }
    boolean result = false;
    Properties serverProps = getServerProperties();
    if (serverProps != null) {
      String httpsProperty = serverProps.getProperty(REQUIRE_HTTPS, "false");
      if (StringUtils.isNotBlank(httpsProperty)
          && (httpsProperty.equalsIgnoreCase("true") || httpsProperty.equalsIgnoreCase("yes"))) {
        isHTTPSRequired = Boolean.TRUE;
        result = true;
      } else {
        isHTTPSRequired = Boolean.FALSE;
      }
    }
    return result;
  }

  /**
   * Returns whether cache control is required.
   *
   * @return <code>true</code> if required
   */
  public boolean isCacheControlRequired() {
    if (cacheControlRequired != null) {
      return cacheControlRequired;
    }

    Properties serverProps = getServerProperties();
    if (serverProps != null) {
      String value = serverProps.getProperty(REQUIRE_CACHE_CONTROL, REQUIRE_CACHE_CONTROL_DEFAULT);
      if (StringUtils.isNotBlank(value)
          && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"))) {
        cacheControlRequired = Boolean.TRUE;

      } else {
        cacheControlRequired = Boolean.FALSE;
      }
    }
    return cacheControlRequired;
  }

  /**
   * Returns the cache control header value.
   *
   * @return the value, may be <code>null</code>
   */
  public String getCacheControl() {
    if (cacheControl != null) {
      return cacheControl;
    }

    Properties serverProps = getServerProperties();
    if (serverProps != null) {
      String value = serverProps.getProperty(CACHE_CONTROL, CACHE_CONTROL_DEFAULT);
      cacheControl = value;
    }
    return cacheControl;
  }

  /**
   * Returns whether a content security policy is required.
   *
   * @return <code>true</code> if required
   */
  public boolean contentSecurityPolicyRequired() {
    if (isContentSecurityPolicyRequired != null) {
      return isContentSecurityPolicyRequired;
    }
    boolean result = false;
    Properties serverProps = getServerProperties();
    if (serverProps != null) {
      String cspProperty =
          serverProps.getProperty(
              REQUIRE_CONTENT_SECURITY_POLICY, REQUIRE_CONTENT_SECURITY_POLICY_DEFAULT);
      if (StringUtils.isNotBlank(cspProperty)
          && (cspProperty.equalsIgnoreCase("true") || cspProperty.equalsIgnoreCase("yes"))) {
        isContentSecurityPolicyRequired = Boolean.TRUE;
        result = true;
      } else {
        isContentSecurityPolicyRequired = Boolean.FALSE;
      }
    }
    return result;
  }

  /**
   * Returns the content security policy.
   *
   * @return the policy, may be <code>null</code>
   */
  public String getContentSecurityPolicy() {
    if (contentSecurityPolicy != null) {
      return contentSecurityPolicy;
    }

    Properties serverProps = getServerProperties();
    if (serverProps != null) {
      String csp =
          serverProps.getProperty(CONTENT_SECURITY_POLICY, CONTENT_SECURITY_POLICY_DEFAULT);
      contentSecurityPolicy = csp;
    }
    return contentSecurityPolicy;
  }

  /**
   * Returns whether X-Frame-Options is required.
   *
   * @return <code>true</code> if required
   */
  public boolean xFrameOptionsRequired() {
    if (isxFrameOptionsRequired != null) {
      return isxFrameOptionsRequired.booleanValue();
    }
    boolean result = false;
    Properties serverProps = getServerProperties();
    if (serverProps != null) {
      String frameProperty =
          serverProps.getProperty(REQUIRE_XFRAME_OPTIONS, REQUIRE_XFRAME_OPTIONS_DEFAULT);
      if (StringUtils.isNotBlank(frameProperty)
          && (frameProperty.equalsIgnoreCase("true") || frameProperty.equalsIgnoreCase("yes"))) {
        isxFrameOptionsRequired = Boolean.TRUE;
        result = true;
      } else {
        isxFrameOptionsRequired = Boolean.FALSE;
      }
    }
    return result;
  }

  /**
   * Returns the X-Frame-Options header value.
   *
   * @return the value, may be <code>null</code>
   */
  public String getXFrameOptions() {
    if (xFrameOptions != null) {
      return xFrameOptions;
    }

    Properties serverProps = getServerProperties();
    if (serverProps != null) {
      String frameOptions = serverProps.getProperty(XFRAME_OPTIONS, XFRAME_OPTIONS_DEFAULT);
      xFrameOptions = frameOptions;
    }
    return xFrameOptions;
  }

  /**
   * Returns whether XSS protection is required.
   *
   * @return <code>true</code> if required
   */
  public boolean xXSSProtectionRequired() {
    if (isXXSSProtectionRequired != null) {
      return isXXSSProtectionRequired.booleanValue();
    }
    boolean result = false;
    Properties serverProps = getServerProperties();
    if (serverProps != null) {
      String xssProperty =
          serverProps.getProperty(REQUIRE_XSS_PROTECTION, REQUIRE_XSS_PROTECTION_DEFAULT);
      if (StringUtils.isNotBlank(xssProperty)
          && (xssProperty.equalsIgnoreCase("true") || xssProperty.equalsIgnoreCase("yes"))) {
        isXXSSProtectionRequired = Boolean.TRUE;
        result = true;
      } else {
        isXXSSProtectionRequired = Boolean.FALSE;
      }
    }
    return result;
  }

  /**
   * Returns the XSS protection header value.
   *
   * @return the value, may be <code>null</code>
   */
  public String getXXSSProtection() {
    if (xXSSProtection != null) {
      return xXSSProtection;
    }

    Properties serverProps = getServerProperties();
    if (serverProps != null) {
      String xssProtection = serverProps.getProperty(XSS_PROTECTION, XSS_PROTECTION_DEFAULT);
      xXSSProtection = xssProtection;
    }
    return xXSSProtection;
  }

  /**
   * Returns whether X-Content-Type-Options is required.
   *
   * @return <code>true</code> if required
   */
  public boolean xContentTypeOptionsRequired() {
    if (isXContentTypeOptionsRequired != null) {
      return isXContentTypeOptionsRequired.booleanValue();
    }
    boolean result = false;
    Properties serverProps = getServerProperties();
    if (serverProps != null) {
      String ctProperty =
          serverProps.getProperty(
              REQUIRE_XCONTENTTYPE_OPTIONS, REQUIRE_XCONTENTTYPE_OPTIONS_DEFAULT);
      if (StringUtils.isNotBlank(ctProperty)
          && (ctProperty.equalsIgnoreCase("true") || ctProperty.equalsIgnoreCase("yes"))) {
        isXContentTypeOptionsRequired = Boolean.TRUE;
        result = true;
      } else {
        isXContentTypeOptionsRequired = Boolean.FALSE;
      }
    }
    return result;
  }

  /**
   * Returns the X-Content-Type-Options header value.
   *
   * @return the value, may be <code>null</code>
   */
  public String getXContentTypeOptions() {
    if (xContentTypeOptions != null) {
      return xContentTypeOptions;
    }

    Properties serverProps = getServerProperties();
    if (serverProps != null) {
      String ctOptions =
          serverProps.getProperty(XCONTENTTYPE_OPTIONS, XCONTENTTYPE_OPTIONS_DEFAULT);
      xContentTypeOptions = ctOptions;
    }
    return xContentTypeOptions;
  }

  /**
   * Returns whether strict transport security is required.
   *
   * @return <code>true</code> if required
   */
  public boolean strictTransportSecurityRequired() {
    if (isStrictTransportSecurityRequired != null) {
      return isStrictTransportSecurityRequired.booleanValue();
    }
    boolean result = false;
    Properties serverProps = getServerProperties();
    if (serverProps != null) {
      String stsProperty =
          serverProps.getProperty(
              REQUIRE_STRICT_TRANSPORT_SECURITY, REQUIRE_STRICT_TRANSPORT_SECURITY_DEFAULT);
      if (StringUtils.isNotBlank(stsProperty)
          && (stsProperty.equalsIgnoreCase("true") || stsProperty.equalsIgnoreCase("yes"))) {
        isStrictTransportSecurityRequired = Boolean.TRUE;
        result = true;
      } else {
        isStrictTransportSecurityRequired = Boolean.FALSE;
      }
    }
    return result;
  }

  /**
   * Returns the HSTS max-age.
   *
   * @return the max-age in seconds
   */
  public int getStrictTransportSecurityMaxAge() {
    if (strictTransportSecurityMaxAge != null) {
      return strictTransportSecurityMaxAge.intValue();
    }
    int result = 0;
    Properties serverProps = getServerProperties();
    if (serverProps != null) {
      String stsMaxAge =
          serverProps.getProperty(
              STRICT_TRANSPORT_SECURITY_MAXAGE, STRICT_TRANSPORT_SECURITY_MAXAGE);
      if (NumberUtils.isNumber(stsMaxAge)) {
        result = NumberUtils.toInt(stsMaxAge);
        if (result > 0) {
          strictTransportSecurityMaxAge = result;
        } else {
          result = 0;
        }
      }
    }
    return result;
  }

  /**
   * Will return an instance of secure random. Will attempt to return a StrongSecureRandom first but
   * will return a standard SecureRandom if Strong is unavailable. May return null if secure random
   * cannot be initialized.
   *
   * @return a secure random instance, may be <code>null</code>
   */
  public static SecureRandom getSecureRandom() {
    SecureRandom ret = new SecureRandom();
    return ret;
  }

  /**
   * Returns the server properties, loading them on first use.
   *
   * @return the server properties, never <code>null</code>
   */
  private PSProperties getServerProperties() {
    File propFile = PSProperties.getConfig(ENTRY_NAME, PROPS_SERVER, getRxConfigDir(SERVER_DIR));

    try {
      ms_serverProps = new PSProperties(propFile.getPath());
    } catch (FileNotFoundException e) {
      log.error("File not found in PSProperties", e);
    } catch (IOException e) {
      log.error("IO Exception in PSProperties", e);
    }

    return ms_serverProps;
  }

  /**
   * Returns the absolute path of a path relative to the installation root.
   *
   * @param path the relative path, never <code>null</code>
   * @return the absolute path, never <code>null</code>
   * @throws IllegalArgumentException if the resolved file does not exist
   */
  public static String getRxConfigDir(String path) {

    File item = new File(PathUtils.getRxDir(null), path);
    if (item.exists() == false) {
      throw new IllegalArgumentException("file does not exist: " + item.getAbsolutePath());
    }
    return item.getAbsolutePath();
  }

  /** Server properties file name. */
  public static final String PROPS_SERVER = "server.properties";

  /** Constant for the name of the entry that reperesents workflow's name/value pair. */
  public static final String ENTRY_NAME = "server_config_base_dir";

  /** Constant for the directory containing all other configuration directories. */
  public static final String BASE_CONFIG_DIR = "rxconfig";

  /**
   * Constant for the directory containing server configs. Assumed to be relative to the Rx
   * directory. No trailing slash.
   */
  public static final String SERVER_DIR = BASE_CONFIG_DIR + "/Server";

  private static PSProperties ms_serverProps = new PSProperties();
}
