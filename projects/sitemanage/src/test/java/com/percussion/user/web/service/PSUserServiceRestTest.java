// REFACTORED: CP-JAVA11
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
package com.percussion.user.web.service;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.role.data.PSRole;
import com.percussion.role.web.service.PSRoleServiceRestClient;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.share.data.PSStringWrapper;
import com.percussion.share.test.PSObjectRestClient.DataRestClientException;
import com.percussion.share.test.PSObjectRestClient.DataValidationRestClientException;
import com.percussion.share.test.PSRestTestCase;
import com.percussion.share.test.PSTestDataCleaner;
import com.percussion.user.data.PSAccessLevel;
import com.percussion.user.data.PSAccessLevelRequest;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.data.PSExternalUser;
import com.percussion.user.data.PSRoleList;
import com.percussion.user.data.PSUser;
import com.percussion.user.data.PSUserList;
import com.percussion.user.service.IPSUserService.PSDirectoryServiceStatus;
import com.percussion.user.service.IPSUserService.PSDirectoryServiceStatus.ServiceStatus;
import com.percussion.user.service.IPSUserService.PSImportUsers;
import com.percussion.user.data.PSImportedUser;
import com.percussion.user.data.PSImportedUser.ImportStatus;

import java.util.Collections;
import java.util.List;

import org.hamcrest.core.CombinableMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test the user service through REST.
 * Refactored for Java 11 and JUnit5.
 */
public class PSUserServiceRestTest extends PSRestTestCase<PSUserServiceRestClient> {

    protected PSUser user;

    @Override
    protected PSUserServiceRestClient getRestClient(final String baseUrl) {
        var client = new PSUserServiceRestClient();
        client.setUrl(baseUrl);
        return client;
    }

    @BeforeEach
    public void setupUser() {
        user = createUser("MyTestId", "MyPassword", asList("Admin"));
    }

    @AfterEach
    public void runCleaner() {
        userCleaner.clean();
    }

    @Test
    public void testCreate() throws Exception {
        userCleaner.add(user.getName());
        restClient.create(user);
    }

    @Test
    public void testDelete() throws Exception {
        testCreate();
        restClient.delete(user.getName());
        userCleaner.remove(user.getName());
    }

    @Test
    public void testDeleteSystemUserShouldFail() {
        try {
            restClient.delete("rxserver");
            fail("Expected DataValidationRestClientException");
        } catch (DataValidationRestClientException e) {
            // expected
        }
    }

    @Test
    public void testCurrent() throws Exception {
        var currentUser = restClient.getCurrentUser();
        assertEquals("admin1", currentUser.getName());
        assertTrue(currentUser.isAdminUser());
    }

    @Test
    public void testFind() throws Exception {
        testCreate();
        var actual = restClient.find(user.getName());
        assertThat(actual.getName(), is(equalTo(user.getName())));
        assertThat(actual.getRoles(), is(equalTo(user.getRoles())));

        var roles = user.getRoles();
        assertTrue(!roles.contains("System"), "Should not include \"System\" role");
        assertTrue(!roles.contains("Default"), "Should not include \"Default\" role");
    }

    @Test
    public void testFindExternalOnRealLDAPServer() throws Exception {
        assumeDirectoryServer(ServiceStatus.ENABLED);
        var externalUsers = restClient.findUsersFromDirectoryService("sun%");
        assertThat(externalUsers, is(not(nullValue())));
        assertThat(externalUsers.get(0).getName(), startsWith("sun"));
    }

    @Test
    public void testFindExternalForDisabledLdap() {
        assumeDirectoryServer(ServiceStatus.DISABLED);
        try {
            restClient.findUsersFromDirectoryService("sun%");
            fail("Expected DataRestClientException");
        } catch (DataRestClientException e) {
            // expected
        }
    }

    @Test
    public void testImportUsers() throws Exception {
        var importUsers = new PSImportUsers();
        var a = new PSExternalUser();
        a.setName("a");
        var b = new PSExternalUser();
        b.setName("b");
        importUsers.setExternalUsers(asList(a, b));

        userCleaner.add("a", "b");

        var importedUsers = restClient.importDirectoryUsers(importUsers);
        assertThat(importedUsers, is(not(nullValue())));
        assertThat(importedUsers.get(0).getName(), is(equalTo("a")));
        assertThat(importedUsers.get(0).getStatus(), is(equalTo(ImportStatus.SUCCESS)));

        assertThat(importedUsers.get(1).getName(), is(equalTo("b")));
        assertThat(importedUsers.get(1).getStatus(), is(equalTo(ImportStatus.SUCCESS)));
        assertThat(importedUsers.size(), is(2));

        // Imported users should be in the contributor role.
        var importedUser = restClient.find("a");
        assertThat(importedUser.getRoles(), hasItem("Contributor"));

        // Import again should show duplicate for "a"
        importUsers.setExternalUsers(asList(a));
        importedUsers = restClient.importDirectoryUsers(importUsers);
        assertThat(importedUsers.size(), is(1));
        assertThat(importedUsers.get(0).getStatus(), is(equalTo(ImportStatus.DUPLICATE)));
    }

