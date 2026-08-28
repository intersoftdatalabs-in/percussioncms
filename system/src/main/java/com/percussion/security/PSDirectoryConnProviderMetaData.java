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

package com.percussion.security;

import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.percussion.data.PSResultSet;
import com.percussion.design.objectstore.PSAuthentication;
import com.percussion.design.objectstore.PSDirectory;
import com.percussion.design.objectstore.PSDirectorySet;
import com.percussion.design.objectstore.PSReference;
import com.percussion.design.objectstore.PSServerConfiguration;
import com.percussion.error.PSSqlException;
import com.percussion.server.PSServer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * The PSDirectoryConnProviderMetaData class implements cataloging for the PSDirectoryConnProvider
 * security provider.
 *
 * @author chadloder
 */
public class PSDirectoryConnProviderMetaData extends PSJndiProviderMetaData {
  /**
   * Construct a meta data object for the specified provider instance.
   *
   * @param inst The provider instance. Can be <CODE>null</CODE>, in which case not all of the
   *     information will be available.
   */
  PSDirectoryConnProviderMetaData(PSDirectoryConnProvider inst) {
    super(inst);
  }

  /** Default constructor to find connection properties, etc. */
  public PSDirectoryConnProviderMetaData() {
    this(null);
  }

  /**
   * Get the name of this security provider.
   *
   * @return the provider's name
   */
  public String getName() {
    return PSDirectoryConnProvider.SP_NAME;
  }

  /**
   * Get the full name of this security provider.
   *
   * @return the provider's full name
   */
  public String getFullName() {
    return "Directory Connection Security Provider";
  }

  /**
   * Get the descritpion of this security provider.
   *
   * @return the provider's description
   */
  public String getDescription() {
    return "Directory server authentication proxy.";
  }

