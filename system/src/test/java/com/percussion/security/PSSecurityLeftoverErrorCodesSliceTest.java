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
package com.percussion.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.SystemErrorCode;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSRuntimeException;
import com.percussion.error.PSSqlException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.server.IPSServerErrors;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #3940 (parent #2616): leftover {@code com.percussion.security} production sites throw typed
 * {@code *ErrorCodes} (not bare {@code IPS*Errors} ints). Dual-write is skipped where the catalog
 * is non-auditable; leftover auditable SEC codes remain dual-write eligible.
 */
@Tag("UnitTest")
class PSSecurityLeftoverErrorCodesSliceTest {

  @Test
  void leftoverCatalogsMatchLegacyIntsAndSkipDualWriteWhereNonAuditable() {
    assertEquals(
        IPSSecurityErrors.AUTHENTICATION_NOT_SUPPORTED,
        SecurityErrorCodes.AUTHENTICATION_NOT_SUPPORTED.numericCode());
    assertEquals(
        IPSSecurityErrors.AUTHENTICATION_FAILED,
        SecurityErrorCodes.AUTHENTICATION_FAILED.numericCode());
    assertEquals(
        IPSSecurityErrors.PROVIDER_NOT_SUPPORTED_BY_CLASS,
        SecurityErrorCodes.PROVIDER_NOT_SUPPORTED_BY_CLASS.numericCode());
    assertEquals(
        IPSSecurityErrors.FILTERS_NOT_SUPPORTED,
        SecurityErrorCodes.FILTERS_NOT_SUPPORTED.numericCode());
    assertEquals(
        IPSSecurityErrors.USERS_NOT_SUPPORTED, SecurityErrorCodes.USERS_NOT_SUPPORTED.numericCode());
    assertEquals(
        IPSSecurityErrors.GROUPS_NOT_SUPPORTED,
        SecurityErrorCodes.GROUPS_NOT_SUPPORTED.numericCode());
    assertEquals(
        IPSSecurityErrors.AUTHENTICATION_REQUIRED,
        SecurityErrorCodes.AUTHENTICATION_REQUIRED.numericCode());
    assertEquals(
        IPSSecurityErrors.SESS_NOT_AUTHORIZED, SecurityErrorCodes.SESS_NOT_AUTHORIZED.numericCode());
    assertEquals(
        IPSSecurityErrors.USER_NOT_AUTHORIZED, SecurityErrorCodes.USER_NOT_AUTHORIZED.numericCode());
    assertEquals(
        IPSSecurityErrors.PROVIDER_INIT_EXCEPTION,
        SecurityErrorCodes.PROVIDER_INIT_EXCEPTION.numericCode());
    assertEquals(
        IPSSecurityErrors.PROVIDER_UNKNOWN, SecurityErrorCodes.PROVIDER_UNKNOWN.numericCode());
    assertEquals(
        IPSSecurityErrors.AUTHENTICATION_FAILED_WITH_MSG,
        SecurityErrorCodes.AUTHENTICATION_FAILED_WITH_MSG.numericCode());
    assertEquals(
        IPSSecurityErrors.PROVIDER_INIT_CATALOG_DISABLED,
        SecurityErrorCodes.PROVIDER_INIT_CATALOG_DISABLED.numericCode());
    assertEquals(
        IPSSecurityErrors.PROVIDER_INSTANCE_NAME_DUPLICATED,
        SecurityErrorCodes.PROVIDER_INSTANCE_NAME_DUPLICATED.numericCode());
    assertEquals(
        IPSSecurityErrors.NATIVE_AUTHENTICATION_FAILURE,
        SecurityErrorCodes.NATIVE_AUTHENTICATION_FAILURE.numericCode());
    assertEquals(
        IPSSecurityErrors.SSL_KEY_STRENGTH_TOO_WEAK,
        SecurityErrorCodes.SSL_KEY_STRENGTH_TOO_WEAK.numericCode());
    assertEquals(
        IPSSecurityErrors.DATA_ENCRYPTION_ERROR_MSG,
        SecurityErrorCodes.DATA_ENCRYPTION_ERROR.numericCode());
    assertEquals(
        IPSSecurityErrors.MULTI_AUTHENTICATION_FAILED,
        SecurityErrorCodes.MULTI_AUTHENTICATION_FAILED.numericCode());
    assertEquals(
        IPSSecurityErrors.METADATA_UNAVAILABLE,
        SecurityErrorCodes.METADATA_UNAVAILABLE.numericCode());
    assertEquals(
        IPSSecurityErrors.GET_GROUPS_FAILURE, SecurityErrorCodes.GET_GROUPS_FAILURE.numericCode());
    assertEquals(
        IPSSecurityErrors.GROUP_PROVIDER_MISSING,
        SecurityErrorCodes.GROUP_PROVIDER_MISSING.numericCode());
    assertEquals(
        IPSSecurityErrors.LOCAL_ROLE_NOT_DEFINED,
        SecurityErrorCodes.LOCAL_ROLE_NOT_DEFINED.numericCode());
    assertEquals(
        IPSSecurityErrors.GLOBAL_ROLE_NOT_DEFINED,
        SecurityErrorCodes.GLOBAL_ROLE_NOT_DEFINED.numericCode());
    assertEquals(
        IPSSecurityErrors.LOCAL_ROLE_ALREADY_DEFINED,
        SecurityErrorCodes.LOCAL_ROLE_ALREADY_DEFINED.numericCode());
    assertEquals(
        IPSSecurityErrors.GLOBAL_ROLE_ALREADY_DEFINED,
        SecurityErrorCodes.GLOBAL_ROLE_ALREADY_DEFINED.numericCode());
    assertEquals(
        IPSSecurityErrors.DIR_PASSWORD_FILTER_INIT_ERROR,
        SecurityErrorCodes.DIR_PASSWORD_FILTER_INIT_ERROR.numericCode());
    assertEquals(
        IPSSecurityErrors.DIR_PASSWORD_REQUIRED,
        SecurityErrorCodes.DIR_PASSWORD_REQUIRED.numericCode());
    assertEquals(
        IPSSecurityErrors.DIR_GET_OBJECTS_FAILED,
        SecurityErrorCodes.DIR_GET_OBJECTS_FAILED.numericCode());
    assertEquals(
        IPSSecurityErrors.DIR_REFERENCED_DIRECTORYSET_NOT_FOUND,
        SecurityErrorCodes.DIR_REFERENCED_DIRECTORYSET_NOT_FOUND.numericCode());
    assertEquals(
        IPSSecurityErrors.DIR_REFERENCED_DIRECTORY_NOT_FOUND,
        SecurityErrorCodes.DIR_REFERENCED_DIRECTORY_NOT_FOUND.numericCode());
    assertEquals(
        IPSSecurityErrors.DIR_REFERENCED_AUTHENTICATION_NOT_FOUND,
        SecurityErrorCodes.DIR_REFERENCED_AUTHENTICATION_NOT_FOUND.numericCode());
    assertEquals(
        IPSSecurityErrors.DIR_REFERENCED_ROLEPROVIDER_NOT_FOUND,
        SecurityErrorCodes.DIR_REFERENCED_ROLEPROVIDER_NOT_FOUND.numericCode());
    assertEquals(
        IPSSecurityErrors.BETABLE_ERROR_UID_NOT_UNIQUE,
        SecurityErrorCodes.BETABLE_ERROR_UID_NOT_UNIQUE.numericCode());
    assertEquals(
        IPSSecurityErrors.NO_EMAIL_ATTRIBUTE_NAME,
        SecurityErrorCodes.NO_EMAIL_ATTRIBUTE_NAME.numericCode());
    assertEquals(
        IPSSecurityErrors.BETABLE_DIRECTORY_CATALOGER_ERROR,
        SecurityErrorCodes.BETABLE_DIRECTORY_CATALOGER_ERROR.numericCode());
    assertEquals(
        IPSSecurityErrors.CATALOG_PROVIDER_CLASS_NOT_FOUND,
        SecurityErrorCodes.CATALOG_PROVIDER_CLASS_NOT_FOUND.numericCode());
    assertEquals(
        IPSSecurityErrors.REFERENCED_DIRECTORY_NOT_FOUND,
        SecurityErrorCodes.REFERENCED_DIRECTORY_NOT_FOUND.numericCode());
    assertEquals(
        IPSSecurityErrors.REFERENCED_AUTHENTICATION_NOT_FOUND,
        SecurityErrorCodes.REFERENCED_AUTHENTICATION_NOT_FOUND.numericCode());
    assertEquals(
        IPSSecurityErrors.DIRECTORY_AUTHENTICATION_FAILED,
        SecurityErrorCodes.DIRECTORY_AUTHENTICATION_FAILED.numericCode());
    assertEquals(
        IPSSecurityErrors.UNKNOWN_NAMING_ERROR,
        SecurityErrorCodes.UNKNOWN_NAMING_ERROR.numericCode());
    assertEquals(
        IPSSecurityErrors.PARSE_JNDI_PROVIDER_URL_ERROR,
        SecurityErrorCodes.PARSE_JNDI_PROVIDER_URL_ERROR.numericCode());
    assertEquals(IPSServerErrors.RAW_DUMP, ServerErrorCodes.RAW_DUMP.numericCode());
    assertEquals(
        IPSServerErrors.RESPONSE_SEND_ERROR, ServerErrorCodes.RESPONSE_SEND_ERROR.numericCode());

    leftoverNonAuditable(SecurityErrorCodes.AUTHENTICATION_NOT_SUPPORTED);
    leftoverNonAuditable(SecurityErrorCodes.PROVIDER_NOT_SUPPORTED_BY_CLASS);
    leftoverNonAuditable(SecurityErrorCodes.FILTERS_NOT_SUPPORTED);
    leftoverNonAuditable(SecurityErrorCodes.USERS_NOT_SUPPORTED);
    leftoverNonAuditable(SecurityErrorCodes.GROUPS_NOT_SUPPORTED);
    leftoverNonAuditable(SecurityErrorCodes.PROVIDER_INIT_EXCEPTION);
    leftoverNonAuditable(SecurityErrorCodes.PROVIDER_UNKNOWN);
    leftoverNonAuditable(SecurityErrorCodes.PROVIDER_INIT_CATALOG_DISABLED);
    leftoverNonAuditable(SecurityErrorCodes.PROVIDER_INSTANCE_NAME_DUPLICATED);
    leftoverNonAuditable(SecurityErrorCodes.METADATA_UNAVAILABLE);
    leftoverNonAuditable(SecurityErrorCodes.GET_GROUPS_FAILURE);
    leftoverNonAuditable(SecurityErrorCodes.GROUP_PROVIDER_MISSING);
    leftoverNonAuditable(SecurityErrorCodes.LOCAL_ROLE_NOT_DEFINED);
    leftoverNonAuditable(SecurityErrorCodes.GLOBAL_ROLE_NOT_DEFINED);
    leftoverNonAuditable(SecurityErrorCodes.DIR_PASSWORD_FILTER_INIT_ERROR);
    leftoverNonAuditable(SecurityErrorCodes.DIR_GET_OBJECTS_FAILED);
    leftoverNonAuditable(SecurityErrorCodes.DIR_REFERENCED_DIRECTORYSET_NOT_FOUND);
    leftoverNonAuditable(SecurityErrorCodes.NO_EMAIL_ATTRIBUTE_NAME);
    leftoverNonAuditable(SecurityErrorCodes.BETABLE_DIRECTORY_CATALOGER_ERROR);
    leftoverNonAuditable(SecurityErrorCodes.CATALOG_PROVIDER_CLASS_NOT_FOUND);
    leftoverNonAuditable(SecurityErrorCodes.UNKNOWN_NAMING_ERROR);
    leftoverNonAuditable(SecurityErrorCodes.PARSE_JNDI_PROVIDER_URL_ERROR);
    leftoverNonAuditable(ServerErrorCodes.RAW_DUMP);
    leftoverNonAuditable(ServerErrorCodes.RESPONSE_SEND_ERROR);

    leftoverAuditable(SecurityErrorCodes.AUTHENTICATION_FAILED);
    leftoverAuditable(SecurityErrorCodes.AUTHENTICATION_FAILED_WITH_MSG);
    leftoverAuditable(SecurityErrorCodes.AUTHENTICATION_REQUIRED);
    leftoverAuditable(SecurityErrorCodes.SESS_NOT_AUTHORIZED);
    leftoverAuditable(SecurityErrorCodes.USER_NOT_AUTHORIZED);
    leftoverAuditable(SecurityErrorCodes.NATIVE_AUTHENTICATION_FAILURE);
    leftoverAuditable(SecurityErrorCodes.SSL_KEY_STRENGTH_TOO_WEAK);
    leftoverAuditable(SecurityErrorCodes.DATA_ENCRYPTION_ERROR);
    leftoverAuditable(SecurityErrorCodes.MULTI_AUTHENTICATION_FAILED);
    leftoverAuditable(SecurityErrorCodes.LOCAL_ROLE_ALREADY_DEFINED);
    leftoverAuditable(SecurityErrorCodes.GLOBAL_ROLE_ALREADY_DEFINED);
    leftoverAuditable(SecurityErrorCodes.DIR_PASSWORD_REQUIRED);
    leftoverAuditable(SecurityErrorCodes.BETABLE_ERROR_UID_NOT_UNIQUE);
    leftoverAuditable(SecurityErrorCodes.DIRECTORY_AUTHENTICATION_FAILED);
  }

