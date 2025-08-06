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
package com.percussion.role.web.service;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.role.data.PSRole;
import com.percussion.role.service.IPSRoleService;
import com.percussion.role.service.impl.PSRoleService;
import com.percussion.share.data.PSStringWrapper;
import com.percussion.share.test.PSRestTestCase;
import com.percussion.share.test.PSTestDataCleaner;
import com.percussion.user.data.PSUser;
import com.percussion.user.data.PSUserList;
import com.percussion.user.web.service.PSUserServiceRestClient;
import org.hamcrest.core.CombinableMatcher;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test the role service through REST.
 * Sunny Sal: "Role REST test, Java 11 and JUnit 5, abhi picture baaki hai!"
 */
class PSRoleServiceRestTest extends PSRestTestCase<PSRoleServiceRestClient> {

    protected PSRole role;

    @Override
    protected PSRoleServiceRestClient getRestClient(String baseUrl) {
        var c = new PSRoleServiceRestClient();
        c.setUrl(baseUrl);
        return c;
    }

    private PSUserServiceRestClient getUserServiceRestClient() throws Exception {
        var c = new PSUserServiceRestClient();
        c.setUrl(baseUrl);
        setupClient(c);
        return c;
    }

    @BeforeEach
    void setupRole() {
        role = createRole("MyTestRole", "MyDescription", "Dashboard");
    }

    @AfterEach
    void runCleaner() {
        roleCleaner.clean();
    }

    @Test
    void testCreate() throws Exception {
        roleCleaner.add(role.getName());
        restClient.create(role);

        var strWrapper = new PSStringWrapper();
        strWrapper.setValue(role.getName());
        assertEquals(role, restClient.find(strWrapper));

        var roleWithUser = createRole("MyTestRoleWithUser", "Role with user", "Dashboard");
        roleWithUser.getUsers().add("Admin");
        roleCleaner.add(roleWithUser.getName());
        restClient.create(roleWithUser);

        strWrapper.setValue(roleWithUser.getName());
        assertEquals(roleWithUser, restClient.find(strWrapper));
    }

    @Test
    void testDelete() throws Exception {
        testCreate();

        // Add Contributor user to role
        var strWrapper = new PSStringWrapper();
        strWrapper.setValue(role.getName());
        var existingRole = restClient.find(strWrapper);
        existingRole.getUsers().add("Contributor");
        restClient.update(existingRole);

        restClient.delete(strWrapper);
        // If successful the cleaner does not need to remove the user.
        roleCleaner.remove(role.getName());

        Exception exception = null;
        try {
            restClient.find(strWrapper);
            fail("Role " + role.getName() + " should not exist");
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);

        // Make sure Contributor user is no longer in role
        var user = getUserServiceRestClient().find("Contributor");
        assertFalse(user.getRoles().contains(role.getName()));
    }

    @Test
    void testDeleteSystemRolesShouldFail() throws Exception {
        var strWrapper = new PSStringWrapper();

        for (var sysRole : PSRoleService.SYSTEM_ROLES) {
            Exception exception = null;
            try {
                strWrapper.setValue(sysRole);
                restClient.delete(strWrapper);
                fail("Should not be able to delete role '" + sysRole + "'");
            } catch (Exception e) {
                exception = e;
            }
            assertNotNull(exception);
        }
    }

    @Test
    void testFind() throws Exception {
        testCreate();
        var strWrapper = new PSStringWrapper();
        strWrapper.setValue(role.getName());
        var actual = restClient.find(strWrapper);
        assertThat(actual.getName(), is(equalTo(role.getName())));
        assertThat(actual.getDescription(), is(equalTo(role.getDescription())));
    }

    @Test
    void testCannotRemoveSelfFromAdminRole() throws Exception {
        var actual = getUserServiceRestClient().getCurrentUser();
        assertThat(actual.getRoles(), hasItem(IPSRoleService.ADMINISTRATOR_ROLE));

        var strWrapper = new PSStringWrapper();
        strWrapper.setValue(IPSRoleService.ADMINISTRATOR_ROLE);

        var adminRole = restClient.find(strWrapper);
        adminRole.getUsers().remove(actual.getName());
        assertThat(adminRole.getUsers(), not(hasItem(actual.getName())));

        Exception exception = null;
        try {
            restClient.update(adminRole);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("cannot.remove.user.admin.role"));
    }

