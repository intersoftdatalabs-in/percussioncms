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
package com.percussion.security;

import com.percussion.design.objectstore.PSGlobalSubject;
import com.percussion.design.objectstore.PSSubject;
import com.percussion.server.PSRequest;
import com.percussion.server.PSUserSession;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case for the {@link PSRoleManager}. Requires the mock role and subject
 * cataloger registrations found in the
 * UnitTestResources\com\percussion\security\cataloger-beans.xml to be added to
 * the cataloger-beans.xml used by the running server, and also requires that the
 * PSRoleManagerTest.class file be manually deployed to the
 * WEB-INF/classes/com/percussion/security directory and the
 * IPSCustomJunitTest.class file be manually deployed to the
 * WEB-INF/classes/com/percussion/testing directory (requires server restart).
 *
 * <p>This test class has been updated for Java 11 compatibility with proper
 * generic type usage and modern Java practices.</p>
 */
@Tag("IntegrationTest")
public class PSRoleManagerTest {

    @Test
    public void testGetRoles() {
        var roles = PSRoleManager.getInstance().getRoles();
        assertFalse(roles.isEmpty(), "Roles list should not be empty");
        assertTrue(roles.contains("Admin"), "Should contain Admin role");
        assertTrue(roles.contains("Editor"), "Should contain Editor role");
    }

    @Test
    public void testGetRolesStringint() {
        var mgr = PSRoleManager.getInstance();
        List<String> roles;

        roles = mgr.getRoles("admin1", PSSubject.SUBJECT_TYPE_USER);
        assertFalse(roles.isEmpty(), "Admin1 roles should not be empty");
        assertTrue(roles.contains("Admin"), "Admin1 should have Admin role");
        assertEquals(roles, mgr.getRoles("admin1", 0), "getRoles with type should match getRoles with 0");

        roles = mgr.getRoles("subCatUser1_1", PSSubject.SUBJECT_TYPE_USER);
        assertFalse(roles.isEmpty(), "SubCatUser1_1 roles should not be empty");
        assertTrue(roles.contains("Author"), "SubCatUser1_1 should have Author role");
        assertEquals(roles, mgr.getRoles("subCatUser1_1", 0), "getRoles with type should match getRoles with 0");

        roles = mgr.getRoles("subCatGroup2", PSSubject.SUBJECT_TYPE_USER);
        assertTrue(roles.isEmpty(), "SubCatGroup2 user roles should be empty");

        roles = mgr.getRoles("subCatGroup2", PSSubject.SUBJECT_TYPE_GROUP);
        assertTrue(roles.contains("QA"), "SubCatGroup2 should have QA role");
        assertEquals(roles, mgr.getRoles("subCatGroup2", 0), "getRoles with type should match getRoles with 0");

        assertEquals(mgr.getRoles(null, 0), mgr.getRoles(), "Null subject should return all roles");

        roles = mgr.getRoles(null, PSSubject.SUBJECT_TYPE_GROUP);
        assertTrue(roles.contains("QA"), "Group roles should contain QA");
        assertFalse(roles.contains("Author"), "Group roles should not contain Author");
        assertFalse(roles.contains("Editor"), "Group roles should not contain Editor");

        assertEquals(mgr.getRoles(null, 0), mgr.getRoles(), "Null subject with type 0 should match getRoles()");

        roles = mgr.getRoles(null, PSSubject.SUBJECT_TYPE_USER);
        assertTrue(roles.contains("Admin"), "User roles should contain Admin");
        assertTrue(roles.contains("Author"), "User roles should contain Author");
    }

    @Test
    public void testIsMemberOfRole() {
        var mgr = PSRoleManager.getInstance();

        assertTrue(mgr.isMemberOfRole("admin1", "Admin", PSSubject.SUBJECT_TYPE_USER),
                "admin1 should be member of Admin role");

        assertTrue(mgr.isMemberOfRole("editor1", "Editor", PSSubject.SUBJECT_TYPE_USER),
                "editor1 should be member of Editor role");

        assertFalse(mgr.isMemberOfRole("editor1", "Admin", PSSubject.SUBJECT_TYPE_USER),
                "editor1 should not be member of Admin role");

        assertTrue(mgr.isMemberOfRole("subCatUser1_1", "Author", PSSubject.SUBJECT_TYPE_USER),
                "subCatUser1_1 should be member of Author role");

        assertFalse(mgr.isMemberOfRole("subCatUser1_1", "Editor", PSSubject.SUBJECT_TYPE_USER),
                "subCatUser1_1 should not be member of Editor role");

        assertTrue(mgr.isMemberOfRole("subCatGroup1", "QA", PSSubject.SUBJECT_TYPE_GROUP),
                "subCatGroup1 should be member of QA role");

        assertFalse(mgr.isMemberOfRole("subCatGroup1", "Editor", PSSubject.SUBJECT_TYPE_GROUP),
                "subCatGroup1 should not be member of Editor role");
    }