    public void assumeDirectoryServer(ServiceStatus status) {
        var actual = restClient.checkDirectoryService();
        org.junit.jupiter.api.Assumptions.assumeTrue(actual.getStatus() == status);
    }

    @Test
    public void testCheckDirectoryService() throws Exception {
        var status = restClient.checkDirectoryService();
        assertThat(status, not(nullValue()));
        assertThat(status.getStatus(), anyOf(is(ServiceStatus.DISABLED), is(ServiceStatus.ENABLED)));
    }

    @Test
    public void testGetRoles() throws Exception {
        var roleList = restClient.getRoles();
        assertThat(roleList.getRoles(), hasItem("Admin"));

        assertTrue(!roleList.getRoles().contains("System"), "Should not include \"System\" role");
        assertTrue(!roleList.getRoles().contains("Default"), "Should not include \"Default\" role");
    }

    @Test
    public void testGetUsers() throws Exception {
        testCreate();
        var userList = restClient.getUsers();
        assertThat(userList.getUsers(), hasItem(user.getName()));
        assertThat(userList.getUsers(), not(hasItem("rxserver")));
    }

    @Test
    public void testGetUserNames() throws Exception {
        testCreate();
        var userList = restClient.getUserNames(user.getName());
        assertThat(userList.getUsers(), hasItem(user.getName()));
        assertThat(userList.getUsers(), not(hasItem("rxserver")));

        for (int i = 0; i < user.getName().length(); i++) {
            var name = user.getName().substring(0, i) + "%";
            userList = restClient.getUserNames(name);
            assertThat(userList.getUsers(), hasItem(user.getName()));
            assertThat(userList.getUsers(), not(hasItem("rxserver")));
        }

        var aUser = createUser("a" + user.getName(), "demo", asList("Admin"));
        userCleaner.add(aUser.getName());
        restClient.create(aUser);
        var userz = createUser(user.getName() + "z", "demo", asList("Admin"));
        userCleaner.add(userz.getName());
        restClient.create(userz);

        userList = restClient.getUserNames(user.getName() + "%");
        assertThat(userList.getUsers(), hasItem(user.getName()));
        assertThat(userList.getUsers(), hasItem(userz.getName()));
        assertThat(userList.getUsers(), not(hasItem(aUser.getName())));
        assertThat(userList.getUsers(), not(hasItem("rxserver")));

        userList = restClient.getUserNames("%" + user.getName());
        assertThat(userList.getUsers(), hasItem(user.getName()));
        assertThat(userList.getUsers(), not(hasItem(userz.getName())));
        assertThat(userList.getUsers(), hasItem(aUser.getName()));
        assertThat(userList.getUsers(), not(hasItem("rxserver")));

        userList = restClient.getUserNames("%" + user.getName() + "%");
        assertThat(userList.getUsers(), hasItem(user.getName()));
        assertThat(userList.getUsers(), hasItem(userz.getName()));
        assertThat(userList.getUsers(), hasItem(aUser.getName()));
        assertThat(userList.getUsers(), not(hasItem("rxserver")));
    }

    @Test
    public void testGetUsersByRole() throws Exception {
        var userList = restClient.getUsersByRole("Admin");
        var numAdminUsers = userList.getUsers().size();
        for (var name : userList.getUsers()) {
            var u = restClient.find(name);
            assertTrue(u.getRoles().contains("Admin"));
        }

        testCreate();

        userList = restClient.getUsersByRole("Admin");
        assertEquals(numAdminUsers + 1, userList.getUsers().size());
        assertThat(userList.getUsers(), hasItem(user.getName()));

        // test role with no users
        assertTrue(restClient.getUsersByRole("foo").getUsers().isEmpty());
    }

    @Test
    public void testUpdate() throws Exception {
        testCreate();
        user.setRoles(asList("Contributor"));
        var actual = restClient.update(user);
        assertThat(actual.getRoles(),
                CombinableMatcher.<Iterable<String>>both(hasItem("Contributor")).and(not(hasItem("Admin"))));
    }