  /**
   * See {@link IPSSecurityProviderMetaData#getObjects(String[], String[])} for description. The
   * following information is specific to this class:
   *
   * <ul>
   *   <li>This class does not support the <code>"_"</code> wildcard in filter patterns, and will
   *       throw a SQLException if one is supplied.
   *   <li>If returning user names, they are not distiguished, and only contain the value of the
   *       user entries principle attribute.
   *   <li>If returning group names, they fully qualified distiguished name.
   * </ul>
   */
  public ResultSet getObjects(String[] objectTypes, String[] filterPattern) throws SQLException {
    List<Object> obType = new ArrayList<>();
    List<Object> obId = new ArrayList<>();
    List<Object> obName = new ArrayList<>();

    if (m_instance instanceof PSDirectoryConnProvider) {
      PSDirectoryConnProvider provider = (PSDirectoryConnProvider) m_instance;

      PSServerConfiguration config = PSServer.getServerConfiguration();

      PSDirectorySet directorySet =
          config.getDirectorySet(provider.getDirectoryProvider().getReference().getName());

      // get the object attribute and ask for it in the results
      String objectAttributeName =
          directorySet.getRequiredAttributeName(PSDirectorySet.OBJECT_ATTRIBUTE_KEY);
      String[] attrIDs = {objectAttributeName};

      Iterator<PSReference> references = PSCatalogerTypes.directoryRefs(directorySet);
      while (references.hasNext()) {
        PSReference reference = references.next();
        PSDirectory directory = config.getDirectory(reference.getName());
        PSAuthentication authentication =
            config.getAuthentication(directory.getAuthenticationRef().getName());

        provider.setProviderProperties(directory, authentication);

        DirContext ctx = null;
        NamingEnumeration<SearchResult> results = null;
        NamingEnumeration<? extends Attribute> attrs = null;
        NamingEnumeration<?> attVals = null;
        try {
          if (objectTypes == null) {
            List<String> types = getSupportedTypes();
            objectTypes = types.toArray(new String[0]);
          }

          for (int i = 0; i < objectTypes.length; i++) {
            if (objectTypes[i].equalsIgnoreCase(OBJECT_TYPE_USER)) {
              // catalog all users under this provider
              ctx = m_instance.getCatalogContext();

              SearchControls controls = new SearchControls();
              controls.setReturningAttributes(attrIDs);
              if (directory.isShallowCatalogOption())
                controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
              else controls.setSearchScope(SearchControls.SUBTREE_SCOPE);

              // search using standard person object class
              String searchFilter =
                  "("
                      + PSJndiProvider.OBJECT_CLASS_ATTR
                      + "="
                      + PSJndiProvider.OBJECT_CLASS_PERSON_VAL
                      + ")";
              if (filterPattern != null)
                searchFilter =
                    PSJndiUtils.getFilterString(filterPattern, objectAttributeName, searchFilter);

              // search with empty name to search current context
              results = ctx.search("", searchFilter, controls);

              // use a set to return unique results.
              Set<String> valSet = new HashSet<>();
              while (results.hasMore()) {
                SearchResult result = results.next();
                for (attrs = result.getAttributes().getAll(); attrs.hasMore(); ) {
                  Attribute attr = attrs.next();

                  attVals = attr.getAll();
                  while (attVals.hasMoreElements()) {
                    String name = attVals.nextElement().toString();
                    valSet.add(PSJndiUtils.unEscapeDnComponent(name));
                  }
                  attVals.close();
                  attVals = null;
                }
                attrs.close();
                attrs = null;
              }

              results.close();
              results = null;

              ctx.close();
              ctx = null;

              for (String name : valSet) {
                obType.add(OBJECT_TYPE_USER);
                obId.add(name);
                obName.add(name); // name and id are the same
              }
            } else if (objectTypes[i].equalsIgnoreCase(OBJECT_TYPE_GROUP)) {
              // catalog all groups through this provider
              Set<String> valSet = new HashSet<>();

              Iterator<IPSGroupProvider> groupProviders = m_instance.getGroupProviders();
              while (groupProviders.hasNext()) {
                IPSGroupProvider gp = groupProviders.next();
                if (gp instanceof PSJndiGroupProvider)
                  ((PSJndiGroupProvider) gp).setProviderUrl(directory.getProviderUrl());

                if (filterPattern == null) valSet.addAll(gp.getGroups(null));
                else {
                  for (int j = 0; j < filterPattern.length; j++) {
                    // intentional: original code used objectTypes index i for filter selection
                    valSet.addAll(gp.getGroups(filterPattern[i]));
                  }
                }
              }

              for (String name : valSet) {
                obType.add(OBJECT_TYPE_GROUP);
                obId.add(name);
                obName.add(name); // name and id are the same
              }
            }
          }
        } catch (NamingException e) {
          // convert to SQLException and re-throw
          throw new PSSqlException(SecurityErrorCodes.DIR_GET_OBJECTS_FAILED, e.toString(), "0");
        } catch (PSSecurityException e) {
          // convert to SQLException and re-throw
          throw new PSSqlException(
              SecurityErrorCodes.DIR_GET_OBJECTS_FAILED, e.getLocalizedMessage(), "0");
        } finally {
          if (attVals != null)
            try {
              attVals.close();
            } catch (NamingException ex) {
            }

          if (attrs != null)
            try {
              attrs.close();
            } catch (NamingException ex) {
            }

          if (results != null)
            try {
              results.close();
            } catch (NamingException ex) {
            }

          if (ctx != null)
            try {
              ctx.close();
            } catch (NamingException ex) {
            }
        }
      }
    }

    HashMap<String, Integer> columnNames = new HashMap<>();
    columnNames.put("OBJECT_TYPE", Integer.valueOf(1));
    columnNames.put("OBJECT_ID", Integer.valueOf(2));
    columnNames.put("OBJECT_NAME", Integer.valueOf(3));

    List<?>[] resultCols = new List<?>[] {obType, obId, obName};

    return new PSResultSet(resultCols, columnNames, ms_GetObjectsRSMeta);
  }
}
