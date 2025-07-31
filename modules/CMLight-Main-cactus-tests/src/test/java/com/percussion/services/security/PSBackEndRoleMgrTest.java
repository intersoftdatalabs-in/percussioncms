// REFACTORED: CP-JAVA11
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

import com.percussion.design.objectstore.PSSubject;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.security.data.PSBackEndRole;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.services.security.data.PSCommunityRoleAssociation;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.security.IPSSecurityWs;
import com.percussion.webservices.security.PSSecurityWsLocator;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case for the {@link IPSBackEndRoleMgr}
 */
@Tag("IntegrationTest")
public class PSBackEndRoleMgrTest {

    @Test
    void testGetRhythmyxRoles() throws Exception {
        var mgr = PSRoleMgrLocator.getBackEndRoleManager();
        var roles = mgr.getRhythmyxRoles();
        assertFalse(roles.isEmpty());
        assertTrue(roles.contains("Admin"));
        assertTrue(roles.contains("Author"));
        assertTrue(roles.contains("Editor"));

        // TODO - the following 2 lines do not work, comment out for now.
        //assertTrue(hasSameMembers(roles, mgr.getRhythmyxRoles(null, 0)));
        //assertTrue(hasSameMembers(roles, mgr.getRhythmyxRoles("", 0)));

        roles = mgr.getRhythmyxRoles("admin1", PSSubject.SUBJECT_TYPE_USER);
        assertFalse(roles.isEmpty());
        assertTrue(roles.contains("Admin"));
        assertTrue(hasSameMembers(roles, mgr.getRhythmyxRoles("admin1", 0)));
        roles = mgr.getRhythmyxRoles("admin1", PSSubject.SUBJECT_TYPE_GROUP);
        assertTrue(roles.isEmpty());

        assertTrue(mgr.getRhythmyxRoles("foo", 0).isEmpty());

        roles = mgr.getRhythmyxRoles(null, PSSubject.SUBJECT_TYPE_USER);
        assertFalse(roles.isEmpty());
        assertTrue(roles.contains("Admin"));
        assertTrue(roles.contains("Author"));
        assertTrue(roles.contains("Editor"));
    }

    @Test
    void testSetRhythmyxRoles() throws Exception {
        final IPSSecurityWs svc = PSSecurityWsLocator.getSecurityWebservice();
        var mgr = PSRoleMgrLocator.getBackEndRoleManager();

        var subName = "editor-test";
        var subType = PSSubject.SUBJECT_TYPE_USER;
        var roleNames = new String[]{"Editor", "Artist"};

        try {
            mgr.setRhythmyxRoles(subName, subType, Arrays.asList(roleNames));
            fail("Should fail on authentication here.");
        } catch (Exception e) {
            // should be here due to fail authentication
        }

        svc.login("admin1", "demo", null, null);

        // set subject to roles
        mgr.setRhythmyxRoles(subName, subType, Arrays.asList(roleNames));

        // validating above operation
        var roles = mgr.getRhythmyxRoles(subName, subType);

        assertTrue(roles.contains("Editor"));
        assertTrue(roles.contains("Artist"));

        // remove the subject from "Artist" role
        roleNames = new String[]{"Editor"};
        mgr.setRhythmyxRoles(subName, subType, Arrays.asList(roleNames));

        // validating above operation
        roles = mgr.getRhythmyxRoles(subName, subType);

        assertTrue(roles.contains("Editor"));
        assertFalse(roles.contains("Artist"));

        // remove the subject from all roles
        mgr.setRhythmyxRoles(subName, subType, Collections.emptyList());

        // validating remove operation
        roles = mgr.getRhythmyxRoles(subName, subType);
        assertFalse(roles.contains("Editor"));
        assertFalse(roles.contains("Artist"));
    }

