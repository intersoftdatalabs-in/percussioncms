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

package com.percussion.errorcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.SystemErrorCode;
import com.intsof.percussioncms.auditlog.codes.AssemblyErrorCodes;
import com.intsof.percussioncms.auditlog.codes.BeansErrorCodes;
import com.intsof.percussioncms.auditlog.codes.CatalogErrorCodes;
import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ConnectionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ContentErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.LocaleErrorCodes;
import com.intsof.percussioncms.auditlog.codes.PathItemErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.cms.IPSCmsErrors;
import com.percussion.conn.IPSConnectionErrors;
import com.percussion.content.IPSContentErrors;
import com.percussion.content.PSContentConversionException;
import com.percussion.design.catalog.IPSCatalogErrors;
import com.percussion.error.IPSBeansErrors;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSBeansException;
import com.percussion.error.PSException;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.error.PSNotFoundException;
import com.percussion.extension.IPSExtensionErrors;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.i18n.IPSLocaleErrors;
import com.percussion.i18n.PSLocaleException;
import com.percussion.server.IPSServerErrors;
import com.percussion.services.assembly.IPSAssemblyErrors;
import com.percussion.workflow.PSEntryNotFoundException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4264 (parent #2616 leftover): modules + misc system production sites throw typed {@code
 * *ErrorCodes}. Leftover catalog codes used by this slice are non-auditable and skip dual-write.
 */
@Tag("UnitTest")
class PSModulesMiscLeftoverErrorCodesSliceTest {

  @BeforeEach
  void setUp() {
    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
    DefaultAuditLogService.Holder.resetToDefault();
    LegacyErrorCodeRegistry.bootstrap();
  }

  @AfterEach
  void tearDown() {
    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
    DefaultAuditLogService.Holder.resetToDefault();
  }

  @Test
  void leftoverCatalogsMatchLegacyInts() {
    assertEquals(IPSBeansErrors.XML_PROCESSING_ERROR, BeansErrorCodes.XML_PROCESSING_ERROR.numericCode());
    assertEquals(IPSAssemblyErrors.MISSING_FINDER, AssemblyErrorCodes.MISSING_FINDER.numericCode());
    assertEquals(IPSExtensionErrors.EXT_INIT_FAILED, ExtensionErrorCodes.EXT_INIT_FAILED.numericCode());
    assertEquals(IPSExtensionErrors.NO_RECORDS, ExtensionErrorCodes.NO_RECORDS.numericCode());
    assertEquals(
        IPSExtensionErrors.EXT_INSTALLER_DEPLOY_NAME_EXPECTED,
        ExtensionErrorCodes.EXT_INSTALLER_DEPLOY_NAME_EXPECTED.numericCode());
    assertEquals(
        IPSContentErrors.UNSUPPORTED_CONVERT_CONSTRUCTOR,
        ContentErrorCodes.UNSUPPORTED_CONVERT_CONSTRUCTOR.numericCode());
    assertEquals(IPSContentErrors.UNSUPPORTED_MIMETYPE, ContentErrorCodes.UNSUPPORTED_MIMETYPE.numericCode());
    assertEquals(
        IPSContentErrors.UNSUPPORTED_CONVERT_METHOD,
        ContentErrorCodes.UNSUPPORTED_CONVERT_METHOD.numericCode());
    assertEquals(
        IPSContentErrors.CONTENT_CONVERSION_UNEXPECTED_ERROR,
        ContentErrorCodes.CONTENT_CONVERSION_UNEXPECTED_ERROR.numericCode());
    assertEquals(
        IPSContentErrors.UNSUPPORTED_EXTRACTION_EXIT,
        ContentErrorCodes.UNSUPPORTED_EXTRACTION_EXIT.numericCode());
    assertEquals(IPSServerErrors.RAW_DUMP, ServerErrorCodes.RAW_DUMP.numericCode());
    assertEquals(IPSServerErrors.ARGUMENT_ERROR, ServerErrorCodes.ARGUMENT_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.CE_TABLE_ALIAS_DUPLICATE,
        ServerErrorCodes.CE_TABLE_ALIAS_DUPLICATE.numericCode());
    assertEquals(
        IPSConnectionErrors.SERVER_GENERATED_EXCEPTION,
        ConnectionErrorCodes.SERVER_GENERATED_EXCEPTION.numericCode());
    assertEquals(
        IPSCatalogErrors.REQ_DOC_MISSING_GENERIC,
        CatalogErrorCodes.REQ_DOC_MISSING_GENERIC.numericCode());
    assertEquals(
        IPSCatalogErrors.REQ_DOC_ROOT_MISSING_GENERIC,
        CatalogErrorCodes.REQ_DOC_ROOT_MISSING_GENERIC.numericCode());
    assertEquals(
        IPSCatalogErrors.REQ_DOC_INVALID_TYPE, CatalogErrorCodes.REQ_DOC_INVALID_TYPE.numericCode());
    assertEquals(
        IPSCmsErrors.SITE_LOOKUP_FAILED, PathItemErrorCodes.SITE_LOOKUP_FAILED.numericCode());
    assertEquals(
        IPSCmsErrors.REQUIRED_RESOURCE_MISSING, CmsErrorCodes.REQUIRED_RESOURCE_MISSING.numericCode());
    assertEquals(
        IPSCmsErrors.CMS_INTERNAL_REQUEST_ERROR,
        CmsErrorCodes.CMS_INTERNAL_REQUEST_ERROR.numericCode());
    assertEquals(IPSCmsErrors.SEARCH_ERROR, CmsErrorCodes.SEARCH_ERROR.numericCode());
    assertEquals(IPSLocaleErrors.INVALID_COLUMN_VALUE, LocaleErrorCodes.INVALID_COLUMN_VALUE.numericCode());
    assertEquals(IPSLocaleErrors.MISSING_COLUMN, LocaleErrorCodes.MISSING_COLUMN.numericCode());
    assertEquals(IPSLocaleErrors.LOCALE_MGR_INIT, LocaleErrorCodes.LOCALE_MGR_INIT.numericCode());
    assertEquals(
        IPSLocaleErrors.LOCALE_MGR_UNEXPECTED_ERROR,
        LocaleErrorCodes.LOCALE_MGR_UNEXPECTED_ERROR.numericCode());
  }

