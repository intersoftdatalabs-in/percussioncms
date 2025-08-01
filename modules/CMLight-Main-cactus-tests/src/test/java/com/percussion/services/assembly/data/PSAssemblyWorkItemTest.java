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
package com.percussion.services.assembly.data;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.services.assembly.IPSAssemblyErrors;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.filter.IPSFilterItem;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.filter.IPSItemFilterRuleDef;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IPSReflectionFilter;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.utils.testing.PSReflectionHelper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test assembly work items.
 */
@Tag("IntegrationTest")
public class PSAssemblyWorkItemTest {
    private static final byte[] REFARRAY = {1, 2, 3, 4, 5, 6, 7};
    private static final byte[] BIGRESULT = new byte[100000];

    static {
        var rand = new Random();
        rand.nextBytes(BIGRESULT);
    }

    private static final IPSGuidManager msGuidMgr = PSGuidManagerLocator.getGuidMgr();

    void setupWorkItemData(PSAssemblyWorkItem item, boolean bigresult, boolean usestream) throws Exception {
        item.setBindings(new HashMap<>());
        item.setId(new PSGuid(PSTypeEnum.ITEM, 1));
        item.setJobId(-1);
        item.setMimeType("text/html");
        item.setParameters(new HashMap<>());
        item.setPath("/foo/bar");
        item.setReferenceId(-1);
        if (usestream) {
            item.setResultStream(new ByteArrayInputStream(bigresult ? BIGRESULT : REFARRAY));
        } else {
            item.setResultData(bigresult ? BIGRESULT : REFARRAY);
        }
        item.setFilter(new IPSItemFilter() {
            @Override
            public List<IPSFilterItem> filter(List<IPSFilterItem> ids, Map<String, String> params) {
                return null;
            }
            @Override public String getName() { return "test"; }
            @Override public void setName(String name) {}
            @Override public String getDescription() { return "dummy"; }
            @Override public void setDescription(String description) {}
            @Override public Integer getLegacyAuthtypeId() { return null; }
            @Override public void setLegacyAuthtypeId(Integer authTypeId) {}
            @Override public Set<IPSItemFilterRuleDef> getRuleDefs() { return null; }
            @Override public void setRuleDefs(Set<IPSItemFilterRuleDef> rules) {}
            @Override public void addRuleDef(IPSItemFilterRuleDef def) {}
            @Override public void removeRuleDef(IPSItemFilterRuleDef def) {}
            @Override public String toXML() { return null; }
            @Override public void fromXML(String xmlsource) {}
            @Override public IPSGuid getGUID() { return null; }
            @Override public void setGUID(IPSGuid newguid) {}
            @Override public IPSItemFilter getParentFilter() { return null; }
            @Override public void setParentFilter(IPSItemFilter parentFilter) {}
        });
    }

    @Test
    public void testResultData() throws Exception {
        var item = new PSAssemblyWorkItem();
        var bos = new ByteArrayOutputStream();
        setupWorkItemData(item, false, false);
        var data = item.getResultData();
        assertEqualArrays(REFARRAY, data);
        setupWorkItemData(item, false, false);
        var is = item.getResultStream();
        IOUtils.copy(is, bos);
        assertEqualArrays(REFARRAY, bos.toByteArray());
        item.clearResults();

        bos.reset();
        setupWorkItemData(item, true, false);
        data = item.getResultData();
        assertEqualArrays(BIGRESULT, data);
        setupWorkItemData(item, true, false);
        is = item.getResultStream();
        IOUtils.copy(is, bos);
        assertEqualArrays(BIGRESULT, bos.toByteArray());
        item.clearResults();

        bos.reset();
        setupWorkItemData(item, false, true);
        data = item.getResultData();
        assertEqualArrays(REFARRAY, data);
        setupWorkItemData(item, false, true);
        is = item.getResultStream();
        IOUtils.copy(is, bos);
        assertEqualArrays(REFARRAY, bos.toByteArray());
        item.clearResults();

        bos.reset();
        setupWorkItemData(item, true, true);
        data = item.getResultData();
        assertEqualArrays(BIGRESULT, data);
        setupWorkItemData(item, true, true);
        is = item.getResultStream();
        IOUtils.copy(is, bos);
        assertEqualArrays(BIGRESULT, bos.toByteArray());
        item.clearResults();
    }

