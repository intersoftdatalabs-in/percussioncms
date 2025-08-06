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
package test.percussion.pso.imageedit.web.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSItemChildEntry;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.pso.imageedit.data.MasterImageMetaData;
import com.percussion.pso.imageedit.data.OpenImageResult;
import com.percussion.pso.imageedit.services.ImageSizeDefinitionManager;
import com.percussion.pso.imageedit.services.cache.ImageCacheManager;
import com.percussion.pso.imageedit.web.impl.ImagePersistenceManagerImpl;
import com.percussion.services.content.data.PSItemStatus;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;

/**
 * // REFACTORED: CP-JAVA11
 */
@ExtendWith(MockitoExtension.class)
public class ImagePersistenceManagerImplTest {
    private static final Logger log = LogManager.getLogger(ImagePersistenceManagerImplTest.class);

    @Mock
    IPSGuidManager gmgr;
    @Mock
    IPSContentWs cws;
    @Mock
    ImageCacheManager cache;
    @Mock
    ImageSizeDefinitionManager isdm;
    @InjectMocks
    ImagePersistenceManagerImpl cut;
    Map<String, String> parentMap;
    Map<String, String> childMap;

    @BeforeEach
    public void setUp() {
        parentMap = new HashMap<>();
        childMap = new HashMap<>();
        cut.setChildFieldMap(childMap);
        cut.setMasterFieldMap(parentMap);
        cut.setGmgr(gmgr);
        cut.setCws(cws);
        cut.setCache(cache);
        cut.setIsdm(isdm);
        cut.setImageContentType("contentType");
    }

    @Test
    public void testCreateImage() {
        log.debug("testing create image");
        var master = new MasterImageMetaData();
        var item = Mockito.mock(PSCoreItem.class);
        var ilist = Collections.singletonList(item);
        var itemGuid = Mockito.mock(IPSGuid.class);
        var glist = Collections.singletonList(itemGuid);
        var itemLoc = new PSLocator(42);
        var folderGuid = Mockito.mock(IPSGuid.class);
        Mockito.when(cws.createItems("contentType", 1)).thenReturn(ilist);
        Mockito.when(cws.saveItems(ilist, false, false)).thenReturn(glist);
        Mockito.when(isdm.getSizedImageNodeName()).thenReturn("childNode");
        Mockito.doNothing().when(cws).addFolderChildren(Mockito.any(IPSGuid.class), Mockito.anyList());
        Mockito.when(gmgr.makeLocator(itemGuid)).thenReturn(itemLoc);
        Mockito.when(gmgr.makeGuid(Mockito.any(PSLocator.class))).thenReturn(folderGuid);
        String id = cut.CreateImage(master, "47", false);
        assertTrue(StringUtils.isNotBlank(id));
        assertEquals("42", id);
    }

    @Test
    public void testValidateSystemTitleUnique() {
        var folderGuid = Mockito.mock(IPSGuid.class);
        var sum1 = new PSItemSummary();
        sum1.setName("item1");
        var summs = new ArrayList<PSItemSummary>();
        summs.add(sum1);
        Mockito.when(gmgr.makeGuid(Mockito.any(PSLocator.class))).thenReturn(folderGuid);
        Mockito.when(cws.findFolderChildren(folderGuid, false)).thenReturn(summs);
        boolean result = cut.validateSystemTitleUnique("item1", "123");
        assertFalse(result);
    }

    @Test
    public void testOpenImage() {
        var item = Mockito.mock(PSCoreItem.class);
        var ilist = Collections.singletonList(item);
        var itemGuid = Mockito.mock(IPSGuid.class);
        var glist = Collections.singletonList(itemGuid);
        var itemLoc = new PSLocator(42);
        var childList = new ArrayList<PSItemChildEntry>();
        var itemStatus = new PSItemStatus(42);
        var isList = Collections.singletonList(itemStatus);
        Mockito.when(gmgr.makeGuid(Mockito.any(PSLocator.class))).thenReturn(itemGuid);
        Mockito.when(cws.prepareForEdit(Mockito.anyList())).thenReturn(isList);
        Mockito.when(cws.loadItems(glist, true, false, false, false)).thenReturn(ilist);
        Mockito.when(cws.loadChildEntries(itemGuid, "childNode", true)).thenReturn(childList);
        Mockito.when(isdm.getSizedImageNodeName()).thenReturn("childNode");
        OpenImageResult oir = cut.OpenImage("42");
        assertNotNull(oir);
    }

    @Test
    public void testUpdateImage() {
        var master = new MasterImageMetaData();
        var item = Mockito.mock(PSCoreItem.class);
        var ilist = Collections.singletonList(item);
        var itemGuid = Mockito.mock(IPSGuid.class);
        var glist = Collections.singletonList(itemGuid);
        var itemLoc = new PSLocator(42);
        var childList = new ArrayList<PSItemChildEntry>();
        var itemStatus = new PSItemStatus(42);
        var isList = Collections.singletonList(itemStatus);
        Mockito.when(gmgr.makeGuid(Mockito.any(PSLocator.class))).thenReturn(itemGuid);
        Mockito.when(cws.prepareForEdit(Mockito.anyList())).thenReturn(isList);
        Mockito.when(cws.loadItems(glist, true, false, false, false)).thenReturn(ilist);
        Mockito.when(cws.saveItems(ilist, false, false)).thenReturn(glist);
        Mockito.when(cws.loadChildEntries(itemGuid, "childNode", true)).thenReturn(childList);
        Mockito.doNothing().when(cws).checkinItems(glist, null);
        Mockito.when(isdm.getSizedImageNodeName()).thenReturn("childNode");
        cut.UpdateImage(master, "42", null);
    }
}
