/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.apibridge;

import com.percussion.rest.about.AboutDetail;
import com.percussion.rest.about.IAboutAdaptor;
import com.percussion.server.PSServer;
import com.percussion.system.utils.PSSiteManageBean;
import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 * Read-only server version and license/copyright disclaimer, shared with the startup console log
 * (see issue #1529 - {@code com.percussion.server.PSStringResources} {@code copyright} and {@code
 * thirdPartyCopyright} keys, printed by {@code PSServer.init}).
 */
@PSSiteManageBean
public class AboutAdaptor implements IAboutAdaptor {

  static final String PRODUCT_NAME = "Percussion CMS";
  static final String BUNDLE_NAME = "com.percussion.server.PSStringResources";

  private final Supplier<String> versionStringSupplier;
  private final Supplier<ResourceBundle> serverBundleSupplier;

  /**
   * Default constructor used by the Spring context. The {@link PSServer#getVersionString()}
   * supplier returns the empty string until {@link PSServer#init()} completes (see {@code
   * PSServer.initVersion} in the server bootstrap). The CXF REST endpoint that backs {@link
   * com.percussion.rest.about.AboutResource} is reachable only after that initialization finishes,
   * so {@code versionString} will be populated for every real request; pre-init invocations are an
   * initialization-order violation that must be addressed at the call site (a guarded
   * {@link Supplier#supplier} that throws or returns a "not-yet-initialized" sentinel is more
   * useful than hiding the bug here).
   */
  public AboutAdaptor() {
    this(PSServer::getVersionString, () -> ResourceBundle.getBundle(BUNDLE_NAME));
  }

  /**
   * Package-visible for unit tests that inject fakes instead of the static {@link PSServer} version
   * accessor and {@link ResourceBundle#getBundle}.
   */
  AboutAdaptor(
      Supplier<String> versionStringSupplier, Supplier<ResourceBundle> serverBundleSupplier) {
    this.versionStringSupplier = versionStringSupplier;
    this.serverBundleSupplier = serverBundleSupplier;
  }

  @Override
  public AboutDetail getAbout() {
    ResourceBundle bundle = serverBundleSupplier.get();
    AboutDetail detail = new AboutDetail();
    detail.setProductName(PRODUCT_NAME);
    detail.setVersionString(versionStringSupplier.get());
    detail.setCopyright(bundle.getString("copyright"));
    detail.setThirdPartyCopyright(bundle.getString("thirdPartyCopyright"));
    return detail;
  }
}
