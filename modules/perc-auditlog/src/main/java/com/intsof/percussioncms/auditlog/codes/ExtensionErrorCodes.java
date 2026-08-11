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

package com.intsof.percussioncms.auditlog.codes;

import com.intsof.percussioncms.auditlog.AuditEventType;
import com.intsof.percussioncms.auditlog.AuditModule;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.SystemErrorCode;

/**
 * Extension error catalog bridging legacy {@code com.percussion.extension.IPSExtensionErrors}
 * ints (7001–7039 extension framework, 7301–7304 JS, 7401–7480 workflow/exit, 7621–7636
 * folder/JEXL/scheme).
 *
 * <p>{@link #numericCode()} preserves historical globally unique ints so exception constructors
 * and bundles stay stable. Every constant sets {@link #isAuditable()} explicitly:
 * authentication / authorization / checkout-checkin security failures dual-write;
 * parameter validation, installer, JS compile, and effect processing noise does not.
 *
 * <p>Module code is {@link AuditModule#SYS}. All ints are registered in the flat
 * {@link LegacyErrorCodeRegistry} (no package-local collision).
 */
public enum ExtensionErrorCodes implements SystemErrorCode {

  BACKEND_COLUMN_ERROR(
      7001,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Backend column error",
      "Backend column error detail={}"),

  UNKNOWN_PARAMETER_TYPE(
      7002,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown parameter type",
      "Unknown parameter type detail={}"),

  CATALOG_EXT_RESOURCE_ERROR(
      7003,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Catalog ext resource error",
      "Catalog ext resource error detail={}"),

  EXT_MISSING_REQUIRED_PARAMETER_ERROR(
      7004,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext missing required parameter error",
      "Ext missing required parameter error detail={}"),

  EXT_MISSING_HTML_PARAMETER_ERROR(
      7005,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext missing html parameter error",
      "Ext missing html parameter error detail={}"),

  EXT_PARAM_VALUE_MISMATCH(
      7006,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext param value mismatch",
      "Ext param value mismatch detail={}"),

  EXT_PARAM_VALUE_INVALID(
      7007,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext param value invalid",
      "Ext param value invalid detail={}"),

  EXT_INSTALLER_DEPLOY_NAME_EXPECTED(
      7008,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext installer deploy name expected",
      "Ext installer deploy name expected detail={}"),

  EXT_INSTALLER_UNSUPPORTED_RESOURCE(
      7009,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext installer unsupported resource",
      "Ext installer unsupported resource detail={}"),

  EXT_INSTALLER_RESOURCE_NOT_EXITING(
      7010,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext installer resource not exiting",
      "Ext installer resource not exiting detail={}"),

  EXT_INSTALLER_RESOURCE_NOT_READABLE(
      7011,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext installer resource not readable",
      "Ext installer resource not readable detail={}"),

  EXT_PROCESSOR_EXCEPTION(
      7012,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext processor exception",
      "Ext processor exception detail={}"),

  UNEXPECTED_EXT_TYPE_EXCEPTION(
      7013,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unexpected ext type exception",
      "Unexpected ext type exception detail={}"),

  EXT_HANDLER_LOAD_UNLOAD_ERROR(
      7014,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext handler load unload error",
      "Ext handler load unload error detail={}"),

  EXT_HANDLER_PREPARE_ERROR(
      7015,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext handler prepare error",
      "Ext handler prepare error detail={}"),

  EXT_HANDLER_DEF_STORE_ERROR(
      7016,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext handler def store error",
      "Ext handler def store error detail={}"),

  EXT_INSTALL_UPDATE_ERROR(
      7017,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext install update error",
      "Ext install update error detail={}"),

  EXT_RESOURCE_STORE_ERROR(
      7018,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext resource store error",
      "Ext resource store error detail={}"),

  EXT_RESOURCE_DELETE_ERROR(
      7019,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext resource delete error",
      "Ext resource delete error detail={}"),

  EXT_NOT_FOUND(
      7020,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext not found",
      "Ext not found detail={}"),

