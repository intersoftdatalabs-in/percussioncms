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

package com.percussion.dashboardmanagement;

import com.percussion.content.PSGenericContentGenerator;
import com.percussion.dashboardmanagement.data.DashboardContent;
import java.io.InputStream;

/**
 * Provides services to add and clean up gadgets on the dashboard. Requires a server URL and an XML
 * file defining the content to be generated. User and password to authenticate against the server
 * are required.
 *
 * @author miltonpividori
 */
public class PSDashboardGadgetGenerator extends PSGenericContentGenerator<DashboardContent> {

  /** The gadget generator responsible for generating and cleaning up gadgets. */
  private final PSGadgetGenerator gadgetGenerator;

  /**
   * Constructs a dashboard gadget generator.
   *
   * @param serverUrl the server URL
   * @param xmlData the XML data input stream
   * @param username the username
   * @param password the password
   */
  public PSDashboardGadgetGenerator(
      String serverUrl, InputStream xmlData, String username, String password) {
    super(serverUrl, xmlData, username, password);
    this.gadgetGenerator = new PSGadgetGenerator(this.serverUrl, this.username, this.password);
  }

  public static void main(String[] args) throws Exception {
    PSGenericContentGenerator.runMainMethod(args, PSDashboardGadgetGenerator.class);
  }

  @Override
  protected Class<DashboardContent> getRootDataType() {
    return DashboardContent.class;
  }

  @Override
  protected void generateAllContent() {
    gadgetGenerator.addGadgets(content.getGadgetDef());
  }

  @Override
  protected void cleanupAllContent() {
    gadgetGenerator.cleanup(content.getGadgetDef());
  }
}
