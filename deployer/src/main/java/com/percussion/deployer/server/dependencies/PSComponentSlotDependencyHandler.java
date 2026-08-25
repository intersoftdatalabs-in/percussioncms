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
import com.percussion.deployer.objectstore.PSDependencyData;
import com.percussion.deployer.objectstore.PSDependencyFile;
import com.percussion.deployer.objectstore.PSDeployComponentUtils;
import com.percussion.deployer.objectstore.PSIdMapping;
import com.percussion.deployer.objectstore.PSTransactionSummary;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSDbmsHelper;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.tablefactory.PSJdbcColumnData;
import com.percussion.tablefactory.PSJdbcRowData;
import com.percussion.tablefactory.PSJdbcSelectFilter;
import com.percussion.tablefactory.PSJdbcTableData;
import com.percussion.tablefactory.PSJdbcTableSchema;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;

/** Class to handle packaging and deploying a component slot */
public class PSComponentSlotDependencyHandler extends PSPairIdDependencyHandler {

  /**
   * Construct a dependency handler.
   *
   * @param def The def for the type supported by this handler. May not be <code>null</code> and
   *     must be of the type supported by this class. See {@link #getType()} for more info.
   * @param dependencyMap The full dependency map. May not be <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   */
  public PSComponentSlotDependencyHandler(PSDependencyDef def, PSDependencyMap dependencyMap) {
    super(def, dependencyMap);
  }

  // see base class
  @Override
  public Iterator<PSDependency> getChildDependencies(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException, PSNotFoundException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");
    if (dep == null) throw new IllegalArgumentException("dep may not be null");
    if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
      throw new IllegalArgumentException("dep wrong type");

    // get SHARED component child dependencies
    PSJdbcSelectFilter filter = getFilterForPairId(dep.getDependencyId(), CR_PARENT_ID, CR_NAME);
    PSJdbcTableData data =
        PSDbmsHelper.getInstance().catalogTableData(CR_TABLE, new String[] {CR_CHILD_ID}, filter);

    Iterator<String> ids = getIdsFromTableData(data, CR_TABLE, CR_CHILD_ID);
    List<PSDependency> childDeps =
        getDepsFromIds(ids, PSComponentDefDependencyHandler.DEPENDENCY_TYPE, tok, -1);

    return childDeps.iterator();
  }

  // see base class
  @Override
  public Iterator<PSDependency> getDependencies(PSSecurityToken tok)
      throws PSDeployException, PSNotFoundException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    Iterator<String> ids = getChildPairIdsFromTable(CR_TABLE, CR_NAME, CR_PARENT_ID, null);