    @Test
    public void testObjectMethods() throws Exception {
        IPSReflectionFilter filter = methodname ->
                !methodname.contains("Node")
                        && !methodname.contains("ResultData")
                        && !methodname.contains("NavHelper")
                        && !methodname.contains("SiteId")
                        && !methodname.contains("Status")
                        && !methodname.contains("Stream");

        var first = new PSAssemblyWorkItem();
        var second = new PSAssemblyWorkItem();
        setupWorkItemData(first, false, false);
        setupWorkItemData(second, false, false);
        first.getBindings().put("a", "b");
        second.getBindings().put("a", "c");
        first.getParameters().put("test1", new String[]{"testval"});
        PSReflectionHelper.testClone(first, second, filter);

        second = (PSAssemblyWorkItem) first.clone();
        PSReflectionHelper.testEquals(first, second, filter);
    }

    private static final String TEST_ITEM_PATH = "//Sites/EnterpriseInvestments/ProductsAndServices"
            + "/Products and Services HomePage Image (News and Glasses).jpg";
    private static final int TEST_ITEM_CONTENTID = 477;
    private static final int TEST_ITEM_REVISION = 1;
    private static final int TEST_ITEM_FOLDERID = 309;
    private static final String TEST_ITEM_PSEUDO_PATH = "/" + TEST_ITEM_CONTENTID + "#" + TEST_ITEM_REVISION;

    @Test
    public void testNormalizeWithNothing() throws Exception {
        var asm = PSAssemblyServiceLocator.getAssemblyService();
        var item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        Exception thrown = assertThrows(PSAssemblyException.class, item::normalize);
        assertEquals(IPSAssemblyErrors.PARAMS_ITEM_SPEC, ((PSAssemblyException) thrown).getErrorCode());
        assertNotNull(thrown.getMessage());
    }

    @Test
    public void testNormalizeWithIdAndFolder() throws Exception {
        var asm = PSAssemblyServiceLocator.getAssemblyService();
        var item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        item.setId(msGuidMgr.makeGuid(new PSLocator(TEST_ITEM_CONTENTID, TEST_ITEM_REVISION)));
        item.setFolderId(TEST_ITEM_FOLDERID);
        item.normalize();

        assertParameterEquals(TEST_ITEM_CONTENTID, item, IPSHtmlParameters.SYS_CONTENTID);
        assertParameterEquals(TEST_ITEM_REVISION, item, IPSHtmlParameters.SYS_REVISION);
        assertParameterEquals(TEST_ITEM_FOLDERID, item, IPSHtmlParameters.SYS_FOLDERID);

        assertNotNull(item.getPath());
        assertEquals(TEST_ITEM_PATH, item.getPath());
    }

    @Test
    public void testNormalizeWithIdOnly() throws Exception {
        var asm = PSAssemblyServiceLocator.getAssemblyService();
        var item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        item.setId(msGuidMgr.makeGuid(new PSLocator(TEST_ITEM_CONTENTID, TEST_ITEM_REVISION)));
        item.normalize();

        assertParameterEquals(TEST_ITEM_CONTENTID, item, IPSHtmlParameters.SYS_CONTENTID);
        assertParameterEquals(TEST_ITEM_REVISION, item, IPSHtmlParameters.SYS_REVISION);
        assertNull(item.getParameterValue(IPSHtmlParameters.SYS_FOLDERID, null));
        assertNotNull(item.getPath());
        assertEquals(TEST_ITEM_PSEUDO_PATH, item.getPath());
    }

    @Test
    public void testNormalizeWithMismatchId() throws Exception {
        var asm = PSAssemblyServiceLocator.getAssemblyService();
        var params = new HashMap<String, String[]>();
        params.put("sys_contentid", new String[]{"999"});
        params.put("sys_revision", new String[]{Integer.toString(TEST_ITEM_REVISION)});
        var item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        item.setParameters(params);
        item.setId(msGuidMgr.makeGuid(new PSLocator(TEST_ITEM_CONTENTID, TEST_ITEM_REVISION)));
        Exception thrown = assertThrows(PSAssemblyException.class, item::normalize);
        assertEquals(IPSAssemblyErrors.PARAMS_ITEM_ID_MISMATCH, ((PSAssemblyException) thrown).getErrorCode());
        assertNotNull(thrown.getMessage());
    }

