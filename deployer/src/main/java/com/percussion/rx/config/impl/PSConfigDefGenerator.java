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
package com.percussion.rx.config.impl;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDeployableElement;
import com.percussion.deployer.objectstore.PSExportDescriptor;
import com.percussion.deployer.server.PSDependencyManager;
import com.percussion.deployer.server.PSDeploymentHandler;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.utils.tools.PSParseFragments;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configuration definition generator. This tool can create a config definition shell based on a
 * passed in descriptor and its element selections.
 *
 * @author erikserating
 */
public class PSConfigDefGenerator {

  private static final Logger log = LogManager.getLogger(PSConfigDefGenerator.class);

  /** Private ctor to inhibit instantiation. */
  private PSConfigDefGenerator() {
    init();
  }

  /**
   * Retrieve the singleton instance of the config def generator.
   *
   * @return the config def generator, never <code>null</code>.
   */
  public static PSConfigDefGenerator getInstance() {
    if (ms_instance == null) ms_instance = new PSConfigDefGenerator();
    return ms_instance;
  }

  /**
   * Generate a config def shell based on the passed in descriptor.
   *
   * @param desc export descriptor, cannot be <code>null</code>.
   * @return the contents for a config def
   */
  public String generate(PSExportDescriptor desc) {
    if (desc == null) throw new IllegalArgumentException("descriptor cannot be null.");
    var dh = PSDeploymentHandler.getInstance();
    var dm = (PSDependencyManager) dh.getDependencyManager();
    var packageName = desc.getName();
    Iterator<PSDeployableElement> it = desc.getPackages();
    var sb = new StringBuilder();
    sb.append(ms_fragments.get("XMLHEAD"));
    while (it.hasNext()) {
      PSDeployableElement pe = it.next();
      if (!(pe instanceof PSDependency)) {
        continue;
      }
      PSDependency el = (PSDependency) pe;
      if (el.getObjectType().equals("Custom")) {
        Iterator<PSDependency> children = el.getDependencies();
        if (children.hasNext()) {
          el = children.next();
        }
      }
      var oName = el.getDisplayName();
      var typeEnum = dm.getGuidType(el.getObjectType());
      if (typeEnum == null) continue;
      var oType = typeEnum.toString();
      var frag = getFragment(oName, oType, packageName);
      if (frag != null) sb.append(frag);
    }
    sb.append("</beans>");
    return sb.toString();
  }

  /**
   * Gets the appropriate fragment and does token replacement on it.
   *
   * @param objectname assumed not <code>null</code>.
   * @param objecttype assumed not <code>null</code>.
   * @param packagename assumed not <code>null</code>.
   * @return the token replaced fragment or <code>null</code> if not found.
   */
  private String getFragment(String objectname, String objecttype, String packagename) {
    String fragment = ms_fragments.get(objecttype.toUpperCase());
    if (fragment == null) return null;
    fragment = StringUtils.replace(fragment, OBJECT_NAME_TOKEN, objectname);
    fragment =
        StringUtils.replace(fragment, SOLUTION_OBJECT_NAME_TOKEN, packagename + "." + objectname);
    return fragment;
  }

  /** Initialize by loading fragments. */
  private void init() {
    try {
      parseFragmentFile();
    } catch (IOException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  /**
   * Parse out the file fragments.
   *
   * @throws IOException
   */
  private void parseFragmentFile() throws IOException {
    var raw = getFragementFileContents();
    ms_fragments = PSParseFragments.parseContent(raw);
  }

  /**
   * Retrieve the fragment file.
   *
   * @return the fragment file contents, never <code>null</code>.
   * @throws IOException if the fragment file cannot be read.
   */
  public String getFragementFileContents() throws IOException {
    try (var in = getClass().getResourceAsStream(FRAGMENT_FILE)) {
      var out = new StringBuilder();
      var b = new byte[4096];
      int n;
      while ((n = in.read(b)) != -1) {
        out.append(new String(b, 0, n));
      }
      return out.toString();
    }
  }

  /** Singleton instance of the generator. */
  private static PSConfigDefGenerator ms_instance;

  /** Cache of config def fragments */
  private Map<String, String> ms_fragments;

  /** Name of the fragment file. */
  private static final String FRAGMENT_FILE = "PSConfigDefGeneratorFragments.txt";

  private static final String OBJECT_NAME_TOKEN = "${objName}";

  private static final String SOLUTION_NAME_TOKEN = "publisherPrefix.solutionName";

  private static final String SOLUTION_OBJECT_NAME_TOKEN = SOLUTION_NAME_TOKEN + ".objName";
}
