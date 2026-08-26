/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.pagemanagement.assembler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.PublisherErrorCodes;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.publisher.PSPublisherException;
import com.percussion.services.publisher.data.PSContentListItem;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import java.io.File;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * {@link PSAbstractTemplateExpanderAdapter} wraps repository failures as typed {@link
 * PublisherErrorCodes#RUNTIME_ERROR} (issue #3846).
 */
@Tag("UnitTest")
class PSAbstractTemplateExpanderAdapterTypedErrorCodeTest {

  @Test
  void expandWrapsRepositoryFailureAsTypedRuntimeError() throws Exception {
    QueryResult results = mock(QueryResult.class);
    when(results.getRows()).thenThrow(new RepositoryException("row walk failed"));
    Map<String, String> parameters =
        Map.of(IPSHtmlParameters.SYS_DELIVERY_CONTEXT, "1");

    StubExpander expander = new StubExpander();
    PSPublisherException thrown =
        assertThrows(
            PSPublisherException.class, () -> expander.expand(results, parameters, Map.of()));
    assertEquals(PublisherErrorCodes.RUNTIME_ERROR.numericCode(), thrown.getErrorCode());
    assertFalse(PublisherErrorCodes.RUNTIME_ERROR.isAuditable());
  }

  static final class StubExpander extends PSAbstractTemplateExpanderAdapter<Object> {
    @Override
    public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
      // unused in this slice
    }

    @Override
    protected IPSGuid getTemplateId(Map<String, String> parameters, Object templateCache)
        throws PSDataServiceException, PSAssemblyException {
      return null;
    }

    @Override
    protected Object createTemplateCache() {
      return new Object();
    }

    @Override
    protected List<PSContentListItem> expandContentListItem(
        PSContentListItem contentListItem, Map<String, String> parameters)
        throws PSDataServiceException {
      return List.of();
    }
  }
}