    @Test
    public void testNormalizeWithMismatchFolder() throws Exception {
        var asm = PSAssemblyServiceLocator.getAssemblyService();
        var params = new HashMap<String, String[]>();
        params.put("sys_contentid", new String[]{Integer.toString(TEST_ITEM_CONTENTID)});
        params.put("sys_revision", new String[]{Integer.toString(TEST_ITEM_REVISION)});
        params.put("sys_folderid", new String[]{Integer.toString(TEST_ITEM_FOLDERID)});
        var item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        item.setParameters(params);
        item.setId(msGuidMgr.makeGuid(new PSLocator(TEST_ITEM_CONTENTID, TEST_ITEM_REVISION)));
        item.setFolderId(999);
        Exception thrown = assertThrows(PSAssemblyException.class, item::normalize);
        assertEquals(IPSAssemblyErrors.PARAMS_ITEM_FOLDER_MISMATCH, ((PSAssemblyException) thrown).getErrorCode());
        assertNotNull(thrown.getMessage());
    }

    @Test
    public void testNormalizeWithParams() throws Exception {
        var asm = PSAssemblyServiceLocator.getAssemblyService();
        var params = new HashMap<String, String[]>();
        params.put("sys_contentid", new String[]{Integer.toString(TEST_ITEM_CONTENTID)});
        params.put("sys_revision", new String[]{Integer.toString(TEST_ITEM_REVISION)});
        params.put("sys_folderid", new String[]{Integer.toString(TEST_ITEM_FOLDERID)});
        var item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        item.setParameters(params);
        item.normalize();

        assertParameterEquals(TEST_ITEM_CONTENTID, item, IPSHtmlParameters.SYS_CONTENTID);
        assertParameterEquals(TEST_ITEM_REVISION, item, IPSHtmlParameters.SYS_REVISION);
        assertParameterEquals(TEST_ITEM_FOLDERID, item, IPSHtmlParameters.SYS_FOLDERID);

        assertNotNull(item.getId());
        var loc = msGuidMgr.makeLocator(item.getId());
        assertEquals(TEST_ITEM_CONTENTID, loc.getId());
        assertEquals(TEST_ITEM_REVISION, loc.getRevision());

        assertNotNull(item.getPath());
        assertEquals(TEST_ITEM_PATH, item.getPath());
    }

    @Test
    public void testNormalizeWithParamsNoFolder() throws Exception {
        var asm = PSAssemblyServiceLocator.getAssemblyService();
        var params = new HashMap<String, String[]>();
        params.put("sys_contentid", new String[]{Integer.toString(TEST_ITEM_CONTENTID)});
        params.put("sys_revision", new String[]{Integer.toString(TEST_ITEM_REVISION)});
        var item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        item.setParameters(params);
        item.normalize();

        assertParameterEquals(TEST_ITEM_CONTENTID, item, IPSHtmlParameters.SYS_CONTENTID);
        assertParameterEquals(TEST_ITEM_REVISION, item, IPSHtmlParameters.SYS_REVISION);
        assertNull(item.getParameterValue(IPSHtmlParameters.SYS_FOLDERID, null));

        assertNotNull(item.getId());
        var loc = msGuidMgr.makeLocator(item.getId());
        assertEquals(TEST_ITEM_CONTENTID, loc.getId());
        assertEquals(TEST_ITEM_REVISION, loc.getRevision());
        assertEquals(0, item.getFolderId());

        assertNotNull(item.getPath());
        assertEquals(TEST_ITEM_PSEUDO_PATH, item.getPath());
    }

    private void assertParameterEquals(int expectedValue, PSAssemblyWorkItem item, String parameterName) {
        assertEquals(Integer.toString(expectedValue), item.getParameterValue(parameterName, null));
    }

    @Test
    public void testNormalizeWithMissingPath() throws Exception {
        var asm = PSAssemblyServiceLocator.getAssemblyService();
        var item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        item.setPath("//Sites/EnterpriseInvestments/Prod/item");
        Exception thrown = assertThrows(PSAssemblyException.class, item::normalize);
        assertEquals(IPSAssemblyErrors.MISSING_PATH, ((PSAssemblyException) thrown).getErrorCode());
    }

