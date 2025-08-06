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
package com.percussion.webservices.transformation.converter;

import com.percussion.design.objectstore.PSRole;
import com.percussion.i18n.PSLocale;
import com.percussion.services.assembly.data.PSTemplateBinding;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.content.data.PSContentTypeSummary;
import com.percussion.services.content.data.PSContentTypeSummaryChild;
import com.percussion.services.content.data.PSFieldDescription;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.security.PSPermissions;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.services.security.data.PSLogin;
import com.percussion.services.security.data.PSUserAccessLevel;
import com.percussion.services.system.data.PSDependency;
import com.percussion.services.system.data.PSDependent;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.assembly.data.PSAssemblyTemplateBindingsBinding;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSConverter} class.
 */
@Tag(IntegrationTest.class)
public class PSConverterTest extends PSConverterTestBase {

    public void testPSLoginConversion() throws Exception {
        var source = new PSLogin();
        source.setSessionId("session");
        source.setSessionTimeout(100_000);
        source.setDefaultCommunity("defaultCommunity");
        source.setDefaultLocaleCode("defaultLocaleCode");

        var target = (PSLogin) roundTripConversion(PSLogin.class,
                com.percussion.webservices.security.data.PSLogin.class, source);

        assertEquals(source, target);

        var source2 = new PSLogin();
        source2.setSessionId("session");
        source2.setSessionTimeout(100_000);
        source2.setDefaultCommunity("defaultCommunity");
        source2.setDefaultLocaleCode("defaultLocaleCode");
        var communities = new ArrayList<PSCommunity>();
        communities.add(new PSCommunity("name", "description"));
        source2.setCommunities(communities);
        var roles = new ArrayList<PSRole>();
        roles.add(new PSRole("name"));
        source2.setRoles(roles);
        var locales = new ArrayList<PSLocale>();
        locales.add(new PSLocale("de-ch", "Swiss German",
                "German language used in Switzerland", PSLocale.STATUS_ACTIVE));
        source2.setLocales(locales);

        var target2 = (PSLogin) roundTripConversion(PSLogin.class,
                com.percussion.webservices.security.data.PSLogin.class, source2);

        assertEquals(source2, target2);
    }

    public void testPSTemplateBindingConversion() throws Exception {
        var source = new PSTemplateBinding(1, "variable", "expression");

        var target = (PSTemplateBinding) roundTripConversion(
                PSTemplateBinding.class,
                PSAssemblyTemplateBindingsBinding.class, source);

        assertEquals(source, target);
    }

    public void testPSObjectSummaryConversion() throws Exception {
        var src = new PSObjectSummary(new PSGuid(PSTypeEnum.NODEDEF, 1001),
                "article", "Article", null);

        var tgt = (PSObjectSummary) roundTripConversion(
                PSObjectSummary.class,
                com.percussion.webservices.common.PSObjectSummary.class, src);
        assertEquals(src, tgt);

        src.setLockedInfo("session", "locker", 1000);

        tgt = (PSObjectSummary) roundTripConversion(PSObjectSummary.class,
                com.percussion.webservices.common.PSObjectSummary.class, src);
        assertEquals(src, tgt);

        var permset = new HashSet<PSPermissions>();
        var accessLevel = new PSUserAccessLevel(permset);
        src.setPermissions(accessLevel);

        tgt = (PSObjectSummary) roundTripConversion(PSObjectSummary.class,
                com.percussion.webservices.common.PSObjectSummary.class, src);
        assertEquals(src, tgt);

        for (var permission : PSPermissions.values()) {
            accessLevel.getPermissions().add(permission);
        }

        tgt = (PSObjectSummary) roundTripConversion(PSObjectSummary.class,
                com.percussion.webservices.common.PSObjectSummary.class, src);
        assertEquals(src, tgt);
    }

    @SuppressWarnings("unchecked")
    public void testPSContentTypeSummaryConversion() throws Exception {
        var src = new PSContentTypeSummary();
        src.setGuid(new PSGuid(PSTypeEnum.NODEDEF, 301));
        src.setName("Article");
        src.setDescription("a content type");

        src.addField(new PSFieldDescription("fld1",
                PSFieldDescription.PSFieldTypeEnum.NUMBER.name()));
        src.addField(new PSFieldDescription("fld2",
                PSFieldDescription.PSFieldTypeEnum.TEXT.name()));

        PSContentTypeSummaryChild child;
        child = new PSContentTypeSummaryChild("child1");
        child.addField(new PSFieldDescription("fld3",
                PSFieldDescription.PSFieldTypeEnum.NUMBER.name()));
        child.addField(new PSFieldDescription("fld4",
                PSFieldDescription.PSFieldTypeEnum.DATE.name()));
        child = new PSContentTypeSummaryChild("child2");
        child.addField(new PSFieldDescription("fld5",
                PSFieldDescription.PSFieldTypeEnum.NUMBER.name()));
        src.addChild(child);

        var tgt = (PSContentTypeSummary) roundTripConversion(
                PSContentTypeSummary.class,
                com.percussion.webservices.content.PSContentTypeSummary.class,
                src);

        assertEquals(src, tgt);

        var src2 = new PSContentTypeSummary();
        src2.setGuid(new PSGuid(PSTypeEnum.NODEDEF, 302));
        src2.setName("name2");
        src2.setDescription("another content type");

        src2.addField(new PSFieldDescription("fld21",
                PSFieldDescription.PSFieldTypeEnum.NUMBER.name()));
        src2.addField(new PSFieldDescription("fld22",
                PSFieldDescription.PSFieldTypeEnum.TEXT.name()));
        var sums = new ArrayList<PSContentTypeSummary>(2);
        sums.add(src);
        sums.add(src2);

        var tgtsums = roundTripListConversion(
                com.percussion.webservices.content.PSContentTypeSummary[].class,
                sums);
        // No assertion needed; roundTripListConversion will throw if fails
    }

    public void testPSDependencyConversion() throws Exception {
        var child = new PSDependent();
        child.setId(111);
        child.setType("type1");
        var convChild = (PSDependent) roundTripConversion(
                PSDependent.class,
                com.percussion.webservices.system.PSDependent.class, child);
        assertEquals(child, convChild);

        var dep = new PSDependency();
        dep.setId(1234);
        dep.addDependent(child);

        var convDep = (PSDependency) roundTripConversion(
                PSDependency.class,
                com.percussion.webservices.system.PSDependency.class, dep);
        assertEquals(dep, convDep);

        child = new PSDependent();
        child.setId(112);
        child.setType("type2");
        dep.addDependent(child);
        var convDep2 = (PSDependency) roundTripConversion(
                PSDependency.class,
                com.percussion.webservices.system.PSDependency.class, dep);
        assertEquals(dep, convDep2);
        assertNotEquals(convDep, convDep2);
        assertNotEquals(dep, convDep);

        var srcList = new ArrayList<PSDependency>(2);
        srcList.add(dep);
        srcList.add(convDep);
        srcList.add(convDep2);
        var tgtList = roundTripListConversion(
                com.percussion.webservices.system.PSDependency[].class, srcList);
        assertEquals(srcList, tgtList);
    }
}