    /**
     * Add and remove Admin role for different user, not for current user.
     */
    @Test
    void testAddRemoveUserFromAdminRole() throws Exception {
        testCreate();
        role.setUsers(asList("Admin", "Contributor"));
        var actual = restClient.update(role);
        assertThat(actual.getUsers(), CombinableMatcher.<Iterable<String>>both(hasItem("Contributor")).and(hasItem("Admin")));

        actual.setUsers(asList("Contributor"));
        actual = restClient.update(actual);

        assertThat(actual.getUsers(), hasItem("Contributor"));
        assertThat(actual.getUsers(), not(hasItem("Admin")));
    }

    @Test
    void testGetAvailableUsers() throws Exception {
        testCreate();

        var roleUsers = getUserServiceRestClient().getUsersByRole(role.getName());
        assertTrue(roleUsers.getUsers().isEmpty());

        var users = getUserServiceRestClient().getUsers();
        assertTrue(users.getUsers().contains("Editor"));
        assertTrue(users.getUsers().contains("Contributor"));
        assertEquals(users, restClient.getAvailableUsers(role));

        role.setUsers(asList("Editor", "Contributor"));
        restClient.update(role);

        var availableUsers = restClient.getAvailableUsers(role);
        assertEquals(users.getUsers().size() - 2, availableUsers.getUsers().size());
        assertFalse(availableUsers.getUsers().contains("Editor"));
        assertFalse(availableUsers.getUsers().contains("Contributor"));
    }

    @Test
    void testUpdateUsers() throws Exception {
        testCreate();

        var roleUsers = getUserServiceRestClient().getUsersByRole(role.getName());
        assertTrue(roleUsers.getUsers().isEmpty());

        var editor = getUserServiceRestClient().find("Editor");
        assertFalse(editor.getRoles().contains(role.getName()));

        var contributor = getUserServiceRestClient().find("Contributor");
        assertFalse(contributor.getRoles().contains(role.getName()));

        role.setUsers(asList("Editor", "Contributor"));
        var roleWithUsers = restClient.update(role);

        assertEquals(2, roleWithUsers.getUsers().size());
        assertTrue(roleWithUsers.getUsers().contains("Editor"));
        assertTrue(roleWithUsers.getUsers().contains("Contributor"));

        editor = getUserServiceRestClient().find("Editor");
        assertTrue(editor.getRoles().contains(role.getName()));

        contributor = getUserServiceRestClient().find("Contributor");
        assertTrue(contributor.getRoles().contains(role.getName()));

        role.setUsers(new ArrayList<>());
        var roleNoUsers = restClient.update(role);

        assertTrue(roleNoUsers.getUsers().isEmpty());

        editor = getUserServiceRestClient().find("Editor");
        assertFalse(editor.getRoles().contains(role.getName()));

        contributor = getUserServiceRestClient().find("Contributor");
        assertFalse(contributor.getRoles().contains(role.getName()));
    }

    @Test
    void testRoleHomepage() throws Exception {
        // Make sure Admin homepage is Dashboard
        var temp = new PSStringWrapper();
        temp.setValue("Admin");
        var adminRole = restClient.find(temp);
        assertEquals(adminRole.getHomepage(), IPSRoleService.HOMEPAGE_TYPE_DASHBOARD);
        // Update the homepage
        adminRole.setHomepage(IPSRoleService.HOMEPAGE_TYPE_EDITOR);
        restClient.update(adminRole);
        adminRole = restClient.find(temp);
        assertEquals(adminRole.getHomepage(), IPSRoleService.HOMEPAGE_TYPE_EDITOR);
        // Reset the homepage for Admin
        adminRole.setHomepage(IPSRoleService.HOMEPAGE_TYPE_DASHBOARD);
        restClient.update(adminRole);

        // Test a new role
        var role = createRole("HomepageRole", "To test home page", IPSRoleService.HOMEPAGE_TYPE_EDITOR);
        roleCleaner.add(role.getName());
        restClient.create(role);
        temp.setValue(role.getName());
        var testRole = restClient.find(temp);
        assertEquals(testRole.getHomepage(), IPSRoleService.HOMEPAGE_TYPE_EDITOR);

        // Test a new role with home as homepage
        var role1 = createRole("HomepageRole1", "To test home page", IPSRoleService.HOMEPAGE_TYPE_HOME);
        roleCleaner.add(role1.getName());
        restClient.create(role1);
        temp.setValue(role1.getName());
        var testRole1 = restClient.find(temp);
        assertEquals(testRole1.getHomepage(), IPSRoleService.HOMEPAGE_TYPE_HOME);

        // Test invalid homepage case
        role.setHomepage("InvalidHomePage");
        restClient.update(role);
        temp.setValue(role.getName());
        testRole = restClient.find(temp);
        assertEquals(testRole.getHomepage(), IPSRoleService.HOMEPAGE_TYPE_DASHBOARD);
    }

