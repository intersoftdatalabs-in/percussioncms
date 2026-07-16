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

import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.web.context.ContextLoader;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Handles User Details Mapping for authorization.
 *
 * @author Shweta Patel
 * @deprecated This class is part of the deprecated secure-membership module.
 */
@Deprecated
public class PSLdapUserDetailsMapper extends LdapUserDetailsMapper {
  private static final String ROLE_TEST = "role_test";
  private static final String ROLE_ADMIN = "Domain Admins";
  private String accessGroupFileName;

  @Override
  public UserDetails mapUserFromContext(
      DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authority) {
    var groups = getAccessGroupsFromXML();
    var originalUser = super.mapUserFromContext(ctx, username, authority);
    var allAuthorities = new ArrayList<SimpleGrantedAuthority>();
    for (var auth : authority) {
      if (auth != null && !auth.getAuthority().isEmpty()) {
        if (groups != null
            && !groups.isEmpty()
            && groups.contains("'" + auth.getAuthority().toUpperCase() + "'")) {
          allAuthorities.add((SimpleGrantedAuthority) auth);
        }
      }
    }
    return new User(
        originalUser.getUsername(),
        Objects.requireNonNullElse(originalUser.getPassword(), ""),
        originalUser.isEnabled(),
        originalUser.isAccountNonExpired(),
        originalUser.isCredentialsNonExpired(),
        originalUser.isAccountNonLocked(),
        allAuthorities);
  }

  public List<String> getAccessGroupsFromXML() {
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
      var root = doc.getDocumentElement();
      var nodeList = root.getElementsByTagName("security:intercept-url");
      for (int temp = 0; temp < nodeList.getLength(); temp++) {
        var node = nodeList.item(temp);
        var element = (Element) node;
        if (element.getAttribute("access") != null && !element.getAttribute("access").isEmpty()) {
          accessString = element.getAttribute("access");
          groups.addAll(Arrays.asList(accessString.split("\\s*,\\s*")));
        }
      }
    } catch (FileNotFoundException e) {
      System.out.println("FileNotFoundException in PSLdapUserDetailsMapper: " + e);
    } catch (ParserConfigurationException e) {
      System.out.println("ParserConfigurationException in PSLdapUserDetailsMapper: " + e);
    } catch (IOException e) {
      System.out.println("IOException in PSLdapUserDetailsMapper: " + e);
    } catch (SAXException e) {
      System.out.println("SAXException in PSLdapUserDetailsMapper: " + e);
    }
    return groups;
  }

  public String getAccessGroupFileName() {
    return accessGroupFileName;
  }

  public void setAccessGroupFileName(String accessGroupFileName) {
    this.accessGroupFileName = accessGroupFileName;
  }
}
