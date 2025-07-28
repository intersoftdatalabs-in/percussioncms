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

// REFACTORED: CP-JAVA11

package com.percussion.apibridge;

import com.percussion.rest.errors.BackendException;
import com.percussion.rest.roles.IRoleAdaptor;
import com.percussion.rest.roles.Role;
import com.percussion.role.data.PSRole;
import com.percussion.role.service.impl.PSRoleService;
import com.percussion.share.data.PSStringWrapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.util.PSSiteManageBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import javax.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adaptor for managing roles in Percussion CMS.
 */
@PSSiteManageBean
@Lazy
public class RoleAdaptor implements IRoleAdaptor {

    @Autowired
    private PSRoleService roleService;

    @Override
    public Role getRole(URI baseURI, String roleName) throws BackendException {
        try {
            var wrap = new PSStringWrapper();
            wrap.setValue(roleName);
            var pRole = roleService.find(wrap);
            return ApiUtils.convertRole(pRole);
        } catch (PSDataServiceException e) {
            throw new BackendException(e);
        }
    }

    @Override
    public Role updateRole(URI baseURI, Role role) {
        try {
            return ApiUtils.convertRole(roleService.update(ApiUtils.convertRole(role)));
        } catch (PSDataServiceException e) {
            throw new WebApplicationException(e);
        }
    }

    @Override
    public Role createRole(URI baseURI, Role role) throws BackendException {
        try {
            return ApiUtils.convertRole(roleService.create(ApiUtils.convertRole(role)));
        } catch (PSDataServiceException e) {
            throw new BackendException(e);
        }
    }

    @Override
    public void deleteRole(URI baseURI, String roleName) throws BackendException {
        try {
            var wrap = new PSStringWrapper(roleName);
            roleService.delete(wrap);
        } catch (PSDataServiceException e) {
            throw new BackendException(e);
        }
    }

    @Override
    public List<Role> findRoles(URI baseURI, String pattern) throws BackendException {
        try {
            var roleList = roleService.getRoleMgr().getDefinedRoles();
            return roleList.stream()
                    .map(s -> ApiUtils.convertRole(roleService.find(new PSStringWrapper(s))))
                    .collect(Collectors.toList());
        } catch (PSDataServiceException e) {
            throw new BackendException(e);
        }
    }

    public RoleAdaptor() {
        // No-op constructor for dependency injection.
    }
}