    @Test
    void testUpdate() throws Exception {
        var intialDescription = "Role with description";
        var changedDescription = "Role description is changed";
        var roleWithDescription = createRole("MyTestRole", intialDescription);

        roleCleaner.add(roleWithDescription.getName());
        var created = restClient.create(roleWithDescription);

        assertEquals(created.getDescription(), intialDescription);

        roleWithDescription.setDescription(changedDescription);

        var role = restClient.update(roleWithDescription);

        assertEquals(role.getDescription(), changedDescription);

        assertNotEquals(role.getDescription(), intialDescription);
    }

    @Test
    void testValidateForDelete() throws Exception {
        PSUser user = null;
        try {
            testCreate();

            var strWrapper = new PSStringWrapper();
            strWrapper.setValue(role.getName());

            try {
                restClient.validateForDelete(restClient.find(strWrapper));
            } catch (Exception e) {
                fail("Unexpected warning");
            }

            // Create a new user and add it to the role
            user = new PSUser();
            user.setName("MyTestUser");
            user.setRoles(Collections.singletonList(role.getName()));
            user = getUserServiceRestClient().create(user);

            try {
                restClient.validateForDelete(restClient.find(strWrapper));
            } catch (Exception e) {
                fail("Unexpected warning");
            }

            user.getRoles().add("Contributor");
            user = getUserServiceRestClient().update(user);

            try {
                restClient.validateForDelete(restClient.find(strWrapper));
            } catch (Exception e) {
                fail("Unexpected warning");
            }

            var contributor = new PSRole();
            contributor.setName("Contributor");

            var contribWrapper = new PSStringWrapper();
            contribWrapper.setValue(contributor.getName());

            try {
                restClient.validateForDelete(restClient.find(contribWrapper));
            } catch (Exception e) {
                // warning expected for workflow, not user
                assertTrue(e.getMessage().contains("Default Workflow"));
                assertFalse(e.getMessage().contains(user.getName()));
            }

            user.getRoles().remove(role.getName());
            user = getUserServiceRestClient().update(user);

            try {
                restClient.validateForDelete(restClient.find(contribWrapper));
            } catch (Exception e) {
                // warning not expected for user, expected for workflow
                assertFalse(e.getMessage().contains(user.getName()));
                assertTrue(e.getMessage().contains("Default Workflow"));
            }
        } finally {
            if (user != null) {
                getUserServiceRestClient().delete(user.getName());
            }
        }
    }

    @Test
    void testValidateDeleteUsersFromRole() throws Exception {
        var userNames = new ArrayList<String>();
        try {
            var user1 = new PSUser();
            user1.setName("MyTestUser1");
            user1.setRoles(Collections.singletonList("Editor"));
            user1 = getUserServiceRestClient().create(user1);
            userNames.add(user1.getName());

            var user2 = new PSUser();
            user2.setName("MyTestUser2");
            user2.setRoles(Collections.singletonList("Editor"));
            user2 = getUserServiceRestClient().create(user2);
            userNames.add(user2.getName());

            var user3 = new PSUser();
            user3.setName("MyTestUser3");
            user3.setRoles(asList("Editor", "Contributor"));
            user3 = getUserServiceRestClient().create(user3);
            userNames.add(user3.getName());

            var userList = new PSUserList();
            userList.setUsers(Collections.singletonList(user3.getName()));

            try {
                restClient.validateDeleteUsersFromRole(userList);
            } catch (Exception e) {
                fail("Unexpected warning");
            }

            userList.setUsers(userNames);

            try {
                restClient.validateDeleteUsersFromRole(userList);
            } catch (Exception e) {
                fail("Unexpected warning");
            }
        } finally {
            for (var userName : userNames) {
                getUserServiceRestClient().delete(userName);
            }
        }
    }

    private final PSTestDataCleaner<String> roleCleaner = new PSTestDataCleaner<>() {
        @Override
        protected void clean(String id) throws Exception {
            var strWrapper = new PSStringWrapper();
            strWrapper.setValue(id);
            restClient.delete(strWrapper);
        }
    };

    private PSRole createRole(String name, String description) {
        return createRole(name, description, null);
    }

    private PSRole createRole(String name, String description, String homepage) {
        var newRole = new PSRole();
        newRole.setName(name);
        newRole.setDescription(description);
        newRole.setHomepage(homepage);
        return newRole;
    }
}
