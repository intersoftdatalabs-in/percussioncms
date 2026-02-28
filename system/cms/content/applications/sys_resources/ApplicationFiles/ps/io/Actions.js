/******************************************************************************
 *
 * [ ps.io.Actions.js ]
 *
 * COPYRIGHT (c) 1999 - 2007 by Percussion Software, Inc., Woburn, MA USA.
 * All rights reserved. This material contains unpublished, copyrighted
 * work including confidential and proprietary information of Percussion.
 *
 *****************************************************************************/

/**
 * Track A1: Rewrote to use jQuery $.ajax() instead of dojo.io.bind().
 * Removed dependencies on dojo.collections.Dictionary, dojo.lang.assert,
 * dojo.lang.type, dojo.string.extras, and dojo.json.
 *
 * Uses IIFE singleton pattern: ps.io.Actions = new (function() { ... })()
 * matching the production bundled form. All callers access methods directly
 * via ps.io.Actions.method(), never via "new ps.io.Actions()".
 *
 * Preserved: synchronous XHR (async: false) to maintain identical behavior.
 */

// ---- Module-scoped constants ----

/**
 * Text to find to determine if the internal server error
 * is a missing auth error.
 */
var SESS_NOTAUTH_TEXT = "Processing Error: Not Authenticated";

/**
 * Error message to be displayed if authentication is needed
 * for current session.
 */
var ERROR_MSG_REQUIRES_AUTH =
  "This request requires authentication, but the " +
  "current session is not authenticated.\nThe user " +
  "session may have expired or the server may have been " +
  "restarted.\nYou must log back into Rhythmyx to continue.";

/**
 * Error message to be displayed if a 404 error code comes
 * back from a request.
 */
var ERROR_MSG_NO_SERVER =
  "Unable connect to the Rhythmyx server.\nThe server may be down." +
  "\nPlease contact your Rhythmyx administrator.";

// ---- Module-scoped private helper functions ----

/**
 * Maps a Dojo-style mimetype string to a jQuery $.ajax dataType.
 * @param {string} mimetype one of the MIMETYPE_* constants.
 * @return {string} jQuery dataType value.
 */
function _mimeToDataType(mimetype) {
  switch (mimetype) {
    case "text/json":
      return "json";
    case "text/xml":
      return "xml";
    case "text/html":
      return "html";
    default:
      return "text";
  }
}

/**
 * Helper that parses a response error string.
 * @param {string} error the error text.
 * @param {XMLHttpRequest} xhr the raw XHR object.
 * @return {{errorCode: *, message: string}}
 */
function _parseError(error, xhr) {
  var results = {};
  var transError = "XMLHttpTransport Error: ";
  if (typeof error === "string" && error.indexOf(transError) === 0) {
    var temp = error.substring(transError.length);
    var eCode = parseInt(temp.substring(0, 3), 10);
    var msg = temp.substring(4);
    results.errorCode = eCode;
    if (eCode === 404 || eCode === 0) {
      msg = ERROR_MSG_NO_SERVER;
      eCode = 404;
    } else if (
      eCode === 500 &&
      xhr &&
      xhr.responseText &&
      xhr.responseText.indexOf(SESS_NOTAUTH_TEXT) !== -1
    ) {
      msg = ERROR_MSG_REQUIRES_AUTH;
    }
    results.message = msg;
  } else {
    results.errorCode = "unknown";
    results.message = error;
  }
  return results;
}

/**
 * Flattens any single-value array properties on the provided object.
 * @param {*} obj the object to flatten.
 * @return {*} the modified object, never null.
 */
function _flattenArrayProperties(obj) {
  if (typeof obj !== "object" || obj === null) return obj;
  var isArr = Array.isArray(obj);
  var newObj = isArr ? [] : {};
  for (var item in obj) {
    if (!Object.prototype.hasOwnProperty.call(obj, item)) continue;
    var prop = obj[item];
    if (Array.isArray(prop) && prop.length === 1) {
      newObj[item] = prop[0];
    } else {
      newObj[item] = prop;
    }
  }
  return newObj;
}

/**
 * Asserts that the provided value is a ps.aa.ObjectId instance
 * (has a serialize method).
 * @param {*} objectId the value to check.
 */
function _assertObjectId(objectId) {
  if (
    objectId == null ||
    typeof objectId !== "object" ||
    typeof objectId.serialize !== "function"
  ) {
    throw new Error("Expected a ps.aa.ObjectId but got: " + objectId);
  }
}

