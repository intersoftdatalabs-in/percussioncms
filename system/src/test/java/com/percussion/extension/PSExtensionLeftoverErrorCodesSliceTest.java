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
package com.percussion.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.data.IPSDataErrors;
import com.percussion.data.PSConversionException;
import com.percussion.design.objectstore.PSExtensionParamDef;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;
import com.percussion.error.PSNotFoundException;
import com.percussion.server.IPSServerErrors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Issue #3970 (parent #2616 leftover): {@code com.percussion.extension} production sites throw
 * typed {@code *ErrorCodes} via IPSErrorCode-aware constructors — not bare {@code IPS*Errors}
 * ints. Dual-write skip is {@code isAuditable()==false} on leftover operational catalog codes.
 */
@Tag("UnitTest")
class PSExtensionLeftoverErrorCodesSliceTest {

  @Test
  void leftoverCatalogsMatchLegacyIntsAndSkipDualWrite() {
    assertEquals(
        IPSExtensionErrors.EXT_HANDLER_INIT_FAILED,
        ExtensionErrorCodes.EXT_HANDLER_INIT_FAILED.numericCode());
    assertEquals(
        IPSExtensionErrors.EXT_RESOURCE_DELETE_ERROR,
        ExtensionErrorCodes.EXT_RESOURCE_DELETE_ERROR.numericCode());
    assertEquals(
        IPSExtensionErrors.EXT_NOT_FOUND, ExtensionErrorCodes.EXT_NOT_FOUND.numericCode());
    assertEquals(
        IPSExtensionErrors.EXT_ALREADY_EXISTS,
        ExtensionErrorCodes.EXT_ALREADY_EXISTS.numericCode());
    assertEquals(
        IPSExtensionErrors.EXT_INSTALL_UPDATE_ERROR,
        ExtensionErrorCodes.EXT_INSTALL_UPDATE_ERROR.numericCode());
    assertEquals(
        IPSExtensionErrors.EXT_RESOURCE_STORE_ERROR,
        ExtensionErrorCodes.EXT_RESOURCE_STORE_ERROR.numericCode());
    assertEquals(
        IPSExtensionErrors.CATALOG_EXT_RESOURCE_ERROR,
        ExtensionErrorCodes.CATALOG_EXT_RESOURCE_ERROR.numericCode());
    assertEquals(
        IPSExtensionErrors.EXT_MANAGER_INIT_FAILED,
        ExtensionErrorCodes.EXT_MANAGER_INIT_FAILED.numericCode());
    assertEquals(
        IPSExtensionErrors.EXT_INIT_FAILED, ExtensionErrorCodes.EXT_INIT_FAILED.numericCode());
    assertEquals(
        IPSExtensionErrors.CLASS_NOT_FOUND, ExtensionErrorCodes.CLASS_NOT_FOUND.numericCode());
    assertEquals(
        IPSExtensionErrors.INVALID_NULL_PARAMS,
        ExtensionErrorCodes.INVALID_NULL_PARAMS.numericCode());
    assertEquals(
        IPSExtensionErrors.INVALID_NUMBER_PARAM,
        ExtensionErrorCodes.INVALID_NUMBER_PARAM.numericCode());
    assertEquals(
        IPSExtensionErrors.INVALID_BOOLEAN_PARAM,
        ExtensionErrorCodes.INVALID_BOOLEAN_PARAM.numericCode());
    assertEquals(
        IPSExtensionErrors.INVALID_DATE_PARAM,
        ExtensionErrorCodes.INVALID_DATE_PARAM.numericCode());
    assertEquals(
        IPSExtensionErrors.INVALID_INDEX_VALUE,
        ExtensionErrorCodes.INVALID_INDEX_VALUE.numericCode());
    assertEquals(
        IPSExtensionErrors.MISSING_REQUIRED_PARAM_NO,
        ExtensionErrorCodes.MISSING_REQUIRED_PARAM_NO.numericCode());
    assertEquals(
        IPSExtensionErrors.EXT_PROCESSOR_EXCEPTION,
        ExtensionErrorCodes.EXT_PROCESSOR_EXCEPTION.numericCode());
    assertEquals(
        IPSExtensionErrors.EXT_PARAM_VALUE_MISMATCH,
        ExtensionErrorCodes.EXT_PARAM_VALUE_MISMATCH.numericCode());
    assertEquals(
        IPSExtensionErrors.EXT_PARAM_VALUE_INVALID,
        ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID.numericCode());
    assertEquals(
        IPSExtensionErrors.JS_CALL_FAILED, ExtensionErrorCodes.JS_CALL_FAILED.numericCode());
    assertEquals(
        IPSExtensionErrors.JS_CALL_FAILED_SRC,
        ExtensionErrorCodes.JS_CALL_FAILED_SRC.numericCode());
    assertEquals(
        IPSExtensionErrors.JS_COMPILE_FAILED,
        ExtensionErrorCodes.JS_COMPILE_FAILED.numericCode());
    assertEquals(
        IPSExtensionErrors.JS_COMPILE_FAILED_SRC,
        ExtensionErrorCodes.JS_COMPILE_FAILED_SRC.numericCode());
    assertEquals(
        IPSExtensionErrors.UNKNOWN_PARAMETER_TYPE,
        ExtensionErrorCodes.UNKNOWN_PARAMETER_TYPE.numericCode());
    assertEquals(IPSServerErrors.ARGUMENT_ERROR, ServerErrorCodes.ARGUMENT_ERROR.numericCode());
    assertEquals(
        IPSDataErrors.UNSUPPORTED_CONVERSION,
        DataErrorCodes.UNSUPPORTED_CONVERSION.numericCode());

    leftoverNonAuditable(ExtensionErrorCodes.EXT_HANDLER_INIT_FAILED);
    leftoverNonAuditable(ExtensionErrorCodes.EXT_NOT_FOUND);
    leftoverNonAuditable(ExtensionErrorCodes.EXT_PROCESSOR_EXCEPTION);
    leftoverNonAuditable(ExtensionErrorCodes.JS_COMPILE_FAILED);
    leftoverNonAuditable(ExtensionErrorCodes.UNKNOWN_PARAMETER_TYPE);
    leftoverNonAuditable(ServerErrorCodes.ARGUMENT_ERROR);
    leftoverNonAuditable(DataErrorCodes.UNSUPPORTED_CONVERSION);
  }

