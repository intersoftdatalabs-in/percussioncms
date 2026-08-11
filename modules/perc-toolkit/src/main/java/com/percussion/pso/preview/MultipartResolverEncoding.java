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
package com.percussion.pso.preview;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/**
 * A multipart resolver that fixes up the encoding. The Content Explorer Applet emits non-standard
 * MIME headers, and this can confuse JBoss/Tomcat, so to get around this, we look for any charset
 * that contains a semicolon, and strip off the junk that follows.
 *
 * @author DavidBenua
 */
public class MultipartResolverEncoding extends StandardServletMultipartResolver {
  /**
   * Creates a new MultipartResolverEncoding.
   */
  public MultipartResolverEncoding() {
    // default
  }


  private static final Logger log = LogManager.getLogger(MultipartResolverEncoding.class);

  /**
   * Determine the cleaned character encoding for the supplied request. The Content Explorer applet
   * sometimes sends headers like "text/plain; charset=UTF-8; some-bogus" which confuse the standard
   * resolver. Our strategy is to strip off any portion after the first semicolon. This helper is
   * public primarily to support unit testing; the behaviour is kept in sync with {@link
   * #resolveMultipart(HttpServletRequest)}.
   *
   * @param request the servlet request, never <code>null</code>
   * @return the sanitized encoding or <code>null</code> if none
   */
  public String determineEncoding(HttpServletRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request may not be null");
    }
    String enc = request.getCharacterEncoding();
    if (enc != null && enc.contains(";")) {
      return StringUtils.substringBefore(enc, ";");
    }
    return enc;
  }

  /**
   * Override resolveMultipart so we can clean up any malformed charset values sent by the Content
   * Explorer applet. The standard resolver does not expose determineEncoding, so we wrap the
   * request and sanitize the values before delegating.
   * @param request the request
   * @return the result
   * @throws MultipartException if an error occurs
   */
  @Override
  public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request)
      throws MultipartException {
    HttpServletRequestWrapper cleaned =
        new HttpServletRequestWrapper(request) {
          /**
           * Returns the character encoding.
           *
           * @return the result
           */
          @Override
          public String getCharacterEncoding() {
            return determineEncoding(this);
          }

          /**
           * Returns the content type.
           *
           * @return the result
           */
          @Override
          public String getContentType() {
            String type = super.getContentType();
            if (type != null && type.contains(";")) {
              return StringUtils.substringBefore(type, ";");
            }
            return type;
          }
        };
    return super.resolveMultipart(cleaned);
  }

  /**
   * cleanupMultipart operation.
   *
   * @param request the request
   */
  @Override
  public void cleanupMultipart(MultipartHttpServletRequest request) {
    super.cleanupMultipart(request);
  }
}