  @Test
  void leftoverProductionExceptionTypesRetainTypedCodes() {
    PSAuthenticationFailedException authFailed =
        new PSAuthenticationFailedException("Directory", "ldap1", "alice");
    assertSame(SecurityErrorCodes.AUTHENTICATION_FAILED, authFailed.getTypedErrorCode());
    assertEquals(SecurityErrorCodes.AUTHENTICATION_FAILED.numericCode(), authFailed.getErrorCode());
    assertTrue(authFailed.isAuditable());

    PSAuthenticationFailedException withMsg =
        new PSAuthenticationFailedException("Directory", "ldap1", "alice", "bad password");
    assertSame(SecurityErrorCodes.AUTHENTICATION_FAILED_WITH_MSG, withMsg.getTypedErrorCode());
    assertTrue(withMsg.isAuditable());

    PSAuthenticationFailedException dirSet =
        new PSAuthenticationFailedException(
            SecurityErrorCodes.DIR_REFERENCED_DIRECTORYSET_NOT_FOUND, new Object[] {"corp"});
    assertSame(
        SecurityErrorCodes.DIR_REFERENCED_DIRECTORYSET_NOT_FOUND, dirSet.getTypedErrorCode());
    assertFalse(dirSet.isAuditable());

    PSAuthenticationFailedException uidDup =
        new PSAuthenticationFailedException(
            SecurityErrorCodes.BETABLE_ERROR_UID_NOT_UNIQUE, new Object[] {"rx", "alice"});
    assertSame(SecurityErrorCodes.BETABLE_ERROR_UID_NOT_UNIQUE, uidDup.getTypedErrorCode());
    assertTrue(uidDup.isAuditable());

    PSAuthenticationFailedExException multi =
        new PSAuthenticationFailedExException(List.of(authFailed).iterator());
    assertSame(SecurityErrorCodes.MULTI_AUTHENTICATION_FAILED, multi.getTypedErrorCode());
    assertTrue(multi.isAuditable());

    PSAuthenticationRequiredException required =
        new PSAuthenticationRequiredException("Application", "rx");
    assertSame(SecurityErrorCodes.AUTHENTICATION_REQUIRED, required.getTypedErrorCode());
    assertTrue(required.isAuditable());

    PSAuthorizationException session =
        new PSAuthorizationException("Application", "rx", "sess-1");
    assertSame(SecurityErrorCodes.SESS_NOT_AUTHORIZED, session.getTypedErrorCode());
    assertTrue(session.isAuditable());

    PSAuthorizationException user =
        new PSAuthorizationException("en-us", "Application", "rx", "Directory", "alice");
    assertSame(SecurityErrorCodes.USER_NOT_AUTHORIZED, user.getTypedErrorCode());
    assertTrue(user.isAuditable());

    PSAuthenticationUnsupportedException unsupported =
        new PSAuthenticationUnsupportedException("ODBC");
    assertSame(SecurityErrorCodes.AUTHENTICATION_NOT_SUPPORTED, unsupported.getTypedErrorCode());
    assertFalse(unsupported.isAuditable());

    PSFiltersNotSupportedException filters = new PSFiltersNotSupportedException("ODBC");
    assertSame(SecurityErrorCodes.FILTERS_NOT_SUPPORTED, filters.getTypedErrorCode());
    assertFalse(filters.isAuditable());

    PSGroupsNotSupportedException groups = new PSGroupsNotSupportedException("ODBC");
    assertSame(SecurityErrorCodes.GROUPS_NOT_SUPPORTED, groups.getTypedErrorCode());
    assertFalse(groups.isAuditable());

    PSUsersNotSupportedException users = new PSUsersNotSupportedException("ODBC");
    assertSame(SecurityErrorCodes.USERS_NOT_SUPPORTED, users.getTypedErrorCode());
    assertFalse(users.isAuditable());

    PSNativeMethodException nativeAuth = new PSNativeMethodException("impersonate failed");
    assertSame(SecurityErrorCodes.NATIVE_AUTHENTICATION_FAILURE, nativeAuth.getTypedErrorCode());
    assertTrue(nativeAuth.isAuditable());

    PSUnsupportedProviderException provider =
        new PSUnsupportedProviderException("com.example.Sp", "ODBC");
    assertSame(SecurityErrorCodes.PROVIDER_NOT_SUPPORTED_BY_CLASS, provider.getTypedErrorCode());
    assertFalse(provider.isAuditable());

    PSRoleAlreadyDefinedException localRole = new PSRoleAlreadyDefinedException("rx", "Editor");
    assertSame(SecurityErrorCodes.LOCAL_ROLE_ALREADY_DEFINED, localRole.getTypedErrorCode());
    assertTrue(localRole.isAuditable());

    PSRoleAlreadyDefinedException globalRole = new PSRoleAlreadyDefinedException("Admin");
    assertSame(SecurityErrorCodes.GLOBAL_ROLE_ALREADY_DEFINED, globalRole.getTypedErrorCode());
    assertTrue(globalRole.isAuditable());

    PSRoleNotDefinedException localMissing = new PSRoleNotDefinedException("rx", "Missing");
    assertSame(SecurityErrorCodes.LOCAL_ROLE_NOT_DEFINED, localMissing.getTypedErrorCode());
    assertFalse(localMissing.isAuditable());

    PSRoleNotDefinedException globalMissing = new PSRoleNotDefinedException("Missing");
    assertSame(SecurityErrorCodes.GLOBAL_ROLE_NOT_DEFINED, globalMissing.getTypedErrorCode());
    assertFalse(globalMissing.isAuditable());

    PSSecurityException meta = new PSSecurityException("catalog unavailable");
    assertSame(SecurityErrorCodes.METADATA_UNAVAILABLE, meta.getTypedErrorCode());
    assertFalse(meta.isAuditable());

    RuntimeException cause = new RuntimeException("naming");
    PSSecurityException naming =
        new PSSecurityException(
            SecurityErrorCodes.UNKNOWN_NAMING_ERROR, new Object[] {"ldap boom"}, cause);
    assertSame(SecurityErrorCodes.UNKNOWN_NAMING_ERROR, naming.getTypedErrorCode());
    assertSame(cause, naming.getCause());
    assertFalse(naming.isAuditable());

    PSRuntimeException filterInit =
        new PSRuntimeException(
            SecurityErrorCodes.DIR_PASSWORD_FILTER_INIT_ERROR,
            new Object[] {"sys_default", "missing"});
    assertSame(SecurityErrorCodes.DIR_PASSWORD_FILTER_INIT_ERROR, filterInit.getTypedErrorCode());
    assertFalse(filterInit.isAuditable());

    PSSqlException dirObjects =
        new PSSqlException(SecurityErrorCodes.DIR_GET_OBJECTS_FAILED, "ldap timeout", "0");
    assertSame(SecurityErrorCodes.DIR_GET_OBJECTS_FAILED, dirObjects.getTypedErrorCode());
    assertFalse(dirObjects.isAuditable());

    PSExtensionProcessingException raw =
        new PSExtensionProcessingException(ServerErrorCodes.RAW_DUMP, "ACL create failed");
    assertSame(ServerErrorCodes.RAW_DUMP, raw.getTypedErrorCode());
    assertEquals(IPSServerErrors.RAW_DUMP, raw.getErrorCode());
    assertFalse(raw.isAuditable());
  }

  @Test
  void typedProductionCtorsRejectNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSSecurityException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSSecurityException((IPSErrorCode) null, new Object[] {"x"}));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PSSecurityException(
                (IPSErrorCode) null, new Object[] {"x"}, new RuntimeException("c")));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSAuthenticationFailedException((IPSErrorCode) null, new Object[] {"x"}));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSAuthorizationException((IPSErrorCode) null, new Object[] {"x"}));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSExtensionProcessingException((IPSErrorCode) null, "sql"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSSqlException((IPSErrorCode) null, "ldap", "0"));
  }

  private static void leftoverNonAuditable(SystemErrorCode code) {
    assertFalse(code.isAuditable(), code.toString());
  }

  private static void leftoverAuditable(SystemErrorCode code) {
    assertTrue(code.isAuditable(), code.toString());
  }
}
