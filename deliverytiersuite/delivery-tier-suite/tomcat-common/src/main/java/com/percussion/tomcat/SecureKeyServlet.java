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

package com.percussion.tomcat;

import com.percussion.security.PSEncryptor;
import com.percussion.utils.io.PathUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Servlet that logs a hard error during initialization when the Percussion secure-key file is
 * missing from the server. The check delegates to {@link
 * com.percussion.security.PSEncryptor#checkSecureKeyPresent(String)}; if it returns {@code false}
 * the servlet writes the failure to both the logger and standard out so that operators notice a
 * missing key on startup even when standard logging is misconfigured.
 */
public class SecureKeyServlet extends HttpServlet {

  /** Default no-argument constructor for the secure key servlet. */
  public SecureKeyServlet() {
    // Default constructor for the secure key servlet.
  }

  private static final long serialVersionUID = 1L;
  private static final Logger logger = LogManager.getLogger(SecureKeyServlet.class);

  @Override
  public void init() throws ServletException {
    var secureKeyPresent =
        PSEncryptor.checkSecureKeyPresent(
            PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR));
    if (!secureKeyPresent) {
      logger.error("*******SECURE KEY FILE IS MISSING!!! NEED TO COPY FROM CMS FIRST******");
      System.out.println("*******SECURE KEY FILE IS MISSING!!! NEED TO COPY FROM CMS FIRST******");
    }
  }
}