    @Test
    void testSetMultipleRhythmyxRoles() throws Exception {
        final IPSSecurityWs svc = PSSecurityWsLocator.getSecurityWebservice();
        var mgr = PSRoleMgrLocator.getBackEndRoleManager();

        var subName = "editor-test";
        var subjectNames = new ArrayList<String>();
        subjectNames.add(subName);
        for (int i = 0; i < 200; i++) {
            subjectNames.add(subName + i);
        }

        var subType = PSSubject.SUBJECT_TYPE_USER;
        var roleNames = new String[]{"Editor", "Artist"};

        try {
            mgr.setRhythmyxRoles(subjectNames, subType, Arrays.asList(roleNames));
            fail("Should fail on authentication here.");
        } catch (Exception e) {
            // should be here due to fail authentication
        }

        svc.login("admin1", "demo", null, null);

        // set subject to roles
        mgr.setRhythmyxRoles(subjectNames, subType, Arrays.asList(roleNames));

        for (var subjectName : subjectNames) {
            var roles = mgr.getRhythmyxRoles(subjectName, subType);

            assertTrue(roles.contains("Editor"));
            assertTrue(roles.contains("Artist"));
        }

        // remove the subject from "Artist" role
        roleNames = new String[]{"Editor"};
        mgr.setRhythmyxRoles(subName, subType, Arrays.asList(roleNames));

        // validating above operation
        var roles = mgr.getRhythmyxRoles(subName, subType);

        assertTrue(roles.contains("Editor"));
        assertFalse(roles.contains("Artist"));

        // remove the subject from all roles
        mgr.setRhythmyxRoles(subName, subType, Collections.emptyList());

        // validating remove operation
        roles = mgr.getRhythmyxRoles(subName, subType);
        assertFalse(roles.contains("Editor"));
        assertFalse(roles.contains("Artist"));
    }

    boolean hasSameMembers(List<?> list1, List<?> list2) {
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }

    @Test
    void testGetCommunityRoles() throws Exception {
        var mgr = PSRoleMgrLocator.getBackEndRoleManager();
        var roles = mgr.getCommunityRoles(10);
        assertTrue(roles.contains("Default"));
        assertTrue(roles.contains("RxPublisher"));
    }

    @Test
    void testLoadCommunities() throws Exception {
        var mgr = PSRoleMgrLocator.getBackEndRoleManager();
        var communities = mgr.loadCommunities(new IPSGuid[]{
                new PSGuid(PSTypeEnum.COMMUNITY_DEF, 10),
                new PSGuid(PSTypeEnum.COMMUNITY_DEF, 1002)});
        assertEquals(2, communities.length);
        assertEquals("Default", communities[0].getName());
        assertEquals("Enterprise_Investments", communities[1].getName());
    }

    @Test
    void testLoadRoles() throws Exception {
        var mgr = PSRoleMgrLocator.getBackEndRoleManager();
        var roles = mgr.loadRoles(new IPSGuid[]{
                new PSGuid(PSTypeEnum.ROLE, 1), new PSGuid(PSTypeEnum.ROLE, 2)});
        assertEquals(2, roles.length);
        assertEquals("Admin", roles[0].getName());
        assertEquals("Author", roles[1].getName());
    }

