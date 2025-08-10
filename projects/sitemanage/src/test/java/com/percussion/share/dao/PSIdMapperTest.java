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
package com.percussion.share.dao;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.utils.guid.IPSGuid;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for {@link PSIdMapper}. Sunny Sal: "ID mapping, Java 11, and GUID ka hero!" */
@Tag("IntegrationTest")
@Tag("integration")
public class PSIdMapperTest {

  private IPSIdMapper idMapper;
  private IPSGuidManager guidManager;

  @BeforeEach
  void setUp() throws Exception {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  @Test
  void testGetGuid() {
    var guid = createGuid();
    var locator = guidManager.makeLocator(guid);
    assertEquals(guid, idMapper.getGuid(locator));

    var guidStr = idMapper.getString(guid);
    assertEquals(guid, idMapper.getGuid(guidStr));
  }

  @Test
  void testGetGuids() {
    var guid1 = createGuid();
    var guid2 = createGuid();

    var guidStrings = new ArrayList<String>();
    guidStrings.add(idMapper.getString(guid1));
    guidStrings.add(idMapper.getString(guid2));

    var guids = idMapper.getGuids(guidStrings);
    assertTrue(guids.contains(guid1) && guids.contains(guid2));
  }

  @Test
  void testGetLocalContentId() {
    var id1 = idMapper.getLocalContentId();
    assertTrue(id1 > 0);
    var id2 = idMapper.getLocalContentId();
    assertTrue(id2 > id1);
  }

  @Test
  void testGetLocator() {
    var guid = createGuid();
    var locator = idMapper.getLocator(guid);
    assertEquals(guid, idMapper.getGuid(locator));

    var guidStr = idMapper.getString(guid);
    assertEquals(locator, idMapper.getLocator(guidStr));
  }

  @Test
  void testGetString() {
    var guid = createGuid();
    var guidStr = idMapper.getString(guid);
    assertEquals(guid, idMapper.getGuid(guidStr));

    var locator = guidManager.makeLocator(guid);
    assertEquals(guidStr, idMapper.getString(locator));
  }

  @Test
  void testGetStrings() {
    var guid1 = createGuid();
    var guid2 = createGuid();

    var guids = new ArrayList<IPSGuid>();
    guids.add(guid1);
    guids.add(guid2);

    var guidStrings = idMapper.getStrings(guids);
    assertTrue(
        guidStrings.contains(idMapper.getString(guid1))
            && guidStrings.contains(idMapper.getString(guid2)));
  }

  public IPSGuidManager getGuidManager() {
    return guidManager;
  }

  public void setGuidManager(IPSGuidManager guidManager) {
    this.guidManager = guidManager;
  }

  public IPSIdMapper getIdMapper() {
    return idMapper;
  }

  public void setIdMapper(IPSIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  private IPSGuid createGuid() {
    var guid = guidManager.createGuid(PSTypeEnum.LEGACY_CONTENT);
    return new PSLegacyGuid(guid.getUUID(), 1);
  }
}