    @Test
    public void testChangePassword() throws Exception {
        final String emailAddress = "fbar@gmail.com";
        final String newPassword = "foobar";
        final String origPassword = user.getPassword();
        user.setEmail(emailAddress);

        // create user
        testCreate();

        // login as user
        restClient.login(user.getName(), user.getPassword());

        // update password
        user.setPassword(newPassword);
        var result = restClient.changePassword(user);

        assertNotNull(result);

        // make sure email address is ok
        var current = restClient.getCurrentUser();
        assertNotNull(current.getEmail());
        assertEquals(emailAddress, current.getEmail());

        restClient.login("Contributor", "demo");

        try {
            result = restClient.changePassword(user);
            fail("Expected error trying to change password");
        } catch (Exception e) {
            assertEquals(DataValidationRestClientException.class.getName(), e.getClass().getName());
        } finally {
            restClient.login("admin1", "demo");
        }
    }

    @Test
    public void testGetCurrentUser() throws Exception {
        var actual = restClient.getCurrentUser();
        assertThat(actual, is(notNullValue()));
    }

    @Test
    public void testCannotRemoveOwnAdminRole() throws Exception {
        var actual = restClient.getCurrentUser();
        assertThat(actual.getRoles(), hasItem("Admin"));

        var uesr = createUser(actual);
        uesr.getRoles().remove("Admin");
        assertThat(uesr.getRoles(), not(hasItem("Admin")));

        try {
            restClient.update(uesr);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("cannot.remove.own.admin.role"));
        }
    }

    private PSUser createUser(final PSCurrentUser currUser) {
        var user = new PSUser();
        user.setName(currUser.getName());
        user.setPassword(currUser.getPassword());
        user.setEmail(currUser.getEmail());
        user.setProviderType(currUser.getProviderType());
        user.setRoles(currUser.getRoles());
        return user;
    }

    /**
     * Add and remove Admin role for different user, not for current user.
     *
     * @throws Exception if error occurs.
     */
    @Test
    public void testAddRemoveAdminRole() throws Exception {
        testCreate();
        user.setRoles(asList("Admin", "Contributor"));
        var actual = restClient.update(user);
        assertThat(actual.getRoles(),
                CombinableMatcher.<Iterable<String>>both(hasItem("Contributor")).and(hasItem("Admin")));

        actual.setRoles(asList("Contributor"));
        actual = restClient.update(actual);

        assertThat(actual.getRoles(), hasItem("Contributor"));
        assertThat(actual.getRoles(), not(hasItem("Admin")));
    }

    @Test
    public void testGetAccessLevel() throws Exception {
        PSUser newUser = null;
        PSRole newRole = null;

        try {
            var req = new PSAccessLevelRequest();
            req.setType("percImageAutoList");

            restClient.login("Admin", "demo");
            req.setParentFolderPath("/Assets");

            var accessLevel = restClient.getAccessLevel(req);
            assertEquals(PSAssignmentTypeEnum.ADMIN.name(), accessLevel.getAccessLevel());

            restClient.login("Editor", "demo");

            accessLevel = restClient.getAccessLevel(req);
            assertEquals(PSAssignmentTypeEnum.ASSIGNEE.name(), accessLevel.getAccessLevel());

            restClient.login("Contributor", "demo");

            accessLevel = restClient.getAccessLevel(req);
            assertEquals(PSAssignmentTypeEnum.ASSIGNEE.name(), accessLevel.getAccessLevel());

            restClient.login("Admin", "demo");

            // test with new user in new role
            var r = new PSRole();
            r.setName("newRole");
            newRole = getRoleServiceRestClient().create(r);

            var u = new PSUser();
            u.setName("newUser");
            u.setPassword("demo");
            u.setRoles(Collections.singletonList("newRole"));
            newUser = getUserServiceRestClient().create(u);

            restClient.login("newUser", "demo");

            accessLevel = restClient.getAccessLevel(req);
            assertEquals(PSAssignmentTypeEnum.READER.name(), accessLevel.getAccessLevel());
        } finally {
            if (newRole != null) {
                var strWrapper = new PSStringWrapper();
                strWrapper.setValue(newRole.getName());

                getRoleServiceRestClient().delete(strWrapper);
            }

            if (newUser != null) {
                getUserServiceRestClient().delete(newUser.getName());
            }
        }
    }

    private final PSTestDataCleaner<String> userCleaner = new PSTestDataCleaner<>() {
        @Override
        protected void clean(String id) throws Exception {
            restClient.delete(id);
        }
    };

    private PSUser createUser(final String name, final String password, final List<String> roles) {
        var newUser = new PSUser();
        newUser.setName(name);
        newUser.setPassword(password);
        newUser.setRoles(roles);

        return newUser;
    }

    private PSUserServiceRestClient getUserServiceRestClient() throws Exception {
        var client = new PSUserServiceRestClient();
        client.setUrl(baseUrl);
        setupClient(client);
        return client;
    }

    private static PSRoleServiceRestClient getRoleServiceRestClient() throws Exception {
        var client = new PSRoleServiceRestClient();
        client.setUrl(baseUrl);
        setupClient(client);
        return client;
    }
}