  @Test
  void productionExceptionTypesRetainTypedCodesAndSkipDualWrite() {
    leftoverNonAuditable(
        new PSExtensionException(ExtensionErrorCodes.EXT_HANDLER_INIT_FAILED, new Object[] {"h", "dir"}),
        ExtensionErrorCodes.EXT_HANDLER_INIT_FAILED);
    leftoverNonAuditable(
        new PSNotFoundException(ExtensionErrorCodes.EXT_NOT_FOUND, "JavaScript/global/missing"),
        ExtensionErrorCodes.EXT_NOT_FOUND);
    leftoverNonAuditable(
        new PSExtensionException(ExtensionErrorCodes.EXT_ALREADY_EXISTS, "dup"),
        ExtensionErrorCodes.EXT_ALREADY_EXISTS);
    leftoverNonAuditable(
        new PSExtensionException(
            ExtensionErrorCodes.EXT_MANAGER_INIT_FAILED, new IllegalStateException("io"), "cfg"),
        ExtensionErrorCodes.EXT_MANAGER_INIT_FAILED);
    leftoverNonAuditable(
        new PSConversionException(ExtensionErrorCodes.INVALID_NULL_PARAMS),
        ExtensionErrorCodes.INVALID_NULL_PARAMS);
    leftoverNonAuditable(
        new PSConversionException(ExtensionErrorCodes.INVALID_NUMBER_PARAM, 3),
        ExtensionErrorCodes.INVALID_NUMBER_PARAM);
    leftoverNonAuditable(
        new PSParameterMismatchException(2, 1), ExtensionErrorCodes.EXT_PARAM_VALUE_MISMATCH);
    leftoverNonAuditable(
        new PSParameterMismatchException("bad value"), ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID);
    leftoverNonAuditable(
        new PSParameterMismatchException("en-us", 4, 1),
        ExtensionErrorCodes.EXT_PARAM_VALUE_MISMATCH);
    leftoverNonAuditable(
        new PSJavaScriptCallException("fn", "boom"), ExtensionErrorCodes.JS_CALL_FAILED);
    leftoverNonAuditable(
        new PSJavaScriptCallException("fn", "boom", "src"), ExtensionErrorCodes.JS_CALL_FAILED_SRC);
    leftoverNonAuditable(
        new PSJavaScriptCompileException("fn", "syntax"), ExtensionErrorCodes.JS_COMPILE_FAILED);
    leftoverNonAuditable(
        new PSJavaScriptCompileException("fn", "syntax", "src"),
        ExtensionErrorCodes.JS_COMPILE_FAILED_SRC);
    leftoverNonAuditable(
        new PSExtensionProcessingException("sys_exit", new IllegalStateException("eval")),
        ExtensionErrorCodes.EXT_PROCESSOR_EXCEPTION);
    leftoverNonAuditable(
        new PSExtensionProcessingException(
            "en-us", "sys_exit", new IllegalStateException("eval")),
        ExtensionErrorCodes.EXT_PROCESSOR_EXCEPTION);
    leftoverNonAuditable(
        new PSConversionException(
            ServerErrorCodes.ARGUMENT_ERROR, new Object[] {"need 1", "processUdf"}),
        ServerErrorCodes.ARGUMENT_ERROR);
    leftoverNonAuditable(
        new PSConversionException(
            DataErrorCodes.UNSUPPORTED_CONVERSION, new Object[] {"java.lang.Object", "Date", "x"}),
        DataErrorCodes.UNSUPPORTED_CONVERSION);
  }