  EXT_ALREADY_EXISTS(
      7021,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext already exists",
      "Ext already exists detail={}"),

  EXT_MANAGER_INIT_FAILED(
      7022,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext manager init failed",
      "Ext manager init failed detail={}"),

  EXT_MANAGER_SHUTDOWN_FAILED(
      7023,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext manager shutdown failed",
      "Ext manager shutdown failed detail={}"),

  EXT_HANDLER_INIT_FAILED(
      7024,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext handler init failed",
      "Ext handler init failed detail={}"),

  EXT_HANDLER_SHUTDOWN_FAILED(
      7025,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext handler shutdown failed",
      "Ext handler shutdown failed detail={}"),

  EXT_INIT_FAILED(
      7026,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Ext init failed",
      "Ext init failed detail={}"),

  INVALID_EXT_TYPE_EXCEPTION(
      7027,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid ext type exception",
      "Invalid ext type exception detail={}"),

  UNKNOWN_EFFECT_PROCESSING_ERROR(
      7028,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Unknown effect processing error",
      "Unknown effect processing error detail={}"),

  INVALID_XML_ELEMENT(
      7029,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid xml element",
      "Invalid xml element detail={}"),

  MISSING_REQUIRED_ATTRIBUTE(
      7030,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing required attribute",
      "Missing required attribute detail={}"),

  CLASS_NOT_FOUND(
      7031,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Class not found",
      "Class not found detail={}"),

  INVALID_NULL_PARAMS(
      7032,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid null params",
      "Invalid null params detail={}"),

  MISSING_REQUIRED_PARAM_NO(
      7033,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing required param no",
      "Missing required param no detail={}"),

  INVALID_STRING_PARAM(
      7034,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid string param",
      "Invalid string param detail={}"),

  INVALID_NUMBER_PARAM(
      7035,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid number param",
      "Invalid number param detail={}"),

  INVALID_BOOLEAN_PARAM(
      7036,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid boolean param",
      "Invalid boolean param detail={}"),

  INVALID_DATE_PARAM(
      7037,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid date param",
      "Invalid date param detail={}"),

  INVALID_INDEX_VALUE(
      7038,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid index value",
      "Invalid index value detail={}"),

  INVALID_NUMBER_DEFAULT(
      7039,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid number default",
      "Invalid number default detail={}"),

  JS_COMPILE_FAILED(
      7301,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Js compile failed",
      "Js compile failed detail={}"),

  JS_COMPILE_FAILED_SRC(
      7302,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Js compile failed src",
      "Js compile failed src detail={}"),

  JS_CALL_FAILED(
      7303,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Js call failed",
      "Js call failed detail={}"),

  JS_CALL_FAILED_SRC(
      7304,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Js call failed src",
      "Js call failed src detail={}"),

  SET_EMPTYXML_STYLESHEET_NULL_SS(
      7401,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Set emptyxml stylesheet null ss",
      "Set emptyxml stylesheet null ss detail={}"),

  SET_EMPTYXML_STYLESHEET_INVALID_URL(
      7402,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Set emptyxml stylesheet invalid url",
      "Set emptyxml stylesheet invalid url detail={}"),

  MISSING_HTML_PARAMETER(
      7403,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing html parameter",
      "Missing html parameter detail={}"),

  COMMUNITIES_AUTHENTICATION_FAILED_NOCOMMUNITY(
      7404,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Communities authentication failed nocommunity",
      "Communities authentication failed nocommunity detail={}"),

  COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY(
      7405,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Communities authentication failed invalid community",
      "Communities authentication failed invalid community detail={}"),

  EMPTY_USRNAME1(
      7406,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Empty usrname1",
      "Empty usrname1 detail={}"),

  EMPTY_USRNAME2(
      7407,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Empty usrname2",
      "Empty usrname2 detail={}"),

  EMPTY_ROLE_LIST(
      7408,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Empty role list",
      "Empty role list detail={}"),

  INVALID_WORKFLOWID(
      7409,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid workflowid",
      "Invalid workflowid detail={}"),

