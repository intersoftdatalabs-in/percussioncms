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
package com.percussion.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.percussion.cas.PSConcatAssemblyLocation;
import com.percussion.data.PSConversionException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSExtensionRef;
import com.percussion.extensions.general.PSPrepareInClause;
import com.percussion.extensions.general.PSSetArrayHtmlParameter;
import com.percussion.extensions.translations.PSFormEncode;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSRequestValidationException;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

/**
 * Leftover extensions-main production throw sites now construct typed {@link ExtensionErrorCodes}
 * (non-auditable). File-root init uses a portable {@link Path} temp directory.
 */
@Tag("UnitTest")
class PSExtensionsMainTypedErrorCodeSliceTest {

  @TempDir Path tempRoot;

  @Test
  void formEncodeMissingNameThrowsTypedMissingRequiredParam() {
    PSFormEncode encode = new PSFormEncode();
    PSConversionException ex =
        assertThrows(PSConversionException.class, () -> encode.processUdf(new Object[] {""}, null));
    assertSame(ExtensionErrorCodes.MISSING_REQUIRED_PARAM_NO, ex.getTypedErrorCode());
    assertEquals(ExtensionErrorCodes.MISSING_REQUIRED_PARAM_NO.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void prepareInClauseNullRequestThrowsTypedProcessorException() {
    PSPrepareInClause exit = new PSPrepareInClause();
    PSRequestValidationException ex =
        assertThrows(
            PSRequestValidationException.class,
            () -> exit.preProcessRequest(new Object[] {"inClause"}, null));
    assertSame(ExtensionErrorCodes.EXT_PROCESSOR_EXCEPTION, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void setArrayHtmlParameterNullRequestThrowsTypedProcessorException() {
    PSSetArrayHtmlParameter exit = new PSSetArrayHtmlParameter();
    PSRequestValidationException ex =
        assertThrows(
            PSRequestValidationException.class,
            () ->
                exit.preProcessRequest(
                    new Object[] {"param", "app/resource", "el", "10"}, null));
    assertSame(ExtensionErrorCodes.EXT_PROCESSOR_EXCEPTION, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void concatAssemblyLocationParamToStringFailureThrowsTypedProcessorException() throws Exception {
    PSConcatAssemblyLocation loc = new PSConcatAssemblyLocation();
    loc.init(mockDef("sys_concat"), tempRoot.toFile());
    IPSRequestContext request = mock(IPSRequestContext.class);
    Object boom =
        new Object() {
          @Override
          public String toString() {
            throw new IllegalStateException("boom");
          }
        };
    PSExtensionException ex =
        assertThrows(
            PSExtensionException.class, () -> loc.createLocation(new Object[] {boom}, request));
    assertSame(ExtensionErrorCodes.EXT_PROCESSOR_EXCEPTION, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void processingExceptionFromFileInfoArgsIsNonAuditable() {
    Object args = new Object[] {"sys_FileInfo", "io"};
    PSExtensionProcessingException ex =
        new PSExtensionProcessingException(ExtensionErrorCodes.EXT_PROCESSOR_EXCEPTION, args);
    assertSame(ExtensionErrorCodes.EXT_PROCESSOR_EXCEPTION, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  private static IPSExtensionDef mockDef(String name) {
    IPSExtensionDef def = mock(IPSExtensionDef.class);
    Mockito.when(def.getRef()).thenReturn(new PSExtensionRef("Java", "global/percussion/", name));
    return def;
  }
}
