/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.user.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;

import javax.xml.bind.annotation.XmlRootElement;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@XmlRootElement(name = "CurrentUser")
@JsonRootName("CurrentUser")
public class PSCurrentUser extends PSUser
{
    private boolean accessibilityUser = false;
    private boolean adminUser = false;
    private boolean designerUser = false;
    private boolean navAdmin = false;
    private boolean userAdmin = false;
    
    public PSCurrentUser()
    {
        super();
    }
    
    public PSCurrentUser(PSUser user)
    {
        setName(user.getName());
        setPassword(user.getPassword());
        setEmail(user.getEmail());
        setProviderType(user.getProviderType());
        setRoles(user.getRoles());
    }
    
    public boolean isAccessibilityUser()
    {
        return accessibilityUser;
    }
    
    public void setAccessibilityUser(boolean isAccessibility)
    {
        accessibilityUser = isAccessibility;
    }
    
    public boolean isAdminUser()
    {
        return adminUser;
    }
    
    public void setAdminUser(boolean isAdmin)
    {
        adminUser = isAdmin;
    }

    /**
     * @param isDesigner
     */
    public void setDesignerUser(boolean isDesigner)
    {
        designerUser = isDesigner;
    }
    
    public boolean isDesignerUser()
    {
        return designerUser;
    }

    public boolean isNavAdmin() {
        return navAdmin;
    }

    public void setNavAdmin(boolean navAdmin) {
        this.navAdmin = navAdmin;
    }

    public boolean isUserAdmin() {
        return userAdmin;
    }

    public void setUserAdmin(boolean userAdmin) {
        this.userAdmin = userAdmin;
    }
}