  ILLEGAL_CONTENTTYPE(
      7410,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Illegal contenttype",
      "Illegal contenttype detail={}"),

  ILLEGAL_IF_CHECKEDOUT(
      7411,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Illegal if checkedout",
      "Illegal if checkedout detail={}"),

  ILLEGAL_IFNOT_CHECKEDOUT(
      7412,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Illegal ifnot checkedout",
      "Illegal ifnot checkedout detail={}"),

  ROLE_ERROR_STATEID_WORKFLOWID(
      7413,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Role error stateid workflowid",
      "Role error stateid workflowid detail={}"),

  ROLES_NOT_ASSIGNED(
      7414,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Roles not assigned",
      "Roles not assigned detail={}"),

  AUTHENTICATION_FAILED1(
      7415,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Authentication failed1",
      "Authentication failed1 detail={}"),

  AUTHENTICATION_FAILED2(
      7416,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Authentication failed2",
      "Authentication failed2 detail={}"),

  WKFLOW_ACTIONLIST_EMPTY(
      7417,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Wkflow actionlist empty",
      "Wkflow actionlist empty detail={}"),

  WKFLOW_CONTEXT_NULL(
      7418,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Wkflow context null",
      "Wkflow context null detail={}"),

  INVALID_WKFLOW_EXT(
      7419,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid wkflow ext",
      "Invalid wkflow ext detail={}"),

  EXEC_EXT_NOTFOUND(
      7420,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exec ext notfound",
      "Exec ext notfound detail={}"),

  STATUS_DOC_EMPTY(
      7421,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Status doc empty",
      "Status doc empty detail={}"),

  CONTENTID_NODENAME_EMPTY(
      7422,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Contentid nodename empty",
      "Contentid nodename empty detail={}"),

  CONTENTID_NODE_MISSING_EMPTY(
      7423,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Contentid node missing empty",
      "Contentid node missing empty detail={}"),

  CONTENTID_NODE_MISSING(
      7424,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Contentid node missing",
      "Contentid node missing detail={}"),

  EXIT_PARAM_NULL(
      7425,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Exit param null",
      "Exit param null detail={}"),

  CONTENTID_NULL(
      7426,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Contentid null",
      "Contentid null detail={}"),

  PUBDOC_UPDATE_ERROR(
      7427,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Pubdoc update error",
      "Pubdoc update error detail={}"),

  HTML_PARAM_NULL1(
      7428,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Html param null1",
      "Html param null1 detail={}"),

  HTML_PARAM_NULL2(
      7429,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Html param null2",
      "Html param null2 detail={}"),

  TABLE_NAME_NULL(
      7430,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Table name null",
      "Table name null detail={}"),

  PRIMARY_KEY_NULL(
      7431,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Primary key null",
      "Primary key null detail={}"),

  ROLEINFO_OBJ_NULL(
      7432,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Roleinfo obj null",
      "Roleinfo obj null detail={}"),

  ROLELIST_EMPTY(
      7433,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Rolelist empty",
      "Rolelist empty detail={}"),

  INVALID_PARAM_NUM(
      7434,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid param num",
      "Invalid param num detail={}"),

  ADMIN_CHECKOUT_ONLY(
      7435,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Admin checkout only",
      "Admin checkout only detail={}"),

  INVALID_TRANSITION_ROLE(
      7436,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Invalid transition role",
      "Invalid transition role detail={}"),

  TRANSITION_COMMENT_NOT_SPECIFIED(
      7437,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Transition comment not specified",
      "Transition comment not specified detail={}"),

  DOC_NOT_CHECKEDOUT(
      7438,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Doc not checkedout",
      "Doc not checkedout detail={}"),

  EDIT_REVISION_MISSING(
      7439,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Edit revision missing",
      "Edit revision missing detail={}"),

  CHECKIN_REVISION_MISMATCH(
      7440,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Checkin revision mismatch",
      "Checkin revision mismatch detail={}"),

  CHECKIN_NOT_ALLOWED(
      7441,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Checkin not allowed",
      "Checkin not allowed detail={}"),