  @Test
  void extensionParamsProductionThrowsRetainTypedCodes() throws Exception {
    PSConversionException nullParams =
        assertThrows(PSConversionException.class, () -> new PSExtensionParams(null));
    leftoverNonAuditable(nullParams, ExtensionErrorCodes.INVALID_NULL_PARAMS);

    PSExtensionParams ep = new PSExtensionParams(new Object[] {Integer.valueOf(7), "abc"});
    leftoverNonAuditable(
        assertThrows(PSConversionException.class, () -> ep.getNumberParam(1, null, true)),
        ExtensionErrorCodes.INVALID_NUMBER_PARAM);
    leftoverNonAuditable(
        assertThrows(PSConversionException.class, () -> ep.getBooleanParam(0, false, true)),
        ExtensionErrorCodes.INVALID_BOOLEAN_PARAM);
    leftoverNonAuditable(
        assertThrows(PSConversionException.class, () -> ep.getDateParam(1, null, true)),
        ExtensionErrorCodes.INVALID_DATE_PARAM);
    leftoverNonAuditable(
        assertThrows(PSConversionException.class, () -> ep.getUncheckedParam(-1)),
        ExtensionErrorCodes.INVALID_INDEX_VALUE);
    leftoverNonAuditable(
        assertThrows(PSConversionException.class, () -> ep.getStringParam(5, null, true)),
        ExtensionErrorCodes.MISSING_REQUIRED_PARAM_NO);
  }

  @Test
  void handlerInitWithNonDirectoryThrowsTyped(@TempDir Path tmp) throws Exception {
    Path notDir = tmp.resolve("not-a-directory.txt");
    Files.writeString(notDir, "x");
    PSExtensionException ex =
        assertThrows(
            PSExtensionException.class, () -> new TestHandler().init(simpleDef(), notDir.toFile()));
    leftoverNonAuditable(ex, ExtensionErrorCodes.EXT_HANDLER_INIT_FAILED);
  }

