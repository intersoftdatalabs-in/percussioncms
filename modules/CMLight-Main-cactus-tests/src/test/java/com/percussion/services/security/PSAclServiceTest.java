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
package com.percussion.services.security;


import com.percussion.error.PSExceptionUtils;
import com.percussion.security.IPSTypedPrincipal.PrincipalTypes;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.security.data.PSAccessLevelImpl;
import com.percussion.services.security.data.PSAclEntryImpl;
import com.percussion.services.security.data.PSAclImpl;
import com.percussion.utils.guid.IPSGuid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Test the {@link IPSAclService} CRUD operations.  See
 * {@link PSAclServiceAccessTest} for other service functionality.
 */
public class PSAclServiceTest
{
    private static final Logger log = LogManager.getLogger(PSAclServiceTest.class);
    //All test acls have this name so they can be easily deleted
    private static final String TEST_ACL_NAME = "aclUnitTest";

    /**
     * object guid for template
     */
    private static IPSGuid ms_templateGuid = new PSGuid(PSTypeEnum.TEMPLATE,
            10023);

    private static IPSGuid ms_templateGuid2 = new PSGuid(PSTypeEnum.TEMPLATE,
            10024);

    /**
     * object guid for slot
     */
    private static IPSGuid ms_slotTemplate = new PSGuid(PSTypeEnum.SLOT, 10023);


    /**
     * Util method for MSM tests, where an ACL is created but either :
     *    1. persisted for deserialization and saveAcl
     *    2. created for serialization and saveAcl
     *
     * @param persist if <code>true</code> saveAcl, else just create and return
     *
     * @return The list, never <code>null</code>.
     *
     * @throws Exception
     */
    private List<IPSAcl> createAcl(boolean persist) throws Exception
    {
        IPSAclService aclService = PSAclServiceLocator.getAclService();

        List<IPSAcl> aclList = new ArrayList<IPSAcl>();
        PSAclImpl acl;
        acl = (PSAclImpl) aclService.createAcl(ms_templateGuid,
                new PSTypedPrincipal("admin1", PrincipalTypes.USER));
        aclList.add(acl);

        IPSAclEntry aclEntry;
        aclEntry = acl.getEntries().iterator().next();
        PSAccessLevelImpl perm = new PSAccessLevelImpl((PSAclEntryImpl)aclEntry,PSPermissions.READ);

        if ( persist )
            aclService.saveAcls(aclList);
        return aclList;
    }

    /**
     * Util method for testing deserialization by MSM
     * @param acl the acl that needs update
     */
    private void addDefaultAclEntryToAcl(PSAclImpl acl)
    {
        PSAclEntryImpl aclEntry = new PSAclEntryImpl(new PSTypedPrincipal(
                "Default", PrincipalTypes.COMMUNITY));
        aclEntry.addPermission(PSPermissions.RUNTIME_VISIBLE);
        acl.addEntry(aclEntry);
    }

    /**
     * Util method for testing deserialization by MSM
     * @param acl
     */
    private void removeAdminAclEntryToAcl(PSAclImpl acl)
    {
        Collection<IPSAclEntry> entries = acl.getEntries();
        IPSAclEntry adminAclEntry = null;
        for (IPSAclEntry entry : entries)
        {
            if ( entry.getName().compareTo("admin1") == 0 )
            {
                adminAclEntry = entry;
                break;
            }
        }
        if ( adminAclEntry != null )
        {
            entries.remove(adminAclEntry);
        }
    }





    /**
     * Creates and persists 1000 ACLs (they all are identical except for the
     * ids.) They all have a name of aclUnitTest so they can be cleaned up later
     * by name.
     *
     * @return Never <code>null</code>.
     * @throws Exception
     */
    private List<IPSGuid> create1000Acls()
            throws Exception
    {
        IPSAclService svc = PSAclServiceLocator.getAclService();
        List<IPSGuid> aclGuids = new ArrayList<IPSGuid>();
        List<IPSAcl> aclList = new ArrayList<IPSAcl>();
        PSAclImpl acl;

        for (int i = 0; i < 1000; i++)
        {
            acl = (PSAclImpl) svc.createAcl(new PSGuid(PSTypeEnum.ACTION, 55000+i),
                    new PSTypedPrincipal("admin1", PrincipalTypes.USER));
            acl.setName(TEST_ACL_NAME);
            aclList.add(acl);
            IPSAclEntry aclEntry;
            aclEntry = acl.getEntries().iterator().next();
            aclEntry.addPermission(PSPermissions.READ);

            aclEntry = new PSAclEntryImpl(new PSTypedPrincipal("Editor",
                    PrincipalTypes.ROLE));
            aclEntry.addPermission(PSPermissions.UPDATE);
            aclEntry.addPermission(PSPermissions.READ);
            acl.addEntry((PSAclEntryImpl) aclEntry);

            aclEntry = new PSAclEntryImpl(new PSTypedPrincipal(
                    PSTypedPrincipal.ANY_COMMUNITY_ENTRY, PrincipalTypes.COMMUNITY));
            aclEntry.addPermission(PSPermissions.RUNTIME_VISIBLE);
            acl.addEntry((PSAclEntryImpl) aclEntry);
        }
        svc.saveAcls(aclList);
        for (IPSAcl aclresult : aclList)
        {
            aclGuids.add(aclresult.getGUID());
        }
        return aclGuids;
    }