  CHECKOUT_NOT_ALLOWED(
      7442,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Checkout not allowed",
      "Checkout not allowed detail={}"),

  CHECKOUT_REVISION_MISMATCH(
      7443,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Checkout revision mismatch",
      "Checkout revision mismatch detail={}"),

  CHECKOUT_REVISION_LIMIT(
      7444,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Checkout revision limit",
      "Checkout revision limit detail={}"),

  TRANSITION_ATTEMPT(
      7445,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Transition attempt",
      "Transition attempt detail={}"),

  MAIL_DOMAIN_NULL(
      7446,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mail domain null",
      "Mail domain null detail={}"),

  MAIL_DOMAIN_EMPTY(
      7447,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mail domain empty",
      "Mail domain empty detail={}"),

  SMTP_HOST_NULL(
      7448,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Smtp host null",
      "Smtp host null detail={}"),

  SMTP_HOST_EMPTY(
      7449,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Smtp host empty",
      "Smtp host empty detail={}"),

  USERNAME_NULL_EMPTY_TRIM(
      7450,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Username null empty trim",
      "Username null empty trim detail={}"),

  INVALID_ADHOC(
      7451,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid adhoc",
      "Invalid adhoc detail={}"),

  STATEROLE_NULL_EMPTY_TRIM(
      7452,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Staterole null empty trim",
      "Staterole null empty trim detail={}"),

  NO_RECORDS(
      7453,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "No records",
      "No records detail={}"),

  ADHOC_ASSIGNMENT_NOT_FOUND(
      7454,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Adhoc assignment not found",
      "Adhoc assignment not found detail={}"),

  TRANSLATION_ALREADY_EXISTS(
      7455,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Translation already exists",
      "Translation already exists detail={}"),

  ILLEGAL_IF_CHECKEDOUT_OVERRIDE(
      7456,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Illegal if checkedout override",
      "Illegal if checkedout override detail={}"),

  VALIDATE_SLOTNAME_NOT_UNIQUE(
      7457,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Validate slotname not unique",
      "Validate slotname not unique detail={}"),

  CHECKOUT_FROM_PUBLIC_STATE(
      7458,
      true,
      AuditEventType.ACCESS_DENIED,
      AuditOutcome.FAILURE,
      "Checkout from public state",
      "Checkout from public state detail={}"),

  INVALID_TRANSITION(
      7459,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid transition",
      "Invalid transition detail={}"),

  MISSING_TRANSITION(
      7460,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing transition",
      "Missing transition detail={}"),

  AUTHENTICATION_FAILED_DIFFERENT_ITEM_USER_COMMUNITIES(
      7461,
      true,
      AuditEventType.AUTH_FAILURE,
      AuditOutcome.FAILURE,
      "Authentication failed different item user communities",
      "Authentication failed different item user communities detail={}"),

  ILLEGAL_EXECUTION_CONTEXT(
      7462,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Illegal execution context",
      "Illegal execution context detail={}"),

  NONPROMOTABLE_RELATIONSHIP(
      7463,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Nonpromotable relationship",
      "Nonpromotable relationship detail={}"),

  WORKFLOWID_IN_REQUEST_ISNULL(
      7464,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Workflowid in request isnull",
      "Workflowid in request isnull detail={}"),

  INVALID_WORKFLOW_ACTION(
      7465,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid workflow action",
      "Invalid workflow action detail={}"),

  ITEM_NOT_IN_PUBLIC_STATE(
      7466,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Item not in public state",
      "Item not in public state detail={}"),

  EFFECT_SELF_TRIGGERED(
      7467,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Effect self triggered",
      "Effect self triggered detail={}"),

  INVALID_OPTION_FOR_FORCETRANSITION(
      7468,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid option for forcetransition",
      "Invalid option for forcetransition detail={}"),

  INVALID_TRANSITION_FOR_EFFECT(
      7469,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Invalid transition for effect",
      "Invalid transition for effect detail={}"),

  MISSING_INTERNAL_REQUEST_RESOURCE(
      7470,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Missing internal request resource",
      "Missing internal request resource detail={}"),

