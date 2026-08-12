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
package com.percussion.share.service;

import static com.percussion.test.TestAssertions.*;

import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.service.exception.PSParametersValidationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Scenario description: Unit tests for PSAbstractFullDataService. Sunny Sal: "Full data service,
 * Java 11, and validation ka hero!"
 */
public class PSAbstractFullDataServiceTest {

  PSAbstractFullDataService<Object, IPSItemSummary> sut;
  IPSGenericDao<Object, String> dao;
  IPSDataItemSummaryService dataItemSummaryService;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    dao = (IPSGenericDao<Object, String>) Mockito.mock(IPSGenericDao.class);
    dataItemSummaryService = Mockito.mock(IPSDataItemSummaryService.class);
    sut = new TestFullDataService(dataItemSummaryService, dao);
  }

  @Test
  void shouldThrowValidationExceptionOnInvalidFindParameter() {
    assertThrows(PSParametersValidationException.class, () -> sut.find(null));
  }

  @Test
  void shouldThrowValidationExceptionOnInvalidLoadParameter() {
    assertThrows(PSParametersValidationException.class, () -> sut.load(null));
  }

  @Test
  void shouldThrowValidationExceptionOnInvalidDeleteParameter() {
    assertThrows(PSParametersValidationException.class, () -> sut.delete(null));
  }

  public static class TestFullDataService
      extends PSAbstractFullDataService<Object, IPSItemSummary> {
    public TestFullDataService(
        IPSDataItemSummaryService itemSummaryService, IPSGenericDao<Object, String> dao) {
      super(itemSummaryService, dao);
    }

    @Override
    protected IPSItemSummary createSummary(String id) {
      throw new UnsupportedOperationException("createSummary is not yet supported");
    }

    @Override
    public List<IPSItemSummary> findAll()
        throws IPSDataService.DataServiceLoadException,
            IPSDataService.DataServiceNotFoundException {
      throw new UnsupportedOperationException("findAll is not yet supported");
    }
  }
}