    @Test
    public void testMemberRoleList() throws Exception {
        var mgr = PSRoleManager.getInstance();
        // login to create a session
        PSSecurityFilter.authenticate(request, response, "admin1", "demo");

        var psreq = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
        var userSession = psreq.getUserSession();

        List<String> roles;
        roles = mgr.memberRoleList(userSession, "author1");
        assertTrue(roles.contains("Author"));
        assertFalse(roles.contains("Admin"));
        assertEquals(roles, mgr.memberRoleList(userSession,
                new PSGlobalSubject("author1", PSSubject.SUBJECT_TYPE_USER, null)));

        roles = mgr.memberRoleList(userSession, "subCatUser1_1");
        assertTrue(roles.contains("Author"));
        assertFalse(roles.contains("Admin"));
        assertEquals(roles, mgr.memberRoleList(userSession,
                new PSGlobalSubject("subCatUser1_1", PSSubject.SUBJECT_TYPE_USER, null)));

        roles = mgr.memberRoleList(userSession, new PSGlobalSubject(
                "subCatGroup1", PSSubject.SUBJECT_TYPE_GROUP, null));
        assertTrue(roles.contains("QA"));
    }

    @Test
    public void testExpandGroups() {
        var mgr = PSRoleManager.getInstance();
        Set<PSSubject> groups = new HashSet<>();
        var foo = new PSGlobalSubject("foo", PSSubject.SUBJECT_TYPE_GROUP, null);
        var editor = new PSGlobalSubject("editor", PSSubject.SUBJECT_TYPE_USER, null);
        var group1 = new PSGlobalSubject("subCatGroup1", PSSubject.SUBJECT_TYPE_GROUP, null);
        var group2 = new PSGlobalSubject("subCatGroup2", PSSubject.SUBJECT_TYPE_GROUP, null);

        groups.add(foo);
        groups.add(editor);
        groups.add(group1);
        groups.add(group2);
        var expanded = mgr.expandGroups(groups);

        assertTrue(expanded.contains(foo));
        assertTrue(expanded.contains(editor));
        assertFalse(expanded.contains(group1));
        assertFalse(expanded.contains(group2));

        Set<String> users = new HashSet<>();

        for (var subject : expanded) {
            if (subject.getType() == PSSubject.SUBJECT_TYPE_USER)
                users.add(subject.getName());
        }
        assertTrue(users.contains("subCatUser1_1"));
        assertTrue(users.contains("subCatUser2_1"));
        assertTrue(users.contains("subCatUser1_2"));
    }

    @Test
    public void testRoleMembersStringintString() {
        Set<String> userNames = new HashSet<>();
        Set<String> groupNames = new HashSet<>();

        getRoleMembers("Editor", 0, null, userNames, groupNames);
        assertTrue(userNames.contains("editor1"));
        assertTrue(userNames.contains("editor2"));
        assertTrue(userNames.contains("subCatUser2_1"));
        assertTrue(userNames.contains("subCatUser2_2"));

        getRoleMembers("QA", PSSubject.SUBJECT_TYPE_USER | PSSubject.SUBJECT_TYPE_GROUP, null, userNames, groupNames);
        assertTrue(userNames.contains("qa1"));
        assertFalse(userNames.contains("editor1"));
        assertFalse(userNames.contains("subCatGroup1"));
        assertTrue(groupNames.contains("subCatGroup1"));
        assertFalse(groupNames.contains("qa1"));

        Set<String> savedUsers = new HashSet<>(userNames);
        Set<String> savedGroups = new HashSet<>(groupNames);
        getRoleMembers("QA", 0, null, userNames, groupNames);
        assertEquals(userNames, savedUsers);
        assertEquals(groupNames, savedGroups);

        getRoleMembers("QA", PSSubject.SUBJECT_TYPE_USER, null, userNames, groupNames);
        assertTrue(userNames.contains("qa1"));
        assertFalse(userNames.contains("editor1"));
        assertFalse(userNames.contains("subCatGroup1"));
        assertTrue(groupNames.isEmpty());

        getRoleMembers("QA", PSSubject.SUBJECT_TYPE_GROUP, null, userNames, groupNames);
        assertTrue(userNames.isEmpty());
        assertTrue(groupNames.contains("subCatGroup1"));
        assertTrue(groupNames.contains("subCatGroup2"));

        getRoleMembers("QA", 0, "qa%", userNames, groupNames);
        assertTrue(userNames.contains("qa1"));
        assertTrue(userNames.contains("qa2"));
        assertTrue(groupNames.isEmpty());

        getRoleMembers("QA", 0, "sub%", userNames, groupNames);
        assertTrue(groupNames.contains("subCatGroup1"));
        assertTrue(groupNames.contains("subCatGroup2"));
        assertTrue(userNames.isEmpty());
    }