  DEPENDENT_ITEM_NOT_IN_DESIRED_STATE(
      7471,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dependent item not in desired state",
      "Dependent item not in desired state detail={}"),

  DEPENDENT_ITEM_CANNOT_GOTO_DESIRED_STATE(
      7472,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Dependent item cannot goto desired state",
      "Dependent item cannot goto desired state detail={}"),

  EFFECT_VALIDATE_MESSAGE(
      7473,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Effect validate message",
      "Effect validate message detail={}"),

  PROMOTE_TRANSITION_FAILED(
      7474,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Promote transition failed",
      "Promote transition failed detail={}"),

  BAD_PUBLISH_CONTENT_INITIALIZATION_DATA(
      7475,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Bad publish content initialization data",
      "Bad publish content initialization data detail={}"),

  BAD_PUBLISH_CONTENT_FILE_DATA(
      7476,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Bad publish content file data",
      "Bad publish content file data detail={}"),

  AUTHTYPE_REGISTRATION_MISSING(
      7477,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Authtype registration missing",
      "Authtype registration missing detail={}"),

  AUTHTYPE_RESOURCE_MISSING(
      7478,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Authtype resource missing",
      "Authtype resource missing detail={}"),

  WF_COMMENT_CANNOT_EXCEED_255(
      7479,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Wf comment cannot exceed 255",
      "Wf comment cannot exceed 255 detail={}"),

  MANDATORY_TRANSITION_VALIDATION_FAILURE(
      7480,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Mandatory transition validation failure",
      "Mandatory transition validation failure detail={}"),

  VARIANT_HAS_RELATIONSHIPS_ERROR(
      7621,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Variant has relationships error",
      "Variant has relationships error detail={}"),

  FOLDER_PATH_ERROR(
      7622,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Folder path error",
      "Folder path error detail={}"),

  JEXL_WRONG_RETURN_TYPE(
      7623,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Jexl wrong return type",
      "Jexl wrong return type detail={}"),

  JEXL_EVALUATION_FAILED(
      7634,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Jexl evaluation failed",
      "Jexl evaluation failed detail={}"),

  ERROR_GETTING_FOLDER_NAMES(
      7635,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Error getting folder names",
      "Error getting folder names detail={}"),

  SCHEME_CANT_BE_FOUND(
      7636,
      false,
      null,
      AuditOutcome.UNKNOWN,
      "Scheme cant be found",
      "Scheme cant be found detail={}");


  private final int numericCode;
  private final boolean auditable;
  private final AuditEventType eventType;
  private final AuditOutcome defaultOutcome;
  private final String userMessageTemplate;
  private final String logMessageTemplate;

  ExtensionErrorCodes(
      int numericCode,
      boolean auditable,
      AuditEventType eventType,
      AuditOutcome defaultOutcome,
      String userMessageTemplate,
      String logMessageTemplate) {
    this.numericCode = numericCode;
    this.auditable = auditable;
    this.eventType = eventType;
    this.defaultOutcome = defaultOutcome;
    this.userMessageTemplate = userMessageTemplate;
    this.logMessageTemplate = logMessageTemplate;
  }

  static {
    ensureRegistered();
  }

  /**
   * Register (or re-register) all constants in {@link LegacyErrorCodeRegistry}. Safe to call
   * repeatedly — used by registry bootstrap and tests after {@code clearForTests}.
   */
  public static void ensureRegistered() {
    for (ExtensionErrorCodes code : values()) {
      LegacyErrorCodeRegistry.register(code.numericCode(), code);
    }
  }

  @Override
  public AuditModule module() {
    return AuditModule.SYS;
  }

  @Override
  public int numericCode() {
    return numericCode;
  }

  @Override
  public String userMessageTemplate() {
    return userMessageTemplate;
  }

  @Override
  public String logMessageTemplate() {
    return logMessageTemplate;
  }

  @Override
  public boolean isAuditable() {
    return auditable;
  }

  @Override
  public AuditEventType eventType() {
    return eventType;
  }

  @Override
  public AuditOutcome defaultOutcome() {
    return defaultOutcome;
  }
}