  @Test
  void handlerConfigMissingFileThrowsTyped(@TempDir Path tmp) {
    Path missing = tmp.resolve("no-such-extensions.xml");
    PSExtensionException ex =
        assertThrows(
            PSExtensionException.class,
            () -> new PSExtensionHandlerConfiguration(missing.toFile(), null, false));
    leftoverNonAuditable(ex, ExtensionErrorCodes.EXT_MANAGER_INIT_FAILED);
  }

  @Test
  void javaScriptUdfParamCountMismatchThrowsTyped() throws Exception {
    PSJavaScriptUdfExtension ext = newUdf("countMismatch", "return 1;", "Number");
    PSConversionException ex =
        assertThrows(PSConversionException.class, () -> ext.processUdf(new Object[0], null));
    leftoverNonAuditable(ex, ServerErrorCodes.ARGUMENT_ERROR);
  }

  @Test
  void javaScriptUdfUnknownParameterTypeThrowsTyped() throws Exception {
    PSJavaScriptUdfExtension ext = newUdf("unknownType", "return p;", "Widget");
    PSConversionException ex =
        assertThrows(PSConversionException.class, () -> ext.processUdf(new Object[] {"x"}, null));
    leftoverNonAuditable(ex, ExtensionErrorCodes.UNKNOWN_PARAMETER_TYPE);
  }

  @Test
  void javaScriptUdfUnsupportedDateConversionThrowsTyped() throws Exception {
    PSJavaScriptUdfExtension ext = newUdf("badDate", "return p;", "Date");
    PSConversionException ex =
        assertThrows(
            PSConversionException.class, () -> ext.processUdf(new Object[] {new Object()}, null));
    leftoverNonAuditable(ex, DataErrorCodes.UNSUPPORTED_CONVERSION);
  }

  @Test
  void typedLogMessageRejectsNullCode() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new TestHandler().logMessage((IPSErrorCode) null, new Object[] {"x"}));
  }

  private static PSJavaScriptUdfExtension newUdf(
      String extensionName, String scriptBody, String paramType) throws PSExtensionException {
    Properties init = new Properties();
    init.setProperty("scriptBody", scriptBody);
    PSExtensionParamDef param = new PSExtensionParamDef("p", paramType);
    PSExtensionDef def =
        new PSExtensionDef(
            new PSExtensionRef("JavaScript", "global/", extensionName),
            List.of(IPSUdfProcessor.class.getName()).iterator(),
            null,
            init,
            List.of(param).iterator());
    PSJavaScriptUdfExtension ext = new PSJavaScriptUdfExtension();
    ext.init(def, null);
    return ext;
  }

  private static IPSExtensionDef simpleDef() {
    Properties init = new Properties();
    init.setProperty(IPSExtensionHandler.INIT_PARAM_CONFIG_FILENAME, "Extensions.xml");
    return new PSExtensionDef(
        new PSExtensionRef("Java", "global/", "testHandler"),
        List.of(IPSExtensionHandler.class.getName()).iterator(),
        null,
        init,
        null);
  }

  private static void leftoverNonAuditable(IPSErrorCode expected) {
    assertFalse(expected.isAuditable(), expected.toString());
  }

  private static void leftoverNonAuditable(PSException ex, IPSErrorCode expected) {
    assertSame(expected, ex.getTypedErrorCode(), expected.toString());
    assertEquals(expected.numericCode(), ex.getErrorCode(), expected.toString());
    assertFalse(ex.isAuditable(), expected.toString());
    assertFalse(expected.isAuditable(), expected.toString());
  }

  /** Package-visible stub so leftover init/log paths can be exercised without a live handler. */
  static final class TestHandler extends PSExtensionHandler {
    @Override
    public String getName() {
      return "testHandler";
    }

    @Override
    protected IPSExtension loadExtension(PSExtensionRef ref) {
      return null;
    }
  }
}
