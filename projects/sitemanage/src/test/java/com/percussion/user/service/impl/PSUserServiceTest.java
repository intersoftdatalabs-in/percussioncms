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

package com.percussion.user.service.impl;

import com.percussion.role.service.IPSRoleService;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.test.PSServletTestCase;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.data.PSUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.service.IPSUtilityService;
import com.percussion.webservices.security.IPSSecurityWs;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test PSUserService API within server runtime environment.
 * // REFACTORED: CP-JAVA11
 * @author YuBingChen (modernized by Sunny Sal)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PSUserServiceTest extends PSServletTestCase {

    private IPSSecurityWs securityWs;
    private IPSUserService userService;
    private IPSSystemProperties systemProps;
    private IPSUtilityService utilityService;
    private final PSMockSystemProps mockProps = new PSMockSystemProps();

    public void setSecurityWs(IPSSecurityWs security) {
        this.securityWs = security;
    }

    public IPSSecurityWs getSecurityWs() {
        return securityWs;
    }

    public void setUtilityService(IPSUtilityService utilityService) {
        this.utilityService = utilityService;
    }

    public IPSUtilityService getUtilityService() {
        return this.utilityService;
    }

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        systemProps = ((PSUserService) userService).getSystemProps();
        setMockProperties(null);
        super.setUp();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        ((PSUserService) userService).setSystemProps(systemProps);
    }

    public void setUserService(IPSUserService service) {
        this.userService = service;
    }

    public IPSUserService getUserService() {
        return userService;
    }

    private void setMockProperties(String roles) {
        ((PSUserService) userService).setSystemProps(mockProps);
        if (StringUtils.isNotBlank(roles))
            mockProps.setAccessibilityRoles(roles);
    }

    @Test
    public void testEditorUser() throws Exception {
        securityWs.login("Editor", "demo", null, null);
        var user = userService.getCurrentUser();

        assertFalse(user.isAccessibilityUser());
        assertFalse(user.isAdminUser());

        setMockProperties("Editor");
        user = userService.getCurrentUser();
        assertTrue(user.isAccessibilityUser());
        assertFalse(user.isAdminUser());
    }

    @Test
    public void testPercussionAdmin() throws Exception {
        PSUser percussionAdmin = null;
        try {
            percussionAdmin = userService.find("PercussionAdmin");
        } catch (Exception e) {
            // User service throws an exception if there is no user with a supplied name
            // We catch the exception here and do nothing but the assertion will take care of both cases.
        }

        if (utilityService.isSaaSEnvironment()) {
            assertNotNull(percussionAdmin);
        } else {
            assertNull(percussionAdmin);
        }
    }

    @Test
    public void testAdminUser() throws Exception {
        securityWs.login("Admin", "demo", null, null);
        var user = userService.getCurrentUser();

        assertFalse(user.isAccessibilityUser());
        assertTrue(user.isAdminUser());

        setMockProperties("Editor,Admin");
        user = userService.getCurrentUser();
        assertTrue(user.isAccessibilityUser());
        assertTrue(user.isAdminUser());
    }

    @Test
    public void testDesignUser() throws Exception {
        var name = "UserServiceTestDesigner";
        var pwd = "demo";

        securityWs.login("Admin", "demo", null, null);

        // ensure user does not yet exist
        try {
            userService.delete(name);
        } catch (Exception e) {
            // ignore, user does not exist
        }

        var designUser = new PSUser();
        designUser.setName(name);
        designUser.setPassword(pwd);
        designUser.setRoles(Collections.singletonList(IPSRoleService.DESIGNER_ROLE));

        designUser = userService.create(designUser);
        assertNotNull(designUser);
        try {
            assertTrue(userService.isDesignUser(name));
            assertFalse(userService.isDesignUser("Admin"));
            assertFalse(userService.isDesignUser("Editor"));

            securityWs.login(name, pwd, null, null);
            var user = userService.getCurrentUser();

            assertFalse(user.isAdminUser());
            assertTrue(user.isDesignerUser());
        } finally {
            securityWs.login("Admin", "demo", null, null);
            userService.delete(name);
        }
    }

    private static class PSMockSystemProps extends Properties implements IPSSystemProperties {
        private static final long serialVersionUID = 1L;

        public void setAccessibilityRoles(String roles) {
            setProperty(ACCESSIBILITY_ROLES, roles);
        }
    }
}
