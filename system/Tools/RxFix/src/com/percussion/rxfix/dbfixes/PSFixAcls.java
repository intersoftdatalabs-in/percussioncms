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
package com.percussion.rxfix.dbfixes;

import com.percussion.rxfix.IPSFix;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.security.IPSAclService;
import com.percussion.services.security.PSAclServiceLocator;
import com.percussion.util.PSPreparedStatement;
import com.percussion.util.PSStringTemplate;
import com.percussion.utils.jdbc.PSConnectionHelper;

import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * <p>
 * Fix duplicate acls and invalid objectids
 */
public class PSFixAcls extends PSFixDBBase implements IPSFix {

    private static final PSStringTemplate ms_deleteAcl =
            new PSStringTemplate("DELETE FROM {schema}.PSX_ACLS "
                    + "WHERE ID=?");
    private static final PSStringTemplate ms_updateObjectId =
            new PSStringTemplate("UPDATE {schema}.PSX_ACLS set OBJECTID=?"
                    + "WHERE ID=?");
    PSStringTemplate ms_allAcls = new PSStringTemplate(
            "SELECT ID,OBJECTTYPE,OBJECTID,NAME,DESCRIPTION,VERSION from {schema}.PSX_ACLS");

    PSStringTemplate ms_deletePerms = new PSStringTemplate(
            "DELETE from {schema}.PSX_ACLENTRYPERMISSIONS where ENTRYID in ( select ID from {schema}.PSX_ACLENTRIES where ACLID = ? )");

    PSStringTemplate ms_deleteEntries = new PSStringTemplate(
            "DELETE from {schema}.PSX_ACLENTRIES where ACLID = ?");

    /**
     * Ctor
     *
     * @throws SQLException
     * @throws NamingException
     */
    public PSFixAcls() throws NamingException, SQLException {
        super();
    }

    @Override
    public void fix(boolean preview)
            throws Exception {
        super.fix(preview);
        IPSAclService aclService = PSAclServiceLocator.getAclService();

        try (Connection c = PSConnectionHelper.getDbConnection();
             PreparedStatement st =
                     PSPreparedStatement.getPreparedStatement(
                             c,
                             ms_allAcls.expand(m_defDict))) {

            HashMap<PSGuid, Long> objectIdList = new HashMap<>();
            HashMap<PSGuid, List<Long>> duplicates = new HashMap<>();
            Map<Long, Long> badObjectIds = new HashMap<>();

            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong(1);
                    int objectType = rs.getInt(2);
                    long objectId = rs.getLong(3);

                    PSGuid guid = new PSGuid(PSTypeEnum.valueOf(objectType), objectId & 0xFFFFFFFFL);

                    if (objectId >> 32 != 0) {
                        badObjectIds.put(id, objectId);
                    }

                    Long aclId = objectIdList.get(guid);
                    if (aclId == null)
                        objectIdList.put(guid, id);
                    else {
                        List<Long> dups = duplicates.get(guid);
                        if (dups == null) {
                            dups = new ArrayList<>();
                            duplicates.put(guid, dups);
                        }
                        dups.add(id);
                    }

                }
            }

            if (duplicates.size() == 0) {
                logInfo(null, "There are no duplcate acls");
            } else {

                for (Map.Entry<PSGuid, List<Long>> duplicate : duplicates.entrySet()) {
                    PSGuid dupeObjectId = duplicate.getKey();

                    Long primaryId = objectIdList.get(dupeObjectId);

                    StringBuilder sb = new StringBuilder();
                    sb.append(renderAclId(primaryId));

                    for (Long duplicateId : duplicate.getValue()) {
                        sb.append(',');
                        sb.append(renderAclId(duplicateId));
                        if (!preview) {

                            try (PreparedStatement st_deletePerms =
                                         PSPreparedStatement.getPreparedStatement(
                                                 c,
                                                 ms_deletePerms.expand(m_defDict))
                            ;PreparedStatement st_deleteEntries =
                                         PSPreparedStatement.getPreparedStatement(
                                                 c,
                                                 ms_deleteEntries.expand(m_defDict));
                                 PreparedStatement st_deleteAcl =
                                         PSPreparedStatement.getPreparedStatement(
                                                 c,
                                                 ms_deleteAcl.expand(m_defDict))

                            ) {

                                st_deletePerms.setLong(1, duplicateId);
                                st_deleteEntries.setLong(1, duplicateId);
                                st_deleteAcl.setLong(1, duplicateId);

                                st_deletePerms.execute();
                                st_deleteEntries.execute();
                                st_deleteAcl.execute();
                            }
                            logSuccess(duplicateId.toString(), "duplicate aclId has been removed");
                        }
                    }

                    if (preview) {
                        logPreview(dupeObjectId.toString(), "objectid has duplicate aclIds : " + sb.toString());
                    }


                }
            }
            if (duplicates.size() == 0) {
                logInfo(null, "There are not acls with bad objectIds");
            }
            else
            {
                for (Map.Entry<Long, Long> badIdEntry : badObjectIds.entrySet()) {
                    Long badAclId = badIdEntry.getKey();
                    Long objectId = badIdEntry.getValue();
                    long corrected = objectId & 0xFFFFFFFFL;
                    if (preview)
                        logPreview(renderAclId(badAclId), "Acl has bad objectId is " + objectId + " should be " + corrected);
                    else {
                        try (PreparedStatement st_update =
                                     PSPreparedStatement.getPreparedStatement(
                                             c,
                                             ms_updateObjectId.expand(m_defDict))) {
                            st_update.setLong(1, corrected);
                            st_update.setLong(2, badAclId);

                            int updated = st_update.executeUpdate();

                            logSuccess(renderAclId(badAclId), "Acl has bad objectId is " + objectId + " has been fixed to " + corrected);

                        }

                    }
                }
            }
        }
        aclService.clearCache();
    }

    private String renderAclId(Long primaryId) {
        PSGuid guid = new PSGuid(primaryId);
        return primaryId.toString() + "(" + guid.toString() + ")";
    }


    @Override
    public String getOperation() {
        return "Fix ACLs";
    }
}
