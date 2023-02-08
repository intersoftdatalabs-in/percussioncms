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

package com.ibm.cadf.model;

import com.ibm.cadf.Messages;
import com.ibm.cadf.exception.CADFException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.List;

public class FederatedCredential extends Credential
{

    private static final long serialVersionUID = 1L;

    private String identity_provider;

    private String user;

    private List<String> groups;

    public FederatedCredential(String type, String token, String identity_provider,
                    String user, List<String> groups) throws CADFException
    {
        super(token);
        this.identity_provider = identity_provider;
        this.user = user;
        this.groups = groups;
    }

    public String getIdentity_provider()
    {
        return identity_provider;
    }

    public void setIdentity_provider(String identity_provider)
    {
        this.identity_provider = identity_provider;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public List<String> getGroups()
    {
        return groups;
    }

    public void setGroups(List<String> groups)
    {
        this.groups = groups;
    }

    @Override
    public boolean isValid() throws CADFException
    {
        boolean missingMandatoryField = false;
        StringBuilder missingMadatoryFields = new StringBuilder();
        if (StringUtils.isEmpty(identity_provider))
        {
            missingMandatoryField = true;
            missingMadatoryFields.append("identity_provider");
        }

        if (StringUtils.isEmpty(user))
        {
            if (missingMandatoryField)
            {
                missingMadatoryFields.append(",");
            }
            else
            {
                missingMandatoryField = true;
            }
            missingMadatoryFields.append("user");
        }

        if (CollectionUtils.isEmpty(groups))
        {
            if (missingMandatoryField)
            {
                missingMadatoryFields.append(",");
            }
            else
            {
                missingMandatoryField = true;
            }
            missingMadatoryFields.append("groups");
        }

        // Validation to ensure FederatedCredential required attributes are set.
        if (!missingMandatoryField)
            return true;
        else
            throw new CADFException(MessageFormat.format(Messages.MISSING_MANDATORY_FIELDS,
                                                         missingMadatoryFields.toString()));
    }

}
