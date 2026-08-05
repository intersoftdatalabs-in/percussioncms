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

package com.percussion.taxonomy.web;

import com.percussion.taxonomy.TaxonomySecurityHelper;
import com.percussion.taxonomy.domain.Language;
import com.percussion.taxonomy.domain.Node;
import com.percussion.taxonomy.domain.Node_editor;
import com.percussion.taxonomy.service.TaxonomyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Base class for taxonomy controllers that perform security checks before delegating to the
 * taxonomy service. Provides the {@link TaxonParams} helper used to parse taxonomy, language, and
 * node parameters from the inbound request.
 */
// TODO: Update this with annotations
public class AbstractControllerWithSecurityChecks {

  /**
   * Default constructor; provided so the implicit default constructor has explicit Javadoc and
   * doclint does not warn about its use.
   */
  public AbstractControllerWithSecurityChecks() {
    // utility base class - no instance state
  }

  /** Holder for the taxonomy-related parameters extracted from an incoming request. */
  protected class TaxonParams {
    private int taxID;
    // Default to english
    private int langID = 1;
    private Integer nodeID = null;
    private Integer parentID = null;
    private boolean forJEXL = false;

    /**
     * @return the taxonomy id parsed from the request.
     */
    public int getTaxID() {
      return taxID;
    }

    /**
     * @return the language id parsed from the request, defaulting to English when unset.
     */
    public int getLangID() {
      return langID;
    }

    /**
     * @return the parsed node id, or <code>null</code> if the request had no node id.
     */
    public Integer getNodeID() {
      return nodeID.intValue();
    }

    /**
     * @return the parsed parent id, or <code>null</code> if the request had no parent id.
     */
    public Integer getParentID() {
      if (parentID == null) {
        return null;
      }
      return parentID.intValue();
    }

    /**
     * @return the parsed node id (which may be <code>null</code>), without the intValue coercion
     *     performed by {@link #getNodeID()}.
     */
    public Integer getNodeIDCanBeNull() {
      return nodeID;
    }

    /**
     * @return <code>true</code> when the request indicates the JEXL evaluation context.
     */
    public boolean getForJEXL() {
      return forJEXL;
    }

    /**
     * @return the parsed parent id (which may be <code>null</code>), without the intValue coercion
     *     performed by {@link #getParentID()}.
     */
    public Integer getParentIDCanBeNull() {
      return parentID;
    }

    /**
     * Constructs a parameter holder from explicit values, optionally overridden by <code>taxID
     * </code>/<code>langID</code> request parameters when present.
     *
     * @param request the current HTTP request, never <code>null</code>.
     * @param taxID the default taxonomy id, used when no request parameter overrides it.
     * @param langID the default language id, used when no request parameter overrides it.
     * @param taxonomyService the taxonomy service, used to resolve taxonomy name to id.
     * @throws Exception if a request parameter cannot be parsed.
     */
    public TaxonParams(
        HttpServletRequest request, int taxID, int langID, TaxonomyService taxonomyService)
        throws Exception {
      if (request.getParameter("taxID") != null && request.getParameter("langID") != null) {
        TaxonParams tp = new TaxonParams(request, taxonomyService);
        this.taxID = tp.getTaxID();
        this.langID = tp.getLangID();
        this.forJEXL = tp.forJEXL;
      } else {
        this.taxID = taxID;
        this.langID = langID;
        if (request.getParameter("forJEXL") != null) {
          this.forJEXL = Boolean.parseBoolean(request.getParameter("forJEXL"));
        }
      }
    }

    /**
     * @return <code>true</code> when a node id was parsed from the request.
     */
    public boolean hasNodeID() {
      return (this.nodeID != null);
    }

    /**
     * @return <code>true</code> when a parent id was parsed from the request.
     */
    public boolean hasParentID() {
      return (this.parentID != null);
    }