    /**
     * Test saving changes to acls
     *
     * @param aclList The currently persisted acls, assumed not
     * <code>null</code>.
     *
     * @return The list of persisted acls after testing save, never
     * <code>null</code>.
     *
     * @throws Exception if the test fails
     */
    private List<IPSAcl> testSave(List<IPSAcl> aclList) throws Exception
    {
        // modify and add something
        List<IPSGuid> aclGuids = new ArrayList<IPSGuid>();
        for (IPSAcl acl : aclList)
        {
            PSAclImpl aclImpl = (PSAclImpl) acl;
            aclGuids.add(aclImpl.getGUID());
            aclImpl.setDescription("modified");
            for (IPSAclEntry entry : aclImpl.getEntries())
                entry.addPermission(PSPermissions.DELETE);

            PSAclEntryImpl newEntry = new PSAclEntryImpl();
            newEntry.setPrincipal(new PSTypedPrincipal("test1",
                    PrincipalTypes.ROLE));
            newEntry.addPermission(PSPermissions.DELETE);
            aclImpl.addEntry(newEntry);
        }

        IPSAclService aclService = PSAclServiceLocator.getAclService();
        aclService.saveAcls(aclList);
        List<IPSAcl> loadList = aclService.loadAclsModifiable(aclGuids);
        assertEquals(aclList, loadList);
        aclList = loadList;

        // check basic roundtrip
        aclService.saveAcls(aclList);
        loadList = aclService.loadAclsModifiable(aclGuids);
        assertEquals(aclList, loadList);
        aclList = loadList;

        // remove a permission
        for (IPSAcl acl : aclList)
        {
            PSAclImpl aclImpl = (PSAclImpl) acl;
            for (IPSAclEntry entry : aclImpl.getEntries())
            {
                entry.removePermission(PSPermissions.DELETE);
                assertFalse(entry.checkPermission(PSPermissions.DELETE));
            }
        }

        aclService.saveAcls(aclList);
        loadList = aclService.loadAclsModifiable(aclGuids);
        assertEquals(aclList, loadList);
        aclList = loadList;

        for (IPSAcl acl : aclList)
        {
            PSAclImpl aclImpl = (PSAclImpl) acl;
            List<IPSAclEntry> entries = new ArrayList<IPSAclEntry>(
                    aclImpl.getEntries());
            for (IPSAclEntry entry : entries)
            {
                if (!aclImpl.isOwner(entry.getPrincipal()))
                {
                    aclImpl.removeEntry((PSAclEntryImpl)entry);
                    assertTrue(aclImpl.findEntry(entry.getPrincipal()) == null);
                }
            }
        }

        aclService.saveAcls(aclList);
        loadList = aclService.loadAcls(aclGuids);
        assertEquals(aclList, loadList);

        return loadList;
    }

    /**
     * Tests deleting the supplied list of acls.
     *
     * @param aclList The acls to delete, assumed not <code>null</code>.
     *
     * @throws Exception if there are any errors.
     */
    public static void deleteAcls(List<IPSAcl> aclList) throws Exception
    {
        IPSAclService aclService = PSAclServiceLocator.getAclService();
        for (IPSAcl acl : aclList)
        {
            IPSGuid aclGuid = ((PSAclImpl)acl).getGUID();
            aclService.deleteAcl(aclGuid);

            try
            {
                aclService.loadAcl(aclGuid);
                fail("Should have thrown");
            }
            catch (PSSecurityException e)
            {
                // expected
            }
        }
    }