/**
 * Asserts that a value is truthy.
 * @param {*} value the value to check.
 * @param {string} message the error message if the assertion fails.
 */
function _assert(value, message) {
  if (!value) {
    throw new Error(message || "Assertion failed");
  }
}

/**
 * Adds a key/value pair to a params object if the value is not
 * null or undefined.
 * @param {Object} params the parameters object.
 * @param {string} key the parameter name.
 * @param {*} value the parameter value.
 */
function _addOptionalParam(params, key, value) {
  if (value != null && value !== undefined) {
    params[key] = value;
  }
}

// ---- Singleton: ps.io.Actions ----
// IIFE — all callers access ps.io.Actions.method() directly.

ps.io.Actions = new (function () {
  // Mime type constants
  this.MIMETYPE_PLAIN = "text/plain";
  this.MIMETYPE_JSON = "text/json";
  this.MIMETYPE_HTML = "text/html";
  this.MIMETYPE_XML = "text/xml";

  // Form submit results holder
  this.formSubmitResults = null;

  /**
   * The number of locales the server has enabled. Set the first time the
   * getLocaleCount() method is called, which caches the returned value here.
   */
  this.localeCount = -1;

  // Error message constants (exposed as instance properties for callers)
  this.SESS_NOTAUTH_TEXT = SESS_NOTAUTH_TEXT;
  this.ERROR_MSG_REQUIRES_AUTH = ERROR_MSG_REQUIRES_AUTH;
  this.ERROR_MSG_NO_SERVER = ERROR_MSG_NO_SERVER;

  // Constant for needs template error
  this.NEEDS_TEMPLATE_ID = "needs_template_id";

  // ---- Action methods ----

  /**
   * Moves a slot item within a slot.
   *
   * @param {ps.aa.ObjectId} objectId the objectId object for
   *  this slot item (Required).
   * @param {string} mode one of the following "up", "down", "reorder"
   * (Required).
   * @param {string} index required if using reorder mode. Can be
   * null or empty if mode is "up" or "down".
   * Required if mode is "reorder".
   * @return ps.io.Response object.
   */
  this.move = function (objectId, mode, index) {
    _assertObjectId(objectId);
    var params = { mode: mode };
    if (index != null && index !== undefined) {
      params.index = index;
    }
    return this._makeRequest(
      "Move",
      this.MIMETYPE_PLAIN,
      objectId.serialize(),
      params,
    );
  };

  /**
   * Moves a slot item to another slot.
   *
   * @param {ps.aa.ObjectId} objectId the objectId object for this slot item
   * (Required).
   * @param {ps.aa.ObjectId} targetslotid the target slot id (Required).
   * @param {string} newtempid the template id,
   * may be null if the method should use the template currently
   * associated with the snippet.
   * @param {string} index the sort rank for the item in the target slot.
   * May be null in which case the item will be appended
   * to the end of the slot items.
   * @return ps.io.Response object. If a template is needed then
   * the isSuccess method of the response will be false and the
   * error message will be {@link ps.io.Actions#NEEDS_TEMPLATE_ID}.
   */
  this.moveToSlot = function (objectId, targetslotid, newtempid, index) {
    _assertObjectId(objectId);
    var params = { newslotid: targetslotid };
    if (newtempid) {
      params.newtemplate = newtempid;
    }
    if (index) {
      params.index = index;
    }
    return this._makeRequest(
      "MoveToSlot",
      this.MIMETYPE_PLAIN,
      objectId.serialize(),
      params,
    );
  };

  /**
   * Retrieves the url for the specified action name and content item.
   *
   * @param {ps.aa.ObjectId} objectId the objectId object for the
   * content item (Required).
   * @param {string} actionname the action name that defines the type
   * of url to be returned. One of the following strings:
   * CE_EDIT, CE_VIEW_CONTENT, CE_VIEW_PROPERTIES, CE_FIELDEDIT,
   * CE_VIEW_REVISIONS, CE_VIEW_AUDIT_TRAIL, PREVIEW_PAGE, PREVIEW_MYPAGE,
   * RC_SEARCH, TOOL_SHOW_AA_RELATIONSHIPS, TOOL_LINK_TO_PAGE, ACTION_xxx.
   *
   * Names of the form ACTION_xxx are generic. Any PSAction name registered
   * with the server can be used. The name should be supplied in place of the
   * xxx. e.g. ACTION_Translate.
   *
   * @return Json object, never null or empty.
   * The returned json object contains the following parameters:
   * url, dlg_height (field edit only), dlg_width (field edit only).
   */
  this.getUrl = function (objectId, actionname) {
    _assertObjectId(objectId);
    var params = { actionname: actionname };
    try {
      if (typeof ___sys_aamode !== "undefined" && ___sys_aamode != null)
        params.sys_aamode = ___sys_aamode;
    } catch (ignore) {}
    return this._makeRequest(
      "GetUrl",
      this.MIMETYPE_JSON,
      objectId.serialize(),
      params,
    );
  };

  /**
   * Looks up an action based on a name, then calculates whether the current
   * user should be able to see the action in this context.
   *
   * @param actionNames A single string or a set of strings, each of which is
   * used to lookup a PSAction. If empty, a successful response
   * with an empty object is returned.
   *
   * @param objectId The ps.aa.ObjectId that identifies the context.
   *
   * @return ps.io.Response whose value (if successful) is a map.
   */
  this.getActionVisibility = function (actionNames, objectId) {
    var o = this._normalizeNames(actionNames);
    if (o instanceof ps.io.Response) return o;
    if (typeof objectId === "undefined" || objectId == null) {
      var result = new ps.io.Response();
      result._m_success = true;
      result._m_value = {};
      return result;
    }

    var params = { names: o.names };
    var result = this._makeRequest(
      "GetActionVisibility",
      this.MIMETYPE_JSON,
      objectId.serialize(),
      params,
    );
    if (result.isSuccess()) {
      result._m_value = result._m_value[0];
    }
    return result;
  };

  /**
   * Converts the supplied type to an Object whose properties are the action
   * names, if necessary and assigns it to another Object as the value of its
   * 'names' property.
   *
   * @param actionNames May be undefined, null, primitive string, String, Array
   * of string or Object.
   *
   * @return Either a ps.io.Response, whose value is an empty Object, if
   * actionNames is undefined or null, or an Object with a single property,
   * names, whose value is an Object with a property for each name supplied
   * in actionNames and whose value is null.
   */
  this._normalizeNames = function (actionNames) {
    if (
      typeof actionNames === "undefined" ||
      actionNames == null ||
      (Array.isArray(actionNames) && actionNames.length === 0)
    ) {
      var result = new ps.io.Response();
      result._m_success = true;
      result._m_value = {};
      return result;
    }

    var p = {};
    if (typeof actionNames === "string") {
      p.names = { actionNames: null };
    } else if (typeof actionNames === "object") {
      if (actionNames instanceof String) p.names = { actionNames: null };
      else p.names = actionNames;
    }
    return p;
  };

  /**
   * Looks up an action based on a name, then retrieves its label.
   *
   * @param actionNames A single string or a set of strings, each of which is
   * used to lookup a PSAction.
   *
   * @return ps.io.Response whose value is a map.
   */
  this.getActionLabels = function (actionNames) {
    var o = this._normalizeNames(actionNames);
    if (o instanceof ps.io.Response) return o;
    var params = { names: o.names };
    return this._makeRequest(
      "GetActionLabels",
      this.MIMETYPE_JSON,
      null,
      params,
    );
  };

  /**
   * Returns the allowed content types for the specified slot.
   * @param {ps.aa.ObjectId} objectId the slot objectid (Required)
   * @return a Json array that will contain a Json object for each content type.
   */
  this.getAllowedContentTypeForSlot = function (objectId) {
    _assertObjectId(objectId);
    return this._makeRequest(
      "GetAllowedContentTypeForSlot",
      this.MIMETYPE_JSON,
      objectId.serialize(),
      null,
    );
  };

  /**
   * Retrieves the content type id for the content id passed in.
   *
   * @param {string} contentid of the content item in question.
   * @return Json object containing sys_contenttypeid.
   */
  this.getContentTypeByContentId = function (contentid) {
    var params = { sys_contentid: contentid };
    return this._makeRequest(
      "GetContentTypeByContentId",
      this.MIMETYPE_JSON,
      null,
      params,
    );
  };

  /**
   * Returns the urls of images of templates.
   * @param {String} contentTypeId (Required)
   * @param {ps.aa.ObjectId} objectId (Required)
   * @return ps.io.Response object containing an array of template info maps.
   */
  this.getTemplateImagesForContentType = function (contentTypeId, objectId) {
    _assert(contentTypeId, "contentTypeId is required");
    _assertObjectId(objectId);
    var params = { sys_contenttypeid: contentTypeId };
    return this._makeRequest(
      "GetTemplateImagesForContentType",
      this.MIMETYPE_JSON,
      objectId.serialize(),
      params,
    );
  };

  /**
   * Creates an item of supplied content type.
   * @param {int} contentTypeId content type id (Required)
   * @param {String} folderPath the path of the folder (Required)
   * @param {String} itemPath the source item path, may be null
   * @param {String} itemTitle title for the newly created item (Required)
   * @return ps.io.Response with itemId, folderId, or validationError.
   */
  this.createItem = function (contentTypeId, folderPath, itemPath, itemTitle) {
    _assert(contentTypeId, "contentTypeId is required");
    _assert(folderPath, "folderPath is required");
    _assert(itemTitle, "itemTitle is required");
    var params = {
      sys_contenttypeid: contentTypeId,
      folderPath: folderPath,
      itemPath: itemPath,
      itemTitle: itemTitle,
    };
    return this._makeRequest("CreateItem", this.MIMETYPE_JSON, null, params);
  };

  /**
   * Returns the item path of the supplied object id.
   * @param {ps.aa.ObjectId} objectId (Required)
   * @return path of the item of supplied objectId.
   */
  this.getItemPath = function (objectId) {
    _assertObjectId(objectId);
    return this._makeRequest(
      "GetItemPath",
      this.MIMETYPE_PLAIN,
      objectId.serialize(),
      null,
    );
  };

  /**
   * Action to get the content id or folder id by path.
   * @param {String} path of item or folder
   * @return JSONObject of id(int) and type(String either item or folder).
   */
  this.getIdByPath = function (path) {
    var params = { path: path };
    return this._makeRequest("GetIdByPath", this.MIMETYPE_JSON, null, params);
  };

  /**
   * Returns allowed templates for the specified snippet and a template count.
   * @param {ps.aa.ObjectId} objectId the snippet objectid (Required)
   * @return ps.io.Response with templateHtml and count.
   */
  this.getAllowedSnippetTemplates = function (objectId) {
    _assertObjectId(objectId);
    return this._makeRequest(
      "GetAllowedSnippetTemplates",
      this.MIMETYPE_JSON,
      objectId.serialize(),
      null,
    );
  };

  /**
   * Returns the allowed templates for the specified slot.
   * @param {ps.aa.ObjectId} objectId the slot objectid (Required).
   * @return a Json array of template objects.
   */
  this.getItemTemplatesForSlot = function (objectId) {
    _assertObjectId(objectId);
    return this._makeRequest(
      "GetItemTemplatesForSlot",
      this.MIMETYPE_JSON,
      objectId.serialize(),
      null,
    );
  };

  /**
   * Returns the assembled content for the specified field.
   * @param {ps.aa.ObjectId} objectId the field objectid (Required)
   * @param {boolean} isAAMode flag indicating that the content
   * should be decorated for active assembly.
   * @return html content.
   */
  this.getFieldContent = function (objectId, isAAMode) {
    _assertObjectId(objectId);
    var params = null;
    if (isAAMode) {
      params = { isaamode: "true" };
      try {
        if (typeof ___sys_aamode !== "undefined" && ___sys_aamode != null)
          params.sys_aamode = ___sys_aamode;
      } catch (ignore) {}
    }
    return this._makeRequest(
      "GetFieldContent",
      this.MIMETYPE_HTML,
      objectId.serialize(),
      params,
    );
  };

  /**
   * Returns the assembled content for the specified slot.
   * @param {ps.aa.ObjectId} objectId the slot objectid (Required)
   * @param {boolean} isAAMode flag indicating that the content
   * @return html content.
   */
  this.getSlotContent = function (objectId, isAAMode) {
    _assertObjectId(objectId);
    var params = null;
    if (isAAMode) {
      params = { isaamode: "true" };
      try {
        if (typeof ___sys_aamode !== "undefined" && ___sys_aamode != null)
          params.sys_aamode = ___sys_aamode;
      } catch (ignore) {}
    }
    return this._makeRequest(
      "GetSlotContent",
      this.MIMETYPE_HTML,
      objectId.serialize(),
      params,
    );
  };

  /**
   * Returns the assembled content for the specified snippet.
   * @param {ps.aa.ObjectId} objectId the snippet objectid (Required)
   * @param {boolean} isAAMode flag indicating that the content
   * @param {String} selectedtext (Optional).
   * @return html content.
   */
  this.getSnippetContent = function (objectId, isAAMode, selectedtext) {
    _assertObjectId(objectId);
    var params = null;
    if (isAAMode) {
      params = { isaamode: "true" };
      try {
        if (typeof ___sys_aamode !== "undefined" && ___sys_aamode != null)
          params.sys_aamode = ___sys_aamode;
      } catch (ignore) {}
    }
    if (
      selectedtext !== undefined &&
      selectedtext !== null &&
      selectedtext.length > 0
    ) {
      if (params == null) params = {};
      params.rxselectedtext = encodeURIComponent(selectedtext);
    }
    return this._makeRequest(
      "GetSnippetContent",
      this.MIMETYPE_HTML,
      objectId.serialize(),
      params,
    );
  };

  /**
   * Return the mime type of the assembled snippet.
   * @param {ps.aa.ObjectId} objectId the snippet objectid (Required)
   * @return a Json object containing mimetype.
   */
  this.getSnippetMimeType = function (objectId) {
    _assertObjectId(objectId);
    return this._makeRequest(
      "GetSnippetMimeType",
      this.MIMETYPE_JSON,
      objectId.serialize(),
      null,
    );
  };

  /**
   * Returns the assembled slot content for snippet picker dialog.
   * @param {ps.aa.ObjectId} objectId the slot objectid (Required)
   * @param {boolean} isTitles flag if true gets titles of the snippets.
   * @return html content.
   */
  this.getRenderedSlotContent = function (objectId, isTitles) {
    _assertObjectId(objectId);
    var params = null;
    if (isTitles) {
      params = { isTitles: "true" };
    }
    return this._makeRequest(
      "GetSnippetPickerSlotContent",
      this.MIMETYPE_HTML,
      objectId.serialize(),
      params,
    );
  };

  /**
   * Removes a specified snippet (or relationship).
   *
   * @param {int} relationshipIds the comma separated list of relationship
   *    ids to be removed. This is a required parameter.
   */
  this.removeSnippet = function (relationshipIds) {
    var params = { relationshipIds: relationshipIds };
    return this._makeRequest(
      "RemoveSnippet",
      this.MIMETYPE_PLAIN,
      null,
      params,
    );
  };

  /**
   * Adds a specified (new) snippet.
   *
   * @param {ps.aa.ObjectId} snippetId id of the new snippet (Required).
   * @param {ps.aa.ObjectId} slotId the slot id of the new snippet (Required).
   * @param {String} folderPath the folder path (Optional).
   * @param {String} siteName the site name (Optional).
   */
  this.addSnippet = function (snippetId, slotId, folderPath, siteName) {
    _assertObjectId(snippetId);
    _assertObjectId(slotId);
    var params = {
      dependentId: snippetId.getContentId(),
      templateId: snippetId.getTemplateId(),
      ownerId: slotId.getContentId(),
      slotId: slotId.getSlotId(),
    };
    _addOptionalParam(params, "folderPath", folderPath);
    _addOptionalParam(params, "siteName", siteName);
    return this._makeRequest("AddSnippet", this.MIMETYPE_JSON, null, params);
  };

  /**
   * Checks in the specified item.
   *
   * @param {int} contentId the id of the item to be checked in.
   * @param {String} commentText the (optional) comment for the checkin action.
   */
  this.checkInItem = function (contentId, commentText) {
    var params = { operation: "checkIn", contentId: contentId };
    _addOptionalParam(params, "comment", commentText);
    return this._makeRequest("Workflow", this.MIMETYPE_PLAIN, null, params);
  };

  /**
   * Checks out the specified item.
   *
   * @param {int} contentId the id of the item to be checked out.
   * @param {String} commentText the (optional) comment for the checkout action.
   */
  this.checkOutItem = function (contentId, commentText) {
    var params = { operation: "checkOut", contentId: contentId };
    _addOptionalParam(params, "comment", commentText);
    return this._makeRequest("Workflow", this.MIMETYPE_PLAIN, null, params);
  };

  /**
   * Transition and check out the specified item.
   *
   * @param {int} contentId the id of the item to be transitioned.
   * @param {String} trigger the (required) trigger name of the transition.
   * @param {String} commentText the (optional) comment.
   * @param {String} adhocUsers the (optional) adhoc users (';' delimited).
   */
  this.transitionCheckOutItem = function (
    contentId,
    trigger,
    commentText,
    adhocUsers,
  ) {
    var params = {
      operation: "transition_checkout",
      contentId: contentId,
      triggerName: trigger,
    };
    _addOptionalParam(params, "comment", commentText);
    _addOptionalParam(params, "adHocUsers", adhocUsers);
    return this._makeRequest("Workflow", this.MIMETYPE_PLAIN, null, params);
  };

  /**
   * Transition the specified item.
   *
   * @param {int} contentId the id of the item to be transitioned.
   * @param {String} trigger the (required) trigger name of the transition.
   * @param {String} commentText the (optional) comment.
   * @param {String} adhocUsers the (optional) adhoc users (';' delimited).
   */
  this.transitionItem = function (contentId, trigger, commentText, adhocUsers) {
    var params = {
      operation: "transition",
      contentId: contentId,
      triggerName: trigger,
    };
    _addOptionalParam(params, "comment", commentText);
    _addOptionalParam(params, "adHocUsers", adhocUsers);
    return this._makeRequest("Workflow", this.MIMETYPE_PLAIN, null, params);
  };

  /**
   * Adds the specified key/value to the given parameters if the
   * value is not null or undefined. Public wrapper for callers.
   *
   * @param {Object} params the parameters object.
   * @param {String} key the name of the optional parameter.
   * @param {*} value the optional parameter value.
   */
  this.addOptionalParam = function (params, key, value) {
    _addOptionalParam(params, key, value);
  };

  /**
   * Retrieves the sort rank for the items relationship.
   * @param {int} relid the relationship id (Required)
   * @return the sort rank.
   */
  this.getItemSortRank = function (relid) {
    var params = { sys_relationshipid: relid };
    return this._makeRequest(
      "GetItemSortRank",
      this.MIMETYPE_PLAIN,
      null,
      params,
    );
  };

  /**
   * Get all server properties.
   * @return a JSON object containing server properties.
   */
  this.getServerProperties = function () {
    return this._makeRequest(
      "GetServerProperties",
      this.MIMETYPE_JSON,
      null,
      null,
    );
  };

  /**
   * Get all registered sites from the system.
   * @return a list of sites as a JSON array.
   */
  this.getSites = function () {
    return this._makeRequest("GetSites", this.MIMETYPE_JSON, null, null);
  };

  /**
   * Get all root folders (Children of //Folders in the CX) from the system.
   * @return a list of root folders as a JSON array.
   */
  this.getRootFolders = function () {
    return this._makeRequest("GetRootFolders", this.MIMETYPE_JSON, null, null);
  };

  /**
   * Resolves the id values for the passed in site and site folder.
   * @return a JSON object with sys_folderid and sys_siteid properties.
   */
  this.resolveSiteFolders = function (siteName, folderPath) {
    var params = { folderPath: folderPath, siteName: siteName };
    return this._makeRequest(
      "ResolveSiteFolders",
      this.MIMETYPE_JSON,
      null,
      params,
    );
  };

  /**
   * Create a folder with supplied name under the parent folder supplied.
   * @param {string} parentFolderPath the parent folder path (Required).
   * @param {string} name the folder name (Required).
   * @param {boolean} isSiteFolder flag indicating site folder.
   * @return a JSON object representing the newly created folder.
   */
  this.createFolder = function (parentFolderPath, name, isSiteFolder) {
    var params = {
      parentFolderPath: parentFolderPath,
      folderName: name,
      category: isSiteFolder ? "sites" : "folders",
    };
    return this._makeRequest("CreateFolder", this.MIMETYPE_JSON, null, params);
  };

  /**
   * Get the children of the supplied folder path filtered for
   * the supplied content type.
   * @param {string} parentFolderPath (Required).
   * @param {int} ctypeid the content type id ("-1" for all).
   * @param {int} slotid the slot id (required if isSiteFolder is true).
   * @param {boolean} isSiteFolder flag for site folder.
   * @return child folders and items as JSON array.
   */
  this.getFolderChildren = function (
    parentFolderPath,
    ctypeid,
    slotid,
    isSiteFolder,
  ) {
    var params = {
      parentFolderPath: parentFolderPath,
      sys_contenttypeid: ctypeid,
      category: isSiteFolder ? "sites" : "folders",
    };
    if (isSiteFolder) params.sys_slotid = slotid;
    return this._makeRequest("GetChildren", this.MIMETYPE_JSON, null, params);
  };

  /**
   * Get the create item URL for the specified content type in a folder.
   * @param {string} parentFolderPath (Required).
   * @param {int} ctypeid the content type id (Required).
   * @param {boolean} isSiteFolder flag for site folder.
   * @return URL to open a content editor.
   */
  this.getCreateItemUrl = function (parentFolderPath, ctypeid, isSiteFolder) {
    var params = {
      parentFolderPath: parentFolderPath,
      sys_contenttypeid: ctypeid,
      category: isSiteFolder ? "sites" : "folders",
    };
    return this._makeRequest(
      "GetCreateItemUrl",
      this.MIMETYPE_JSON,
      null,
      params,
    );
  };

  /**
   * Gets the inline link parent ids for the specified dependent id.
   *
   * @param {int} dependentId The dependent id.
   * @param {Array} managedIds The managed ids as array of integers.
   * @return the response which contains the parent ids.
   */
  this.getInlinelinkParentIds = function (dependentId, managedIds) {
    var params = {
      dependentId: dependentId,
      managedIds: JSON.stringify(managedIds),
    };
    return this._makeRequest(
      "GetInlinelinkParents",
      this.MIMETYPE_JSON,
      null,
      params,
    );
  };

  /**
   * Gets the value of the field from the content editor.
   * @param {ps.aa.ObjectId} objectId (Required).
   */
  this.getContentEditorFieldValue = function (objectId) {
    _assertObjectId(objectId);
    return this._makeRequest(
      "GetContentEditorFieldValue",
      this.MIMETYPE_HTML,
      objectId.serialize(),
      null,
    );
  };

  /**
   * Sets the supplied value to the supplied field.
   * @param {ps.aa.ObjectId} objectId of the field (Required).
   * @param {String} fieldValue the value to set.
   */
  this.setContentEditorFieldValue = function (objectId, fieldValue) {
    _assertObjectId(objectId);
    var params = { fieldValue: fieldValue };
    return this._makeRequest(
      "SetContentEditorFieldValue",
      this.MIMETYPE_JSON,
      objectId.serialize(),
      params,
    );
  };

  /**
   * Builds action url for updating an item.
   */
  this.getUpdateItemUrl = function () {
    return this._buildRequestUrl("UpdateItem", null);
  };

  /**
   * Retrieves the server session max timeout in seconds.
   */
  this.getMaxTimeout = function () {
    return this._makeRequest("GetMaxTimeout", this.MIMETYPE_PLAIN, null, null);
  };

  /**
   * How many locales does the server support. If the request to the server
   * fails for any reason, the value is conservatively set to 1. The value is
   * cached after the first request.
   *
   * @return a value >= 1.
   */
  this.getLocaleCount = function () {
    if (this.localeCount > -1) return this.localeCount;

    var response = this._makeRequest(
      "GetLocaleCount",
      this.MIMETYPE_PLAIN,
      null,
      null,
    );
    if (response.isSuccess()) {
      this.localeCount = parseInt(response.getValue(), 10);
    } else {
      this.localeCount = 1;
    }
    return this.localeCount;
  };

  /**
   * Builds action url for getting the related content search results.
   */
  this.getRcSearchUrl = function () {
    return this._buildRequestUrl("GetSearchResults", null);
  };

  /**
   * Calls onsubmit on the supplied form object. If the form is not bound to
   * the supplied formObject alerts the user and returns null.
   * @param formObj the form object that needs to be submitted.
   * @return ps.io.Response object.
   */
  this.submitForm = function (formObj) {
    this.formSubmitResults = null;
    var fn = this._formBindConfig.formNode;
    var fid = typeof fn === "string" ? fn : fn.id;
    if (formObj.id !== fid) {
      alert("Error occurred submitting the form. The form is not bound.");
      return this.formSubmitResults;
    }
    formObj.onsubmit();
    return this.formSubmitResults;
  };

  /**
   * Initializes the form bind configuration for the supplied action and form.
   * This method must be called before calling submitForm.
   * @param {string} reqUrl the request URL.
   * @param {string} formId the form element id.
   * @param {string} mimetype the expected response mimetype.
   */
  this.initFormBind = function (reqUrl, formId, mimetype) {
    var _self = this;
    this._formBindConfig = {
      url: reqUrl,
      formNode: formId,
      mimetype: mimetype,
    };

    var formEl =
      typeof formId === "string" ? document.getElementById(formId) : formId;
    if (!formEl) return;

    $(formEl)
      .off("submit.psFormBind")
      .on("submit.psFormBind", function (e) {
        e.preventDefault();
        var result = new ps.io.Response();
        var dataType = _mimeToDataType(mimetype);

        $.ajax({
          url: reqUrl,
          type: "POST",
          data: $(formEl).serialize(),
          dataType: dataType,
          async: false,
          cache: false,
          success: function (data) {
            result._m_success = true;
            if (typeof data === "object")
              result._m_value = _flattenArrayProperties(data);
            else result._m_value = data;
          },
          error: function (xhr, textStatus, errorThrown) {
            result._m_success = false;
            var errorMsg =
              "XMLHttpTransport Error: " +
              xhr.status +
              " " +
              (errorThrown || textStatus);
            var msg = _parseError(errorMsg, xhr);
            result._m_value = msg.message;
            result._m_errorcode = msg.errorCode;
          },
        });

        _self.formSubmitResults = result;
      });
  };

  /**
   * Function to test calling the server.
   *
   * @param {string} mode
   * @return ps.io.Response object.
   */
  this.test = function (mode) {
    var params = { mode: mode };
    return this._makeRequest("Test", this.MIMETYPE_PLAIN, null, params);
  };

  /**
   * Sends a test request to the server to keep the session alive.
   */
  this.keepAlive = function () {
    var aresponse = ps.io.Actions.getMaxTimeout();
    if (!aresponse.isSuccess()) return;
    var delay = parseInt(aresponse.getValue(), 10) * 900;
    setTimeout(function () {
      ps.io.Actions.keepAlive();
    }, delay);
  };

  // ---- Private instance methods ----

  /**
   * Builds the server request url for the action.
   * Requires that __rxroot was set with the correct server root.
   * @param {string} action the action name, assumed not null or empty.
   * @param {string} objectId the object id string, may be null.
   */
  this._buildRequestUrl = function (action, objectId) {
    var base = __rxroot + "/contentui/aa?action=" + action;
    if (objectId != null && objectId !== undefined) {
      base += "&objectId=" + encodeURIComponent(objectId);
    }
    return base;
  };

  /**
   * Gets request url and makes the request using jQuery $.ajax().
   *
   * @param {string} action the action name, assumed not null or empty.
   * @param {string} mimetype the expected response mimetype.
   * @param {string} objectId the object id string, may be null.
   * @param {Object} params a plain object of parameters. May be null.
   * @return {ps.io.Response} the response object.
   */
  this._makeRequest = function (action, mimetype, objectId, params) {
    var result = new ps.io.Response();
    var requestUrl = this._buildRequestUrl(action, objectId);
    var dataType = _mimeToDataType(mimetype);

    // Build the data payload, filtering out null/empty values
    var data = {};
    if (params !== null && params !== undefined) {
      for (var key in params) {
        if (!Object.prototype.hasOwnProperty.call(params, key)) continue;
        var val = params[key];
        if (val == null || val === "" || val === undefined) continue;
        data[key] = val;
      }
    }

    $.ajax({
      url: requestUrl,
      type: "POST",
      data: data,
      dataType: dataType,
      async: false,
      cache: false,
      success: function (responseData) {
        result._m_success = true;
        result._m_value = _flattenArrayProperties(responseData);
      },
      error: function (xhr, textStatus, errorThrown) {
        result._m_success = false;
        var errorMsg =
          "XMLHttpTransport Error: " +
          xhr.status +
          " " +
          (errorThrown || textStatus);
        var msg = _parseError(errorMsg, xhr);
        result._m_value = msg.message;
        result._m_errorcode = msg.errorCode;
      },
    });

    return result;
  };

  /**
   * If the provided action response indicates an error,
   * reports the error to the user.
   * Does nothing if the response indicates success.
   * @param response an instance of ps.io.Response. Not null.
   */
  this.maybeReportActionError = function (response) {
    if (response && !response.isSuccess()) {
      ps.error(response.getValue());
    }
  };
})();