    return getDepsFromIds(ids, DEPENDENCY_TYPE, tok).iterator();
  }

  // see base class
  @Override
  public PSDependency getDependency(PSSecurityToken tok, String id) throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");
    if (id == null || id.trim().length() == 0)
      throw new IllegalArgumentException("id may not be null or empty");

    return getDependency(tok, id, CR_TABLE, CR_NAME, CR_PARENT_ID);
  }

  /**
   * Provides the list of child dependency types this class can discover. The child types supported
   * by this handler are:
   *
   * <ol>
   *   <li>ComponentDef
   * </ol>
   *
   * @return An iterator over zero or more types as <code>String</code> objects, never <code>null
   *     </code>, does not contain <code>null</code> or empty entries.
   */
  @Override
  public Iterator<String> getChildTypes() {
    return ms_childTypes.iterator();
  }

  // see base class
  protected String getPairParentType() {
    return PSComponentDefDependencyHandler.DEPENDENCY_TYPE;
  }

  // see base class
  @Override
  public String getType() {
    return DEPENDENCY_TYPE;
  }

  // see base class
  @Override
  public Iterator<PSDependencyFile> getDependencyFiles(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");
    if (dep == null) throw new IllegalArgumentException("dep may not be null");
    if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
      throw new IllegalArgumentException("dep wrong type");

    PSJdbcSelectFilter filter = getFilterForPairId(dep.getDependencyId(), CR_PARENT_ID, CR_NAME);
    PSDependencyData depData = getDepDataFromTable(CR_TABLE, filter, true);

    List<PSDependencyFile> files = new ArrayList<>();
    files.add(getDepFileFromDepData(depData));

    return files.iterator();
  }

  // see base class
  @Override
  public void installDependencyFiles(
      PSSecurityToken tok, PSArchiveHandler archive, PSDependency dep, PSImportCtx ctx)
      throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");
    if (archive == null) throw new IllegalArgumentException("archive may not be null");
    if (dep == null) throw new IllegalArgumentException("dep may not be null");
    if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
      throw new IllegalArgumentException("dep wrong type");
    if (ctx == null) throw new IllegalArgumentException("ctx may not be null");

    // retrieve the data from the archive
    Iterator<PSDependencyFile> files = getDependecyDataFiles(archive, dep);
    PSDependencyFile file = files.next();
    PSDependencyData crData = getDepDataFromFile(archive, file);

    // delete from TABLE where PARENT_ID=parentId and NAME=childId
    deleteDepFromTable(
        tok,
        dep,
        ctx,
        crData.getSchema(),
        CR_TABLE,
        CR_NAME,
        CR_PARENT_ID,
        PSComponentDefDependencyHandler.DEPENDENCY_TYPE);

    // install the data
    PSJdbcTableData newData = crData.getData();
    newData = transferIdsForCRData(newData, ctx, tok);
    installDependencyData(
        crData.getSchema(), newData, dep, ctx, PSTransactionSummary.ACTION_CREATED, null);
  }

  /**
   * Transfer ids from the given data for the <code>CR_TABLE</code>.
   *
   * @param srcData The data from the source server, assume not <code>null</code>
   * @param ctx The import context to aid in the installation, assume not <code>null</code>.
   * @param tok The security token, assume not <code>null</code>.
   * @return The transfered table data, it will never <code>null</code>, all rows will have their
   *     action set to {@link PSJdbcRowData#ACTION_INSERT}.
   * @throws PSDeployException if an error occurs.
   */
  private PSJdbcTableData transferIdsForCRData(
      PSJdbcTableData srcData, PSImportCtx ctx, PSSecurityToken tok) throws PSDeployException {
    List<PSJdbcRowData> rows = PSDeployComponentUtils.cloneList(srcData.getRows());

    if (rows.isEmpty()) // not expecting no rows
    throw new PSDeployException(DeploymentErrorCodes.NO_ROWS_TO_PROCESS);

    // get the source row
    List<PSJdbcRowData> tgtRowList = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      PSJdbcRowData srcRow = rows.get(i);

      // walk the columns and build a new row, xform the ids as we go
      // xform the ids for CR_CHILD_ID and CR_PARENT_ID
      List<PSJdbcColumnData> tgtColList = new ArrayList<>();
      Iterator<PSJdbcColumnData> srcCols = srcRow.getColumns();
      while (srcCols.hasNext()) {
        PSJdbcColumnData col = srcCols.next();
        String colName = col.getName();
        if (colName.equalsIgnoreCase(CR_CHILD_ID) || colName.equalsIgnoreCase(CR_PARENT_ID)) {
          PSIdMapping mapping =
              getIdMapping(ctx, col.getValue(), PSComponentDefDependencyHandler.DEPENDENCY_TYPE);
          if (mapping != null) col.setValue(mapping.getTargetId());
        }

        tgtColList.add(col);
      }

      PSJdbcRowData tgtRow = new PSJdbcRowData(tgtColList.iterator(), PSJdbcRowData.ACTION_INSERT);
      tgtRowList.add(tgtRow);
    }

    return new PSJdbcTableData(CR_TABLE, tgtRowList.iterator());
  }

  /** Constant for this handler's supported type */
  static final String DEPENDENCY_TYPE = "ComponentSlot";

  // Constants for component slot table and column names
  private static final String CR_TABLE = "RXSYSCOMPONENTRELATIONS";
  private static final String CR_NAME = "COMPONENTSLOTNAME";
  private static final String CR_CHILD_ID = "CHILDCOMPONENTID";
  private static final String CR_PARENT_ID = "COMPONENTID";

  /**
   * The schema for CR_TABLE, initialized by constructor, will never be <code>null</code> or
   * modified after that.
   */
  PSJdbcTableSchema m_crSchema;

  /** List of child types supported by this handler, it will never be <code>null</code> or empty. */
  private static final List<String> ms_childTypes = new ArrayList<>();

  static {
    ms_childTypes.add(PSComponentDefDependencyHandler.DEPENDENCY_TYPE);
  }
}
