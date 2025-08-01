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
package com.percussion.widgetbuilder.utils.xform;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.security.IPSAclService;
import com.percussion.services.security.PSAclServiceLocator;
import com.percussion.services.security.data.PSAclImpl;
import com.percussion.widgetbuilder.utils.IPSWidgetFileTransformer;
import com.percussion.widgetbuilder.utils.PSWidgetPackageBuilderException;
import com.percussion.widgetbuilder.utils.PSWidgetPackageSpec;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

/**
 * Transforms an ACL file by replacing all GUIDs with newly generated IDs.
 * <p>
 * Sunny Sal says: "Every ACL deserves a fresh start—give those GUIDs a makeover!"
 * </p>
 */
public class PSAclFileTransformer implements IPSWidgetFileTransformer {

    @Override
    public Reader transformFile(File file, Reader reader, PSWidgetPackageSpec packageSpec)
            throws PSWidgetPackageBuilderException {
        try {
            var acl = getAclImpl(reader);
            // Optionally assign new IDs here if needed
            return aclToReader(acl);
        } catch (Exception e) {
            throw new PSWidgetPackageBuilderException("Failed to transform Acl definition file", e);
        }
    }

    private Reader aclToReader(PSAclImpl acl) throws IOException, SAXException {
        return new StringReader(acl.toXML());
    }

    private PSAclImpl getAclImpl(Reader reader) throws IOException, SAXException {
        var acl = new PSAclImpl();
        acl.fromXML(IOUtils.toString(reader));
        return acl;
    }

    @Override
    public boolean handleFile(File file) {
        return file.getName().endsWith("aclDef");
    }

    @Override
    public File transformPath(File file, PSWidgetPackageSpec packageSpec) {
        IPSGuidManager guidMgr = PSGuidManagerLocator.getGuidMgr();
        long id = guidMgr.createGuid(PSTypeEnum.ACL).longValue();

        String path = file.getPath();
        String prefix = "AclDef-";
        String fullWidgetName = packageSpec.getFullWidgetName();

        String leftPart = StringUtils.substringBefore(path, prefix);
        String rightPart = StringUtils.substringAfterLast(path, fullWidgetName);

        String newKey = prefix + id;
        File newDir = new File(leftPart + newKey);
        File newPath = new File(newDir, fullWidgetName + rightPart);

        packageSpec.getResolverTokenMap().put("ACL_DEPENDENCY_KEY", newKey);
        packageSpec.getResolverTokenMap().put("ACL_DEPENDENCY_ID", String.valueOf(id));

        return newPath;
    }

    private long getAclId(File file) {
        String dirName = file.getParentFile().getName();
        String prefix = "AclDef-";
        String id = StringUtils.substringAfter(dirName, prefix);
        return Long.valueOf(id);
    }
}