    private void getRoleMembers(String name, int typeFlags, String filter,
                                Set<String> userNames, Set<String> groupNames) {
        filterSubjectNames(
                PSRoleManager.getInstance().roleMembers(name, typeFlags, filter),
                userNames, groupNames);
    }

    private void filterSubjectNames(Collection<PSSubject> members,
                                    Set<String> userNames, Set<String> groupNames) {
        userNames.clear();
        groupNames.clear();
        for (var sub : members) {
            if (sub.getType() == PSSubject.SUBJECT_TYPE_USER)
                userNames.add(sub.getName());
            else
                groupNames.add(sub.getName());
        }
    }

    @Test
    public void testGetSubjectsStringString() {
        var mgr = PSRoleManager.getInstance();
        Set<String> userNames = new HashSet<>();
        Set<String> groupNames = new HashSet<>();
        Collection<PSSubject> subjects;

        subjects = mgr.getSubjects("Editor", null);
        filterSubjectNames(subjects, userNames, groupNames);
        assertTrue(groupNames.isEmpty());
        assertTrue(userNames.contains("editor1"));
        assertTrue(userNames.contains("subCatUser2_1"));

        subjects = mgr.getSubjects("Editor", "ed%");
        filterSubjectNames(subjects, userNames, groupNames);
        assertTrue(groupNames.isEmpty());
        assertTrue(userNames.contains("editor1"));
        assertFalse(userNames.contains("subCatUser2_1"));

        subjects = mgr.getSubjects(null, "ed%");
        filterSubjectNames(subjects, userNames, groupNames);
        assertTrue(groupNames.isEmpty());
        assertEquals(2, userNames.size());
        assertTrue(userNames.contains("editor1"));
        assertTrue(userNames.contains("editor2"));

        subjects = mgr.getSubjects(null, "subCatUser2%");
        filterSubjectNames(subjects, userNames, groupNames);
        assertTrue(groupNames.isEmpty());
        assertEquals(2, userNames.size());
        assertTrue(userNames.contains("subCatUser2_1"));
        assertTrue(userNames.contains("subCatUser2_2"));

        subjects = mgr.getSubjects(null, "subCatGroup%");
        filterSubjectNames(subjects, userNames, groupNames);
        assertTrue(userNames.isEmpty());
        assertEquals(2, groupNames.size());
        assertTrue(groupNames.contains("subCatGroup1"));
        assertTrue(groupNames.contains("subCatGroup2"));

        subjects = mgr.getSubjects("", "");
        filterSubjectNames(subjects, userNames, groupNames);
        assertFalse(groupNames.isEmpty());
        assertTrue(userNames.contains("editor1"));
        assertTrue(userNames.contains("editor2"));
        assertTrue(userNames.contains("subCatUser2_1"));
        assertTrue(userNames.contains("subCatUser2_2"));
        assertTrue(groupNames.contains("subCatGroup1"));
        assertTrue(groupNames.contains("subCatGroup2"));
    }

    @Test
    public void testGetRoleEmailAddresses() {
        var mgr = PSRoleManager.getInstance();
        Set<?> noMailSet = new HashSet<>();
        Set<String> mailSet;

        mailSet = mgr.getRoleEmailAddresses("Editor", null, null, noMailSet);
        assertTrue(mailSet.contains("subCatUser2_1@test.percussion.com"));
        assertTrue(mailSet.contains("subCatUser2_2@test.percussion.com"));
        assertEquals(mailSet, mgr.getRoleEmailAddresses("Editor", null, null, null));
        Set<String> users = new HashSet<>();
        Set<String> groups = new HashSet<>();
        filterSubjectNames(noMailSet, users, groups);
        assertTrue(users.contains("editor1"));
        assertTrue(users.contains("editor2"));

        mailSet = mgr.getRoleEmailAddresses("Editor", "mail", "Default", null);
        assertTrue(mailSet.isEmpty());

        mailSet = mgr.getRoleEmailAddresses("foo", null, null, null);
        assertTrue(mailSet.isEmpty());
    }

    @Test
    public void testGetSubjectEmailAddresses() {
        var mgr = PSRoleManager.getInstance();

        Set<?> mailSet = mgr.getSubjectEmailAddresses("subCatUser2_1", null, null);
        assertEquals(1, mailSet.size());
        assertTrue(mailSet.contains("subCatUser2_1@test.percussion.com"));
        assertEquals(mailSet, mgr.getSubjectEmailAddresses("subCatUser2_1", "foo", null));
        mailSet = mgr.getSubjectEmailAddresses("subCatUser2_1", null, "Default");
        assertTrue(mailSet.isEmpty());
    }
}