    /**
     * Constructs a parameter holder from the incoming request, parsing the standard
     * taxonomy/language/node/parent parameters and validating them.
     *
     * @param request the current HTTP request, never <code>null</code>.
     * @param taxonomyService the taxonomy service, used to resolve taxonomy name to id.
     * @throws Exception if a request parameter is invalid or cannot be parsed.
     */
    public TaxonParams(HttpServletRequest request, TaxonomyService taxonomyService)
        throws Exception {
      // there are two cases here --- Shawn
      // 1. if there is no parameter, getParemeter returns null
      // 2. if there parameter is on Request but has no value returns empty String eg nodeID=&
      if (request.getParameter("taxID") != null && request.getParameter("taxID").length() > 0) {
        try {
          this.taxID = Integer.parseInt(request.getParameter("taxID"));
        } catch (NumberFormatException e) {
          int taxId =
              taxonomyService.getTaxonomyIdByName(
                  TaxonomySecurityHelper.sanitizeInputForXSS(
                      StringUtils.stripToEmpty(request.getParameter("taxID"))));
          if (taxId > 0) {
            this.taxID = taxId;
          }
        }
        if (this.taxID <= 0) {
          throw new Exception("Invalid taxID param");
        }
      } else {
        this.taxID = 1;
      } // Default case if everything fails

      if (request.getParameter("langID") != null && request.getParameter("langID").length() > 0) {
        this.langID = Integer.parseInt(request.getParameter("langID"));
        if (this.langID <= 0) {
          throw new Exception("Invalid langID param");
        }
      } else {
        this.langID = Language.DEFAUL_LANG;
      } // Default case if everything fails

      if (request.getParameter("nodeID") != null && request.getParameter("nodeID").length() > 0) {
        this.nodeID = Integer.valueOf(Integer.parseInt(request.getParameter("nodeID")));
        if (this.nodeID <= 0) {
          throw new Exception("Invalid nodeID param");
        }
      }
      if (request.getParameter("parentID") != null
          && request.getParameter("parentID").length() > 0) {
        this.parentID = Integer.valueOf(Integer.parseInt(request.getParameter("parentID")));
        if (this.parentID < 0) {
          throw new Exception("Invalid parentID param");
        }
      }
    }
  }

  /**
   * Returns the current user's name, defaulting to <code>unknown</code> if the request has no
   * remote user.
   *
   * @param request the current HTTP request, never <code>null</code>.
   * @return the remote user name, never <code>null</code>.
   */
  protected String getUserName(HttpServletRequest request) {
    return StringUtils.defaultString(request.getRemoteUser(), "unknown");
  }

  /**
   * Verifies that the current user can edit the supplied taxonomy node, throwing if not.
   *
   * @param node the taxonomy node being edited, never <code>null</code>.
   * @throws Exception when the current user is not authorized to edit the node.
   */
  protected void verifyNodeIsEditable(Node node) throws Exception {
    // TODO throw new class
    if (!canEditNode(node)) {
      throw new Exception("Taxonomy Permission Exception: cannot edit node");
    }
  }

  /**
   * Determines whether the current user can edit the supplied taxonomy node based on the taxonomy
   * admin role and the node's editor role assignments.
   *
   * @param node the taxonomy node being checked, never <code>null</code>.
   * @return <code>true</code> when the current user is authorized to edit the node.
   * @throws Exception reserved for future implementation.
   */
  protected boolean canEditNode(Node node) throws Exception {
    boolean ret = false;

    if (TaxonomySecurityHelper.amITaxonomyAdmin()) {
      ret = true;
    } else {
      List<String> myRoles = TaxonomySecurityHelper.getMyRoles();
      Collection<Node_editor> editors = node.getNodeEditors();

      if (myRoles != null && editors != null) {

        for (Node_editor editor : editors) {

          if (myRoles.contains(editor.getRole())) {
            ret = true;
            break;
          }
        }
      }
    }
    return ret;
  }
}
