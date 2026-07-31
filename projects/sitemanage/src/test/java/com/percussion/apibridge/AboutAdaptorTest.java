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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.rest.about.AboutDetail;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class AboutAdaptorTest {

  @Test
  void getAbout_returnsProductNameVersionAndDisclaimerText() {
    ResourceBundle bundle = mock(ResourceBundle.class);
    when(bundle.getString("copyright")).thenReturn("Percussion CMS Copyright 1999-2026");
    when(bundle.getString("thirdPartyCopyright")).thenReturn("This product includes...");

    AboutAdaptor adaptor = new AboutAdaptor(() -> "Version 8.2.0 Build 20260731 (1)", () -> bundle);

    AboutDetail detail = adaptor.getAbout();

    assertNotNull(detail);
    assertEquals(AboutAdaptor.PRODUCT_NAME, detail.getProductName());
    assertEquals("Version 8.2.0 Build 20260731 (1)", detail.getVersionString());
    assertEquals("Percussion CMS Copyright 1999-2026", detail.getCopyright());
    assertEquals("This product includes...", detail.getThirdPartyCopyright());
  }

  @Test
  void defaultConstructor_usesRealServerBundleName() {
    // The default constructor wires PSServer::getVersionString and the real
    // com.percussion.server.PSStringResources bundle; verify the bundle name constant
    // matches the resource actually loaded by PSServer.init at startup.
    assertEquals("com.percussion.server.PSStringResources", AboutAdaptor.BUNDLE_NAME);
  }
}
