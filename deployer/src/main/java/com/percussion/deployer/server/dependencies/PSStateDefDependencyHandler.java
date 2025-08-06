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
package com.percussion.deployer.server.dependencies;


import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSIdMap;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSDbmsHelper;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.tablefactory.PSJdbcFilterContainer;
import com.percussion.tablefactory.PSJdbcRowData;
import com.percussion.tablefactory.PSJdbcSelectFilter;
import com.percussion.tablefactory.PSJdbcTableData;
import com.percussion.utils.collections.PSIteratorUtils;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class to handle discovery of state dependencies.  
 * The <code>PSWorkflowDefDependencyHandler</code> class handles the packaging
 * and installation of a state.
 */
public class PSStateDefDependencyHandler extends PSDataObjectDependencyHandler
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
    */
   public PSStateDefDependencyHandler(PSDependencyDef def,
      PSDependencyMap dependencyMap)
   {
      super(def, dependencyMap);
   }

   // see base class
   @Override
   public Iterator<PSDependency> getChildDependencies(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException
   {
      if (tok == null || dep == null || !dep.getObjectType().equals(DEPENDENCY_TYPE)) {
         throw new IllegalArgumentException("Invalid arguments provided.");
      }

      var childDeps = new HashSet<PSDependency>();
      var childIDs = getChildIdsForStateDep(dep);
      childIDs.forEachRemaining(childId -> {
         try {
            var handler = getDependencyHandler(PSTransitionDefDependencyHandler.DEPENDENCY_TYPE);
            var childDep = handler.getDependency(tok, childId, PSWorkflowDefDependencyHandler.DEPENDENCY_TYPE, dep.getParentId());
            if (childDep != null) {
               childDep.setDependencyType(PSDependency.TYPE_LOCAL);
               childDeps.add(childDep);
            }
         } catch (PSDeployException e) {
            throw new RuntimeException(e);
         }
      });

      return childDeps.iterator();
    }

    /**
     * Get a list of ids of child transition dependencies for a given state 
     * deployable object.
     *
     * @param dep The state deployable object, assume not <code>null</code>.
     *
     * @return An iterator over zero or more ids of child dependencies as
     * <code>String</code>. It will never be <code>null</code>, but may be
     * empty.
     *
     * @throws PSDeployException if any error occurs.
     */
    private Iterator<String> getChildIdsForStateDep(PSDependency dep)
      throws PSDeployException
    {
      var dbmsHelper = PSDbmsHelper.getInstance();
      var fltTransFromStateId = new PSJdbcSelectFilter(TRANSITION_FROM_STATEID, PSJdbcSelectFilter.EQUALS, dep.getDependencyId(), Types.INTEGER);
      var fltWorkflowId = new PSJdbcSelectFilter(WORKFLOW_ID, PSJdbcSelectFilter.EQUALS, dep.getParentId(), Types.INTEGER);

      var fltWhere = new PSJdbcFilterContainer();
      fltWhere.doAND(fltTransFromStateId);
      fltWhere.doAND(fltWorkflowId);

      var data = dbmsHelper.catalogTableData(TRANSITIONS_TABLE, new String[]{TRANSITION_ID}, fltWhere);

      return data != null && data.getRows().hasNext()
         ? data.getRows().stream()
            .map(row -> getColumnValueNullable(STATES_TABLE, TRANSITION_ID, row))
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .iterator()
         : PSIteratorUtils.emptyIterator();
    }

   // see base class
   public Iterator getDependencies(PSSecurityToken tok) throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");
      
      List deps = new ArrayList();
      Iterator ids = getChildPairIdsFromTable(STATES_TABLE, STATE_ID, 
         WORKFLOW_ID, null);
      while (ids.hasNext())
      {
         String id = (String)ids.next();
         PSPairDependencyId pairId = new PSPairDependencyId(id);
         PSDependency dep = getDependency(tok, pairId.getChildId(), 
            PSWorkflowDefDependencyHandler.DEPENDENCY_TYPE, 
            pairId.getParentId());
         if (dep != null)
            deps.add(dep);
      }
      
      return deps.iterator();
   }
   
   // see base class
   public Iterator getDependencies(PSSecurityToken tok, String parentType, 
      String parentId) throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");

      if (parentType == null || parentType.trim().length() == 0)
         throw new IllegalArgumentException(
            "parentType may not be null or empty");
            
      if (parentId == null || parentId.trim().length() == 0)
         throw new IllegalArgumentException(
            "parentId may not be null or empty");

      List deps = new ArrayList();
      Iterator ids = getChildPairIdsFromTable(STATES_TABLE, STATE_ID, 
         WORKFLOW_ID, parentId);
      while (ids.hasNext())
      {
         String id = (String)ids.next();
         PSPairDependencyId pairId = new PSPairDependencyId(id);
         PSDependency dep = getDependency(tok, pairId.getChildId(), 
            PSWorkflowDefDependencyHandler.DEPENDENCY_TYPE, 
            pairId.getParentId());
         if (dep != null)
            deps.add(dep);
      }
      
      return deps.iterator();
      
   }
   
   // see base class
   public PSDependency getDependency(PSSecurityToken tok, String id, 
      String parentType, String parentId)
         throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");

      if (id == null || id.trim().length() == 0)
         throw new IllegalArgumentException("id may not be null or empty");
      
      if (parentType == null || parentType.trim().length() == 0)
         throw new IllegalArgumentException(
            "parentType may not be null or empty");
            
      if (parentId == null || parentId.trim().length() == 0)
         throw new IllegalArgumentException(
            "parentId may not be null or empty");

      if (!parentType.equals(PSWorkflowDefDependencyHandler.DEPENDENCY_TYPE))
         throw new IllegalArgumentException("parentType wrong type");
            
      PSDependency stateDep = null;
      
      // create the filter from the id of the state dependency
      PSJdbcSelectFilter fltStateId = new PSJdbcSelectFilter(STATE_ID,
         PSJdbcSelectFilter.EQUALS, id, Types.INTEGER);
      PSJdbcSelectFilter fltWorklowId = new PSJdbcSelectFilter(WORKFLOW_ID,
         PSJdbcSelectFilter.EQUALS, parentId, 
         Types.INTEGER);

      PSJdbcFilterContainer fltFinal = new PSJdbcFilterContainer();
      fltFinal.doAND(fltStateId);
      fltFinal.doAND(fltWorklowId);
      
      // get the result set from the database
      String[] columns = {STATE_NAME};  
      PSDbmsHelper dbmsHelper = PSDbmsHelper.getInstance(); 
      
      PSJdbcTableData data = dbmsHelper.catalogTableData(
         STATES_TABLE, columns, fltFinal);

      // should only get back one, take the first if found
      if (data != null && data.getRows().hasNext())
      {
         Iterator rows = data.getRows();
         PSJdbcRowData row = (PSJdbcRowData) rows.next();
         String stateName = dbmsHelper.getColumnString(STATES_TABLE, STATE_NAME,
            row);
         stateDep = createDependency(m_def, id, stateName);
         stateDep.setParent(parentId, parentType);
      }

      return stateDep;
   }
    

   // see base class
   public boolean doesDependencyExist(PSSecurityToken tok, String id, 
      String parentId) throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");

      if (id == null || id.trim().length() == 0)
         throw new IllegalArgumentException("id may not be null or empty");
      
      if (parentId == null || parentId.trim().length() == 0)
         throw new IllegalArgumentException(
            "parentId may not be null or empty");
      
      return getDependency(tok, id, 
         PSWorkflowDefDependencyHandler.DEPENDENCY_TYPE, parentId) != null;
   }

   
   /**
    * Provides the list of child dependency types this class can discover.
    * The child types supported by this handler are:
    * <ol>
    * <li>TransitionDef</li>
    * </ol>
    *
    * @return An iterator over one or more types as <code>String</code>
    * objects, never <code>null</code>, does not contain <code>null</code> or
    * empty entries.
    */
   @Override
   public Iterator<String> getChildTypes()
   {
      return ms_childTypes.iterator();
   }

   // see base class
   public String getType()
   {
      return DEPENDENCY_TYPE;
   }

   // see base class
   public String getParentType()
   {
      return PSWorkflowDefDependencyHandler.DEPENDENCY_TYPE;
   }
   
   // see base class
   public void reserveNewId(PSDependency dep, PSIdMap idMap)
      throws PSDeployException
   {
      if (dep == null)
         throw new IllegalArgumentException("dep may not be null");

      if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
         throw new IllegalArgumentException("dep wrong type");

      if (idMap == null)
         throw new IllegalArgumentException("idMap may not be null");

      reserveNewId(dep, idMap, STATES_TABLE, DEPENDENCY_TYPE);
   }

   /**
    * Override the method from super class, but this is to get the next id 
    * specifically for <code>STATE_ID</code> in <code>STATES_TABLE</code>.
    */
   protected String getNextId(String table, PSDependency dep, 
      String tgtParentId) throws PSDeployException
   {
      if (table == null || table.trim().length() == 0)
         throw new IllegalArgumentException("table may not be null or empty");
         
      if (dep == null)
         throw new IllegalArgumentException("dep may not be null");
      
      if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
         throw new IllegalArgumentException("dep wrong type");

      if (tgtParentId == null || tgtParentId.trim().length() == 0)
         throw new IllegalArgumentException(
            "tgtParentId may not be null or empty");
      
      String newId = null;
      
      // Check to see if there are already any states in this workflow.  If not,
      // reuse the source state ids so intial state will keep its id
      Iterator ids = getChildPairIdsFromTable(STATES_TABLE, STATE_ID, 
         WORKFLOW_ID, tgtParentId);
      if (!ids.hasNext())
         newId = dep.getDependencyId();
      else
      {
         // already have states, so get the next available id
         int id = PSDbmsHelper.getInstance().getNextIdInMemory(STATES_TABLE, 
            STATE_ID, WORKFLOW_ID, tgtParentId);
            
         newId = String.valueOf(id);
      }
      
      return newId;
   }
   
   // see base class
   public Iterator getDependencyFiles(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");

      if (dep == null)
         throw new IllegalArgumentException("dep may not be null");

      if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
         throw new IllegalArgumentException("dep wrong type");

      // nothing to deploy, assume it has been handled for workflow handler
      return PSIteratorUtils.emptyIterator();
   }

   // see base class
   public void installDependencyFiles(PSSecurityToken tok,
      PSArchiveHandler archive, PSDependency dep, PSImportCtx ctx)
         throws PSDeployException
   {
      // nothing to install, assume it has been handled for workflow handler
   }
   
   /**
    * Constant for this handler's supported type
    */
   final static String DEPENDENCY_TYPE = "StateDef";

   // private table and column names
   private static final String STATES_TABLE = "STATES";
   private static final String STATE_ID = "STATEID";
   private static final String STATE_NAME = "STATENAME";

   private static final String TRANSITIONS_TABLE = "TRANSITIONS";
   private static final String TRANSITION_FROM_STATEID =
      "TRANSITIONFROMSTATEID";
   private static final String TRANSITION_ID = "TRANSITIONID";
   private static final String WORKFLOW_ID = "WORKFLOWAPPID";

   /**
    * List of child types supported by this handler, it will never be
    * <code>null</code> or empty.
    */
   private static final List<String> ms_childTypes = List.of(PSTransitionDefDependencyHandler.DEPENDENCY_TYPE);

}