    @Test
    void testCommunityCRUD() throws Exception {
        var mgr = PSRoleMgrLocator.getBackEndRoleManager();

        PSCommunity community = null;
        try {
            // delete left-overs from previous tests
            var communityName = "TestCommunity";
            var communities = mgr.findCommunitiesByName(communityName);
            if (!communities.isEmpty()) {
                community = communities.get(0);
                mgr.deleteCommunity(community.getGUID());
            }

            assertThrows(IllegalArgumentException.class, () -> mgr.createCommunity(null, null));
            assertThrows(IllegalArgumentException.class, () -> mgr.createCommunity(" ", null));
            assertThrows(IllegalArgumentException.class, () -> mgr.saveCommunity(null));

            // create new test community
            community = mgr.createCommunity(communityName, "Testing communities");

            // associate all existing roles
            var roles = mgr.findRolesByName("%");
            for (var role : roles)
                community.addRoleAssociation(new PSGuid(PSTypeEnum.ROLE, role.getId()));

            // save the test community
            mgr.saveCommunity(community);

            // Modify and resave
            communities = mgr.findCommunitiesByName(communityName);
            assertNotNull(communities);
            assertEquals(1, communities.size());
            community = communities.get(0);
            community.setDescription("New description");
            mgr.saveCommunity(community);

            assertThrows(IllegalArgumentException.class,
                    () -> mgr.createCommunity(communityName.toUpperCase(), null));

            // read community back and verify its equal to what we saved
            communities = mgr.findCommunitiesByName(communityName);
            assertFalse(communities.isEmpty() && communities.size() > 1);
            var community2 = communities.get(0);
            assertEquals(community, community2);

            // set new role associations, only every 2nd
            int index = 0;
            Collection<IPSGuid> roleAssociations = new ArrayList<>();
            for (var role : roles) {
                if ((index % 2) > 0)
                    roleAssociations.add(new PSGuid(PSTypeEnum.ROLE, role.getId()));
                index++;
            }
            community.setRoleAssociations(roleAssociations);

            // save the test community
            mgr.saveCommunity(community);

            // read community back and verify its equal to what we saved
            communities = mgr.findCommunitiesByName(communityName);
            assertFalse(communities.isEmpty() && communities.size() > 1);
            community2 = communities.get(0);
            assertEquals(community, community2);

            // associate all existing roles
            for (var role : roles)
                community.addRoleAssociation(new PSGuid(PSTypeEnum.ROLE, role.getId()));

            // save the test community
            mgr.saveCommunity(community);

            // read community back and verify its equal to what we saved
            communities = mgr.findCommunitiesByName(communityName);
            assertFalse(communities.isEmpty() && communities.size() > 1);
            community2 = communities.get(0);
            assertEquals(community, community2);

            // remove associated role
            community.removeRoleAssociation(new PSGuid(PSTypeEnum.ROLE, roles.get(0).getId()));

            // save the test community
            mgr.saveCommunity(community);

            // read community back and verify its equal to what we saved
            communities = mgr.findCommunitiesByName(communityName);
            assertFalse(communities.isEmpty() && communities.size() > 1);
            community2 = communities.get(0);
            assertEquals(community, community2);

            // find all communities
            communities = mgr.findCommunitiesByName("%");
            assertNotNull(communities);
            assertTrue(communities.size() > 0);
            var count = communities.size();
            communities = mgr.findCommunitiesByName(null);
            assertNotNull(communities);
            assertEquals(count, communities.size());
            communities = mgr.findCommunitiesByName(" ");
            assertNotNull(communities);
            assertEquals(count, communities.size());

            // find the test community
            communities = mgr.findCommunitiesByName(communityName);
            assertNotNull(communities);
            assertEquals(1, communities.size());

            // update the test community
            community.setDescription("New description");
            mgr.saveCommunity(community);

            assertThrows(IllegalArgumentException.class, () -> mgr.deleteCommunity(null));
        } finally {
            // delete the test community
            if (community != null)
                mgr.deleteCommunity(community.getGUID());
        }
    }

    @Test
    void testFindCommunitiesByRole() throws Exception {
        var guidMgr = PSGuidManagerLocator.getGuidMgr();
        var roleMgr = PSRoleMgrLocator.getBackEndRoleManager();
        var roleIds = new ArrayList<IPSGuid>();

        var editor = guidMgr.makeGuid(4, PSTypeEnum.ROLE);
        var author = guidMgr.makeGuid(2, PSTypeEnum.ROLE);
        var eiMembers = guidMgr.makeGuid(304, PSTypeEnum.ROLE);
        var ciMembers = guidMgr.makeGuid(307, PSTypeEnum.ROLE);

        roleIds.add(editor);
        roleIds.add(author);
        assertTrue(roleMgr.findCommunitiesByRole(roleIds).isEmpty());

        roleIds.clear();
        roleIds.add(eiMembers);
        var results = roleMgr.findCommunitiesByRole(roleIds);
        assertEquals(1, results.size());
        assertEquals(eiMembers.getUUID(), results.get(0).getRoleId());
        assertEquals(1002, results.get(0).getCommunityId());

        roleIds.clear();
        roleIds.add(ciMembers);
        results = roleMgr.findCommunitiesByRole(roleIds);
        assertEquals(1, results.size());
        assertEquals(ciMembers.getUUID(), results.get(0).getRoleId());
        assertEquals(1003, results.get(0).getCommunityId());

        roleIds.add(eiMembers);
        roleIds.add(editor);
        roleIds.add(author);

        results = roleMgr.findCommunitiesByRole(roleIds);
        assertEquals(2, results.size());
    }
}
