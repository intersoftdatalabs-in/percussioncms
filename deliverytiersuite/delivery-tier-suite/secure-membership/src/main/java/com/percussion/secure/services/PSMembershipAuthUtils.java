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
// REFACTORED: CP-JAVA11
package com.percussion.secure.services;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.context.ContextLoader;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/** @deprecated This class is part of the deprecated secure-membership module. */
@Deprecated
public class PSMembershipAuthUtils {

  private static final Logger log = LogManager.getLogger(PSMembershipAuthUtils.class);

  public static List<String> getAccessGroupsFromXML(String accessGroupFileName) {
    var groups = new ArrayList<String>();
    String accessString;
    var context = ContextLoader.getCurrentWebApplicationContext();
    var ctx = context.getServletContext();
    var filePath = ctx.getRealPath(accessGroupFileName);
    try {
      var accessGroupFile = new File(filePath);
      var dbFactory =
          PSSecureXMLUtils.getSecuredDocumentBuilderFactory(
              new PSXmlSecurityOptions(true, true, true, false, true, false));
      var dBuilder = dbFactory.newDocumentBuilder();
      var doc = dBuilder.parse(accessGroupFile);
      var nodeList = doc.getElementsByTagName("intercept-url");
      for (int temp = 0; temp < nodeList.getLength(); temp++) {
        var node = nodeList.item(temp);
        var element = (Element) node;
        if (element.getAttribute("access") != null && !element.getAttribute("access").isEmpty()) {
          accessString = element.getAttribute("access");
          groups.addAll(Arrays.asList(accessString.split("\\s*,\\s*")));
        }
      }
    } catch (FileNotFoundException e) {
      log.error(
          "FileNotFoundException in PSLdapUserDetailsMapper..... {}",
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } catch (ParserConfigurationException e) {
      log.error(
          "ParserConfigurationException in PSLdapUserDetailsMapper..... {}",
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } catch (IOException e) {
      log.error(
          "IOException in PSLdapUserDetailsMapper..... {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } catch (SAXException e) {
      log.error(
          "SAXException in PSLdapUserDetailsMapper..... {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return groups;
  }
}