  @Test
  void leftoverNonAuditableCodesSkipDualWrite() {
    DefaultAuditLogService svc = DefaultAuditLogService.createDefault();
    // BeansErrorCodes.XML_PROCESSING_ERROR (1001) collides with another catalog's NATIVE_ERROR in
    // the legacy registry; dual-write skip for beans is covered in perc-i18n / utils tests.
    List<SystemErrorCode> leftovers =
        List.of(
            AssemblyErrorCodes.MISSING_FINDER,
            ExtensionErrorCodes.EXT_INIT_FAILED,
            ExtensionErrorCodes.NO_RECORDS,
            ExtensionErrorCodes.EXT_INSTALLER_DEPLOY_NAME_EXPECTED,
            ExtensionErrorCodes.EXT_INSTALLER_UNSUPPORTED_RESOURCE,
            ExtensionErrorCodes.EXT_INSTALLER_RESOURCE_NOT_EXITING,
            ExtensionErrorCodes.EXT_INSTALLER_RESOURCE_NOT_READABLE,
            ContentErrorCodes.UNSUPPORTED_CONVERT_CONSTRUCTOR,
            ContentErrorCodes.UNSUPPORTED_MIMETYPE,
            ContentErrorCodes.UNSUPPORTED_CONVERT_METHOD,
            ContentErrorCodes.CONTENT_CONVERSION_UNEXPECTED_ERROR,
            ContentErrorCodes.UNSUPPORTED_EXTRACTION_EXIT,
            ServerErrorCodes.RAW_DUMP,
            ServerErrorCodes.ARGUMENT_ERROR,
            ServerErrorCodes.CE_TABLE_ALIAS_DUPLICATE,
            ConnectionErrorCodes.SERVER_GENERATED_EXCEPTION,
            CatalogErrorCodes.REQ_DOC_MISSING_GENERIC,
            CatalogErrorCodes.REQ_DOC_ROOT_MISSING_GENERIC,
            CatalogErrorCodes.REQ_DOC_INVALID_TYPE,
            PathItemErrorCodes.SITE_LOOKUP_FAILED,
            CmsErrorCodes.REQUIRED_RESOURCE_MISSING,
            CmsErrorCodes.CMS_INTERNAL_REQUEST_ERROR,
            CmsErrorCodes.SEARCH_ERROR,
            LocaleErrorCodes.INVALID_COLUMN_VALUE,
            LocaleErrorCodes.MISSING_COLUMN,
            LocaleErrorCodes.LOCALE_MGR_INIT,
            LocaleErrorCodes.LOCALE_MGR_UNEXPECTED_ERROR);

    for (SystemErrorCode code : leftovers) {
      assertFalse(code.isAuditable(), code.toString());
      assertSame(code, LegacyErrorCodeRegistry.find(code.numericCode()).orElseThrow());
      AuditLogId id =
          LegacyErrorCodeRegistry.logIfAuditable(
              svc, code.numericCode(), AuditContext.builder().actor("jdoe").build());
      assertEquals(LegacyErrorCodeRegistry.SKIPPED, id, code.toString());
    }
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void productionExceptionTypesRetainTypedCodes() {
    leftoverNonAuditable(
        new PSContentConversionException(ContentErrorCodes.UNSUPPORTED_MIMETYPE),
        ContentErrorCodes.UNSUPPORTED_MIMETYPE);
    leftoverNonAuditable(
        new PSLocaleException(LocaleErrorCodes.LOCALE_MGR_INIT, "boom"),
        LocaleErrorCodes.LOCALE_MGR_INIT);
    leftoverNonAuditable(
        new PSExtensionException(ExtensionErrorCodes.EXT_INIT_FAILED, "mode"),
        ExtensionErrorCodes.EXT_INIT_FAILED);
    leftoverNonAuditable(
        new PSExtensionProcessingException(CmsErrorCodes.CMS_INTERNAL_REQUEST_ERROR, new Object[] {"a"}),
        CmsErrorCodes.CMS_INTERNAL_REQUEST_ERROR);
    leftoverNonAuditable(
        new PSNotFoundException(PathItemErrorCodes.SITE_LOOKUP_FAILED, new Object[] {"r", "1"}),
        PathItemErrorCodes.SITE_LOOKUP_FAILED);
    leftoverNonAuditable(
        new PSIllegalArgumentException(CatalogErrorCodes.REQ_DOC_MISSING_GENERIC),
        CatalogErrorCodes.REQ_DOC_MISSING_GENERIC);
    leftoverNonAuditable(
        new PSEntryNotFoundException(ExtensionErrorCodes.NO_RECORDS), ExtensionErrorCodes.NO_RECORDS);

    PSBeansException beans =
        new PSBeansException(BeansErrorCodes.XML_PROCESSING_ERROR, "xml boom");
    assertSame(BeansErrorCodes.XML_PROCESSING_ERROR, beans.getTypedErrorCode());
    assertEquals(BeansErrorCodes.XML_PROCESSING_ERROR.numericCode(), beans.getErrorCode());
    assertFalse(beans.isAuditable());
  }

  private static void leftoverNonAuditable(PSException ex, IPSErrorCode expected) {
    assertSame(expected, ex.getTypedErrorCode());
    assertEquals(expected.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }
}