    /**
     * Creates and saves 3 test acls. The first is for a template
     * ({@link #ms_templateGuid}) with the following entries:
     * <ul>
     * <li>admin1 (user) - OWNER, READ</li>
     * <li>Editor (role) - READ, UPDATE</li>
     * <li>AnyCommunity (community) - RUNTIME_VISIBLE</li>
     * <li>comm1 (community) - RUNTIME_VISIBLE</li>
     * <li>comm2 (community) - no visibility</li>
     * </ul>
     * The second is for a slot:
     * <ul>
     * <li>admin2 (user) - OWNER, READ</li>
     * <li>Admin (role) - READ, UPDATE</li>
     * <li>AnyCommunity (community) - RUNTIME_VISIBLE</li>
     * <li>comm1 (community) - RUNTIME_VISIBLE</li>
     * <li>comm2 (community) - RUNTIME_VISIBLE</li>
     * </ul>
     * The third is for another template ({@link #ms_templateGuid2}):
     * <ul>
     * <li>admin1 (user) - OWNER, READ</li>
     * <li>Editor (role) - READ, UPDATE</li>
     * <li>AnyCommunity (community) - no visibility</li>
     * <li>comm1 (community) - no visibility</li>
     * <li>comm2 (community) - RUNTIME_VISIBLE</li>
     * </ul>
     *
     * @return The test acls, never <code>null</code> or empty.
     *
     * @throws Exception If there are any errors.
     */
    public static List<IPSAcl> createTestAcls() throws Exception
    {
        IPSAclService aclService = PSAclServiceLocator.getAclService();
        List<IPSAcl> aclList = new ArrayList<IPSAcl>();

        //cleanup acls from previous tests
        List<IPSGuid> ids = new ArrayList<IPSGuid>();
        ids.add(ms_slotTemplate);
        ids.add(ms_templateGuid);
        ids.add(ms_templateGuid2);

        // because of the way the tests are setup, it is possible to get multiple
        // acls for a given object GUID, this loop will remedy that situation
        List<IPSAcl> toDelete = aclService.loadAclsForObjects(ids);
        boolean finishedCleanup = false;
        while (!finishedCleanup)
        {
            for (IPSAcl acl : toDelete)
            {
                if (acl != null)
                    aclService.deleteAcl(acl.getGUID());
            }
            toDelete = aclService.loadAclsForObjects(ids);
            boolean found = false;
            for (IPSAcl acl : toDelete)
            {
                if (acl != null)
                    found = true;
            }
            finishedCleanup = !found;
        }

        PSAclImpl acl;
        acl = (PSAclImpl) aclService.createAcl(ms_templateGuid,
                new PSTypedPrincipal("admin1", PrincipalTypes.USER));
        acl.setName(TEST_ACL_NAME);
        aclList.add(acl);

        IPSAclEntry aclEntry;
        aclEntry = acl.getEntries().iterator().next();
        aclEntry.addPermission(PSPermissions.READ);

        aclEntry = new PSAclEntryImpl(new PSTypedPrincipal("Editor",
                PrincipalTypes.ROLE));
        aclEntry.addPermission(PSPermissions.UPDATE);
        aclEntry.addPermission(PSPermissions.READ);
        acl.addEntry((PSAclEntryImpl) aclEntry);

        aclEntry = new PSAclEntryImpl(new PSTypedPrincipal(
                PSTypedPrincipal.ANY_COMMUNITY_ENTRY, PrincipalTypes.COMMUNITY));
        aclEntry.addPermission(PSPermissions.RUNTIME_VISIBLE);
        acl.addEntry((PSAclEntryImpl) aclEntry);

        aclEntry = new PSAclEntryImpl(new PSTypedPrincipal("comm1",
                PrincipalTypes.COMMUNITY));
        aclEntry.addPermission(PSPermissions.RUNTIME_VISIBLE);
        acl.addEntry((PSAclEntryImpl) aclEntry);

        aclEntry = new PSAclEntryImpl(new PSTypedPrincipal("comm2",
                PrincipalTypes.COMMUNITY));
        acl.addEntry((PSAclEntryImpl) aclEntry);

        // acl 2
        acl = (PSAclImpl) aclService.createAcl(ms_slotTemplate,
                new PSTypedPrincipal("admin2", PrincipalTypes.USER));
        acl.setName(TEST_ACL_NAME);
        aclList.add(acl);

        aclEntry = acl.getEntries().iterator().next();
        aclEntry.addPermission(PSPermissions.READ);

        aclEntry = new PSAclEntryImpl(new PSTypedPrincipal("Admin",
                PrincipalTypes.ROLE));
        aclEntry.addPermission(PSPermissions.UPDATE);
        aclEntry.addPermission(PSPermissions.READ);
        acl.addEntry((PSAclEntryImpl) aclEntry);

        aclEntry = new PSAclEntryImpl(new PSTypedPrincipal(
                PSTypedPrincipal.ANY_COMMUNITY_ENTRY, PrincipalTypes.COMMUNITY));
        aclEntry.addPermission(PSPermissions.RUNTIME_VISIBLE);
        acl.addEntry((PSAclEntryImpl) aclEntry);

        aclEntry = new PSAclEntryImpl(new PSTypedPrincipal("comm1",
                PrincipalTypes.COMMUNITY));
        aclEntry.addPermission(PSPermissions.RUNTIME_VISIBLE);
        acl.addEntry((PSAclEntryImpl) aclEntry);

        aclEntry = new PSAclEntryImpl(new PSTypedPrincipal("comm2",
                PrincipalTypes.COMMUNITY));
        aclEntry.addPermission(PSPermissions.RUNTIME_VISIBLE);
        acl.addEntry((PSAclEntryImpl) aclEntry);

        // acl 3
        acl = (PSAclImpl) aclService.createAcl(ms_templateGuid2,
                new PSTypedPrincipal("admin1", PrincipalTypes.USER));
        acl.setName(TEST_ACL_NAME);
        aclList.add(acl);

        aclEntry = acl.getEntries().iterator().next();
        aclEntry.addPermission(PSPermissions.READ);

        aclEntry = new PSAclEntryImpl(new PSTypedPrincipal("Editor",
                PrincipalTypes.ROLE));
        aclEntry.addPermission(PSPermissions.UPDATE);
        aclEntry.addPermission(PSPermissions.READ);
        acl.addEntry((PSAclEntryImpl) aclEntry);

        aclEntry = new PSAclEntryImpl(new PSTypedPrincipal(
                PSTypedPrincipal.ANY_COMMUNITY_ENTRY, PrincipalTypes.COMMUNITY));
        acl.addEntry((PSAclEntryImpl) aclEntry);

        aclEntry = new PSAclEntryImpl(new PSTypedPrincipal("comm1",
                PrincipalTypes.COMMUNITY));
        acl.addEntry((PSAclEntryImpl) aclEntry);

        aclEntry = new PSAclEntryImpl(new PSTypedPrincipal("comm2",
                PrincipalTypes.COMMUNITY));
        aclEntry.addPermission(PSPermissions.RUNTIME_VISIBLE);
        acl.addEntry((PSAclEntryImpl) aclEntry);

        aclService.saveAcls(aclList);

        return aclList;
    }


