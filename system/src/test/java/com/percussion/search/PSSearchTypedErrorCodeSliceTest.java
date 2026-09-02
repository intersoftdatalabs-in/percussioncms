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
package com.percussion.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.LuceneErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SearchErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.error.IPSErrorCode;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.search.lucene.PSSearchQueryImpl;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4155 (parent #2616): leftover search production call-sites use typed {@link
 * SearchErrorCodes} / {@link LuceneErrorCodes} (not bare {@code IPSSearchErrors} ints). Operational
 * codes skip dual-write; authentication failure remains auditable.
 */
@Tag("UnitTest")
public class PSSearchTypedErrorCodeSliceTest {

  @Test
  public void adminLockedUsesTypedNonAuditableCode() {
    PSAdminLockedException ex = new PSAdminLockedException();
    assertEquals(SearchErrorCodes.ADMIN_HANDLER_LOCKED.numericCode(), ex.getErrorCode());
    assertSame(SearchErrorCodes.ADMIN_HANDLER_LOCKED, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
    assertFalse(SearchErrorCodes.ADMIN_HANDLER_LOCKED.isAuditable());
  }

  @Test
  public void searchEngineRequiredUsesTypedNonAuditableCode() {
    PSSearchException ex = new PSSearchException(SearchErrorCodes.SEARCH_ENGINE_REQUIRED);
    assertEquals(SearchErrorCodes.SEARCH_ENGINE_REQUIRED.numericCode(), ex.getErrorCode());
    assertSame(SearchErrorCodes.SEARCH_ENGINE_REQUIRED, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void failedInitUsesTypedNonAuditableCode() {
    String[] args = {"com.example.Engine", "boom"};
    PSSearchException ex = new PSSearchException(SearchErrorCodes.SEARCH_ENGINE_FAILED_INIT, args);
    assertEquals(SearchErrorCodes.SEARCH_ENGINE_FAILED_INIT.numericCode(), ex.getErrorCode());
    assertSame(SearchErrorCodes.SEARCH_ENGINE_FAILED_INIT, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void htmlSearchMissingParameterUsesTypedNonAuditableCode() {
    Object[] args = {"HTML", "sys_searchid"};
    PSExtensionProcessingException ex =
        new PSExtensionProcessingException(SearchErrorCodes.HTML_SEARCH_MISSING_PARAMETER, args);
    assertEquals(SearchErrorCodes.HTML_SEARCH_MISSING_PARAMETER.numericCode(), ex.getErrorCode());
    assertSame(SearchErrorCodes.HTML_SEARCH_MISSING_PARAMETER, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void rawDumpPauseMessageUsesTypedNonAuditableCode() {
    PSSearchException ex =
        new PSSearchException(ServerErrorCodes.RAW_DUMP, "Search server unavailable");
    assertEquals(ServerErrorCodes.RAW_DUMP.numericCode(), ex.getErrorCode());
    assertSame(ServerErrorCodes.RAW_DUMP, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void luceneHitsIoExceptionUsesTypedNonAuditableCode() {
    IOException cause = new IOException("hits");
    PSSearchException ex = new PSSearchException(LuceneErrorCodes.HITS_IOEXCEPTION, cause);
    assertEquals(LuceneErrorCodes.HITS_IOEXCEPTION.numericCode(), ex.getErrorCode());
    assertSame(LuceneErrorCodes.HITS_IOEXCEPTION, ex.getTypedErrorCode());
    assertSame(cause, ex.getCause());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void luceneIndexIoSearchingUsesTypedNonAuditableCode() {
    IOException cause = new IOException("index");
    Object[] args = {"[301]"};
    PSSearchException ex =
        new PSSearchException(LuceneErrorCodes.INDEX_IO_EXCEPTION_SEARCHING, cause, args);
    assertEquals(LuceneErrorCodes.INDEX_IO_EXCEPTION_SEARCHING.numericCode(), ex.getErrorCode());
    assertSame(LuceneErrorCodes.INDEX_IO_EXCEPTION_SEARCHING, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void authenticationFailedRemainsAuditableForDualWrite() {
    PSSearchException ex =
        new PSSearchException(SearchErrorCodes.SEARCH_ENGINE_AUTHENTICATION_FAILED);
    assertEquals(
        SearchErrorCodes.SEARCH_ENGINE_AUTHENTICATION_FAILED.numericCode(), ex.getErrorCode());
    assertSame(SearchErrorCodes.SEARCH_ENGINE_AUTHENTICATION_FAILED, ex.getTypedErrorCode());
    assertTrue(ex.isAuditable());
    assertTrue(SearchErrorCodes.SEARCH_ENGINE_AUTHENTICATION_FAILED.isAuditable());
  }

  @Test
  public void typedConstructorsRejectNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSSearchException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSSearchException((IPSErrorCode) null, "x"));
  }

  @Test
  public void secondQueryImplConstructionUsesTypedUseGetInstance() throws Exception {
    PSSearchQueryImpl.getInstance();
    Constructor<PSSearchQueryImpl> ctor = PSSearchQueryImpl.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    InvocationTargetException wrapped =
        assertThrows(InvocationTargetException.class, ctor::newInstance);
    PSSearchException se = (PSSearchException) wrapped.getCause();
    assertSame(SearchErrorCodes.USE_GET_INSTANCE, se.getTypedErrorCode());
    assertFalse(se.isAuditable());
  }

  @Test
  public void nonNumericContentTypeUsesTypedInvalidIndexContentType() throws Exception {
    PSSearchQueryImpl query = PSSearchQueryImpl.getInstance();
    Method reader = PSSearchQueryImpl.class.getDeclaredMethod("getIndexReader", String.class);
    reader.setAccessible(true);
    InvocationTargetException wrapped =
        assertThrows(
            InvocationTargetException.class, () -> reader.invoke(query, "not-a-type-id"));
    PSSearchException se = (PSSearchException) wrapped.getCause();
    assertSame(SearchErrorCodes.INVALID_INDEX_CONTENTTYPE, se.getTypedErrorCode());
    assertFalse(se.isAuditable());
  }
}
