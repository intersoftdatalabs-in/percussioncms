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

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.*;
=======
import static org.junit.Assert.*;
>>>>>>> development-8.1.x

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
<<<<<<< HEAD
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/** // REFACTORED: CP-JAVA11 */
@ExtendWith(MockitoExtension.class)
public class ImagePersistenceManagerImplTest {
  private static final Logger log = LogManager.getLogger(ImagePersistenceManagerImplTest.class);

  @Mock IPSGuidManager gmgr;
  @Mock IPSContentWs cws;
  @Mock ImageCacheManager cache;
  @Mock ImageSizeDefinitionManager isdm;
  @InjectMocks ImagePersistenceManagerImpl cut;
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
=======
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

public class ImagePersistenceManagerImplTest {
  private static final Logger log = LogManager.getLogger(ImagePersistenceManagerImplTest.class);

  Mockery context;
  ImagePersistenceManagerImpl cut;

  IPSGuidManager gmgr;
  IPSContentWs cws;
  ImageCacheManager cache;
  ImageSizeDefinitionManager isdm;

  Map<String, String> parentMap;
  Map<String, String> childMap;

  @Before
  public void setUp() throws Exception {
    context =
        new Mockery() {
          {
            setImposteriser(ClassImposteriser.INSTANCE);
          }
        };
    cut = new ImagePersistenceManagerImpl();
    parentMap = new HashMap<String, String>();
    childMap = new HashMap<String, String>();
    cut.setChildFieldMap(childMap);
    cut.setMasterFieldMap(parentMap);

    gmgr = context.mock(IPSGuidManager.class);
    cut.setGmgr(gmgr);

    cws = context.mock(IPSContentWs.class);
    cut.setCws(cws);

    cache = context.mock(ImageCacheManager.class);
    cut.setCache(cache);

    isdm = context.mock(ImageSizeDefinitionManager.class);
    cut.setIsdm(isdm);

>>>>>>> development-8.1.x
    cut.setImageContentType("contentType");
  }

  @Test
<<<<<<< HEAD
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
=======
  @SuppressWarnings("unchecked")
  public final void testCreateImage() {
    log.debug("testing create image");

    MasterImageMetaData master = new MasterImageMetaData();

    try {
      final PSCoreItem item = context.mock(PSCoreItem.class);
      final List<PSCoreItem> ilist = Collections.<PSCoreItem>singletonList(item);
      final IPSGuid itemGuid = context.mock(IPSGuid.class);
      final List<IPSGuid> glist = Collections.<IPSGuid>singletonList(itemGuid);
      final PSLocator itemLoc = new PSLocator(42);
      final IPSGuid folderGuid = context.mock(IPSGuid.class, "folderGuid");

      context.checking(
          new Expectations() {
            {
              one(cws).createItems("contentType", 1);
              will(returnValue(ilist));
              one(cws).saveItems(ilist, false, false);
              will(returnValue(glist));
              atLeast(1).of(isdm).getSizedImageNodeName();
              will(returnValue("childNode"));
              one(cws).addFolderChildren(with(any(IPSGuid.class)), with(any(List.class)));
              one(gmgr).makeLocator(itemGuid);
              will(returnValue(itemLoc));
              one(gmgr).makeGuid(with(any(PSLocator.class)));
              will(returnValue(folderGuid));
            }
          });

      String id = cut.CreateImage(master, "47", false);
      assertTrue(StringUtils.isNotBlank(id));
      assertEquals("42", id);
      context.assertIsSatisfied();

    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("exception");
    }
  }

  @Test
  public final void testValidateSystemTitleUnique() {
    final IPSGuid folderGuid = context.mock(IPSGuid.class);
    final PSItemSummary sum1 =
        new PSItemSummary() {
          {
            setName("item1");
          }
        };
    final List<PSItemSummary> summs =
        new ArrayList<PSItemSummary>() {
          {
            add(sum1);
          }
        };

    try {
      context.checking(
          new Expectations() {
            {
              one(gmgr).makeGuid(with(any(PSLocator.class)));
              will(returnValue(folderGuid));
              one(cws).findFolderChildren(folderGuid, false);
              will(returnValue(summs));
            }
          });

      boolean result = cut.validateSystemTitleUnique("item1", "123");
      assertFalse(result);
      context.assertIsSatisfied();

    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception caught");
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public final void testOpenImage() {
    final PSCoreItem item = context.mock(PSCoreItem.class);
    final List<PSCoreItem> ilist = Collections.<PSCoreItem>singletonList(item);
    final IPSGuid itemGuid = context.mock(IPSGuid.class);
    final List<IPSGuid> glist = Collections.<IPSGuid>singletonList(itemGuid);
    final PSLocator itemLoc = new PSLocator(42);
    final List<PSItemChildEntry> childList = new ArrayList<PSItemChildEntry>();

    final PSItemStatus itemStatus = new PSItemStatus(42);
    final List<PSItemStatus> isList = Collections.<PSItemStatus>singletonList(itemStatus);
    try {
      context.checking(
          new Expectations() {
            {
              atLeast(1).of(gmgr).makeGuid(with(any(PSLocator.class)));
              will(returnValue(itemGuid));
              one(cws).prepareForEdit(with(any(List.class)));
              will(returnValue(isList));
              one(cws).loadItems(glist, true, false, false, false);
              will(returnValue(ilist));
              one(cws).loadChildEntries(itemGuid, "childNode", true);
              will(returnValue(childList));
              one(isdm).getSizedImageNodeName();
              will(returnValue("childNode"));
            }
          });
      OpenImageResult oir = cut.OpenImage("42");
      assertNotNull(oir);
      context.assertIsSatisfied();
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("exception");
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public final void testUpdateImage() {
    MasterImageMetaData master = new MasterImageMetaData();
    try {
      final PSCoreItem item = context.mock(PSCoreItem.class);
      final List<PSCoreItem> ilist = Collections.<PSCoreItem>singletonList(item);
      final IPSGuid itemGuid = context.mock(IPSGuid.class);
      final List<IPSGuid> glist = Collections.<IPSGuid>singletonList(itemGuid);
      final PSLocator itemLoc = new PSLocator(42);
      final List<PSItemChildEntry> childList = new ArrayList<PSItemChildEntry>();

      final PSItemStatus itemStatus = new PSItemStatus(42);
      final List<PSItemStatus> isList = Collections.<PSItemStatus>singletonList(itemStatus);

      context.checking(
          new Expectations() {
            {
              atLeast(1).of(gmgr).makeGuid(with(any(PSLocator.class)));
              will(returnValue(itemGuid));
              one(cws).prepareForEdit(with(any(List.class)));
              will(returnValue(isList));
              one(cws).loadItems(glist, true, false, false, false);
              will(returnValue(ilist));
              one(cws).saveItems(ilist, false, false);
              will(returnValue(glist));
              one(cws).loadChildEntries(itemGuid, "childNode", true);
              will(returnValue(childList));
              one(cws).checkinItems(glist, null);
              one(isdm).getSizedImageNodeName();
              will(returnValue("childNode"));

              one(isdm).getSizedImageNodeName();
              will(returnValue("childNode"));
            }
          });
      cut.UpdateImage(master, "42", null);

      context.assertIsSatisfied();
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("exception");
    }
>>>>>>> development-8.1.x
  }
}