    /**
     * Test loading the acls
     *
     * @param aclList The list of acls to expect, assumed not <code>null</code>.
     * @return The loaded acls, never <code>null</code>. These are modifiable
     * and may be saved.
     * @throws Exception if the test fails.
     */
    private List<IPSAcl> testLoadMethods(List<IPSAcl> aclList) throws Exception
    {
        IPSAclService aclService = PSAclServiceLocator.getAclService();
        List<IPSGuid> aclGuids = new ArrayList<IPSGuid>();
        List<IPSGuid> objectGuids = new ArrayList<IPSGuid>();
        for (IPSAcl acl : aclList)
        {
            PSAclImpl aclImpl = (PSAclImpl)acl;
            IPSGuid guid = aclImpl.getGUID();
            IPSGuid objGuid = new PSGuid(PSTypeEnum.valueOf(
                    aclImpl.getObjectType()), aclImpl.getObjectId());
            IPSAcl idtest = aclService.loadAcl(guid);
            IPSAcl objtest = aclService.loadAclForObject(objGuid);
            assertTrue(idtest.equals(objtest));

            aclGuids.add(guid);
            objectGuids.add(objGuid);
        }
        List<IPSAcl> loadList = aclService.loadAclsModifiable(aclGuids);
        assertEquals(aclList, loadList);
        for (IPSAcl acl : loadList)
        {
            PSAclImpl aclImpl = (PSAclImpl)acl;
            assertEquals(acl, aclService.loadAcl(
                    aclImpl.getGUID()));
        }

        assertEquals(loadList.size(),
                aclService.loadAclsForObjects(objectGuids).size());

        return loadList;
    }

    //clean all test acls before starting test
    static
    {
        IPSAclService svc = PSAclServiceLocator.getAclService();
        try
        {
            List<IPSAcl> acls = svc.loadAcls(null);
            for (IPSAcl acl : acls)
            {
                if (acl.getName().equalsIgnoreCase(TEST_ACL_NAME))
                    svc.deleteAcl(acl.getGUID());
            }
        }
        catch (Exception e)
        {
            System.out.println("Failed while cleaning up test acls.");
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }
}
