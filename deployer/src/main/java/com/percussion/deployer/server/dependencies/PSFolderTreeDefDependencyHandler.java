/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.deployer.server.dependencies;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.server.PSRelationshipProcessor;
import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.collections.PSIteratorUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to handle packaging and installing folder tree defintion dependencies.
 * See {@link PSFolderTreeDependencyHandler} class description for more
 * information on folder trees.
 */
public class PSFolderTreeDefDependencyHandler
   extends PSFolderObjectDependencyHandler
{
   /**
    * Construct a dependency handler.
    *
    * @param def The def for the type supported by this handler.  May not be
    * <code>null</code> and must be of the type supported by this class.  See
    * {@link #getType()} for more info.
    * @param dependencyMap The full dependency map.  May not be
    * <code>null</code>.
    *
    * @throws IllegalArgumentException if any param is invalid.
    * @throws PSDeployException if any other error occurs.
    */
   public PSFolderTreeDefDependencyHandler(PSDependencyDef def,
      PSDependencyMap dependencyMap) throws PSDeployException
   {
      super(def, dependencyMap);
   }

   // see base class
   public Iterator getChildDependencies(PSSecurityToken tok, PSDependency dep)
           throws PSDeployException, PSNotFoundException {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");

      if (dep == null)
         throw new IllegalArgumentException("dep may not be null");

      if (! dep.getObjectType().equals(DEPENDENCY_TYPE))
         throw new IllegalArgumentException("dep wrong type");

      return getChildDependencies(tok, dep.getDependencyId(),
         PSDependency.TYPE_LOCAL).iterator();
    }

   // see base class
   public Iterator getDependencies(PSSecurityToken tok) throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");

      // ancestors not supported
      return PSIteratorUtils.emptyIterator();
   }

   // see base class
   public PSDependency getDependency(PSSecurityToken tok, String id)
      throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");

      if (id == null || id.trim().length() == 0)
         throw new IllegalArgumentException("id may not be null or empty");

      PSDependency dep = null;

      PSRelationshipProcessor proc = getRelationshipProcessor(tok);
      PSComponentSummary sum = getFolderSummary(proc, id);
      if (sum != null)
         dep = createDependency(m_def, id, sum.getName());

      return dep;
   }

   /**
    * Provides the list of child dependency types this class can discover.
    * The child types supported by this handler are:
    * <ol>
    * <li>Community</li>
    * <li>DisplayFormatDef</li>
    * <li>FolderTreeDef</li>
    * <li>FolderTranslations</li>
    * </ol>
    *
    * @return An iterator over zero or more types as <code>String</code>
    * objects, never <code>null</code>, does not contain <code>null</code> or
    * empty entries.
    */
   public Iterator getChildTypes()
   {
      return ms_childTypes.iterator();
   }

   // see base class
   public String getType()
   {
      return DEPENDENCY_TYPE;
   }

   /**
    * Constant for this handler's supported type
    */
   final static String DEPENDENCY_TYPE = "FolderTreeDef";

   /**
    * List of child types supported by this handler, it will never be
    * <code>null</code> or empty.
    */
   private static List ms_childTypes = new ArrayList<>();

   static
   {
      ms_childTypes.add(PSCommunityDefDependencyHandler.DEPENDENCY_TYPE);
      ms_childTypes.add(PSDisplayFormatDefDependencyHandler.DEPENDENCY_TYPE);
      ms_childTypes.add(PSFolderTreeDefDependencyHandler.DEPENDENCY_TYPE);
      ms_childTypes.add(PSFolderTranslationsDependencyHandler.DEPENDENCY_TYPE);
   }
}