    @Test
    public void testNormalizeWithPath() throws Exception {
        var asm = PSAssemblyServiceLocator.getAssemblyService();
        var item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        item.setPath(TEST_ITEM_PATH);
        item.normalize();

        assertParameterEquals(TEST_ITEM_CONTENTID, item, IPSHtmlParameters.SYS_CONTENTID);
        assertParameterEquals(TEST_ITEM_REVISION, item, IPSHtmlParameters.SYS_REVISION);
        assertParameterEquals(TEST_ITEM_FOLDERID, item, IPSHtmlParameters.SYS_FOLDERID);

        assertNotNull(item.getId());
        var loc = msGuidMgr.makeLocator(item.getId());
        assertEquals(TEST_ITEM_CONTENTID, loc.getId());
        assertEquals(TEST_ITEM_REVISION, loc.getRevision());
        assertEquals(TEST_ITEM_FOLDERID, item.getFolderId());

        assertNotNull(item.getPath());
        assertEquals(TEST_ITEM_PATH, item.getPath());
    }

    @Test
    public void testNormalizeWithPseudoPathAndFolder() throws Exception {
        var asm = PSAssemblyServiceLocator.getAssemblyService();
        var params = new HashMap<String, String[]>();
        params.put("sys_folderid", new String[]{Integer.toString(TEST_ITEM_FOLDERID)});
        var item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        item.setParameters(params);
        item.setPath(TEST_ITEM_PSEUDO_PATH);
        item.normalize();

        assertParameterEquals(TEST_ITEM_CONTENTID, item, IPSHtmlParameters.SYS_CONTENTID);
        assertParameterEquals(TEST_ITEM_REVISION, item, IPSHtmlParameters.SYS_REVISION);
        assertParameterEquals(TEST_ITEM_FOLDERID, item, IPSHtmlParameters.SYS_FOLDERID);

        assertNotNull(item.getId());
        var loc = msGuidMgr.makeLocator(item.getId());
        assertEquals(TEST_ITEM_CONTENTID, loc.getId());
        assertEquals(TEST_ITEM_REVISION, loc.getRevision());
        assertEquals(TEST_ITEM_FOLDERID, item.getFolderId());

        assertNotNull(item.getPath());
        assertEquals(TEST_ITEM_PATH, item.getPath());
    }

    @Test
    public void testNormalizeWithPseudoPathNoFolder() throws Exception {
        var asm = PSAssemblyServiceLocator.getAssemblyService();
        var item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        item.setPath(TEST_ITEM_PSEUDO_PATH);
        item.normalize();

        assertParameterEquals(TEST_ITEM_CONTENTID, item, IPSHtmlParameters.SYS_CONTENTID);
        assertParameterEquals(TEST_ITEM_REVISION, item, IPSHtmlParameters.SYS_REVISION);
        assertNull(item.getParameterValue(IPSHtmlParameters.SYS_FOLDERID, null));

        assertNotNull(item.getId());
        var loc = msGuidMgr.makeLocator(item.getId());
        assertEquals(TEST_ITEM_CONTENTID, loc.getId());
        assertEquals(TEST_ITEM_REVISION, loc.getRevision());
        assertEquals(0, item.getFolderId());

        assertNotNull(item.getPath());
        assertEquals(TEST_ITEM_PSEUDO_PATH, item.getPath());
    }

    @Test
    public void testNormalization1() throws Exception {
        var asm = PSAssemblyServiceLocator.getAssemblyService();
        var params = new HashMap<String, String[]>();
        params.put("sys_contentid", new String[]{"477"});
        params.put("sys_revision", new String[]{"1"});
        params.put("sys_folderid", new String[]{"309"});

        var item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        item.setParameters(params);
        item.setReferenceId(1);
        item.setJobId(10);
        item.normalize();
        assertEquals("477", item.getParameterValue("sys_contentid", null));
        assertEquals("1", item.getParameterValue("sys_revision", null));
        assertEquals("309", item.getParameterValue("sys_folderid", null));
        assertNotNull(item.getId());

        item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        item.setParameters(params);
        item.setReferenceId(2);
        item.setJobId(11);
        item.normalize();

        params = new HashMap<>();
        params.put("sys_contentid", new String[]{"477"});
        params.put("sys_revision", new String[]{"1"});

        item = (PSAssemblyWorkItem) asm.createAssemblyItem();
        item.setParameters(params);
        item.setReferenceId(1);
        item.setJobId(10);
        item.normalize();
        assertNotNull(item.getPath());

        // Test special node loading code for legacy
        assertNotNull(item.getNode());
    }

    public static void assertEqualArrays(byte[] a, byte[] b) {
        assertArrayEquals(a, b);
    }
}
